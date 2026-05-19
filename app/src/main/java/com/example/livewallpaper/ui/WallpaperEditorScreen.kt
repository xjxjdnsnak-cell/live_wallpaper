package com.example.livewallpaper.ui

import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.example.livewallpaper.data.PRESET_ORIGINAL
import com.example.livewallpaper.data.TEXT_POSITION_BOTTOM
import com.example.livewallpaper.data.TEXT_POSITION_CENTER
import com.example.livewallpaper.data.TEXT_POSITION_TOP
import com.example.livewallpaper.data.WallpaperConfig
import com.example.livewallpaper.data.WallpaperItem
import com.example.livewallpaper.media.VideoFileInspector
import com.example.livewallpaper.media.VideoGenerationProgress
import com.example.livewallpaper.wallpaper.FillMode
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToLong

@Composable
fun WallpaperEditorScreen(
    item: WallpaperItem?,
    thumbnail: Bitmap?,
    onBack: () -> Unit,
    onSave: (WallpaperItem, WallpaperConfig) -> Unit,
    onGenerateCopy: suspend (WallpaperItem, WallpaperConfig, (VideoGenerationProgress) -> Unit) -> Result<WallpaperItem>,
    onOpenGenerated: (WallpaperItem) -> Unit,
    onSetGeneratedCurrent: (WallpaperItem) -> Unit,
) {
    var config by remember(item?.id) { mutableStateOf(item?.config ?: WallpaperConfig()) }
    val scope = rememberCoroutineScope()
    var isGenerating by remember { mutableStateOf(false) }
    var generationProgress by remember { mutableStateOf<VideoGenerationProgress?>(null) }
    var generationError by remember { mutableStateOf<String?>(null) }
    var generatedItem by remember { mutableStateOf<WallpaperItem?>(null) }
    val durationMs = (item?.durationMs ?: 60_000L).coerceAtLeast(1_000L)
    val startValue = (config.startMs ?: 0L).coerceIn(0L, durationMs - 1)
    val endValue = (config.endMs ?: durationMs).coerceIn(startValue + 1, durationMs)

    Scaffold(containerColor = LuminaBackground) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = onBack, shape = RoundedCornerShape(18.dp)) { Text("返回") }
                Text("编辑壁纸", color = LuminaText, fontSize = 22.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Button(
                    enabled = item != null,
                    onClick = {
                        if (item != null) {
                            onSave(
                                item,
                                config.copy(
                                    startMs = startValue.takeIf { it > 0L },
                                    endMs = endValue.takeIf { it < durationMs },
                                ),
                            )
                        }
                    },
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = LuminaPrimary),
                ) { Text("保存配置") }
            }

            if (item == null) {
                EditorCard {
                    Text("壁纸不存在", color = LuminaText, fontWeight = FontWeight.Bold)
                    Text("可能已经被删除。", color = LuminaMuted)
                }
            } else {
                EditorPreview(item, thumbnail, config)
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    PresetGroup(config) { config = it }
                    PlaybackGroup(config, startValue, endValue, durationMs) { config = it }
                    CompositionGroup(config) { config = it }
                    ColorGroup(config) { config = it }
                    AtmosphereGroup(config) { config = it }
                    TextGroup(config) { config = it }
                    OutlinedButton(
                        onClick = { config = WallpaperConfig() },
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        shape = RoundedCornerShape(22.dp),
                    ) { Text("重置全部") }
                    GenerationGroup(
                        item = item,
                        config = config.copy(
                            startMs = startValue.takeIf { it > 0L },
                            endMs = endValue.takeIf { it < durationMs },
                        ),
                        isGenerating = isGenerating,
                        progress = generationProgress,
                        error = generationError,
                        generatedItem = generatedItem,
                        onGenerate = {
                            scope.launch {
                                isGenerating = true
                                generationError = null
                                generatedItem = null
                                generationProgress = VideoGenerationProgress(0f, "准备中", VideoGenerationProgress.Stage.PREPARING)
                                val result = runCatching {
                                    onGenerateCopy(
                                        item,
                                        config.copy(
                                            startMs = startValue.takeIf { it > 0L },
                                            endMs = endValue.takeIf { it < durationMs },
                                        ),
                                    ) { progress -> scope.launch { generationProgress = progress } }
                                }.getOrElse { Result.failure(it) }
                                result
                                    .onSuccess {
                                        generatedItem = it
                                        generationProgress = VideoGenerationProgress(1f, "已完成", VideoGenerationProgress.Stage.COMPLETED)
                                    }
                                    .onFailure {
                                        generationError = it.message ?: "生成失败"
                                        generationProgress = VideoGenerationProgress(1f, "失败", VideoGenerationProgress.Stage.FAILED)
                                    }
                                isGenerating = false
                            }
                        },
                        onOpenGenerated = onOpenGenerated,
                        onSetGeneratedCurrent = onSetGeneratedCurrent,
                        onContinueEditing = {
                            generatedItem = null
                            generationProgress = null
                            generationError = null
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun PresetGroup(config: WallpaperConfig, onChange: (WallpaperConfig) -> Unit) {
    EditorCard {
        SectionHeader("预设") { onChange(WallpaperConfig()) }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            presets.forEach { preset ->
                ChoiceButton(
                    label = preset.label,
                    selected = config.presetName == preset.name,
                    onClick = { onChange(preset.apply(config)) },
                )
            }
        }
    }
}

@Composable
private fun PlaybackGroup(
    config: WallpaperConfig,
    startValue: Long,
    endValue: Long,
    durationMs: Long,
    onChange: (WallpaperConfig) -> Unit,
) {
    EditorCard {
        SectionHeader("播放") {
            onChange(config.copy(startMs = null, endMs = null, playbackSpeed = 1.0f, muted = true))
        }
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Text("静音", color = LuminaText, modifier = Modifier.weight(1f))
            Switch(checked = config.muted, onCheckedChange = { onChange(config.copy(muted = it, presetName = PRESET_ORIGINAL)) })
        }
        Text("播放速度", color = LuminaMuted, fontSize = 13.sp)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            listOf(0.5f, 1.0f, 1.5f).forEach { speed ->
                ChoiceButton("${speed}x", config.playbackSpeed == speed, { onChange(config.copy(playbackSpeed = speed, presetName = PRESET_ORIGINAL)) }, Modifier.weight(1f))
            }
        }
        Text("起始时间 ${VideoFileInspector.formatDuration(startValue)}", color = LuminaMuted, fontSize = 13.sp)
        Slider(
            value = startValue.toFloat(),
            onValueChange = { value ->
                val nextStart = value.roundToLong().coerceIn(0L, durationMs - 1)
                val nextEnd = maxOf(endValue, nextStart + 1)
                onChange(config.copy(startMs = nextStart.takeIf { it > 0L }, endMs = nextEnd.takeIf { it < durationMs }, presetName = PRESET_ORIGINAL))
            },
            valueRange = 0f..durationMs.toFloat(),
        )
        Text("结束时间 ${VideoFileInspector.formatDuration(endValue)}", color = LuminaMuted, fontSize = 13.sp)
        Slider(
            value = endValue.toFloat(),
            onValueChange = { value ->
                val nextEnd = value.roundToLong().coerceIn(startValue + 1, durationMs)
                onChange(config.copy(endMs = nextEnd.takeIf { it < durationMs }, presetName = PRESET_ORIGINAL))
            },
            valueRange = 0f..durationMs.toFloat(),
        )
    }
}

