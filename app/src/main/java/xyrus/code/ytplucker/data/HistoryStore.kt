package xyrus.code.ytplucker.data

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.core.content.edit
import xyrus.code.ytplucker.domain.model.DownloadedFile

object HistoryStore {

    private val collections = listOf(
        MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
    )

    private fun dismissedPrefs(context: Context): SharedPreferences =
        context.getSharedPreferences("history_dismissed", Context.MODE_PRIVATE)

    private fun dismissedSet(context: Context): Set<String> =
        dismissedPrefs(context).getStringSet(DISMISSED_KEY, emptySet()) ?: emptySet()

    fun list(context: Context): List<DownloadedFile> {
        val dismissed = dismissedSet(context)
        val out = ArrayList<DownloadedFile>()
        val projection = arrayOf(
            MediaStore.MediaColumns._ID,
            MediaStore.MediaColumns.DISPLAY_NAME,
            MediaStore.MediaColumns.MIME_TYPE,
            MediaStore.MediaColumns.SIZE,
            MediaStore.MediaColumns.DATE_ADDED,
        )
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
                        val uri = android.content.ContentUris.withAppendedId(collection, id)
                        if (uri.toString() in dismissed) continue
                        out += DownloadedFile(
                            uri = uri,
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

    fun dismiss(context: Context, uri: Uri) {
        val prefs = dismissedPrefs(context)
        val current = prefs.getStringSet(DISMISSED_KEY, emptySet())?.toMutableSet() ?: mutableSetOf()
        current.add(uri.toString())
        prefs.edit { putStringSet(DISMISSED_KEY, current) }
    }

    fun dismissAll(context: Context) {
        val prefs = dismissedPrefs(context)
        val allUris = mutableSetOf<String>()
        val projection = arrayOf(MediaStore.MediaColumns._ID)
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
                    while (c.moveToNext()) {
                        allUris.add(android.content.ContentUris.withAppendedId(collection, c.getLong(idCol)).toString())
                    }
                }
            }
        }
        prefs.edit { putStringSet(DISMISSED_KEY, allUris) }
    }

    fun openIntent(item: DownloadedFile): Intent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(item.uri, item.mime)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    private const val DISMISSED_KEY = "dismissed_uris"
}
