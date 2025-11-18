package com.matuyuhi.media3.sample.playback

import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.analytics.AnalyticsListener
import androidx.media3.exoplayer.analytics.PlaybackStats
import androidx.media3.exoplayer.analytics.PlaybackStatsListener
import com.matuyuhi.media3.sample.data.HistoryRepository
import kotlinx.coroutines.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HistoryRecorder @Inject constructor(
    private val historyRepository: HistoryRepository
) {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var currentMediaId: String? = null
    private var sessionStartTime: Long = 0
    private var totalPlayTime: Long = 0

    fun attachToPlayer(player: ExoPlayer) {
        val statsListener = PlaybackStatsListener(true) { eventTime, playbackStats ->
            handlePlaybackStats(eventTime, playbackStats)
        }

        player.addAnalyticsListener(statsListener)

        player.addListener(object : Player.Listener {
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                if (reason == Player.MEDIA_ITEM_TRANSITION_REASON_SEEK) return

                // 前のアイテムの記録
                recordCurrentItem(
                    when (reason) {
                        Player.MEDIA_ITEM_TRANSITION_REASON_AUTO -> HistoryRepository.CompletionReason.COMPLETED
                        else -> HistoryRepository.CompletionReason.SKIPPED
                    }
                )

                // 新しいアイテムの開始
                mediaItem?.let {
                    currentMediaId = it.mediaId
                    sessionStartTime = System.currentTimeMillis()
                    totalPlayTime = 0
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                recordCurrentItem(HistoryRepository.CompletionReason.ERROR)
            }
        })
    }

    private fun handlePlaybackStats(
        eventTime: AnalyticsListener.EventTime,
        playbackStats: PlaybackStats
    ) {
        totalPlayTime = playbackStats.totalPlayTimeMs
    }

    private fun recordCurrentItem(reason: HistoryRepository.CompletionReason) {
        val mediaId = currentMediaId ?: return
        val playDuration = totalPlayTime

        // 15秒以上再生 or 完走で記録
        if (playDuration >= 15000 || reason == HistoryRepository.CompletionReason.COMPLETED) {
            scope.launch {
                historyRepository.addEntry(
                    HistoryRepository.HistoryEntry(
                        mediaId = mediaId,
                        timestamp = sessionStartTime,
                        playDurationMs = playDuration,
                        completionReason = reason
                    )
                )
            }
        }

        currentMediaId = null
        totalPlayTime = 0
    }
}