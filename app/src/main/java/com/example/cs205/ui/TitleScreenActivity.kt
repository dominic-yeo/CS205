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
                        onShowCredits = { showCredits() }
                    )
                }
            }
        }
    }

    private fun startGame() {
        val intent = Intent(this, GameActivity::class.java)
        startActivity(intent)
        finish() // Close the title screen
    }

    private fun showCredits() {
        val intent = Intent(this, CreditsActivity::class.java)
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
    onShowCredits: () -> Unit
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
            fontSize = 48.sp,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.headlineLarge
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "Prevent deadlocks. Complete the processes.",
            fontSize = 18.sp,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyLarge
        )
        
        Spacer(modifier = Modifier.height(48.dp))
        
        Button(
            onClick = onStartGame,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp),
            contentPadding = PaddingValues(16.dp)
        ) {
            Text("Start Game", fontSize = 20.sp)
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Button(
            onClick = onShowCredits,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp),
            contentPadding = PaddingValues(12.dp)
        ) {
            Text("Credits", fontSize = 18.sp)
        }
    }
} 