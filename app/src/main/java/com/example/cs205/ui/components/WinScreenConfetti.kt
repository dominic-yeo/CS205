package com.example.cs205.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random
import kotlin.math.sin

@Composable
fun WinScreenConfetti(
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val colors = remember {
        listOf(
            Color(0xFFE91E63), // Pink
            Color(0xFF2196F3), // Blue
            Color(0xFF4CAF50), // Green
            Color(0xFFFFC107), // Amber
            Color(0xFF9C27B0), // Purple
            Color(0xFFFF5722), // Deep Orange
            Color(0xFF00BCD4), // Cyan
            Color(0xFFFFEB3B), // Yellow
        )
    }

    val confetti = remember { mutableStateListOf<ConfettiPiece>() }
    var showConfetti by remember { mutableStateOf(true) }

    // Track time for smooth floating motion
    var time by remember { mutableStateOf(0f) }
    
    // Update time for wave motion
    LaunchedEffect(Unit) {
        val startTime = System.currentTimeMillis()
        while (true) {
            time = (System.currentTimeMillis() - startTime) / 1000f
            delay(16) // roughly 60fps
        }
    }

    // Create initial confetti
    LaunchedEffect(Unit) {
        // Clear any existing confetti
        confetti.clear()
        
        // Create new confetti pieces with staggered start positions
        repeat(100) { index ->
            // Stagger the starting positions from top to ensure continued flow
            val yOffset = if (index < 20) {
                -Random.nextFloat() * 100  // First batch starts very close to the top
            } else if (index < 50) {
                -Random.nextFloat() * 300  // Second batch starts a bit higher
            } else {
                -Random.nextFloat() * 600  // Rest start much higher for delayed entry
            }
            
            confetti.add(
                ConfettiPiece(
                    initialPosition = Offset(
                        Random.nextFloat() * 1000,
                        yOffset
                    ),
                    rotation = Random.nextFloat() * 360f,
                    rotationSpeed = Random.nextFloat() * 10f - 5f, // Moderate rotation speed
                    color = colors.random(),
                    confettiSize = 10f + Random.nextFloat() * 15f, // Slightly smaller for gentler look
                    velocity = Offset(
                        Random.nextFloat() * 2f - 1f, // Subtle horizontal drift
                        Random.nextFloat() * 1.5f + 0.5f // Slow falling speed
                    ),
                    waveFrequency = Random.nextFloat() * 2f + 1f, // For gentle side-to-side motion
                    waveAmplitude = Random.nextFloat() * 1.5f + 0.5f // Small wave amplitude
                )
            )
        }

        // Keep the confetti visible for longer
        coroutineScope.launch {
            delay(15000) // Show for 15 seconds
            showConfetti = false
        }
    }

    Box(modifier = modifier) {
        Canvas(
            modifier = Modifier.fillMaxSize()
        ) {
            if (showConfetti) {
                confetti.forEach { piece ->
                    withTransform({
                        translate(
                            left = piece.position.x,
                            top = piece.position.y
                        )
                        rotate(piece.rotation)
                    }) {
                        // Draw different confetti shapes for variety
                        when (piece.shape) {
                            ConfettiShape.RECTANGLE -> {
                                // Draw filled rectangle
                                drawRect(
                                    color = piece.color.copy(alpha = piece.alpha),
                                    size = Size(piece.confettiSize, piece.confettiSize * 0.5f)
                                )
                                
                                // Draw outline for better visibility
                                drawRect(
                                    color = piece.color.copy(alpha = piece.alpha),
                                    size = Size(piece.confettiSize, piece.confettiSize * 0.5f),
                                    style = Stroke(width = 1f)
                                )
                            }
                            ConfettiShape.CIRCLE -> {
                                // Draw circle confetti
                                drawCircle(
                                    color = piece.color.copy(alpha = piece.alpha),
                                    radius = piece.confettiSize * 0.4f
                                )
                            }
                            ConfettiShape.TRIANGLE -> {
                                // Draw triangle confetti
                                val path = Path().apply {
                                    moveTo(0f, -piece.confettiSize * 0.4f)
                                    lineTo(piece.confettiSize * 0.4f, piece.confettiSize * 0.4f)
                                    lineTo(-piece.confettiSize * 0.4f, piece.confettiSize * 0.4f)
                                    close()
                                }
                                drawPath(
                                    path = path,
                                    color = piece.color.copy(alpha = piece.alpha)
                                )
                            }
                        }
                    }
                    piece.update(size.width, size.height, time)
                }
            }
        }
    }
}

enum class ConfettiShape {
    RECTANGLE, CIRCLE, TRIANGLE
}

class ConfettiPiece(
    val initialPosition: Offset,
    var rotation: Float,
    val rotationSpeed: Float,
    val color: Color,
    val confettiSize: Float,
    val velocity: Offset,
    val waveFrequency: Float = 1f,
    val waveAmplitude: Float = 1f
) {
    var position by mutableStateOf(initialPosition)
    var alpha by mutableStateOf(1f)
    val shape = ConfettiShape.values().random() // Random shape for each piece
    private val initialXPosition = initialPosition.x
    
    fun update(maxWidth: Float, maxHeight: Float, time: Float) {
        // Apply gravity - much lighter for slower falling effect
        val gravity = 0.03f
        
        // Calculate horizontal wave motion using sine wave
        val waveOffset = sin(time * waveFrequency + initialPosition.y * 0.1f) * waveAmplitude
        
        position = Offset(
            initialXPosition + waveOffset + velocity.x,
            position.y + velocity.y + gravity
        )
        
        rotation = (rotation + rotationSpeed * 0.1f) % 360f // Slower rotation
        
        // Reset position if off screen (loop)
        if (position.y > maxHeight + 50) {
            position = Offset(
                Random.nextFloat() * maxWidth,
                -Random.nextFloat() * 200
            )
            alpha = 1f
        } else if (position.y > maxHeight * 0.8f) {
            // Start fading as confetti approaches bottom of screen
            alpha = (maxHeight - position.y) / (maxHeight * 0.2f)
        }
        
        // Handle horizontal looping
        if (position.x < -50) {
            position = Offset(maxWidth + 25, position.y)
        } else if (position.x > maxWidth + 50) {
            position = Offset(-25f, position.y)
        }
    }
} 