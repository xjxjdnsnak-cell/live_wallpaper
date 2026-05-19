package com.example.livewallpaper.ui

import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.livewallpaper.data.WallpaperItem
import com.example.livewallpaper.ui.components.InfoCard
import com.example.livewallpaper.ui.components.LocalWallpaperCard
import com.example.livewallpaper.ui.components.SectionHeader

@Composable
fun FavoritesScreen(
    items: List<WallpaperItem>,
    thumbnails: Map<String, Bitmap>,
    onOpenDetail: (String) -> Unit,
    onToggleFavorite: (WallpaperItem) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(LuminaBackground)
            .verticalScroll(rememberScrollState())
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        Text("收藏", color = LuminaText, fontSize = 26.sp, fontWeight = FontWeight.Bold)
        if (items.isEmpty()) {
            InfoCard {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("还没有收藏", color = LuminaText, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Text("在详情页点击收藏，喜欢的动态壁纸会出现在这里。", color = LuminaMuted, fontSize = 13.sp)
                }
            }
        } else {
            SectionHeader("我的收藏", "${items.size} 张")
            androidx.compose.foundation.layout.Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items.forEach { item ->
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        LocalWallpaperCard(item = item, thumbnail = thumbnails[item.id], onClick = { onOpenDetail(item.id) })
                        Button(
                            onClick = { onToggleFavorite(item) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = LuminaPrimary),
                        ) {
                            Text("取消收藏", fontSize = 12.sp)
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(90.dp))
    }
}
