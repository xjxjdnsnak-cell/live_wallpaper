package com.example.livewallpaper.data

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.livewallpaper.media.VideoFileInspector
import com.example.livewallpaper.media.VideoGenerationResult
import com.example.livewallpaper.wallpaper.FillMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import kotlin.math.abs

private val Context.wallpaperLibraryStore by preferencesDataStore(name = "wallpaper_library")

class WallpaperLibraryRepository(private val store: DataStore<Preferences>) {
    val state: Flow<WallpaperLibraryState> = store.data.map { prefs ->
        WallpaperLibraryState(
            items = decodeItems(prefs[WALLPAPER_ITEMS_JSON].orEmpty()),
            currentWallpaperId = prefs[CURRENT_WALLPAPER_ID]?.ifBlank { null },
        )
    }

    suspend fun importVideos(context: Context, uris: List<Uri>) {
        if (uris.isEmpty()) return
        val now = System.currentTimeMillis()
        store.edit { prefs ->
            val existing = decodeItems(prefs[WALLPAPER_ITEMS_JSON].orEmpty())
            val byUri = existing.associateBy { it.uri }.toMutableMap()
            uris.forEach { uri ->
                val uriString = uri.toString()
                val old = byUri[uriString]
                val id = old?.id ?: idForUri(uriString)
                val inspection = VideoFileInspector.inspect(context, uriString)
                val thumbnailPath = inspection.thumbnail?.let { saveThumbnail(context, id, it) } ?: old?.thumbnailPath
                byUri[uriString] = WallpaperItem(
                    id = id,
                    uri = uriString,
                    displayName = inspection.displayName ?: old?.displayName,
                    durationMs = inspection.durationMs ?: old?.durationMs,
                    sizeBytes = inspection.sizeBytes ?: old?.sizeBytes,
                    thumbnailPath = thumbnailPath,
                    category = old?.category ?: WallpaperItem.DEFAULT_CATEGORY,
                    isFavorite = old?.isFavorite ?: false,
                    createdAt = old?.createdAt ?: now,
                    lastViewedAt = old?.lastViewedAt,
                    useCount = old?.useCount ?: 0,
                    config = old?.config ?: WallpaperConfig(),
                    isGenerated = old?.isGenerated ?: false,
                    originalUri = old?.originalUri,
                    sourceWallpaperId = old?.sourceWallpaperId,
                    generatedAt = old?.generatedAt,
                    generatedFromConfig = old?.generatedFromConfig ?: false,
                )
            }
            prefs[WALLPAPER_ITEMS_JSON] = encodeItems(byUri.values.sortedByDescending { it.createdAt })
        }
    }

    suspend fun addGeneratedWallpaper(
        context: Context,
        source: WallpaperItem,
        result: VideoGenerationResult.Success,
        config: WallpaperConfig,
    ): WallpaperItem {
        val now = System.currentTimeMillis()
        val id = idForUri(result.uri)
        val inspection = VideoFileInspector.inspect(context, result.uri)
        val thumbnailPath = inspection.thumbnail?.let { saveThumbnail(context, id, it) }
        val generatedItem = WallpaperItem(
            id = id,
            uri = result.uri,
            displayName = adaptedName(source.displayName ?: inspection.displayName ?: "wallpaper"),
            durationMs = inspection.durationMs ?: result.durationMs,
            sizeBytes = inspection.sizeBytes ?: result.sizeBytes,
            thumbnailPath = thumbnailPath,
            category = source.category,
            isFavorite = false,
            createdAt = now,
            lastViewedAt = null,
            useCount = 0,
            config = config.copy(startMs = null, endMs = null, muted = true),
            isGenerated = true,
            originalUri = source.uri,
            sourceWallpaperId = source.id,
            generatedAt = now,
            generatedFromConfig = true,
        )
        store.edit { prefs ->
            val items = decodeItems(prefs[WALLPAPER_ITEMS_JSON].orEmpty())
            prefs[WALLPAPER_ITEMS_JSON] = encodeItems(listOf(generatedItem) + items.filterNot { it.id == id })
        }
        return generatedItem
    }

    suspend fun setCurrentWallpaper(id: String) = updateItem(id) { item ->
        item.copy(useCount = item.useCount + 1)
    }.also {
        store.edit { prefs -> prefs[CURRENT_WALLPAPER_ID] = id }
    }

    suspend fun clearCurrentWallpaper() {
        store.edit { prefs -> prefs.remove(CURRENT_WALLPAPER_ID) }
    }

    suspend fun markViewed(id: String) = updateItem(id) { item ->
        item.copy(lastViewedAt = System.currentTimeMillis())
    }

    suspend fun setFavorite(id: String, favorite: Boolean) = updateItem(id) { item ->
        item.copy(isFavorite = favorite)
    }

