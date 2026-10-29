package ir.navaasemani.app

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ir.navaasemani.app.data.AudioItem
import ir.navaasemani.app.data.CATEGORIES
import ir.navaasemani.app.ui.*
import androidx.compose.ui.unit.Dp

private val BorderGlow = Color(0x5575D8D4)
private val GlassSurface = Color(0xC7163042)

private fun Modifier.glass(shape: RoundedCornerShape = RoundedCornerShape(22.dp)): Modifier =
    this.clip(shape)
        .background(Brush.linearGradient(listOf(Color(0xE5264555), GlassSurface, Color(0xC0132537))))
        .border(1.dp, Brush.linearGradient(listOf(BorderGlow, Color(0x338DA4B7), Color(0x227DA4A5))), shape)

private fun iconForCategory(cat: String) = when (cat) {
    "مداحی" -> Icons.Outlined.Mic
    "روضه" -> Icons.Outlined.FavoriteBorder
    "سخنرانی" -> Icons.Outlined.RecordVoiceOver
    "ادعیه" -> Icons.Outlined.MenuBook
    "مناجات" -> Icons.Outlined.NightsStay
    else -> Icons.Outlined.AutoStories
}

private data class Showcase(
    val label: String, val title: String, val subtitle: String,
    val category: String, val occasion: String?, val first: Color, val second: Color
)

private val showcases = listOf(
    Showcase("مناسبت‌ها", "گلچین نواهای محرم", "ویژه روضه و مداحی‌های محرم", "مداحی", "محرم", Color(0xFF174D50), Color(0xFF172C44)),
    Showcase("پیشنهاد امروز", "آرامش با تلاوت", "سوره‌های کوتاه و تلاوت‌های منتخب", "قرآن", null, Color(0xFF344F6A), Color(0xFF102E42)),
    Showcase("خلوت شبانه", "لحظه‌ای با مناجات", "نیایش‌ها و دعاهای ویژه", "مناجات", null, Color(0xFF3B375F), Color(0xFF102C3C))
)

