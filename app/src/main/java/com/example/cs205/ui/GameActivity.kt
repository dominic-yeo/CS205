package com.example.cs205.ui

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.cs205.model.GameState
import com.example.cs205.model.Process
import com.example.cs205.model.Resource
import com.example.cs205.model.ResourceInstance
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.IntOffset
import kotlin.math.roundToInt
import androidx.compose.animation.core.*
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.alpha
import androidx.compose.runtime.getValue
import androidx.compose.animation.animateColorAsState
import android.graphics.Paint
import android.util.Log
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import androidx.core.graphics.createBitmap
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import com.example.cs205.ui.components.TrailEffect
import com.example.cs205.ui.components.ProcessCompletionEffects
import com.example.cs205.ui.components.WinScreenConfetti
import com.example.cs205.ui.components.AnimatedWinText
import com.example.cs205.ui.components.SparkleEffect
import android.media.MediaPlayer
import android.provider.Settings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers

class GameActivity : ComponentActivity() {
    private val viewModel: GameViewModel by viewModels { GameViewModelFactory(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Get the level from the intent
        val level = intent.getIntExtra("LEVEL", 1)
        viewModel.initializeLevel(level)

        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    GameScreen(
                        viewModel = viewModel,
                        onBackToTitle = {
                            // Create a new intent to start TitleScreenActivity
                            val intent = Intent(this, TitleScreenActivity::class.java)
                            // Clear the activity stack and start fresh
                            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            startActivity(intent)
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun GameScreen(viewModel: GameViewModel, onBackToTitle: () -> Unit) {
    val gameState by viewModel.gameState.collectAsState()
    val scope = rememberCoroutineScope()

    // Update time every second
    LaunchedEffect(Unit) {
        while (true) {
            viewModel.updateTime()
            delay(1000)
        }
    }

    if (gameState.isGameWon) {
        WinScreen(gameState, viewModel, onBackToTitle)
    } else {
        GamePlayScreen(gameState, viewModel)
    }
}

fun shareBitmap(context: Context, bitmap: Bitmap) {
    try {
        val cachePath = File(context.cacheDir, "images")
        cachePath.mkdirs()
        val file = File(cachePath, "screenshot.png")
        val fileOutputStream = FileOutputStream(file)
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, fileOutputStream)
        fileOutputStream.close()

        val uri = FileProvider.getUriForFile(
            context,
            "com.example.cs205.fileprovider",
            file
        )

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        context.startActivity(Intent.createChooser(shareIntent, "Share Screenshot"))

    } catch (e: IOException) {
        Log.e("WinScreen", "Error sharing bitmap: ${e.message}")
        Toast.makeText(context, "Failed to share screenshot", Toast.LENGTH_SHORT).show()
    }
}

@Composable
fun WinScreen(
    gameState: GameState,
    viewModel: GameViewModel,
    onBackToTitle: () -> Unit
) {
    val timeInSeconds = gameState.timeElapsed / 1000
    val timePenalty = (-(timeInSeconds / 3).toInt()) // -1 point per 10 seconds
    val finalScore = gameState.score + timePenalty

    val context = LocalContext.current
    val component = context as ComponentActivity
    val bitmapState = remember { mutableStateOf<Bitmap?>(null) }
    
    // Animation states for entering elements
    val contentAlpha = remember { Animatable(0f) }
    val contentScale = remember { Animatable(0.8f) }
    
    // Animate content entry
    LaunchedEffect(Unit) {
        contentAlpha.animateTo(
            targetValue = 1f,
            animationSpec = tween(1000, easing = FastOutSlowInEasing)
        )
        contentScale.animateTo(
            targetValue = 1f,
            animationSpec = spring(
                dampingRatio = 0.6f,
                stiffness = 300f
            )
        )
    }

    fun captureAndShareScreenshot() {
        component.window?.decorView?.rootView?.let { rootView ->
            try {
                val bitmap = createBitmap(rootView.width, rootView.height)
                val canvas = Canvas(bitmap)
                rootView.draw(canvas)
                bitmapState.value = bitmap
                shareBitmap(context, bitmap)
            } catch (e: Exception) {
                Log.e("WinScreen", "Error capturing screenshot: ${e.message}")
                Toast.makeText(context, "Failed to capture screenshot", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    // Check and update high score when the screen is first displayed
    // Add score as points for shop
    LaunchedEffect(Unit) {
        viewModel.addPoints(finalScore)
        viewModel.checkAndUpdateHighScore(finalScore, gameState.level)
        
        // Play a victory sound
        try {
            val soundId = android.provider.Settings.System.DEFAULT_NOTIFICATION_URI
            val mediaPlayer = MediaPlayer().apply {
                setDataSource(context, soundId)
                prepare()
                start()
                setOnCompletionListener { mp -> mp.release() }
            }
        } catch (e: Exception) {
            Log.e("WinScreen", "Error playing sound: ${e.message}")
        }
    }

    // Full-screen background with sparkles
    Box(modifier = Modifier.fillMaxSize()) {
        // Background sparkle effect
        SparkleEffect(modifier = Modifier.fillMaxSize())
        
        // Confetti overlay
        WinScreenConfetti(modifier = Modifier.fillMaxSize())
        
        // Main content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .alpha(contentAlpha.value)
                .scale(contentScale.value),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Animated congratulations text
            AnimatedWinText(
                text = "🎉 Congratulations! 🎉",
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "You've completed all processes!",
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onBackground
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Score Breakdown
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f)
                ),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(
                    defaultElevation = 6.dp
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    Text(
                        text = "Score Breakdown",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    Text("Base Score: ${gameState.score}")
                    Text("Time: ${timeInSeconds}s")
                    Text("Time Penalty: $timePenalty")
                    Divider(modifier = Modifier.padding(vertical = 12.dp))
                    Text(
                        text = "Final Score: $finalScore",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    
                    // Add high score comparison
                    val highScore = viewModel.highScore
                    if (finalScore > highScore) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(top = 12.dp)
                        ) {
                            Text(
                                text = "🏆 New High Score!",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                            // Add pulsing animation for new high score
                            val pulseAnimation = rememberInfiniteTransition(label = "pulse")
                            val pulse by pulseAnimation.animateFloat(
                                initialValue = 1f,
                                targetValue = 1.2f,
                                animationSpec = infiniteRepeatable(
                                    animation = tween(600, easing = FastOutSlowInEasing),
                                    repeatMode = RepeatMode.Reverse
                                ),
                                label = "pulseAnim"
                            )
                            
                            Text(
                                text = " $finalScore",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.scale(pulse)
                            )
                        }
                    } else {
                        Text(
                            text = "High Score: $highScore",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.padding(top = 12.dp)
                        )
                    }

                    val points by viewModel.osPoints.collectAsState(initial = 0)
                    Text(
                        text = "OS Points: $points",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Buttons with animations
            val buttonScale = remember { Animatable(1f) }
            val shareButtonScale = remember { Animatable(1f) }
            val scope = rememberCoroutineScope()
            
            Button(
                onClick = { 
                    // Animate button press
                    scope.launch {
                        buttonScale.animateTo(
                            targetValue = 0.9f,
                            animationSpec = tween(100)
                        )
                        buttonScale.animateTo(
                            targetValue = 1f,
                            animationSpec = spring(
                                dampingRatio = 0.6f,
                                stiffness = 300f
                            )
                        )
                        onBackToTitle()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth(0.7f)
                    .scale(buttonScale.value),
                shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text(
                    "Back to Title Screen", 
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            
            Button(
                onClick = {
                    // Animate button press
                    scope.launch {
                        shareButtonScale.animateTo(
                            targetValue = 0.9f,
                            animationSpec = tween(100)
                        )
                        shareButtonScale.animateTo(
                            targetValue = 1f,
                            animationSpec = spring(
                                dampingRatio = 0.6f,
                                stiffness = 300f
                            )
                        )
                        captureAndShareScreenshot()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth(0.7f)
                    .scale(shareButtonScale.value),
                shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondary
                )
            ) {
                Text(
                    "Share High Score",
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
        }
    }
}

// Data class to track dragging state
data class DragState(
    val isDragging: Boolean = false,
    val resourceInstance: ResourceInstance? = null,
    val initialPosition: Offset = Offset.Zero,
    val dragOffset: Offset = Offset.Zero
)

@Composable
fun GamePlayScreen(gameState: GameState, viewModel: GameViewModel) {
    var dragState by remember { mutableStateOf(DragState()) }
    var processPositions by remember { mutableStateOf(mutableMapOf<Int, Offset>()) }
    var processCardSizes by remember { mutableStateOf(mutableMapOf<Int, Size>()) }

    Box(modifier = Modifier.fillMaxSize()) {
        // Main game content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Score and Time
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Score: ${gameState.score}",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "Time: ${gameState.timeElapsed / 1000}s",
                    style = MaterialTheme.typography.titleMedium
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Add instruction text
            if (gameState.currentProcess == null) {
                Text(
                    text = "Select a process to begin collecting resources",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            // Processes
            Text(
                text = "Processes",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(gameState.processes) { process ->
                    ProcessCard(
                        process = process,
                        isSelected = process == gameState.currentProcess,
                        onSelect = { viewModel.selectProcess(process) },
                        collectedResources = if (process == gameState.currentProcess) {
                            gameState.collectedResources
                        } else {
                            emptyList()
                        },
                        modifier = Modifier.onGloballyPositioned { coordinates ->
                            processPositions[process.id] = coordinates.boundsInWindow().topLeft
                            processCardSizes[process.id] = Size(
                                coordinates.size.width.toFloat(),
                                coordinates.size.height.toFloat()
                            )
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Resources
            Text(
                text = "Resources",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(vertical = 8.dp)
            )

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(
                    items = gameState.resourceInstances,
                    key = { it.instanceId }
                ) { resourceInstance ->
                    DraggableResourceCard(
                        resourceInstance = resourceInstance,
                        isCollected = resourceInstance in gameState.collectedResources,
                        isAvailable = resourceInstance.spawnTime <= gameState.timeElapsed,
                        onDragStart = { position -> 
                            dragState = DragState(
                                isDragging = true,
                                resourceInstance = resourceInstance,
                                initialPosition = position,
                                dragOffset = Offset.Zero
                            )
                        },
                        onDrag = { _, dragAmount ->
                            dragState = dragState.copy(
                                dragOffset = dragState.dragOffset + dragAmount
                            )
                        },
                        onDragEnd = {
                            // Check if resource was dropped on a valid process
                            processPositions.forEach { (processId, position) ->
                                val size = processCardSizes[processId] ?: return@forEach
                                val finalPosition = dragState.initialPosition + dragState.dragOffset
                                if (finalPosition.x in position.x..(position.x + size.width) &&
                                    finalPosition.y in position.y..(position.y + size.height)
                                ) {
                                    val process = gameState.processes.find { it.id == processId }
                                    if (process != null && !process.isCompleted) {
                                        viewModel.selectProcess(process)
                                        dragState.resourceInstance?.let { 
                                            viewModel.collectResource(it)
                                        }
                                    }
                                }
                            }
                            dragState = DragState()
                        },
                        onDiscard = { viewModel.discardResource(resourceInstance) },
                        gameState = gameState
                    )
                }
            }
        }

        // Trail effect - positioned as overlay in the Box
        if (dragState.isDragging) {
            val currentPosition = dragState.initialPosition + dragState.dragOffset
            dragState.resourceInstance?.let { resource ->
                TrailEffect(
                    isDragging = true,
                    currentPosition = currentPosition,
                    color = Color(resource.resource.color),
                    maxPoints = 30,
                    tailLifetime = 700,
                    pointSpacing = 4,
                    headSize = 0f
                )
            }
        }

        // Dragged resource - on top of everything
        if (dragState.isDragging) {
            Box(
                modifier = Modifier
                    .offset { 
                        IntOffset(
                            (dragState.initialPosition.x + dragState.dragOffset.x).roundToInt(),
                            (dragState.initialPosition.y + dragState.dragOffset.y).roundToInt()
                        )
                    }
            ) {
                ResourceChip(
                    resource = dragState.resourceInstance?.resource ?: return@Box,
                    size = 32.dp  // Make the dragged resource slightly larger
                )
            }
        }
    }
}

@Composable
fun ProcessCard(
    process: Process,
    isSelected: Boolean,
    onSelect: () -> Unit,
    modifier: Modifier,
    collectedResources: List<ResourceInstance> = emptyList()
) {
    // Track if we should show the completion effect
    var showCompletionEffect by remember { mutableStateOf(false) }
    var scoreVisible by remember { mutableStateOf(false) }
    val context = LocalContext.current
    
    // Add animation states
    val scale by animateFloatAsState(
        targetValue = if (process.isCompleted) 1.05f else 1f,
        animationSpec = spring(
            dampingRatio = 0.7f,
            stiffness = 400f
        ),
        label = "scale"
    )
    
    // Animated values for score floating upward
    val scoreYOffset by animateFloatAsState(
        targetValue = if (scoreVisible) -80f else 0f,
        animationSpec = tween(2000, easing = FastOutSlowInEasing),
        label = "scoreYOffset"
    )
    
    // Animated value for score fading out
    val scoreOpacity by animateFloatAsState(
        targetValue = if (scoreVisible) 0f else 1f,
        animationSpec = tween(2000, easing = LinearEasing),
        finishedListener = { 
            if (scoreVisible) scoreVisible = false 
        },
        label = "scoreOpacity"
    )
    
    val alpha by animateFloatAsState(
        targetValue = if (process.isCompleted) 0.9f else 1f,
        animationSpec = tween(durationMillis = 500),
        label = "alpha"
    )

    val backgroundColor by animateColorAsState(
        targetValue = when {
            process.isCompleted -> MaterialTheme.colorScheme.secondaryContainer
            isSelected -> MaterialTheme.colorScheme.primaryContainer
            else -> MaterialTheme.colorScheme.surface
        },
        animationSpec = tween(durationMillis = 500),
        label = "background"
    )

    // Detect process completion to trigger effects
    LaunchedEffect(process.isCompleted) {
        if (process.isCompleted) {
            showCompletionEffect = true
            scoreVisible = true
            
            // Play completion sound
            try {
                // Use a system sound that exists in Android
                val soundId = android.provider.Settings.System.DEFAULT_NOTIFICATION_URI
                val mediaPlayer = MediaPlayer().apply {
                    setDataSource(context, soundId)
                    prepare()
                    start()
                    setOnCompletionListener { mp -> mp.release() }
                }
            } catch (e: Exception) {
                Log.e("ProcessCard", "Error playing sound: ${e.message}")
            }
            
            // Reset completion effect after some time
            kotlinx.coroutines.delay(2000)
            showCompletionEffect = false
            // Note: The score animation will finish on its own
        }
    }

    Box {
        // The main card
        Card(
            modifier = modifier
                .fillMaxWidth()
                .scale(scale)
                .alpha(alpha)
                .clickable(enabled = !process.isCompleted) { onSelect() },
            colors = CardDefaults.cardColors(
                containerColor = backgroundColor
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = process.name,
                        style = MaterialTheme.typography.titleMedium
                    )
                    
                    if (process.isCompleted) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Completed",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .size(24.dp)
                                .scale(scale)
                        )
                    }
                }
                
                if (process.isCompleted) {
                    Text(
                        text = "Completed!",
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                } else {
                    Text(
                        text = "Needs:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    // Group required resources by their ID and count occurrences
                    val requiredCounts = process.requiredResources
                        .groupBy { it.id }
                        .mapValues { it.value.size }
                    
                    // Count collected resources by their ID
                    val collectedCounts = collectedResources
                        .groupBy { it.resource.id }
                        .mapValues { it.value.size }
                    
                    // Display each unique required resource with its count
                    requiredCounts.forEach { (resourceId, requiredCount) ->
                        val resource = process.requiredResources.first { it.id == resourceId }
                        val collectedCount = collectedCounts[resourceId] ?: 0
                        
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            ResourceChip(
                                resource = resource,
                                isFulfilled = collectedCount >= requiredCount
                            )
                            Text(
                                text = "$collectedCount/$requiredCount",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (collectedCount >= requiredCount) 
                                    MaterialTheme.colorScheme.primary 
                                else 
                                    MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
        
        // Process completion effect overlay
        ProcessCompletionEffects(
            isCompleted = showCompletionEffect && process.isCompleted,
            modifier = Modifier.fillMaxSize()
        )
        
        // Floating score animation - float up and fade away
        if (scoreVisible) {
            Text(
                text = "+100",
                color = Color(0xFF4CAF50),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset(y = scoreYOffset.dp)
                    .alpha(1f - scoreOpacity)
            )
        }
    }
}

@Composable
fun DraggableResourceCard(
    resourceInstance: ResourceInstance,
    isCollected: Boolean,
    isAvailable: Boolean,
    onDragStart: (Offset) -> Unit,
    onDrag: (change: Offset, dragAmount: Offset) -> Unit,
    onDragEnd: () -> Unit,
    onDiscard: () -> Unit,
    gameState: GameState
) {
    var cardPosition by remember { mutableStateOf(Offset.Zero) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .onGloballyPositioned { coordinates ->
                // Store the card's position
                cardPosition = coordinates.boundsInWindow().topLeft
            }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { offset ->
                        if (isAvailable && !isCollected) {
                            // Pass the absolute position (card position + touch offset)
                            onDragStart(cardPosition + offset)
                        }
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        onDrag(change.position, dragAmount)
                    },
                    onDragEnd = { onDragEnd() },
                    onDragCancel = { onDragEnd() }
                )
            },
        colors = CardDefaults.cardColors(
            containerColor = when {
                isCollected -> MaterialTheme.colorScheme.secondaryContainer
                !isAvailable -> MaterialTheme.colorScheme.surfaceVariant
                else -> MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ResourceChip(resourceInstance.resource)
                Text(
                    text = resourceInstance.resource.name,
                    style = MaterialTheme.typography.bodyLarge
                )
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                when {
                    isCollected -> Text("✓", color = MaterialTheme.colorScheme.primary)
                    !isAvailable -> Text("⏳", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (!isCollected) {
                    IconButton(
                        onClick = onDiscard,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Text("×", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }
}

@Composable
fun ResourceChip(
    resource: Resource,
    isFulfilled: Boolean = false,
    size: androidx.compose.ui.unit.Dp = 24.dp
) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(Color(resource.color))
            .border(
                width = if (isFulfilled) 2.dp else 1.dp,
                color = if (isFulfilled) Color.Green else MaterialTheme.colorScheme.outline,
                shape = CircleShape
            )
    )
}