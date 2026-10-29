package ir.navaasemani.app.data

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import org.json.JSONObject

class Downloads(private val context: Context) {
    private val manager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
    private val prefs = context.getSharedPreferences("downloads-v1", Context.MODE_PRIVATE)
    private fun idFor(item: AudioItem): Long = prefs.getLong(item.id, -1L)

    fun status(item: AudioItem): Int? {
        val id = idFor(item)
        if (id < 0) return null
        return try {
            manager.query(DownloadManager.Query().setFilterById(id))?.use { cursor ->
                if (cursor.moveToFirst()) cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS)) else null
            }
        } catch (_: Exception) { null }
    }

    fun downloadedUri(item: AudioItem): Uri? {
        val id = idFor(item)
        return if (id >= 0 && status(item) == DownloadManager.STATUS_SUCCESSFUL) manager.getUriForDownloadedFile(id) else null
    }

    fun request(item: AudioItem): Boolean {
        if (!item.downloadAllowed || !item.playable) return false
        if (status(item) in listOf(DownloadManager.STATUS_PENDING, DownloadManager.STATUS_RUNNING, DownloadManager.STATUS_PAUSED, DownloadManager.STATUS_SUCCESSFUL)) return true
        return try {
            val ext = Uri.parse(item.streamUrl).lastPathSegment.orEmpty().substringAfterLast('.', "mp3").lowercase()
                .takeIf { it in listOf("mp3", "m4a", "ogg", "opus", "wav", "aac") } ?: "ogg"
            val filename = item.id.replace(Regex("[^a-zA-Z0-9_-]"), "_") + ".$ext"
            val req = DownloadManager.Request(Uri.parse(item.streamUrl))
                .setTitle(item.title).setDescription(item.artist)
                .setMimeType(if (ext == "ogg") "audio/ogg" else "audio/mpeg")
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                .setAllowedOverMetered(true)
                .setDestinationInExternalPublicDir(Environment.DIRECTORY_MUSIC, "NavaAsemani/$filename")
            val id = manager.enqueue(req)
            prefs.edit().putLong(item.id, id).apply()
            true
        } catch (_: Exception) { false }
    }
}
