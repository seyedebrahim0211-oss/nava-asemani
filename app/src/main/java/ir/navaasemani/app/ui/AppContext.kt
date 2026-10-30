package ir.navaasemani.app.ui

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.staticCompositionLocalOf
import ir.navaasemani.app.PlayerUi
import ir.navaasemani.app.data.CatalogData
import ir.navaasemani.app.data.DownloadInfo
import ir.navaasemani.app.data.LibraryState
import ir.navaasemani.app.data.Track

/** وضعیت لحظه‌ای که همه صفحه‌ها می‌خوانند */
data class AppSnapshot(
    val catalog: CatalogData = CatalogData.EMPTY,
    val library: LibraryState = LibraryState(),
    val downloads: Map<String, DownloadInfo> = emptyMap(),
    val player: PlayerUi = PlayerUi(),
    val refreshing: Boolean = false
) {
    val currentTrack: Track? get() = catalog.track(player.currentId)
}

/** کارهایی که از هر جای رابط کاربری قابل انجام است */
class NavaActions(
    val play: (Track, List<Track>) -> Unit,
    val openMaddah: (String) -> Unit,
    val openCollection: (String) -> Unit,
    val openPlayer: () -> Unit,
    val toggleFavorite: (String) -> Unit,
    val download: (Track) -> Unit,
    val removeDownload: (String) -> Unit,
    val askPlaylist: (Track) -> Unit,
    val openUrl: (String) -> Unit,
    val share: (Track) -> Unit,
    val toggleFollow: (String) -> Unit
)

val LocalSnapshot = compositionLocalOf { AppSnapshot() }
val LocalActions = staticCompositionLocalOf<NavaActions> { error("NavaActions not provided") }
