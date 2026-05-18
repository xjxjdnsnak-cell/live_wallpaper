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
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.example.livewallpaper.data.WallpaperSettings

@Composable
fun PreviewScreen(settings: WallpaperSettings, onBack: () -> Unit) {
    val ripples = remember { mutableStateListOf<Pair<Float, Float>>() }
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("预览 - 填充模式: ${settings.fillMode}")
        Box(Modifier.weight(1f)) {
            AndroidView(
                factory = { context ->
                    PlayerView(context).apply {
                        useController = false
                        player = ExoPlayer.Builder(context).build().also { p ->
                            p.repeatMode = Player.REPEAT_MODE_ALL
                            p.volume = if (settings.muted) 0f else 1f
                            settings.videoUri?.let { p.setMediaItem(MediaItem.fromUri(Uri.parse(it))); p.prepare(); p.play() }
                        }
                    }
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
                ripples.forEach { (x, y) -> drawCircle(Color.White.copy(alpha = 0.35f), 48f, androidx.compose.ui.geometry.Offset(x, y)) }
            }
        }
        Button(onClick = onBack, modifier = Modifier.height(48.dp)) { Text("返回") }
    }
}
