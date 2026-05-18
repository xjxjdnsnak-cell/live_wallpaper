package com.example.livewallpaper.wallpaper

data class Ripple(
    val x: Float,
    val y: Float,
    var radius: Float = 0f,
    var alpha: Float = 1f,
    var ageMs: Long = 0,
    val lifeMs: Long = 800,
)

class TouchEffect {
    private val ripples = mutableListOf<Ripple>()

    fun onTap(x: Float, y: Float) {
        ripples += Ripple(x, y)
    }

    fun update(deltaMs: Long) {
        ripples.forEach {
            it.ageMs += deltaMs
            it.radius += deltaMs * 0.15f
            it.alpha = (1f - it.ageMs.toFloat() / it.lifeMs).coerceAtLeast(0f)
        }
        ripples.removeAll { it.ageMs >= it.lifeMs || it.alpha <= 0f }
    }

    fun items(): List<Ripple> = ripples.toList()
}
