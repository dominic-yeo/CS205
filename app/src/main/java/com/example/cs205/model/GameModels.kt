package com.example.cs205.model

data class Resource(
    val id: String,
    val name: String,
    val color: Long, // Color value for visual representation
    val spawnTime: Long = 0 // Time in milliseconds when the resource becomes available
)

data class Process(
    val id: Int,
    val name: String,
    val requiredResources: List<Resource>,
    val isCompleted: Boolean = false
)

data class GameState(
    val processes: List<Process>,
    val availableResources: List<Resource>,
    val currentProcess: Process? = null,
    val collectedResources: List<Resource> = emptyList(),
    val score: Int = 0,
    val timeElapsed: Long = 0,
    val isGameWon: Boolean = false
) 