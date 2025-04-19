package com.example.cs205.ui

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.cs205.data.HighScoreDbHelper

class TitleScreenActivity : ComponentActivity() {
    private val highScoreDbHelper by lazy { HighScoreDbHelper(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    var currentHighScore by remember { mutableStateOf(highScoreDbHelper.getHighScore(1)) }
                    
                    TitleScreen(
                        onStartGame = { startGame() },
                        onLevels = { showLevels() },
                        onShop = { goShop() },
                        highScore = currentHighScore,
                        onResetHighScores = {
                            highScoreDbHelper.resetAllHighScores()
                            currentHighScore = 0
                        }
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

    private fun goShop() {
        val intent = Intent(this, ShopActivity::class.java)
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
    onLevels: () -> Unit,
    onShop: () -> Unit,
    highScore: Int,
    onResetHighScores: () -> Unit
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
        
        // High score display
        Card(
            modifier = Modifier
                .fillMaxWidth(0.7f)
                .padding(bottom = 16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "High Score",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = highScore.toString(),
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
        
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
            modifier = Modifier
                .fillMaxWidth(0.7f)
                .padding(bottom = 16.dp)
        ) {
            Text("Levels")
        }

        Button(
            onClick = onShop,
            modifier = Modifier
                .fillMaxWidth(0.7f)
                .padding(bottom = 16.dp)
        ) {
            Text("Shop")
        }

        // // Add reset button
        // OutlinedButton(
        //     onClick = onResetHighScores,
        //     modifier = Modifier
        //         .fillMaxWidth(0.7f)
        //         .padding(top = 8.dp),
        //     colors = ButtonDefaults.outlinedButtonColors(
        //         contentColor = MaterialTheme.colorScheme.error
        //     )
        // ) {
        //     Text("Reset High Scores")
        // }
    }
} 