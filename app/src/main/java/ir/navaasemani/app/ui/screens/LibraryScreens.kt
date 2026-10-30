package ir.navaasemani.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.PersonAdd
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import ir.navaasemani.app.ui.CoverArt
import ir.navaasemani.app.ui.EmptyState
import ir.navaasemani.app.ui.LocalActions
import ir.navaasemani.app.ui.LocalSnapshot
import ir.navaasemani.app.ui.MaddahAvatar
import ir.navaasemani.app.ui.Pill
import ir.navaasemani.app.ui.TrackRow
import ir.navaasemani.app.ui.theme.Danger
import ir.navaasemani.app.ui.theme.Gold
import ir.navaasemani.app.ui.theme.Muted
import ir.navaasemani.app.ui.theme.Surface3
import ir.navaasemani.app.ui.theme.Turquoise

@Composable
fun LibraryScreen(contentPadding: PaddingValues, onPlaylist: (String) -> Unit) {
    val snap = LocalSnapshot.current
    val actions = LocalActions.current
    val catalog = snap.catalog
    var tab by rememberSaveable { mutableIntStateOf(0) }
    val tabs = listOf("علاقه‌مندی‌ها", "فهرست‌ها", "مداحان من", "اخیراً")
    LazyColumn(Modifier.fillMaxSize(), contentPadding = contentPadding) {
        item {
            Text("کتابخانه من", style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.statusBarsPadding().padding(start = 20.dp, top = 14.dp))
        }
        item {
            LazyRow(contentPadding = PaddingValues(16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(tabs.size) { i -> Pill(tabs[i], selected = tab == i, onClick = { tab = i }) }
            }
        }
        when (tab) {
            0 -> {
                val tracks = snap.library.favorites.mapNotNull { catalog.track(it) }
                if (tracks.isEmpty()) item {
                    EmptyState(Icons.Rounded.FavoriteBorder, "هنوز علاقه‌مندی ندارید", "از منوی ⋮ یا دکمه قلب در پخش‌کننده، آثار محبوبتان را اینجا جمع کنید.")
                }
                items(tracks, key = { "fav" + it.id }) { TrackRow(it, tracks) }
            }
            1 -> {
                val lists = snap.library.playlists.entries.toList()
                if (lists.isEmpty()) item {
                    EmptyState(Icons.AutoMirrored.Rounded.QueueMusic, "فهرست پخشی نساخته‌اید", "از منوی ⋮ هر اثر «افزودن به فهرست پخش» را بزنید و یک نام بنویسید.")
                }
                items(lists, key = { "pl" + it.key }) { (name, ids) ->
                    Row(
                        Modifier.fillMaxWidth().clickable { onPlaylist(name) }.padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CoverArt("pl-$name", Modifier.size(58.dp), shape = RoundedCornerShape(12.dp), label = name.take(2), labelSize = 16)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(name, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text("${ids.size} اثر", style = MaterialTheme.typography.bodySmall, color = Muted)
                        }
                    }
                }
            }
            2 -> {
                val maddahs = snap.library.following.mapNotNull { catalog.maddah(it) }
                if (maddahs.isEmpty()) item {
                    EmptyState(Icons.Rounded.PersonAdd, "مداحی را دنبال نکرده‌اید", "در صفحه هر مداح «دنبال‌کردن» را بزنید تا آثارش در خانه برایتان بیاید.")
                }
                items(maddahs, key = { "m" + it.id }) { m ->
                    Row(
                        Modifier.fillMaxWidth().clickable { actions.openMaddah(m.id) }.padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        MaddahAvatar(m, 58.dp)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(m.displayName, style = MaterialTheme.typography.titleSmall)
                            Text("${catalog.tracksOf(m.id).size} اثر", style = MaterialTheme.typography.bodySmall, color = Muted)
                        }
                    }
                }
            }
            else -> {
                val tracks = snap.library.recent.mapNotNull { catalog.track(it) }
                if (tracks.isEmpty()) item {
                    EmptyState(Icons.Rounded.History, "تاریخچه خالی است", "آثاری که می‌شنوید اینجا ثبت می‌شوند.")
                }
                items(tracks, key = { "rc" + it.id }) { TrackRow(it, tracks) }
            }
        }
    }
}

@Composable
fun DownloadsScreen(contentPadding: PaddingValues) {
    val snap = LocalSnapshot.current
    val actions = LocalActions.current
    val catalog = snap.catalog
    val entries = snap.downloads.entries.mapNotNull { (id, info) -> catalog.track(id)?.let { it to info } }
    val done = entries.filter { it.second.done }
    val active = entries.filter { !it.second.done }
    val totalBytes = done.sumOf { it.second.total.coerceAtLeast(0) }
    val doneTracks = done.map { it.first }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = contentPadding) {
        item {
            Column(Modifier.statusBarsPadding().padding(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 8.dp)) {
                Text("دانلودها", style = MaterialTheme.typography.headlineMedium)
                Text(
                    "${done.size} فایل · ${"%.1f".format(totalBytes / 1_048_576.0)} مگابایت · پوشه Music/NavaAsemani",
                    style = MaterialTheme.typography.bodySmall, color = Muted
                )
            }
        }
        if (entries.isEmpty()) item {
            EmptyState(Icons.Rounded.Download, "هنوز چیزی دانلود نکرده‌اید",
                "آثاری که ناشر لینک دانلود مستقیم داده با دکمه دانلود در پخش‌کننده یا منوی ⋮ ذخیره می‌شوند و بدون اینترنت پخش می‌شوند.")
        }
        if (active.isNotEmpty()) {
            item { Text("در حال دانلود", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(start = 20.dp, top = 8.dp)) }
            items(active, key = { "a" + it.first.id }) { (track, info) ->
                Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(track.title, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(
                                if (info.failed) "خطا در دانلود؛ دوباره تلاش کنید" else "${(info.progress * 100).toInt()}٪",
                                style = MaterialTheme.typography.bodySmall, color = if (info.failed) Danger else Muted
                            )
                        }
                        if (info.failed) IconButton(onClick = { actions.removeDownload(track.id); actions.download(track) }) {
                            Icon(Icons.Rounded.Refresh, "تلاش دوباره", tint = Gold)
                        }
                        IconButton(onClick = { actions.removeDownload(track.id) }) { Icon(Icons.Rounded.Close, "لغو", tint = Muted) }
                    }
                    Spacer(Modifier.height(6.dp))
                    LinearProgressIndicator(
                        progress = { info.progress }, modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(4.dp)),
                        color = Turquoise, trackColor = Surface3
                    )
                }
            }
        }
        if (done.isNotEmpty()) {
            item { Text("آماده پخش بدون اینترنت", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(start = 20.dp, top = 12.dp, bottom = 4.dp)) }
            items(doneTracks, key = { "d" + it.id }) { t ->
                TrackRow(t, doneTracks, trailing = {
                    IconButton(onClick = { actions.removeDownload(t.id) }) { Icon(Icons.Rounded.Delete, "حذف", tint = Muted) }
                })
            }
        }
    }
}
