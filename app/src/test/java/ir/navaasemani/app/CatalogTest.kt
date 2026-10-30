package ir.navaasemani.app

import ir.navaasemani.app.data.CatalogParser
import ir.navaasemani.app.data.Persian
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class CatalogTest {
    private fun catalogFile(): File =
        listOf(File("../catalog/catalog.json"), File("catalog/catalog.json")).first { it.exists() }

    @Test
    fun repositoryCatalogParses() {
        val data = CatalogParser.parse(catalogFile().readText())
        assertTrue("catalog must contain maddahs", data.maddahs.isNotEmpty())
        assertTrue("catalog must contain tracks", data.tracks.isNotEmpty())
        data.tracks.forEach { t -> assertTrue("unknown maddah ${t.maddahId}", data.maddah(t.maddahId) != null) }
    }

    @Test
    fun legacyFormatStillWorks() {
        val json = """{"version":1,"items":[{"id":"a","title":"t","artist":"x","category":"مداحی",
            "streamUrl":"https://e.org/a.mp3","downloadAllowed":true}]}"""
        val data = CatalogParser.parse(json)
        assertEquals(1, data.tracks.size)
        assertEquals("x", data.maddah(data.tracks[0].maddahId)?.name)
        assertTrue(data.tracks[0].canDownload)
    }

    @Test
    fun insecureLinksAreDropped() {
        val json = """{"maddahs":[{"id":"m","name":"n"}],"tracks":[{"id":"t","maddahId":"m","title":"x",
            "streamUrl":"http://e.org/a.mp3","downloadAllowed":true}]}"""
        val t = CatalogParser.parse(json).tracks.single()
        assertFalse(t.playable)
        assertFalse(t.canDownload)
    }

    @Test
    fun persianSearchNormalization() {
        assertEquals(Persian.normalize("علي كريمي"), Persian.normalize("علی کریمی"))
        assertEquals("نوحه 12", Persian.normalize("نوحه ۱۲"))
        assertEquals("می شود", Persian.normalize("می‌شود"))
    }
}
