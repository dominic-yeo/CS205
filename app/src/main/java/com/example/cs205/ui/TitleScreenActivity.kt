package com.example.cs205.ui

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class TitleScreenActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    TitleScreen(
                        onStartGame = { startGame() },
                        onLevels = { showLevels() }
                    )
                }
            }
        }
    }

    private fun startGame() {
        val intent = Intent(this, GameActivity::class.java)
        intent.putExtra("LEVEL", 1) // Start with level 1 for quick start
        startActivity(intent)
    }

    private fun showLevels() {
        val intent = Intent(this, LevelsActivity::class.java)
        startActivity(intent)
    }

    override fun onPause() {
        super.onPause()
        // TODO: Pause background music when implemented
    }

    override fun onResume() {
        super.onResume()
        // TODO: Resume background music when implemented
    }
}

@Composable
fun TitleScreen(
    onStartGame: () -> Unit,
    onLevels: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Deadlock Dash",
            style = MaterialTheme.typography.headlineLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 32.dp)
        )
        
        Button(
            onClick = onStartGame,
            modifier = Modifier
                .fillMaxWidth(0.7f)
                .padding(bottom = 16.dp)
        ) {
            Text("Quick Start")
        }

        Button(
            onClick = onLevels,
            modifier = Modifier.fillMaxWidth(0.7f)
        ) {
            Text("Levels")
        }
    }
} 