package ir.navaasemani.app.ui

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import ir.navaasemani.app.AppViewModel
import kotlinx.coroutines.flow.collectLatest
import ir.navaasemani.app.BuildConfig
import ir.navaasemani.app.data.Track
import ir.navaasemani.app.ui.screens.AboutScreen
import ir.navaasemani.app.ui.screens.AllMaddahsScreen
import ir.navaasemani.app.ui.screens.CollectionScreen
import ir.navaasemani.app.ui.screens.DownloadsScreen
import ir.navaasemani.app.ui.screens.HomeScreen
import ir.navaasemani.app.ui.screens.LibraryScreen
import ir.navaasemani.app.ui.screens.MaddahScreen
import ir.navaasemani.app.ui.screens.MiniPlayer
import ir.navaasemani.app.ui.screens.PlayerScreen
import ir.navaasemani.app.ui.screens.PlaylistScreen
import ir.navaasemani.app.ui.screens.SearchScreen
import ir.navaasemani.app.ui.theme.Cream
import ir.navaasemani.app.ui.theme.Gold
import ir.navaasemani.app.ui.theme.Muted
import ir.navaasemani.app.ui.theme.Night
import ir.navaasemani.app.ui.theme.Surface1
import ir.navaasemani.app.ui.theme.Surface3

private enum class Tab(val label: String) { HOME("خانه"), SEARCH("جست‌وجو"), LIBRARY("کتابخانه"), DOWNLOADS("دانلودها") }

/** صفحه‌هایی که روی زبانه‌ها باز می‌شوند و با «بازگشت» بسته می‌شوند */
private sealed interface Page {
    data class MaddahPage(val id: String) : Page
    data class CollectionPage(val id: String) : Page
    data class PlaylistPage(val name: String) : Page
    data object AllMaddahs : Page
    data object About : Page
}

private fun tabIcon(tab: Tab) = when (tab) {
    Tab.HOME -> Icons.Rounded.Home
    Tab.SEARCH -> Icons.Rounded.Search
    Tab.LIBRARY -> Icons.Rounded.LibraryMusic
    Tab.DOWNLOADS -> Icons.Rounded.Download
}

