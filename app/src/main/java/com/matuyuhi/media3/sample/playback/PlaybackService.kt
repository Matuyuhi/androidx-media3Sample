package com.matuyuhi.media3.sample.playback

import android.app.PendingIntent
import android.content.Intent
import android.util.Log
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.*
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import javax.inject.Inject

@AndroidEntryPoint
class PlaybackService : MediaLibraryService() {

    @Inject
    lateinit var player: ExoPlayer
    @Inject lateinit var sessionCallback: SessionCallback
    @Inject lateinit var playerManager: PlayerManager
    @Inject lateinit var historyRecorder: HistoryRecorder

    private var mediaSession: MediaLibrarySession? = null
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private val TAG = "PlaybackService"

    override fun onCreate() {
        super.onCreate()

        val sessionActivityIntent = packageManager?.getLaunchIntentForPackage(packageName)
        val sessionActivityPendingIntent = PendingIntent.getActivity(
            this, 0, sessionActivityIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        mediaSession = MediaLibrarySession.Builder(this, player, sessionCallback)
            .setSessionActivity(sessionActivityPendingIntent)
            .build()

        // 初期化とリスナー設定
        playerManager.initialize(player)
        historyRecorder.attachToPlayer(player)

        // 復元
        scope.launch {
            playerManager.restoreQueue()
        }

        // プレイヤー状態監視
        player.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_READY && player.playWhenReady) {
                    setMediaNotificationProvider(
                        DefaultMediaNotificationProvider.Builder(applicationContext)
                            .setChannelId("playback_channel")
                            .setNotificationId(1001)
                            .build()
                    )
                }
            }
        })
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaLibrarySession? {
        return mediaSession
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        Log.d(TAG, "onTaskRemoved called")

        // 現在の履歴を保存
        historyRecorder.forceRecordCurrentItem()

        val player = mediaSession?.player
        if (player?.playWhenReady == false || player?.mediaItemCount == 0) {
            stopSelf()
        }
    }

    override fun onDestroy() {
        Log.d(TAG, "onDestroy called")

        // クリーンアップ処理
        try {
            historyRecorder.cleanup()
            playerManager.cleanup()
        } catch (e: Exception) {
            Log.e(TAG, "Error during cleanup", e)
        }

        // Coroutine scopeをキャンセル
        scope.cancel()

        // MediaSessionとPlayerをリリース
        mediaSession?.run {
            player.release()
            release()
            mediaSession = null
        }

        super.onDestroy()
    }
}