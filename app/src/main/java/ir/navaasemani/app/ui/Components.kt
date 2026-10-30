package ir.navaasemani.app.ui

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.PlaylistAdd
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.DownloadDone
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Forum
import androidx.compose.material.icons.rounded.HourglassTop
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Podcasts
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.SmartDisplay
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import ir.navaasemani.app.data.Maddah
import ir.navaasemani.app.data.Track
import ir.navaasemani.app.data.TrackCollection
import ir.navaasemani.app.ui.theme.Cream
import ir.navaasemani.app.ui.theme.Faint
import ir.navaasemani.app.ui.theme.Gold
import ir.navaasemani.app.ui.theme.GoldGradient
import ir.navaasemani.app.ui.theme.Muted
import ir.navaasemani.app.ui.theme.Night
import ir.navaasemani.app.ui.theme.Surface2
import ir.navaasemani.app.ui.theme.Turquoise
import ir.navaasemani.app.ui.theme.paletteFor

/** نقش محراب و کاشی؛ امضای بصری اپ روی کاورها */
@Composable
fun MihrabPattern(modifier: Modifier = Modifier, color: Color = Gold, alpha: Float = 0.28f) {
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val stroke = Stroke(width = (w / 160f).coerceIn(1.2f, 3f))
        val c = color.copy(alpha = alpha)
        for (i in 0..2) {
            val inset = w * (0.18f + i * 0.09f)
            val top = h * (0.16f + i * 0.08f)
            val path = Path().apply {
                moveTo(inset, h * 1.02f)
                lineTo(inset, h * 0.52f)
                quadraticTo(inset, top + h * 0.06f, w / 2f, top)
                quadraticTo(w - inset, top + h * 0.06f, w - inset, h * 0.52f)
                lineTo(w - inset, h * 1.02f)
            }
            drawPath(path, c, style = stroke)
        }
        val r = w * 0.07f
        val cx = w / 2f
        val cy = h * 0.09f
        val star = Path().apply {
            for (k in 0 until 16) {
                val ang = Math.PI * k / 8.0 - Math.PI / 2
                val rad = if (k % 2 == 0) r else r * 0.45f
                val x = cx + (rad * Math.cos(ang)).toFloat()
                val y = cy + (rad * Math.sin(ang)).toFloat()
                if (k == 0) moveTo(x, y) else lineTo(x, y)
            }
            close()
        }
        drawPath(star, c.copy(alpha = alpha * 1.6f))
    }
}

