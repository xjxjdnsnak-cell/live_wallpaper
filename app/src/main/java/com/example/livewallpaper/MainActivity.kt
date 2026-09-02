package com.example.livewallpaper

import android.app.ActivityManager
import android.app.WallpaperManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.util.LruCache
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.livewallpaper.data.SettingsRepository
import com.example.livewallpaper.data.WallpaperItem
import com.example.livewallpaper.data.WallpaperLibraryRepository
import com.example.livewallpaper.data.WallpaperSettings
import com.example.livewallpaper.media.VideoUriPermissionHelper
import com.example.livewallpaper.media.VideoGenerationResult
import com.example.livewallpaper.media.WallpaperVideoGenerator
import com.example.livewallpaper.ui.CategoryScreen
import com.example.livewallpaper.ui.FavoritesScreen
import com.example.livewallpaper.ui.HomeScreen
import com.example.livewallpaper.ui.LuminaBackground
import com.example.livewallpaper.ui.LuminaTheme
import com.example.livewallpaper.ui.PreviewScreen
import com.example.livewallpaper.ui.ProfileScreen
import com.example.livewallpaper.ui.SettingsScreen
import com.example.livewallpaper.ui.SplashScreen
import com.example.livewallpaper.ui.WallpaperDetailScreen
import com.example.livewallpaper.ui.WallpaperEditorScreen
import com.example.livewallpaper.ui.components.LuminaBottomNavBar
import com.example.livewallpaper.ui.components.LuminaTab
import com.example.livewallpaper.wallpaper.InteractiveVideoWallpaperService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class MainActivity : ComponentActivity() {
    private val settingsRepository by lazy { SettingsRepository.fromContext(this) }
    private val libraryRepository by lazy { WallpaperLibraryRepository.fromContext(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { LuminaTheme { App(settingsRepository, libraryRepository, ::openWallpaperPicker) } }
    }

    private fun openWallpaperPicker() {
        val componentName = ComponentName(this, InteractiveVideoWallpaperService::class.java)
        try {
            startActivity(
                Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER)
                    .putExtra(WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT, componentName),
            )
        } catch (t: Throwable) {
            Log.w("MainActivity", "Direct wallpaper intent failed", t)
            try {
                startActivity(Intent(WallpaperManager.ACTION_LIVE_WALLPAPER_CHOOSER))
            } catch (inner: Throwable) {
                Log.e("MainActivity", "Wallpaper chooser unavailable", inner)
                startActivity(Intent(Settings.ACTION_SETTINGS))
            }
        }
    }
}

