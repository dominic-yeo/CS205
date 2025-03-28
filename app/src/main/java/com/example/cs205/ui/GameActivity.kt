package com.example.cs205.ui

import android.content.Intent
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class GameActivity : ComponentActivity() {
    private val viewModel: GameViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
        WinScreen(gameState, onBackToTitle)
    } else {
        GamePlayScreen(gameState, viewModel)
    }
}

@Composable
fun WinScreen(gameState: GameState, onBackToTitle: () -> Unit) {
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
        
        Text(
            text = "Final Score: ${gameState.score}",
            style = MaterialTheme.typography.titleLarge
        )
        
        Text(
            text = "Time: ${gameState.timeElapsed / 1000}s",
            style = MaterialTheme.typography.titleLarge
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Button(
            onClick = onBackToTitle,
            modifier = Modifier.fillMaxWidth(0.7f)
        ) {
            Text("Back to Title Screen")
        }
    }
}

@Composable
fun GamePlayScreen(gameState: GameState, viewModel: GameViewModel) {
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
                    onSelect = { viewModel.selectProcess(process) }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Resources
        Text(
            text = "Resources",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(gameState.availableResources) { resource ->
                ResourceCard(
                    resource = resource,
                    isCollected = resource in gameState.collectedResources,
                    isAvailable = resource.spawnTime <= gameState.timeElapsed,
                    onCollect = { viewModel.collectResource(resource) },
                    gameState = gameState
                )
            }
        }
    }
}

@Composable
fun ProcessCard(
    process: Process,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onSelect),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) 
                MaterialTheme.colorScheme.primaryContainer 
            else 
                MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
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
                    Text(
                        text = "✓",
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.titleLarge
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Required Resources:",
                style = MaterialTheme.typography.bodyMedium
            )
            
            Row(
                modifier = Modifier.padding(top = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                process.requiredResources.forEach { resource ->
                    ResourceChip(resource)
                }
            }
        }
    }
}

@Composable
fun ResourceCard(
    resource: Resource,
    isCollected: Boolean,
    isAvailable: Boolean,
    onCollect: () -> Unit,
    gameState: GameState
) {
    val canCollect = isAvailable && 
                     gameState.currentProcess != null && 
                     gameState.currentProcess.requiredResources.any { it.id == resource.id }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                onClick = onCollect,
                enabled = canCollect
            ),
        colors = CardDefaults.cardColors(
            containerColor = when {
                isCollected -> MaterialTheme.colorScheme.secondaryContainer
                !isAvailable -> MaterialTheme.colorScheme.surfaceVariant
                !canCollect -> MaterialTheme.colorScheme.errorContainer
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
                ResourceChip(resource)
                Text(
                    text = resource.name,
                    style = MaterialTheme.typography.bodyLarge
                )
            }
            when {
                isCollected -> Text("✓", color = MaterialTheme.colorScheme.primary)
                !isAvailable -> Text("⏳", color = MaterialTheme.colorScheme.onSurfaceVariant)
                gameState.currentProcess == null -> Text("Select a process first", 
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error)
                !gameState.currentProcess.requiredResources.any { it.id == resource.id } -> 
                    Text("Not needed by selected process",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
fun ResourceChip(resource: Resource) {
    Box(
        modifier = Modifier
            .size(24.dp)
            .clip(CircleShape)
            .background(Color(resource.color))
            .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
    )
} 