@Composable
fun NavaApp(vm: AppViewModel) {
    val context = LocalContext.current
    val catalog by vm.catalog.collectAsState()
    val library by vm.library.state.collectAsState()
    val downloads by vm.downloadStates.collectAsState()
    val player by vm.player.collectAsState()
    val refreshing by vm.refreshing.collectAsState()
    val snackbar = remember { SnackbarHostState() }

    var tab by remember { mutableStateOf(Tab.HOME) }
    val pages = remember { mutableStateListOf<Page>() }
    var playerOpen by remember { mutableStateOf(false) }
    var searchCategory by remember { mutableStateOf<String?>(null) }
    var playlistTarget by remember { mutableStateOf<Track?>(null) }

    LaunchedEffect(Unit) { vm.messages.collectLatest { snackbar.showSnackbar(it) } }

    fun pop() {
        if (pages.isNotEmpty()) pages.removeAt(pages.lastIndex)
    }

    fun openUrl(url: String) {
        if (!url.startsWith("https://") && !url.startsWith("http://")) return
        try {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (_: ActivityNotFoundException) {
            vm.toast("برنامه‌ای برای باز کردن این پیوند پیدا نشد")
        }
    }

    val actions = remember {
        NavaActions(
            play = { track, queue ->
                if (vm.play(track, queue)) {
                    // در حال پخش در خود اپ
                } else if (track.sourceUrl.isNotBlank()) {
                    vm.toast("این اثر در صفحه رسمی ${track.sourceName.ifBlank { "ناشر" }} پخش می‌شود")
                    openUrl(track.sourceUrl)
                } else {
                    vm.toast("برای این اثر هنوز لینک پخش ثبت نشده است")
                }
            },
            openMaddah = { id -> playerOpen = false; pages.add(Page.MaddahPage(id)) },
            openCollection = { id -> pages.add(Page.CollectionPage(id)) },
            openPlayer = { playerOpen = true },
            toggleFavorite = vm::toggleFavorite,
            download = vm::download,
            removeDownload = vm::removeDownload,
            askPlaylist = { playlistTarget = it },
            openUrl = { openUrl(it) },
            share = { track ->
                val artist = vm.catalog.value.artistLine(track)
                val link = track.sourceUrl.ifBlank { track.streamUrl }
                val text = buildString {
                    append("«${track.title}» با نوای $artist")
                    if (link.isNotBlank()) append("\n").append(link)
                    if (BuildConfig.REPO_URL.isNotBlank()) append("\n\nبشنوید در اپ نوای آسمانی:\n${BuildConfig.REPO_URL}/releases/latest")
                }
                val send = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, text)
                context.startActivity(Intent.createChooser(send, "اشتراک‌گذاری").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            },
            toggleFollow = vm::toggleFollow
        )
    }

    fun goBack() {
        if (playerOpen) {
            playerOpen = false
        } else if (pages.isNotEmpty()) {
            pop()
        } else {
            tab = Tab.HOME
        }
    }

    BackHandler(enabled = playerOpen || pages.isNotEmpty() || tab != Tab.HOME) { goBack() }

    val snapshot = AppSnapshot(catalog, library, downloads, player, refreshing)
    CompositionLocalProvider(LocalSnapshot provides snapshot, LocalActions provides actions) {
        BoxWithConstraints(Modifier.fillMaxSize().background(Night)) {
            val wide = maxWidth >= 720.dp
            val current = snapshot.currentTrack
            Row(Modifier.fillMaxSize()) {
                if (wide) {
                    NavigationRail(containerColor = Surface1, modifier = Modifier.statusBarsPadding()) {
                        Tab.entries.forEach { t ->
                            NavigationRailItem(
                                selected = tab == t && pages.isEmpty(),
                                onClick = { tab = t; pages.clear() },
                                icon = { Icon(tabIcon(t), t.label) }, label = { Text(t.label) },
                                colors = NavigationRailItemDefaults.colors(
                                    selectedIconColor = Night, indicatorColor = Gold,
                                    selectedTextColor = Gold, unselectedIconColor = Muted, unselectedTextColor = Muted
                                )
                            )
                        }
                    }
                }
                Box(Modifier.weight(1f).fillMaxSize()) {
                    // فضای پایین برای مینی‌پلیر و نوار ناوبری
                    val bottomSpace = (if (current != null) 76.dp else 0.dp) + (if (!wide) 80.dp else 16.dp)
                    val padding = PaddingValues(bottom = bottomSpace)
                    val top = pages.lastOrNull()
                    AnimatedContent(
                        targetState = top ?: tab,
                        transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(160)) },
                        label = "page"
                    ) { target ->
                        when (target) {
                            is Page.MaddahPage -> MaddahScreen(target.id, padding, onBack = { pop() })
                            is Page.CollectionPage -> CollectionScreen(target.id, padding, onBack = { pop() })
                            is Page.PlaylistPage -> PlaylistScreen(target.name, padding,
                                onBack = { pop() },
                                onDelete = { vm.library.deletePlaylist(target.name); pop() })
                            Page.AllMaddahs -> AllMaddahsScreen(padding, onBack = { pop() })
                            Page.About -> AboutScreen(padding, onBack = { pop() }, onRefresh = { vm.refresh() })
                            Tab.HOME -> HomeScreen(
                                padding,
                                onSearch = { searchCategory = null; tab = Tab.SEARCH },
                                onAbout = { pages.add(Page.About) },
                                onAllMaddahs = { pages.add(Page.AllMaddahs) },
                                onCategory = { searchCategory = it; tab = Tab.SEARCH },
                                onRefresh = { vm.refresh() }
                            )
                            Tab.SEARCH -> SearchScreen(padding, searchCategory,
                                onSubmit = { vm.library.addSearch(it) }, onClearHistory = { vm.library.clearSearches() })
                            Tab.LIBRARY -> LibraryScreen(padding, onPlaylist = { pages.add(Page.PlaylistPage(it)) })
                            Tab.DOWNLOADS -> DownloadsScreen(padding)
                            else -> Unit
                        }
                    }

                    Column(Modifier.align(Alignment.BottomCenter).fillMaxWidth()) {
                        SnackbarHost(snackbar) { data ->
                            Snackbar(data, containerColor = Surface3, contentColor = Cream, shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.padding(horizontal = 12.dp))
                        }
                        if (current != null) MiniPlayer(current, vm, onOpen = { playerOpen = true })
                        if (!wide) {
                            NavigationBar(containerColor = Surface1.copy(alpha = 0.97f), tonalElevation = 0.dp) {
                                Tab.entries.forEach { t ->
                                    NavigationBarItem(
                                        selected = tab == t && pages.isEmpty(),
                                        onClick = { tab = t; pages.clear() },
                                        icon = { Icon(tabIcon(t), t.label) },
                                        label = { Text(t.label, style = MaterialTheme.typography.labelSmall) },
                                        colors = NavigationBarItemDefaults.colors(
                                            selectedIconColor = Night, indicatorColor = Gold, selectedTextColor = Gold,
                                            unselectedIconColor = Muted, unselectedTextColor = Muted
                                        )
                                    )
                                }
                            }
                        } else {
                            Box(Modifier.navigationBarsPadding())
                        }
                    }
                }
            }

            AnimatedVisibility(
                visible = playerOpen && current != null,
                enter = slideInVertically(tween(320)) { it },
                exit = slideOutVertically(tween(260)) { it }
            ) {
                PlayerScreen(vm, onClose = { playerOpen = false })
            }
        }

        playlistTarget?.let { target ->
            var name by remember(target.id) { mutableStateOf("") }
            AlertDialog(
                onDismissRequest = { playlistTarget = null },
                containerColor = Surface1,
                title = { Text("افزودن به فهرست پخش") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        library.playlists.keys.forEach { existing ->
                            TextButton(onClick = { vm.addToPlaylist(existing, target.id); playlistTarget = null }) {
                                Text("＋ $existing", color = Cream)
                            }
                        }
                        OutlinedTextField(name, { name = it }, singleLine = true, label = { Text("نام فهرست تازه") },
                            modifier = Modifier.fillMaxWidth())
                    }
                },
                confirmButton = {
                    TextButton(onClick = { vm.addToPlaylist(name, target.id); playlistTarget = null }, enabled = name.isNotBlank()) {
                        Text("ساختن و افزودن", color = Gold)
                    }
                },
                dismissButton = { TextButton(onClick = { playlistTarget = null }) { Text("انصراف", color = Muted) } }
            )
        }
    }
}
