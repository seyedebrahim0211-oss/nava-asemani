package ir.navaasemani.app.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

data class LibraryState(
    val favorites: List<String> = emptyList(),
    val playlists: Map<String, List<String>> = emptyMap(),
    val recent: List<String> = emptyList(),
    val following: List<String> = emptyList(),
    val searches: List<String> = emptyList(),
    val positions: Map<String, Long> = emptyMap()
)

/** کتابخانه شخصی کاربر روی همین گوشی (بدون حساب کاربری و بدون ارسال به سرور) */
class LibraryStore(context: Context) {
    private val prefs = context.getSharedPreferences("library-v2", Context.MODE_PRIVATE)
    private val legacy = context.getSharedPreferences("library-v1", Context.MODE_PRIVATE)
    private val _state = MutableStateFlow(load())
    val state: StateFlow<LibraryState> = _state.asStateFlow()

    private fun list(key: String): List<String> = runCatching {
        val arr = JSONArray(prefs.getString(key, "[]"))
        (0 until arr.length()).map { arr.optString(it) }.filter { it.isNotBlank() }
    }.getOrDefault(emptyList())

    private fun load(): LibraryState {
        val playlists = runCatching {
            val o = JSONObject(prefs.getString("playlists", "{}") ?: "{}")
            o.keys().asSequence().associateWith { key ->
                val arr = o.optJSONArray(key) ?: JSONArray()
                (0 until arr.length()).map { arr.optString(it) }.filter { it.isNotBlank() }
            }
        }.getOrDefault(emptyMap())
        val positions = runCatching {
            val o = JSONObject(prefs.getString("positions", "{}") ?: "{}")
            o.keys().asSequence().associateWith { o.optLong(it) }
        }.getOrDefault(emptyMap())
        var favorites = list("favorites")
        // انتقال یک‌باره علاقه‌مندی‌های نسخه قبل
        if (favorites.isEmpty() && !prefs.getBoolean("migrated", false)) {
            favorites = runCatching {
                val arr = JSONArray(legacy.getString("favorites", "[]"))
                (0 until arr.length()).map { arr.optString(it) }.filter { it.isNotBlank() }
            }.getOrDefault(emptyList())
            prefs.edit().putBoolean("migrated", true).putString("favorites", JSONArray(favorites).toString()).apply()
        }
        return LibraryState(favorites, playlists, list("recent"), list("following"), list("searches"), positions)
    }

    private fun update(transform: (LibraryState) -> LibraryState) {
        val next = transform(_state.value)
        _state.value = next
        val playlists = JSONObject().apply { next.playlists.forEach { (k, v) -> put(k, JSONArray(v)) } }
        val positions = JSONObject().apply { next.positions.forEach { (k, v) -> put(k, v) } }
        prefs.edit()
            .putString("favorites", JSONArray(next.favorites).toString())
            .putString("recent", JSONArray(next.recent).toString())
            .putString("following", JSONArray(next.following).toString())
            .putString("searches", JSONArray(next.searches).toString())
            .putString("playlists", playlists.toString())
            .putString("positions", positions.toString())
            .apply()
    }

    fun toggleFavorite(id: String): Boolean {
        var added = false
        update { s ->
            added = id !in s.favorites
            s.copy(favorites = if (added) listOf(id) + s.favorites else s.favorites - id)
        }
        return added
    }

    fun toggleFollow(maddahId: String): Boolean {
        var added = false
        update { s ->
            added = maddahId !in s.following
            s.copy(following = if (added) listOf(maddahId) + s.following else s.following - maddahId)
        }
        return added
    }

    fun addRecent(id: String) = update { s -> s.copy(recent = (listOf(id) + (s.recent - id)).take(40)) }

    fun addSearch(query: String) {
        val q = query.trim()
        if (q.length < 2) return
        update { s -> s.copy(searches = (listOf(q) + (s.searches - q)).take(12)) }
    }

    fun clearSearches() = update { it.copy(searches = emptyList()) }

    fun addToPlaylist(name: String, id: String) {
        val clean = name.trim().take(48)
        if (clean.isEmpty()) return
        update { s ->
            val current = s.playlists[clean].orEmpty()
            s.copy(playlists = s.playlists + (clean to if (id in current) current else current + id))
        }
    }

    fun removeFromPlaylist(name: String, id: String) = update { s ->
        s.copy(playlists = s.playlists + (name to (s.playlists[name].orEmpty() - id)))
    }

    fun deletePlaylist(name: String) = update { s -> s.copy(playlists = s.playlists - name) }

    fun savePosition(id: String, positionMs: Long) = update { s ->
        val next = if (positionMs <= 5_000) s.positions - id else s.positions + (id to positionMs)
        s.copy(positions = next.entries.toList().takeLast(60).associate { it.key to it.value })
    }
}
