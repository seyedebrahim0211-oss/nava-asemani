package ir.navaasemani.app

import android.app.DownloadManager
import android.content.ComponentName
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.foundation.Canvas
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import ir.navaasemani.app.data.*
import ir.navaasemani.app.player.PlaybackService
import ir.navaasemani.app.ui.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.max

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { NavaTheme {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                NavaApp()
            }
        } }
    }
}

private enum class Tab(val label: String) {
    HOME("خانه"), EXPLORE("جست‌وجو"), DOWNLOADS("دانلودها"), LIBRARY("کتابخانه"), ABOUT("درباره ما")
}

@Composable
private fun NavaApp() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val prefs = remember { LibraryPrefs(context) }
    val downloads = remember { Downloads(context) }
    val snackbar = remember { SnackbarHostState() }
    var items by remember { mutableStateOf(Catalog.bundled(context)) }
    var favorites by remember { mutableStateOf(prefs.favorites()) }
    var tab by remember { mutableStateOf(Tab.HOME) }
    var category by remember { mutableStateOf<String?>(null) }
    var selected by remember { mutableStateOf<AudioItem?>(null) }
    var playlistTarget by remember { mutableStateOf<AudioItem?>(null) }
    var playlistName by remember { mutableStateOf("") }
    var listVersion by remember { mutableIntStateOf(0) }
    var player by remember { mutableStateOf<MediaController?>(null) }
    var isPlaying by remember { mutableStateOf(false) }
    var currentId by remember { mutableStateOf("") }
    var position by remember { mutableLongStateOf(0L) }
    var duration by remember { mutableLongStateOf(0L) }
    var refreshing by remember { mutableStateOf(false) }

    DisposableEffect(context) {
        val token = SessionToken(context, ComponentName(context, PlaybackService::class.java))
        val future = MediaController.Builder(context, token).buildAsync()
        var listener: Player.Listener? = null
        future.addListener({
            try {
                val c = future.get()
                val l = object : Player.Listener {
                    override fun onEvents(player: Player, events: Player.Events) {
                        currentId = player.currentMediaItem?.mediaId.orEmpty()
                        isPlaying = player.isPlaying
                    }
                }
                listener = l
                c.addListener(l)
                player = c
                currentId = c.currentMediaItem?.mediaId.orEmpty()
                isPlaying = c.isPlaying
            } catch (_: Exception) { }
        }, ContextCompat.getMainExecutor(context))
        onDispose {
            player?.let { c -> listener?.let(c::removeListener) }
            MediaController.releaseFuture(future)
        }
    }
    LaunchedEffect(player) {
        while (true) {
            val c = player
            position = (c?.currentPosition ?: 0).coerceAtLeast(0)
            duration = (c?.duration ?: 0).coerceAtLeast(0)
            delay(800)
        }
    }
    LaunchedEffect(Unit) {
        refreshing = BuildConfig.CATALOG_URL.startsWith("https://")
        Catalog.refresh()?.let { remote -> if (remote.isNotEmpty()) items = remote }
        refreshing = false
    }

    fun play(item: AudioItem) {
        val controller = player
        if (!item.playable) {
            scope.launch { snackbar.showSnackbar("برای این نمونه هنوز فایل صوتی مجاز ثبت نشده است") }
            return
        }
        if (controller == null) {
            scope.launch { snackbar.showSnackbar("پخش‌کننده در حال آماده‌شدن است؛ دوباره تلاش کنید") }
            return
        }
        val queue = items.filter { it.playable }
        val media = queue.map { entry ->
            val url = downloads.downloadedUri(entry)?.toString() ?: entry.streamUrl
            MediaItem.Builder().setMediaId(entry.id).setUri(url).setMediaMetadata(
                MediaMetadata.Builder().setTitle(entry.title).setArtist(entry.artist).build()
            ).build()
        }
        val index = queue.indexOfFirst { it.id == item.id }.coerceAtLeast(0)
        controller.setMediaItems(media, index, 0)
        controller.prepare()
        controller.play()
        currentId = item.id
    }

    fun download(item: AudioItem) {
        val started = downloads.request(item)
        listVersion++
        scope.launch { snackbar.showSnackbar(if (started) "فایل به صف دانلود اندروید اضافه شد" else "این اثر مجوز دانلود یا لینک معتبر ندارد") }
    }

    fun refresh() {
        scope.launch {
            refreshing = true
            val remote = Catalog.refresh()
            if (!remote.isNullOrEmpty()) {
                items = remote
                snackbar.showSnackbar("فهرست آثار به‌روزرسانی شد")
            } else snackbar.showSnackbar("فهرست آنلاین در دسترس نیست؛ نسخه محلی نمایش داده می‌شود")
            refreshing = false
        }
    }

    val currentItem = items.firstOrNull { it.id == currentId }
    BoxWithConstraints(Modifier.fillMaxSize().background(Ink)) {
        val wide = maxWidth >= 720.dp
        Row(Modifier.fillMaxSize()) {
            if (wide) NavigationRail(containerColor = Panel, header = {
                Icon(Icons.Default.GraphicEq, null, tint = Gold, modifier = Modifier.padding(16.dp).size(30.dp))
            }) {
                Tab.entries.forEach { entry ->
                    NavigationRailItem(selected = tab == entry, onClick = { tab = entry }, icon = {
                        Icon(tabIcon(entry), entry.label)
                    }, label = { Text(entry.label) }, alwaysShowLabel = true)
                }
            }
            Scaffold(
                modifier = Modifier.fillMaxSize(),
                containerColor = Ink,
                snackbarHost = { SnackbarHost(snackbar) },
                bottomBar = {
                    Column {
                        if (currentItem != null) MiniPlayer(
                            currentItem, isPlaying, position, duration,
                            onToggle = { if (isPlaying) player?.pause() else player?.play() },
                            onExpand = { selected = currentItem }
                        )
                        if (!wide) NavigationBar(containerColor = Panel, tonalElevation = 0.dp) {
                            Tab.entries.forEach { entry ->
                                NavigationBarItem(
                                    selected = tab == entry,
                                    onClick = { tab = entry },
                                    icon = { Icon(tabIcon(entry), contentDescription = entry.label) },
                                    label = { Text(entry.label, fontSize = 10.sp) },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = Jade, selectedTextColor = Jade,
                                        indicatorColor = PanelLight, unselectedIconColor = Muted,
                                        unselectedTextColor = Muted
                                    )
                                )
                            }
                        }
                    }
                }
            ) { padding ->
                Box(Modifier.padding(padding).fillMaxSize().widthIn(max = 1160.dp)) {
                    when (tab) {
                        Tab.HOME -> HomeScreen(items, refreshing,
                            onRefresh = ::refresh,
                            onCategory = { category = it; tab = Tab.EXPLORE },
                            onItem = { selected = it }, onPlay = ::play,
                            onFavorite = { favorites = prefs.toggle(it.id) }, favorites = favorites)
                        Tab.EXPLORE -> ExploreScreen(items, category,
                            onCategory = { category = it }, onItem = { selected = it },
                            onPlay = ::play, favorites = favorites,
                            onFavorite = { favorites = prefs.toggle(it.id) })
                        Tab.DOWNLOADS -> DownloadsScreen(items, downloads, listVersion,
                            onPlay = ::play, onItem = { selected = it }, onRefresh = { listVersion++ })
                        Tab.LIBRARY -> LibraryScreen(items, prefs, favorites, listVersion,
                            onPlay = ::play, onItem = { selected = it }, onRemove = { entry, playlist ->
                                if (playlist == null) favorites = prefs.toggle(entry.id)
                                else { prefs.removeFromPlaylist(playlist, entry.id); listVersion++ }
                            })
                        Tab.ABOUT -> AboutScreen(onRefresh = ::refresh, refreshing = refreshing)
                    }
                }
            }
        }
    }
    if (selected != null) {
        val item = selected!!
        ModalBottomSheet(onDismissRequest = { selected = null }, containerColor = Panel,
            dragHandle = { BottomSheetDefaults.DragHandle(color = Gold) }) {
            DetailSheet(item, item.id in favorites,
                onPlay = { play(item) },
                onDownload = { download(item) },
                onFavorite = { favorites = prefs.toggle(item.id) },
                onPlaylist = { playlistTarget = item },
                onSource = {
                    if (item.sourceUrl.isNotEmpty()) {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(item.sourceUrl)))
                    }
                })
        }
    }
    if (playlistTarget != null) {
        AlertDialog(
            onDismissRequest = { playlistTarget = null },
            title = { Text("افزودن به فهرست پخش") },
            text = { Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(playlistName, { playlistName = it }, label = { Text("نام فهرست جدید یا موجود") }, singleLine = true)
                prefs.playlistNames().forEach { name ->
                    TextButton(onClick = {
                        prefs.addToPlaylist(name, playlistTarget!!.id)
                        listVersion++; playlistTarget = null
                    }) { Text(name) }
                }
            } },
            confirmButton = { TextButton(onClick = {
                prefs.addToPlaylist(playlistName, playlistTarget!!.id)
                playlistName = ""; listVersion++; playlistTarget = null
            }, enabled = playlistName.isNotBlank()) { Text("ذخیره") } },
            dismissButton = { TextButton(onClick = { playlistTarget = null }) { Text("انصراف") } }
        )
    }
}

