package com.lossydragon.media3.player

import android.content.ContentResolver
import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.DocumentsContract
import androidx.annotation.OptIn
import androidx.core.net.toUri
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.SimpleBasePlayer
import androidx.media3.common.util.UnstableApi
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.lossydragon.media3.R
import com.lossydragon.media3.db.XmpPreferences
import com.lossydragon.media3.model.FrameSnapshot
import com.lossydragon.media3.model.ModuleFile
import kotlin.math.abs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.helllabs.libxmp.Xmp
import org.helllabs.libxmp.model.ModInfo
import timber.log.Timber

/**
 * Media3 [SimpleBasePlayer] backed by [XmpEngine].
 * Manages queue, playback lifecycle, audio focus, and Android Auto item resolution.
 */
@OptIn(UnstableApi::class)
class XmpPlayer(
    private val context: Context,
    private val engine: XmpEngine,
    private val prefs: XmpPreferences
) : SimpleBasePlayer(Looper.getMainLooper()) {

    private var repeatMode = REPEAT_MODE_OFF
    private var shuffleModeEnabled = false
    private var hasFocus = false

    private val artworkUri: Uri by lazy {
        Uri.Builder()
            .scheme(ContentResolver.SCHEME_ANDROID_RESOURCE)
            .authority(context.packageName)
            .appendPath(context.resources.getResourceTypeName(R.drawable.icon512))
            .appendPath(context.resources.getResourceEntryName(R.drawable.icon512))
            .build()
    }

    /** Builds a [MediaItem] with placeholder metadata for initial queue population. */
    private fun ModuleFile.toMediaItem(): MediaItem =
        MediaItem.Builder()
            .setUri(uri)
            .setMediaId(uri.toString())
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(resolvedName.ifBlank { name })
                    .setArtist(resolvedType.ifBlank { extension.uppercase() })
                    .setArtworkUri(artworkUri)
                    .setIsPlayable(true)
                    .build()
            )
            .build()

    /** Builds [MediaMetadata] from libxmp after the module is loaded — includes real duration. */
    private fun ModuleFile.toRealMetadata(duration: Long): MediaMetadata =
        MediaMetadata.Builder()
            .setTitle(Xmp.getModName().ifBlank { resolvedName.ifBlank { name } })
            .setArtist(Xmp.getModType().ifBlank { resolvedType.ifBlank { extension.uppercase() } })
            .setDurationMs(duration)
            .setArtworkUri(artworkUri)
            .setIsPlayable(true)
            .build()

    @Volatile
    private var pendingSeekPositionMs: Long = -1L

    private val playlist = mutableListOf<MediaItem>()
    private val queue = mutableListOf<ModuleFile>()
    private var currentIndex = 0

    private val mainHandler = Handler(Looper.getMainLooper())
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var positionUpdateJob: Job? = null

    val frameFlow: StateFlow<FrameSnapshot?> get() = engine.frameFlow
    val isPlaying: StateFlow<Boolean> get() = engine.isPlaying
    val positionMs: StateFlow<Long> get() = engine.positionMs
    val currentSequenceFlow: StateFlow<Int> get() = engine.currentSequenceFlow

    val currentIndexFlow: StateFlow<Int>
        field = MutableStateFlow(0)
    val queueFlow: StateFlow<List<ModuleFile>>
        field = MutableStateFlow(emptyList())
    val moduleLoadedFlow: StateFlow<Int>
        field = MutableStateFlow(0)

    private var audioFocusRequest: AudioFocusRequest? = null
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    var playAllSequences: Boolean
        get() = engine.playAllSequences
        set(value) {
            engine.playAllSequences = value
        }

    fun setSequence(index: Int): Boolean = engine.setSequence(index)

    fun getSequenceDurations(): List<Int> = engine.getSequenceDurations()

    private fun requestAudioFocus() {
        if (hasFocus) return

        val request = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            .setAcceptsDelayedFocusGain(true)
            .setOnAudioFocusChangeListener { change ->
                when (change) {
                    AudioManager.AUDIOFOCUS_GAIN -> {
                        hasFocus = true
                        engine.resume()
                    }

                    AudioManager.AUDIOFOCUS_LOSS,
                    AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                        hasFocus = false
                        engine.pause()
                    }
                }
            }
            .build()

        audioFocusRequest = request

        val result = audioManager.requestAudioFocus(request)
        if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) hasFocus = true

        Timber.d("Audio focus result=$result")
    }

    /** Releases audio focus. Call from [XmpService.onDestroy]. */
    fun abandonAudioFocus() {
        audioFocusRequest?.let { audioManager.abandonAudioFocusRequest(it) }
        audioFocusRequest = null
        hasFocus = false
    }

    init {
        scope.launch {
            engine.isPlaying.collect { playing ->
                invalidateState()
                if (playing) {
                    startPositionUpdates()
                } else {
                    stopPositionUpdates()
                    if (engine.endedNaturally) mainHandler.post { advanceToNext() }
                }
            }
        }
    }

    /** Loads [files] into the queue and starts playback at [startAt]. */
    fun loadQueue(files: List<ModuleFile>, startAt: Int) {
        requestAudioFocus()

        queue.clear()
        queue.addAll(files)

        playlist.clear()
        playlist.addAll(files.map { it.toMediaItem() })

        currentIndex = startAt.coerceIn(0, playlist.lastIndex)
        queueFlow.value = files
        currentIndexFlow.value = currentIndex

        invalidateState()
        loadAndStartAt(currentIndex)
    }

    private fun loadAndStartAt(index: Int) {
        val file = queue.getOrNull(index) ?: return

        Thread {
            if (engine.load(file)) {
                val realItem = MediaItem.Builder()
                    .setUri(file.uri)
                    .setMediaId(file.uri.toString())
                    .setMediaMetadata(file.toRealMetadata(engine.durationMs.value))
                    .build()

                mainHandler.post {
                    playlist[index] = realItem
                    moduleLoadedFlow.value++
                    invalidateState()
                }

                engine.start()
                mainHandler.post { invalidateState() }
            } else {
                mainHandler.post { advanceToNext() }
            }
        }.start()
    }

    private fun navigate(to: Int) {
        currentIndex = to
        currentIndexFlow.value = currentIndex
        pendingSeekPositionMs = -1L
        invalidateState()
        loadAndStartAt(currentIndex)
    }

    private fun clearQueue() {
        playlist.clear()
        queue.clear()
        currentIndex = 0
        currentIndexFlow.value = 0
        queueFlow.value = emptyList()
        invalidateState()
    }

    private fun advanceToNext() {
        when {
            shuffleModeEnabled -> {
                val candidates = queue.indices.filter { it != currentIndex }
                if (candidates.isEmpty()) {
                    clearQueue()
                } else {
                    navigate(candidates.random())
                }
            }

            repeatMode == Player.REPEAT_MODE_ONE -> navigate(currentIndex)

            repeatMode == Player.REPEAT_MODE_ALL ->
                navigate(
                    if (currentIndex + 1 <
                        queue.size
                    ) {
                        currentIndex + 1
                    } else {
                        0
                    }
                )

            currentIndex + 1 < queue.size -> navigate(currentIndex + 1)

            else -> clearQueue()
        }
    }

    private fun advanceToPrevious() {
        when {
            shuffleModeEnabled -> {
                val candidates = queue.indices.filter { it != currentIndex }
                if (candidates.isNotEmpty()) {
                    navigate(candidates.random())
                } else {
                    navigate(currentIndex)
                }
            }

            repeatMode == Player.REPEAT_MODE_ONE -> navigate(currentIndex)

            repeatMode == Player.REPEAT_MODE_ALL -> navigate(
                if (currentIndex - 1 >=
                    0
                ) {
                    currentIndex - 1
                } else {
                    queue.lastIndex
                }
            )

            currentIndex - 1 >= 0 -> navigate(currentIndex - 1)
        }
    }

    fun next() = advanceToNext()

    fun previous() {
        if (engine.positionMs.value > 3_000L) {
            pendingSeekPositionMs = 0L
            engine.seek(0)
            invalidateState()
        } else {
            advanceToPrevious()
        }
    }

    fun jumpToIndex(index: Int) {
        if (index in queue.indices) navigate(index)
    }

    private fun startPositionUpdates() {
        positionUpdateJob?.cancel()
        positionUpdateJob = scope.launch {
            while (true) {
                delay(500L)
                if (pendingSeekPositionMs >= 0 &&
                    abs(engine.positionMs.value - pendingSeekPositionMs) < 2_000L
                ) {
                    pendingSeekPositionMs = -1L
                }
                invalidateState()
            }
        }
    }

    private fun stopPositionUpdates() {
        positionUpdateJob?.cancel()
        positionUpdateJob = null
    }

    override fun getState(): State {
        val commands = Player.Commands.Builder().addAll(
            COMMAND_PLAY_PAUSE,
            COMMAND_SEEK_IN_CURRENT_MEDIA_ITEM,
            COMMAND_SEEK_TO_MEDIA_ITEM,
            COMMAND_SEEK_TO_PREVIOUS,
            COMMAND_SEEK_TO_NEXT,
            COMMAND_GET_CURRENT_MEDIA_ITEM,
            COMMAND_GET_METADATA,
            COMMAND_GET_TIMELINE,
            COMMAND_STOP,
            COMMAND_PREPARE,
            COMMAND_SET_MEDIA_ITEM,
            COMMAND_CHANGE_MEDIA_ITEMS,
            COMMAND_SET_REPEAT_MODE,
            COMMAND_SET_SHUFFLE_MODE,
        ).build()

        val playlistItems = playlist.mapIndexed { i, item ->
            val uid = item.mediaId.ifEmpty {
                item.localConfiguration?.uri?.toString() ?: i.toString()
            }
            val durationUs = if (i == currentIndex && engine.durationMs.value > 0) {
                engine.durationMs.value * 1_000L
            } else {
                C.TIME_UNSET
            }
            MediaItemData.Builder(uid)
                .setMediaItem(item)
                .setIsSeekable(true)
                .setDurationUs(durationUs)
                .build()
        }

        val position = if (pendingSeekPositionMs >= 0) {
            pendingSeekPositionMs
        } else {
            engine.positionMs.value
        }

        return State.Builder()
            .setAvailableCommands(commands)
            .setPlaylist(playlistItems)
            .setShuffleModeEnabled(shuffleModeEnabled)
            .setRepeatMode(repeatMode)
            .setCurrentMediaItemIndex(currentIndex)
            .setPlayWhenReady(engine.isPlaying.value, PLAY_WHEN_READY_CHANGE_REASON_USER_REQUEST)
            .setPlaybackState(if (playlist.isEmpty()) STATE_IDLE else STATE_READY)
            .setContentPositionMs(position)
            .build()
    }

    override fun handleSetPlayWhenReady(playWhenReady: Boolean): ListenableFuture<*> {
        if (!playWhenReady) {
            engine.pause()
        } else if (!engine.isPlaying.value) {
            requestAudioFocus()
            engine.resume()
        }
        return Futures.immediateVoidFuture()
    }

    override fun handleSetRepeatMode(repeatMode: Int): ListenableFuture<*> {
        this.repeatMode = repeatMode
        invalidateState()
        return Futures.immediateVoidFuture()
    }

    override fun handleSetShuffleModeEnabled(shuffleModeEnabled: Boolean): ListenableFuture<*> {
        this.shuffleModeEnabled = shuffleModeEnabled
        invalidateState()
        return Futures.immediateVoidFuture()
    }

    override fun handleSeek(
        mediaItemIndex: Int,
        positionMs: Long,
        seekCommand: Int
    ): ListenableFuture<*> {
        when (seekCommand) {
            COMMAND_SEEK_TO_NEXT,
            COMMAND_SEEK_TO_NEXT_MEDIA_ITEM -> advanceToNext()

            COMMAND_SEEK_TO_PREVIOUS,
            COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM -> previous()

            else -> {
                pendingSeekPositionMs = positionMs
                engine.seek(positionMs.toInt())
                invalidateState()
            }
        }
        return Futures.immediateVoidFuture()
    }

    override fun handleStop(): ListenableFuture<*> {
        Thread { engine.stop() }.start()
        clearQueue()
        return Futures.immediateVoidFuture()
    }

    /** Android Auto — resolves selected item to full directory queue. */
    override fun handleSetMediaItems(
        mediaItems: List<MediaItem>,
        startIndex: Int,
        startPositionMs: Long
    ): ListenableFuture<*> {
        val files = mediaItems.mapNotNull { item ->
            val uri = item.localConfiguration?.uri ?: return@mapNotNull null
            ModuleFile(
                uri = uri,
                name = item.mediaMetadata.title?.toString() ?: uri.lastPathSegment ?: "Unknown",
                sizeBytes = 0L,
                extension = uri.lastPathSegment?.substringAfterLast('.') ?: "",
            )
        }
        if (files.isEmpty()) return Futures.immediateVoidFuture()

        val firstUri = files.first().uri
        val treeUri = runBlocking { prefs.getLastDirectoryUri() }?.toUri()
            ?: return Futures.immediateVoidFuture()

        val docId = DocumentsContract.getDocumentId(firstUri)
        val parentDocId = docId.substringBeforeLast('/')
        val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, parentDocId)

        val siblings = mutableListOf<ModuleFile>()
        context.contentResolver.query(
            childrenUri,
            arrayOf(
                DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                DocumentsContract.Document.COLUMN_MIME_TYPE,
            ),
            null,
            null,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
        )?.use { cursor ->
            while (cursor.moveToNext()) {
                val sibDocId = cursor.getString(0)
                val name = cursor.getString(1)
                val mime = cursor.getString(2)
                if (mime == DocumentsContract.Document.MIME_TYPE_DIR) continue
                val sibUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, sibDocId)
                if (Xmp.testFromFd(context, sibUri, ModInfo())) {
                    siblings.add(
                        ModuleFile(
                            uri = sibUri,
                            name = name,
                            sizeBytes = 0L,
                            extension = name.substringAfterLast('.', ""),
                        )
                    )
                }
            }
        }

        val resolved = siblings.ifEmpty { files }
        loadQueue(resolved, resolved.indexOfFirst { it.uri == firstUri }.coerceAtLeast(0))
        return Futures.immediateVoidFuture()
    }

    /** Releases coroutine scope and audio engine. Call from [XmpService.onDestroy]. */
    fun releaseEngine() {
        scope.cancel("Releasing Engine")
        Thread { engine.stop() }.start()
    }
}
