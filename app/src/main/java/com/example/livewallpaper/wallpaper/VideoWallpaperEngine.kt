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
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class VideoWallpaperEngine(private val context: Context) {
    private var holder: SurfaceHolder? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val repository = SettingsRepository.fromContext(context)
    private val renderer = WallpaperRenderer()
    private val touchEffect = TouchEffect()

    private var player: ExoPlayer? = null
    private var drawJob: Job? = null
    private var settingsJob: Job? = null

    private var visible = false
    private var settings: WallpaperSettings = WallpaperSettings()
    private var boundSurfaceHolder: SurfaceHolder? = null
    private var currentVideoUri: String? = null

    fun onSurfaceCreated(surfaceHolder: SurfaceHolder) {
        holder = surfaceHolder
        Log.i(TAG, "surface created")
        ensurePlayer()
        bindSurfaceIfNeeded()
        startSettingsCollectionIfNeeded()
        drawPlaceholderIfNeeded()
        refreshPlaybackState()
    }

    fun onSurfaceChanged() {
        Log.i(TAG, "surface changed")
        drawPlaceholderIfNeeded()
    }

    fun onVisibilityChanged(isVisible: Boolean) {
        visible = isVisible
        if (isVisible) {
            refreshPlaybackState()
            drawPlaceholderIfNeeded()
        } else {
            player?.pause()
            stopPlaceholderLoop()
        }
    }

    fun onOffsetsChanged(xOffset: Float) {
        if (!settings.parallaxEnabled) return
        // Phase 1: keep value for future overlay transform. Do not draw on video surface now.
        val ignoredParallax = (xOffset - 0.5f) * 40f
        if (ignoredParallax.isNaN()) {
            Log.w(TAG, "Invalid parallax offset: $xOffset")
        }
    }

    fun onTouchEvent(event: MotionEvent) {
        if (!settings.touchEffectEnabled) return
        if (event.action == MotionEvent.ACTION_DOWN) {
            touchEffect.onTap(event.x, event.y)
            // Keep effect state but avoid drawing on same surface as ExoPlayer video.
        }
    }

    fun onSurfaceDestroyed() {
        stopPlaceholderLoop()
        clearSurfaceBinding()
        releasePlayer()
        holder = null
    }

    fun onDestroy() {
        onSurfaceDestroyed()
        settingsJob?.cancel()
        settingsJob = null
        scope.cancel()
    }

    private fun startSettingsCollectionIfNeeded() {
        if (settingsJob != null) return
        settingsJob = scope.launch {
            repository.settings.collectLatest { newSettings ->
                val old = settings
                settings = newSettings

                val p = ensurePlayer()
                p.volume = if (newSettings.muted) 0f else 1f
                p.playbackParameters = PlaybackParameters(newSettings.playbackSpeed)

                val normalizedOldUri = old.videoUri?.ifBlank { null }
                val normalizedNewUri = newSettings.videoUri?.ifBlank { null }
                if (normalizedOldUri != normalizedNewUri || currentVideoUri != normalizedNewUri) {
                    currentVideoUri = normalizedNewUri
                    applyVideoUriToPlayer(normalizedNewUri)
                }

                refreshPlaybackState()
                drawPlaceholderIfNeeded()
            }
        }
    }

    private fun ensurePlayer(): ExoPlayer {
        player?.let { return it }
        val p = ExoPlayer.Builder(context).build().also {
            it.repeatMode = Player.REPEAT_MODE_ALL
            it.volume = if (settings.muted) 0f else 1f
            it.playbackParameters = PlaybackParameters(settings.playbackSpeed)
        }
        player = p
        bindSurfaceIfNeeded()
        currentVideoUri = settings.videoUri?.ifBlank { null }
        applyVideoUriToPlayer(currentVideoUri)
        return p
    }

    private fun bindSurfaceIfNeeded() {
        val p = player ?: return
        val h = holder ?: return
        if (boundSurfaceHolder === h) return
        clearSurfaceBinding()
        p.setVideoSurfaceHolder(h)
        boundSurfaceHolder = h
    }

    private fun clearSurfaceBinding() {
        val p = player ?: return
        val h = boundSurfaceHolder ?: return
        try {
            p.clearVideoSurfaceHolder(h)
        } catch (t: Throwable) {
            Log.w(TAG, "clearVideoSurfaceHolder failed", t)
        } finally {
            boundSurfaceHolder = null
        }
    }

    private fun applyVideoUriToPlayer(videoUri: String?) {
        val p = player ?: return
        try {
            p.stop()
            p.clearMediaItems()
            if (videoUri.isNullOrBlank()) {
                Log.i(TAG, "No video URI, rendering placeholder only")
                return
            }
            p.setMediaItem(MediaItem.fromUri(Uri.parse(videoUri)))
            p.prepare()
            p.playWhenReady = visible
        } catch (t: Throwable) {
            Log.e(TAG, "Failed to apply video uri=$videoUri", t)
        }
    }

    private fun refreshPlaybackState() {
        val p = player ?: return
        if (settings.videoUri.isNullOrBlank()) {
            p.pause()
            return
        }
        p.playWhenReady = visible
        if (visible) p.play() else p.pause()
    }

    private fun drawPlaceholderIfNeeded() {
        if (!visible) return
        if (!settings.videoUri.isNullOrBlank()) {
            stopPlaceholderLoop()
            return
        }
        if (drawJob?.isActive == true) return
        drawJob = scope.launch(Dispatchers.Default) {
            while (visible && settings.videoUri.isNullOrBlank()) {
                drawPlaceholderFrame()
                delay(500)
            }
        }
    }

    private fun stopPlaceholderLoop() {
        drawJob?.cancel()
        drawJob = null
    }

    private fun drawPlaceholderFrame() {
        val surfaceHolder = holder ?: return
        val canvas: Canvas = try {
            surfaceHolder.lockCanvas()
        } catch (t: Throwable) {
            Log.e(TAG, "lockCanvas failed for placeholder", t)
            return
        } ?: return

        try {
            renderer.drawPlaceholder(canvas, "请选择视频")
        } catch (t: Throwable) {
            Log.e(TAG, "draw placeholder failed", t)
        } finally {
            surfaceHolder.unlockCanvasAndPost(canvas)
        }
    }

    private fun releasePlayer() {
        clearSurfaceBinding()
        player?.release()
        player = null
    }

    companion object {
        private const val TAG = "VideoWallpaperEngine"
    }
}
