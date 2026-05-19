package com.example.livewallpaper.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import com.example.livewallpaper.wallpaper.FillMode
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class SettingsRepositoryTest {
    private fun createStore(): DataStore<Preferences> {
        val file = File(System.getProperty("java.io.tmpdir"), "test-settings-${System.nanoTime()}.preferences_pb")
        file.deleteOnExit()
        return PreferenceDataStoreFactory.create { file }
    }

    @Test
    fun defaultSettings() = runTest {
        val repo = SettingsRepository(createStore())

        val v = repo.settings.first()
        assertEquals(null, v.currentWallpaperId)
        assertEquals(null, v.videoUri)
        assertEquals(true, v.muted)
        assertEquals(FillMode.CENTER_CROP, v.fillMode)
        assertEquals(1.0f, v.playbackSpeed)
        assertEquals(null, v.startMs)
        assertEquals(null, v.endMs)
        assertEquals(1.0f, v.brightness)
        assertEquals(1.0f, v.contrast)
        assertEquals(1.0f, v.saturation)
        assertEquals(0f, v.warmth)
        assertEquals(0f, v.exposure)
        assertEquals(1.0f, v.scale)
        assertEquals(0f, v.offsetX)
        assertEquals(0f, v.offsetY)
        assertEquals(0f, v.rotation)
        assertEquals(false, v.mirror)
        assertEquals(0f, v.blur)
        assertEquals(0f, v.vignette)
        assertEquals(0f, v.topGradient)
        assertEquals(0f, v.bottomGradient)
        assertEquals(false, v.textEnabled)
        assertEquals("", v.overlayText)
        assertEquals(TEXT_POSITION_BOTTOM, v.textPosition)
        assertEquals(0.8f, v.textOpacity)
        assertEquals(PRESET_ORIGINAL, v.presetName)
        assertEquals(true, v.touchEffectEnabled)
        assertEquals(true, v.parallaxEnabled)
        assertEquals(null, v.lastPlaybackError)
    }

    @Test
    fun readWriteSettings() = runTest {
        val repo = SettingsRepository(createStore())
        repo.updateVideoUri("content://video.mp4")
        repo.updateMuted(false)
        repo.updateFillMode(FillMode.STRETCH)
        repo.updatePlaybackSpeed(1.5f)
        repo.updateTouchEffectEnabled(false)
        repo.updateParallaxEnabled(false)
        repo.updateLastPlaybackError("视频无法播放，请重新选择文件")

        val v = repo.settings.first()
        assertEquals("content://video.mp4", v.videoUri)
        assertEquals(false, v.muted)
        assertEquals(FillMode.STRETCH, v.fillMode)
        assertEquals(1.5f, v.playbackSpeed)
        assertTrue(!v.touchEffectEnabled)
        assertTrue(!v.parallaxEnabled)
        assertEquals("视频无法播放，请重新选择文件", v.lastPlaybackError)
    }

    @Test
    fun updatingVideoClearsPlaybackError() = runTest {
        val repo = SettingsRepository(createStore())
        repo.updateLastPlaybackError("视频无法播放，请重新选择文件")
        repo.updateVideoUri("content://new-video.mp4")

        val v = repo.settings.first()
        assertEquals("content://new-video.mp4", v.videoUri)
        assertEquals(null, v.lastPlaybackError)
    }

    @Test
    fun updateCurrentWallpaperStoresConfig() = runTest {
        val repo = SettingsRepository(createStore())
        val config = WallpaperConfig(
            fillMode = FillMode.FIT_CENTER,
            playbackSpeed = 1.5f,
            muted = false,
            startMs = 1_000L,
            endMs = 5_000L,
            brightness = 0.8f,
            contrast = 1.2f,
            saturation = 1.4f,
            warmth = -0.3f,
            exposure = 0.2f,
            scale = 1.3f,
            offsetX = 0.2f,
            offsetY = -0.4f,
            rotation = 8f,
            mirror = true,
            blur = 0.25f,
            vignette = 0.4f,
            topGradient = 0.3f,
            bottomGradient = 0.5f,
            textEnabled = true,
            overlayText = "LUMINA",
            textPosition = TEXT_POSITION_TOP,
            textOpacity = 0.6f,
            presetName = "cinema",
        )

        repo.updateCurrentWallpaper("content://video.mp4", "wp_1", config)

        val v = repo.settings.first()
        assertEquals("wp_1", v.currentWallpaperId)
        assertEquals("content://video.mp4", v.videoUri)
        assertEquals(false, v.muted)
        assertEquals(FillMode.FIT_CENTER, v.fillMode)
        assertEquals(1.5f, v.playbackSpeed)
        assertEquals(1_000L, v.startMs)
        assertEquals(5_000L, v.endMs)
        assertEquals(0.8f, v.brightness)
        assertEquals(1.2f, v.contrast)
        assertEquals(1.4f, v.saturation)
        assertEquals(-0.3f, v.warmth)
        assertEquals(0.2f, v.exposure)
        assertEquals(1.3f, v.scale)
        assertEquals(0.2f, v.offsetX)
        assertEquals(-0.4f, v.offsetY)
        assertEquals(8f, v.rotation)
        assertEquals(true, v.mirror)
        assertEquals(0.25f, v.blur)
        assertEquals(0.4f, v.vignette)
        assertEquals(0.3f, v.topGradient)
        assertEquals(0.5f, v.bottomGradient)
        assertEquals(true, v.textEnabled)
        assertEquals("LUMINA", v.overlayText)
        assertEquals(TEXT_POSITION_TOP, v.textPosition)
        assertEquals(0.6f, v.textOpacity)
        assertEquals("cinema", v.presetName)
    }
}
