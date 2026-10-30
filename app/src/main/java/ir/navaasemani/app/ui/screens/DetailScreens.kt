package ir.navaasemani.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.PersonAdd
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.ButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import ir.navaasemani.app.data.Track
import ir.navaasemani.app.ui.CoverArt
import ir.navaasemani.app.ui.EmptyState
import ir.navaasemani.app.ui.GoldButton
import ir.navaasemani.app.ui.LocalActions
import ir.navaasemani.app.ui.LocalSnapshot
import ir.navaasemani.app.ui.MaddahAvatar
import ir.navaasemani.app.ui.MihrabPattern
import ir.navaasemani.app.ui.Pill
import ir.navaasemani.app.ui.TrackRow
import ir.navaasemani.app.ui.linkIcon
import ir.navaasemani.app.ui.linkLabel
import ir.navaasemani.app.ui.theme.Cream
import ir.navaasemani.app.ui.theme.Gold
import ir.navaasemani.app.ui.theme.Muted
import ir.navaasemani.app.ui.theme.Night
import ir.navaasemani.app.ui.theme.paletteFor

@Composable
fun BackButton(onBack: () -> Unit, modifier: Modifier = Modifier) {
    IconButton(
        onClick = onBack,
        modifier = modifier.statusBarsPadding().padding(8.dp).size(42.dp).clip(CircleShape).background(Night.copy(alpha = 0.45f))
    ) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "بازگشت", tint = Cream) }
}

@Composable
private fun PlayControls(tracks: List<Track>, extra: @Composable () -> Unit = {}) {
    val actions = LocalActions.current
    val playable = tracks.filter { it.playable }
    Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        GoldButton("پخش همه", Icons.Rounded.PlayArrow, onClick = {
            val first = playable.firstOrNull() ?: tracks.firstOrNull()
            if (first != null) actions.play(first, tracks)
        })
        Spacer(Modifier.width(10.dp))
        if (playable.size > 1) {
            IconButton(onClick = { val pick = playable.random(); actions.play(pick, playable.shuffled()) }) {
                Icon(Icons.Rounded.Shuffle, "پخش تصادفی", tint = Gold)
            }
        }
        Spacer(Modifier.weight(1f))
        extra()
    }
}

@Composable
fun MaddahScreen(maddahId: String, contentPadding: PaddingValues, onBack: () -> Unit) {
    val snap = LocalSnapshot.current
    val actions = LocalActions.current
    val maddah = snap.catalog.maddah(maddahId)
    if (maddah == null) {
        Box(Modifier.fillMaxSize()) {
            EmptyState(Icons.Rounded.LibraryMusic, "مداح پیدا نشد", "ممکن است از آرشیو حذف شده باشد.", Modifier.align(Alignment.Center))
            BackButton(onBack)
        }
        return
    }
    val tracks = snap.catalog.tracksOf(maddah.id)
    val following = maddah.id in snap.library.following
    val (top, _) = paletteFor(maddah.id)
    val occasions = tracks.map { it.occasion }.filter { it.isNotBlank() }.distinct()
    Box(Modifier.fillMaxSize()) {
        LazyColumn(Modifier.fillMaxSize(), contentPadding = contentPadding) {
            item {
                Box(Modifier.fillMaxWidth().background(Brush.verticalGradient(listOf(top, Night)))) {
                    MihrabPattern(Modifier.fillMaxWidth().height(320.dp), alpha = 0.12f)
                    Column(
                        Modifier.fillMaxWidth().statusBarsPadding().padding(top = 56.dp, bottom = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        MaddahAvatar(maddah, 136.dp, ring = true)
                        Spacer(Modifier.height(14.dp))
                        Text(maddah.displayName, style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center)
                        val sub = listOf(maddah.bio, maddah.city).filter { it.isNotBlank() }.joinToString(" · ")
                        if (sub.isNotBlank()) Text(sub, style = MaterialTheme.typography.bodyMedium, color = Muted, textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 32.dp))
                        Spacer(Modifier.height(6.dp))
                        Text("${tracks.size} اثر در آرشیو · ${tracks.count { it.playable }} قابل پخش",
                            style = MaterialTheme.typography.labelMedium, color = Gold)
                    }
                }
            }
            item {
                PlayControls(tracks) {
                    OutlinedButton(
                        onClick = { actions.toggleFollow(maddah.id) },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = if (following) Night else Cream,
                            containerColor = if (following) Gold else Color.Transparent)
                    ) {
                        Icon(if (following) Icons.Rounded.Check else Icons.Rounded.PersonAdd, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(if (following) "دنبال می‌کنید" else "دنبال‌کردن")
                    }
                }
            }
            if (maddah.links.isNotEmpty()) {
                item { Column {
                    Text("کانال‌ها و صفحات رسمی", style = MaterialTheme.typography.titleSmall, color = Muted,
                        modifier = Modifier.padding(start = 20.dp, top = 10.dp, bottom = 8.dp))
                    LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(maddah.links) { link ->
                            Pill(link.label.ifBlank { linkLabel(link.type) }, selected = false,
                                onClick = { actions.openUrl(link.url) }, icon = linkIcon(link.type))
                        }
                    }
                } }
            }
            if (occasions.size > 1) item {
                Text("مناسبت‌ها: " + occasions.joinToString("، "), style = MaterialTheme.typography.bodySmall, color = Muted,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp))
            }
            item {
                Text("آثار", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(start = 20.dp, top = 16.dp, bottom = 6.dp))
            }
            if (tracks.isEmpty()) item {
                EmptyState(Icons.Rounded.LibraryMusic, "هنوز اثری ثبت نشده",
                    "آثار این مداح به‌زودی از کانال‌های رسمی به آرشیو اضافه می‌شود.")
            }
            items(tracks, key = { it.id }) { TrackRow(it, tracks, showArtist = false) }
        }
        BackButton(onBack)
    }
}

