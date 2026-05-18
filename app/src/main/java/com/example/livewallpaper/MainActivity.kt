package com.example.livewallpaper

import android.app.WallpaperManager
import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.livewallpaper.data.SettingsRepository
import com.example.livewallpaper.data.WallpaperSettings
import com.example.livewallpaper.media.VideoUriPermissionHelper
import com.example.livewallpaper.ui.HomeScreen
import com.example.livewallpaper.ui.PreviewScreen
import com.example.livewallpaper.ui.SettingsScreen
import com.example.livewallpaper.wallpaper.InteractiveVideoWallpaperService
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val repository by lazy { SettingsRepository.fromContext(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MaterialTheme { App(repository, ::openWallpaperPicker) } }
    }

    private fun openWallpaperPicker() {
        val componentName = ComponentName(this, InteractiveVideoWallpaperService::class.java)
        try {
            startActivity(Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER).putExtra(WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT, componentName))
        } catch (t: Throwable) {
            Log.w("MainActivity", "Direct wallpaper intent failed", t)
            try { startActivity(Intent(WallpaperManager.ACTION_LIVE_WALLPAPER_CHOOSER)) }
            catch (inner: Throwable) { Log.e("MainActivity", "Wallpaper chooser unavailable", inner); startActivity(Intent(Settings.ACTION_SETTINGS)) }
        }
    }
}

@Composable
private fun App(repository: SettingsRepository, onSetWallpaper: () -> Unit) {
    val context = LocalContext.current
    val navController = rememberNavController()
    val settings by repository.settings.collectAsState(initial = WallpaperSettings())
    val scope = rememberCoroutineScope()
    val pickVideo = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            VideoUriPermissionHelper.takePersistableUriPermission(context.contentResolver, uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            scope.launch { repository.updateVideoUri(uri.toString()) }
        }
    }

    Scaffold { padding ->
        NavHost(navController, "home", Modifier.padding(padding)) {
            composable("home") {
                HomeScreen(settings, { pickVideo.launch(arrayOf("video/*")) }, { navController.navigate("preview") }, { navController.navigate("settings") }, onSetWallpaper)
            }
            composable("preview") { PreviewScreen(settings, onBack = { navController.popBackStack() }) }
            composable("settings") {
                SettingsScreen(
                    settings,
                    onBack = { navController.popBackStack() },
                    onMutedChanged = { scope.launch { repository.updateMuted(it) } },
                    onFillModeChanged = { scope.launch { repository.updateFillMode(it) } },
                    onSpeedChanged = { scope.launch { repository.updatePlaybackSpeed(it) } },
                    onTouchChanged = { scope.launch { repository.updateTouchEffectEnabled(it) } },
                    onParallaxChanged = { scope.launch { repository.updateParallaxEnabled(it) } },
                )
            }
        }
    }
}
