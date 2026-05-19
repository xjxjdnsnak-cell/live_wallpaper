package com.example.livewallpaper.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.livewallpaper.wallpaper.FillMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "wallpaper_settings")

class SettingsRepository(private val store: DataStore<Preferences>) {
    val settings: Flow<WallpaperSettings> = store.data.map { prefs ->
        WallpaperSettings(
            currentWallpaperId = prefs[CURRENT_WALLPAPER_ID]?.ifBlank { null },
            videoUri = prefs[VIDEO_URI]?.ifBlank { null },
            muted = prefs[MUTED] ?: true,
            fillMode = FillMode.fromStorage(prefs[FILL_MODE]),
            playbackSpeed = prefs[PLAYBACK_SPEED] ?: 1.0f,
            startMs = prefs[START_MS],
            endMs = prefs[END_MS],
            brightness = prefs[BRIGHTNESS] ?: 1.0f,
            contrast = prefs[CONTRAST] ?: 1.0f,
            saturation = prefs[SATURATION] ?: 1.0f,
            warmth = prefs[WARMTH] ?: 0f,
            exposure = prefs[EXPOSURE] ?: 0f,
            scale = prefs[SCALE] ?: 1.0f,
            offsetX = prefs[OFFSET_X] ?: 0f,
            offsetY = prefs[OFFSET_Y] ?: 0f,
            rotation = prefs[ROTATION] ?: 0f,
            mirror = prefs[MIRROR] ?: false,
            blur = prefs[BLUR] ?: 0f,
            vignette = prefs[VIGNETTE] ?: 0f,
            topGradient = prefs[TOP_GRADIENT] ?: 0f,
            bottomGradient = prefs[BOTTOM_GRADIENT] ?: 0f,
            textEnabled = prefs[TEXT_ENABLED] ?: false,
            overlayText = prefs[OVERLAY_TEXT].orEmpty(),
            textPosition = prefs[TEXT_POSITION] ?: TEXT_POSITION_BOTTOM,
            textOpacity = prefs[TEXT_OPACITY] ?: 0.8f,
            presetName = prefs[PRESET_NAME] ?: PRESET_ORIGINAL,
            touchEffectEnabled = prefs[TOUCH_EFFECT_ENABLED] ?: true,
            parallaxEnabled = prefs[PARALLAX_ENABLED] ?: true,
            lastPlaybackError = prefs[LAST_PLAYBACK_ERROR]?.ifBlank { null },
        )
    }

