package ir.navaasemani.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.DownloadDone
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Forward10
import androidx.compose.material.icons.rounded.HourglassTop
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.Replay10
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.CompositionLocalProvider
import androidx.media3.common.Player
import ir.navaasemani.app.AppViewModel
import ir.navaasemani.app.data.Persian
import ir.navaasemani.app.data.Track
import ir.navaasemani.app.ui.LocalActions
import ir.navaasemani.app.ui.LocalSnapshot
import ir.navaasemani.app.ui.TrackCover
import ir.navaasemani.app.ui.TrackMenuButton
import ir.navaasemani.app.ui.theme.Cream
import ir.navaasemani.app.ui.theme.Gold
import ir.navaasemani.app.ui.theme.GoldGradient
import ir.navaasemani.app.ui.theme.Muted
import ir.navaasemani.app.ui.theme.Night
import ir.navaasemani.app.ui.theme.Surface2
import ir.navaasemani.app.ui.theme.Surface3
import ir.navaasemani.app.ui.theme.Turquoise
import ir.navaasemani.app.ui.theme.paletteFor

@Composable
fun MiniPlayer(track: Track, vm: AppViewModel, onOpen: () -> Unit, modifier: Modifier = Modifier) {
    val snap = LocalSnapshot.current
    val p = snap.player
    val progress by vm.progress.collectAsState()
    val (top, _) = paletteFor(track.maddahId)
    Surface(
        shape = RoundedCornerShape(18.dp), color = Surface2, shadowElevation = 12.dp,
        modifier = modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 6.dp)
            .pointerInput(track.id) { detectVerticalDragGestures { _, drag -> if (drag < -14f) onOpen() } }
            .clickable(onClick = onOpen)
    ) {
        Column(Modifier.background(Brush.horizontalGradient(listOf(top.copy(alpha = 0.55f), Surface2)))) {
            Row(Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                TrackCover(track, Modifier.size(46.dp), RoundedCornerShape(10.dp))
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(track.title, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(snap.catalog.artistLine(track), style = MaterialTheme.typography.bodySmall, color = Muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                IconButton(onClick = vm::togglePlay) {
                    if (p.buffering && !p.isPlaying) CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp, color = Gold)
                    else Icon(if (p.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, "پخش/توقف", tint = Cream, modifier = Modifier.size(30.dp))
                }
                IconButton(onClick = vm::next) { Icon(Icons.Rounded.SkipNext, "بعدی", tint = Cream) }
            }
            LinearProgressIndicator(
                progress = { if (progress.duration > 0) (progress.position.toFloat() / progress.duration).coerceIn(0f, 1f) else 0f },
                modifier = Modifier.fillMaxWidth().height(2.dp), color = Gold, trackColor = Color.Transparent
            )
        }
    }
}

@Composable
fun PlayerScreen(vm: AppViewModel, onClose: () -> Unit) {
    val snap = LocalSnapshot.current
    val actions = LocalActions.current
    val track = snap.currentTrack ?: return
    val p = snap.player
    val progress by vm.progress.collectAsState()
    val (top, bottom) = paletteFor(track.maddahId)
    val download = snap.downloads[track.id]
    val favorite = track.id in snap.library.favorites
    var dragging by remember { mutableStateOf<Float?>(null) }
    var speedMenu by remember { mutableStateOf(false) }
    var sleepMenu by remember { mutableStateOf(false) }

    BoxWithConstraints(
        Modifier.fillMaxSize()
            .background(Brush.verticalGradient(listOf(top, bottom, Night)))
            .pointerInput(Unit) { detectVerticalDragGestures { _, drag -> if (drag > 30f) onClose() } }
    ) {
        val landscape = maxWidth > maxHeight && maxWidth > 600.dp

        val header: @Composable () -> Unit = {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onClose) { Icon(Icons.Rounded.KeyboardArrowDown, "بستن", tint = Cream, modifier = Modifier.size(32.dp)) }
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("در حال پخش", style = MaterialTheme.typography.labelSmall, color = Muted)
                    Text(track.occasion.ifBlank { track.category }, style = MaterialTheme.typography.labelLarge, maxLines = 1)
                }
                TrackMenuButton(track)
            }
        }
        val cover: @Composable (Modifier) -> Unit = { m ->
            Box(m.aspectRatio(1f).shadow(24.dp, RoundedCornerShape(28.dp))) {
                TrackCover(track, Modifier.fillMaxSize(), RoundedCornerShape(28.dp), showLabel = true)
            }
        }
        val controls: @Composable () -> Unit = {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(track.title, style = MaterialTheme.typography.headlineSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        Text(
                            snap.catalog.artistLine(track), style = MaterialTheme.typography.titleSmall, color = Gold,
                            modifier = Modifier.clip(RoundedCornerShape(6.dp)).clickable { onClose(); actions.openMaddah(track.maddahId) }
                        )
                    }
                    IconButton(onClick = { actions.toggleFavorite(track.id) }) {
                        Icon(if (favorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder, "علاقه‌مندی",
                            tint = if (favorite) Gold else Cream, modifier = Modifier.size(28.dp))
                    }
                }
                Spacer(Modifier.height(10.dp))
                // نوار پیشرفت همیشه چپ‌به‌راست، مثل همه پخش‌کننده‌ها
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                    Column {
                        val duration = progress.duration.coerceAtLeast(1L).toFloat()
                        Slider(
                            value = dragging ?: progress.position.toFloat().coerceIn(0f, duration),
                            onValueChange = { dragging = it },
                            onValueChangeFinished = { dragging?.let { vm.seekTo(it.toLong()) }; dragging = null },
                            valueRange = 0f..duration,
                            colors = SliderDefaults.colors(thumbColor = Gold, activeTrackColor = Gold, inactiveTrackColor = Surface3)
                        )
                        Row(Modifier.fillMaxWidth()) {
                            Text(Persian.time((dragging?.toLong()) ?: progress.position), style = MaterialTheme.typography.labelSmall, color = Muted)
                            Spacer(Modifier.weight(1f))
                            Text(if (progress.duration > 0) Persian.time(progress.duration) else track.duration, style = MaterialTheme.typography.labelSmall, color = Muted)
                        }
                        Spacer(Modifier.height(6.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = vm::toggleShuffle) {
                                Icon(Icons.Rounded.Shuffle, "تصادفی", tint = if (p.shuffle) Gold else Muted)
                            }
                            IconButton(onClick = { vm.seekBy(-10_000) }) { Icon(Icons.Rounded.Replay10, "۱۰ ثانیه عقب", tint = Cream) }
                            IconButton(onClick = vm::previous, modifier = Modifier.size(52.dp)) {
                                Icon(Icons.Rounded.SkipPrevious, "قبلی", tint = Cream, modifier = Modifier.size(38.dp))
                            }
                            Box(
                                Modifier.size(76.dp).shadow(16.dp, CircleShape).clip(CircleShape).background(GoldGradient)
                                    .clickable(onClick = vm::togglePlay),
                                contentAlignment = Alignment.Center
                            ) {
                                if (p.buffering && !p.isPlaying) CircularProgressIndicator(Modifier.size(30.dp), color = Night, strokeWidth = 3.dp)
                                else Icon(if (p.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, "پخش/توقف", tint = Night, modifier = Modifier.size(42.dp))
                            }
                            IconButton(onClick = vm::next, modifier = Modifier.size(52.dp)) {
                                Icon(Icons.Rounded.SkipNext, "بعدی", tint = Cream, modifier = Modifier.size(38.dp))
                            }
                            IconButton(onClick = { vm.seekBy(10_000) }) { Icon(Icons.Rounded.Forward10, "۱۰ ثانیه جلو", tint = Cream) }
                            IconButton(onClick = vm::cycleRepeat) {
                                Icon(if (p.repeatMode == Player.REPEAT_MODE_ONE) Icons.Rounded.RepeatOne else Icons.Rounded.Repeat, "تکرار",
                                    tint = if (p.repeatMode == Player.REPEAT_MODE_OFF) Muted else Gold)
                            }
                        }
                    }
                }
                Spacer(Modifier.height(14.dp))
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Night.copy(alpha = 0.35f)).padding(4.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically
                ) {
                    when {
                        download?.done == true -> IconButton(onClick = { actions.removeDownload(track.id) }) {
                            Icon(Icons.Rounded.DownloadDone, "دانلود شده", tint = Turquoise)
                        }
                        download?.active == true -> IconButton(onClick = { actions.removeDownload(track.id) }) {
                            Box(contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(progress = { download?.progress ?: 0f }, modifier = Modifier.size(26.dp), color = Turquoise, strokeWidth = 2.5.dp)
                                Icon(Icons.Rounded.HourglassTop, null, tint = Cream, modifier = Modifier.size(12.dp))
                            }
                        }
                        else -> IconButton(onClick = { actions.download(track) }, enabled = track.canDownload) {
                            Icon(Icons.Rounded.Download, "دانلود", tint = if (track.canDownload) Cream else Muted.copy(alpha = 0.4f))
                        }
                    }
                    Box {
                        IconButton(onClick = { speedMenu = true }) {
                            Icon(Icons.Rounded.Speed, "سرعت", tint = if (p.speed != 1f) Gold else Cream)
                        }
                        DropdownMenu(expanded = speedMenu, onDismissRequest = { speedMenu = false }) {
                            listOf(0.75f, 1f, 1.25f, 1.5f, 1.75f, 2f).forEach { s ->
                                DropdownMenuItem(text = { Text("${s}x" + if (s == p.speed) "  ✓" else "") },
                                    onClick = { speedMenu = false; vm.setSpeed(s) })
                            }
                        }
                    }
                    Box {
                        IconButton(onClick = { sleepMenu = true }) {
                            Icon(Icons.Rounded.Bedtime, "زمان‌سنج خواب", tint = if (p.sleepEndsAt > 0) Gold else Cream)
                        }
                        DropdownMenu(expanded = sleepMenu, onDismissRequest = { sleepMenu = false }) {
                            listOf(15, 30, 45, 60, 90).forEach { m ->
                                DropdownMenuItem(text = { Text("$m دقیقه") }, onClick = { sleepMenu = false; vm.setSleepTimer(m) })
                            }
                            if (p.sleepEndsAt > 0) DropdownMenuItem(text = { Text("خاموش") }, onClick = { sleepMenu = false; vm.setSleepTimer(0) })
                        }
                    }
                    IconButton(onClick = { actions.share(track) }) { Icon(Icons.Rounded.Share, "اشتراک", tint = Cream) }
                    IconButton(onClick = { if (track.sourceUrl.isNotBlank()) actions.openUrl(track.sourceUrl) }, enabled = track.sourceUrl.isNotBlank()) {
                        Icon(Icons.Rounded.Public, "منبع", tint = Cream)
                    }
                }
                Spacer(Modifier.height(10.dp))
                Text(
                    listOf(track.sourceName.takeIf { it.isNotBlank() }?.let { "منبع: $it" }, track.license.takeIf { it.isNotBlank() })
                        .filterNotNull().joinToString(" · "),
                    style = MaterialTheme.typography.labelSmall, color = Muted, textAlign = TextAlign.Center,
                    maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.fillMaxWidth()
                )
            }
        }

        if (landscape) {
            Row(
                Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                cover(Modifier.fillMaxHeight(0.85f))
                Spacer(Modifier.width(28.dp))
                Column(Modifier.weight(1f)) {
                    header()
                    Spacer(Modifier.height(12.dp))
                    controls()
                }
            }
        } else {
            Column(
                Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(horizontal = 22.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                header()
                Spacer(Modifier.weight(1f))
                cover(Modifier.fillMaxWidth().widthIn(max = 420.dp))
                Spacer(Modifier.weight(1f))
                Box(Modifier.widthIn(max = 520.dp)) { controls() }
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}
