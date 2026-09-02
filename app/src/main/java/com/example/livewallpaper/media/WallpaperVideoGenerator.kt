package com.example.livewallpaper.media

import android.content.Context
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.media.MediaMuxer
import android.net.Uri
import android.util.Log
import com.example.livewallpaper.data.WallpaperConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import kotlin.coroutines.coroutineContext
import java.io.File
import java.nio.ByteBuffer
import java.util.Locale

object WallpaperVideoGenerator {
    private const val TAG = "WallpaperVideoGenerator"
    private const val OUTPUT_DIR = "generated_wallpapers"
    private const val DEFAULT_BUFFER_SIZE = 1024 * 1024

    suspend fun generate(
        context: Context,
        sourceUri: String,
        config: WallpaperConfig,
        outputFileName: String,
        onProgress: (VideoGenerationProgress) -> Unit,
    ): VideoGenerationResult = withContext(Dispatchers.IO) {
        // Audit P-4: throttle per-sample GENERATING callbacks to one per 100ms of
        // wall-clock; stage transitions (start/saving/completion) always pass through.
        val throttler = ProgressThrottler()
        val progress = { fraction: Float, message: String, stage: VideoGenerationProgress.Stage ->
            val forced = stage != VideoGenerationProgress.Stage.GENERATING
            if (throttler.shouldEmit(forced)) {
                onProgress(VideoGenerationProgress(fraction.coerceIn(0f, 1f), message, stage))
            }
        }
        progress(0f, "准备中", VideoGenerationProgress.Stage.PREPARING)

        val source = runCatching { Uri.parse(sourceUri) }.getOrNull()
            ?: return@withContext VideoGenerationResult.Failure("原始视频地址无效")

        val readable = runCatching {
            context.contentResolver.openInputStream(source)?.use { true } == true
        }.getOrDefault(false)
        if (!readable) return@withContext VideoGenerationResult.Failure("无法读取原始视频，请重新选择文件")

        val durationMs = readDurationMs(context, source)
        val startMs = (config.startMs ?: 0L).coerceAtLeast(0L)
        val endMs = config.endMs ?: durationMs
        if (endMs != null && endMs <= startMs) {
            return@withContext VideoGenerationResult.Failure("结束时间必须大于起始时间")
        }

        val outputDir = context.getExternalFilesDir(OUTPUT_DIR)
            ?: return@withContext VideoGenerationResult.Failure("无法创建输出目录")
        if (!outputDir.exists() && !outputDir.mkdirs()) {
            return@withContext VideoGenerationResult.Failure("无法创建输出目录")
        }

        val output = File(outputDir, sanitizeMp4Name(outputFileName))
        if (output.exists()) output.delete()

        try {
            val result = runCatching {
                trimVideoOnlyMp4(context, source, output, startMs, endMs, progress)
            }.fold(
                onSuccess = { success ->
                    progress(1f, "已完成", VideoGenerationProgress.Stage.COMPLETED)
                    success
                },
                onFailure = { throwable ->
                    if (throwable is CancellationException) throw throwable
                    Log.e(TAG, "Generate wallpaper copy failed", throwable)
                    runCatching { output.delete() }
                    VideoGenerationResult.Failure("生成失败，请换一个视频重试", throwable)
                },
            )
            if (result is VideoGenerationResult.Failure) {
                runCatching { output.delete() }
            }
            result
        } finally {
            // Audit P-11: a cancelled remux must not leave a partial output file behind.
            if (!isActive) {
                runCatching { output.delete() }
            }
        }
    }

