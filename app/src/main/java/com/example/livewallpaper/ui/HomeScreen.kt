package com.example.livewallpaper.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.livewallpaper.data.WallpaperSettings

@Composable
fun HomeScreen(
    settings: WallpaperSettings,
    onPickVideo: () -> Unit,
    onPreview: () -> Unit,
    onOpenSettings: () -> Unit,
    onSetWallpaper: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("当前视频: ${settings.videoUri ?: "未选择"}")
        Button(onClick = onPickVideo) { Text("选择视频") }
        Button(onClick = onPreview) { Text("预览壁纸") }
        Button(onClick = onSetWallpaper) { Text("设置为动态壁纸") }
        Button(onClick = onOpenSettings) { Text("设置") }
    }
}
