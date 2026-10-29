package ir.navaasemani.app.data

import android.content.Context
import ir.navaasemani.app.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

val CATEGORIES = listOf("مداحی", "روضه", "سخنرانی", "ادعیه", "مناجات", "قرآن")

data class AudioItem(
    val id: String,
    val title: String,
    val artist: String,
    val category: String,
    val occasion: String = "",
    val description: String = "",
    val streamUrl: String = "",
    val sourceUrl: String = "",
    val sourceName: String = "",
    val license: String = "",
    val downloadAllowed: Boolean = false,
    val duration: String = "",
    val tags: String = "",
    val isFeatured: Boolean = false,
    val isPopular: Boolean = false
) {
    val playable get() = streamUrl.startsWith("https://")
}

object Catalog {
    fun parse(json: String): List<AudioItem> {
        val arr = when (val root = org.json.JSONTokener(json).nextValue()) {
            is JSONArray -> root
            is JSONObject -> root.optJSONArray("items") ?: JSONArray()
            else -> JSONArray()
        }
        return buildList {
            val ids = mutableSetOf<String>()
            for (i in 0 until arr.length()) {
                val o = arr.optJSONObject(i) ?: continue
                val id = o.optString("id").trim()
                val category = o.optString("category")
                if (id.isBlank() || !ids.add(id) || category !in CATEGORIES) continue
                val streamUrl = o.optString("streamUrl")
                val sourceUrl = o.optString("sourceUrl")
                add(AudioItem(
                    id = id, title = o.optString("title"), artist = o.optString("artist"),
                    category = category, occasion = o.optString("occasion"),
                    description = o.optString("description"),
                    streamUrl = streamUrl.takeIf { it.startsWith("https://") } ?: "",
                    sourceUrl = sourceUrl.takeIf { it.startsWith("https://") } ?: "",
                    sourceName = o.optString("sourceName"), license = o.optString("license"),
                    downloadAllowed = o.optBoolean("downloadAllowed", false) && streamUrl.startsWith("https://"),
                    duration = o.optString("duration"), tags = o.optString("tags"),
                    isFeatured = o.optBoolean("isFeatured", false),
                    isPopular = o.optBoolean("isPopular", false)
                ))
            }
        }
    }

    fun bundled(context: Context): List<AudioItem> =
        parse(context.assets.open("catalog.json").bufferedReader().use { it.readText() })

    suspend fun refresh(): List<AudioItem>? = withContext(Dispatchers.IO) {
        val endpoint = BuildConfig.CATALOG_URL
        if (!endpoint.startsWith("https://")) return@withContext null
        try {
            val conn = URL(endpoint).openConnection() as HttpURLConnection
            conn.connectTimeout = 10_000
            conn.readTimeout = 15_000
            conn.setRequestProperty("Accept", "application/json")
            try {
                if (conn.responseCode !in 200..299 || conn.contentLengthLong > 2_000_000L) return@withContext null
                val body = conn.inputStream.bufferedReader().use { it.readText().take(2_000_001) }
                if (body.length > 2_000_000) null else parse(body)
            } finally { conn.disconnect() }
        } catch (_: Exception) { null }
    }
}