@Composable
private fun CompositionGroup(config: WallpaperConfig, onChange: (WallpaperConfig) -> Unit) {
    EditorCard {
        SectionHeader("构图") {
            onChange(
                config.copy(
                    fillMode = FillMode.CENTER_CROP,
                    scale = 1f,
                    offsetX = 0f,
                    offsetY = 0f,
                    rotation = 0f,
                    mirror = false,
                    presetName = PRESET_ORIGINAL,
                ),
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            FillMode.entries.forEach { mode ->
                ChoiceButton(mode.name, config.fillMode == mode, { onChange(config.copy(fillMode = mode, presetName = PRESET_ORIGINAL)) }, Modifier.weight(1f))
            }
        }
        LabeledSlider("缩放", config.scale, 0.8f..2.0f) { onChange(config.copy(scale = it, presetName = PRESET_ORIGINAL)) }
        LabeledSlider("水平偏移", config.offsetX, -1f..1f) { onChange(config.copy(offsetX = it, presetName = PRESET_ORIGINAL)) }
        LabeledSlider("垂直偏移", config.offsetY, -1f..1f) { onChange(config.copy(offsetY = it, presetName = PRESET_ORIGINAL)) }
        LabeledSlider("旋转", config.rotation, -15f..15f) { onChange(config.copy(rotation = it, presetName = PRESET_ORIGINAL)) }
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Text("镜像", color = LuminaText, modifier = Modifier.weight(1f))
            Switch(checked = config.mirror, onCheckedChange = { onChange(config.copy(mirror = it, presetName = PRESET_ORIGINAL)) })
        }
    }
}

