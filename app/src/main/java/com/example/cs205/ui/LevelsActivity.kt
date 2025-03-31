package com.example.cs205.ui

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

class LevelsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    LevelsScreen(
                        onLevelSelected = { level ->
                            val intent = Intent(this, GameActivity::class.java)
                            intent.putExtra("LEVEL", level)
                            startActivity(intent)
                        },
                        onBack = { finish() }
                    )
                }
            }
        }
    }
}

@Composable
fun LevelsScreen(onLevelSelected: (Int) -> Unit, onBack: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Back button
        Button(
            onClick = onBack,
            modifier = Modifier.padding(bottom = 16.dp)
        ) {
            Text("Back")
        }

        Text(
            text = "Select Level",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(2) { level ->
                LevelCard(
                    level = level + 1,
                    isUnlocked = true,
                    onSelect = { onLevelSelected(level + 1) }
                )
            }
            // Locked levels
            items(2) { level ->
                LevelCard(
                    level = level + 3,
                    isUnlocked = false,
                    onSelect = { }
                )
            }
        }
    }
}

@Composable
fun LevelCard(level: Int, isUnlocked: Boolean, onSelect: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f),
        colors = CardDefaults.cardColors(
            containerColor = if (isUnlocked) 
                MaterialTheme.colorScheme.primaryContainer 
            else 
                MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            if (isUnlocked) {
                Button(
                    onClick = onSelect,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Level $level")
                }
            } else {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "Locked level",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
} 