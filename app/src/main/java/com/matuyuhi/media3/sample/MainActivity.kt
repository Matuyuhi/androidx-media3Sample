package com.matuyuhi.media3.sample

import android.content.ComponentName
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.matuyuhi.media3.sample.data.HistoryRepository
import com.matuyuhi.media3.sample.data.MediaCatalog
import com.matuyuhi.media3.sample.playback.PlaybackService
import com.matuyuhi.media3.sample.playback.PlayerManager
import com.matuyuhi.media3.sample.ui.PlaybackHistorySheet
import com.matuyuhi.media3.sample.ui.VideoSurface
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.guava.await
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity: ComponentActivity() {
    @Inject
    lateinit var mediaCatalog: MediaCatalog

    @Inject
    lateinit var historyRepository: HistoryRepository

    @Inject
    lateinit var playerManager: PlayerManager

    @RequiresApi(Build.VERSION_CODES.P)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MaterialTheme {
                MainScreen(
                    mediaCatalog = mediaCatalog,
                    historyRepository = historyRepository,
                    playerManager = playerManager
                )
            }
        }
    }
}

@Composable
private fun MainScreen(
    mediaCatalog: MediaCatalog,
    historyRepository: HistoryRepository,
    playerManager: PlayerManager
) {
    val context = LocalContext.current
    val sessionToken = remember {
        SessionToken(
            context,
            ComponentName(context, PlaybackService::class.java)
        )
    }

    var controller by remember { mutableStateOf<MediaController?>(null) }
    var isHistorySheetVisible by remember { mutableStateOf(false) }

    // MediaControllerの初期化
    LaunchedEffect(sessionToken) {
        controller = MediaController.Builder(context, sessionToken).buildAsync().await()
        // メディアカタログから全てのメディアアイテムを取得して追加
        mediaCatalog.getAllMediaItems().forEach { mediaItem ->
            controller?.addMediaItem(mediaItem)
        }
        controller?.prepare()
        controller?.playWhenReady = true
    }

    // キュー情報を取得
    var upcomingQueue by remember { mutableStateOf(emptyList<androidx.media3.common.MediaItem>()) }
    val currentMediaItem by playerManager.currentMediaItem.collectAsState()

    // キューを定期的に更新
    LaunchedEffect(controller) {
        while (true) {
            controller?.let { ctrl ->
                val currentIndex = ctrl.currentMediaItemIndex
                val totalItems = ctrl.mediaItemCount
                val upcoming = mutableListOf<androidx.media3.common.MediaItem>()

                // 現在のアイテムより後のアイテムを取得
                for (i in (currentIndex + 1) until totalItems) {
                    upcoming.add(ctrl.getMediaItemAt(i))
                }
                upcomingQueue = upcoming
            }
            delay(1000) // 1秒ごとに更新
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        floatingActionButton = {
            FloatingActionButton(
                onClick = { isHistorySheetVisible = true },
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            ) {
                Icon(
                    imageVector = Icons.Default.QueueMusic,
                    contentDescription = "再生履歴とキューを表示"
                )
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize()) {
            VideoSurface(
                modifier = Modifier.fillMaxSize(),
                sessionToken = sessionToken
            )

            // BottomSheet
            PlaybackHistorySheet(
                isVisible = isHistorySheetVisible,
                onDismiss = { isHistorySheetVisible = false },
                historyRepository = historyRepository,
                mediaCatalog = mediaCatalog,
                currentMediaItem = currentMediaItem,
                upcomingQueue = upcomingQueue,
                onMediaItemClick = { mediaItem ->
                    // メディアアイテムをクリックしたときの処理
                    controller?.let { ctrl ->
                        // キュー内の該当アイテムを探して再生
                        for (i in 0 until ctrl.mediaItemCount) {
                            if (ctrl.getMediaItemAt(i).mediaId == mediaItem.mediaId) {
                                ctrl.seekToDefaultPosition(i)
                                ctrl.play()
                                break
                            }
                        }
                    }
                    isHistorySheetVisible = false
                }
            )
        }
    }
}