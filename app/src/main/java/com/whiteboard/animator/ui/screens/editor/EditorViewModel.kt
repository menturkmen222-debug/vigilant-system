package com.whiteboard.animator.ui.screens.editor

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.whiteboard.animator.data.model.ProjectWithScenes
import com.whiteboard.animator.data.model.Scene
import com.whiteboard.animator.data.repository.ProjectRepository
import com.whiteboard.animator.engine.AnimationEngine
import com.whiteboard.animator.data.model.SceneBitmaps
import com.whiteboard.animator.engine.AssetManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class EditorViewModel @Inject constructor(
    private val repository: ProjectRepository,
    private val assetManager: AssetManager,
    private val ttsManager: com.whiteboard.animator.engine.TTSManager
) : ViewModel() {

    private val _projectData = MutableStateFlow<ProjectWithScenes?>(null)
    val projectData = _projectData.asStateFlow()
    
    // Editor State
    private val _selectedSceneId = MutableStateFlow<Long?>(null)
    val selectedSceneId = _selectedSceneId.asStateFlow()
    
    private val _isPlaying = MutableStateFlow(false)
    val isPlaying = _isPlaying.asStateFlow()
    
    private val _playbackProgress = MutableStateFlow(0f)
    val playbackProgress = _playbackProgress.asStateFlow()
    
    // Current frame for preview
    private val _currentPreviewFrame = MutableStateFlow<Bitmap?>(null)
    val currentPreviewFrame = _currentPreviewFrame.asStateFlow()
    
    private var playbackJob: Job? = null
    private val animationEngine = AnimationEngine()
    
    // Cache for bitmaps
    var sceneBitmaps = SceneBitmaps()
        private set

    fun getParams(): SceneBitmaps {
        return sceneBitmaps
    }

    fun loadProject(projectId: Long) {
        viewModelScope.launch {
            repository.getProjectFlow(projectId).collect { project ->
                if (project != null) {
                    val fullData = repository.getProjectWithScenes(projectId)
                    _projectData.value = fullData
                    
                    if (_selectedSceneId.value == null && fullData?.scenes?.isNotEmpty() == true) {
                        _selectedSceneId.value = fullData.scenes.first().scene.id
                    }
                    
                    // Preload basic assets for preview
                    loadBitmaps(fullData)
                }
            }
        }
    }

    private suspend fun loadBitmaps(data: ProjectWithScenes?) {
        data ?: return
        // For preview, we currently just load default or simple
        // But to better support per-scene, let's load what we need.
        val handBitmap = assetManager.loadBuiltInBitmap("hand_writing") // Default for now
        val bgBitmap = assetManager.loadBuiltInBitmap("bg_whiteboard")
        
        // Map to all scenes for now (optimization: load on demand or proper map)
        val handMap = data.scenes.associate { it.scene.id to (handBitmap ?: return@associate null) }.filterValues { it != null } as Map<Long, Bitmap>
        val bgMap = data.scenes.associate { it.scene.id to (bgBitmap ?: return@associate null) }.filterValues { it != null } as Map<Long, Bitmap>
        
        sceneBitmaps = SceneBitmaps(
            sceneHandBitmaps = handMap,
            sceneBackgroundBitmaps = bgMap
        )
    }

    fun selectScene(sceneId: Long) {
        _selectedSceneId.value = sceneId
        renderPreviewFrame(sceneId, 1.0f) // Show full scene
    }

    fun togglePlayback() {
        if (_isPlaying.value) {
            pausePlayback()
        } else {
            startPlayback()
        }
    }

    private fun startPlayback() {
        playbackJob?.cancel()
        _isPlaying.value = true
        
        playbackJob = viewModelScope.launch {
            val startTime = System.currentTimeMillis()
            val project = _projectData.value?.project ?: return@launch
            val scenes = _projectData.value?.scenes ?: return@launch
            
            // Calculate total duration roughly
            val totalDuration = scenes.sumOf { it.scene.durationMs + it.scene.transitionDurationMs }
            
            while (isActive && _isPlaying.value) {
                val elapsed = System.currentTimeMillis() - startTime
                val progress = (elapsed.toFloat() / totalDuration).coerceIn(0f, 1f)
                _playbackProgress.value = progress
                
                if (progress >= 1f) {
                    _isPlaying.value = false
                    break
                }
                
                // Determine current scene based on progress
                // Logic needed to find which scene is active at current time
                // Then render frame using AnimationEngine
                
                delay(16) // ~60fps
            }
        }
    }

    private fun pausePlayback() {
        _isPlaying.value = false
        playbackJob?.cancel()
    }
    
    private fun renderPreviewFrame(sceneId: Long, progress: Float) {
        // Implementation to render static frame for preview
        // Use AnimationEngine.renderFrame to a Bitmap
        // Post to _currentPreviewFrame
    }

    fun deleteScene(sceneId: Long) {
        viewModelScope.launch {
            repository.deleteScene(sceneId)
            // Determine new selection if current was deleted
        }
    }
    
    fun reorderScene(fromIndex: Int, toIndex: Int) {
        val projectId = _projectData.value?.project?.id ?: return
        viewModelScope.launch {
            repository.reorderScenes(projectId, fromIndex, toIndex)
        }
    }

    fun saveDrawing(sceneId: Long, paths: List<List<com.whiteboard.animator.data.model.PathPoint>>, color: Int, width: Float) {
        viewModelScope.launch {
            paths.forEachIndexed { index, points ->
                if (points.isNotEmpty()) {
                    repository.addDrawingPath(
                        sceneId = sceneId,
                        points = points,
                        color = color,
                        strokeWidth = width,
                        orderIndex = index // This logic might need refinement for accumulating index
                    )
                }
            }
            // Trigger refresh
            loadProject(_projectData.value?.project?.id ?: return@launch)
        }
    }

    fun updateSceneText(sceneId: Long, newText: String) {
        viewModelScope.launch {
            repository.updateSceneText(sceneId, newText)
            loadProject(_projectData.value?.project?.id ?: return@launch)
        }
    }

    fun generateVoiceover(sceneId: Long, text: String) {
        viewModelScope.launch {
            val project = _projectData.value?.project ?: return@launch
            val outputDir = assetManager.getProjectVoiceoverDir(project.id)
            val fileName = "vo_${sceneId}_${System.currentTimeMillis()}"
            
            val result = ttsManager.generateAudio(text, outputDir, fileName)
            
            result.onSuccess { ttsResult ->
                // Update scene with new voiceover path and duration
                // We should also update duration to match audio if audio is longer than current duration?
                // For now, just set path.
                val scene = _projectData.value?.scenes?.find { it.scene.id == sceneId }?.scene
                if (scene != null) {
                    val updatedScene = scene.copy(
                        voiceoverPath = ttsResult.filePath
                        // We could auto-update duration here:
                        // durationMs = maxOf(scene.durationMs, ttsResult.durationMs)
                    )
                    repository.updateScene(updatedScene)
                    loadProject(project.id)
                }
            }
        }
    }
}