@Composable
private fun tabIcon(tab: Tab) = when (tab) {
    Tab.HOME -> Icons.Outlined.Home
    Tab.EXPLORE -> Icons.Outlined.Search
    Tab.DOWNLOADS -> Icons.Outlined.FileDownload
    Tab.LIBRARY -> Icons.Outlined.LibraryMusic
    Tab.ABOUT -> Icons.Outlined.Info
}

@Composable
private fun Header(title: String, subtitle: String = "", action: (@Composable () -> Unit)? = null) {
    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween) {
        Column { Text(title, style = MaterialTheme.typography.titleLarge, color = Cream)
            if (subtitle.isNotEmpty()) Text(subtitle, color = Muted, fontSize = 12.sp) }
        action?.invoke()
    }
}

@Composable
private fun Hero(onExplore: () -> Unit) {
    Box(Modifier.fillMaxWidth().height(206.dp).clip(RoundedCornerShape(26.dp))
        .background(Brush.linearGradient(listOf(Color(0xFF0A4D50), Panel, Ink)))) {
        MihrabPattern(Modifier.align(Alignment.CenterStart).size(225.dp), Gold.copy(alpha = .20f))
        Column(Modifier.align(Alignment.CenterEnd).padding(horizontal = 23.dp).widthIn(max = 300.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("بِسْمِ اللّهِ الرَّحْمٰنِ الرَّحِيمِ", color = Gold, fontSize = 12.sp)
            Text("هر نوا، راهی به نور", fontWeight = FontWeight.Bold, color = Cream, fontSize = 27.sp,
                lineHeight = 38.sp)
            Text("آرشیو مداحی، روضه، سخنرانی، دعا، مناجات و قرآن", color = Cream.copy(alpha = .8f), fontSize = 12.sp)
            FilledTonalButton(onClick = onExplore,
                colors = ButtonDefaults.filledTonalButtonColors(containerColor = Jade, contentColor = Ink)) {
                Icon(Icons.Default.Search, null, Modifier.size(17.dp)); Spacer(Modifier.width(5.dp)); Text("کاوش در نواها")
            }
        }
    }
}

@Composable
private fun MihrabPattern(modifier: Modifier = Modifier, color: Color = Gold) {
    Canvas(modifier) {
        val w = size.width; val h = size.height
        for (i in 0..3) {
            val inset = 12f + i * 17f
            val p = Path().apply {
                moveTo(inset, h - inset)
                lineTo(inset, h * .40f)
                cubicTo(inset, h * .20f, w * .32f, h * .19f, w * .50f, h * .04f + inset)
                cubicTo(w * .68f, h * .19f, w - inset, h * .20f, w - inset, h * .40f)
                lineTo(w - inset, h - inset)
            }
            drawPath(p, color.copy(alpha = .6f - i * .11f), style = Stroke(width = 2.2f))
        }
        drawCircle(color.copy(alpha = .25f), radius = w * .035f, center = Offset(w * .5f, h * .44f))
    }
}

private fun categoryIcon(c: String) = when (c) {
    "مداحی" -> Icons.Outlined.Mic
    "روضه" -> Icons.Outlined.FavoriteBorder
    "سخنرانی" -> Icons.Outlined.RecordVoiceOver
    "ادعیه" -> Icons.Outlined.MenuBook
    "مناجات" -> Icons.Outlined.Nightlight
    else -> Icons.Outlined.AutoStories
}

@Composable
private fun CategoryTile(category: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Column(modifier.clip(RoundedCornerShape(18.dp)).background(Panel).clickable(onClick = onClick).padding(13.dp),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(9.dp)) {
        Box(Modifier.size(54.dp).clip(CircleShape).background(PanelLight), contentAlignment = Alignment.Center) {
            Icon(categoryIcon(category), null, tint = if (category == "قرآن") Gold else Jade, modifier = Modifier.size(29.dp))
        }
        Text(category, fontSize = 13.sp, fontWeight = FontWeight.Medium, maxLines = 1)
    }
}

@Composable
private fun CategoryGrid(onClick: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        CATEGORIES.chunked(3).forEach { group ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                group.forEach { CategoryTile(it, Modifier.weight(1f), onClick = { onClick(it) }) }
            }
        }
    }
}

