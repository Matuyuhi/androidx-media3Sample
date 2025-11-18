package com.matuyuhi.media3.sample.playback

import android.util.Log
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
    private val TAG = "HistoryRecorder"

    private var currentMediaId: String? = null
    private var sessionStartTime: Long = 0
    private var totalPlayTime: Long = 0
    private var totalDuration: Long = 0

    companion object {
        private const val MIN_PLAY_DURATION_MS = 15_000L // 15秒
        private const val MIN_COMPLETION_RATIO = 0.3f // 30%
    }

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
                    totalDuration = player.duration.takeIf { dur -> dur > 0 } ?: 0
                    Log.d(TAG, "Started tracking: ${it.mediaId}, duration: ${totalDuration}ms")
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                Log.e(TAG, "Player error occurred", error)
                recordCurrentItem(HistoryRepository.CompletionReason.ERROR)
            }
        })
    }

    private fun handlePlaybackStats(
        eventTime: AnalyticsListener.EventTime,
        playbackStats: PlaybackStats
    ) {
        totalPlayTime = playbackStats.totalPlayTimeMs
        // durationの更新
        if (totalDuration <= 0 && eventTime.eventPlaybackPositionMs > 0) {
            try {
                if (!eventTime.timeline.isEmpty) {
                    val window = androidx.media3.common.Timeline.Window()
                    totalDuration = eventTime.timeline.getWindow(0, window).durationMs
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to get duration from timeline", e)
            }
        }
    }

    private fun shouldRecord(
        playDuration: Long,
        totalDuration: Long,
        reason: HistoryRepository.CompletionReason
    ): Boolean {
        return when {
            // 完走した場合は必ず記録
            reason == HistoryRepository.CompletionReason.COMPLETED -> true

            // 15秒以上再生した場合は記録
            playDuration >= MIN_PLAY_DURATION_MS -> true

            // 総再生時間の30%以上再生した場合は記録（短い曲対応）
            totalDuration > 0 && (playDuration.toFloat() / totalDuration) >= MIN_COMPLETION_RATIO -> {
                Log.d(TAG, "Recording based on completion ratio: ${playDuration.toFloat() / totalDuration}")
                true
            }

            else -> {
                Log.d(TAG, "Not recording: duration=${playDuration}ms, total=${totalDuration}ms, reason=$reason")
                false
            }
        }
    }

    private fun recordCurrentItem(reason: HistoryRepository.CompletionReason) {
        val mediaId = currentMediaId ?: return
        val playDuration = totalPlayTime

        if (shouldRecord(playDuration, totalDuration, reason)) {
            scope.launch {
                try {
                    historyRepository.addEntry(
                        HistoryRepository.HistoryEntry(
                            mediaId = mediaId,
                            timestamp = sessionStartTime,
                            playDurationMs = playDuration,
                            completionReason = reason,
                            totalDurationMs = totalDuration
                        )
                    )
                    Log.d(TAG, "Recorded history: $mediaId, ${playDuration}ms, $reason")
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to record history", e)
                }
            }
        }

        currentMediaId = null
        totalPlayTime = 0
        totalDuration = 0
    }

    /**
     * アプリ終了時など、現在再生中のアイテムを強制的に記録する
     */
    fun forceRecordCurrentItem() {
        Log.d(TAG, "Force recording current item")
        recordCurrentItem(HistoryRepository.CompletionReason.SKIPPED)
    }

    /**
     * クリーンアップ: Coroutine scopeをキャンセル
     */
    fun cleanup() {
        Log.d(TAG, "Cleaning up HistoryRecorder")
        forceRecordCurrentItem()
        scope.cancel()
    }
}
