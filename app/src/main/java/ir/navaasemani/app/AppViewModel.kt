package ir.navaasemani.app

import android.app.Application
import android.content.ComponentName
import android.net.Uri
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import ir.navaasemani.app.data.CatalogData
import ir.navaasemani.app.data.CatalogRepository
import ir.navaasemani.app.data.DownloadInfo
import ir.navaasemani.app.data.Downloads
import ir.navaasemani.app.data.LibraryStore
import ir.navaasemani.app.data.Track
import ir.navaasemani.app.player.PlaybackService
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class PlayerUi(
    val connected: Boolean = false,
    val currentId: String = "",
    val isPlaying: Boolean = false,
    val buffering: Boolean = false,
    val shuffle: Boolean = false,
    val repeatMode: Int = Player.REPEAT_MODE_OFF,
    val speed: Float = 1f,
    val sleepEndsAt: Long = 0L,
    val queueIds: List<String> = emptyList()
)

/** موقعیت پخش جدا نگه داشته می‌شود تا فقط نوار پیشرفت هر نیم‌ثانیه بازسازی شود، نه کل صفحه */
data class Progress(val position: Long = 0L, val duration: Long = 0L)

class AppViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = CatalogRepository(app)
    val library = LibraryStore(app)
    private val downloads = Downloads(app)

    private val _catalog = MutableStateFlow(repo.local())
    val catalog: StateFlow<CatalogData> = _catalog.asStateFlow()

    private val _refreshing = MutableStateFlow(false)
    val refreshing: StateFlow<Boolean> = _refreshing.asStateFlow()

    private val _downloads = MutableStateFlow<Map<String, DownloadInfo>>(emptyMap())
    val downloadStates: StateFlow<Map<String, DownloadInfo>> = _downloads.asStateFlow()

    private val _player = MutableStateFlow(PlayerUi())
    val player: StateFlow<PlayerUi> = _player.asStateFlow()

    private val _progress = MutableStateFlow(Progress())
    val progress: StateFlow<Progress> = _progress.asStateFlow()

    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val messages: SharedFlow<String> = _messages.asSharedFlow()

    private var controller: MediaController? = null
    private var releaseController: (() -> Unit)? = null
    private var sleepJob: Job? = null
    private var tick = 0

    private val listener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) = syncPlayer()
        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            mediaItem?.mediaId?.takeIf { it.isNotBlank() }?.let(library::addRecent)
        }
    }

    init {
        connect()
        refresh(silent = true)
        _downloads.value = downloads.queryAll()
        viewModelScope.launch {
            while (true) {
                delay(500)
                tick++
                val c = controller
                if (c != null && c.isPlaying) {
                    _progress.value = Progress(c.currentPosition.coerceAtLeast(0), c.duration.coerceAtLeast(0))
                    if (tick % 10 == 0) rememberPosition()
                }
                if (tick % 3 == 0 && _downloads.value.values.any { it.active }) {
                    val before = _downloads.value
                    val after = downloads.queryAll()
                    _downloads.value = after
                    after.forEach { (id, info) ->
                        if (info.done && before[id]?.done == false) {
                            val title = _catalog.value.track(id)?.title.orEmpty()
                            _messages.tryEmit("دانلود «$title» کامل شد")
                        }
                    }
                }
            }
        }
    }

    private fun connect() {
        val context = getApplication<Application>()
        val token = SessionToken(context, ComponentName(context, PlaybackService::class.java))
        val future = MediaController.Builder(context, token).buildAsync()
        releaseController = { MediaController.releaseFuture(future) }
        future.addListener({
            runCatching { future.get() }.getOrNull()?.let { c ->
                controller = c
                c.addListener(listener)
                syncPlayer()
            }
        }, ContextCompat.getMainExecutor(context))
    }

    private fun syncPlayer() {
        val c = controller ?: return
        val ids = (0 until c.mediaItemCount).map { c.getMediaItemAt(it).mediaId }
        _player.value = _player.value.copy(
            connected = true,
            currentId = c.currentMediaItem?.mediaId.orEmpty(),
            isPlaying = c.isPlaying,
            buffering = c.playbackState == Player.STATE_BUFFERING,
            shuffle = c.shuffleModeEnabled,
            repeatMode = c.repeatMode,
            speed = c.playbackParameters.speed,
            queueIds = ids
        )
        _progress.value = Progress(c.currentPosition.coerceAtLeast(0), c.duration.coerceAtLeast(0))
    }

    private fun rememberPosition() {
        val c = controller ?: return
        val id = c.currentMediaItem?.mediaId ?: return
        // فقط برای سخنرانی و روضه‌های طولانی (بیش از ۱۰ دقیقه) ادامه از همان‌جا
        if (c.duration > 10 * 60_000L) {
            val nearEnd = c.duration - c.currentPosition < 30_000
            library.savePosition(id, if (nearEnd) 0L else c.currentPosition)
        }
    }

    fun refresh(silent: Boolean = false) {
        if (_refreshing.value) return
        viewModelScope.launch {
            if (!repo.hasRemote) {
                if (!silent) _messages.tryEmit("آدرس آرشیو آنلاین تنظیم نشده؛ فهرست داخلی نمایش داده می‌شود")
                return@launch
            }
            _refreshing.value = true
            val remote = repo.fetchRemote()
            if (remote != null) {
                val before = _catalog.value.tracks.size
                _catalog.value = remote
                val added = remote.tracks.size - before
                if (!silent) _messages.tryEmit(if (added > 0) "$added اثر تازه به آرشیو اضافه شد" else "آرشیو به‌روز است")
            } else if (!silent) {
                _messages.tryEmit("اتصال به آرشیو آنلاین برقرار نشد؛ نسخه ذخیره‌شده نمایش داده می‌شود")
            }
            _refreshing.value = false
        }
    }

    private fun mediaItem(track: Track): MediaItem {
        val catalog = _catalog.value
        val local = downloads.localUri(track.id)
        val meta = MediaMetadata.Builder()
            .setTitle(track.title)
            .setArtist(catalog.artistLine(track))
            .setAlbumTitle(track.occasion.ifBlank { track.category })
        if (track.artwork.isNotBlank()) meta.setArtworkUri(Uri.parse(track.artwork))
        return MediaItem.Builder()
            .setMediaId(track.id)
            .setUri(local ?: Uri.parse(track.streamUrl))
            .setMediaMetadata(meta.build())
            .build()
    }

    /** اگر اثر لینک مستقیم نداشته باشد false برمی‌گرداند تا صفحه ناشر باز شود. */
    fun play(track: Track, queue: List<Track> = listOf(track)): Boolean {
        val localAvailable = _downloads.value[track.id]?.done == true
        if (!track.playable && !localAvailable) return false
        val c = controller
        if (c == null) {
            _messages.tryEmit("پخش‌کننده در حال آماده‌شدن است؛ چند لحظه بعد دوباره بزنید")
            return true
        }
        if (_player.value.currentId == track.id && c.mediaItemCount > 0) {
            if (!c.isPlaying) c.play()
            return true
        }
        rememberPosition()
        val list = queue.filter { it.playable || _downloads.value[it.id]?.done == true }
            .let { if (it.any { t -> t.id == track.id }) it else listOf(track) + it }
        val index = list.indexOfFirst { it.id == track.id }.coerceAtLeast(0)
        val resume = library.state.value.positions[track.id] ?: 0L
        c.setMediaItems(list.map { mediaItem(it) }, index, resume)
        c.prepare()
        c.play()
        library.addRecent(track.id)
        return true
    }

    fun togglePlay() {
        val c = controller ?: return
        if (c.isPlaying) {
            c.pause()
            rememberPosition()
        } else {
            if (c.playbackState == Player.STATE_ENDED) c.seekTo(0)
            c.play()
        }
    }

    fun next() { controller?.seekToNextMediaItem() }
    fun previous() { controller?.seekToPrevious() }
    fun seekTo(ms: Long) {
        controller?.seekTo(ms)
        _progress.value = _progress.value.copy(position = ms)
    }
    fun seekBy(deltaMs: Long) {
        val c = controller ?: return
        seekTo((c.currentPosition + deltaMs).coerceIn(0, c.duration.coerceAtLeast(0)))
    }
    fun toggleShuffle() { controller?.let { it.shuffleModeEnabled = !it.shuffleModeEnabled } }
    fun cycleRepeat() {
        val c = controller ?: return
        c.repeatMode = when (c.repeatMode) {
            Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
            Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
            else -> Player.REPEAT_MODE_OFF
        }
    }
    fun setSpeed(speed: Float) {
        controller?.setPlaybackSpeed(speed)
        _messages.tryEmit("سرعت پخش: ${speed}x")
    }

    fun setSleepTimer(minutes: Int) {
        sleepJob?.cancel()
        if (minutes <= 0) {
            _player.value = _player.value.copy(sleepEndsAt = 0L)
            _messages.tryEmit("زمان‌سنج خواب خاموش شد")
            return
        }
        val endsAt = System.currentTimeMillis() + minutes * 60_000L
        _player.value = _player.value.copy(sleepEndsAt = endsAt)
        _messages.tryEmit("پخش پس از $minutes دقیقه متوقف می‌شود")
        sleepJob = viewModelScope.launch {
            delay(minutes * 60_000L)
            controller?.pause()
            _player.value = _player.value.copy(sleepEndsAt = 0L)
        }
    }

    fun download(track: Track) {
        val ok = downloads.request(track, _catalog.value.artistLine(track))
        _downloads.value = downloads.queryAll()
        _messages.tryEmit(
            if (ok) "«${track.title}» به صف دانلود اضافه شد"
            else "ناشر برای این اثر لینک دانلود مستقیم منتشر نکرده است"
        )
    }

    fun removeDownload(trackId: String) {
        downloads.remove(trackId)
        _downloads.value = downloads.queryAll()
        _messages.tryEmit("فایل حذف شد")
    }

    fun toggleFavorite(trackId: String) {
        val added = library.toggleFavorite(trackId)
        _messages.tryEmit(if (added) "به علاقه‌مندی‌ها اضافه شد" else "از علاقه‌مندی‌ها حذف شد")
    }

    fun toggleFollow(maddahId: String) {
        val added = library.toggleFollow(maddahId)
        val name = _catalog.value.maddah(maddahId)?.displayName.orEmpty()
        _messages.tryEmit(if (added) "$name را دنبال می‌کنید" else "دنبال‌کردن $name لغو شد")
    }

    fun addToPlaylist(name: String, trackId: String) {
        library.addToPlaylist(name, trackId)
        _messages.tryEmit("به فهرست «${name.trim()}» اضافه شد")
    }

    fun toast(message: String) { _messages.tryEmit(message) }

    override fun onCleared() {
        rememberPosition()
        controller?.removeListener(listener)
        releaseController?.invoke()
        controller = null
        super.onCleared()
    }
}