@Composable
private fun AudioRow(item: AudioItem, favorite: Boolean, onSelect: () -> Unit,
                     onPlay: () -> Unit, onFavorite: () -> Unit) {
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(17.dp))
        .background(Panel).clickable(onClick = onSelect).padding(11.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(Modifier.size(56.dp).clip(RoundedCornerShape(13.dp))
            .background(Brush.linearGradient(listOf(Color(0xFF124C4D), PanelLight))), contentAlignment = Alignment.Center) {
            Icon(categoryIcon(item.category), null, tint = Gold, modifier = Modifier.size(28.dp))
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text(item.title, fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("${item.artist}  •  ${item.category}", fontSize = 11.sp,
                color = Muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (!item.playable) Text("نمونه بدون فایل صوتی", fontSize = 10.sp, color = Gold)
        }
        IconButton(onClick = onFavorite, modifier = Modifier.size(33.dp)) {
            Icon(if (favorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder, "علاقه‌مندی",
                tint = if (favorite) Gold else Muted, modifier = Modifier.size(19.dp))
        }
        IconButton(onClick = onPlay, modifier = Modifier.size(40.dp)) {
            Icon(if (item.playable) Icons.Filled.PlayCircle else Icons.Outlined.Info,
                "پخش", tint = Jade, modifier = Modifier.size(33.dp))
        }
    }
}

@Composable
private fun HomeScreen(items: List<AudioItem>, refreshing: Boolean,
                       onRefresh: () -> Unit, onCategory: (String) -> Unit,
                       onItem: (AudioItem) -> Unit, onPlay: (AudioItem) -> Unit,
                       onFavorite: (AudioItem) -> Unit, favorites: Set<String>) {
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { Header("نوای آسمانی", "کتابخانه صوتی معارف و نواهای آسمانی") {
            IconButton(onClick = onRefresh, enabled = !refreshing) { Icon(Icons.Default.Refresh, "به‌روزرسانی", tint = Jade) }
        } }
        item { Hero(onExplore = { onCategory("مداحی") }) }
        item { Header("دسته‌بندی‌ها", "هر آنچه برای شنیدن نیاز دارید") }
        item { CategoryGrid(onClick = onCategory) }
        item { Header("تازه‌ترین نواها", "آثار نمونه و فهرست تأییدشده") }
        items(items, key = { it.id }) { entry ->
            AudioRow(entry, entry.id in favorites, { onItem(entry) }, { onPlay(entry) }, { onFavorite(entry) })
        }
        item { Spacer(Modifier.height(12.dp)) }
    }
}

@Composable
private fun ExploreScreen(items: List<AudioItem>, preselected: String?,
                          onCategory: (String?) -> Unit, onItem: (AudioItem) -> Unit,
                          onPlay: (AudioItem) -> Unit, favorites: Set<String>,
                          onFavorite: (AudioItem) -> Unit) {
    var query by remember { mutableStateOf("") }
    val filtered = remember(query, preselected, items) {
        items.filter { entry ->
            (preselected == null || entry.category == preselected) &&
            (query.isBlank() || listOf(entry.title, entry.artist, entry.occasion, entry.description, entry.tags, entry.category)
                .any { it.contains(query.trim(), ignoreCase = true) })
        }
    }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Header("جست‌وجو و کاوش", "در میان شش دسته محتوای مذهبی") }
        item {
            OutlinedTextField(query, { query = it }, modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("نام مداح، قاری، شعر، دعا و موضوع...", fontSize = 13.sp) },
                leadingIcon = { Icon(Icons.Default.Search, null) }, singleLine = true,
                shape = RoundedCornerShape(17.dp))
        }
        item { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            (listOf<String?>(null) + CATEGORIES).chunked(4).forEach { group ->
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    group.forEach { c -> FilterChip(
                        selected = preselected == c, onClick = { onCategory(c) },
                        label = { Text(c ?: "همه", fontSize = 11.sp) }
                    ) }
                }
            }
        } }
        item { Text("${filtered.size} اثر", color = Muted, fontSize = 12.sp) }
        if (filtered.isEmpty()) item { EmptyMessage("موردی پیدا نشد؛ عبارت دیگری جست‌وجو کنید") }
        items(filtered, key = { it.id }) { entry ->
            AudioRow(entry, entry.id in favorites, { onItem(entry) }, { onPlay(entry) }, { onFavorite(entry) })
        }
    }
}

