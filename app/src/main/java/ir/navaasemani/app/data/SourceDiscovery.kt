package ir.navaasemani.app.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import org.json.JSONArray
import org.json.JSONObject
import java.net.URI
import java.security.MessageDigest

/**
 * Lightweight metadata-only source index. No third-party audio, lyrics or images are copied.
 * Only links to the publishers' pages are retained; publishing/media rights stay with them.
 * At most two homepage requests every 12 hours (unless user explicitly presses refresh).
 */
object SourceDiscovery {
    private const val CACHE_KEY = "source-index-json"
    private const val TIME_KEY = "source-index-updated"
    private const val INTERVAL_MS = 12 * 60 * 60 * 1000L

    private data class Source(val name: String, val url: String, val host: String, val prefix: String)
    private val sources = listOf(
        Source("کآشوب", "https://kashoob.com/", "kashoob.com", "/audio/"),
        Source("نوا", "https://navaapp.com/", "navaapp.com", "/nava/")
    )

    private fun digest(input: String): String =
        MessageDigest.getInstance("SHA-256").digest(input.toByteArray(Charsets.UTF_8))
            .take(10).joinToString("") { "%02x".format(it.toInt() and 0xff) }

    private fun category(text: String): String = when {
        text.contains("سخنرانی") || text.contains("منبر") -> "سخنرانی"
        text.contains("روضه") -> "روضه"
        text.contains("مناجات") -> "مناجات"
        text.contains("دعای ") || text.contains("زیارت ") -> "ادعیه"
        text.contains("تلاوت") || text.contains("قرائت") || text.contains("سوره ") -> "قرآن"
        else -> "مداحی"
    }

    private fun occasion(text: String): String = listOf(
        "فاطمیه", "محرم", "اربعین", "رمضان", "غدیر", "شب قدر", "صفر",
        "ولادت", "شهادت", "عاشورا", "امام حسین", "امام زمان"
    ).firstOrNull { text.contains(it, ignoreCase = true) }.orEmpty()

    private fun fromHomepage(source: Source): List<AudioItem> {
        val document = Jsoup.connect(source.url)
            .userAgent("NavaAsemani/0.2 (metadata-only, 12h refresh; contact source owner)")
            .timeout(12_000)
            .maxBodySize(900_000)
            .followRedirects(true)
            .get()
        val unique = linkedMapOf<String, AudioItem>()
        for (anchor in document.select("a[href]")) {
            val url = anchor.absUrl("href").substringBefore('#').substringBefore('?')
            val uri = try { URI(url) } catch (_: Exception) { continue }
            if (uri.scheme != "https" || uri.host?.lowercase()?.removePrefix("www.") != source.host) continue
            if (!uri.path.orEmpty().startsWith(source.prefix)) continue
            val title = anchor.text().trim().replace(Regex("\\s+"), " ")
                .ifBlank { anchor.attr("title").trim() }
            if (title.length !in 4..140 || title.contains("مشاهده همه") || title == "بیشتر") continue
            val sourceLabel = "منبع: ${source.name}"
            unique.putIfAbsent(url, AudioItem(
                id = "source-" + digest(url),
                title = title,
                artist = sourceLabel,
                category = category(title),
                occasion = occasion(title),
                description = "اطلاعات معرفی اثر از رسانه ${source.name}؛ پخش و دریافت فایل فقط در صفحه اصلی ناشر.",
                streamUrl = "",
                sourceUrl = url,
                sourceName = source.name,
                license = "پیوند ارجاع به ناشر؛ مجوز بازنشر فایل صوتی دریافت نشده است.",
                downloadAllowed = false,
                tags = "${source.name} تازه‌ها معرفی مداحی"
            ))
            if (unique.size >= 26) break
        }
        return unique.values.toList()
    }

    private fun cacheToJson(items: List<AudioItem>): String = JSONObject().apply {
        put("items", JSONArray().apply {
            items.forEach { item -> put(JSONObject().apply {
                put("id", item.id)
                put("title", item.title)
                put("artist", item.artist)
                put("category", item.category)
                put("occasion", item.occasion)
                put("description", item.description)
                put("sourceUrl", item.sourceUrl)
                put("sourceName", item.sourceName)
                put("license", item.license)
                put("downloadAllowed", false)
                put("tags", item.tags)
            }) }
        })
    }.toString()

    suspend fun load(context: Context, force: Boolean = false): List<AudioItem> =
        withContext(Dispatchers.IO) {
            val prefs = context.getSharedPreferences("source-index-v1", Context.MODE_PRIVATE)
            val cached = runCatching { Catalog.parse(prefs.getString(CACHE_KEY, "") ?: "") }
                .getOrDefault(emptyList())
            val elapsed = System.currentTimeMillis() - prefs.getLong(TIME_KEY, 0L)
            if (!force && cached.isNotEmpty() && elapsed in 0 until INTERVAL_MS) return@withContext cached
            val collected = mutableListOf<AudioItem>()
            for (source in sources) {
                try {
                    collected.addAll(fromHomepage(source))
                } catch (_: Exception) {
                    // Respect publisher blocking, timeouts, and connectivity problems.
                }
            }
            if (collected.isNotEmpty()) {
                val merged = (collected + cached).distinctBy { it.sourceUrl }.take(80)
                prefs.edit().putString(CACHE_KEY, cacheToJson(merged))
                    .putLong(TIME_KEY, System.currentTimeMillis()).apply()
                merged
            } else cached
        }
}