@Composable
private fun App(
    settingsRepository: SettingsRepository,
    libraryRepository: WallpaperLibraryRepository,
    onSetWallpaper: () -> Unit,
) {
    val context = LocalContext.current
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val settings by settingsRepository.settings.collectAsState(initial = WallpaperSettings())
    val libraryState by libraryRepository.state.collectAsState(initial = com.example.livewallpaper.data.WallpaperLibraryState())
    // Audit P-6: bounded thumbnail cache instead of an unbounded in-memory map.
    val thumbnails = remember {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
        val fromMemoryClass = (activityManager?.memoryClass ?: 0) * 1024 * 1024 / 8
        ThumbnailCache(if (fromMemoryClass > 0) fromMemoryClass else THUMBNAIL_CACHE_FALLBACK_BYTES)
    }
    val scope = rememberCoroutineScope()
    val currentItem = libraryState.items.firstOrNull { it.id == libraryState.currentWallpaperId }
        ?: libraryState.items.firstOrNull { it.id == settings.currentWallpaperId }
        ?: libraryState.items.firstOrNull { it.uri == settings.videoUri }

    suspend fun setCurrent(item: WallpaperItem) {
        libraryRepository.setCurrentWallpaper(item.id)
        settingsRepository.updateCurrentWallpaper(item.uri, item.id, item.config)
    }

    suspend fun clearCurrent() {
        libraryRepository.clearCurrentWallpaper()
        settingsRepository.updateCurrentWallpaper(null, null, null)
    }

    suspend fun importVideos(uris: List<Uri>) {
        if (uris.isEmpty()) return
        uris.forEach { uri ->
            VideoUriPermissionHelper.takePersistableUriPermission(
                context.contentResolver,
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION,
            )
        }
        withContext(Dispatchers.IO) { libraryRepository.importVideos(context, uris) }
        val firstUri = uris.first().toString()
        val id = WallpaperLibraryRepository.idForUri(firstUri)
        libraryRepository.setCurrentWallpaper(id)
        settingsRepository.updateCurrentWallpaper(firstUri, id, libraryState.items.firstOrNull { it.id == id }?.config)
    }

    val pickVideos = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        scope.launch { importVideos(uris) }
    }

    fun openDetail(id: String) {
        navController.navigate("detail/$id")
    }

    LaunchedEffect(settings.videoUri, libraryState.items) {
        val currentUri = settings.videoUri?.takeIf { it.isNotBlank() } ?: return@LaunchedEffect
        if (libraryState.items.none { it.uri == currentUri }) {
            withContext(Dispatchers.IO) { libraryRepository.importVideos(context, listOf(Uri.parse(currentUri))) }
        } else if (libraryState.currentWallpaperId == null) {
            libraryState.items.firstOrNull { it.uri == currentUri }?.let { item ->
                libraryRepository.setCurrentWallpaper(item.id)
                settingsRepository.updateCurrentWallpaper(item.uri, item.id, item.config)
            }
        }
    }

    LaunchedEffect(libraryState.items) {
        val missing = libraryState.items.filter { item ->
            item.thumbnailPath != null && thumbnails[item.id] == null
        }
        if (missing.isNotEmpty()) {
            val loaded = withContext(Dispatchers.IO) {
                missing.mapNotNull { item ->
                    val bitmap = item.thumbnailPath?.let { path ->
                        runCatching { BitmapFactory.decodeFile(path.takeIf { File(it).exists() }) }.getOrNull()
                    }
                    if (bitmap != null) item.id to bitmap else null
                }
            }
            loaded.forEach { (id, bitmap) ->
                thumbnails.put(id, bitmap)
            }
        }
    }

    val rootTabs = listOf(
        LuminaTab("home", "首页", "H"),
        LuminaTab("category", "发现", "D"),
        LuminaTab("favorites", "收藏", "F"),
        LuminaTab("profile", "我的", "M"),
    )
    val showBottomBar = currentRoute in rootTabs.map { it.route }

    Scaffold(
        containerColor = LuminaBackground,
        bottomBar = {
            if (showBottomBar) {
                LuminaBottomNavBar(
                    selectedRoute = currentRoute ?: "home",
                    tabs = rootTabs,
                    onTabSelected = { route ->
                        navController.navigate(route) {
                            popUpTo("home") { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onAddVideo = { pickVideos.launch(arrayOf("video/*")) },
                )
            }
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = "splash",
            modifier = Modifier.padding(if (showBottomBar) padding else PaddingValues()),
        ) {
            composable("splash") {
                SplashScreen {
                    navController.navigate("home") {
                        popUpTo("splash") { inclusive = true }
                    }
                }
            }
            composable("home") {
                HomeScreen(
                    settings = settings,
                    items = libraryState.items,
                    currentItem = currentItem,
                    thumbnails = thumbnails,
                    onPickVideo = { pickVideos.launch(arrayOf("video/*")) },
                    onClearVideo = { scope.launch { clearCurrent() } },
                    onPreview = { navController.navigate("preview") },
                    onOpenSettings = { navController.navigate("settings") },
                    onSetWallpaper = onSetWallpaper,
                    onOpenCategory = { navController.navigate("category") },
                    onOpenDetail = ::openDetail,
                )
            }
            composable("category") {
                CategoryScreen(
                    items = libraryState.items,
                    thumbnails = thumbnails,
                    onOpenDetail = ::openDetail,
                )
            }
            composable("favorites") {
                FavoritesScreen(
                    items = libraryState.items.filter { it.isFavorite },
                    thumbnails = thumbnails,
                    onOpenDetail = ::openDetail,
                    onToggleFavorite = { item -> scope.launch { libraryRepository.setFavorite(item.id, false) } },
                )
            }
            composable("profile") {
                ProfileScreen(
                    settings = settings,
                    items = libraryState.items,
                    currentItem = currentItem,
                    thumbnails = thumbnails,
                    onOpenSettings = { navController.navigate("settings") },
                    onClearVideo = { scope.launch { clearCurrent() } },
                    onPreview = { navController.navigate("preview") },
                    onSetWallpaper = onSetWallpaper,
                    onOpenDetail = ::openDetail,
                )
            }
            composable("detail/{wallpaperId}") { entry ->
                val id = entry.arguments?.getString("wallpaperId").orEmpty()
                val item = libraryState.items.firstOrNull { it.id == id }
                val sourceItem = item?.sourceWallpaperId?.let { sourceId ->
                    libraryState.items.firstOrNull { it.id == sourceId }
                }
                LaunchedEffect(id) {
                    if (id.isNotBlank()) libraryRepository.markViewed(id)
                }
                WallpaperDetailScreen(
                    item = item,
                    sourceItem = sourceItem,
                    thumbnail = item?.let { thumbnails[it.id] },
                    isCurrent = item != null && (item.id == libraryState.currentWallpaperId || item.uri == settings.videoUri),
                    onBack = { navController.popBackStack() },
                    onPreview = { navController.navigate("preview/$id") },
                    onEdit = { selected -> navController.navigate("editor/${selected.id}") },
                    onSetWallpaper = onSetWallpaper,
                    onSetCurrent = { selected -> scope.launch { setCurrent(selected) } },
                    onToggleFavorite = { selected -> scope.launch { libraryRepository.setFavorite(selected.id, !selected.isFavorite) } },
                    onDelete = { selected ->
                        scope.launch {
                            val deleted = libraryRepository.delete(context, selected.id)
                            if (deleted?.uri == settings.videoUri) settingsRepository.updateCurrentWallpaper(null, null, null)
                            navController.popBackStack()
                        }
                    },
                    onCategoryChanged = { selected, category ->
                        scope.launch { libraryRepository.setCategory(selected.id, category) }
                    },
                )
            }
            composable("editor/{wallpaperId}") { entry ->
                val id = entry.arguments?.getString("wallpaperId").orEmpty()
                val item = libraryState.items.firstOrNull { it.id == id }
                WallpaperEditorScreen(
                    item = item,
                    thumbnail = item?.let { thumbnails[it.id] },
                    onBack = { navController.popBackStack() },
                    onSave = { selected, config ->
                        scope.launch {
                            libraryRepository.updateConfig(selected.id, config)
                            if (selected.id == libraryState.currentWallpaperId || selected.uri == settings.videoUri) {
                                settingsRepository.updateCurrentWallpaper(selected.uri, selected.id, config)
                            }
                            navController.popBackStack()
                        }
                    },
                    onGenerateCopy = { selected, config, onProgress ->
                        withContext(Dispatchers.IO) {
                            val result = WallpaperVideoGenerator.generate(
                                context = context,
                                sourceUri = selected.uri,
                                config = config,
                                outputFileName = generatedOutputName(selected),
                                onProgress = onProgress,
                            )
                            when (result) {
                                is VideoGenerationResult.Success -> {
                                    val generated = libraryRepository.addGeneratedWallpaper(
                                        context = context,
                                        source = selected,
                                        result = result,
                                        config = config,
                                    )
                                    Result.success(generated)
                                }
                                is VideoGenerationResult.Failure -> {
                                    Result.failure(IllegalStateException(result.message, result.cause))
                                }
                            }
                        }
                    },
                    onOpenGenerated = { generated ->
                        navController.navigate("detail/${generated.id}")
                    },
                    onSetGeneratedCurrent = { generated ->
                        scope.launch { setCurrent(generated) }
                    },
                )
            }
            composable("preview") {
                PreviewScreen(
                    settings = settings,
                    thumbnail = currentItem?.let { thumbnails[it.id] },
                    config = currentItem?.config,
                    onBack = { navController.popBackStack() },
                )
            }
            composable("preview/{wallpaperId}") { entry ->
                val id = entry.arguments?.getString("wallpaperId").orEmpty()
                val item = libraryState.items.firstOrNull { it.id == id }
                PreviewScreen(
                    settings = settings.copy(videoUri = item?.uri ?: settings.videoUri),
                    thumbnail = item?.let { thumbnails[it.id] },
                    config = item?.config,
                    onBack = { navController.popBackStack() },
                )
            }
            composable("settings") {
                SettingsScreen(
                    settings,
                    onBack = { navController.popBackStack() },
                    onMutedChanged = { scope.launch { settingsRepository.updateMuted(it) } },
                    onFillModeChanged = { scope.launch { settingsRepository.updateFillMode(it) } },
                    onSpeedChanged = { scope.launch { settingsRepository.updatePlaybackSpeed(it) } },
                    onTouchChanged = { scope.launch { settingsRepository.updateTouchEffectEnabled(it) } },
                    onParallaxChanged = { scope.launch { settingsRepository.updateParallaxEnabled(it) } },
                )
            }
        }
    }
}

private fun generatedOutputName(item: WallpaperItem): String {
    val base = (item.displayName ?: "wallpaper").substringBeforeLast('.', item.displayName ?: "wallpaper")
    return "${base}_${System.currentTimeMillis()}_adapted.mp4"
}

private const val THUMBNAIL_CACHE_FALLBACK_BYTES = 48 * 1024 * 1024

/**
 * Size-bounded thumbnail store (audit P-6).
 *
 * Backed by [LruCache] capped at a byte budget (sized by the caller from
 * ActivityManager.memoryClass). It stays a read-only [Map] so all screens keep
 * indexing it unchanged. Compose invalidation: readers subscribe to [revision]
 * inside [get], and [put] — the only mutation, hence the only moment eviction
 * can happen — bumps it, so compositions re-resolve and evicted entries render
 * the existing placeholder. Evicted bitmaps are recycled; on minSdk 29 their
 * pixels live on the Java heap anyway, so at worst a GC would have freed them.
 */
private class ThumbnailCache(maxBytes: Int) : AbstractMap<String, Bitmap>() {
    private val revision = mutableStateOf(0)
    private val cache = object : LruCache<String, Bitmap>(maxBytes) {
        override fun sizeOf(key: String, value: Bitmap): Int = value.byteCount

        override fun entryRemoved(evicted: Boolean, key: String, oldValue: Bitmap, newValue: Bitmap?) {
            if (evicted) oldValue.recycle()
        }
    }

    override val size: Int
        get() {
            revision.value
            return cache.size()
        }

    override val entries: Set<Map.Entry<String, Bitmap>>
        get() {
            revision.value
            return cache.snapshot().entries
        }

    override fun get(key: String): Bitmap? {
        revision.value // subscribe the reading composition to put/eviction invalidation
        return cache.get(key)
    }

    fun put(id: String, bitmap: Bitmap) {
        cache.put(id, bitmap)
        revision.value += 1
    }
}
