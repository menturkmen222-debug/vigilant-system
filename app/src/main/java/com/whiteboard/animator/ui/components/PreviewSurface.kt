package com.whiteboard.animator.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import com.whiteboard.animator.data.model.Scene
import com.whiteboard.animator.data.model.SceneAsset
import com.whiteboard.animator.data.model.SceneBitmaps
import com.whiteboard.animator.data.model.DrawingPath
import com.whiteboard.animator.engine.AnimationEngine

/**
 * Surface for rendering animation previews.
 * Bridges Jetpack Compose Canvas with Android generic Canvas used by AnimationEngine.
 */
@Composable
fun PreviewSurface(
    currentScene: Scene?,
    assets: List<SceneAsset>,
    drawings: List<DrawingPath> = emptyList(), // Added parameter
    bitmaps: SceneBitmaps,
    progress: Float,
    modifier: Modifier = Modifier
) {
    if (currentScene == null) return

    val engine = remember { AnimationEngine() }

    Canvas(modifier = modifier.fillMaxSize()) {
        drawIntoCanvas { canvas ->
            val nativeCanvas = canvas.nativeCanvas
            
            // Render using the shared engine logic
            engine.renderFrame(
                canvas = nativeCanvas,
                scene = currentScene,
                assets = assets,
                drawingPaths = drawings, // Pass drawings
                handBitmap = bitmaps.sceneHandBitmaps[currentScene.projectId],
                backgroundBitmap = bitmaps.sceneBackgroundBitmaps[currentScene.projectId],
                assetBitmaps = bitmaps.assetBitmaps,
                progress = progress,
                width = size.width.toInt(),
                height = size.height.toInt()
            )
        }
    }
}
