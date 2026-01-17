package com.whiteboard.animator.ui.screens.editor

import android.view.MotionEvent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.whiteboard.animator.data.model.DrawingPath
import com.whiteboard.animator.data.model.PathPoint

@OptIn(ExperimentalComposeUiApi::class, ExperimentalMaterial3Api::class)
@Composable
fun DrawingDialog(
    onDismiss: () -> Unit,
    onSave: (List<List<PathPoint>>, Color, Float) -> Unit
) {
    var rawPaths by remember { mutableStateOf(listOf<List<PathPoint>>()) }
    var currentPath by remember { mutableStateOf(listOf<PathPoint>()) }
    
    // Convert to Compose Path for rendering
    fun List<PathPoint>.toPath(): Path {
        val path = Path()
        if (isNotEmpty()) {
            path.moveTo(first().x, first().y)
            for (i in 1 until size) {
                path.lineTo(get(i).x, get(i).y)
            }
        }
        return path
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false) // Full screen
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Draw") },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Cancel")
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = {
                                if (rawPaths.isNotEmpty()) {
                                    rawPaths = rawPaths.dropLast(1)
                                }
                            },
                            enabled = rawPaths.isNotEmpty()
                        ) {
                            Icon(Icons.Default.Undo, contentDescription = "Undo")
                        }
                        IconButton(onClick = { 
                            onSave(rawPaths, Color.Black, 5f)
                            onDismiss()
                        }) {
                            Icon(Icons.Default.Check, contentDescription = "Save")
                        }
                    }
                )
            }
        ) { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .background(Color.White)
            ) {
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInteropFilter { event ->
                            val x = event.x
                            val y = event.y
                            
                            when (event.action) {
                                MotionEvent.ACTION_DOWN -> {
                                    currentPath = listOf(PathPoint(x, y))
                                    true
                                }
                                MotionEvent.ACTION_MOVE -> {
                                    currentPath = currentPath + PathPoint(x, y)
                                    true
                                }
                                MotionEvent.ACTION_UP -> {
                                    if (currentPath.isNotEmpty()) {
                                        rawPaths = rawPaths + listOf(currentPath)
                                        currentPath = emptyList()
                                    }
                                    true
                                }
                                else -> false
                            }
                        }
                ) {
                    // Draw saved paths
                    rawPaths.forEach { points ->
                        drawPath(
                            path = points.toPath(),
                            color = Color.Black,
                            style = Stroke(width = 5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                        )
                    }
                    
                    // Draw current path
                    if (currentPath.isNotEmpty()) {
                        drawPath(
                            path = currentPath.toPath(),
                            color = Color.Black,
                            style = Stroke(width = 5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                        )
                    }
                }
            }
        }
    }
}