/** کاور اثر یا مجموعه: عکس اگر باشد، در غیر این صورت گرادیان اختصاصی با نقش محراب */
@Composable
fun CoverArt(
    seed: String,
    modifier: Modifier = Modifier,
    imageUrl: String = "",
    label: String = "",
    shape: Shape = RoundedCornerShape(14.dp),
    colors: Pair<Color, Color>? = null,
    labelSize: Int = 13
) {
    val (top, bottom) = colors ?: paletteFor(seed)
    Box(
        modifier
            .clip(shape)
            .background(Brush.linearGradient(listOf(top, bottom), start = Offset.Zero, end = Offset.Infinite))
    ) {
        MihrabPattern(Modifier.fillMaxSize())
        if (label.isNotBlank()) {
            Text(
                label, color = Cream.copy(alpha = 0.92f), fontSize = labelSize.sp, fontWeight = FontWeight.Black,
                maxLines = 2, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center,
                modifier = Modifier.align(Alignment.BottomCenter).padding(horizontal = 8.dp, vertical = 10.dp)
            )
        }
        if (imageUrl.isNotBlank()) {
            AsyncImage(
                model = imageUrl, contentDescription = null, contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
fun TrackCover(track: Track, modifier: Modifier = Modifier, shape: Shape = RoundedCornerShape(12.dp), showLabel: Boolean = false) {
    val maddah = LocalSnapshot.current.catalog.maddah(track.maddahId)
    CoverArt(
        seed = track.maddahId, modifier = modifier, imageUrl = track.artwork.ifBlank { maddah?.photo.orEmpty() },
        label = if (showLabel) maddah?.name.orEmpty() else "", shape = shape
    )
}

@Composable
fun MaddahAvatar(maddah: Maddah, size: Dp, modifier: Modifier = Modifier, ring: Boolean = false) {
    val (top, bottom) = paletteFor(maddah.id)
    val initial = maddah.name.trim().split(' ').lastOrNull()?.firstOrNull()?.toString().orEmpty()
    Box(
        modifier
            .size(size)
            .then(if (ring) Modifier.border(2.dp, GoldGradient, CircleShape).padding(4.dp) else Modifier)
            .clip(CircleShape)
            .background(Brush.linearGradient(listOf(top, bottom))),
        contentAlignment = Alignment.Center
    ) {
        MihrabPattern(Modifier.fillMaxSize(), alpha = 0.18f)
        Text(initial, color = Cream, fontWeight = FontWeight.Black, fontSize = (size.value * 0.36f).sp)
        if (maddah.photo.isNotBlank()) {
            AsyncImage(maddah.photo, contentDescription = maddah.displayName, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        }
    }
}

@Composable
fun SectionHeader(title: String, modifier: Modifier = Modifier, action: String? = null, onAction: () -> Unit = {}) {
    Row(
        modifier.fillMaxWidth().padding(start = 20.dp, end = 12.dp, top = 22.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(width = 4.dp, height = 18.dp).clip(RoundedCornerShape(2.dp)).background(GoldGradient))
        Spacer(Modifier.width(10.dp))
        Text(title, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
        if (action != null) {
            Text(
                action, color = Gold, style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable(onClick = onAction).padding(8.dp)
            )
        }
    }
}

/** میله‌های متحرک «در حال پخش» */
@Composable
fun PlayingBars(modifier: Modifier = Modifier, playing: Boolean = true, color: Color = Gold) {
    val transition = rememberInfiniteTransition(label = "bars")
    val a by transition.animateFloat(0.3f, 1f, infiniteRepeatable(tween(420), RepeatMode.Reverse), label = "a")
    val b by transition.animateFloat(1f, 0.25f, infiniteRepeatable(tween(530), RepeatMode.Reverse), label = "b")
    val c by transition.animateFloat(0.5f, 0.9f, infiniteRepeatable(tween(360), RepeatMode.Reverse), label = "c")
    Canvas(modifier.size(16.dp)) {
        val values = if (playing) listOf(a, b, c) else listOf(0.35f, 0.6f, 0.45f)
        val bw = size.width / 5f
        values.forEachIndexed { i, v ->
            val bh = size.height * v
            drawRoundRect(
                color, topLeft = Offset(bw * (i * 2), size.height - bh), size = Size(bw, bh),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(bw / 2, bw / 2)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Pill(text: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier, icon: ImageVector? = null) {
    Surface(
        onClick = onClick, shape = RoundedCornerShape(50),
        color = if (selected) Gold else Surface2,
        contentColor = if (selected) Night else Cream,
        modifier = modifier
    ) {
        Row(Modifier.padding(horizontal = 16.dp, vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) {
                Icon(icon, null, Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
            }
            Text(text, style = MaterialTheme.typography.labelLarge)
        }
    }
}

fun linkIcon(type: String): ImageVector = when (type.lowercase()) {
    "instagram" -> Icons.Rounded.PhotoCamera
    "telegram" -> Icons.AutoMirrored.Rounded.Send
    "youtube", "aparat" -> Icons.Rounded.SmartDisplay
    "eitaa", "rubika", "bale", "soroush" -> Icons.Rounded.Forum
    "podcast", "rss", "castbox" -> Icons.Rounded.Podcasts
    else -> Icons.Rounded.Language
}

fun linkLabel(type: String): String = when (type.lowercase()) {
    "instagram" -> "اینستاگرام"
    "telegram" -> "تلگرام"
    "youtube" -> "یوتیوب"
    "aparat" -> "آپارات"
    "eitaa" -> "ایتا"
    "rubika" -> "روبیکا"
    "bale" -> "بله"
    "soroush" -> "سروش"
    "podcast", "rss", "castbox" -> "پادکست"
    else -> "سایت رسمی"
}

@Composable
fun TrackRow(
    track: Track,
    queue: List<Track>,
    modifier: Modifier = Modifier,
    index: Int? = null,
    showArtist: Boolean = true,
    trailing: (@Composable () -> Unit)? = null
) {
    val snap = LocalSnapshot.current
    val actions = LocalActions.current
    val isCurrent = snap.player.currentId == track.id
    val download = snap.downloads[track.id]
    val favorite = track.id in snap.library.favorites
    Row(
        modifier
            .fillMaxWidth()
            .clickable { actions.play(track, queue) }
            .padding(start = 16.dp, end = 4.dp, top = 7.dp, bottom = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (index != null) {
            Text(
                "$index", color = if (index <= 3) Gold else Faint, fontWeight = FontWeight.Black,
                fontSize = 18.sp, textAlign = TextAlign.Center, modifier = Modifier.width(30.dp)
            )
            Spacer(Modifier.width(6.dp))
        }
        Box {
            TrackCover(track, Modifier.size(54.dp))
            if (isCurrent) {
                Box(
                    Modifier.size(54.dp).clip(RoundedCornerShape(12.dp)).background(Night.copy(alpha = 0.55f)),
                    contentAlignment = Alignment.Center
                ) { PlayingBars(playing = snap.player.isPlaying) }
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                track.title, maxLines = 1, overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.titleSmall, color = if (isCurrent) Gold else Cream
            )
            val meta = buildList {
                if (showArtist) add(snap.catalog.artistLine(track))
                if (track.occasion.isNotBlank()) add(track.occasion) else add(track.category)
                if (track.duration.isNotBlank()) add(track.duration)
            }.filter { it.isNotBlank() }.joinToString(" · ")
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (!track.playable && download?.done != true) {
                    Icon(Icons.Rounded.Public, null, tint = Turquoise, modifier = Modifier.size(13.dp))
                    Spacer(Modifier.width(4.dp))
                } else if (download?.done == true) {
                    Icon(Icons.Rounded.DownloadDone, null, tint = Turquoise, modifier = Modifier.size(13.dp))
                    Spacer(Modifier.width(4.dp))
                }
                Text(meta, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodySmall, color = Muted)
            }
        }
        if (favorite) Icon(Icons.Rounded.Favorite, null, tint = Gold, modifier = Modifier.size(16.dp).padding(end = 2.dp))
        if (trailing != null) trailing() else TrackMenuButton(track)
    }
}

@Composable
fun TrackMenuButton(track: Track) {
    val snap = LocalSnapshot.current
    val actions = LocalActions.current
    var open by remember { mutableStateOf(false) }
    val download = snap.downloads[track.id]
    val favorite = track.id in snap.library.favorites
    Box {
        IconButton(onClick = { open = true }) { Icon(Icons.Rounded.MoreVert, "گزینه‌ها", tint = Muted) }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            DropdownMenuItem(
                text = { Text(if (track.playable) "پخش" else "شنیدن در صفحه ناشر") },
                leadingIcon = { Icon(if (track.playable) Icons.Rounded.PlayArrow else Icons.Rounded.Public, null) },
                onClick = { open = false; actions.play(track, listOf(track)) }
            )
            DropdownMenuItem(
                text = { Text(if (favorite) "حذف از علاقه‌مندی‌ها" else "افزودن به علاقه‌مندی‌ها") },
                leadingIcon = { Icon(if (favorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder, null) },
                onClick = { open = false; actions.toggleFavorite(track.id) }
            )
            DropdownMenuItem(
                text = { Text("افزودن به فهرست پخش") },
                leadingIcon = { Icon(Icons.AutoMirrored.Rounded.PlaylistAdd, null) },
                onClick = { open = false; actions.askPlaylist(track) }
            )
            when {
                download?.done == true -> DropdownMenuItem(
                    text = { Text("حذف فایل دانلودشده") }, leadingIcon = { Icon(Icons.Rounded.Delete, null) },
                    onClick = { open = false; actions.removeDownload(track.id) }
                )
                download?.active == true -> DropdownMenuItem(
                    text = { Text("لغو دانلود") }, leadingIcon = { Icon(Icons.Rounded.HourglassTop, null) },
                    onClick = { open = false; actions.removeDownload(track.id) }
                )
                track.canDownload -> DropdownMenuItem(
                    text = { Text("دانلود") }, leadingIcon = { Icon(Icons.Rounded.Download, null) },
                    onClick = { open = false; actions.download(track) }
                )
            }
            DropdownMenuItem(
                text = { Text("صفحه مداح") }, leadingIcon = { Icon(Icons.Rounded.Person, null) },
                onClick = { open = false; actions.openMaddah(track.maddahId) }
            )
            if (track.sourceUrl.isNotBlank()) DropdownMenuItem(
                text = { Text("منبع: ${track.sourceName.ifBlank { "صفحه ناشر" }}") },
                leadingIcon = { Icon(Icons.Rounded.Public, null) },
                onClick = { open = false; actions.openUrl(track.sourceUrl) }
            )
            DropdownMenuItem(
                text = { Text("اشتراک‌گذاری") }, leadingIcon = { Icon(Icons.Rounded.Share, null) },
                onClick = { open = false; actions.share(track) }
            )
        }
    }
}

@Composable
fun TrackCard(track: Track, queue: List<Track>, modifier: Modifier = Modifier) {
    val snap = LocalSnapshot.current
    val actions = LocalActions.current
    val isCurrent = snap.player.currentId == track.id
    Column(modifier.width(150.dp).clip(RoundedCornerShape(16.dp)).clickable { actions.play(track, queue) }) {
        Box {
            TrackCover(track, Modifier.size(150.dp), RoundedCornerShape(16.dp), showLabel = true)
            Box(
                Modifier.align(Alignment.BottomStart).padding(8.dp).size(34.dp).clip(CircleShape)
                    .background(if (isCurrent) Gold else Night.copy(alpha = 0.7f)),
                contentAlignment = Alignment.Center
            ) {
                if (isCurrent) PlayingBars(playing = snap.player.isPlaying, color = Night)
                else Icon(if (track.playable) Icons.Rounded.PlayArrow else Icons.Rounded.Public, null, tint = Cream, modifier = Modifier.size(20.dp))
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(track.title, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.titleSmall,
            color = if (isCurrent) Gold else Cream, modifier = Modifier.padding(horizontal = 2.dp))
        Text(snap.catalog.artistLine(track), maxLines = 1, overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.bodySmall, color = Muted, modifier = Modifier.padding(horizontal = 2.dp))
    }
}

@Composable
fun CollectionCard(collection: TrackCollection, count: Int, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val colors = if (collection.colors.size >= 2) Color(collection.colors[0]) to Color(collection.colors[1]) else paletteFor(collection.id)
    Box(modifier.clip(RoundedCornerShape(20.dp)).clickable(onClick = onClick)) {
        CoverArt(collection.id, Modifier.fillMaxSize(), shape = RoundedCornerShape(20.dp), colors = colors)
        Column(Modifier.align(Alignment.BottomStart).padding(16.dp)) {
            Text(collection.title, style = MaterialTheme.typography.titleLarge, color = Cream)
            if (collection.subtitle.isNotBlank()) Text(collection.subtitle, style = MaterialTheme.typography.bodySmall, color = Cream.copy(alpha = 0.75f), maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("$count اثر", style = MaterialTheme.typography.labelMedium, color = Gold)
        }
    }
}

@Composable
fun EmptyState(icon: ImageVector, title: String, text: String, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth().padding(36.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(84.dp).clip(CircleShape).background(Surface2), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = Gold, modifier = Modifier.size(38.dp))
        }
        Spacer(Modifier.height(16.dp))
        Text(title, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
        Spacer(Modifier.height(6.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium, color = Muted, textAlign = TextAlign.Center)
    }
}

@Composable
fun GoldButton(text: String, icon: ImageVector?, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier.clip(RoundedCornerShape(50)).background(GoldGradient).clickable(onClick = onClick)
            .padding(horizontal = 22.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) {
                Icon(icon, null, tint = Night, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(6.dp))
            }
            Text(text, color = Night, style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
fun SquareCoverBox(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    Box(modifier.fillMaxWidth().aspectRatio(1f), content = content)
}

@Composable
fun HorizontalSpacer(width: Dp) = Spacer(Modifier.width(width))

@Composable
fun VerticalGap(height: Dp) = Spacer(Modifier.height(height))
