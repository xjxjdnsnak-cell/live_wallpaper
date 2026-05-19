package com.example.livewallpaper.wallpaper

import android.content.Context
import android.graphics.Canvas
import android.net.Uri
import android.util.Log
import android.view.MotionEvent
import android.view.SurfaceHolder
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.example.livewallpaper.data.SettingsRepository
import com.example.livewallpaper.data.WallpaperSettings
import com.example.livewallpaper.media.VideoFileInspector
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

class VideoWallpaperEngine(private val context: Context) {
    private var holder: SurfaceHolder? = null
    private var boundSurfaceHolder: SurfaceHolder? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val repository = SettingsRepository.fromContext(context)
    private val renderer = WallpaperRenderer()
    private val touchEffect = TouchEffect()

    private var player: ExoPlayer? = null
    private var drawJob: Job? = null
    private var clipLoopJob: Job? = null
    private var settingsJob: Job? = null
    private var visible: Boolean = false
    private var parallaxX: Float = 0f
    private var settings: WallpaperSettings = WallpaperSettings()
    private var preparedVideoUri: String? = null
    private var settingsLoaded: Boolean = false

    fun onSurfaceCreated(surfaceHolder: SurfaceHolder) {
        holder = surfaceHolder
        Log.i(TAG, "surface created")
        ensurePlayer()
        startSettingsCollection()
    }

    fun onSurfaceChanged() {
        Log.i(TAG, "surface changed")
        player?.let { bindPlayerSurface(it) }
    }

    fun onVisibilityChanged(isVisible: Boolean) {
        visible = isVisible
        if (isVisible) {
            if (!settings.videoUri.isNullOrBlank()) {
                player?.playWhenReady = true
                restartClipLoopIfNeeded()
            }
            restartRenderLoopIfNeeded()
        } else {
            player?.pause()
            stopRenderLoop()
            stopClipLoop()
        }
    }

    fun onOffsetsChanged(xOffset: Float) {
        if (!settings.parallaxEnabled) return
        parallaxX = (xOffset - 0.5f) * 40f
    }

    fun onTouchEvent(event: MotionEvent) {
        if (!settings.touchEffectEnabled) return
        if (!settings.videoUri.isNullOrBlank()) return
        if (event.action == MotionEvent.ACTION_DOWN) {
            touchEffect.onTap(event.x, event.y)
        }
    }

    fun onSurfaceDestroyed() {
        stopRenderLoop()
        stopClipLoop()
        releasePlayer()
        settingsJob?.cancel()
        settingsJob = null
        settingsLoaded = false
        holder = null
        boundSurfaceHolder = null
    }

    fun onDestroy() {
        onSurfaceDestroyed()
        scope.cancel()
    }

    private fun startSettingsCollection() {
        if (settingsJob?.isActive == true) return
        settingsJob = scope.launch {
            repository.settings.collect { newSettings ->
                applySettings(newSettings)
            }
        }
    }

    private fun ensurePlayer(): ExoPlayer? {
        if (holder == null) return null
        player?.let { existing ->
            bindPlayerSurface(existing)
            return existing
        }

        return ExoPlayer.Builder(context).build().also { p ->
            p.repeatMode = Player.REPEAT_MODE_ALL
            p.addListener(
                object : Player.Listener {
                    override fun onPlayerError(error: PlaybackException) {
                        Log.e(TAG, "Wallpaper video playback failed", error)
                        scope.launch {
                            repository.updateLastPlaybackError(VideoFileInspector.playbackErrorSummary())
                        }
                    }

                    override fun onPlaybackStateChanged(playbackState: Int) {
                        if (playbackState == Player.STATE_READY) {
                            scope.launch { repository.updateLastPlaybackError(null) }
                        }
                    }
                },
            )
            bindPlayerSurface(p)
            player = p
            applyPlayerSettings(p, settings)
        }
    }

    private fun bindPlayerSurface(p: ExoPlayer) {
        val surfaceHolder = holder ?: return
        if (boundSurfaceHolder === surfaceHolder) return
        boundSurfaceHolder?.let { oldHolder ->
            p.clearVideoSurfaceHolder(oldHolder)
        }
        p.setVideoSurfaceHolder(surfaceHolder)
        boundSurfaceHolder = surfaceHolder
    }

