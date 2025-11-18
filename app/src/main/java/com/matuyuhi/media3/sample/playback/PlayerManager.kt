package com.matuyuhi.media3.sample.playback

import android.content.Context
import android.media.AudioDeviceCallback
import android.media.AudioManager
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.matuyuhi.media3.sample.data.QueueRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlayerManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val queueRepository: QueueRepository
) {
    private lateinit var player: ExoPlayer
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentPosition = MutableStateFlow(0L)
    val currentPosition: StateFlow<Long> = _currentPosition.asStateFlow()

    private val _bufferedPosition = MutableStateFlow(0L)
    val bufferedPosition: StateFlow<Long> = _bufferedPosition.asStateFlow()

    private val _currentMediaItem = MutableStateFlow<MediaItem?>(null)
    val currentMediaItem: StateFlow<MediaItem?> = _currentMediaItem.asStateFlow()

    private var saveJob: Job? = null

    fun initialize(exoPlayer: ExoPlayer) {
        player = exoPlayer

        // AudioFocus設定
        player.setAudioAttributes(
            AudioAttributes.Builder()
                .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
                .setUsage(C.USAGE_MEDIA)
                .build(),
            true
        )

        // リスナー設定
        player.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _isPlaying.value = isPlaying
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                _currentMediaItem.value = mediaItem
                scheduleSaveQueueState()
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                updatePositions()
                if (playbackState == Player.STATE_READY) {
                    scheduleSaveQueueState()
                }
            }

            override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
                scheduleSaveQueueState()
            }

            override fun onRepeatModeChanged(repeatMode: Int) {
                scheduleSaveQueueState()
            }
        })

        // ポジション更新
        scope.launch {
            while (isActive) {
                if (player.isPlaying) {
                    updatePositions()
                }
                delay(1000)
            }
        }

        // BecomingNoisy対応
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        audioManager.registerAudioDeviceCallback(object : AudioDeviceCallback() {
            override fun onAudioDevicesRemoved(removedDevices: Array<out android.media.AudioDeviceInfo>?) {
                if (player.isPlaying) {
                    player.pause()
                }
            }
        }, null)
    }

    private fun updatePositions() {
        _currentPosition.value = player.currentPosition
        _bufferedPosition.value = player.bufferedPosition
    }

    suspend fun restoreQueue() {
        val state = queueRepository.loadQueueState() ?: return

        withContext(Dispatchers.Main) {
            val mediaItems = state.mediaIds.map { id ->
                MediaItem.Builder()
                    .setMediaId(id)
                    .build()
            }

            if (mediaItems.isNotEmpty()) {
                player.setMediaItems(mediaItems, state.currentIndex, state.positionMs)
                player.shuffleModeEnabled = state.shuffleEnabled
                player.repeatMode = state.repeatMode
                player.prepare()
            }
        }
    }

    fun saveQueueState() {
        scope.launch {
            val mediaIds = (0 until player.mediaItemCount).map {
                player.getMediaItemAt(it).mediaId
            }

            queueRepository.saveQueueState(
                QueueRepository.QueueState(
                    mediaIds = mediaIds,
                    currentIndex = player.currentMediaItemIndex.coerceAtLeast(0),
                    positionMs = player.currentPosition,
                    shuffleEnabled = player.shuffleModeEnabled,
                    repeatMode = player.repeatMode
                )
            )
        }
    }

    private fun scheduleSaveQueueState() {
        saveJob?.cancel()
        saveJob = scope.launch {
            delay(1000) // デバウンス
            saveQueueState()
        }
    }
}