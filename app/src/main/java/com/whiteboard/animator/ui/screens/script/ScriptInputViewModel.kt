package com.whiteboard.animator.ui.screens.script

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.whiteboard.animator.data.preferences.PreferenceManager
import com.whiteboard.animator.data.repository.ProjectRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ScriptInputViewModel @Inject constructor(
    private val repository: ProjectRepository,
    private val preferenceManager: PreferenceManager
) : ViewModel() {

    private val _script = MutableStateFlow("")
    val script = _script.asStateFlow()

    private val _isGenerating = MutableStateFlow(false)
    val isGenerating = _isGenerating.asStateFlow()

    // Preview of how many scenes will be generated
    private val _scenePreview = MutableStateFlow<List<String>>(emptyList())
    val scenePreview = _scenePreview.asStateFlow()

    fun onScriptChanged(text: String) {
        _script.value = text
        updatePreview(text)
    }

    private fun updatePreview(text: String) {
        if (text.isBlank()) {
            _scenePreview.value = emptyList()
            return
        }

        // Simple preview logic matching repository implementation
        val paragraphs = text.split(Regex("\\n\\s*\\n"))
            .map { it.trim() }
            .filter { it.isNotEmpty() }
        
        if (paragraphs.size > 1) {
            _scenePreview.value = paragraphs
        } else {
            // Fallback to sentence splitting
            _scenePreview.value = text.split(Regex("(?<=[.!?])\\s+"))
                .map { it.trim() }
                .filter { it.isNotEmpty() }
        }
    }

    fun generateProject(onProjectCreated: (Long) -> Unit) {
        if (_script.value.isBlank()) return

        viewModelScope.launch {
            _isGenerating.value = true
            try {
                // Create project
                val projectId = repository.createProject(
                    name = "New Project ${System.currentTimeMillis() / 1000}",
                    script = _script.value
                )

                // Generate scenes
                repository.createScenesFromScript(projectId, _script.value)
                
                onProjectCreated(projectId)
            } finally {
                _isGenerating.value = false
            }
        }
    }

    fun generateScriptFromTopic(topic: String) {
        if (topic.isBlank()) return
        
        viewModelScope.launch {
            _isGenerating.value = true
            try {
                // Get default style from preferences
                val defaultStyle = preferenceManager.defaultVideoStyle.first()
                val generatedScript = repository.generateStyledScriptFromTopic(topic, defaultStyle)
                onScriptChanged(generatedScript)
            } catch (e: Exception) {
                // Fallback if anything fails (e.g. preference read)
                onScriptChanged("Error: ${e.message}")
            } finally {
                _isGenerating.value = false
            }
        }
    }
}