    private fun applySettings(newSettings: WallpaperSettings) {
        val previousVideoUri = settings.videoUri?.takeIf { it.isNotBlank() }
        val nextVideoUri = newSettings.videoUri?.takeIf { it.isNotBlank() }
        if (nextVideoUri != null) {
            stopRenderLoop()
        }
        settings = newSettings
        settingsLoaded = true

        val p = ensurePlayer()
        if (p != null) {
            applyPlayerSettings(p, newSettings)
            applyClipBounds(p, newSettings)
            if (nextVideoUri != previousVideoUri || nextVideoUri != preparedVideoUri) {
                prepareVideo(p, nextVideoUri)
            }
        }

        if (!newSettings.parallaxEnabled) {
            parallaxX = 0f
        }
        restartRenderLoopIfNeeded()
        restartClipLoopIfNeeded()
    }

    private fun applyPlayerSettings(p: ExoPlayer, currentSettings: WallpaperSettings) {
        p.volume = if (currentSettings.muted) 0f else 1f
        p.playbackParameters = PlaybackParameters(currentSettings.playbackSpeed.coerceAtLeast(0.1f))
        p.videoScalingMode = when (currentSettings.fillMode) {
            FillMode.CENTER_CROP -> C.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING
            FillMode.FIT_CENTER,
            FillMode.STRETCH -> C.VIDEO_SCALING_MODE_SCALE_TO_FIT
        }
    }

    private fun prepareVideo(p: ExoPlayer, videoUri: String?) {
        if (videoUri == null) {
            p.pause()
            p.clearMediaItems()
            preparedVideoUri = null
            return
        }

        try {
            p.setMediaItem(MediaItem.fromUri(Uri.parse(videoUri)))
            p.prepare()
            settings.startMs?.takeIf { it > 0L }?.let { p.seekTo(it) }
            p.playWhenReady = visible
            preparedVideoUri = videoUri
            restartClipLoopIfNeeded()
        } catch (t: Throwable) {
            preparedVideoUri = null
            Log.e(TAG, "Failed to play video uri=$videoUri", t)
            scope.launch {
                repository.updateLastPlaybackError(VideoFileInspector.playbackErrorSummary())
            }
        }
    }

    private fun restartRenderLoopIfNeeded() {
        if (!settingsLoaded || !visible || !settings.videoUri.isNullOrBlank()) {
            stopRenderLoop()
            return
        }
        if (drawJob?.isActive == true) return
        drawJob = scope.launch(Dispatchers.Default) {
            var last = System.currentTimeMillis()
            while (visible && settings.videoUri.isNullOrBlank()) {
                val now = System.currentTimeMillis()
                val delta = now - last
                last = now
                touchEffect.update(delta)
                drawPlaceholder()
                kotlinx.coroutines.delay(16)
            }
        }
    }

    private fun stopRenderLoop() {
        drawJob?.cancel()
        drawJob = null
    }

    private fun applyClipBounds(p: ExoPlayer, currentSettings: WallpaperSettings) {
        val start = currentSettings.startMs ?: 0L
        val end = currentSettings.endMs
        if (start > 0L && p.currentPosition < start) {
            p.seekTo(start)
        } else if (end != null && end > start && p.currentPosition >= end) {
            p.seekTo(start)
        }
    }

    private fun restartClipLoopIfNeeded() {
        stopClipLoop()
        val end = settings.endMs ?: return
        val start = settings.startMs ?: 0L
        if (!settingsLoaded || !visible || settings.videoUri.isNullOrBlank() || end <= start) return
        clipLoopJob = scope.launch {
            while (visible && !settings.videoUri.isNullOrBlank()) {
                val p = player
                if (p != null && p.currentPosition >= end) {
                    p.seekTo(start)
                }
                delay(300)
            }
        }
    }

    private fun stopClipLoop() {
        clipLoopJob?.cancel()
        clipLoopJob = null
    }

    private fun drawPlaceholder() {
        val canvas: Canvas = try {
            holder?.lockCanvas()
        } catch (t: Throwable) {
            Log.e(TAG, "lockCanvas failed", t)
            return
        } ?: return
        try {
            renderer.drawPlaceholder(canvas, "请选择视频")
            if (settings.touchEffectEnabled) {
                renderer.drawRipples(canvas, touchEffect.items(), parallaxX)
            }
        } catch (t: Throwable) {
            Log.e(TAG, "draw failed", t)
        } finally {
            holder?.unlockCanvasAndPost(canvas)
        }
    }

    private fun releasePlayer() {
        player?.let { p ->
            boundSurfaceHolder?.let { p.clearVideoSurfaceHolder(it) }
            p.release()
        }
        player = null
        preparedVideoUri = null
        stopClipLoop()
    }

    companion object {
        const val TAG = "VideoWallpaperEngine"
    }
}
