package com.example.livewallpaper.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.livewallpaper.data.WallpaperSettings
import com.example.livewallpaper.wallpaper.FillMode

@Composable
fun SettingsScreen(
    settings: WallpaperSettings,
    onBack: () -> Unit,
    onMutedChanged: (Boolean) -> Unit,
    onFillModeChanged: (FillMode) -> Unit,
    onSpeedChanged: (Float) -> Unit,
    onTouchChanged: (Boolean) -> Unit,
    onParallaxChanged: (Boolean) -> Unit,
) {
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("静音")
        Switch(checked = settings.muted, onCheckedChange = onMutedChanged)
        Text("填充模式")
        FillMode.entries.forEach { mode ->
            androidx.compose.foundation.layout.Row {
                RadioButton(selected = settings.fillMode == mode, onClick = { onFillModeChanged(mode) })
                Text(mode.name)
            }
        }
        Text("速度")
        listOf(0.5f, 1.0f, 1.5f).forEach {
            Button(onClick = { onSpeedChanged(it) }) { Text("${it}x") }
        }
        Text("触摸特效")
        Switch(checked = settings.touchEffectEnabled, onCheckedChange = onTouchChanged)
        Text("桌面视差")
        Switch(checked = settings.parallaxEnabled, onCheckedChange = onParallaxChanged)
        Button(onClick = onBack) { Text("返回") }
    }
}
