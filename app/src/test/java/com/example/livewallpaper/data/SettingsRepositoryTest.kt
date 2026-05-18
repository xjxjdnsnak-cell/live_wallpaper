package com.example.livewallpaper.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.test.core.app.ApplicationProvider
import com.example.livewallpaper.wallpaper.FillMode
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class SettingsRepositoryTest {
    private fun createStore(): DataStore<Preferences> {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val file = File(context.cacheDir, "test-settings-${System.nanoTime()}.preferences_pb")
        return PreferenceDataStoreFactory.create { file }
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

        val v = repo.settings.first()
        assertEquals("content://video.mp4", v.videoUri)
        assertEquals(false, v.muted)
        assertEquals(FillMode.STRETCH, v.fillMode)
        assertEquals(1.5f, v.playbackSpeed)
        assertTrue(!v.touchEffectEnabled)
        assertTrue(!v.parallaxEnabled)
    }
}