@Composable
fun GlassHomeScreen(
    items: List<AudioItem>,
    refreshing: Boolean,
    onRefresh: () -> Unit,
    onSearch: () -> Unit,
    onCategory: (String) -> Unit,
    onOccasion: (String) -> Unit,
    onItem: (AudioItem) -> Unit,
    onPlay: (AudioItem) -> Unit,
    favorites: Set<String>,
    onFavorite: (AudioItem) -> Unit,
    currentItem: AudioItem?
) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val wide = maxWidth >= 680.dp
        val columns = if (wide) 6 else 3
        val padding = if (wide) 24.dp else 16.dp
        val maddahi = remember(items) { items.filter { it.category == "مداحی" } }
        val special = remember(items) {
            items.filter { it.category == "مداحی" && (it.isFeatured || it.occasion.contains("محرم") || it.occasion.contains("فاطمیه")) }
        }
        val popular = remember(items) { items.filter { it.category == "مداحی" && it.isPopular } }
        val roza = remember(items) { items.filter { it.category == "روضه" } }
        val sermons = remember(items) { items.filter { it.category == "سخنرانی" } }
        val prayers = remember(items) { items.filter { it.category == "ادعیه" || it.category == "مناجات" } }
        val quran = remember(items) { items.filter { it.category == "قرآن" } }

        LazyColumn(
            modifier = Modifier.fillMaxSize().background(
                Brush.verticalGradient(listOf(Color(0xFF08131F), Ink, Color(0xFF0B1D29)))
            ),
            contentPadding = PaddingValues(start = padding, end = padding, top = 17.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(19.dp)
        ) {
            item {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(48.dp).glass(RoundedCornerShape(16.dp)), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.GraphicEq, null, tint = Gold, modifier = Modifier.size(31.dp))
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("نوای آسمانی", fontSize = 22.sp, color = Cream, fontWeight = FontWeight.ExtraBold)
                        Text("هر نوا، راهی به نور", color = Muted, fontSize = 11.sp)
                    }
                    IconButton(onClick = onRefresh, enabled = !refreshing) {
                        Icon(Icons.Default.Refresh, "به‌روزرسانی", tint = Jade)
                    }
                }
            }
            item {
                Row(Modifier.fillMaxWidth().height(54.dp).glass().clickable(onClick = onSearch)
                    .padding(horizontal = 15.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Search, null, tint = Jade)
                    Spacer(Modifier.width(10.dp))
                    Text("مداح، شعر، دعا، سخنرانی یا سوره...", color = Muted,
                        maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 13.sp)
                }
            }
            item {
                Text("برای لحظه‌های معنوی", fontWeight = FontWeight.Bold, fontSize = 17.sp, color = Cream)
                Spacer(Modifier.height(12.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(showcases) { hero ->
                        val w: Dp = if (wide) 355.dp else (maxWidth * .85f).coerceAtMost(340.dp)
                        GlassHero(hero, Modifier.width(w), onClick = {
                            if (hero.occasion != null) onOccasion(hero.occasion) else onCategory(hero.category)
                        })
                    }
                }
            }
            item {
                Text("مناسبت‌ها", fontWeight = FontWeight.Bold, fontSize = 17.sp, color = Cream)
                Spacer(Modifier.height(10.dp))
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("محرم", "صفر", "اربعین", "فاطمیه", "رمضان", "شب قدر", "اعیاد").forEach { occasion ->
                        Row(Modifier.glass(RoundedCornerShape(20.dp)).clickable { onOccasion(occasion) }
                            .padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.AutoAwesome, null, tint = Gold, modifier = Modifier.size(15.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(occasion, color = Cream, fontSize = 12.sp)
                        }
                    }
                }
            }
            item {
                GlassSectionHeader("دسته‌بندی محتوا", "شش مسیر برای شنیدن", onClick = onSearch)
                Spacer(Modifier.height(10.dp))
                CATEGORIES.chunked(columns).forEach { group ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                        group.forEach { cat ->
                            Column(Modifier.weight(1f).glass(RoundedCornerShape(19.dp))
                                .clickable { onCategory(cat) }
                                .padding(horizontal = 2.dp, vertical = 12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally) {
                                Box(Modifier.size(42.dp).background(Color(0x5543B7B0), CircleShape),
                                    contentAlignment = Alignment.Center) {
                                    Icon(iconForCategory(cat), null, tint = if (cat == "قرآن") Gold else Jade,
                                        modifier = Modifier.size(24.dp))
                                }
                                Spacer(Modifier.height(8.dp))
                                Text(cat, color = Cream, maxLines = 1, fontSize = if (wide) 13.sp else 11.sp)
                            }
                        }
                    }
                    Spacer(Modifier.height(9.dp))
                }
            }
            item {
                GlassAudioSection("مداحی‌های مناسبتی", "ویژه ایام و مناسبت‌ها", special, "مداحی",
                    { onCategory("مداحی") }, onItem, onPlay, favorites, onFavorite)
            }
            item {
                GlassAudioSection("مداحی‌های معروف", "منتخب و نشان‌گذاری‌شده توسط مدیر", popular, "مداحی",
                    { onCategory("مداحی") }, onItem, onPlay, favorites, onFavorite)
            }
            item {
                GlassAudioSection("تازه‌ترین مداحی‌ها", "جدیدترین آثار آرشیو", maddahi, "مداحی",
                    { onCategory("مداحی") }, onItem, onPlay, favorites, onFavorite)
            }
            item {
                GlassAudioSection("روضه‌های منتخب", "دل‌نشین و مناسب مراسم", roza, "روضه",
                    { onCategory("روضه") }, onItem, onPlay, favorites, onFavorite)
            }
            item {
                GlassAudioSection("سخنرانی‌های مذهبی", "آگاهی و معارف", sermons, "سخنرانی",
                    { onCategory("سخنرانی") }, onItem, onPlay, favorites, onFavorite)
            }
            item {
                GlassAudioSection("دعا و مناجات", "برای خلوت و نیایش", prayers, "ادعیه",
                    { onCategory("ادعیه") }, onItem, onPlay, favorites, onFavorite)
            }
            item {
                GlassAudioSection("قرآن روز", "تلاوت‌های آرامش‌بخش", quran, "قرآن",
                    { onCategory("قرآن") }, onItem, onPlay, favorites, onFavorite)
            }
            if (currentItem != null) {
                item {
                    GlassSectionHeader("ادامه شنیدن", "آخرین محتوای در حال پخش", onClick = { onItem(currentItem) })
                    Spacer(Modifier.height(8.dp))
                    GlassAudioCard(currentItem, currentItem.id in favorites, onOpen = { onItem(currentItem) },
                        onPlay = { onPlay(currentItem) }, onFavorite = { onFavorite(currentItem) },
                        modifier = Modifier.fillMaxWidth())
                }
            }
            item {
                Row(Modifier.fillMaxWidth().glass().padding(15.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.VerifiedUser, null, tint = Gold)
                    Spacer(Modifier.width(10.dp))
                    Text("فقط آثار دارای مجوز برای پخش و دانلود فعال می‌شوند.", color = Muted, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
private fun GlassSectionHeader(title: String, caption: String, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, color = Cream, fontSize = 17.sp, fontWeight = FontWeight.Bold)
            Text(caption, color = Muted, fontSize = 11.sp)
        }
        TextButton(onClick = onClick) {
            Text("مشاهده همه", color = Jade, fontSize = 11.sp)
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, null, tint = Jade, modifier = Modifier.size(17.dp))
        }
    }
}

