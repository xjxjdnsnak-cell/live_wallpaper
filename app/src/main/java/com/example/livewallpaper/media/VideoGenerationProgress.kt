package com.example.livewallpaper.media

data class VideoGenerationProgress(
    val fraction: Float,
    val message: String,
    val stage: Stage,
) {
    enum class Stage {
        PREPARING,
        GENERATING,
        SAVING,
        COMPLETED,
        FAILED,
    }
}
