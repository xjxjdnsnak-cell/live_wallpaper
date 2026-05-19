package com.example.livewallpaper.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.livewallpaper.ui.LuminaMuted
import com.example.livewallpaper.ui.LuminaPrimary
import com.example.livewallpaper.ui.LuminaSecondary
import com.example.livewallpaper.ui.LuminaText
import com.example.livewallpaper.data.WallpaperItem
import com.example.livewallpaper.media.VideoFileInspector

@Composable
fun WallpaperPreview(
    colors: List<Color>,
    modifier: Modifier = Modifier,
    thumbnail: Bitmap? = null,
    cornerRadius: Int = 24,
    overlay: @Composable (BoxScope.() -> Unit)? = null,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadius.dp))
            .background(Brush.linearGradient(colors.ifEmpty { defaultGradient }))
    ) {
        if (thumbnail != null) {
            Image(
                bitmap = thumbnail.asImageBitmap(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Brush.verticalGradient(listOf(Color.Transparent, Color(0xAA1F1F28)))),
            )
        } else {
            SoftSparkles()
        }
        overlay?.invoke(this)
    }
}

@Composable
fun LocalWallpaperCard(
    item: WallpaperItem,
    thumbnail: Bitmap?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .width(118.dp)
            .clickable(onClick = onClick),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        WallpaperPreview(
            colors = localGradientFor(item.category),
            thumbnail = thumbnail,
            modifier = Modifier.height(162.dp).fillMaxWidth(),
            cornerRadius = 18,
            overlay = {
                Text(
                    text = if (item.isFavorite) "♥ 收藏" else "▶ ${item.useCount}",
                    color = Color.White,
                    fontSize = 12.sp,
                    modifier = Modifier.align(Alignment.BottomStart).padding(10.dp),
                )
            },
        )
        Text(
            text = item.displayName ?: "本地动态壁纸",
            color = LuminaText,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
fun LocalCategoryCard(
    item: WallpaperItem,
    thumbnail: Bitmap?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    WallpaperPreview(
        colors = localGradientFor(item.category),
        thumbnail = thumbnail,
        modifier = modifier
            .fillMaxWidth()
            .height(108.dp)
            .shadow(8.dp, RoundedCornerShape(22.dp))
            .clickable(onClick = onClick),
        cornerRadius = 22,
        overlay = {
            Column(
                modifier = Modifier.align(Alignment.BottomStart).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(item.displayName ?: "本地动态壁纸", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 17.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("${item.category} · ${VideoFileInspector.formatDuration(item.durationMs)}", color = Color.White.copy(alpha = 0.85f), fontSize = 13.sp)
            }
        },
    )
}

@Composable
fun LuminaChip(text: String, selected: Boolean = false, onClick: (() -> Unit)? = null) {
    Surface(
        color = if (selected) LuminaSecondary else Color.White,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
    ) {
        Text(
            text = text,
            color = if (selected) LuminaPrimary else LuminaMuted,
            fontSize = 13.sp,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
        )
    }
}

@Composable
fun ActionTile(label: String, symbol: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(LuminaSecondary),
            contentAlignment = Alignment.Center,
        ) {
            Text(symbol, color = LuminaPrimary, fontWeight = FontWeight.Bold)
        }
        Text(label, color = LuminaText, fontSize = 12.sp)
    }
}

@Composable
fun InfoCard(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Surface(
        color = Color.White,
        shape = RoundedCornerShape(24.dp),
        modifier = modifier.shadow(8.dp, RoundedCornerShape(24.dp), ambientColor = Color(0x11000000), spotColor = Color(0x11000000)),
    ) {
        Box(modifier = Modifier.padding(16.dp)) {
            content()
        }
    }
}

@Composable
private fun SoftSparkles() {
    Box(Modifier.fillMaxSize()) {
        listOf(
            Triple(24.dp, 28.dp, 8.dp),
            Triple(90.dp, 74.dp, 5.dp),
            Triple(170.dp, 38.dp, 7.dp),
        ).forEach { (x, y, size) ->
            Box(
                modifier = Modifier
                    .padding(start = x, top = y)
                    .size(size)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.45f)),
            )
        }
    }
}

@Composable
fun SectionHeader(title: String, action: String? = null, onAction: (() -> Unit)? = null) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, color = LuminaText, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Spacer(Modifier.weight(1f))
        if (action != null) {
            Text(
                text = action,
                color = LuminaMuted,
                fontSize = 12.sp,
                modifier = Modifier.then(if (onAction != null) Modifier.clickable(onClick = onAction) else Modifier),
            )
        }
    }
}

private val defaultGradient = listOf(Color(0xFFEEEAF7), Color(0xFFD7C8F6), Color(0xFF7E6AAE))

fun localGradientFor(category: String): List<Color> = when (category) {
    "风景" -> listOf(Color(0xFFE7F0FF), Color(0xFF8DA9D8), Color(0xFF475A87))
    "动漫" -> listOf(Color(0xFFF9E8FF), Color(0xFFB99AE8), Color(0xFF6E54A2))
    "游戏" -> listOf(Color(0xFF241B45), Color(0xFF7458C7), Color(0xFF6AD7FF))
    "科幻" -> listOf(Color(0xFF17213C), Color(0xFF4E54A8), Color(0xFF93E4FF))
    "插画" -> listOf(Color(0xFFFFF1F5), Color(0xFFDAB8F8), Color(0xFF8166B1))
    else -> defaultGradient
}
