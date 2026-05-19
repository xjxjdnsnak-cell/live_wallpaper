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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.example.livewallpaper.ui.components.ActionTile
import com.example.livewallpaper.ui.components.InfoCard
import com.example.livewallpaper.ui.components.LocalWallpaperCard
import com.example.livewallpaper.ui.components.LuminaChip
import com.example.livewallpaper.ui.components.SectionHeader
import com.example.livewallpaper.ui.components.WallpaperPreview
import com.example.livewallpaper.ui.components.localGradientFor

@Composable
fun HomeScreen(
    settings: WallpaperSettings,
    items: List<WallpaperItem>,
    currentItem: WallpaperItem?,
    thumbnails: Map<String, Bitmap>,
    onPickVideo: () -> Unit,
    onClearVideo: () -> Unit,
    onPreview: () -> Unit,
    onOpenSettings: () -> Unit,
    onSetWallpaper: () -> Unit,
    onOpenCategory: () -> Unit,
    onOpenDetail: (String) -> Unit,
) {
    var query by remember { mutableStateOf("") }
    val searchResults = remember(query, items) {
        val keyword = query.trim()
        if (keyword.isBlank()) emptyList() else items.filter {
            it.displayName.orEmpty().contains(keyword, ignoreCase = true)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(LuminaBackground)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        HomeHeader(onOpenSettings)
        SearchBox(query, onQueryChanged = { query = it })
        if (query.isNotBlank()) {
            SearchResults(searchResults, thumbnails, onOpenDetail)
        } else {
            FeaturedBanner(currentItem, thumbnails[currentItem?.id], onOpenDetail)
            QuickActions(onOpenCategory, onPreview, onSetWallpaper)
            CurrentVideoPanel(settings, currentItem, onPickVideo, onClearVideo, onPreview, onSetWallpaper)
            DailyRecommendations(items, thumbnails, onOpenDetail, onPickVideo)
            HotTags(onOpenCategory)
        }
        Spacer(Modifier.height(80.dp))
    }
}

@Composable
private fun HomeHeader(onOpenSettings: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Row(horizontalArrangement = Arrangement.spacedBy(24.dp), verticalAlignment = Alignment.Bottom) {
            Text("推荐", color = LuminaText, fontSize = 26.sp, fontWeight = FontWeight.Bold)
            Text("关注", color = LuminaMuted, fontSize = 17.sp, modifier = Modifier.padding(bottom = 3.dp))
        }
        Spacer(Modifier.weight(1f))
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
private fun SearchBox(query: String, onQueryChanged: (String) -> Unit) {
    TextField(
        value = query,
        onValueChange = onQueryChanged,
        placeholder = { Text("搜索壁纸、合集、用户", color = LuminaMuted) },
        leadingIcon = { Text("⌕", color = LuminaMuted, fontSize = 18.sp) },
        singleLine = true,
        shape = RoundedCornerShape(18.dp),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = Color.White,
            unfocusedContainerColor = Color.White,
            disabledContainerColor = Color.White,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
        ),
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun FeaturedBanner(currentItem: WallpaperItem?, thumbnail: Bitmap?, onOpenDetail: (String) -> Unit) {
    if (currentItem == null) {
        EmptyLibraryBanner()
        return
    }
    WallpaperPreview(
        colors = localGradientFor(currentItem.category),
        thumbnail = thumbnail,
        modifier = Modifier
            .fillMaxWidth()
            .height(188.dp)
            .clickable { onOpenDetail(currentItem.id) },
        cornerRadius = 28,
        overlay = {
            Column(
                modifier = Modifier.align(Alignment.BottomStart).padding(22.dp),
                verticalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                Text(currentItem.displayName ?: "我的动态壁纸", color = Color.White, fontSize = 25.sp, fontWeight = FontWeight.Bold)
                Text("当前本地动态壁纸", color = Color.White.copy(alpha = 0.86f), fontSize = 14.sp)
            }
        },
    )
}

@Composable
private fun EmptyLibraryBanner() {
    WallpaperPreview(
        colors = listOf(Color(0xFFF7F2FF), Color(0xFFD9CCF2), Color(0xFF7E6AAE)),
        modifier = Modifier.fillMaxWidth().height(188.dp),
        cornerRadius = 28,
        overlay = {
            Column(
                modifier = Modifier.align(Alignment.BottomStart).padding(22.dp),
                verticalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                Text("还没有动态壁纸", color = Color.White, fontSize = 25.sp, fontWeight = FontWeight.Bold)
                Text("点击下方 + 添加你的第一张动态壁纸", color = Color.White.copy(alpha = 0.86f), fontSize = 14.sp)
            }
        },
    )
}

@Composable
private fun QuickActions(onOpenCategory: () -> Unit, onPreview: () -> Unit, onSetWallpaper: () -> Unit) {
    InfoCard {
        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            ActionTile("排行", "R", onClick = onOpenCategory, modifier = Modifier.weight(1f))
            ActionTile("分类", "C", onClick = onOpenCategory, modifier = Modifier.weight(1f))
            ActionTile("动态壁纸", "P", onClick = onPreview, modifier = Modifier.weight(1f))
            ActionTile("专属合辑", "M", onClick = onSetWallpaper, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun CurrentVideoPanel(
    settings: WallpaperSettings,
    currentItem: WallpaperItem?,
    onPickVideo: () -> Unit,
    onClearVideo: () -> Unit,
    onPreview: () -> Unit,
    onSetWallpaper: () -> Unit,
) {
    InfoCard {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            SectionHeader("当前视频", if (settings.videoUri.isNullOrBlank()) null else "清除", onClearVideo)
            Text("URI: ${settings.videoUri ?: "未选择"}", color = LuminaMuted, fontSize = 12.sp)
            Text("文件名: ${currentItem?.displayName ?: VideoFileInspector.UNKNOWN}", color = LuminaText, fontSize = 14.sp)
            Text(
                "时长 ${VideoFileInspector.formatDuration(currentItem?.durationMs)} · 大小 ${VideoFileInspector.formatSize(currentItem?.sizeBytes)}",
                color = LuminaMuted,
                fontSize = 13.sp,
            )
            settings.lastPlaybackError?.let {
                Text(it, color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                PrimaryPill("添加视频", onPickVideo, Modifier.weight(1f))
                SecondaryPill("预览", onPreview, Modifier.weight(1f))
                SecondaryPill("设为壁纸", onSetWallpaper, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun DailyRecommendations(items: List<WallpaperItem>, thumbnails: Map<String, Bitmap>, onOpenDetail: (String) -> Unit, onPickVideo: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionHeader("每日推荐", if (items.isEmpty()) null else "本地库")
        if (items.isEmpty()) {
            InfoCard {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("还没有动态壁纸", color = LuminaText, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Text("点击下方 + 添加你的第一张动态壁纸", color = LuminaMuted, fontSize = 13.sp)
                    PrimaryPill("添加视频", onPickVideo)
                }
            }
        } else {
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items.take(12).forEach { item ->
                    LocalWallpaperCard(item = item, thumbnail = thumbnails[item.id], onClick = { onOpenDetail(item.id) })
                }
            }
        }
    }
}

@Composable
private fun SearchResults(items: List<WallpaperItem>, thumbnails: Map<String, Bitmap>, onOpenDetail: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionHeader("搜索结果")
        if (items.isEmpty()) {
            InfoCard {
                Text("没有找到匹配的本地壁纸", color = LuminaMuted)
            }
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
private fun HotTags(onOpenCategory: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionHeader("热门标签")
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            listOf("# 风景", "# 插画", "# 动漫", "# 科幻", "# 游戏").forEach {
                LuminaChip(it, onClick = onOpenCategory)
            }
        }
    }
}

@Composable
private fun PrimaryPill(label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Button(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        colors = ButtonDefaults.buttonColors(containerColor = LuminaPrimary),
        modifier = modifier.height(44.dp),
    ) {
        Text(label, color = Color.White, fontSize = 13.sp)
    }
}

@Composable
private fun SecondaryPill(label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Button(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        colors = ButtonDefaults.buttonColors(containerColor = LuminaSecondary, contentColor = LuminaPrimary),
        modifier = modifier.height(44.dp),
    ) {
        Text(label, fontSize = 13.sp)
    }
}