@Composable
private fun DownloadsScreen(items: List<AudioItem>, downloads: Downloads, version: Int,
                            onPlay: (AudioItem) -> Unit, onItem: (AudioItem) -> Unit,
                            onRefresh: () -> Unit) {
    @Suppress("UNUSED_VARIABLE") val refreshSignal = version
    val downloaded = items.mapNotNull { item -> downloads.status(item)?.let { item to it } }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Header("دانلودهای من", "فایل‌های مجاز در پوشه Music/NavaAsemani") {
            IconButton(onClick = onRefresh) { Icon(Icons.Default.Refresh, "بررسی دانلودها", tint = Jade) }
        } }
        if (downloaded.isEmpty()) item { EmptyMessage("هنوز فایلی دانلود نشده است. از جزئیات آثار، دانلود را انتخاب کنید.") }
        items(downloaded, key = { it.first.id }) { (item, status) ->
            val message = when (status) {
                DownloadManager.STATUS_SUCCESSFUL -> "دانلود کامل"
                DownloadManager.STATUS_RUNNING -> "در حال دانلود"
                DownloadManager.STATUS_PENDING -> "در صف دانلود"
                DownloadManager.STATUS_PAUSED -> "دانلود متوقف شده"
                else -> "دانلود ناموفق؛ امکان تلاش دوباره در جزئیات"
            }
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(17.dp)).background(Panel).clickable { onItem(item) }.padding(14.dp)) {
                Text(item.title, fontWeight = FontWeight.Bold)
                Text(message, color = if (status == DownloadManager.STATUS_SUCCESSFUL) Jade else Gold, fontSize = 12.sp)
                if (status == DownloadManager.STATUS_SUCCESSFUL) TextButton(onClick = { onPlay(item) }) { Text("پخش آفلاین") }
            }
        }
    }
}

