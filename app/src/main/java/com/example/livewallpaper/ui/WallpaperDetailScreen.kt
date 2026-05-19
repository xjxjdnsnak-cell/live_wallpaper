package com.example.livewallpaper.ui

import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.livewallpaper.data.WallpaperItem
import com.example.livewallpaper.media.VideoFileInspector
import com.example.livewallpaper.ui.components.ActionTile
import com.example.livewallpaper.ui.components.WallpaperPreview
import com.example.livewallpaper.ui.components.localGradientFor
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.util.Date

@Composable
fun WallpaperDetailScreen(
    item: WallpaperItem?,
    sourceItem: WallpaperItem? = null,
    thumbnail: Bitmap?,
    isCurrent: Boolean,
    onBack: () -> Unit,
    onPreview: () -> Unit,
    onEdit: (WallpaperItem) -> Unit,
    onSetWallpaper: () -> Unit,
    onSetCurrent: (WallpaperItem) -> Unit,
    onToggleFavorite: (WallpaperItem) -> Unit,
    onDelete: (WallpaperItem) -> Unit,
    onCategoryChanged: (WallpaperItem, String) -> Unit,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(
        containerColor = LuminaBackground,
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(LuminaBackground),
        ) {
            Box {
                WallpaperPreview(
                    colors = localGradientFor(item?.category ?: WallpaperItem.DEFAULT_CATEGORY),
                    thumbnail = thumbnail,
                    modifier = Modifier.fillMaxWidth().height(430.dp),
                    cornerRadius = 0,
                )
                CircleButton("‹", onBack, Modifier.align(Alignment.TopStart).padding(18.dp))
                CircleButton("…", { scope.launch { snackbarHostState.showSnackbar("更多操作已在下方提供") } }, Modifier.align(Alignment.TopEnd).padding(18.dp))
            }

            Surface(
                color = Color.White,
                shape = RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp),
                modifier = Modifier.fillMaxWidth().weight(1f),
            ) {
                if (item == null) {
                    EmptyDetail(onBack)
                } else {
                    DetailContent(
                        item = item,
                        sourceItem = sourceItem,
                        isCurrent = isCurrent,
                        snackbarHostState = snackbarHostState,
                        onPreview = onPreview,
                        onEdit = onEdit,
                        onSetWallpaper = onSetWallpaper,
                        onSetCurrent = onSetCurrent,
                        onToggleFavorite = onToggleFavorite,
                        onDelete = onDelete,
                        onCategoryChanged = onCategoryChanged,
                    )
                }
            }
        }
    }
}