@Composable
fun CollectionScreen(collectionId: String, contentPadding: PaddingValues, onBack: () -> Unit) {
    val snap = LocalSnapshot.current
    val collection = snap.catalog.collections.firstOrNull { it.id == collectionId }
    val tracks = collection?.let { snap.catalog.tracksIn(it) }.orEmpty()
    val colors = collection?.colors?.takeIf { it.size >= 2 }?.let { Color(it[0]) to Color(it[1]) } ?: paletteFor(collectionId)
    Box(Modifier.fillMaxSize()) {
        LazyColumn(Modifier.fillMaxSize(), contentPadding = contentPadding) {
            item {
                Box(Modifier.fillMaxWidth().height(300.dp)) {
                    CoverArt(collectionId, Modifier.fillMaxSize(), shape = RoundedCornerShape(0.dp), colors = colors)
                    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Night))))
                    Column(Modifier.align(Alignment.BottomStart).padding(20.dp)) {
                        Text(collection?.title ?: "مجموعه", style = MaterialTheme.typography.headlineLarge)
                        if (!collection?.subtitle.isNullOrBlank()) Text(collection?.subtitle.orEmpty(), color = Cream.copy(alpha = 0.8f))
                        Text("${tracks.size} اثر", color = Gold, style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
            item { PlayControls(tracks) { DownloadAllButton(tracks) } }
            if (tracks.isEmpty()) item { EmptyState(Icons.Rounded.LibraryMusic, "این مجموعه خالی است", "به‌زودی آثار تازه اضافه می‌شود.") }
            items(tracks, key = { it.id }) { TrackRow(it, tracks) }
        }
        BackButton(onBack)
    }
}

@Composable
fun PlaylistScreen(name: String, contentPadding: PaddingValues, onBack: () -> Unit, onDelete: () -> Unit) {
    val snap = LocalSnapshot.current
    val ids = snap.library.playlists[name].orEmpty()
    val tracks = ids.mapNotNull { snap.catalog.track(it) }
    Box(Modifier.fillMaxSize()) {
        LazyColumn(Modifier.fillMaxSize(), contentPadding = contentPadding) {
            item {
                Box(Modifier.fillMaxWidth().height(260.dp)) {
                    CoverArt("pl-$name", Modifier.fillMaxSize(), shape = RoundedCornerShape(0.dp))
                    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Night))))
                    Column(Modifier.align(Alignment.BottomStart).padding(20.dp)) {
                        Text("فهرست پخش", color = Gold, style = MaterialTheme.typography.labelLarge)
                        Text(name, style = MaterialTheme.typography.headlineLarge, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        Text("${tracks.size} اثر", color = Muted)
                    }
                }
            }
            item {
                PlayControls(tracks) {
                    IconButton(onClick = onDelete) { Icon(Icons.Rounded.Delete, "حذف فهرست", tint = Muted) }
                }
            }
            if (tracks.isEmpty()) item {
                EmptyState(Icons.Rounded.LibraryMusic, "فهرست خالی است", "از منوی ⋮ هر اثر، «افزودن به فهرست پخش» را بزنید.")
            }
            items(tracks, key = { it.id }) { TrackRow(it, tracks) }
        }
        BackButton(onBack)
    }
}

@Composable
fun AllMaddahsScreen(contentPadding: PaddingValues, onBack: () -> Unit) {
    val snap = LocalSnapshot.current
    val actions = LocalActions.current
    val maddahs = snap.catalog.maddahs.filter { !it.hidden }.sortedBy { it.name }
    Box(Modifier.fillMaxSize()) {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(104.dp), modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 16.dp, end = 16.dp, top = 0.dp,
                bottom = contentPadding.calculateBottomPadding() + 16.dp
            ),
            horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Text("مداحان", style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.statusBarsPadding().padding(start = 52.dp, top = 14.dp, bottom = 8.dp))
            }
            items(maddahs, key = { it.id }) { m ->
                Column(
                    Modifier.clip(RoundedCornerShape(14.dp)).clickable { actions.openMaddah(m.id) }.padding(4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    MaddahAvatar(m, 92.dp, ring = m.id in snap.library.following)
                    Spacer(Modifier.height(8.dp))
                    Text(m.name, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, maxLines = 2)
                    Text("${snap.catalog.tracksOf(m.id).size} اثر", style = MaterialTheme.typography.labelSmall, color = Muted)
                }
            }
        }
        BackButton(onBack)
    }
}

@Composable
fun DownloadAllButton(tracks: List<Track>) {
    val actions = LocalActions.current
    val snap = LocalSnapshot.current
    val pending = tracks.filter { it.canDownload && snap.downloads[it.id]?.let { d -> d.done || d.active } != true }
    if (pending.isNotEmpty()) {
        IconButton(onClick = { pending.forEach(actions.download) }) { Icon(Icons.Rounded.Download, "دانلود همه", tint = Gold) }
    }
}
