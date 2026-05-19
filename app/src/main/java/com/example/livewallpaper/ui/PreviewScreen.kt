package com.example.livewallpaper.ui

import android.graphics.RenderEffect
import android.graphics.Shader
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.example.livewallpaper.data.TEXT_POSITION_CENTER
import com.example.livewallpaper.data.TEXT_POSITION_TOP
import com.example.livewallpaper.data.WallpaperConfig
import com.example.livewallpaper.data.WallpaperSettings
import com.example.livewallpaper.wallpaper.FillMode
import kotlinx.coroutines.delay

@Composable
fun PreviewScreen(
    settings: WallpaperSettings,
    thumbnail: Bitmap?,
    config: WallpaperConfig? = null,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val previewConfig = config ?: WallpaperConfig(
        fillMode = settings.fillMode,
        playbackSpeed = settings.playbackSpeed,
        muted = settings.muted,
        startMs = settings.startMs,
        endMs = settings.endMs,
        brightness = settings.brightness,
        contrast = settings.contrast,
        saturation = settings.saturation,
        warmth = settings.warmth,
        exposure = settings.exposure,
        scale = settings.scale,
        offsetX = settings.offsetX,
        offsetY = settings.offsetY,
        rotation = settings.rotation,
        mirror = settings.mirror,
        blur = settings.blur,
        vignette = settings.vignette,
        topGradient = settings.topGradient,
        bottomGradient = settings.bottomGradient,
        textEnabled = settings.textEnabled,
        overlayText = settings.overlayText,
        textPosition = settings.textPosition,
        textOpacity = settings.textOpacity,
        presetName = settings.presetName,
    )
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

    LaunchedEffect(settings.videoUri, previewConfig.startMs) {
        player.pause()
        player.clearMediaItems()
        settings.videoUri?.takeIf { it.isNotBlank() }?.let { uri ->
            player.setMediaItem(MediaItem.fromUri(Uri.parse(uri)))
            player.prepare()
            previewConfig.startMs?.takeIf { it > 0L }?.let { player.seekTo(it) }
            player.playWhenReady = true
        }
    }

    LaunchedEffect(previewConfig.muted, previewConfig.playbackSpeed) {
        player.volume = if (previewConfig.muted) 0f else 1f
        player.playbackParameters = PlaybackParameters(previewConfig.playbackSpeed.coerceAtLeast(0.1f))
    }

    LaunchedEffect(previewConfig.startMs, previewConfig.endMs, settings.videoUri) {
        val start = previewConfig.startMs ?: 0L
        val end = previewConfig.endMs
        if (start > 0L) player.seekTo(start)
        if (end != null && end > start) {
            while (true) {
                if (player.currentPosition >= end) player.seekTo(start)
                delay(300)
            }
        }
    }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("预览 - ${previewConfig.fillMode} / ${previewConfig.playbackSpeed}x")
        PreviewThumbnail(thumbnail)
        Box(Modifier.weight(1f)) {
            TransformedVideoLayer(previewConfig) {
                AndroidView(
                    factory = { viewContext ->
                        PlayerView(viewContext).apply {
                            useController = false
                            this.player = player
                        }
                    },
                    update = { view ->
                        view.player = player
                        view.resizeMode = previewConfig.fillMode.toResizeMode()
                        view.applyPreviewBlur(previewConfig.blur)
                    },
                    modifier = Modifier.fillMaxSize(),
                )
            }
            VisualEffectOverlay(previewConfig)
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

@Composable
fun TransformedVideoLayer(config: WallpaperConfig, content: @Composable () -> Unit) {
    Box(
        Modifier
            .fillMaxSize()
            .graphicsLayer {
                scaleX = config.scale * if (config.mirror) -1f else 1f
                scaleY = config.scale
                translationX = config.offsetX.coerceIn(-1f, 1f) * size.width * 0.35f
                translationY = config.offsetY.coerceIn(-1f, 1f) * size.height * 0.35f
                rotationZ = config.rotation.coerceIn(-15f, 15f)
            },
    ) {
        content()
    }
}

@Composable
fun VisualEffectOverlay(config: WallpaperConfig, modifier: Modifier = Modifier) {
    val brightness = config.brightness.coerceIn(0.1f, 2.0f)
    Box(modifier.fillMaxSize()) {
        val exposure = config.exposure.coerceIn(-1f, 1f)
        val contrast = config.contrast.coerceIn(0.5f, 1.5f)
        val saturation = config.saturation.coerceIn(0f, 2f)
        val warmth = config.warmth.coerceIn(-1f, 1f)

        when {
            brightness < 1f -> Box(
                Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = ((1f - brightness) * 0.65f).coerceIn(0f, 0.65f))),
            )

            brightness > 1f -> Box(
                Modifier
                    .fillMaxSize()
                    .background(Color.White.copy(alpha = ((brightness - 1f) * 0.32f).coerceIn(0f, 0.32f))),
            )
        }
        when {
            exposure < 0f -> Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = (-exposure * 0.35f).coerceIn(0f, 0.35f))))
            exposure > 0f -> Box(Modifier.fillMaxSize().background(Color.White.copy(alpha = (exposure * 0.28f).coerceIn(0f, 0.28f))))
        }
        when {
            contrast < 1f -> Box(Modifier.fillMaxSize().background(Color(0xFF8B819A).copy(alpha = ((1f - contrast) * 0.22f).coerceIn(0f, 0.22f))))
            contrast > 1f -> Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = ((contrast - 1f) * 0.14f).coerceIn(0f, 0.14f))))
        }
        if (saturation < 1f) {
            Box(Modifier.fillMaxSize().background(Color(0xFFB7B1C5).copy(alpha = ((1f - saturation) * 0.28f).coerceIn(0f, 0.28f))))
        } else if (saturation > 1f) {
            Box(Modifier.fillMaxSize().background(Color(0xFF7E6AAE).copy(alpha = ((saturation - 1f) * 0.08f).coerceIn(0f, 0.08f))))
        }
        when {
            warmth < 0f -> Box(Modifier.fillMaxSize().background(Color(0xFF7EA7FF).copy(alpha = (-warmth * 0.18f).coerceIn(0f, 0.18f))))
            warmth > 0f -> Box(Modifier.fillMaxSize().background(Color(0xFFFFB46E).copy(alpha = (warmth * 0.18f).coerceIn(0f, 0.18f))))
        }

        val vignetteAlpha = config.vignette.coerceIn(0f, 1f) * 0.58f
        if (vignetteAlpha > 0f) {
            Canvas(Modifier.fillMaxSize()) {
                drawRect(
                    brush = Brush.radialGradient(
                        colorStops = arrayOf(
                            0.0f to Color.Transparent,
                            0.55f to Color.Transparent,
                            1.0f to Color.Black.copy(alpha = vignetteAlpha),
                        ),
                        center = Offset(size.width / 2f, size.height / 2f),
                        radius = size.maxDimension * 0.72f,
                    ),
                )
            }
        }

        if (config.topGradient > 0f) {
            Canvas(Modifier.fillMaxSize()) {
                drawRect(
                    brush = Brush.verticalGradient(
                        0f to Color.Black.copy(alpha = config.topGradient.coerceIn(0f, 1f) * 0.42f),
                        0.45f to Color.Transparent,
                    ),
                )
            }
        }
        if (config.bottomGradient > 0f) {
            Canvas(Modifier.fillMaxSize()) {
                drawRect(
                    brush = Brush.verticalGradient(
                        0.55f to Color.Transparent,
                        1f to Color.Black.copy(alpha = config.bottomGradient.coerceIn(0f, 1f) * 0.42f),
                    ),
                )
            }
        }

        if (config.textEnabled && config.overlayText.isNotBlank()) {
            val alignment = when (config.textPosition) {
                TEXT_POSITION_TOP -> Alignment.TopCenter
                TEXT_POSITION_CENTER -> Alignment.Center
                else -> Alignment.BottomCenter
            }
            Text(
                text = config.overlayText,
                color = Color.White.copy(alpha = config.textOpacity.coerceIn(0f, 1f)),
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .align(alignment)
                    .padding(24.dp),
            )
        }
    }
}

@Composable
private fun PreviewThumbnail(thumbnail: Bitmap?) {
    if (thumbnail != null) {
        Image(
            bitmap = thumbnail.asImageBitmap(),
            contentDescription = "当前视频缩略图",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxWidth().height(96.dp),
        )
    } else {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(96.dp)
                .background(Color(0xFF202124)),
            contentAlignment = Alignment.Center,
        ) {
            Text("无缩略图", color = Color.White)
        }
    }
}

fun FillMode.toResizeMode(): Int = when (this) {
    FillMode.CENTER_CROP -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
    FillMode.FIT_CENTER -> AspectRatioFrameLayout.RESIZE_MODE_FIT
    FillMode.STRETCH -> AspectRatioFrameLayout.RESIZE_MODE_FILL
}

fun PlayerView.applyPreviewBlur(value: Float) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return
    val radius = value.coerceIn(0f, 1f) * 24f
    setRenderEffect(
        if (radius <= 0.1f) {
            null
        } else {
            RenderEffect.createBlurEffect(radius, radius, Shader.TileMode.CLAMP)
        },
    )
}
