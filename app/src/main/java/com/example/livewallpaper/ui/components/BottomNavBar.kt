package com.example.livewallpaper.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.livewallpaper.ui.LuminaMuted
import com.example.livewallpaper.ui.LuminaPrimary
import com.example.livewallpaper.ui.LuminaSecondary

data class LuminaTab(
    val route: String,
    val label: String,
    val symbol: String,
)

@Composable
fun LuminaBottomNavBar(
    selectedRoute: String,
    tabs: List<LuminaTab>,
    onTabSelected: (String) -> Unit,
    onAddVideo: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        color = Color.White,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        modifier = modifier.shadow(12.dp, RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            tabs.take(2).forEach { tab ->
                BottomTab(tab, selectedRoute == tab.route) { onTabSelected(tab.route) }
            }
            Box(
                modifier = Modifier
                    .size(58.dp)
                    .background(LuminaPrimary, CircleShape)
                    .clickable(onClick = onAddVideo),
                contentAlignment = Alignment.Center,
            ) {
                Text("+", color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.Light)
            }
            tabs.drop(2).forEach { tab ->
                BottomTab(tab, selectedRoute == tab.route) { onTabSelected(tab.route) }
            }
        }
    }
}

@Composable
private fun BottomTab(tab: LuminaTab, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(width = 58.dp, height = 52.dp)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        androidx.compose.foundation.layout.Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = tab.symbol,
                color = if (selected) LuminaPrimary else LuminaMuted,
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = tab.label,
                color = if (selected) LuminaPrimary else LuminaMuted,
                fontSize = 11.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            )
            if (selected) {
                Box(
                    modifier = Modifier
                        .padding(top = 3.dp)
                        .size(width = 14.dp, height = 3.dp)
                        .background(LuminaSecondary, RoundedCornerShape(3.dp)),
                )
            }
        }
    }
}