@Composable
private fun LibraryScreen(items: List<AudioItem>, prefs: LibraryPrefs, favorites: Set<String>, version: Int,
                          onPlay: (AudioItem) -> Unit, onItem: (AudioItem) -> Unit, onRemove: (AudioItem, String?) -> Unit) {
    @Suppress("UNUSED_VARIABLE") val refreshSignal = version
    var showPlaylist by remember { mutableStateOf<String?>(null) }
    val saved = if (showPlaylist == null) items.filter { it.id in favorites }
                else items.filter { it.id in prefs.playlist(showPlaylist!!) }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Header("کتابخانه من", "علاقه‌مندی‌ها و فهرست‌های شخصی") }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("فهرست‌ها", fontWeight = FontWeight.Bold, color = Gold)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilterChip(selected = showPlaylist == null, onClick = { showPlaylist = null }, label = { Text("محبوب‌ها") })
                    prefs.playlistNames().take(3).forEach { name ->
                        FilterChip(selected = showPlaylist == name, onClick = { showPlaylist = name },
                            label = { Text(name.take(12)) })
                    }
                }
                if (prefs.playlistNames().size > 3) Text("فهرست‌های بیشتر از بخش افزودن به فهرست در جزئیات آثار قابل انتخاب هستند", fontSize = 11.sp, color = Muted)
            }
        }
        if (saved.isEmpty()) item { EmptyMessage("این فهرست هنوز خالی است؛ با نشان قلب یا گزینه فهرست پخش، آثار را ذخیره کنید") }
        items(saved, key = { it.id }) { item ->
            AudioRow(item, item.id in favorites, { onItem(item) }, { onPlay(item) }, { onRemove(item, showPlaylist) })
        }
    }
}

