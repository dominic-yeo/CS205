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

    fun captureAndShareScreenshot() {
        component.window?.decorView?.rootView?.let { rootView ->
            try {
                val bitmap = createBitmap(rootView.width, rootView.height)
                val canvas = Canvas(bitmap)
                rootView.draw(canvas)
                bitmapState.value = bitmap
                shareBitmap(context, bitmap)
                // The last expression in the try block is the call to shareBitmap,
                // which likely returns Unit (or its result is unused).
            } catch (e: Exception) {
                Log.e("WinScreen", "Error capturing screenshot: ${e.message}")
                Toast.makeText(context, "Failed to capture screenshot", Toast.LENGTH_SHORT).show()
                // The last expression in the catch block is the call to Toast.makeText,
                // which returns Unit.
            }
        }
    // Check and update high score when the screen is first displayed
    LaunchedEffect(Unit) {
        viewModel.checkAndUpdateHighScore(finalScore, gameState.level)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "🎉 Congratulations! 🎉",
            style = MaterialTheme.typography.headlineLarge,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "You've completed all processes!",
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Score Breakdown
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = "Score Breakdown",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Text("Base Score: ${gameState.score}")
                Text("Time: ${timeInSeconds}s")
                Text("Time Penalty: $timePenalty")
                Divider(modifier = Modifier.padding(vertical = 8.dp))
                Text(
                    text = "Final Score: $finalScore",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                
                // Add high score comparison
                val highScore = viewModel.highScore
                if (finalScore > highScore) {
                    Text(
                        text = "🏆 New High Score!",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                } else {
                    Text(
                        text = "High Score: $highScore",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Button(
            onClick = onBackToTitle,
            modifier = Modifier.fillMaxWidth(0.7f)
        ) {
            Text("Back to Title Screen")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                captureAndShareScreenshot()
            },
            modifier = Modifier.fillMaxWidth(0.7f)
        ) {
            Text("Save High Score")
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
                    onSelect = { },
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

    // Render dragged resource overlay
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
            dragState.resourceInstance?.let { ResourceChip(it.resource) }
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
    // Add animation states
    val scale by animateFloatAsState(
        targetValue = if (process.isCompleted) 1.05f else 1f,
        animationSpec = spring(
            dampingRatio = 0.7f,
            stiffness = 400f
        ),
        label = "scale"
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
    isFulfilled: Boolean = false
) {
    Box(
        modifier = Modifier
            .size(24.dp)
            .clip(CircleShape)
            .background(Color(resource.color))
            .border(
                width = if (isFulfilled) 2.dp else 1.dp,
                color = if (isFulfilled) Color.Green else MaterialTheme.colorScheme.outline,
                shape = CircleShape
            )
    )
}