@Composable
private fun ColorGroup(config: WallpaperConfig, onChange: (WallpaperConfig) -> Unit) {
    EditorCard {
        SectionHeader("调色") {
            onChange(
                config.copy(
                    brightness = 1f,
                    contrast = 1f,
                    saturation = 1f,
                    warmth = 0f,
                    exposure = 0f,
                    presetName = PRESET_ORIGINAL,
                ),
            )
        }
        LabeledSlider("亮度", config.brightness, 0.5f..1.5f) { onChange(config.copy(brightness = it, presetName = PRESET_ORIGINAL)) }
        LabeledSlider("对比度", config.contrast, 0.5f..1.5f) { onChange(config.copy(contrast = it, presetName = PRESET_ORIGINAL)) }
        LabeledSlider("饱和度", config.saturation, 0f..2f) { onChange(config.copy(saturation = it, presetName = PRESET_ORIGINAL)) }
        LabeledSlider("色温", config.warmth, -1f..1f) { onChange(config.copy(warmth = it, presetName = PRESET_ORIGINAL)) }
        LabeledSlider("曝光", config.exposure, -1f..1f) { onChange(config.copy(exposure = it, presetName = PRESET_ORIGINAL)) }
    }
}

@Composable
private fun AtmosphereGroup(config: WallpaperConfig, onChange: (WallpaperConfig) -> Unit) {
    EditorCard {
        SectionHeader("氛围") {
            onChange(config.copy(blur = 0f, vignette = 0f, topGradient = 0f, bottomGradient = 0f, presetName = PRESET_ORIGINAL))
        }
        LabeledSlider("模糊", config.blur, 0f..1f) { onChange(config.copy(blur = it, presetName = PRESET_ORIGINAL)) }
        LabeledSlider("暗角", config.vignette, 0f..1f) { onChange(config.copy(vignette = it, presetName = PRESET_ORIGINAL)) }
        LabeledSlider("顶部渐变", config.topGradient, 0f..1f) { onChange(config.copy(topGradient = it, presetName = PRESET_ORIGINAL)) }
        LabeledSlider("底部渐变", config.bottomGradient, 0f..1f) { onChange(config.copy(bottomGradient = it, presetName = PRESET_ORIGINAL)) }
    }
}