@Composable
private fun AboutScreen(onRefresh: () -> Unit, refreshing: Boolean) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(22.dp),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(19.dp)) {
        Icon(Icons.Default.GraphicEq, null, tint = Gold, modifier = Modifier.size(76.dp))
        Text("نوای آسمانی", style = MaterialTheme.typography.titleLarge)
        Text("هر نوا، راهی به نور", color = Gold)
        Text("نسخه آزمایشی ۰.۱ — کتابخانه صوتی مذهبی با پشتیبانی از مداحی، روضه، سخنرانی، ادعیه، مناجات و قرآن.",
            textAlign = TextAlign.Center, color = Cream, lineHeight = 28.sp)
        HorizontalDivider(color = PanelLight)
        Text("آثار نمونه فقط برای نمایش ساختار برنامه هستند. برای پنج بخش نخست لازم است رسانه‌های مجاز در کاتالوگ ثبت شوند. هیچ دریافت خودکاری از سایت‌های ثالث بدون مجوز در این نسخه فعال نیست.",
            color = Muted, fontSize = 13.sp, lineHeight = 25.sp)
        Text("نمونه تلاوت‌های قرآن: Wikimedia Commons، اثر Anasskoko، مجوز CC BY-SA 4.0. برای مشاهده اطلاعات دقیق هر نمونه روی آن ضربه بزنید.",
            color = Muted, fontSize = 12.sp, lineHeight = 23.sp)
        Button(onClick = onRefresh, enabled = !refreshing) {
            Icon(Icons.Default.Refresh, null); Spacer(Modifier.width(6.dp)); Text("دریافت مجدد فهرست")
        }
        Text("سورس پروژه برای توسعه و انتشار در GitHub آماده شده است.", color = Gold, fontSize = 12.sp)
    }
}

@Composable
private fun EmptyMessage(message: String) {
    Column(Modifier.fillMaxWidth().padding(22.dp), horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Icon(Icons.Outlined.Inbox, null, tint = Gold, modifier = Modifier.size(42.dp))
        Text(message, textAlign = TextAlign.Center, color = Muted, lineHeight = 24.sp)
    }
}

