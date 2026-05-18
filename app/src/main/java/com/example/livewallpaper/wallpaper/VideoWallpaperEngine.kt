package com.example.livewallpaper.wallpaper

import android.content.Context
import android.graphics.Canvas
import android.net.Uri
import android.util.Log
import android.view.MotionEvent
import android.view.SurfaceHolder
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.example.livewallpaper.data.SettingsRepository
import com.example.livewallpaper.data.WallpaperSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class VideoWallpaperEngine(private val context: Context) {
    private var holder: SurfaceHolder? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val repository = SettingsRepository.fromContext(context)
    private val renderer = WallpaperRenderer()
    private val touchEffect = TouchEffect()

    private var player: ExoPlayer? = null
    private var drawJob: Job? = null
    private var visible: Boolean = false
    private var parallaxX: Float = 0f
    private var settings: WallpaperSettings = WallpaperSettings()

    fun onSurfaceCreated(surfaceHolder: SurfaceHolder) {
        holder = surfaceHolder
        Log.i(TAG, "surface created")
        scope.launch {
            settings = repository.settings.first()
            initPlayer()
            startRenderLoop()
        }
    }

    fun onSurfaceChanged() {
        Log.i(TAG, "surface changed")
    }

    fun onVisibilityChanged(isVisible: Boolean) {
        visible = isVisible
        if (isVisible) {
            player?.playWhenReady = true
            startRenderLoop()
        } else {
            player?.pause()
            stopRenderLoop()
        }
    }

    fun onOffsetsChanged(xOffset: Float) {
        if (!settings.parallaxEnabled) return
        parallaxX = (xOffset - 0.5f) * 40f
    }

    fun onTouchEvent(event: MotionEvent) {
        if (!settings.touchEffectEnabled) return
        if (event.action == MotionEvent.ACTION_DOWN) {
            touchEffect.onTap(event.x, event.y)
        }
    }

    fun onSurfaceDestroyed() {
        stopRenderLoop()
        releasePlayer()
    }

    fun onDestroy() {
        onSurfaceDestroyed()
        scope.cancel()
    }

    private fun initPlayer() {
        releasePlayer()
        val p = ExoPlayer.Builder(context).build()
        p.repeatMode = Player.REPEAT_MODE_ALL
        p.volume = if (settings.muted) 0f else 1f
        p.playbackParameters = PlaybackParameters(settings.playbackSpeed)
        val video = settings.videoUri?.takeIf { it.isNotBlank() }
        if (video != null) {
            try {
                p.setMediaItem(MediaItem.fromUri(Uri.parse(video)))
                p.prepare()
                p.playWhenReady = visible
            } catch (t: Throwable) {
                Log.e(TAG, "Failed to play video uri=$video", t)
            }
        }
        player = p
    }

    private fun startRenderLoop() {
        if (drawJob?.isActive == true) return
        drawJob = scope.launch(Dispatchers.Default) {
            var last = System.currentTimeMillis()
            while (visible) {
                val now = System.currentTimeMillis()
                val delta = now - last
                last = now
                touchEffect.update(delta)
                drawOverlay()
                kotlinx.coroutines.delay(16)
            }
        }
    }

    private fun stopRenderLoop() {
        drawJob?.cancel()
        drawJob = null
    }

    private fun drawOverlay() {
        val canvas: Canvas = try {
            holder?.lockCanvas()
        } catch (t: Throwable) {
            Log.e(TAG, "lockCanvas failed", t)
            return
        } ?: return
        try {
            if (settings.videoUri.isNullOrBlank()) {
                renderer.drawPlaceholder(canvas, "请选择视频")
            }
            renderer.drawRipples(canvas, touchEffect.items(), parallaxX)
        } catch (t: Throwable) {
            Log.e(TAG, "draw failed", t)
        } finally {
            holder?.unlockCanvasAndPost(canvas)
        }
    }

    private fun releasePlayer() {
        player?.release()
        player = null
    }

    companion object { const val TAG = "VideoWallpaperEngine" }
}
