package com.whiteboard.animator.ui.screens.asset

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.whiteboard.animator.data.model.AssetType
import com.whiteboard.animator.data.repository.ProjectRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import java.io.File
import java.io.FileOutputStream

@HiltViewModel
class AssetLibraryViewModel @Inject constructor(
    private val repository: ProjectRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _assets = MutableStateFlow<List<com.whiteboard.animator.data.model.BuiltInAsset>>(emptyList())
    // For now we just mock built-in assets for the UI to consume, 
    // or we could load them from repository if we had a table for "available built-in assets".
    // Since the screen expects objects that look like they have an ID and path:

    // Actually the screen seems to be using BuiltInAsset definition which might be local to UI or checking data model
    // Let's assume we maintain a list of resource pointers.

    // Tabs
    val tabs = listOf("Shapes", "Hands", "Backgrounds", "User")
    
    private val _selectedTab = MutableStateFlow(0)
    val selectedTab = _selectedTab.asStateFlow()

    init {
        loadAssetsForTab(0)
    }

    fun onTabSelected(index: Int) {
        _selectedTab.value = index
        loadAssetsForTab(index)
    }

    private fun loadAssetsForTab(index: Int) {
        // Mock data logic for display
        // In a real app, this would query a "Library" or "Marketplace" repository
        // or list files from assets folder.
        val mockAssets = when(index) {
            0 -> listOf(
                com.whiteboard.animator.data.model.BuiltInAsset("1", "Star", com.whiteboard.animator.data.model.AssetType.ICON, "doodle_star", emptyList(), "Shapes"),
                com.whiteboard.animator.data.model.BuiltInAsset("2", "Arrow", com.whiteboard.animator.data.model.AssetType.ICON, "doodle_arrow", emptyList(), "Shapes"),
                com.whiteboard.animator.data.model.BuiltInAsset("3", "Circle", com.whiteboard.animator.data.model.AssetType.ICON, "doodle_circle", emptyList(), "Shapes"),
                com.whiteboard.animator.data.model.BuiltInAsset("4", "Rectangle", com.whiteboard.animator.data.model.AssetType.SHAPE, "shape_rectangle", emptyList(), "Shapes")
            )
            1 -> listOf(
                 com.whiteboard.animator.data.model.BuiltInAsset("10", "Pointing", com.whiteboard.animator.data.model.AssetType.HAND, "hand_pointing", emptyList(), "Hands"),
                 com.whiteboard.animator.data.model.BuiltInAsset("11", "Writing", com.whiteboard.animator.data.model.AssetType.HAND, "hand_writing", emptyList(), "Hands")
            )
            2 -> listOf(
                 com.whiteboard.animator.data.model.BuiltInAsset("20", "Whiteboard", com.whiteboard.animator.data.model.AssetType.BACKGROUND, "bg_whiteboard", emptyList(), "Backgrounds"),
                 com.whiteboard.animator.data.model.BuiltInAsset("21", "Chalkboard", com.whiteboard.animator.data.model.AssetType.BACKGROUND, "bg_chalkboard", emptyList(), "Backgrounds")
            )
            else -> emptyList()
        }
        _assets.value = mockAssets
    }
    
    val assets = _assets.asStateFlow()

    fun addAssetToScene(sceneId: Long, asset: com.whiteboard.animator.data.model.BuiltInAsset) {
        viewModelScope.launch {
            repository.addBuiltInAsset(
                sceneId = sceneId,
                assetType = asset.type,
                resourceName = asset.path
            )
        }
    }
    
    fun importImage(sceneId: Long, uri: Uri) {
        viewModelScope.launch {
            // Copy to app storage
            val stream = context.contentResolver.openInputStream(uri)
            stream?.use { input ->
                val file = File(context.filesDir, "imported_${System.currentTimeMillis()}.png")
                FileOutputStream(file).use { output ->
                    input.copyTo(output)
                }
                repository.addUserImage(sceneId, file.absolutePath)
            }
        }
    }
}