@Composable
private fun MiniPlayer(item: AudioItem, isPlaying: Boolean, position: Long, duration: Long,
                       onToggle: () -> Unit, onExpand: () -> Unit) {
    Column(Modifier.fillMaxWidth().background(PanelLight).clickable(onClick = onExpand)) {
        LinearProgressIndicator(progress = { if (duration > 0) (position.toFloat() / duration).coerceIn(0f, 1f) else 0f },
            modifier = Modifier.fillMaxWidth().height(2.dp), color = Jade, trackColor = Ink)
        Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.GraphicEq, null, tint = Gold)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(item.title, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(item.artist, fontSize = 10.sp, color = Muted)
            }
            IconButton(onClick = onToggle) { Icon(if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow, "پخش/توقف", tint = Jade) }
        }
    }
}

@Composable
private fun DetailSheet(item: AudioItem, favorite: Boolean, onPlay: () -> Unit, onDownload: () -> Unit,
                        onFavorite: () -> Unit, onPlaylist: () -> Unit, onSource: () -> Unit) {
    Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 23.dp).padding(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(15.dp)) {
        Box(Modifier.fillMaxWidth().height(160.dp).clip(RoundedCornerShape(20.dp)).background(
            Brush.linearGradient(listOf(Color(0xFF0D5F61), PanelLight, Ink))), contentAlignment = Alignment.Center) {
            MihrabPattern(Modifier.size(150.dp), Gold)
            Icon(categoryIcon(item.category), null, tint = Jade, modifier = Modifier.size(52.dp))
        }
        Text(item.category, color = Gold, fontSize = 12.sp)
        Text(item.title, style = MaterialTheme.typography.titleLarge, lineHeight = 33.sp)
        Text(item.artist + if (item.occasion.isNotBlank()) " • ${item.occasion}" else "", color = Muted)
        if (item.description.isNotBlank()) Text(item.description, color = Cream, fontSize = 13.sp, lineHeight = 24.sp)
        if (!item.playable) Text("این مورد صرفاً نمایشی است و فایل صوتی برای پخش ندارد.", color = Gold, fontSize = 12.sp)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            Button(onClick = onPlay, enabled = item.playable, modifier = Modifier.weight(1f)) {
                Icon(Icons.Default.PlayArrow, null); Spacer(Modifier.width(5.dp)); Text("پخش")
            }
            FilledTonalButton(onClick = onDownload, enabled = item.downloadAllowed, modifier = Modifier.weight(1f)) {
                Icon(Icons.Default.FileDownload, null, Modifier.size(18.dp)); Spacer(Modifier.width(5.dp)); Text("دانلود")
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onFavorite, modifier = Modifier.weight(1f)) {
                Icon(if (favorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder, null, Modifier.size(18.dp))
                Spacer(Modifier.width(5.dp)); Text("علاقه‌مندی")
            }
            OutlinedButton(onClick = onPlaylist, modifier = Modifier.weight(1f)) {
                Icon(Icons.Default.PlaylistAdd, null, Modifier.size(18.dp))
                Spacer(Modifier.width(5.dp)); Text("فهرست پخش")
            }
        }
        HorizontalDivider(color = PanelLight)
        Text("منبع و حقوق اثر", color = Gold, fontWeight = FontWeight.Bold)
        Text("${item.sourceName.ifBlank { "هنوز منبعی ثبت نشده" }}\n${item.license.ifBlank { "مجوز فایل صوتی ثبت نشده" }}",
            fontSize = 12.sp, color = Muted, lineHeight = 23.sp)
        if (item.sourceUrl.isNotBlank()) TextButton(onClick = onSource) {
            Icon(Icons.Default.OpenInNew, null, Modifier.size(18.dp)); Text("مشاهده صفحه منبع")
        }
    }
}
