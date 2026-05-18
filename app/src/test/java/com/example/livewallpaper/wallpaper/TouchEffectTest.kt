package com.example.livewallpaper.wallpaper

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TouchEffectTest {
    @Test
    fun lifecycleCreateUpdateFadeRemove() {
        val effect = TouchEffect()
        effect.onTap(10f, 20f)
        assertEquals(1, effect.items().size)

        effect.update(100)
        val first = effect.items().first()
        assertTrue(first.radius > 0f)
        assertTrue(first.alpha < 1f)

        effect.update(1000)
        assertTrue(effect.items().isEmpty())
    }
}