@Composable
private fun DetailContent(
    item: WallpaperItem,
    sourceItem: WallpaperItem?,
    isCurrent: Boolean,
    snackbarHostState: SnackbarHostState,
    onPreview: () -> Unit,
    onEdit: (WallpaperItem) -> Unit,
    onSetWallpaper: () -> Unit,
    onSetCurrent: (WallpaperItem) -> Unit,
    onToggleFavorite: (WallpaperItem) -> Unit,
    onDelete: (WallpaperItem) -> Unit,
    onCategoryChanged: (WallpaperItem, String) -> Unit,
) {
    var categoryExpanded by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .padding(20.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(item.displayName ?: "本地动态壁纸", color = LuminaText, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                Text(
                    "${item.category} · ${VideoFileInspector.formatDuration(item.durationMs)} · ${VideoFileInspector.formatSize(item.sizeBytes)}",
                    color = LuminaMuted,
                    fontSize = 13.sp,
                )
            }
            Button(
                onClick = { onSetCurrent(item) },
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(containerColor = if (isCurrent) LuminaSecondary else LuminaPrimary, contentColor = if (isCurrent) LuminaPrimary else Color.White),
            ) {
                Text(if (isCurrent) "当前" else "设为当前")
            }
        }

        if (item.isGenerated) {
            GeneratedInfo(item = item, sourceItem = sourceItem)
        }

        Row(horizontalArrangement = Arrangement.SpaceAround, modifier = Modifier.fillMaxWidth()) {
            ActionTile(if (item.isFavorite) "取消" else "收藏", if (item.isFavorite) "♥" else "☆", { onToggleFavorite(item) }, Modifier.weight(1f))
            ActionTile("分类", "C", { categoryExpanded = true }, Modifier.weight(1f))
            ActionTile("下载", "↓", { scope.launch { snackbarHostState.showSnackbar("本地版本暂未接入下载") } }, Modifier.weight(1f))
            ActionTile("删除", "×", { onDelete(item) }, Modifier.weight(1f))
        }
        ConfigSummary(item)
        Box {
            DropdownMenu(expanded = categoryExpanded, onDismissRequest = { categoryExpanded = false }) {
                WallpaperItem.CATEGORIES.forEach { category ->
                    DropdownMenuItem(
                        text = { Text(category) },
                        onClick = {
                            categoryExpanded = false
                            onCategoryChanged(item, category)
                        },
                    )
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            DetailButton("预览", onPreview, Modifier.weight(1f))
            DetailButton("编辑壁纸", { onEdit(item) }, Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            DetailButton("设置为动态壁纸", onSetWallpaper, Modifier.weight(1f))
            DetailButton("设为当前壁纸", { onSetCurrent(item) }, Modifier.weight(1f))
        }
    }
}

@Composable
private fun ConfigSummary(item: WallpaperItem) {
    val config = item.config
    val clipText = when {
        config.startMs != null || config.endMs != null ->
            "${VideoFileInspector.formatDuration(config.startMs ?: 0L)} - ${VideoFileInspector.formatDuration(config.endMs)}"
        else -> "完整视频"
    }
    val colorEnabled = config.brightness != 1.0f ||
        config.contrast != 1.0f ||
        config.saturation != 1.0f ||
        config.warmth != 0f ||
        config.exposure != 0f
    val maskEnabled = config.vignette > 0f || config.topGradient > 0f || config.bottomGradient > 0f || config.blur > 0f
    val textEnabled = config.textEnabled && config.overlayText.isNotBlank()
    Surface(
        color = LuminaSecondary,
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("编辑配置", color = LuminaText, fontWeight = FontWeight.Bold)
            Text("${config.presetName} · ${config.playbackSpeed}x · ${config.fillMode} · ${if (config.muted) "静音" else "有声"}", color = LuminaMuted, fontSize = 13.sp)
            Text("片段：$clipText", color = LuminaMuted, fontSize = 13.sp)
            Text("调色：${if (colorEnabled) "已启用" else "默认"} · 遮罩：${if (maskEnabled) "已启用" else "默认"} · 文字：${if (textEnabled) "已启用" else "无"}", color = LuminaMuted, fontSize = 13.sp)
        }
    }
}

@Composable
private fun GeneratedInfo(item: WallpaperItem, sourceItem: WallpaperItem?) {
    Surface(
        color = LuminaSecondary,
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("已适配", color = LuminaText, fontWeight = FontWeight.Bold)
            Text("来源：${sourceItem?.displayName ?: item.originalUri ?: "未知"}", color = LuminaMuted, fontSize = 13.sp)
            Text("生成时间：${formatGeneratedTime(item.generatedAt)}", color = LuminaMuted, fontSize = 13.sp)
            Text("生成方式：按编辑配置生成本地 MP4 副本", color = LuminaMuted, fontSize = 13.sp)
        }
    }
}

private fun formatGeneratedTime(value: Long?): String {
    if (value == null || value <= 0L) return "未知"
    return DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date(value))
}

@Composable
private fun EmptyDetail(onBack: () -> Unit) {
    Column(
        modifier = Modifier.padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("壁纸不存在", color = LuminaText, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Text("可能已经被删除。", color = LuminaMuted)
        DetailButton("返回", onBack, Modifier.fillMaxWidth())
    }
}

@Composable
private fun CircleButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(42.dp)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.84f))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, color = LuminaText, fontSize = 22.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun DetailButton(label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Button(
        onClick = onClick,
        modifier = modifier.height(46.dp),
        shape = RoundedCornerShape(22.dp),
        colors = ButtonDefaults.buttonColors(containerColor = LuminaPrimary, contentColor = Color.White),
    ) {
        Text(label)
    }
}
