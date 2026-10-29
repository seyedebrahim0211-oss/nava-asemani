package ir.navaasemani.app.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

class LibraryPrefs(context: Context) {
    private val prefs = context.getSharedPreferences("library-v1", Context.MODE_PRIVATE)
    private fun values(key: String): Set<String> = try {
        val arr = JSONArray(prefs.getString(key, "[]"))
        buildSet { for (i in 0 until arr.length()) arr.optString(i).takeIf { it.isNotBlank() }?.let(::add) }
    } catch (_: Exception) { emptySet() }
    private fun save(key: String, ids: Set<String>) {
        prefs.edit().putString(key, JSONArray(ids.toList()).toString()).apply()
    }
    fun favorites(): Set<String> = values("favorites")
    fun toggle(id: String): Set<String> = favorites().toMutableSet().also {
        if (!it.add(id)) it.remove(id)
        save("favorites", it)
    }
    fun playlistNames(): List<String> = try {
        val arr = JSONArray(prefs.getString("names", "[]"))
        (0 until arr.length()).map { arr.optString(it) }.filter { it.isNotBlank() }
    } catch (_: Exception) { emptyList() }
    fun playlist(name: String): Set<String> = values("playlist:$name")
    fun addToPlaylist(name: String, id: String) {
        val clean = name.trim().take(48)
        if (clean.isEmpty()) return
        val all = playlistNames().toMutableSet().apply { add(clean) }
        prefs.edit().putString("names", JSONArray(all.toList()).toString()).apply()
        save("playlist:$clean", playlist(clean) + id)
    }
    fun removeFromPlaylist(name: String, id: String) = save("playlist:$name", playlist(name) - id)
}
