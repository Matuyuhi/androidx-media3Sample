package com.matuyuhi.media3.sample.ui

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import androidx.media3.ui.PlayerView
import kotlinx.coroutines.guava.await

@Composable
fun VideoSurface(
    modifier: Modifier = Modifier,
    sessionToken: SessionToken,
    useStyled: Boolean = true
) {
    val context = LocalContext.current
    var controller by remember { mutableStateOf<MediaController?>(null) }
    var playerView by remember { mutableStateOf<androidx.media3.ui.PlayerView?>(null) }

    LaunchedEffect(sessionToken) {
        val future = MediaController.Builder(context, sessionToken).buildAsync()
        controller = future.await()
        playerView?.player = controller
    }

    DisposableEffect(Unit) {
        onDispose {
            controller?.release()
            controller = null
        }
    }

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            androidx.media3.ui.PlayerView(ctx).apply {
                useController = true
                playerView = this
            }
        },
        update = { view ->
            if (controller != null) {
                view.player = controller
            }
        }
    )
}