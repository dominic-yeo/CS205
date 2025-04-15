package com.example.cs205.ui

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
import com.example.cs205.data.ShopItem

val shopItems = listOf(
    ShopItem(
        id = 1,
        name = "Faster Resources I",
        description = "Resources are produced 1s faster",
        cost = 500,
        effect = { gameState -> gameState.copy() }
    )
)

class ShopActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    var score by remember { mutableIntStateOf(1000) } // Example score
                    ShopScreen(
                        onBack = { finish() },
                        shopItems = shopItems,
                        osPoints = score,
                        onBuy = { item ->
                            if (score >= item.cost) {
                                score -= item.cost
                                item.Buy()
                            } else {
                                // not enough points
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun ShopScreen(
    onBack: () -> Unit,
    shopItems: List<ShopItem>,
    osPoints: Int,
    onBuy: (ShopItem) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "OS Upgrades",
            style = MaterialTheme.typography.headlineLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 32.dp)
        )

        Text(
            text = "Welcome to the OS Upgrade Shop!",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            shopItems.forEach { item ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .padding(vertical = 8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = item.name,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = item.description,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Cost: ${item.cost}",
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Button(
                                onClick = { onBuy(item) },
                                enabled = osPoints >= item.cost
                            ) {
                                Text("Buy")
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "OS Points: $osPoints",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center
        )

        Button(
            onClick = onBack,
            modifier = Modifier
                .fillMaxWidth(0.7f)
                .padding(top = 32.dp)
        ) {
            Text("Back to Title")
        }
    }
}