package com.whiteboard.animator.ui.screens.export

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.whiteboard.animator.data.model.AspectRatio
import com.whiteboard.animator.data.model.Project
import com.whiteboard.animator.data.model.Resolution
import com.whiteboard.animator.data.model.SceneBitmaps
import com.whiteboard.animator.data.repository.ProjectRepository
import com.whiteboard.animator.engine.AssetManager
import com.whiteboard.animator.engine.VideoExporter
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@HiltViewModel
class ExportViewModel @Inject constructor(
    private val repository: ProjectRepository,
    private val videoExporter: VideoExporter,
    private val assetManager: AssetManager,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _project = MutableStateFlow<Project?>(null)
    val project = _project.asStateFlow()

    private val _isExporting = MutableStateFlow(false)
    val isExporting = _isExporting.asStateFlow()
    
    val exportProgress = videoExporter.progress
    val exportStatus = videoExporter.status

    private val _selectedResolution = MutableStateFlow(Resolution.HD_720P)
    val selectedResolution = _selectedResolution.asStateFlow()

    fun loadProject(projectId: Long) {
        viewModelScope.launch {
            _project.value = repository.getProject(projectId)
        }
    }
    
    fun setResolution(resolution: Resolution) {
        _selectedResolution.value = resolution
    }

    fun startExport(onComplete: (String) -> Unit) {
        val proj = _project.value ?: return
        if (_isExporting.value) return
        
        _isExporting.value = true
        
        viewModelScope.launch {
            try {
                // Get all data
                val fullData = repository.getProjectWithScenes(proj.id) ?: return@launch
                
                // Load all bitmaps
                val bitmaps = prepareBitmaps(fullData)
                
                // Helper to get project storage dir
                val dir = File(context.getExternalFilesDir(android.os.Environment.DIRECTORY_MOVIES), "WhiteboardExports")
                dir.mkdirs()
                val filename = "Export_${proj.name.replace(" ", "_")}_${System.currentTimeMillis()}.mp4"
                val file = File(dir, filename)
                
                // Map asset lists and drawings
                val sceneAssetsMap = fullData.scenes.associate { it.scene.id to it.assets }
                val sceneDrawingsMap = fullData.scenes.associate { it.scene.id to it.drawings }
                
                val result = videoExporter.exportVideo(
                    scenes = fullData.scenes.map { it.scene },
                    sceneAssets = sceneAssetsMap,
                    sceneDrawings = sceneDrawingsMap, // Pass drawings
                    bitmaps = bitmaps,
                    outputPath = file.absolutePath,
                    resolution = _selectedResolution.value,
                    aspectRatio = proj.aspectRatio
                )
                
                if (result.isSuccess) {
                    onComplete(result.getOrNull() ?: "")
                }
                
            } finally {
                _isExporting.value = false
            }
        }
    }

    private suspend fun prepareBitmaps(data: com.whiteboard.animator.data.model.ProjectWithScenes): SceneBitmaps {
        val assetBitmaps = mutableMapOf<Long, android.graphics.Bitmap>()
        val sceneBackgroundBitmaps = mutableMapOf<Long, android.graphics.Bitmap>()
        val sceneHandBitmaps = mutableMapOf<Long, android.graphics.Bitmap>()

        // Cache loaded files to avoid duplicates
        val bitmapCache = mutableMapOf<String, android.graphics.Bitmap>()

        // 1. Assets
        data.scenes.flatMap { it.assets }.forEach { asset ->
            if (!assetBitmaps.containsKey(asset.id)) {
                val bitmap = if (asset.isBuiltIn) {
                    assetManager.loadBuiltInBitmap(asset.assetPath)
                } else {
                    assetManager.loadBitmapFromPath(asset.assetPath)
                }
                if (bitmap != null) {
                    assetBitmaps[asset.id] = bitmap
                }
            }
        }

        // 2. Backgrounds & Hands
        data.scenes.forEach { sceneItem ->
            val scene = sceneItem.scene
            
            // Background
            val bgPath = scene.backgroundImagePath ?: data.project.customBackgroundPath
            var bgBitmap: android.graphics.Bitmap? = null
            
            if (bgPath != null) {
                 // Custom/Specific path
                 bgBitmap = bitmapCache.getOrPut(bgPath) {
                     assetManager.loadBitmapFromPath(bgPath) ?: assetManager.loadBuiltInBitmap(bgPath) ?: return@getOrPut null!!
                 }
            } else {
                // Built-in types
                val resource = when(data.project.backgroundType) {
                    com.whiteboard.animator.data.model.BackgroundType.CHALKBOARD -> "bg_chalkboard"
                    com.whiteboard.animator.data.model.BackgroundType.PAPER -> "bg_paper"
                    else -> null // Whiteboard
                }
                if (resource != null) {
                    bgBitmap = bitmapCache.getOrPut(resource) {
                         assetManager.loadBuiltInBitmap(resource) ?: return@getOrPut null!!
                    }
                }
            }
            if (bgBitmap != null) {
                sceneBackgroundBitmaps[scene.id] = bgBitmap
            }

            // Hand
            val handStyle = scene.handStyle ?: data.project.handStyle
            val handResource = when(handStyle) {
                 com.whiteboard.animator.data.model.HandStyle.POINTING -> "hand_pointing"
                 com.whiteboard.animator.data.model.HandStyle.WRITING -> "hand_writing"
                 com.whiteboard.animator.data.model.HandStyle.CARTOON -> "hand_cartoon"
                 com.whiteboard.animator.data.model.HandStyle.MARKER -> "hand_marker"
                 else -> null
            }
            if (handResource != null) {
                val handBitmap = bitmapCache.getOrPut(handResource) {
                     assetManager.loadBuiltInBitmap(handResource) ?: return@getOrPut null!!
                }
                if (handBitmap != null) sceneHandBitmaps[scene.id] = handBitmap
            }
        }

        return SceneBitmaps(sceneHandBitmaps, sceneBackgroundBitmaps, assetBitmaps)
    }}