    suspend fun setCategory(id: String, category: String) = updateItem(id) { item ->
        item.copy(category = category.takeIf { it in WallpaperItem.CATEGORIES } ?: WallpaperItem.DEFAULT_CATEGORY)
    }

    suspend fun updateConfig(id: String, config: WallpaperConfig) = updateItem(id) { item ->
        item.copy(config = config)
    }

    suspend fun delete(context: Context, id: String): WallpaperItem? {
        var deleted: WallpaperItem? = null
        store.edit { prefs ->
            val items = decodeItems(prefs[WALLPAPER_ITEMS_JSON].orEmpty())
            deleted = items.firstOrNull { it.id == id }
            val kept = items.filterNot { it.id == id }
            prefs[WALLPAPER_ITEMS_JSON] = encodeItems(kept)
            if (prefs[CURRENT_WALLPAPER_ID] == id) {
                prefs.remove(CURRENT_WALLPAPER_ID)
            }
        }
        deleted?.let { deleteGeneratedFileIfOwned(context, it) }
        return deleted
    }

    private suspend fun updateItem(id: String, transform: (WallpaperItem) -> WallpaperItem) {
        store.edit { prefs ->
            val items = decodeItems(prefs[WALLPAPER_ITEMS_JSON].orEmpty())
            prefs[WALLPAPER_ITEMS_JSON] = encodeItems(items.map { if (it.id == id) transform(it) else it })
        }
    }

    companion object {
        val WALLPAPER_ITEMS_JSON = stringPreferencesKey("wallpaper_items_json")
        val CURRENT_WALLPAPER_ID = stringPreferencesKey("current_wallpaper_id")

        fun fromContext(context: Context): WallpaperLibraryRepository = WallpaperLibraryRepository(context.wallpaperLibraryStore)
        fun idForUri(uri: String): String = stableId(uri)
    }
}

private fun stableId(uri: String): String = "wp_${abs(uri.hashCode())}_${uri.length}"

private fun saveThumbnail(context: Context, id: String, bitmap: Bitmap): String {
    val dir = File(context.filesDir, "wallpaper_thumbnails").apply { mkdirs() }
    val file = File(dir, "$id.png")
    file.outputStream().use { output ->
        bitmap.compress(Bitmap.CompressFormat.PNG, 90, output)
    }
    return file.absolutePath
}

private fun encodeItems(items: List<WallpaperItem>): String {
    val array = JSONArray()
    items.forEach { item ->
        array.put(
            JSONObject()
                .put("id", item.id)
                .put("uri", item.uri)
                .putNullable("displayName", item.displayName)
                .putNullable("durationMs", item.durationMs)
                .putNullable("sizeBytes", item.sizeBytes)
                .putNullable("thumbnailPath", item.thumbnailPath)
                .put("category", item.category)
                .put("isFavorite", item.isFavorite)
                .put("createdAt", item.createdAt)
                .putNullable("lastViewedAt", item.lastViewedAt)
                .put("useCount", item.useCount)
                .put("config", encodeConfig(item.config))
                .put("isGenerated", item.isGenerated)
                .putNullable("originalUri", item.originalUri)
                .putNullable("sourceWallpaperId", item.sourceWallpaperId)
                .putNullable("generatedAt", item.generatedAt)
                .put("generatedFromConfig", item.generatedFromConfig),
        )
    }
    return array.toString()
}

private fun decodeItems(json: String): List<WallpaperItem> {
    if (json.isBlank()) return emptyList()
    return runCatching {
        val array = JSONArray(json)
        buildList {
            for (index in 0 until array.length()) {
                val obj = array.getJSONObject(index)
                add(
                    WallpaperItem(
                        id = obj.getString("id"),
                        uri = obj.getString("uri"),
                        displayName = obj.optNullableString("displayName"),
                        durationMs = obj.optNullableLong("durationMs"),
                        sizeBytes = obj.optNullableLong("sizeBytes"),
                        thumbnailPath = obj.optNullableString("thumbnailPath"),
                        category = obj.optString("category", WallpaperItem.DEFAULT_CATEGORY),
                        isFavorite = obj.optBoolean("isFavorite", false),
                        createdAt = obj.optLong("createdAt", 0L),
                        lastViewedAt = obj.optNullableLong("lastViewedAt"),
                        useCount = obj.optInt("useCount", 0),
                        config = decodeConfig(obj.optJSONObject("config")),
                        isGenerated = obj.optBoolean("isGenerated", false),
                        originalUri = obj.optNullableString("originalUri"),
                        sourceWallpaperId = obj.optNullableString("sourceWallpaperId"),
                        generatedAt = obj.optNullableLong("generatedAt"),
                        generatedFromConfig = obj.optBoolean("generatedFromConfig", false),
                    ),
                )
            }
        }
    }.getOrDefault(emptyList())
}

