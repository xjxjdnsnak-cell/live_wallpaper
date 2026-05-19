package com.example.livewallpaper.ui

import android.net.Uri
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.example.livewallpaper.data.WallpaperSettings
import com.example.livewallpaper.wallpaper.FillMode

@Composable
fun PreviewScreen(settings: WallpaperSettings, onBack: () -> Unit) {
    val context = LocalContext.current
    val player = remember(context) {
        ExoPlayer.Builder(context).build().apply {
            repeatMode = Player.REPEAT_MODE_ALL
        }
    }
    val ripples = remember { mutableStateListOf<Pair<Float, Float>>() }

    DisposableEffect(player) {
        onDispose {
            player.pause()
            player.clearMediaItems()
            player.release()
        }
    }

    LaunchedEffect(settings.videoUri) {
        player.pause()
        player.clearMediaItems()
        settings.videoUri?.takeIf { it.isNotBlank() }?.let { uri ->
            player.setMediaItem(MediaItem.fromUri(Uri.parse(uri)))
            player.prepare()
            player.playWhenReady = true
        }
    }

    LaunchedEffect(settings.muted, settings.playbackSpeed) {
        player.volume = if (settings.muted) 0f else 1f
        player.playbackParameters = PlaybackParameters(settings.playbackSpeed.coerceAtLeast(0.1f))
    }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("预览 - 填充模式: ${settings.fillMode}")
        Box(Modifier.weight(1f)) {
            AndroidView(
                factory = { viewContext ->
                    PlayerView(viewContext).apply {
                        useController = false
                        this.player = player
                    }
                },
                update = { view ->
                    view.player = player
                    view.resizeMode = settings.fillMode.toResizeMode()
                },
                modifier = Modifier.fillMaxSize(),
            )
            Canvas(
                modifier = Modifier.fillMaxSize().pointerInput(Unit) {
                    detectTapGestures { o ->
                        ripples.add(o.x to o.y)
                        if (ripples.size > 12) ripples.removeAt(0)
                    }
                },
            ) {
                ripples.forEach { (x, y) ->
                    drawCircle(
                        Color.White.copy(alpha = 0.35f),
                        48f,
                        androidx.compose.ui.geometry.Offset(x, y),
                    )
                }
            }
        }
        Button(onClick = onBack, modifier = Modifier.height(48.dp)) { Text("返回") }
    }
}

private fun FillMode.toResizeMode(): Int = when (this) {
    FillMode.CENTER_CROP -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
    FillMode.FIT_CENTER -> AspectRatioFrameLayout.RESIZE_MODE_FIT
    FillMode.STRETCH -> AspectRatioFrameLayout.RESIZE_MODE_FILL
}
