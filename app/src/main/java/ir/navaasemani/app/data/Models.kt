package ir.navaasemani.app.data

import org.json.JSONArray
import org.json.JSONObject
import org.json.JSONTokener
import java.security.MessageDigest

/** پیوند رسمی مداح: سایت، اینستاگرام، تلگرام، یوتیوب، آپارات، ایتا، روبیکا، پادکست */
data class SocialLink(val type: String, val url: String, val label: String)

data class Maddah(
    val id: String,
    val name: String,
    val honorific: String = "",
    val bio: String = "",
    val city: String = "",
    val photo: String = "",
    val featured: Boolean = false,
    val hidden: Boolean = false,
    val links: List<SocialLink> = emptyList()
) {
    val displayName: String get() = if (honorific.isBlank()) name else "$honorific $name"
}

data class Track(
    val id: String,
    val maddahId: String,
    val title: String,
    val category: String,
    val occasion: String = "",
    val year: String = "",
    val duration: String = "",
    val streamUrl: String = "",
    val downloadUrl: String = "",
    val artwork: String = "",
    val sourceUrl: String = "",
    val sourceName: String = "",
    val license: String = "",
    val downloadAllowed: Boolean = false,
    val tags: String = "",
    val featured: Boolean = false,
    val popular: Boolean = false,
    val addedAt: String = "",
    val featuring: String = ""
) {
    /** فقط لینک مستقیم HTTPS قابل پخش در خود اپ است؛ بقیه به صفحه ناشر می‌روند. */
    val playable: Boolean get() = streamUrl.startsWith("https://")
    val fileUrl: String get() = downloadUrl.ifBlank { streamUrl }
    val canDownload: Boolean get() = downloadAllowed && fileUrl.startsWith("https://")
}

data class TrackCollection(
    val id: String,
    val title: String,
    val subtitle: String = "",
    val occasion: String = "",
    val category: String = "",
    val colors: List<Long> = emptyList()
)

data class CatalogData(
    val version: Int,
    val updated: String,
    val maddahs: List<Maddah>,
    val tracks: List<Track>,
    val collections: List<TrackCollection>
) {
    private val byId: Map<String, Maddah> = maddahs.associateBy { it.id }
    private val trackById: Map<String, Track> = tracks.associateBy { it.id }

    fun maddah(id: String): Maddah? = byId[id]
    fun track(id: String): Track? = trackById[id]
    fun tracksOf(maddahId: String): List<Track> = tracks.filter { it.maddahId == maddahId }
    fun artistLine(track: Track): String {
        val main = maddah(track.maddahId)?.displayName ?: ""
        return if (track.featuring.isBlank()) main else "$main و ${track.featuring}"
    }
    fun tracksIn(collection: TrackCollection): List<Track> = tracks.filter { t ->
        (collection.occasion.isBlank() || t.occasion.contains(collection.occasion)) &&
            (collection.category.isBlank() || t.category == collection.category)
    }
    val categories: List<String>
        get() = (DEFAULT_CATEGORIES + tracks.map { it.category }).filter { it.isNotBlank() }.distinct()

    companion object {
        val DEFAULT_CATEGORIES = listOf("مداحی", "روضه", "نوحه", "سخنرانی", "ادعیه", "مناجات", "قرآن")
        val EMPTY = CatalogData(0, "", emptyList(), emptyList(), emptyList())
    }
}

object CatalogParser {
    private fun JSONObject.str(key: String): String = optString(key, "").let { if (it == "null") "" else it.trim() }
    private fun https(value: String): String = value.takeIf { it.startsWith("https://") } ?: ""

    fun parse(json: String): CatalogData {
        val root = JSONTokener(json).nextValue()
        if (root is JSONArray) return parseLegacy(root, 1, "")
        if (root !is JSONObject) return CatalogData.EMPTY
        if (root.has("tracks") || root.has("maddahs")) return parseV3(root)
        return parseLegacy(root.optJSONArray("items") ?: JSONArray(), root.optInt("version", 1), root.str("updated"))
    }

