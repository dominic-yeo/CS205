package com.example.cs205.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.example.cs205.model.GameState
import com.example.cs205.model.Process
import com.example.cs205.model.Resource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class GameViewModel : ViewModel() {
    private val _gameState = MutableStateFlow(createInitialGameState())
    val gameState: StateFlow<GameState> = _gameState.asStateFlow()

    private var startTime: Long = 0
    private var lastResourceSpawnTime: Long = 0
    private val resourceSpawnInterval = 5000L // Spawn a new resource every 5 seconds
    private var isGameActive = true
    private var nextResourceIndex = 0

    init {
        startTime = System.currentTimeMillis()
        lastResourceSpawnTime = startTime
    }

    fun selectProcess(process: Process) {
        _gameState.update { currentState ->
            if (currentState.currentProcess != null) {
                // If switching processes, lose all collected resources
                currentState.copy(
                    currentProcess = process,
                    collectedResources = emptyList()
                )
            } else {
                currentState.copy(currentProcess = process)
            }
        }
    }

    fun collectResource(resource: Resource) {
        _gameState.update { currentState ->
            val currentProcess = currentState.currentProcess ?: return@update currentState
            
            if (currentProcess.requiredResources.any { it.id == resource.id } && 
                currentState.collectedResources.none { it.id == resource.id } &&
                resource.spawnTime <= currentState.timeElapsed) {
                
                val newCollectedResources = currentState.collectedResources + resource
                
                // Check if process is completed
                if (currentProcess.requiredResources.all { required ->
                    newCollectedResources.any { collected -> collected.id == required.id }
                }) {
                    val updatedProcesses = currentState.processes.map { 
                        if (it.id == currentProcess.id) it.copy(isCompleted = true)
                        else it
                    }
                    
                    // Check if all processes are completed
                    val isGameWon = updatedProcesses.all { it.isCompleted }
                    
                    if (isGameWon) {
                        isGameActive = false
                    }
                    
                    currentState.copy(
                        processes = updatedProcesses,
                        currentProcess = null,
                        collectedResources = emptyList(),
                        score = currentState.score + 100,
                        isGameWon = isGameWon
                    )
                } else {
                    currentState.copy(collectedResources = newCollectedResources)
                }
            } else {
                currentState
            }
        }
    }

    fun updateTime() {
        if (!isGameActive) return
        
        _gameState.update { currentState ->
            val currentTime = System.currentTimeMillis()
            val timeElapsed = currentTime - startTime
            
            // Check if we should spawn a new resource
            if (currentTime - lastResourceSpawnTime >= resourceSpawnInterval) {
                lastResourceSpawnTime = currentTime
                spawnNewResource(timeElapsed)
            }
            
            currentState.copy(timeElapsed = timeElapsed)
        }
    }

    private fun spawnNewResource(spawnTime: Long) {
        _gameState.update { currentState ->
            if (nextResourceIndex < currentState.availableResources.size) {
                val updatedResources = currentState.availableResources.toMutableList()
                // Set spawn time for the next resource
                updatedResources[nextResourceIndex] = updatedResources[nextResourceIndex].copy(spawnTime = spawnTime)
                nextResourceIndex++
                currentState.copy(availableResources = updatedResources)
            } else {
                currentState
            }
        }
    }

    private fun createInitialGameState(): GameState {
        val resources = listOf(
            Resource("R1", "CPU", 0xFFE57373, 0), // First resource available immediately
            Resource("R2", "Memory", 0xFF81C784, Long.MAX_VALUE),
            Resource("R3", "Disk", 0xFF64B5F6, Long.MAX_VALUE),
            Resource("R4", "Network", 0xFFFFB74D, Long.MAX_VALUE),
            Resource("R5", "Printer", 0xFFBA68C8, Long.MAX_VALUE)
        )

        val processes = listOf(
            Process(1, "Process A", listOf(resources[0], resources[1])),
            Process(2, "Process B", listOf(resources[1], resources[2], resources[3])),
            Process(3, "Process C", listOf(resources[0], resources[3], resources[4]))
        )

        return GameState(
            processes = processes,
            availableResources = resources
        )
    }
} 