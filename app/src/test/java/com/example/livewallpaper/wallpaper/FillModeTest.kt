package com.example.livewallpaper.wallpaper

import org.junit.Assert.assertEquals
import org.junit.Test

class FillModeTest {
    @Test
    fun parseValid() {
        assertEquals(FillMode.FIT_CENTER, FillMode.fromStorage("FIT_CENTER"))
    }

    @Test
    fun parseInvalidFallback() {
        assertEquals(FillMode.CENTER_CROP, FillMode.fromStorage("INVALID"))
    }
}
