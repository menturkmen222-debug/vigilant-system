package com.whiteboard.animator.ui.screens.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.whiteboard.animator.R
import com.whiteboard.animator.data.model.Scene
import com.whiteboard.animator.data.model.SceneWithAssets
import com.whiteboard.animator.ui.components.PreviewSurface

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
    projectId: Long,
    onNavigateBack: () -> Unit,
    onExport: () -> Unit,
    onOpenAssetLibrary: (Long) -> Unit,
    onOpenCharacterManager: () -> Unit,
    viewModel: EditorViewModel = hiltViewModel()
) {
    LaunchedEffect(projectId) {
        viewModel.loadProject(projectId)
    }

    val projectData by viewModel.projectData.collectAsStateWithLifecycle()
    val selectedSceneId by viewModel.selectedSceneId.collectAsStateWithLifecycle()
    val isPlaying by viewModel.isPlaying.collectAsStateWithLifecycle()
    val playbackProgress by viewModel.playbackProgress.collectAsStateWithLifecycle()
    
    // Drawing and Dialog States
    var showDrawingDialog by remember { mutableStateOf(false) }
    var showEditTextDialog by remember { mutableStateOf(false) }
    var showVoiceoverDialog by remember { mutableStateOf(false) }

    if (showDrawingDialog) {
        DrawingDialog(
            onDismiss = { showDrawingDialog = false },
            onSave = { paths, color, width ->
                selectedSceneId?.let { id ->
                    viewModel.saveDrawing(id, paths, color.toArgb(), width)
                }
            }
        )
    }
    
    if (showEditTextDialog && selectedSceneId != null) {
        val currentText = projectData?.scenes?.find { it.scene.id == selectedSceneId }?.scene?.text ?: ""
        EditTextDialog(
            initialText = currentText,
            onDismiss = { showEditTextDialog = false },
            onConfirm = { newText ->
                selectedSceneId?.let { id -> viewModel.updateSceneText(id, newText) }
                showEditTextDialog = false
            }
        )
    }

    if (showVoiceoverDialog) {
        VoiceoverDialog(
            onDismiss = { showVoiceoverDialog = false },
            onGenerate = {
                selectedSceneId?.let { id -> 
                    val text = projectData?.scenes?.find { it.scene.id == id }?.scene?.text ?: ""
                    viewModel.generateVoiceover(id, text) 
                }
                showVoiceoverDialog = false
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Column {
                        Text(
                            text = projectData?.project?.name ?: stringResource(R.string.editor_title),
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.cd_back))
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.togglePlayback() }) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isPlaying) stringResource(R.string.preview_pause) else stringResource(R.string.preview_play)
                        )
                    }
                    IconButton(onClick = onOpenCharacterManager) {
                        Icon(Icons.Default.Person, contentDescription = "Characters")
                    }
                    Button(
                        onClick = onExport,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    ) {
                        Text(stringResource(R.string.action_export))
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Preview Area (16:9 Aspect Ratio)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .background(Color.White) // Whiteboard background
                    .border(1.dp, Color.LightGray),
                contentAlignment = Alignment.Center
            ) {
                if (selectedSceneId != null && projectData != null) {
                    val sceneItem = projectData?.scenes?.find { it.scene.id == selectedSceneId }
                    if (sceneItem != null) {
                        PreviewSurface(
                            currentScene = sceneItem.scene,
                            assets = sceneItem.assets,
                            drawings = sceneItem.drawings, // Pass drawing paths
                            bitmaps = viewModel.getParams(), // Access bitmaps from ViewModel
                            progress = if (isPlaying) playbackProgress else 1.0f // Show full scene if not playing
                        )
                    }
                } else {
                    Text("Select a scene to preview")
                }
            }
            
            // Progress Bar
            LinearProgressIndicator(
                progress = { playbackProgress },
                modifier = Modifier.fillMaxWidth(),
            )
            
            Divider()

            // Timeline
            Text(
                text = "Timeline",
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(start = 16.dp, top = 8.dp)
            )
            
            LazyRow(
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(0.3f)
            ) {
                itemsIndexed(projectData?.scenes ?: emptyList()) { index, sceneItem ->
                    SceneTimelineItem(
                        scene = sceneItem.scene,
                        index = index,
                        isSelected = sceneItem.scene.id == selectedSceneId,
                        onClick = { viewModel.selectScene(sceneItem.scene.id) }
                    )
                }
                
                // Add Scene Button
                item {
                    OutlinedButton(
                        onClick = { /* Add Scene Logic */ },
                        modifier = Modifier
                            .height(100.dp)
                            .width(80.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                    }
                }
            }
            
            Divider()
            
            // Selected Scene Tools
            if (selectedSceneId != null) {
                val selectedScene = projectData?.scenes?.find { it.scene.id == selectedSceneId }
                
                Column(
                    modifier = Modifier
                        .weight(0.4f)
                        .padding(16.dp)
                        .fillMaxWidth()
                ) {
                    Text(
                        text = "Scene ${selectedScene?.scene?.orderIndex?.plus(1) ?: 0} Settings",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = selectedScene?.scene?.text ?: "",
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        ActionButton(
                            icon = Icons.Default.Image,
                            label = stringResource(R.string.editor_assets),
                            onClick = { selectedSceneId?.let { onOpenAssetLibrary(it) } }
                        )
                        ActionButton(
                            icon = Icons.Default.Brush,
                            label = "Draw",
                            onClick = { showDrawingDialog = true }
                        )
                        ActionButton(
                            icon = Icons.Default.Edit,
                            label = stringResource(R.string.action_edit),
                            onClick = { showEditTextDialog = true }
                        )
                        ActionButton(
                            icon = Icons.Default.Mic,
                            label = stringResource(R.string.editor_voiceover),
                            onClick = { showVoiceoverDialog = true }
                        )
                        ActionButton(
                            icon = Icons.Default.Delete,
                            label = stringResource(R.string.action_delete),
                            onClick = { selectedSceneId?.let { viewModel.deleteScene(it) } },
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SceneTimelineItem(
    scene: Scene,
    index: Int,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .width(140.dp)
            .height(100.dp)
            .border(
                width = if (isSelected) 2.dp else 0.dp,
                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                shape = RoundedCornerShape(12.dp)
            ),
        elevation = CardDefaults.cardElevation(if (isSelected) 4.dp else 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.size(20.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "${index + 1}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
                Text(
                    text = "${scene.durationMs / 1000}s",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = scene.text,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun ActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
    color: Color = MaterialTheme.colorScheme.primary
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier
                .size(40.dp)
                .background(
                    color = color.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(8.dp)
                )
                .padding(8.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            overflow = TextOverflow.Ellipsis,
            maxLines = 1
        )
    }
}
