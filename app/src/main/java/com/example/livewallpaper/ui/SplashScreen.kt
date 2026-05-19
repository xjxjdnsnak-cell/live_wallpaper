package com.example.livewallpaper.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(onFinished: () -> Unit) {
    val visible = remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        visible.value = true
        delay(900)
        onFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color.White, LuminaSecondary, Color(0xFFD8CCF2)))),
    ) {
        SoftLight(Modifier.align(Alignment.TopEnd).padding(top = 72.dp, end = 42.dp))
        SoftLight(Modifier.align(Alignment.BottomStart).padding(start = 36.dp, bottom = 118.dp))
        AnimatedVisibility(
            visible = visible.value,
            enter = fadeIn(),
            modifier = Modifier.align(Alignment.Center),
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("LUMINA", color = LuminaPrimaryDark, fontSize = 42.sp, fontWeight = FontWeight.Bold, letterSpacing = 4.sp)
                Text("WALLPAPER", color = LuminaMuted, fontSize = 12.sp, letterSpacing = 3.sp)
                Text(
                    "每一张壁纸，点亮你的日常",
                    color = LuminaMuted,
                    fontSize = 15.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 26.dp),
                )
            }
        }
    }
}

@Composable
private fun SoftLight(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(128.dp)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.42f)),
    )
}
