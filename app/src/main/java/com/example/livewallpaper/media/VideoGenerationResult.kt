package com.example.livewallpaper.media

sealed interface VideoGenerationResult {
    data class Success(
        val uri: String,
        val path: String,
        val durationMs: Long?,
        val sizeBytes: Long,
    ) : VideoGenerationResult

    data class Failure(
        val message: String,
        val cause: Throwable? = null,
    ) : VideoGenerationResult
}