    private fun parseV3(root: JSONObject): CatalogData {
        val maddahs = mutableListOf<Maddah>()
        val ids = mutableSetOf<String>()
        root.optJSONArray("maddahs")?.let { arr ->
            for (i in 0 until arr.length()) {
                val o = arr.optJSONObject(i) ?: continue
                val id = o.str("id")
                if (id.isBlank() || !ids.add(id)) continue
                val links = mutableListOf<SocialLink>()
                o.optJSONArray("links")?.let { la ->
                    for (j in 0 until la.length()) {
                        val l = la.optJSONObject(j) ?: continue
                        val url = https(l.str("url"))
                        if (url.isNotEmpty()) links.add(SocialLink(l.str("type").ifBlank { "site" }, url, l.str("label")))
                    }
                }
                maddahs.add(Maddah(
                    id = id, name = o.str("name"), honorific = o.str("honorific"), bio = o.str("bio"),
                    city = o.str("city"), photo = https(o.str("photo")), featured = o.optBoolean("featured", false),
                    hidden = o.optBoolean("hidden", false), links = links
                ))
            }
        }
        val tracks = mutableListOf<Track>()
        val trackIds = mutableSetOf<String>()
        root.optJSONArray("tracks")?.let { arr ->
            for (i in 0 until arr.length()) {
                val o = arr.optJSONObject(i) ?: continue
                val id = o.str("id")
                val maddahId = o.str("maddahId")
                if (id.isBlank() || maddahId !in ids || !trackIds.add(id)) continue
                tracks.add(Track(
                    id = id, maddahId = maddahId, title = o.str("title"),
                    category = o.str("category").ifBlank { "مداحی" }, occasion = o.str("occasion"),
                    year = o.str("year"), duration = o.str("duration"),
                    streamUrl = https(o.str("streamUrl")), downloadUrl = https(o.str("downloadUrl")),
                    artwork = https(o.str("artwork")), sourceUrl = https(o.str("sourceUrl")),
                    sourceName = o.str("sourceName"), license = o.str("license"),
                    downloadAllowed = o.optBoolean("downloadAllowed", false), tags = o.str("tags"),
                    featured = o.optBoolean("featured", false), popular = o.optBoolean("popular", false),
                    addedAt = o.str("addedAt"), featuring = o.str("featuring")
                ))
            }
        }
        val collections = mutableListOf<TrackCollection>()
        root.optJSONArray("collections")?.let { arr ->
            for (i in 0 until arr.length()) {
                val o = arr.optJSONObject(i) ?: continue
                val id = o.str("id")
                if (id.isBlank()) continue
                val colors = mutableListOf<Long>()
                o.optJSONArray("colors")?.let { ca -> for (j in 0 until ca.length()) parseColor(ca.optString(j))?.let(colors::add) }
                collections.add(TrackCollection(id, o.str("title"), o.str("subtitle"), o.str("occasion"), o.str("category"), colors))
            }
        }
        return CatalogData(root.optInt("version", 3), root.str("updated"), maddahs, tracks, collections)
    }

    /** سازگاری با catalog.json نسخه‌های قبلی (فهرست تخت items با فیلد artist) */
    private fun parseLegacy(arr: JSONArray, version: Int, updated: String): CatalogData {
        val maddahs = linkedMapOf<String, Maddah>()
        val tracks = mutableListOf<Track>()
        val ids = mutableSetOf<String>()
        for (i in 0 until arr.length()) {
            val o = arr.optJSONObject(i) ?: continue
            val id = o.str("id")
            if (id.isBlank() || !ids.add(id)) continue
            val artist = o.str("artist").ifBlank { "ناشناس" }
            val maddahId = "artist-" + shortHash(artist)
            maddahs.getOrPut(maddahId) { Maddah(maddahId, artist) }
            tracks.add(Track(
                id = id, maddahId = maddahId, title = o.str("title"), category = o.str("category").ifBlank { "مداحی" },
                occasion = o.str("occasion"), duration = o.str("duration"), streamUrl = https(o.str("streamUrl")),
                sourceUrl = https(o.str("sourceUrl")), sourceName = o.str("sourceName"), license = o.str("license"),
                downloadAllowed = o.optBoolean("downloadAllowed", false), tags = o.str("tags"),
                featured = o.optBoolean("isFeatured", false), popular = o.optBoolean("isPopular", false)
            ))
        }
        return CatalogData(version, updated, maddahs.values.toList(), tracks, emptyList())
    }

    fun parseColor(value: String?): Long? {
        val hex = value?.trim()?.removePrefix("#") ?: return null
        return when (hex.length) {
            6 -> hex.toLongOrNull(16)?.let { 0xFF000000L or it }
            8 -> hex.toLongOrNull(16)
            else -> null
        }
    }

    fun shortHash(input: String): String =
        MessageDigest.getInstance("SHA-256").digest(input.toByteArray(Charsets.UTF_8))
            .take(6).joinToString("") { "%02x".format(it.toInt() and 0xff) }
}
