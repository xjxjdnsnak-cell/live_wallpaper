package com.example.livewallpaper.wallpaper

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint

class WallpaperRenderer {
    private val background = Paint().apply { color = Color.rgb(20, 20, 20) }
    private val textPaint = Paint().apply { color = Color.WHITE; textSize = 42f; isAntiAlias = true }
    private val ripplePaint = Paint().apply { color = Color.WHITE; style = Paint.Style.STROKE; strokeWidth = 4f; isAntiAlias = true }

    fun drawPlaceholder(canvas: Canvas, text: String) {
        canvas.drawRect(0f, 0f, canvas.width.toFloat(), canvas.height.toFloat(), background)
        canvas.drawText(text, 40f, canvas.height / 2f, textPaint)
    }

    fun drawRipples(canvas: Canvas, ripples: List<Ripple>, parallaxX: Float = 0f) {
        ripples.forEach {
            ripplePaint.alpha = (it.alpha * 255).toInt().coerceIn(0, 255)
            canvas.drawCircle(it.x + parallaxX, it.y, it.radius, ripplePaint)
        }
    }
}
