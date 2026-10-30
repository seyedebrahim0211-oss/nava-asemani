package ir.navaasemani.app.data

import android.content.Context
import ir.navaasemani.app.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/**
 * آرشیو زنده: کاتالوگ از پوشه catalog/ همین مخزن گیت‌هاب خوانده می‌شود
 * (اول jsDelivr که در ایران معمولاً در دسترس‌تر است، بعد raw.githubusercontent).
 * نسخه داخل APK و آخرین نسخه دریافت‌شده هم برای حالت آفلاین نگه داشته می‌شوند.
 */
class CatalogRepository(private val context: Context) {
    private val cacheFile = File(context.filesDir, "catalog-cache.json")

    fun bundled(): CatalogData = runCatching {
        CatalogParser.parse(context.assets.open("catalog.json").bufferedReader().use { it.readText() })
    }.getOrDefault(CatalogData.EMPTY)

    fun cached(): CatalogData? = runCatching {
        if (cacheFile.exists()) CatalogParser.parse(cacheFile.readText()) else null
    }.getOrNull()?.takeIf { it.tracks.isNotEmpty() }

    /** بهترین نسخه موجود بدون اینترنت */
    fun local(): CatalogData {
        val bundled = bundled()
        val cached = cached() ?: return bundled
        return if (cached.updated >= bundled.updated) cached else bundled
    }

    val hasRemote: Boolean get() = remoteUrls().isNotEmpty()

    private fun remoteUrls(): List<String> =
        BuildConfig.CATALOG_URLS.split(',').map { it.trim() }.filter { it.startsWith("https://") }

    suspend fun fetchRemote(): CatalogData? = withContext(Dispatchers.IO) {
        for (endpoint in remoteUrls()) {
            val body = runCatching { download(endpoint) }.getOrNull() ?: continue
            val parsed = runCatching { CatalogParser.parse(body) }.getOrNull() ?: continue
            if (parsed.tracks.isEmpty()) continue
            runCatching { cacheFile.writeText(body) }
            return@withContext parsed
        }
        null
    }

    private fun download(endpoint: String): String? {
        val separator = if (endpoint.contains('?')) '&' else '?'
        // پارامتر زمان برای دورزدن کش طولانی CDN
        val url = URL("$endpoint${separator}t=${System.currentTimeMillis() / 600_000}")
        val conn = url.openConnection() as HttpURLConnection
        conn.connectTimeout = 10_000
        conn.readTimeout = 20_000
        conn.setRequestProperty("Accept", "application/json")
        conn.setRequestProperty("User-Agent", "NavaAsemani/${BuildConfig.VERSION_NAME}")
        return try {
            if (conn.responseCode !in 200..299) return null
            val text = conn.inputStream.bufferedReader().use { it.readText() }
            if (text.length > 8_000_000) null else text
        } finally {
            conn.disconnect()
        }
    }
}
