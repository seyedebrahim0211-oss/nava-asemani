package ir.navaasemani.app.data

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment

data class DownloadInfo(val status: Int, val downloaded: Long, val total: Long) {
    val done: Boolean get() = status == DownloadManager.STATUS_SUCCESSFUL
    val active: Boolean get() = status == DownloadManager.STATUS_PENDING ||
        status == DownloadManager.STATUS_RUNNING || status == DownloadManager.STATUS_PAUSED
    val failed: Boolean get() = status == DownloadManager.STATUS_FAILED
    val progress: Float get() = if (total > 0) (downloaded.toFloat() / total).coerceIn(0f, 1f) else 0f
}

/**
 * دانلود مستقیم از لینک رسمی ناشر با DownloadManager اندروید.
 * فایل‌ها در پوشه Music/NavaAsemani ذخیره می‌شوند و در اپ‌های موسیقی دیگر هم دیده می‌شوند.
 */
class Downloads(context: Context) {
    private val manager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
    private val prefs = context.getSharedPreferences("downloads-v1", Context.MODE_PRIVATE)

    fun trackedIds(): Map<String, Long> =
        prefs.all.mapNotNull { (k, v) -> (v as? Long)?.let { k to it } }.toMap()

    fun queryAll(): Map<String, DownloadInfo> {
        val tracked = trackedIds()
        if (tracked.isEmpty()) return emptyMap()
        val byDownloadId = tracked.entries.associate { it.value to it.key }
        val result = mutableMapOf<String, DownloadInfo>()
        runCatching {
            manager.query(DownloadManager.Query().setFilterById(*tracked.values.toLongArray()))?.use { c ->
                val idCol = c.getColumnIndexOrThrow(DownloadManager.COLUMN_ID)
                val statusCol = c.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS)
                val soFarCol = c.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR)
                val totalCol = c.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES)
                while (c.moveToNext()) {
                    val trackId = byDownloadId[c.getLong(idCol)] ?: continue
                    result[trackId] = DownloadInfo(c.getInt(statusCol), c.getLong(soFarCol), c.getLong(totalCol))
                }
            }
        }
        // دانلودهایی که کاربر از بیرون پاک کرده از فهرست حذف شوند
        val missing = tracked.keys - result.keys
        if (missing.isNotEmpty()) prefs.edit().apply { missing.forEach { remove(it) } }.apply()
        return result
    }

    fun localUri(trackId: String): Uri? {
        val id = prefs.getLong(trackId, -1L)
        if (id < 0) return null
        return runCatching {
            val ok = manager.query(DownloadManager.Query().setFilterById(id))?.use { c ->
                c.moveToFirst() && c.getInt(c.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS)) == DownloadManager.STATUS_SUCCESSFUL
            } ?: false
            if (ok) manager.getUriForDownloadedFile(id) else null
        }.getOrNull()
    }

    fun request(track: Track, artist: String): Boolean {
        if (!track.canDownload) return false
        val existing = queryAll()[track.id]
        if (existing != null && (existing.active || existing.done)) return true
        return runCatching {
            val url = track.fileUrl
            val ext = Uri.parse(url).lastPathSegment.orEmpty().substringAfterLast('.', "mp3").lowercase()
                .takeIf { it in listOf("mp3", "m4a", "ogg", "oga", "opus", "wav", "aac") } ?: "mp3"
            val safe = (track.maddahId + "-" + track.id).replace(Regex("[^a-zA-Z0-9_-]"), "_").take(80)
            val mime = when (ext) {
                "ogg", "oga", "opus" -> "audio/ogg"
                "m4a", "aac" -> "audio/mp4"
                "wav" -> "audio/wav"
                else -> "audio/mpeg"
            }
            val req = DownloadManager.Request(Uri.parse(url))
                .setTitle(track.title)
                .setDescription(artist)
                .setMimeType(mime)
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                .setAllowedOverMetered(true)
                .setAllowedOverRoaming(true)
                .setDestinationInExternalPublicDir(Environment.DIRECTORY_MUSIC, "NavaAsemani/$safe.$ext")
            prefs.edit().putLong(track.id, manager.enqueue(req)).apply()
            true
        }.getOrDefault(false)
    }

    /** لغو دانلود در حال انجام یا حذف فایل دانلودشده */
    fun remove(trackId: String) {
        val id = prefs.getLong(trackId, -1L)
        if (id >= 0) runCatching { manager.remove(id) }
        prefs.edit().remove(trackId).apply()
    }
}