    private suspend fun trimVideoOnlyMp4(
        context: Context,
        source: Uri,
        output: File,
        startMs: Long,
        endMs: Long?,
        progress: (Float, String, VideoGenerationProgress.Stage) -> Unit,
    ): VideoGenerationResult {
        var extractor: MediaExtractor? = null
        var muxer: MediaMuxer? = null
        var muxerStarted = false
        var wroteSample = false

        try {
            extractor = MediaExtractor().apply {
                setDataSource(context, source, null)
            }
            val videoTrackIndex = findVideoTrack(extractor)
            if (videoTrackIndex < 0) return VideoGenerationResult.Failure("当前视频格式不支持生成")

            val format = extractor.getTrackFormat(videoTrackIndex)
            val maxInputSize = if (format.containsKey(MediaFormat.KEY_MAX_INPUT_SIZE)) {
                format.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE).coerceAtLeast(DEFAULT_BUFFER_SIZE)
            } else {
                DEFAULT_BUFFER_SIZE
            }
            val startUs = startMs * 1000L
            val endUs = endMs?.let { it * 1000L }
            val expectedDurationUs = endUs?.minus(startUs)?.coerceAtLeast(1L)

            extractor.selectTrack(videoTrackIndex)
            extractor.seekTo(startUs, MediaExtractor.SEEK_TO_CLOSEST_SYNC)

            muxer = MediaMuxer(output.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            val outputTrackIndex = muxer.addTrack(format)
            muxer.start()
            muxerStarted = true

            val buffer = ByteBuffer.allocateDirect(maxInputSize)
            val info = MediaCodec.BufferInfo()
            var firstSampleTimeUs: Long? = null

            progress(0.05f, "正在生成", VideoGenerationProgress.Stage.GENERATING)
            while (true) {
                // Audit P-11: bail out promptly when the caller's coroutine is cancelled.
                coroutineContext.ensureActive()
                val sampleTrackIndex = extractor.sampleTrackIndex
                if (sampleTrackIndex == -1) break
                if (sampleTrackIndex != videoTrackIndex) {
                    extractor.advance()
                    continue
                }

                val sampleTimeUs = extractor.sampleTime
                if (sampleTimeUs < 0 || (endUs != null && sampleTimeUs > endUs)) break

                buffer.clear()
                val sampleSize = extractor.readSampleData(buffer, 0)
                if (sampleSize <= 0) break

                val firstTime = firstSampleTimeUs ?: sampleTimeUs.also { firstSampleTimeUs = it }
                info.set(
                    0,
                    sampleSize,
                    (sampleTimeUs - firstTime).coerceAtLeast(0L),
                    extractor.sampleFlags,
                )
                muxer.writeSampleData(outputTrackIndex, buffer, info)
                wroteSample = true

                if (expectedDurationUs != null) {
                    val fraction = ((sampleTimeUs - startUs).toFloat() / expectedDurationUs.toFloat())
                        .coerceIn(0f, 1f)
                    progress(0.05f + fraction * 0.88f, "正在生成", VideoGenerationProgress.Stage.GENERATING)
                }

                extractor.advance()
            }

            if (!wroteSample) return VideoGenerationResult.Failure("裁剪片段没有可用视频帧")

            progress(0.96f, "正在保存", VideoGenerationProgress.Stage.SAVING)
        } finally {
            runCatching {
                if (muxerStarted) muxer?.stop()
            }.onFailure { Log.w(TAG, "Muxer stop failed", it) }
            runCatching { muxer?.release() }
            runCatching { extractor?.release() }
        }

        val size = output.length()
        if (size <= 0L) {
            runCatching { output.delete() }
            return VideoGenerationResult.Failure("生成文件为空")
        }

        val generatedDuration = endMs?.minus(startMs)
        return VideoGenerationResult.Success(
            uri = Uri.fromFile(output).toString(),
            path = output.absolutePath,
            durationMs = generatedDuration,
            sizeBytes = size,
        )
    }

    private fun findVideoTrack(extractor: MediaExtractor): Int {
        for (index in 0 until extractor.trackCount) {
            val format = extractor.getTrackFormat(index)
            val mime = format.getString(MediaFormat.KEY_MIME)
            if (mime?.startsWith("video/") == true) return index
        }
        return -1
    }

    private fun readDurationMs(context: Context, uri: Uri): Long? =
        runCatching {
            val retriever = MediaMetadataRetriever()
            try {
                retriever.setDataSource(context, uri)
                retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull()
            } finally {
                retriever.release()
            }
        }.getOrNull()

    private fun sanitizeMp4Name(name: String): String {
        val cleaned = name
            .replace(Regex("""[\\/:*?"<>|]"""), "_")
            .trim()
            .ifBlank { "wallpaper_${System.currentTimeMillis()}.mp4" }
        return if (cleaned.lowercase(Locale.US).endsWith(".mp4")) cleaned else "$cleaned.mp4"
    }
}
