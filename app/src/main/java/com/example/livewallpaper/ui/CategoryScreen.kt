package com.example.livewallpaper.ui

import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
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
import com.example.livewallpaper.ui.components.InfoCard
import com.example.livewallpaper.ui.components.LocalCategoryCard

@Composable
fun CategoryScreen(
    items: List<WallpaperItem>,
    thumbnails: Map<String, Bitmap>,
    onOpenDetail: (String) -> Unit,
) {
    var selectedCategory by remember { mutableStateOf(WallpaperItem.CATEGORIES.first()) }
    val wallpapers = items.filter { it.category == selectedCategory }
    val counts = remember(items) { items.groupingBy { it.category }.eachCount() }

    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(LuminaBackground)
            .padding(top = 18.dp, bottom = 92.dp),
    ) {
        Column(
            modifier = Modifier
                .width(104.dp)
                .fillMaxHeight()
                .padding(start = 12.dp, end = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("分类", color = LuminaText, fontSize = 24.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(8.dp))
            WallpaperItem.CATEGORIES.forEach { category ->
                CategoryTab(
                    text = category,
                    count = counts[category] ?: 0,
                    selected = selectedCategory == category,
                    onClick = { selectedCategory = category },
                )
            }
        }

        LazyColumn(
            modifier = Modifier.weight(1f).padding(end = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Text(selectedCategory, color = LuminaText, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.weight(1f))
                    Text("${wallpapers.size} 张", color = LuminaMuted, fontSize = 14.sp)
                }
                Spacer(Modifier.height(14.dp))
            }
            if (wallpapers.isEmpty()) {
                item {
                    InfoCard {
                        Text("这个分类还没有本地壁纸", color = LuminaMuted)
                    }
                }
            } else {
                items(wallpapers) { item ->
                    LocalCategoryCard(item = item, thumbnail = thumbnails[item.id], onClick = { onOpenDetail(item.id) })
                }
            }
            item { Spacer(Modifier.height(16.dp)) }
        }
    }
}

@Composable
private fun CategoryTab(text: String, count: Int, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(if (selected) Color.White else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (selected) {
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(20.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(LuminaPrimary),
            )
            Spacer(Modifier.width(8.dp))
        }
        Column {
            Text(
                text = text,
                color = if (selected) LuminaPrimary else LuminaMuted,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                fontSize = 14.sp,
            )
            Text("$count", color = LuminaMuted, fontSize = 10.sp)
        }
    }
}
