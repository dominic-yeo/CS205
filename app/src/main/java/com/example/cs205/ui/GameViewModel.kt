package com.example.cs205.ui

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.example.cs205.model.GameState
import com.example.cs205.model.Process
import com.example.cs205.model.Resource
import com.example.cs205.model.ResourceInstance
import com.example.cs205.util.VibrationUtil
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class GameViewModel(private val context: Context) : ViewModel() {
    private val _gameState = MutableStateFlow(createInitialGameState())
    val gameState: StateFlow<GameState> = _gameState.asStateFlow()

    private var startTime: Long = 0
    private var lastResourceSpawnTime: Long = 0
    private val resourceSpawnInterval = 2000L // Spawn a new resource every 5 seconds
    private var isGameActive = true
    private var nextResourceIndex = 0
    private val MAX_RESOURCES = 5

    init {
        startTime = System.currentTimeMillis()
        lastResourceSpawnTime = startTime
    }

    private fun returnResourcesToPool(currentState: GameState, resourcesToReturn: List<ResourceInstance>): List<ResourceInstance> {
        val remainingSpace = MAX_RESOURCES - currentState.resourceInstances.size
        // If remainingSpace <= 0, don't return any resources
        // Otherwise, return only up to remainingSpace resources
        return if (remainingSpace <= 0) {
            emptyList()
        } else {
            resourcesToReturn.take(remainingSpace)
        }
    }

    fun selectProcess(process: Process) {
        _gameState.update { currentState ->
            // Don't allow selecting completed processes
            if (process.isCompleted) {
                return@update currentState
            }
            
            // If selecting a different process, return collected resources to the pool
            // and deduct points in levels 1-2
            if (currentState.currentProcess != process) {
                val returnedResources = returnResourcesToPool(currentState, currentState.collectedResources)
                val pointDeduction = if (currentState.level <= 2 && currentState.collectedResources.isNotEmpty()) -10 else 0
                
                currentState.copy(
                    currentProcess = process,
                    collectedResources = emptyList(),
                    resourceInstances = currentState.resourceInstances + returnedResources,
                    score = (currentState.score + pointDeduction).coerceAtLeast(0)
                )
            } else {
                currentState
            }
        }
    }

    fun collectResource(resourceInstance: ResourceInstance) {
        _gameState.update { currentState ->
            val currentProcess = currentState.currentProcess ?: return@update currentState
            
            if (currentProcess.isCompleted) {
                return@update currentState
            }

            if (currentProcess.requiredResources.any { it.id == resourceInstance.resource.id } && 
                currentState.collectedResources.none { it.instanceId == resourceInstance.instanceId } &&
                resourceInstance.spawnTime <= currentState.timeElapsed) {
                
                val newCollectedResources = currentState.collectedResources + resourceInstance
                
                // Check if process is completed
                if (currentProcess.requiredResources.all { required ->
                    newCollectedResources.count { it.resource.id == required.id } >= 
                    currentProcess.requiredResources.count { it.id == required.id }
                }) {
                    // Trigger vibration when process completes with a longer duration
                    VibrationUtil.vibrate(context, 500)
                    
                    val updatedProcesses = currentState.processes.map { 
                        if (it.id == currentProcess.id) it.copy(isCompleted = true)
                        else it
                    }
                    
                    val isGameWon = updatedProcesses.all { it.isCompleted }
                    if (isGameWon) {
                        isGameActive = false
                    }
                    
                    // First remove the just-collected resource from resourceInstances
                    val updatedResourceInstances = currentState.resourceInstances.filterNot { 
                        it.instanceId == resourceInstance.instanceId 
                    }
                    
                    // Then try to return all collected resources to the pool
                    val returnedResources = returnResourcesToPool(
                        currentState.copy(resourceInstances = updatedResourceInstances), 
                        newCollectedResources
                    )
                    
                    currentState.copy(
                        processes = updatedProcesses,
                        currentProcess = null,
                        collectedResources = emptyList(),
                        resourceInstances = updatedResourceInstances + returnedResources,
                        score = currentState.score + 100,
                        isGameWon = isGameWon
                    )
                } else {
                    currentState.copy(
                        collectedResources = newCollectedResources,
                        resourceInstances = currentState.resourceInstances.filterNot { 
                            it.instanceId == resourceInstance.instanceId 
                        }
                    )
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
            // Only spawn if we have less than MAX_RESOURCES
            if (currentState.resourceInstances.size >= MAX_RESOURCES) {
                return@update currentState
            }

            if (currentState.level == 2) {
                // For level 2, randomly select a resource to spawn
                val resource = currentState.availableResources.random()
                val newInstance = ResourceInstance(
                    resource = resource,
                    spawnTime = spawnTime
                )
                currentState.copy(
                    resourceInstances = currentState.resourceInstances + newInstance
                )
            } else {
                // Original logic for level 1
                if (nextResourceIndex < currentState.availableResources.size) {
                    val resource = currentState.availableResources[nextResourceIndex]
                    val newInstance = ResourceInstance(
                        resource = resource,
                        spawnTime = spawnTime
                    )
                    nextResourceIndex++
                    currentState.copy(
                        resourceInstances = currentState.resourceInstances + newInstance
                    )
                } else {
                    currentState
                }
            }
        }
    }

    fun initializeLevel(level: Int) {
        _gameState.update { 
            createInitialGameState(level)
        }
    }

    private fun createInitialGameState(level: Int = 1): GameState {
        val resources = listOf(
            Resource("R1", "CPU", 0xFFE57373, 0),
            Resource("R2", "Memory", 0xFF81C784, Long.MAX_VALUE),
            Resource("R3", "Disk", 0xFF64B5F6, Long.MAX_VALUE),
            Resource("R4", "Network", 0xFFFFB74D, Long.MAX_VALUE),
            Resource("R5", "Printer", 0xFFBA68C8, Long.MAX_VALUE)
        )

        val processes = when (level) {
            1 -> listOf(
                Process(1, "Process A", listOf(resources[0], resources[1])),
                Process(2, "Process B", listOf(resources[1], resources[2], resources[3])),
                Process(3, "Process C", listOf(resources[0], resources[3], resources[4]))
            )
            2 -> {
                // Constants for level 2
                val MAX_REQUIREMENTS = 4  // Maximum resources a process can require
                val MAX_DUPLICATES = 2    // Maximum copies of the same resource

                // Create randomized processes
                (1..3).map { processId ->
                    // Randomly decide how many resources this process needs (2 to MAX_REQUIREMENTS)
                    val numRequirements = (2..MAX_REQUIREMENTS).random()
                    
                    // Create list of required resources
                    val requirements = mutableListOf<Resource>()
                    repeat(numRequirements) {
                        // Pick a random resource
                        val resource = resources.random()
                        // Check how many times this resource is already required
                        val currentCount = requirements.count { it.id == resource.id }
                        // Only add if we haven't hit the duplicate limit
                        if (currentCount < MAX_DUPLICATES) {
                            requirements.add(resource)
                        }
                    }
                    
                    Process(processId, "Process ${('A' + processId - 1)}", requirements)
                }
            }
            else -> listOf()
        }

        // For level 2, modify the spawnTime of resources to be 0 so they can spawn multiple times
        val modifiedResources = if (level == 2) {
            resources.map { it.copy(spawnTime = 0) }
        } else {
            resources
        }

        return GameState(
            processes = processes,
            availableResources = modifiedResources,
            level = level
        )
    }

    fun discardResource(resourceInstance: ResourceInstance) {
        _gameState.update { currentState ->
            // Deduct points for discarding in levels 1-2
            val pointDeduction = if (currentState.level <= 2) -10 else 0
            
            currentState.copy(
                resourceInstances = currentState.resourceInstances.filterNot { 
                    it.instanceId == resourceInstance.instanceId 
                },
                score = (currentState.score + pointDeduction).coerceAtLeast(0)
            )
        }
    }

    private fun checkProcessCompletion(state: GameState): GameState {
        val currentProcess = state.currentProcess ?: return state
        
        if (currentProcess.isCompleted) {
            return state
        }

        // Check if all required resources are collected
        if (currentProcess.requiredResources.all { required ->
            state.collectedResources.count { it.resource.id == required.id } >= 
            currentProcess.requiredResources.count { it.id == required.id }
        }) {
            val updatedProcesses = state.processes.map { process ->
                if (process.id == currentProcess.id) {
                    process.copy(isCompleted = true)
                } else {
                    process
                }
            }
            
            val isGameWon = updatedProcesses.all { it.isCompleted }
            if (isGameWon) {
                isGameActive = false
            }
            
            // Instead of returning resources to pool, just discard them
            return state.copy(
                processes = updatedProcesses,
                currentProcess = null,
                collectedResources = emptyList(),
                score = state.score + 100,
                isGameWon = isGameWon
            )
        }
        return state
    }
} 