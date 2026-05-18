package com.example.livewallpaper.data

import com.example.livewallpaper.wallpaper.FillMode

data class WallpaperSettings(
    val videoUri: String? = null,
    val muted: Boolean = true,
    val fillMode: FillMode = FillMode.CENTER_CROP,
    val playbackSpeed: Float = 1.0f,
    val touchEffectEnabled: Boolean = true,
    val parallaxEnabled: Boolean = true,
)
