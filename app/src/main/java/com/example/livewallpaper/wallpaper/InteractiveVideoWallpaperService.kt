package com.example.livewallpaper.wallpaper

import android.service.wallpaper.WallpaperService
import android.util.Log
import android.view.MotionEvent
import android.view.SurfaceHolder

class InteractiveVideoWallpaperService : WallpaperService() {
    override fun onCreateEngine(): Engine = InteractiveEngine()

    inner class InteractiveEngine : Engine() {
        private val engine = VideoWallpaperEngine(applicationContext)

        override fun onCreate(surfaceHolder: SurfaceHolder?) {
            super.onCreate(surfaceHolder)
            setTouchEventsEnabled(true)
        }

        override fun onSurfaceCreated(holder: SurfaceHolder) {
            super.onSurfaceCreated(holder)
            engine.onSurfaceCreated(holder)
        }

        override fun onSurfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
            super.onSurfaceChanged(holder, format, width, height)
            engine.onSurfaceChanged()
        }

        override fun onVisibilityChanged(visible: Boolean) {
            super.onVisibilityChanged(visible)
            engine.onVisibilityChanged(visible)
        }

        override fun onTouchEvent(event: MotionEvent) {
            super.onTouchEvent(event)
            engine.onTouchEvent(event)
        }

        override fun onOffsetsChanged(
            xOffset: Float,
            yOffset: Float,
            xOffsetStep: Float,
            yOffsetStep: Float,
            xPixelOffset: Int,
            yPixelOffset: Int,
        ) {
            super.onOffsetsChanged(xOffset, yOffset, xOffsetStep, yOffsetStep, xPixelOffset, yPixelOffset)
            engine.onOffsetsChanged(xOffset)
        }

        override fun onSurfaceDestroyed(holder: SurfaceHolder) {
            super.onSurfaceDestroyed(holder)
            engine.onSurfaceDestroyed()
        }

        override fun onDestroy() {
            super.onDestroy()
            Log.i("WallpaperService", "engine destroyed")
            engine.onDestroy()
        }
    }
}
