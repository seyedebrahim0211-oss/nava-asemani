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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import ir.navaasemani.app.data.Persian
import ir.navaasemani.app.ui.CollectionCard
import ir.navaasemani.app.ui.CoverArt
import ir.navaasemani.app.ui.GoldButton
import ir.navaasemani.app.ui.LocalActions
import ir.navaasemani.app.ui.LocalSnapshot
import ir.navaasemani.app.ui.MaddahAvatar
import ir.navaasemani.app.ui.MihrabPattern
import ir.navaasemani.app.ui.SectionHeader
import ir.navaasemani.app.ui.TrackCard
import ir.navaasemani.app.ui.TrackRow
import ir.navaasemani.app.ui.theme.Cream
import ir.navaasemani.app.ui.theme.Gold
import ir.navaasemani.app.ui.theme.GoldGradient
import ir.navaasemani.app.ui.theme.Muted
import ir.navaasemani.app.ui.theme.Night
import ir.navaasemani.app.ui.theme.Surface2
import ir.navaasemani.app.ui.theme.paletteFor
import java.util.Calendar

@Composable
fun HomeScreen(
    contentPadding: PaddingValues,
    onSearch: () -> Unit,
    onAbout: () -> Unit,
    onAllMaddahs: () -> Unit,
    onCategory: (String) -> Unit,
    onRefresh: () -> Unit
) {
    val snap = LocalSnapshot.current
    val actions = LocalActions.current
    val catalog = snap.catalog
    val hijri = remember { Persian.hijriToday() }
    val greeting = remember { Persian.greeting(Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) }
    val todayCollection = catalog.collections.firstOrNull { it.id == Persian.occasionCollection(hijri) }
        ?: catalog.collections.firstOrNull()
    val maddahs = catalog.maddahs.filter { !it.hidden }
        .sortedWith(compareByDescending<ir.navaasemani.app.data.Maddah> { it.id in snap.library.following }
            .thenByDescending { it.featured }
            .thenByDescending { catalog.tracksOf(it.id).size })
    val fresh = catalog.tracks.sortedByDescending { it.addedAt }.take(14)
    val popular = catalog.tracks.filter { it.popular }.ifEmpty { catalog.tracks.take(8) }
    val recent = snap.library.recent.mapNotNull { catalog.track(it) }.take(12)
    val followed = catalog.tracks.filter { it.maddahId in snap.library.following }.take(14)
    val playableCount = catalog.tracks.count { it.playable }

    LazyColumn(Modifier.fillMaxSize(), contentPadding = contentPadding) {
        item {
            Row(
                Modifier.fillMaxWidth().statusBarsPadding().padding(start = 20.dp, end = 8.dp, top = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(Modifier.size(42.dp).clip(RoundedCornerShape(12.dp)).background(GoldGradient), contentAlignment = Alignment.Center) {
                    MihrabPattern(Modifier.fillMaxSize().padding(4.dp), color = Night, alpha = 0.8f)
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(greeting, style = MaterialTheme.typography.bodySmall, color = Muted)
                    Text("نوای آسمانی", style = MaterialTheme.typography.headlineSmall)
                }
                IconButton(onClick = onRefresh) {
                    if (snap.refreshing) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp, color = Gold)
                    else Icon(Icons.Rounded.Refresh, "به‌روزرسانی آرشیو", tint = Cream)
                }
                IconButton(onClick = onAbout) { Icon(Icons.Rounded.Info, "درباره", tint = Cream) }
            }
        }
        item {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp).clip(RoundedCornerShape(16.dp))
                    .background(Surface2).clickable(onClick = onSearch).padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Rounded.Search, null, tint = Muted)
                Spacer(Modifier.width(10.dp))
                Text("جست‌وجوی مداح، نوحه، مناسبت…", color = Muted, style = MaterialTheme.typography.bodyMedium)
            }
        }
        if (todayCollection != null) item {
            val tracks = catalog.tracksIn(todayCollection)
            val colors = if (todayCollection.colors.size >= 2) Color(todayCollection.colors[0]) to Color(todayCollection.colors[1])
            else paletteFor(todayCollection.id)
            Box(
                Modifier.padding(horizontal = 20.dp).fillMaxWidth().height(210.dp).clip(RoundedCornerShape(24.dp))
                    .clickable { actions.openCollection(todayCollection.id) }
            ) {
                CoverArt(todayCollection.id, Modifier.fillMaxSize(), shape = RoundedCornerShape(24.dp), colors = colors)
                Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Night.copy(alpha = 0.75f)))))
                Column(Modifier.align(Alignment.BottomStart).padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.AutoAwesome, null, tint = Gold, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(
                            "مناسبت امروز" + (hijri?.let { " · ${it.label}" } ?: ""),
                            style = MaterialTheme.typography.labelMedium, color = Gold
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(todayCollection.title, style = MaterialTheme.typography.headlineMedium, color = Cream)
                    Text(todayCollection.subtitle, style = MaterialTheme.typography.bodyMedium, color = Cream.copy(alpha = 0.8f),
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Spacer(Modifier.height(12.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        GoldButton("شنیدن", Icons.Rounded.PlayArrow, onClick = {
                            val first = tracks.firstOrNull { it.playable } ?: tracks.firstOrNull()
                            if (first != null) actions.play(first, tracks) else actions.openCollection(todayCollection.id)
                        })
                        Spacer(Modifier.width(12.dp))
                        Text("${tracks.size} اثر", style = MaterialTheme.typography.labelLarge, color = Cream)
                    }
                }
            }
        }
        if (maddahs.isNotEmpty()) {
            item { SectionHeader("مداحان", action = "همه", onAction = onAllMaddahs) }
            item {
                LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    items(maddahs, key = { it.id }) { m ->
                        Column(
                            Modifier.width(88.dp).clip(RoundedCornerShape(12.dp)).clickable { actions.openMaddah(m.id) },
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            MaddahAvatar(m, 84.dp, ring = m.featured || m.id in snap.library.following)
                            Spacer(Modifier.height(8.dp))
                            Text(m.name, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold,
                                maxLines = 2, textAlign = TextAlign.Center, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
        }
        if (recent.isNotEmpty()) {
            item { SectionHeader("ادامه شنیدن") }
            item {
                LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(recent, key = { "r" + it.id }) { TrackCard(it, recent) }
                }
            }
        }
        if (followed.isNotEmpty()) {
            item { SectionHeader("از مداحان دنبال‌شده") }
            item {
                LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(followed, key = { "f" + it.id }) { TrackCard(it, followed) }
                }
            }
        }
        if (catalog.collections.isNotEmpty()) {
            item { SectionHeader("مجموعه‌ها") }
            item {
                LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(catalog.collections, key = { it.id }) { c ->
                        CollectionCard(c, catalog.tracksIn(c).size, Modifier.width(250.dp).height(140.dp)) { actions.openCollection(c.id) }
                    }
                }
            }
        }
        if (fresh.isNotEmpty()) {
            item { SectionHeader("تازه‌های آرشیو") }
            item {
                LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(fresh, key = { "n" + it.id }) { TrackCard(it, fresh) }
                }
            }
        }
        if (popular.isNotEmpty()) {
            item { SectionHeader("پرشنیده‌ها") }
            itemsIndexed(popular, key = { _, t -> "p" + t.id }) { i, t -> TrackRow(t, popular, index = i + 1) }
        }
        item { SectionHeader("دسته‌ها") }
        item {
            Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                catalog.categories.chunked(2).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        row.forEach { cat ->
                            val (a, b) = paletteFor("cat-$cat")
                            Box(
                                Modifier.weight(1f).height(76.dp).clip(RoundedCornerShape(16.dp))
                                    .background(Brush.horizontalGradient(listOf(a, b))).clickable { onCategory(cat) }
                            ) {
                                MihrabPattern(Modifier.fillMaxSize(), alpha = 0.2f)
                                Text(cat, style = MaterialTheme.typography.titleMedium, color = Cream,
                                    modifier = Modifier.align(Alignment.CenterStart).padding(16.dp))
                            }
                        }
                        if (row.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
            }
        }
        item {
            Text(
                "${catalog.tracks.size} اثر از ${maddahs.size} مداح · $playableCount اثر قابل پخش مستقیم\n" +
                    "آثاری که ناشر لینک مستقیم منتشر نکرده، در صفحه رسمی همان ناشر پخش می‌شوند.",
                style = MaterialTheme.typography.bodySmall, color = Muted, textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(24.dp)
            )
        }
    }
}