@Composable
private fun TextGroup(config: WallpaperConfig, onChange: (WallpaperConfig) -> Unit) {
    EditorCard {
        SectionHeader("文字") {
            onChange(
                config.copy(
                    textEnabled = false,
                    overlayText = "",
                    textPosition = TEXT_POSITION_BOTTOM,
                    textOpacity = 0.8f,
                    presetName = PRESET_ORIGINAL,
                ),
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Text("启用文字", color = LuminaText, modifier = Modifier.weight(1f))
            Switch(checked = config.textEnabled, onCheckedChange = { onChange(config.copy(textEnabled = it, presetName = PRESET_ORIGINAL)) })
        }
        OutlinedTextField(
            value = config.overlayText,
            onValueChange = { onChange(config.copy(overlayText = it, presetName = PRESET_ORIGINAL)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            label = { Text("文字内容") },
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            listOf(TEXT_POSITION_TOP to "顶部", TEXT_POSITION_CENTER to "居中", TEXT_POSITION_BOTTOM to "底部").forEach { (value, label) ->
                ChoiceButton(label, config.textPosition == value, { onChange(config.copy(textPosition = value, presetName = PRESET_ORIGINAL)) }, Modifier.weight(1f))
            }
        }
        LabeledSlider("文字透明度", config.textOpacity, 0f..1f) { onChange(config.copy(textOpacity = it, presetName = PRESET_ORIGINAL)) }
    }
}

@Composable
private fun GenerationGroup(
    item: WallpaperItem,
    config: WallpaperConfig,
    isGenerating: Boolean,
    progress: VideoGenerationProgress?,
    error: String?,
    generatedItem: WallpaperItem?,
    onGenerate: () -> Unit,
    onOpenGenerated: (WallpaperItem) -> Unit,
    onSetGeneratedCurrent: (WallpaperItem) -> Unit,
    onContinueEditing: () -> Unit,
) {
    EditorCard {
        Text("生成壁纸副本", color = LuminaText, fontSize = 17.sp, fontWeight = FontWeight.Bold)
        Text(
            "按当前起止时间生成新的本地 MP4，并自动加入壁纸库。调色、遮罩和文字会随配置保存，第一版不烘焙进视频。",
            color = LuminaMuted,
            fontSize = 13.sp,
        )
        Button(
            enabled = !isGenerating,
            onClick = onGenerate,
            modifier = Modifier.fillMaxWidth().height(50.dp),
            shape = RoundedCornerShape(22.dp),
            colors = ButtonDefaults.buttonColors(containerColor = LuminaPrimary),
        ) {
            Text(if (isGenerating) "正在生成" else "生成壁纸副本")
        }
        progress?.let { current ->
            LinearProgressIndicator(
                progress = { current.fraction },
                modifier = Modifier.fillMaxWidth(),
                color = LuminaPrimary,
                trackColor = LuminaSecondary,
            )
            Text(current.message, color = LuminaMuted, fontSize = 13.sp)
        }
        if (error != null) {
            Text(error, color = Color(0xFFB54A5A), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        }
        if (generatedItem != null) {
            Text("${item.displayName ?: "当前壁纸"} 已生成适配副本", color = LuminaText, fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                DetailMiniButton("查看详情", { onOpenGenerated(generatedItem) }, Modifier.weight(1f))
                DetailMiniButton("设为当前", { onSetGeneratedCurrent(generatedItem) }, Modifier.weight(1f))
                DetailMiniButton("继续编辑", onContinueEditing, Modifier.weight(1f))
            }
        }
        Text(
            "当前片段：${VideoFileInspector.formatDuration(config.startMs ?: 0L)} - ${VideoFileInspector.formatDuration(config.endMs)}",
            color = LuminaMuted,
            fontSize = 12.sp,
        )
    }
}

@Composable
private fun EditorPreview(item: WallpaperItem, thumbnail: Bitmap?, config: WallpaperConfig) {
    val context = LocalContext.current
    val player = remember(item.id) {
        ExoPlayer.Builder(context).build().apply {
            repeatMode = Player.REPEAT_MODE_ALL
        }
    }

    DisposableEffect(player) {
        onDispose {
            player.pause()
            player.clearMediaItems()
            player.release()
        }
    }

    LaunchedEffect(item.uri, config.startMs) {
        player.pause()
        player.clearMediaItems()
        player.setMediaItem(MediaItem.fromUri(Uri.parse(item.uri)))
        player.prepare()
        config.startMs?.takeIf { it > 0L }?.let { player.seekTo(it) }
        player.playWhenReady = true
    }

    LaunchedEffect(config.muted, config.playbackSpeed) {
        player.volume = if (config.muted) 0f else 1f
        player.playbackParameters = PlaybackParameters(config.playbackSpeed.coerceAtLeast(0.1f))
    }

    LaunchedEffect(config.startMs, config.endMs) {
        val start = config.startMs ?: 0L
        val end = config.endMs
        if (end != null && end > start) {
            while (true) {
                if (player.currentPosition >= end) player.seekTo(start)
                delay(300)
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(330.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(Color(0xFF1D1A2B)),
    ) {
        TransformedVideoLayer(config) {
            AndroidView(
                factory = { viewContext ->
                    PlayerView(viewContext).apply {
                        useController = false
                        this.player = player
                    }
                },
                update = { view ->
                    view.player = player
                    view.resizeMode = config.fillMode.toResizeMode()
                    view.applyPreviewBlur(config.blur)
                },
                modifier = Modifier.fillMaxSize(),
            )
        }
        if (thumbnail != null) {
            Image(
                bitmap = thumbnail.asImageBitmap(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                alpha = 0.12f,
                modifier = Modifier.fillMaxSize(),
            )
        }
        VisualEffectOverlay(config)
        Text(
            text = "${presetLabel(config.presetName)} · ${config.playbackSpeed}x",
            color = Color.White,
            modifier = Modifier.align(Alignment.BottomStart).padding(18.dp),
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun EditorCard(content: @Composable ColumnScope.() -> Unit) {
    Surface(
        color = Color.White,
        shape = RoundedCornerShape(24.dp),
        shadowElevation = 2.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            content = content,
        )
    }
}

@Composable
private fun SectionHeader(text: String, onReset: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Text(text, color = LuminaText, fontSize = 17.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
        OutlinedButton(onClick = onReset, shape = RoundedCornerShape(18.dp)) { Text("重置") }
    }
}

@Composable
private fun ChoiceButton(label: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Button(
        onClick = onClick,
        modifier = modifier.height(42.dp),
        shape = RoundedCornerShape(18.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (selected) LuminaPrimary else LuminaSecondary,
            contentColor = if (selected) Color.White else LuminaPrimaryDark,
        ),
    ) { Text(label, fontSize = 12.sp) }
}

@Composable
private fun DetailMiniButton(label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.height(42.dp),
        shape = RoundedCornerShape(18.dp),
    ) {
        Text(label, fontSize = 12.sp)
    }
}

@Composable
private fun LabeledSlider(label: String, value: Float, range: ClosedFloatingPointRange<Float>, onChange: (Float) -> Unit) {
    Text("$label ${"%.2f".format(value)}", color = LuminaMuted, fontSize = 13.sp)
    Slider(value = value.coerceIn(range.start, range.endInclusive), onValueChange = onChange, valueRange = range)
}

private data class EditorPreset(
    val name: String,
    val label: String,
    val apply: (WallpaperConfig) -> WallpaperConfig,
)

private val presets = listOf(
    EditorPreset("original", "原图") { WallpaperConfig() },
    EditorPreset("cool", "清冷") { it.copy(saturation = 0.9f, warmth = -0.4f, contrast = 1.05f, presetName = "cool") },
    EditorPreset("sunny", "暖阳") { it.copy(warmth = 0.5f, brightness = 1.08f, saturation = 1.1f, presetName = "sunny") },
    EditorPreset("cinema", "电影") { it.copy(contrast = 1.2f, brightness = 0.95f, vignette = 0.35f, presetName = "cinema") },
    EditorPreset("night", "暗夜") { it.copy(brightness = 0.75f, contrast = 1.15f, vignette = 0.5f, presetName = "night") },
    EditorPreset("soft", "柔和") { it.copy(contrast = 0.9f, saturation = 0.85f, brightness = 1.05f, presetName = "soft") },
    EditorPreset("cyber", "赛博") { it.copy(saturation = 1.5f, contrast = 1.25f, warmth = -0.2f, presetName = "cyber") },
)

private fun presetLabel(name: String): String = presets.firstOrNull { it.name == name }?.label ?: "原图"