    suspend fun updateVideoUri(uri: String?) = store.edit {
        if (uri.isNullOrBlank()) {
            it.remove(VIDEO_URI)
            it.remove(CURRENT_WALLPAPER_ID)
            it.remove(START_MS)
            it.remove(END_MS)
        } else {
            it[VIDEO_URI] = uri
        }
        it.remove(LAST_PLAYBACK_ERROR)
    }
    suspend fun updateCurrentWallpaper(uri: String?, wallpaperId: String?, config: WallpaperConfig?) = store.edit {
        if (uri.isNullOrBlank()) it.remove(VIDEO_URI) else it[VIDEO_URI] = uri
        if (wallpaperId.isNullOrBlank()) it.remove(CURRENT_WALLPAPER_ID) else it[CURRENT_WALLPAPER_ID] = wallpaperId

        val nextConfig = config ?: WallpaperConfig()
        it[MUTED] = nextConfig.muted
        it[FILL_MODE] = nextConfig.fillMode.name
        it[PLAYBACK_SPEED] = nextConfig.playbackSpeed
        if (nextConfig.startMs == null) it.remove(START_MS) else it[START_MS] = nextConfig.startMs
        if (nextConfig.endMs == null) it.remove(END_MS) else it[END_MS] = nextConfig.endMs
        it[BRIGHTNESS] = nextConfig.brightness
        it[CONTRAST] = nextConfig.contrast
        it[SATURATION] = nextConfig.saturation
        it[WARMTH] = nextConfig.warmth
        it[EXPOSURE] = nextConfig.exposure
        it[SCALE] = nextConfig.scale
        it[OFFSET_X] = nextConfig.offsetX
        it[OFFSET_Y] = nextConfig.offsetY
        it[ROTATION] = nextConfig.rotation
        it[MIRROR] = nextConfig.mirror
        it[BLUR] = nextConfig.blur
        it[VIGNETTE] = nextConfig.vignette
        it[TOP_GRADIENT] = nextConfig.topGradient
        it[BOTTOM_GRADIENT] = nextConfig.bottomGradient
        it[TEXT_ENABLED] = nextConfig.textEnabled
        it[OVERLAY_TEXT] = nextConfig.overlayText
        it[TEXT_POSITION] = nextConfig.textPosition
        it[TEXT_OPACITY] = nextConfig.textOpacity
        it[PRESET_NAME] = nextConfig.presetName
        it.remove(LAST_PLAYBACK_ERROR)
    }
    suspend fun updateMuted(value: Boolean) = store.edit { it[MUTED] = value }
    suspend fun updateFillMode(value: FillMode) = store.edit { it[FILL_MODE] = value.name }
    suspend fun updatePlaybackSpeed(value: Float) = store.edit { it[PLAYBACK_SPEED] = value }
    suspend fun updateTouchEffectEnabled(value: Boolean) = store.edit { it[TOUCH_EFFECT_ENABLED] = value }
    suspend fun updateParallaxEnabled(value: Boolean) = store.edit { it[PARALLAX_ENABLED] = value }
    suspend fun updateLastPlaybackError(value: String?) = store.edit {
        if (value.isNullOrBlank()) it.remove(LAST_PLAYBACK_ERROR) else it[LAST_PLAYBACK_ERROR] = value
    }

    companion object {
        val CURRENT_WALLPAPER_ID = stringPreferencesKey("current_wallpaper_id")
        val VIDEO_URI = stringPreferencesKey("video_uri")
        val MUTED = booleanPreferencesKey("muted")
        val FILL_MODE = stringPreferencesKey("fill_mode")
        val PLAYBACK_SPEED = floatPreferencesKey("playback_speed")
        val START_MS = longPreferencesKey("start_ms")
        val END_MS = longPreferencesKey("end_ms")
        val BRIGHTNESS = floatPreferencesKey("brightness")
        val CONTRAST = floatPreferencesKey("contrast")
        val SATURATION = floatPreferencesKey("saturation")
        val WARMTH = floatPreferencesKey("warmth")
        val EXPOSURE = floatPreferencesKey("exposure")
        val SCALE = floatPreferencesKey("scale")
        val OFFSET_X = floatPreferencesKey("offset_x")
        val OFFSET_Y = floatPreferencesKey("offset_y")
        val ROTATION = floatPreferencesKey("rotation")
        val MIRROR = booleanPreferencesKey("mirror")
        val BLUR = floatPreferencesKey("blur")
        val VIGNETTE = floatPreferencesKey("vignette")
        val TOP_GRADIENT = floatPreferencesKey("top_gradient")
        val BOTTOM_GRADIENT = floatPreferencesKey("bottom_gradient")
        val TEXT_ENABLED = booleanPreferencesKey("text_enabled")
        val OVERLAY_TEXT = stringPreferencesKey("overlay_text")
        val TEXT_POSITION = stringPreferencesKey("text_position")
        val TEXT_OPACITY = floatPreferencesKey("text_opacity")
        val PRESET_NAME = stringPreferencesKey("preset_name")
        val TOUCH_EFFECT_ENABLED = booleanPreferencesKey("touch_effect_enabled")
        val PARALLAX_ENABLED = booleanPreferencesKey("parallax_enabled")
        val LAST_PLAYBACK_ERROR = stringPreferencesKey("last_playback_error")

        fun fromContext(context: Context): SettingsRepository = SettingsRepository(context.dataStore)
    }
}
