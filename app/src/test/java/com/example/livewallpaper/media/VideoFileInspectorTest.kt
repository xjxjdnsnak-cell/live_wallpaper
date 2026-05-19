package com.example.livewallpaper.media

import org.junit.Assert.assertEquals
import org.junit.Test

class VideoFileInspectorTest {
    @Test
    fun formatDurationUnknown() {
        assertEquals(VideoFileInspector.UNKNOWN, VideoFileInspector.formatDuration(null))
        assertEquals(VideoFileInspector.UNKNOWN, VideoFileInspector.formatDuration(-1))
    }

    @Test
    fun formatDurationMinutesAndHours() {
        assertEquals("0:00", VideoFileInspector.formatDuration(0))
        assertEquals("1:05", VideoFileInspector.formatDuration(65_000))
        assertEquals("1:02:03", VideoFileInspector.formatDuration(3_723_000))
    }

    @Test
    fun formatSizeUnknownAndUnits() {
        assertEquals(VideoFileInspector.UNKNOWN, VideoFileInspector.formatSize(null))
        assertEquals(VideoFileInspector.UNKNOWN, VideoFileInspector.formatSize(-1))
        assertEquals("512 B", VideoFileInspector.formatSize(512))
        assertEquals("1 KB", VideoFileInspector.formatSize(1024))
        assertEquals("1.5 KB", VideoFileInspector.formatSize(1536))
        assertEquals("2 MB", VideoFileInspector.formatSize(2 * 1024 * 1024))
    }

    @Test
    fun playbackErrorSummaryIsUserSafe() {
        assertEquals(
            "视频无法播放，请重新选择文件",
            VideoFileInspector.playbackErrorSummary(),
        )
    }
}
