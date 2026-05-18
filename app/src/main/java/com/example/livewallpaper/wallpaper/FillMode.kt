package com.example.livewallpaper.wallpaper

enum class FillMode {
    CENTER_CROP,
    FIT_CENTER,
    STRETCH;

    companion object {
        fun fromStorage(value: String?): FillMode = entries.firstOrNull { it.name == value } ?: CENTER_CROP
    }
}
