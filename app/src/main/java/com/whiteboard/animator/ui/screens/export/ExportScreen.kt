package com.whiteboard.animator.ui.screens.export

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.whiteboard.animator.R
import com.whiteboard.animator.data.model.ExportStatus
import com.whiteboard.animator.data.model.Resolution

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportScreen(
    projectId: Long,
    onNavigateBack: () -> Unit,
    onExportComplete: () -> Unit,
    viewModel: ExportViewModel = hiltViewModel()
) {
    LaunchedEffect(projectId) {
        viewModel.loadProject(projectId)
    }

    val project by viewModel.project.collectAsStateWithLifecycle()
    val isExporting by viewModel.isExporting.collectAsStateWithLifecycle()
    val progress by viewModel.exportProgress.collectAsStateWithLifecycle()
    val status by viewModel.exportStatus.collectAsStateWithLifecycle()
    val selectedResolution by viewModel.selectedResolution.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.export_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack, enabled = !isExporting) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.cd_back))
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            
            project?.let {
                Text(it.name, style = MaterialTheme.typography.headlineSmall)
                Spacer(modifier = Modifier.height(24.dp))
            }

            if (isExporting || status == ExportStatus.COMPLETE) {
                // Progress UI
                ExportProgressContent(
                    status = status,
                    progress = progress,
                    onDone = onExportComplete
                )
            } else {
                // Settings UI
                Text(stringResource(R.string.export_resolution), style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ResolutionChip(
                        resolution = Resolution.SD_480P,
                        selected = selectedResolution == Resolution.SD_480P,
                        onClick = { viewModel.setResolution(Resolution.SD_480P) }
                    )
                    ResolutionChip(
                        resolution = Resolution.HD_720P,
                        selected = selectedResolution == Resolution.HD_720P,
                        onClick = { viewModel.setResolution(Resolution.HD_720P) }
                    )
                    ResolutionChip(
                        resolution = Resolution.FHD_1080P,
                        selected = selectedResolution == Resolution.FHD_1080P,
                        onClick = { viewModel.setResolution(Resolution.FHD_1080P) }
                    )
                }
                
                Spacer(modifier = Modifier.height(32.dp))
                
                Button(
                    onClick = { 
                        viewModel.startExport { 
                            // Optionally handle path
                        } 
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp)
                ) {
                    Text(stringResource(R.string.export_start))
                }
            }
        }
    }
}

@Composable
fun ResolutionChip(
    resolution: Resolution,
    selected: Boolean,
    onClick: () -> Unit
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { 
            val text = when(resolution) {
                Resolution.SD_480P -> "480p"
                Resolution.HD_720P -> "720p"
                Resolution.FHD_1080P -> "1080p"
            }
            Text(text)
        }
    )
}

@Composable
fun ExportProgressContent(
    status: ExportStatus,
    progress: Float,
    onDone: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxWidth()
    ) {
        if (status == ExportStatus.COMPLETE) {
            Text(stringResource(R.string.export_success), style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = onDone) {
                Text(stringResource(R.string.action_done))
            }
        } else {
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth().height(8.dp),
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text("${(progress * 100).toInt()}%", style = MaterialTheme.typography.headlineMedium)
            Spacer(modifier = Modifier.height(8.dp))
            Text("Status: $status", style = MaterialTheme.typography.bodyMedium)
        }
    }
}
