package xyrus.code.ytplucker.data

import android.content.ContentValues
import android.content.Context
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.webkit.MimeTypeMap
import java.io.File

/**
 * Publishes a finished download into the device's public media collections so it shows up in the
 * gallery / music player — routed by type: video to Movies, image to Pictures, audio to Music,
 * each under a "YT-Plucker" sub-folder.
 *
 * On API 29+ this uses MediaStore's RELATIVE_PATH (scoped storage, no permission needed). On
 * API 28 and below it writes to the public directory directly (requires WRITE_EXTERNAL_STORAGE)
 * and asks the media scanner to index it.
 */
object MediaExporter {

    /** Public sub-folder name used across all three media roots. */
    const val ALBUM = "YT-Plucker"

    private data class Target(
        val collection: Uri,
        val publicDir: String, // Environment.DIRECTORY_*
    )

    fun mimeOf(file: File): String {
        val ext = file.extension.lowercase()
        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext)
            ?: when (ext) {
                "mkv" -> "video/x-matroska"
                "m4a" -> "audio/mp4"
                "opus" -> "audio/opus"
                else -> "application/octet-stream"
            }
    }

    private fun targetFor(mime: String): Target = when {
        mime.startsWith("image") -> Target(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI, Environment.DIRECTORY_PICTURES,
        )
        mime.startsWith("audio") -> Target(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, Environment.DIRECTORY_MUSIC,
        )
        else -> Target( // video/* and anything unknown lands in Movies
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI, Environment.DIRECTORY_MOVIES,
        )
    }

    /**
     * Copy [source] into the appropriate public collection. Returns the content Uri of the saved
     * item, or null on failure. Never holds the file in the heap — streams disk-to-disk.
     */
    fun export(context: Context, source: File, mime: String = mimeOf(source)): Uri? {
        val target = targetFor(mime)
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            exportViaMediaStore(context, source, mime, target)
        } else {
            exportLegacy(context, source, mime, target)
        }
    }

    private fun exportViaMediaStore(context: Context, source: File, mime: String, target: Target): Uri? {
        val resolver = context.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, source.name)
            put(MediaStore.MediaColumns.MIME_TYPE, mime)
            put(MediaStore.MediaColumns.RELATIVE_PATH, "${target.publicDir}/$ALBUM")
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        }
        val uri = resolver.insert(target.collection, values) ?: return null
        return try {
            resolver.openOutputStream(uri)?.use { out ->
                source.inputStream().use { it.copyTo(out, DEFAULT_BUFFER_SIZE) }
            } ?: throw IllegalStateException("null output stream")
            values.clear()
            values.put(MediaStore.MediaColumns.IS_PENDING, 0)
            resolver.update(uri, values, null, null)
            uri
        } catch (e: Exception) {
            resolver.delete(uri, null, null) // roll back the pending row
            null
        }
    }

    private fun exportLegacy(context: Context, source: File, mime: String, target: Target): Uri? {
        @Suppress("DEPRECATION")
        val dir = File(Environment.getExternalStoragePublicDirectory(target.publicDir), ALBUM)
        if (!dir.exists() && !dir.mkdirs()) return null
        val dest = File(dir, source.name)
        return try {
            source.inputStream().use { input -> dest.outputStream().use { input.copyTo(it) } }
            var result: Uri? = null
            MediaScannerConnection.scanFile(context, arrayOf(dest.absolutePath), arrayOf(mime)) { _, uri ->
                result = uri
            }
            result ?: Uri.fromFile(dest)
        } catch (e: Exception) {
            null
        }
    }
}
