package ir.navaasemani.app

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.Player
import ir.navaasemani.app.data.AudioItem
import ir.navaasemani.app.ui.*

@Composable
fun NavaFullPlayer(item: AudioItem, controller: Player?, position: Long, duration: Long,
                   favorite: Boolean, onFavorite: () -> Unit, onDownload: () -> Unit,
                   onSource: () -> Unit, onClose: () -> Unit) {
    val safeDuration = duration.coerceAtLeast(0L)
    val safePosition = position.coerceIn(0L, safeDuration.coerceAtLeast(1L))
    var seeking by remember(item.id) { mutableFloatStateOf(-1f) }
    val ratio = if (safeDuration > 0) (safePosition.toFloat() / safeDuration).coerceIn(0f, 1f) else 0f
    val playing = controller?.isPlaying == true
    val primary = Color(0xFF29D2C0)
    fun fmt(ms: Long): String {
        val seconds = (ms / 1000L).coerceAtLeast(0L)
        return "%02d:%02d".format(seconds / 60, seconds % 60)
    }
    Column(Modifier.fillMaxWidth().background(
        Brush.verticalGradient(listOf(Color(0xFF102F41), Ink, Color(0xFF0B1826)))
    ).padding(horizontal = 24.dp).padding(bottom = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onClose) { Icon(Icons.Default.KeyboardArrowDown, "بستن", tint = Cream) }
            Spacer(Modifier.weight(1f))
            Text("در حال پخش", color = Gold, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.weight(1f))
            IconButton(onClick = onSource, enabled = item.sourceUrl.isNotBlank()) {
                Icon(Icons.Default.OpenInNew, "منبع اثر", tint = Cream)
            }
        }
        Box(Modifier.fillMaxWidth().heightIn(min = 170.dp, max = 280.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(Brush.linearGradient(listOf(Color(0xFF17605E), Color(0xFF193E54), Color(0xFF111E30))))
            .border(1.dp, Gold.copy(alpha = .5f), RoundedCornerShape(28.dp)),
            contentAlignment = Alignment.Center) {
            Canvas(Modifier.fillMaxSize()) {
                for (i in 1..5) {
                    drawCircle(Gold.copy(alpha = .09f), radius = size.minDimension * (i * .095f),
                        center = Offset(size.width * .5f, size.height * .5f),
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2f))
                }
            }
            Icon(Icons.Default.GraphicEq, null, tint = Gold, modifier = Modifier.size(90.dp))
        }
        Text(item.title, color = Cream, fontSize = 22.sp, fontWeight = FontWeight.Bold,
            maxLines = 2, overflow = TextOverflow.Ellipsis)
        Text(item.artist, color = Muted, fontSize = 13.sp)
        Column {
            Slider(value = if (seeking >= 0f) seeking else ratio,
                onValueChange = { seeking = it },
                onValueChangeFinished = {
                    if (safeDuration > 0 && seeking >= 0f) controller?.seekTo((safeDuration * seeking).toLong())
                    seeking = -1f
                }, enabled = safeDuration > 0,
                colors = SliderDefaults.colors(thumbColor = primary, activeTrackColor = primary,
                    inactiveTrackColor = PanelLight))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(fmt(safePosition), color = Muted, fontSize = 11.sp)
                Text(if (safeDuration > 0) fmt(safeDuration) else "--:--", color = Muted, fontSize = 11.sp)
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { controller?.let { it.repeatMode =
                if (it.repeatMode == Player.REPEAT_MODE_ONE) Player.REPEAT_MODE_OFF else Player.REPEAT_MODE_ONE
            } }) {
                Icon(Icons.Default.RepeatOne, "تکرار", tint = if (controller?.repeatMode == Player.REPEAT_MODE_ONE) primary else Muted)
            }
            IconButton(onClick = { controller?.seekToPreviousMediaItem() }, enabled = controller?.hasPreviousMediaItem() == true) {
                Icon(Icons.Default.SkipPrevious, "قبلی", tint = Cream, modifier = Modifier.size(32.dp))
            }
            Box(Modifier.size(70.dp).clip(CircleShape)
                .background(Brush.linearGradient(listOf(primary, Gold)))
                .clickable { if (playing) controller?.pause() else controller?.play() },
                contentAlignment = Alignment.Center) {
                Icon(if (playing) Icons.Default.Pause else Icons.Default.PlayArrow,
                    "پخش یا توقف", tint = Ink, modifier = Modifier.size(40.dp))
            }
            IconButton(onClick = { controller?.seekToNextMediaItem() }, enabled = controller?.hasNextMediaItem() == true) {
                Icon(Icons.Default.SkipNext, "بعدی", tint = Cream, modifier = Modifier.size(32.dp))
            }
            IconButton(onClick = onFavorite) {
                Icon(if (favorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                    "علاقه‌مندی", tint = if (favorite) Gold else Muted)
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            OutlinedButton(onClick = onDownload, enabled = item.downloadAllowed) {
                Icon(Icons.Default.Download, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("دانلود")
            }
            OutlinedButton(onClick = onSource, enabled = item.sourceUrl.isNotBlank()) {
                Icon(Icons.Default.Link, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("صفحه منبع")
            }
        }
        Text("پخش و دانلود فقط برای آثار دارای مجوز فعال است", color = Muted, fontSize = 11.sp)
    }
}
