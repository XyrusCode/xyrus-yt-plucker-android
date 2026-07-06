package xyrus.code.ytplucker.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import xyrus.code.ytplucker.domain.model.DownloadedFile

/**
 * Reads back the files this app has published into the public media collections (see
 * [MediaExporter]) so the History tab can list them. Reading MediaStore keeps history in sync
 * with reality — if the user deletes a file in their gallery, it drops out of history for free.
 */
object HistoryStore {

    private val collections = listOf(
        MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
    )

    fun list(context: Context): List<DownloadedFile> {
        val out = ArrayList<DownloadedFile>()
        val projection = arrayOf(
            MediaStore.MediaColumns._ID,
            MediaStore.MediaColumns.DISPLAY_NAME,
            MediaStore.MediaColumns.MIME_TYPE,
            MediaStore.MediaColumns.SIZE,
            MediaStore.MediaColumns.DATE_ADDED,
        )
        // Match only our album folder, using the storage-model-appropriate column.
        val (selection, args) = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            "${MediaStore.MediaColumns.RELATIVE_PATH} LIKE ?" to arrayOf("%${MediaExporter.ALBUM}%")
        } else {
            @Suppress("DEPRECATION")
            val dataCol = MediaStore.MediaColumns.DATA
            "$dataCol LIKE ?" to arrayOf("%${MediaExporter.ALBUM}%")
        }
        for (collection in collections) {
            runCatching {
                context.contentResolver.query(collection, projection, selection, args, null)?.use { c ->
                    val idCol = c.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
                    val nameCol = c.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME)
                    val mimeCol = c.getColumnIndexOrThrow(MediaStore.MediaColumns.MIME_TYPE)
                    val sizeCol = c.getColumnIndexOrThrow(MediaStore.MediaColumns.SIZE)
                    val dateCol = c.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_ADDED)
                    while (c.moveToNext()) {
                        val id = c.getLong(idCol)
                        out += DownloadedFile(
                            uri = android.content.ContentUris.withAppendedId(collection, id),
                            name = c.getString(nameCol) ?: "(unknown)",
                            mime = c.getString(mimeCol) ?: "application/octet-stream",
                            sizeBytes = c.getLong(sizeCol),
                            dateAddedSec = c.getLong(dateCol),
                        )
                    }
                }
            }
        }
        return out.sortedByDescending { it.dateAddedSec }
    }

    /** Intent that opens a downloaded item in the device's native player/opener for its type. */
    fun openIntent(item: DownloadedFile): Intent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(item.uri, item.mime)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
}