@Composable
private fun GlassAudioSection(
    title: String, subtitle: String, entries: List<AudioItem>, category: String,
    onMore: () -> Unit, onItem: (AudioItem) -> Unit, onPlay: (AudioItem) -> Unit,
    favorites: Set<String>, onFavorite: (AudioItem) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
        GlassSectionHeader(title, subtitle, onMore)
        if (entries.isEmpty()) {
            Row(Modifier.fillMaxWidth().glass().clickable(onClick = onMore)
                .padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(iconForCategory(category), null, tint = Gold)
                Spacer(Modifier.width(12.dp))
                Column {
                    Text("هنوز اثری در این بخش ثبت نشده", color = Cream, fontSize = 13.sp)
                    Text("آثار مجاز را از پنل مدیریت اضافه کنید", color = Muted, fontSize = 11.sp)
                }
            }
        } else {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                items(entries.take(14), key = { it.id }) { entry ->
                    GlassAudioCard(entry, entry.id in favorites, { onItem(entry) },
                        { onPlay(entry) }, { onFavorite(entry) }, modifier = Modifier.width(218.dp))
                }
            }
        }
    }
}

@Composable
private fun GlassAudioCard(
    entry: AudioItem, favorite: Boolean, onOpen: () -> Unit, onPlay: () -> Unit,
    onFavorite: () -> Unit, modifier: Modifier = Modifier
) {
    Column(modifier.glass().clickable(onClick = onOpen).padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(7.dp)) {
        Box(Modifier.fillMaxWidth().height(108.dp).clip(RoundedCornerShape(15.dp))
            .background(Brush.linearGradient(listOf(Color(0xFF215B5C), Color(0xFF1A354D), Color(0xFF273044))))) {
            IslamicPattern(Modifier.fillMaxSize())
            Icon(iconForCategory(entry.category), null, tint = Gold,
                modifier = Modifier.align(Alignment.Center).size(40.dp))
            if (!entry.playable) {
                Text("پیش‌نمایش", color = Cream, fontSize = 10.sp,
                    modifier = Modifier.align(Alignment.TopStart)
                        .padding(8.dp).background(Color(0xB8000000), RoundedCornerShape(8.dp))
                        .padding(horizontal = 7.dp, vertical = 3.dp))
            }
        }
        Text(entry.title, color = Cream, fontWeight = FontWeight.SemiBold, fontSize = 13.sp,
            maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(entry.artist, color = Muted, fontSize = 11.sp, maxLines = 1,
            overflow = TextOverflow.Ellipsis)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(entry.occasion.ifBlank { entry.category }, color = Gold, fontSize = 10.sp,
                maxLines = 1, modifier = Modifier.weight(1f))
            IconButton(onClick = onFavorite, modifier = Modifier.size(32.dp)) {
                Icon(if (favorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                    "علاقه‌مندی", tint = if (favorite) Gold else Muted, modifier = Modifier.size(19.dp))
            }
            IconButton(onClick = if (entry.playable) onPlay else onOpen, modifier = Modifier.size(32.dp)) {
                Icon(if (entry.playable) Icons.Filled.PlayCircle else Icons.Outlined.Info,
                    "پخش یا جزئیات", tint = Jade, modifier = Modifier.size(27.dp))
            }
        }
    }
}

@Composable
private fun GlassHero(hero: Showcase, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(modifier.height(194.dp).clip(RoundedCornerShape(27.dp))
        .background(Brush.linearGradient(listOf(hero.first, hero.second, Ink)))
        .border(1.dp, Brush.linearGradient(listOf(Gold.copy(alpha = .65f),
            Color.White.copy(alpha = .12f))), RoundedCornerShape(27.dp))
        .clickable(onClick = onClick)) {
        IslamicPattern(Modifier.fillMaxSize())
        Column(Modifier.align(Alignment.CenterStart).padding(18.dp).fillMaxWidth(.85f),
            verticalArrangement = Arrangement.spacedBy(9.dp)) {
            Text(hero.label, color = Gold, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Text(hero.title, color = Cream, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
            Text(hero.subtitle, color = Cream.copy(alpha = .77f), fontSize = 11.sp, maxLines = 2)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.PlayCircle, null, tint = Jade)
                Spacer(Modifier.width(6.dp))
                Text("کاوش مجموعه", fontSize = 12.sp, color = Cream)
            }
        }
    }
}

@Composable
private fun IslamicPattern(modifier: Modifier) {
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        for (i in 0..4) {
            val gap = 12f + i * 17f
            val x = w * .70f - i * 4f
            val r = h * .55f - gap
            if (r > 0f) {
                val p = Path().apply {
                    moveTo(x - r, h)
                    lineTo(x - r, h * .48f)
                    cubicTo(x - r, h * .13f, x + r, h * .13f, x + r, h * .48f)
                    lineTo(x + r, h)
                }
                drawPath(p, Gold.copy(alpha = .15f - i * .02f), style = Stroke(width = 2f))
            }
        }
        drawCircle(Jade.copy(alpha = .08f), radius = w * .30f,
            center = Offset(w * .88f, h * .05f))
    }
}
