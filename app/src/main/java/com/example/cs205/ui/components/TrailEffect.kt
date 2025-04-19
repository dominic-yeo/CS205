package com.example.cs205.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.PathEffect
import kotlinx.coroutines.delay
import kotlin.math.max
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

// Data class to represent tail points
data class TailPoint(
    val position: Offset,
    val creationTime: Long,
    val color: Color
)

/**
 * A composable that renders a curved arc trail effect behind an object being dragged,
 * similar to a comet tail that thickens near the head.
 */
@Composable
fun TrailEffect(
    modifier: Modifier = Modifier,
    isDragging: Boolean,
    currentPosition: Offset,
    color: Color,
    maxPoints: Int = 12,        // Number of points to form the curve
    tailLifetime: Int = 400,    // Lifetime of the tail in milliseconds
    pointSpacing: Long = 16,    // Time between recording positions for the curve
    headSize: Float = 24f       // Size of the head circle, 0f to not draw a head
) {
    // Keep track of recent positions to form the curve
    var tailPoints by remember { mutableStateOf(emptyList<TailPoint>()) }
    val currentTime = remember { mutableStateOf(0L) }
    var lastPointTime by remember { mutableStateOf(0L) }
    
    // Update time and add new points when dragging
    LaunchedEffect(isDragging, currentPosition) {
        while (isDragging) {
            val time = System.currentTimeMillis()
            currentTime.value = time
            
            // Add a new point at controlled intervals
            if (time - lastPointTime >= pointSpacing) {
                lastPointTime = time
                
                // Add the current position to the tail points
                tailPoints = (listOf(
                    TailPoint(
                        position = currentPosition,
                        creationTime = time,
                        color = color
                    )
                ) + tailPoints).take(maxPoints)  // Keep only the most recent points
            }
            
            // Remove expired points
            tailPoints = tailPoints.filter { point ->
                time - point.creationTime < tailLifetime
            }
            
            delay(16) // Approximately 60fps
        }
        
        // When not dragging, let tail fade naturally
        if (!isDragging) {
            val time = System.currentTimeMillis()
            tailPoints = tailPoints.filter { point ->
                time - point.creationTime < tailLifetime
            }
        }
    }
    
    Canvas(modifier = modifier.fillMaxSize()) {
        val currentTimeValue = currentTime.value
        
        // Need at least 2 points to draw anything
        if (tailPoints.size < 2) return@Canvas
        
        // Get the most recent point (head position)
        val headPoint = tailPoints.first()
        
        // Draw a solid, thick tapered tail
        tailPoints.asReversed().forEachIndexed { index, point ->
            if (index < tailPoints.size - 1) {
                val nextPoint = tailPoints.asReversed()[index + 1]
                
                // Calculate thickness - gradually thinning from head to tail
                // Use a non-linear curve for more dramatic effect
                val progress = index.toFloat() / (tailPoints.size - 1).toFloat()
                // Use a cubic or higher power curve for a more dramatic comet-like taper
                val taperFactor = 1.0f - (progress * progress * progress)  // Cubic taper for more dramatic effect
                
                // Much thicker at the head - if headSize is 0, use a default thickness
                val baseThickness = if (headSize <= 0f) 40f else headSize * 1.5f
                val strokeWidth = baseThickness * taperFactor
                
                // Higher opacity throughout
                val alpha = 0.9f * (1.0f - (progress * progress * 0.8f))  // Modified alpha to match cubic taper
                
                // Draw a simple line segment between points
                val dx = nextPoint.position.x - point.position.x
                val dy = nextPoint.position.y - point.position.y
                val distance = sqrt(dx * dx + dy * dy)
                
                // Only draw if points are not on top of each other
                if (distance > 0.1f) {
                    drawLine(
                        color = color.copy(alpha = alpha),
                        start = point.position,
                        end = nextPoint.position,
                        strokeWidth = strokeWidth,
                        cap = StrokeCap.Round
                    )
                }
            }
        }
        
        // Only draw the head/sphere if headSize > 0
        if (headSize > 0f) {
            // Draw the head (sphere) - with a bright, large head
            drawCircle(
                color = color.copy(alpha = 1.0f),
                radius = headSize * 0.8f,
                center = headPoint.position
            )
            
            // Inner bright core
            drawCircle(
                color = Color.White.copy(alpha = 0.9f),
                radius = headSize * 0.4f,
                center = headPoint.position
            )
            
            // Add highlight to make the sphere pop
            drawCircle(
                color = Color.White.copy(alpha = 0.7f),
                radius = headSize * 0.2f,
                center = Offset(
                    headPoint.position.x - headSize * 0.15f,
                    headPoint.position.y - headSize * 0.15f
                )
            )
        }
    }
} 