package com.example.cs205.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun AnimatedWinText(
    text: String,
    modifier: Modifier = Modifier
) {
    // Create an infinite animation for color cycling
    val infiniteTransition = rememberInfiniteTransition(label = "colorTransition")
    val colorIndex by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000),
            repeatMode = RepeatMode.Restart
        ),
        label = "colorCycle"
    )
    
    // Create a pulsing scale animation
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )
    
    // Define vibrant colors to cycle through
    val colors = listOf(
        Color(0xFFFF5252), // Red
        Color(0xFFFF4081), // Pink
        Color(0xFF7C4DFF), // Deep Purple
        Color(0xFF536DFE), // Indigo
        Color(0xFF448AFF), // Blue
        Color(0xFF40C4FF), // Light Blue
        Color(0xFF18FFFF), // Cyan
        Color(0xFF69F0AE), // Green
        Color(0xFFFFF176), // Yellow
        Color(0xFFFFD54F)  // Amber
    )
    
    // Calculate the current color based on the animation progress
    val currentColorIndex = (colorIndex * colors.size).toInt() % colors.size
    val nextColorIndex = (currentColorIndex + 1) % colors.size
    val progress = (colorIndex * colors.size) % 1f
    
    val currentColor = lerp(
        colors[currentColorIndex],
        colors[nextColorIndex],
        progress
    )
    
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.headlineLarge.copy(
                fontSize = 32.sp
            ),
            color = currentColor,
            textAlign = TextAlign.Center,
            modifier = Modifier.scale(scale)
        )
    }
}

// Helper function to linearly interpolate between two colors
private fun lerp(start: Color, end: Color, fraction: Float): Color {
    return Color(
        red = start.red + (end.red - start.red) * fraction,
        green = start.green + (end.green - start.green) * fraction,
        blue = start.blue + (end.blue - start.blue) * fraction,
        alpha = start.alpha + (end.alpha - start.alpha) * fraction
    )
} 