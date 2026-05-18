package com.example.livewallpaper.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.preferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.livewallpaper.wallpaper.FillMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "wallpaper_settings")

class SettingsRepository(private val store: DataStore<Preferences>) {
    val settings: Flow<WallpaperSettings> = store.data.map { prefs ->
        WallpaperSettings(
            videoUri = prefs[VIDEO_URI]?.ifBlank { null },
            muted = prefs[MUTED] ?: true,
            fillMode = FillMode.fromStorage(prefs[FILL_MODE]),
            playbackSpeed = prefs[PLAYBACK_SPEED] ?: 1.0f,
            touchEffectEnabled = prefs[TOUCH_EFFECT_ENABLED] ?: true,
            parallaxEnabled = prefs[PARALLAX_ENABLED] ?: true,
        )
    }

    suspend fun updateVideoUri(uri: String?) = store.edit {
        if (uri.isNullOrBlank()) it.remove(VIDEO_URI) else it[VIDEO_URI] = uri
    }
    suspend fun updateMuted(value: Boolean) = store.edit { it[MUTED] = value }
    suspend fun updateFillMode(value: FillMode) = store.edit { it[FILL_MODE] = value.name }
    suspend fun updatePlaybackSpeed(value: Float) = store.edit { it[PLAYBACK_SPEED] = value }
    suspend fun updateTouchEffectEnabled(value: Boolean) = store.edit { it[TOUCH_EFFECT_ENABLED] = value }
    suspend fun updateParallaxEnabled(value: Boolean) = store.edit { it[PARALLAX_ENABLED] = value }

    companion object {
        val VIDEO_URI = preferencesKey<String>("video_uri")
        val MUTED = booleanPreferencesKey("muted")
        val FILL_MODE = preferencesKey<String>("fill_mode")
        val PLAYBACK_SPEED = floatPreferencesKey("playback_speed")
        val TOUCH_EFFECT_ENABLED = booleanPreferencesKey("touch_effect_enabled")
        val PARALLAX_ENABLED = booleanPreferencesKey("parallax_enabled")

        fun fromContext(context: Context): SettingsRepository = SettingsRepository(context.dataStore)
    }
}
