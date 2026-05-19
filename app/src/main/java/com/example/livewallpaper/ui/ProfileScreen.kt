package com.example.livewallpaper.ui

import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.livewallpaper.data.WallpaperItem
import com.example.livewallpaper.data.WallpaperSettings
import com.example.livewallpaper.media.VideoFileInspector
import com.example.livewallpaper.ui.components.InfoCard
import com.example.livewallpaper.ui.components.LocalWallpaperCard
import com.example.livewallpaper.ui.components.SectionHeader
import com.example.livewallpaper.ui.components.WallpaperPreview
import com.example.livewallpaper.ui.components.localGradientFor

@Composable
fun ProfileScreen(
    settings: WallpaperSettings,
    items: List<WallpaperItem>,
    currentItem: WallpaperItem?,
    thumbnails: Map<String, Bitmap>,
    onOpenSettings: () -> Unit,
    onClearVideo: () -> Unit,
    onPreview: () -> Unit,
    onSetWallpaper: () -> Unit,
    onOpenDetail: (String) -> Unit,
) {
    val favorites = items.count { it.isFavorite }
    val recent = items.filter { it.lastViewedAt != null }.sortedByDescending { it.lastViewedAt }.take(5)
    val mostUsed = items.filter { it.useCount > 0 }.sortedByDescending { it.useCount }.take(5)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(LuminaBackground)
            .verticalScroll(rememberScrollState())
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        ProfileHeader(onOpenSettings)
        StatRow(favorites, items.size, recent.size)
        FunctionGrid(favorites, items.size, recent.size)
        CurrentVideoProfileCard(settings, currentItem, thumbnails[currentItem?.id], onClearVideo, onPreview, onSetWallpaper, onOpenDetail)
        HistorySection("最近浏览", recent, thumbnails, onOpenDetail)
        HistorySection("最常使用", mostUsed, thumbnails, onOpenDetail)
        Collections()
        Spacer(Modifier.height(90.dp))
    }
}

@Composable
private fun ProfileHeader(onOpenSettings: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .size(74.dp)
                .clip(CircleShape)
                .background(LuminaSecondary),
            contentAlignment = Alignment.Center,
        ) {
            Text("L", color = LuminaPrimary, fontSize = 30.sp, fontWeight = FontWeight.Bold)
        }
        Column(Modifier.padding(start = 14.dp).weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text("Lumina User", color = LuminaText, fontSize = 21.sp, fontWeight = FontWeight.Bold)
            Text("点亮灵感，收藏美好。", color = LuminaMuted, fontSize = 13.sp)
        }
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(Color.White)
                .clickable(onClick = onOpenSettings),
            contentAlignment = Alignment.Center,
        ) {
            Text("设", color = LuminaPrimary, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun StatRow(favorites: Int, uploads: Int, history: Int) {
    InfoCard {
        Row(horizontalArrangement = Arrangement.SpaceAround, modifier = Modifier.fillMaxWidth()) {
            ProfileStat("$favorites", "收藏")
            ProfileStat("$uploads", "上传")
            ProfileStat("$history", "浏览")
        }
    }
}

@Composable
private fun ProfileStat(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, color = LuminaText, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Text(label, color = LuminaMuted, fontSize = 12.sp)
    }
}

@Composable
private fun FunctionGrid(favorites: Int, uploads: Int, history: Int) {
    InfoCard {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ProfileFunction("我的收藏 $favorites", Modifier.weight(1f))
                ProfileFunction("我上传的 $uploads", Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ProfileFunction("我喜欢的 $favorites", Modifier.weight(1f))
                ProfileFunction("浏览历史 $history", Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun ProfileFunction(text: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .height(58.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(LuminaSecondary),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, color = LuminaPrimary, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun CurrentVideoProfileCard(
    settings: WallpaperSettings,
    currentItem: WallpaperItem?,
    thumbnail: Bitmap?,
    onClearVideo: () -> Unit,
    onPreview: () -> Unit,
    onSetWallpaper: () -> Unit,
    onOpenDetail: (String) -> Unit,
) {
    InfoCard {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SectionHeader("当前壁纸")
            WallpaperPreview(
                colors = localGradientFor(currentItem?.category ?: WallpaperItem.DEFAULT_CATEGORY),
                thumbnail = thumbnail,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(118.dp)
                    .then(if (currentItem != null) Modifier.clickable { onOpenDetail(currentItem.id) } else Modifier),
                cornerRadius = 20,
            )
            Text("URI: ${settings.videoUri ?: "未选择"}", color = LuminaMuted, fontSize = 12.sp)
            Text("文件名: ${currentItem?.displayName ?: VideoFileInspector.UNKNOWN}", color = LuminaText, fontSize = 14.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = onPreview,
                    enabled = !settings.videoUri.isNullOrBlank(),
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = LuminaPrimary),
                    modifier = Modifier.weight(1f),
                ) { Text("预览") }
                Button(
                    onClick = onSetWallpaper,
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = LuminaSecondary, contentColor = LuminaPrimary),
                    modifier = Modifier.weight(1f),
                ) { Text("设置壁纸") }
                Button(
                    onClick = onClearVideo,
                    enabled = !settings.videoUri.isNullOrBlank(),
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = LuminaPrimary),
                    modifier = Modifier.weight(1f),
                ) { Text("清除") }
            }
        }
    }
}

@Composable
private fun HistorySection(title: String, items: List<WallpaperItem>, thumbnails: Map<String, Bitmap>, onOpenDetail: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionHeader(title, if (items.isEmpty()) null else "${items.size} 张")
        if (items.isEmpty()) {
            InfoCard { Text("暂无$title", color = LuminaMuted) }
        } else {
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items.forEach { item ->
                    LocalWallpaperCard(item = item, thumbnail = thumbnails[item.id], onClick = { onOpenDetail(item.id) })
                }
            }
        }
    }
}

@Composable
private fun Collections() {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionHeader("我的合集", "本地")
        listOf("星空收藏夹", "治愈风景", "游戏壁纸").forEach { name ->
            InfoCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(LuminaSecondary),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("L", color = LuminaPrimary, fontWeight = FontWeight.Bold)
                    }
                    Column(Modifier.padding(start = 12.dp).weight(1f)) {
                        Text(name, color = LuminaText, fontWeight = FontWeight.SemiBold)
                        Text("本地合集占位", color = LuminaMuted, fontSize = 12.sp)
                    }
                    Text("›", color = LuminaMuted, fontSize = 22.sp)
                }
            }
        }
    }
}
