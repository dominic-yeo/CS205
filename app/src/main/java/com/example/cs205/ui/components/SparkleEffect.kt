package com.example.cs205.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random

@Composable
fun SparkleEffect(
    modifier: Modifier = Modifier
) {
    val sparkles = remember { mutableStateListOf<Sparkle>() }
    val coroutineScope = rememberCoroutineScope()
    
    // Create initial sparkles
    LaunchedEffect(Unit) {
        repeat(20) {
            delay(Random.nextLong(0, 500)) // Stagger sparkle creation
            sparkles.add(
                Sparkle(
                    position = Offset(
                        Random.nextFloat() * 1000,
                        Random.nextFloat() * 1500
                    ),
                    scale = Random.nextFloat() * 0.5f + 0.5f,
                    rotationSpeed = Random.nextFloat() * 2f + 1f,
                    color = Color(
                        red = 0.9f + Random.nextFloat() * 0.1f,
                        green = 0.9f + Random.nextFloat() * 0.1f,
                        blue = 0.8f + Random.nextFloat() * 0.2f,
                        alpha = 0.8f + Random.nextFloat() * 0.2f
                    )
                )
            )
        }
        
        // Keep adding new sparkles occasionally
        coroutineScope.launch {
            while (true) {
                delay(500)
                if (sparkles.size < 40) { // Keep max 40 sparkles
                    sparkles.add(
                        Sparkle(
                            position = Offset(
                                Random.nextFloat() * 1000,
                                Random.nextFloat() * 1500
                            ),
                            scale = Random.nextFloat() * 0.5f + 0.5f,
                            rotationSpeed = Random.nextFloat() * 2f + 1f,
                            color = Color(
                                red = 0.9f + Random.nextFloat() * 0.1f,
                                green = 0.9f + Random.nextFloat() * 0.1f,
                                blue = 0.8f + Random.nextFloat() * 0.2f,
                                alpha = 0.8f + Random.nextFloat() * 0.2f
                            )
                        )
                    )
                }
            }
        }
    }
    
    // Animation for all sparkles
    val infiniteTransition = rememberInfiniteTransition(label = "sparkleTransition")
    val pulseSize by infiniteTransition.animateFloat(
        initialValue = 0.7f,
        targetValue = 1.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 1000,
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "sparkleSize"
    )

    Box(modifier = modifier) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            sparkles.forEach { sparkle ->
                sparkle.update()
                
                // Draw four-pointed star
                withTransform({
                    translate(sparkle.position.x, sparkle.position.y)
                    rotate(sparkle.rotation)
                    scale(sparkle.scale * pulseSize * sparkle.pulseOffset)
                }) {
                    val starPath = Path().apply {
                        val outerRadius = 15f
                        val innerRadius = 5f
                        
                        moveTo(0f, -outerRadius)
                        lineTo(innerRadius * 0.5f, -innerRadius * 0.5f)
                        lineTo(outerRadius, 0f)
                        lineTo(innerRadius * 0.5f, innerRadius * 0.5f)
                        lineTo(0f, outerRadius)
                        lineTo(-innerRadius * 0.5f, innerRadius * 0.5f)
                        lineTo(-outerRadius, 0f)
                        lineTo(-innerRadius * 0.5f, -innerRadius * 0.5f)
                        close()
                    }
                    
                    // Draw with gradient from center
                    drawPath(
                        path = starPath,
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color.White,
                                sparkle.color
                            ),
                            center = Offset.Zero,
                            radius = 20f
                        ),
                        alpha = sparkle.alpha
                    )
                    
                    // Add glow effect
                    drawCircle(
                        color = sparkle.color.copy(alpha = 0.3f),
                        radius = 10f * sparkle.scale * pulseSize,
                        alpha = sparkle.alpha * 0.7f
                    )
                }
            }
        }
    }
}

class Sparkle(
    val position: Offset,
    val scale: Float,
    val rotationSpeed: Float,
    val color: Color
) {
    var rotation by mutableStateOf(Random.nextFloat() * 360f)
    var alpha by mutableStateOf(0f)
    var pulseOffset by mutableStateOf(0.8f + Random.nextFloat() * 0.4f)
    private val fadeInSpeed = 0.05f + Random.nextFloat() * 0.05f
    private val fadeOutTrigger = 0.95f + Random.nextFloat() * 0.05f
    private val fadeOutSpeed = 0.01f + Random.nextFloat() * 0.02f
    private var state = SparkleState.FADING_IN
    
    fun update() {
        rotation = (rotation + rotationSpeed) % 360f
        
        when (state) {
            SparkleState.FADING_IN -> {
                alpha += fadeInSpeed
                if (alpha >= fadeInTrigger()) {
                    alpha = 1f
                    state = SparkleState.VISIBLE
                }
            }
            SparkleState.VISIBLE -> {
                if (Random.nextFloat() > fadeOutTrigger) {
                    state = SparkleState.FADING_OUT
                }
            }
            SparkleState.FADING_OUT -> {
                alpha -= fadeOutSpeed
                if (alpha <= 0f) {
                    alpha = 0f
                    state = SparkleState.FADING_IN
                }
            }
        }
    }
    
    private fun fadeInTrigger(): Float {
        return 0.7f + Random.nextFloat() * 0.3f
    }
    
    enum class SparkleState {
        FADING_IN,
        VISIBLE,
        FADING_OUT
    }
} 