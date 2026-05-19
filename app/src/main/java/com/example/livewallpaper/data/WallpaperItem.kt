package com.example.livewallpaper.data

import com.example.livewallpaper.wallpaper.FillMode

data class WallpaperConfig(
    val fillMode: FillMode = FillMode.CENTER_CROP,
    val playbackSpeed: Float = 1.0f,
    val muted: Boolean = true,
    val startMs: Long? = null,
    val endMs: Long? = null,
    val brightness: Float = 1.0f,
    val contrast: Float = 1.0f,
    val saturation: Float = 1.0f,
    val warmth: Float = 0f,
    val exposure: Float = 0f,
    val scale: Float = 1.0f,
    val offsetX: Float = 0f,
    val offsetY: Float = 0f,
    val rotation: Float = 0f,
    val mirror: Boolean = false,
    val blur: Float = 0f,
    val vignette: Float = 0f,
    val topGradient: Float = 0f,
    val bottomGradient: Float = 0f,
    val textEnabled: Boolean = false,
    val overlayText: String = "",
    val textPosition: String = TEXT_POSITION_BOTTOM,
    val textOpacity: Float = 0.8f,
    val presetName: String = PRESET_ORIGINAL,
)

const val TEXT_POSITION_TOP = "top"
const val TEXT_POSITION_CENTER = "center"
const val TEXT_POSITION_BOTTOM = "bottom"
const val PRESET_ORIGINAL = "original"

data class WallpaperItem(
    val id: String,
    val uri: String,
    val displayName: String? = null,
    val durationMs: Long? = null,
    val sizeBytes: Long? = null,
    val thumbnailPath: String? = null,
    val category: String = DEFAULT_CATEGORY,
    val isFavorite: Boolean = false,
    val createdAt: Long,
    val lastViewedAt: Long? = null,
    val useCount: Int = 0,
    val config: WallpaperConfig = WallpaperConfig(),
    val isGenerated: Boolean = false,
    val originalUri: String? = null,
    val sourceWallpaperId: String? = null,
    val generatedAt: Long? = null,
    val generatedFromConfig: Boolean = false,
) {
    companion object {
        const val DEFAULT_CATEGORY = "其他"
        val CATEGORIES = listOf("风景", "动漫", "游戏", "插画", "科幻", "视觉", "艺术", "建筑", "动物", "其他")
    }
}

data class WallpaperLibraryState(
    val items: List<WallpaperItem> = emptyList(),
    val currentWallpaperId: String? = null,
)
