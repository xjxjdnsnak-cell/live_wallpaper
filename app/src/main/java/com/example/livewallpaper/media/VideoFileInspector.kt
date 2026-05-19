package com.example.livewallpaper.media

import android.content.ContentResolver
import android.content.Context
import android.database.Cursor
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import kotlin.math.roundToInt

data class VideoInspection(
    val uri: String,
    val displayName: String? = null,
    val durationMs: Long? = null,
    val sizeBytes: Long? = null,
    val isAccessible: Boolean = false,
    val thumbnail: Bitmap? = null,
    val warning: String? = null,
)

object VideoFileInspector {
    private const val TAG = "VideoFileInspector"
    const val UNKNOWN = "未知"
    const val USER_PLAYBACK_ERROR = "视频无法播放，请重新选择文件"

    fun inspect(context: Context, uriString: String): VideoInspection {
        val uri = runCatching { Uri.parse(uriString) }.getOrNull()
            ?: return VideoInspection(uri = uriString, warning = "视频 URI 无效")

        val (displayName, sizeBytes) = queryDisplayNameAndSize(context.contentResolver, uri)
        val accessible = canOpen(context.contentResolver, uri)
        if (!accessible) {
            return VideoInspection(
                uri = uriString,
                displayName = displayName,
                sizeBytes = sizeBytes,
                isAccessible = false,
                warning = "无法访问当前视频，请重新选择文件",
            )
        }

        val metadata = readMediaMetadata(context, uri)
        return VideoInspection(
            uri = uriString,
            displayName = displayName ?: uri.lastPathSegment,
            durationMs = metadata.durationMs,
            sizeBytes = sizeBytes,
            isAccessible = true,
            thumbnail = metadata.thumbnail,
        )
    }

    fun formatDuration(durationMs: Long?): String {
        if (durationMs == null || durationMs < 0) return UNKNOWN
        val totalSeconds = durationMs / 1000
        val seconds = totalSeconds % 60
        val minutes = (totalSeconds / 60) % 60
        val hours = totalSeconds / 3600
        return if (hours > 0) {
            "%d:%02d:%02d".format(hours, minutes, seconds)
        } else {
            "%d:%02d".format(minutes, seconds)
        }
    }

    fun formatSize(sizeBytes: Long?): String {
        if (sizeBytes == null || sizeBytes < 0) return UNKNOWN
        if (sizeBytes < 1024) return "$sizeBytes B"

        val units = listOf("KB", "MB", "GB")
        var value = sizeBytes.toDouble() / 1024.0
        var unitIndex = 0
        while (value >= 1024.0 && unitIndex < units.lastIndex) {
            value /= 1024.0
            unitIndex++
        }
        val rounded = (value * 10).roundToInt() / 10.0
        return if (rounded % 1.0 == 0.0) {
            "${rounded.toInt()} ${units[unitIndex]}"
        } else {
            "%.1f %s".format(rounded, units[unitIndex])
        }
    }

    fun playbackErrorSummary(): String = USER_PLAYBACK_ERROR

    private fun canOpen(contentResolver: ContentResolver, uri: Uri): Boolean {
        return runCatching {
            contentResolver.openInputStream(uri)?.use { true } ?: false
        }.getOrElse {
            Log.w(TAG, "Cannot open video uri=$uri", it)
            false
        }
    }

    private fun queryDisplayNameAndSize(contentResolver: ContentResolver, uri: Uri): Pair<String?, Long?> {
        return runCatching {
            contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE), null, null, null)
                ?.use { cursor -> readOpenableColumns(cursor) }
        }.getOrElse {
            Log.w(TAG, "Cannot query video uri=$uri", it)
            null
        } ?: (null to null)
    }

    private fun readOpenableColumns(cursor: Cursor): Pair<String?, Long?> {
        if (!cursor.moveToFirst()) return null to null
        val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
        val name = if (nameIndex >= 0 && !cursor.isNull(nameIndex)) cursor.getString(nameIndex) else null
        val size = if (sizeIndex >= 0 && !cursor.isNull(sizeIndex)) cursor.getLong(sizeIndex) else null
        return name to size
    }

    private fun readMediaMetadata(context: Context, uri: Uri): MediaMetadata {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(context, uri)
            val duration = retriever
                .extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                ?.toLongOrNull()
            val thumbnail = retriever.getFrameAtTime(0, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                ?: retriever.frameAtTime
            MediaMetadata(duration, thumbnail?.scaledToMaxSide(480))
        } catch (t: Throwable) {
            Log.w(TAG, "Cannot read video metadata uri=$uri", t)
            MediaMetadata()
        } finally {
            runCatching { retriever.release() }
        }
    }

    private fun Bitmap.scaledToMaxSide(maxSide: Int): Bitmap {
        val largestSide = maxOf(width, height)
        if (largestSide <= maxSide || largestSide <= 0) return this
        val scale = maxSide.toFloat() / largestSide.toFloat()
        val newWidth = (width * scale).roundToInt().coerceAtLeast(1)
        val newHeight = (height * scale).roundToInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(this, newWidth, newHeight, true)
    }

    private data class MediaMetadata(
        val durationMs: Long? = null,
        val thumbnail: Bitmap? = null,
    )
}
