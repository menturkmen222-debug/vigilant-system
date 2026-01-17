package com.whiteboard.animator.engine

import android.graphics.*
import android.graphics.drawable.Drawable
import androidx.compose.ui.geometry.Offset
import com.whiteboard.animator.data.model.*
import kotlinx.serialization.json.Json
import kotlin.math.min

/**
 * Animation Engine for rendering whiteboard-style animations.
 * 
 * Handles:
 * - Hand-drawing text reveal effect
 * - Asset animations (fade, zoom, slide, etc.)
 * - Scene transitions
 * - Subtitle rendering
 * 
 * All rendering is done to Canvas for both preview and video export.
 */
class AnimationEngine {
    
    // Reusable Paint objects for performance
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.LEFT
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
    }
    
    private val subtitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }
    
    private val subtitleBgPaint = Paint().apply {
        style = Paint.Style.FILL
    }
    
    private val assetPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    
    /**
     * Render a single frame for a scene.
     * 
     * @param canvas Target canvas to draw on
     * @param scene The scene to render
     * @param assets Assets in this scene
     * @param handBitmap Optional hand bitmap for drawing effect
     * @param backgroundBitmap Optional background bitmap
     * @param assetBitmaps Map of asset ID to loaded bitmap
     * @param progress Animation progress (0.0 to 1.0)
     * @param width Canvas width
     * @param height Canvas height
     */
    fun renderFrame(
        canvas: Canvas,
        scene: Scene,
        assets: List<SceneAsset>,
        drawingPaths: List<DrawingPath> = emptyList(), // Added drawing paths
        handBitmap: Bitmap? = null,
        backgroundBitmap: Bitmap? = null,
        assetBitmaps: Map<Long, Bitmap> = emptyMap(),
        progress: Float,
        width: Int,
        height: Int
    ) {
        // 1. Draw background
        drawBackground(canvas, scene, backgroundBitmap, width, height)
        
        // 2. Draw assets with staggered animations
        drawAssets(canvas, assets, assetBitmaps, progress, width, height)

        // 2.5 Draw freehand paths
        drawPaths(canvas, drawingPaths, progress, width, height)
        
        // 3. Draw text with hand-drawing effect
        if (scene.text.isNotBlank()) {
            drawTextWithHandEffect(
                canvas = canvas,
                text = scene.text,
                handBitmap = handBitmap,
                progress = progress,
                width = width,
                height = height,
                showHand = scene.handStyle != HandStyle.NONE,
                animationStyle = scene.textAnimationStyle
            )
        }
        
        // 4. Draw subtitles if enabled
        if (scene.showSubtitles && scene.text.isNotBlank()) {
            drawSubtitle(canvas, scene.text, progress, width, height)
        }
    }
    
    /**
     * Render a transition between two scenes.
     * 
     * @param canvas Target canvas
     * @param scene1Bitmap Rendered frame of outgoing scene
     * @param scene2Bitmap Rendered frame of incoming scene
     * @param transition Transition style
     * @param progress Transition progress (0.0 = scene1, 1.0 = scene2)
     * @param width Canvas width
     * @param height Canvas height
     */
    fun renderTransition(
        canvas: Canvas,
        scene1Bitmap: Bitmap?,
        scene2Bitmap: Bitmap?,
        transition: TransitionStyle,
        progress: Float,
        width: Int,
        height: Int
    ) {
        when (transition) {
            TransitionStyle.NONE -> {
                // Instant cut - show scene2 when progress >= 0.5
                val bitmap = if (progress < 0.5f) scene1Bitmap else scene2Bitmap
                bitmap?.let { canvas.drawBitmap(it, 0f, 0f, null) }
            }
            
            TransitionStyle.FADE -> {
                // Crossfade
                scene1Bitmap?.let {
                    assetPaint.alpha = ((1f - progress) * 255).toInt()
                    canvas.drawBitmap(it, 0f, 0f, assetPaint)
                }
                scene2Bitmap?.let {
                    assetPaint.alpha = (progress * 255).toInt()
                    canvas.drawBitmap(it, 0f, 0f, assetPaint)
                }
                assetPaint.alpha = 255
            }
            
            TransitionStyle.SLIDE -> {
                // Slide left
                scene1Bitmap?.let {
                    val offsetX = -width * progress
                    canvas.drawBitmap(it, offsetX, 0f, null)
                }
                scene2Bitmap?.let {
                    val offsetX = width * (1f - progress)
                    canvas.drawBitmap(it, offsetX, 0f, null)
                }
            }
            
            TransitionStyle.ZOOM -> {
                canvas.save()
                val centerX = width / 2f
                val centerY = height / 2f
                
                // Zoom out scene1
                scene1Bitmap?.let {
                    val scale = 1f - progress * 0.3f
                    assetPaint.alpha = ((1f - progress) * 255).toInt()
                    canvas.translate(centerX, centerY)
                    canvas.scale(scale, scale)
                    canvas.translate(-centerX, -centerY)
                    canvas.drawBitmap(it, 0f, 0f, assetPaint)
                }
                canvas.restore()
                
                canvas.save()
                // Zoom in scene2
                scene2Bitmap?.let {
                    val scale = 0.7f + progress * 0.3f
                    assetPaint.alpha = (progress * 255).toInt()
                    canvas.translate(centerX, centerY)
                    canvas.scale(scale, scale)
                    canvas.translate(-centerX, -centerY)
                    canvas.drawBitmap(it, 0f, 0f, assetPaint)
                }
                canvas.restore()
                assetPaint.alpha = 255
            }
            
            TransitionStyle.WIPE -> {
                // Wipe from left to right
                scene1Bitmap?.let { canvas.drawBitmap(it, 0f, 0f, null) }
                scene2Bitmap?.let {
                    val clipWidth = (width * progress).toInt()
                    canvas.save()
                    canvas.clipRect(0, 0, clipWidth, height)
                    canvas.drawBitmap(it, 0f, 0f, null)
                    canvas.restore()
                }
            }
        }
    }
    
    // ========================================================================
    // PRIVATE DRAWING METHODS
    // ========================================================================
    
    /**
     * Draw the scene background.
     */
    private fun drawBackground(
        canvas: Canvas,
        scene: Scene,
        backgroundBitmap: Bitmap?,
        width: Int,
        height: Int
    ) {
        // Fill with background color or white
        val bgColor = scene.backgroundColor ?: Color.WHITE
        canvas.drawColor(bgColor)
        
        // Draw background image if available
        backgroundBitmap?.let { bg ->
            val srcRect = Rect(0, 0, bg.width, bg.height)
            val dstRect = Rect(0, 0, width, height)
            canvas.drawBitmap(bg, srcRect, dstRect, null)
        }
    }
    
    /**
     * Draw assets with staggered animations.
     */
    private fun drawAssets(
        canvas: Canvas,
        assets: List<SceneAsset>,
        assetBitmaps: Map<Long, Bitmap>,
        totalProgress: Float,
        canvasWidth: Int,
        canvasHeight: Int
    ) {
        assets.forEachIndexed { index, asset ->
            if (!asset.isVisible) return@forEachIndexed
            
            val bitmap = assetBitmaps[asset.id] ?: return@forEachIndexed
            
            // Calculate staggered progress for this asset
            val assetProgress = calculateAssetProgress(totalProgress, index, assets.size)
            
            if (assetProgress <= 0f) return@forEachIndexed  // Not yet visible
            
            drawAnimatedAsset(
                canvas = canvas,
                bitmap = bitmap,
                asset = asset,
                progress = assetProgress.coerceAtMost(1f),
                canvasWidth = canvasWidth,
                canvasHeight = canvasHeight
            )
        }
    }
    
    /**
     * Draw a single asset with animation.
     */
    private fun drawAnimatedAsset(
        canvas: Canvas,
        bitmap: Bitmap,
        asset: SceneAsset,
        progress: Float,
        canvasWidth: Int,
        canvasHeight: Int
    ) {
        canvas.save()
        
        // Calculate scaled dimensions
        val assetWidth = bitmap.width * asset.scale
        val assetHeight = bitmap.height * asset.scale
        
        // Get position based on placement
        val position = calculatePlacement(
            asset.placement,
            asset.customX,
            asset.customY,
            assetWidth,
            assetHeight,
            canvasWidth.toFloat(),
            canvasHeight.toFloat()
        )
        
        // Apply animation transformations
        assetPaint.alpha = (255 * asset.opacity).toInt()
        
        when (asset.animationStyle) {
            AnimationStyle.FADE_IN -> {
                assetPaint.alpha = (255 * progress * asset.opacity).toInt()
            }
            
            AnimationStyle.FADE_OUT -> {
                assetPaint.alpha = (255 * (1f - progress) * asset.opacity).toInt()
            }
            
            AnimationStyle.ZOOM_IN -> {
                val scale = progress * asset.scale
                val centerX = position.x + assetWidth / 2
                val centerY = position.y + assetHeight / 2
                canvas.translate(centerX, centerY)
                canvas.scale(scale / asset.scale, scale / asset.scale)
                canvas.translate(-centerX, -centerY)
            }
            
            AnimationStyle.ZOOM_OUT -> {
                val scale = (1f - progress * 0.5f) * asset.scale
                val centerX = position.x + assetWidth / 2
                val centerY = position.y + assetHeight / 2
                canvas.translate(centerX, centerY)
                canvas.scale(scale / asset.scale, scale / asset.scale)
                canvas.translate(-centerX, -centerY)
            }
            
            AnimationStyle.SLIDE_LEFT -> {
                val offsetX = (1f - progress) * canvasWidth
                canvas.translate(-offsetX, 0f)
            }
            
            AnimationStyle.SLIDE_RIGHT -> {
                val offsetX = (1f - progress) * canvasWidth
                canvas.translate(offsetX, 0f)
            }
            
            AnimationStyle.SLIDE_UP -> {
                val offsetY = (1f - progress) * canvasHeight
                canvas.translate(0f, -offsetY)
            }
            
            AnimationStyle.SLIDE_DOWN -> {
                val offsetY = (1f - progress) * canvasHeight
                canvas.translate(0f, offsetY)
            }
            
            AnimationStyle.POP -> {
                val scale = if (progress < 0.5f) {
                    progress * 2.4f  // Overshoot
                } else {
                    1.2f - (progress - 0.5f) * 0.4f  // Settle back
                } * asset.scale
                val centerX = position.x + assetWidth / 2
                val centerY = position.y + assetHeight / 2
                canvas.translate(centerX, centerY)
                canvas.scale(scale / asset.scale, scale / asset.scale)
                canvas.translate(-centerX, -centerY)
            }
            
            AnimationStyle.HAND_DRAW, AnimationStyle.NONE -> {
                // No transformation, just fade in slightly for HAND_DRAW
                if (asset.animationStyle == AnimationStyle.HAND_DRAW) {
                    assetPaint.alpha = (255 * progress.coerceAtMost(1f) * asset.opacity).toInt()
                }
            }
        }
        
        // Apply rotation
        if (asset.rotation != 0f) {
            val centerX = position.x + assetWidth / 2
            val centerY = position.y + assetHeight / 2
            canvas.rotate(asset.rotation, centerX, centerY)
        }
        
        // Draw the bitmap
        val dstRect = RectF(
            position.x,
            position.y,
            position.x + assetWidth,
            position.y + assetHeight
        )
        canvas.drawBitmap(bitmap, null, dstRect, assetPaint)
        
        canvas.restore()
        assetPaint.alpha = 255
    }
    
    /**
     * Draw text with hand-drawing reveal effect.
     */
    private fun drawTextWithHandEffect(
        canvas: Canvas,
        text: String,
        handBitmap: Bitmap?,
        progress: Float,
        width: Int,
        height: Int,
        showHand: Boolean,
        animationStyle: AnimationStyle
    ) {
        // Text settings
        val textSize = height * 0.045f
        textPaint.textSize = textSize
        textPaint.color = Color.DKGRAY
        
        // Calculate text bounds
        val maxWidth = width * 0.85f
        val lines = wrapText(text, textPaint, maxWidth)
        val lineHeight = textSize * 1.4f
        val totalTextHeight = lines.size * lineHeight
        
        // Position text in upper-middle area
        val startY = height * 0.2f + (height * 0.4f - totalTextHeight) / 2
        val startX = (width - maxWidth) / 2
        
        // Calculate visible characters based on progress
        val totalChars = lines.sumOf { it.length }
        val visibleChars = (totalChars * progress).toInt()
        
        var charsDrawn = 0
        var lastCharX = startX
        var lastCharY = startY
        
        for ((lineIndex, line) in lines.withIndex()) {
            val lineY = startY + lineIndex * lineHeight
            
            if (animationStyle == AnimationStyle.HAND_DRAW) {
                // Draw character by character for hand-draw effect
                val lineVisibleChars = (visibleChars - charsDrawn).coerceIn(0, line.length)
                val visibleText = line.take(lineVisibleChars)
                
                if (visibleText.isNotEmpty()) {
                    canvas.drawText(visibleText, startX, lineY, textPaint)
                    
                    // Track position for hand
                    if (lineVisibleChars > 0 && lineVisibleChars <= line.length) {
                        lastCharX = startX + textPaint.measureText(visibleText)
                        lastCharY = lineY
                    }
                }
                
                charsDrawn += line.length
            } else {
                // Fade in whole text
                textPaint.alpha = (255 * progress).toInt()
                canvas.drawText(line, startX, lineY, textPaint)
                textPaint.alpha = 255
            }
        }
        
        // Draw hand at writing position
        if (showHand && handBitmap != null && progress < 1f && animationStyle == AnimationStyle.HAND_DRAW) {
            val handScale = height * 0.15f / handBitmap.height
            val handWidth = handBitmap.width * handScale
            val handHeight = handBitmap.height * handScale
            
            // Position hand at end of text
            val handX = lastCharX - handWidth * 0.2f
            val handY = lastCharY - handHeight * 0.5f
            
            val dstRect = RectF(handX, handY, handX + handWidth, handY + handHeight)
            canvas.drawBitmap(handBitmap, null, dstRect, null)
        }
    }
    
    // Cache for parsed paths to avoid JSON decoding on every frame
    private val pathCache = mutableMapOf<Long, Path>()
    
    /**
     * Draw user-drawn paths with progressive stroke animation.
     */
    private fun drawPaths(
        canvas: Canvas,
        paths: List<DrawingPath>,
        progress: Float,
        width: Int,
        height: Int
    ) {
        if (paths.isEmpty()) return
        
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }

        // Use PathMeasure to animate the stroke
        val pathMeasure = PathMeasure()
        
        paths.forEach { drawingPath ->
            paint.color = drawingPath.strokeColor
            paint.strokeWidth = drawingPath.strokeWidth

            try {
                // Get or parse path
                val fullPath = pathCache.getOrPut(drawingPath.id) {
                    val points = kotlinx.serialization.json.Json.decodeFromString<List<PathPoint>>(drawingPath.pathData)
                    Path().apply {
                        if (points.isNotEmpty()) {
                            moveTo(points[0].x, points[0].y)
                            for (i in 1 until points.size) {
                                lineTo(points[i].x, points[i].y)
                            }
                        }
                    }
                }
                
                // Animate path drawing
                pathMeasure.setPath(fullPath, false)
                val totalLength = pathMeasure.length
                
                // Animate from 0 to 100% of the path length based on scene progress
                // We create a drawing effect where the path traces itself
                if (progress > 0) {
                    val drawLength = totalLength * progress.coerceIn(0f, 1f)
                    val segmentPath = Path()
                    // getSegment is true for startWithMoveTo
                    if (pathMeasure.getSegment(0f, drawLength, segmentPath, true)) {
                        canvas.drawPath(segmentPath, paint)
                    }
                }
            } catch (e: Exception) {
                // Fallback if parsing fails
            }
        }
    }

    /**
     * Draw subtitle bar at bottom of screen.
     */
    private fun drawSubtitle(
        canvas: Canvas,
        text: String,
        progress: Float,
        width: Int,
        height: Int
    ) {
        // Don't show subtitle until text is mostly visible
        if (progress < 0.3f) return
        
        val subtitleAlpha = ((progress - 0.3f) / 0.2f).coerceIn(0f, 1f)
        
        // Background bar
        val bgHeight = height * 0.08f
        val bgTop = height - bgHeight
        subtitleBgPaint.color = Color.argb((180 * subtitleAlpha).toInt(), 0, 0, 0)
        canvas.drawRect(0f, bgTop, width.toFloat(), height.toFloat(), subtitleBgPaint)
        
        // Subtitle text
        subtitlePaint.textSize = height * 0.035f
        subtitlePaint.color = Color.argb((255 * subtitleAlpha).toInt(), 255, 255, 255)
        
        // Truncate long text
        val maxChars = 80
        val displayText = if (text.length > maxChars) text.take(maxChars - 3) + "..." else text
        
        val textY = bgTop + bgHeight * 0.65f
        canvas.drawText(displayText, width / 2f, textY, subtitlePaint)
    }
    
    // ========================================================================
    // HELPER METHODS
    // ========================================================================
    
    /**
     * Calculate staggered progress for an asset.
     * Assets appear sequentially rather than all at once.
     */
    private fun calculateAssetProgress(
        totalProgress: Float,
        assetIndex: Int,
        totalAssets: Int
    ): Float {
        if (totalAssets == 0) return 0f
        
        // Each asset gets a portion of the timeline
        val assetDuration = 0.8f / totalAssets
        val assetStart = 0.1f + (assetIndex * assetDuration * 0.7f)
        val assetEnd = assetStart + assetDuration
        
        return ((totalProgress - assetStart) / (assetEnd - assetStart))
    }
    
    /**
     * Calculate asset position based on placement setting.
     */
    private fun calculatePlacement(
        placement: ImagePlacement,
        customX: Float,
        customY: Float,
        assetWidth: Float,
        assetHeight: Float,
        canvasWidth: Float,
        canvasHeight: Float
    ): PointF {
        val padding = min(canvasWidth, canvasHeight) * 0.05f
        
        return when (placement) {
            ImagePlacement.CENTER -> PointF(
                (canvasWidth - assetWidth) / 2,
                (canvasHeight - assetHeight) / 2
            )
            ImagePlacement.TOP_LEFT -> PointF(padding, padding)
            ImagePlacement.TOP_CENTER -> PointF((canvasWidth - assetWidth) / 2, padding)
            ImagePlacement.TOP_RIGHT -> PointF(canvasWidth - assetWidth - padding, padding)
            ImagePlacement.LEFT_CENTER -> PointF(padding, (canvasHeight - assetHeight) / 2)
            ImagePlacement.RIGHT_CENTER -> PointF(
                canvasWidth - assetWidth - padding,
                (canvasHeight - assetHeight) / 2
            )
            ImagePlacement.BOTTOM_LEFT -> PointF(padding, canvasHeight - assetHeight - padding)
            ImagePlacement.BOTTOM_CENTER -> PointF(
                (canvasWidth - assetWidth) / 2,
                canvasHeight - assetHeight - padding
            )
            ImagePlacement.BOTTOM_RIGHT -> PointF(
                canvasWidth - assetWidth - padding,
                canvasHeight - assetHeight - padding
            )
            ImagePlacement.CUSTOM -> PointF(customX, customY)
        }
    }
    
    /**
     * Wrap text to fit within a maximum width.
     */
    private fun wrapText(text: String, paint: Paint, maxWidth: Float): List<String> {
        val words = text.split(" ")
        val lines = mutableListOf<String>()
        var currentLine = StringBuilder()
        
        for (word in words) {
            val testLine = if (currentLine.isEmpty()) word else "$currentLine $word"
            val testWidth = paint.measureText(testLine)
            
            if (testWidth > maxWidth && currentLine.isNotEmpty()) {
                lines.add(currentLine.toString())
                currentLine = StringBuilder(word)
            } else {
                currentLine = StringBuilder(testLine)
            }
        }
        
        if (currentLine.isNotEmpty()) {
            lines.add(currentLine.toString())
        }
        
        return lines
    }
}