private fun encodeConfig(config: WallpaperConfig): JSONObject =
    JSONObject()
        .put("fillMode", config.fillMode.name)
        .put("playbackSpeed", config.playbackSpeed)
        .put("muted", config.muted)
        .putNullable("startMs", config.startMs)
        .putNullable("endMs", config.endMs)
        .put("brightness", config.brightness)
        .put("contrast", config.contrast)
        .put("saturation", config.saturation)
        .put("warmth", config.warmth)
        .put("exposure", config.exposure)
        .put("scale", config.scale)
        .put("offsetX", config.offsetX)
        .put("offsetY", config.offsetY)
        .put("rotation", config.rotation)
        .put("mirror", config.mirror)
        .put("blur", config.blur)
        .put("vignette", config.vignette)
        .put("topGradient", config.topGradient)
        .put("bottomGradient", config.bottomGradient)
        .put("textEnabled", config.textEnabled)
        .put("overlayText", config.overlayText)
        .put("textPosition", config.textPosition)
        .put("textOpacity", config.textOpacity)
        .put("presetName", config.presetName)

private fun decodeConfig(obj: JSONObject?): WallpaperConfig {
    if (obj == null) return WallpaperConfig()
    return WallpaperConfig(
        fillMode = FillMode.fromStorage(obj.optNullableString("fillMode")),
        playbackSpeed = obj.optDouble("playbackSpeed", 1.0).toFloat().coerceAtLeast(0.1f),
        muted = obj.optBoolean("muted", true),
        startMs = obj.optNullableLong("startMs"),
        endMs = obj.optNullableLong("endMs"),
        brightness = obj.optFloat("brightness", 1.0f).coerceIn(0.5f, 1.5f),
        contrast = obj.optFloat("contrast", 1.0f).coerceIn(0.5f, 1.5f),
        saturation = obj.optFloat("saturation", 1.0f).coerceIn(0f, 2.0f),
        warmth = obj.optFloat("warmth", 0f).coerceIn(-1f, 1f),
        exposure = obj.optFloat("exposure", 0f).coerceIn(-1f, 1f),
        scale = obj.optFloat("scale", 1.0f).coerceIn(0.8f, 2.0f),
        offsetX = obj.optFloat("offsetX", 0f).coerceIn(-1f, 1f),
        offsetY = obj.optFloat("offsetY", 0f).coerceIn(-1f, 1f),
        rotation = obj.optFloat("rotation", 0f).coerceIn(-15f, 15f),
        mirror = obj.optBoolean("mirror", false),
        blur = obj.optFloat("blur", 0f).coerceIn(0f, 1f),
        vignette = obj.optFloat("vignette", 0f).coerceIn(0f, 1f),
        topGradient = obj.optFloat("topGradient", 0f).coerceIn(0f, 1f),
        bottomGradient = obj.optFloat("bottomGradient", 0f).coerceIn(0f, 1f),
        textEnabled = obj.optBoolean("textEnabled", false),
        overlayText = obj.optNullableString("overlayText").orEmpty(),
        textPosition = obj.optNullableString("textPosition") ?: TEXT_POSITION_BOTTOM,
        textOpacity = obj.optFloat("textOpacity", 0.8f).coerceIn(0f, 1f),
        presetName = obj.optNullableString("presetName") ?: PRESET_ORIGINAL,
    )
}

private fun JSONObject.putNullable(key: String, value: Any?): JSONObject {
    if (value == null) put(key, JSONObject.NULL) else put(key, value)
    return this
}

private fun JSONObject.optNullableString(key: String): String? {
    if (!has(key) || isNull(key)) return null
    return optString(key).ifBlank { null }
}

private fun JSONObject.optNullableLong(key: String): Long? {
    if (!has(key) || isNull(key)) return null
    return optLong(key)
}

private fun JSONObject.optFloat(key: String, defaultValue: Float): Float {
    if (!has(key) || isNull(key)) return defaultValue
    return optDouble(key, defaultValue.toDouble()).toFloat()
}

private fun adaptedName(name: String): String {
    val base = name.substringBeforeLast('.', name)
    return "${base}_已适配.mp4"
}

private fun deleteGeneratedFileIfOwned(context: Context, item: WallpaperItem) {
    if (!item.isGenerated) return
    val outputDir = context.getExternalFilesDir("generated_wallpapers") ?: return
    val file = runCatching {
        val uri = Uri.parse(item.uri)
        if (uri.scheme == "file") File(uri.path.orEmpty()) else File(item.uri)
    }.getOrNull() ?: return
    val outputRoot = runCatching { outputDir.canonicalFile }.getOrNull() ?: return
    val target = runCatching { file.canonicalFile }.getOrNull() ?: return
    if (target.toPath().startsWith(outputRoot.toPath()) && target.isFile) {
        runCatching { target.delete() }
    }
}
