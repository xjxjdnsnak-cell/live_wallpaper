package com.example.livewallpaper.wallpaper

import java.util.concurrent.CopyOnWriteArrayList

data class Ripple(
    val x: Float,
    val y: Float,
    var radius: Float = 0f,
    var alpha: Float = 1f,
    var ageMs: Long = 0,
    val lifeMs: Long = 800,
)

class TouchEffect {
    // CopyOnWriteArrayList: onTap() (main thread) and update()/items() (render loop on
    // Dispatchers.Default) touch this list concurrently. COW makes add/removeAll atomic and
    // iteration runs on a snapshot, so no ConcurrentModificationException can occur.
    private val ripples = CopyOnWriteArrayList<Ripple>()

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
