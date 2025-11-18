package com.matuyuhi.media3.sample

import android.content.ComponentName
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.matuyuhi.media3.sample.data.MediaCatalog
import com.matuyuhi.media3.sample.playback.PlaybackService
import com.matuyuhi.media3.sample.ui.VideoSurface
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.guava.await
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity: ComponentActivity() {
    @Inject
    lateinit var mediaCatalog: MediaCatalog

    @RequiresApi(Build.VERSION_CODES.P)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val context = LocalContext.current
            val mediaCat = mediaCatalog
            val sessionToken = remember {
                SessionToken(
                    context,
                    ComponentName(context, PlaybackService::class.java)
                )
            }

            LaunchedEffect(sessionToken) {
                val controller = MediaController.Builder(context, sessionToken).buildAsync().await()
                // メディアカタログから全てのメディアアイテムを取得して追加
                mediaCat.getAllMediaItems().forEach { mediaItem ->
                    controller.addMediaItem(mediaItem)
                }
                controller.prepare()
                controller.playWhenReady = true
            }

            VideoSurface(
                modifier = Modifier.fillMaxSize(),
                sessionToken = sessionToken
            )
        }
    }
}