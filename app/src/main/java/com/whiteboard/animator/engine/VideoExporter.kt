package com.whiteboard.animator.engine

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.MediaMuxer
import android.util.Log
import com.whiteboard.animator.data.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import java.io.FileInputStream
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield
import java.io.File
import java.nio.ByteBuffer
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Video Exporter for rendering and encoding whiteboard animation videos.
 * 
 * Uses:
 * - AnimationEngine for frame rendering
 * - MediaCodec for H.264 encoding
 * - MediaMuxer for MP4 container
 * 
 * Supports background processing with progress tracking.
 */
@Singleton
class VideoExporter @Inject constructor(
    private val context: Context
) {
    private val animationEngine = AnimationEngine()
    
    // Progress tracking
    private val _progress = MutableStateFlow(0f)
    val progress: StateFlow<Float> = _progress.asStateFlow()
    
    private val _status = MutableStateFlow(ExportStatus.IDLE)
    val status: StateFlow<ExportStatus> = _status.asStateFlow()
    
    companion object {
        private const val TAG = "VideoExporter"
        private const val TIMEOUT_US = 10000L
    }
    
    /**
     * Export a project to video file.
     * 
     * @param scenes List of scenes to render
     * @param sceneAssets Map of scene ID to list of assets
     * @param bitmaps Pre-loaded bitmaps for assets, backgrounds, hands
     * @param outputPath Path for output video file
     * @param resolution Video resolution
     * @param aspectRatio Video aspect ratio
     * @param fps Frames per second (12-30)
     * @param onProgress Progress callback (0.0 to 1.0)
     * @return Result with output path or error
     */
    suspend fun exportVideo(
        scenes: List<Scene>,
        sceneAssets: Map<Long, List<SceneAsset>>,
        sceneDrawings: Map<Long, List<DrawingPath>> = emptyMap(), // Added parameter
        bitmaps: SceneBitmaps,
        outputPath: String,
        resolution: Resolution = Resolution.HD_720P,
        aspectRatio: AspectRatio = AspectRatio.HORIZONTAL_16_9,
        fps: Int = 24,
        onProgress: (Float) -> Unit = {}
    ): Result<String> = withContext(Dispatchers.Default) {
        _status.value = ExportStatus.PREPARING
        _progress.value = 0f
        
        // Resources to clean up
        var videoEncoder: MediaCodec? = null
        var audioEncoder: MediaCodec? = null
        var muxer: MediaMuxer? = null
        var frameBitmap: Bitmap? = null
        var currentAudioData: ByteArray? = null
        var currentAudioPosition = 0
        
        try {
            val (width, height) = calculateDimensions(resolution, aspectRatio)
            val totalDurationMs = scenes.sumOf { it.durationMs + it.transitionDurationMs }
            val totalFrames = ((totalDurationMs / 1000f) * fps).toInt()
            
            if (totalFrames == 0) return@withContext Result.failure(Exception("No frames"))
            
            // Directories
            File(outputPath).parentFile?.mkdirs()
            
            // 1. Setup Video Encoder
            val videoMime = MediaFormat.MIMETYPE_VIDEO_AVC
            val videoFormat = MediaFormat.createVideoFormat(videoMime, width, height).apply {
                setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible)
                setInteger(MediaFormat.KEY_BIT_RATE, calculateBitRate(width, height, fps))
                setInteger(MediaFormat.KEY_FRAME_RATE, fps)
                setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1)
            }
            videoEncoder = MediaCodec.createEncoderByType(videoMime)
            videoEncoder.configure(videoFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            videoEncoder.start()
            
            // 2. Setup Audio Encoder (AAC)
            val audioMime = MediaFormat.MIMETYPE_AUDIO_AAC
            val sampleRate = 44100 // Target sample rate
            val channelCount = 1
            val audioBitRate = 64000
            val audioFormat = MediaFormat.createAudioFormat(audioMime, sampleRate, channelCount).apply {
                setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC)
                setInteger(MediaFormat.KEY_BIT_RATE, audioBitRate)
                setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 16384)
            }
            audioEncoder = MediaCodec.createEncoderByType(audioMime)
            audioEncoder.configure(audioFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            audioEncoder.start()
            
            // 3. Setup Muxer
            muxer = MediaMuxer(outputPath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            
            var videoTrackIndex = -1
            var audioTrackIndex = -1
            var muxerStarted = false
            
            // Helper to start muxer if ready
            fun checkMuxerStart() {
                if (videoTrackIndex >= 0 && audioTrackIndex >= 0 && !muxerStarted) {
                    muxer!!.start()
                    muxerStarted = true
                    Log.d(TAG, "Muxer started")
                }
            }
            
            // Rendering setup
            frameBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val frameCanvas = Canvas(frameBitmap)
            val bufferInfo = MediaCodec.BufferInfo()
            
            _status.value = ExportStatus.ENCODING_VIDEO
            
            var frameIndex = 0
            var currentSceneIndex = 0
            var sceneStartFrame = 0
            var inTransition = false
            var transitionStartFrame = 0
            
            // Audio state
            var audioTotalSamplesWritten = 0L
            val samplesPerFrame = sampleRate / fps
            val pcmBuffer = ByteArray(samplesPerFrame * 2) // 16-bit = 2 bytes
            
            while (frameIndex < totalFrames) {
                yield()
                if (!isActive) throw Exception("Cancelled")
                
                // --- VIDEO RENDERING ---
                val scene = scenes.getOrNull(currentSceneIndex) ?: break
                val sceneFrameCount = ((scene.durationMs / 1000f) * fps).toInt()
                val transitionFrameCount = ((scene.transitionDurationMs / 1000f) * fps).toInt()
                
                val sceneProgress = if (inTransition) 1f else ((frameIndex - sceneStartFrame).toFloat() / sceneFrameCount).coerceIn(0f, 1f)
                
                frameCanvas.drawColor(android.graphics.Color.WHITE)
                
                if (inTransition && currentSceneIndex < scenes.size - 1) {
                    val transitionProgress = ((frameIndex - transitionStartFrame).toFloat() / transitionFrameCount).coerceIn(0f, 1f)
                    val scene1 = renderSceneToBitmap(scene, sceneAssets[scene.id] ?: emptyList(), bitmaps, 1f, width, height)
                    val scene2 = renderSceneToBitmap(scenes[currentSceneIndex + 1], sceneAssets[scenes[currentSceneIndex + 1].id] ?: emptyList(), bitmaps, 0f, width, height)
                    animationEngine.renderTransition(frameCanvas, scene1, scene2, scene.transitionStyle, transitionProgress, width, height)
                    scene1.recycle()
                    scene2.recycle()
                    
                    if (transitionProgress >= 1f) {
                        currentSceneIndex++
                        sceneStartFrame = frameIndex
                        inTransition = false
                        currentAudioData = null
                    }
                } else {
                    animationEngine.renderFrame(
                        frameCanvas, scene, sceneAssets[scene.id] ?: emptyList(), 
                        sceneDrawings[scene.id] ?: emptyList(), // Pass drawings
                        bitmaps.sceneHandBitmaps[scene.id], 
                        bitmaps.sceneBackgroundBitmaps[scene.id], 
                        bitmaps.assetBitmaps, sceneProgress, width, height
                    )
                    
                    if (sceneProgress >= 1f && !inTransition) {
                        if (transitionFrameCount > 0 && currentSceneIndex < scenes.size - 1) {
                            inTransition = true
                            transitionStartFrame = frameIndex
                        } else {
                            currentSceneIndex++
                            sceneStartFrame = frameIndex
                            currentAudioData = null
                        }
                    }
                }
                
                // Encode Video Frame
                encodeFrame(videoEncoder, muxer!!, frameBitmap, frameIndex, bufferInfo, fps) { idx ->
                    videoTrackIndex = idx
                    checkMuxerStart()
                    videoTrackIndex
                }
                
                // --- AUDIO ENCODING ---
                // We play audio for the current scene.
                if (!inTransition && scene.voiceoverPath != null && currentAudioData == null) {
                     currentAudioData = loadAndResample(scene.voiceoverPath, sampleRate)
                     currentAudioPosition = 0
                }
                
                // Read PCM chunk
                var bytesCopied = 0
                if (currentAudioData != null) {
                    val remaining = currentAudioData!!.size - currentAudioPosition
                    val toCopy = minOf(pcmBuffer.size, remaining)
                    if (toCopy > 0) {
                        System.arraycopy(currentAudioData!!, currentAudioPosition, pcmBuffer, 0, toCopy)
                        currentAudioPosition += toCopy
                        bytesCopied = toCopy
                    }
                }
                
                // Pad with silence if EOF or error
                if (bytesCopied < pcmBuffer.size) {
                    for (i in bytesCopied until pcmBuffer.size) pcmBuffer[i] = 0
                }
                
                // Encode Audio Chunk
                val inputIndex = audioEncoder.dequeueInputBuffer(TIMEOUT_US)
                if (inputIndex >= 0) {
                    val buf = audioEncoder.getInputBuffer(inputIndex)
                    buf?.clear()
                    buf?.put(pcmBuffer)
                    val pts = (audioTotalSamplesWritten * 1_000_000L / sampleRate)
                    audioEncoder.queueInputBuffer(inputIndex, 0, pcmBuffer.size, pts, 0)
                    audioTotalSamplesWritten += samplesPerFrame
                }
                
                // Drain Audio
                drainEncoderOutput(audioEncoder, muxer!!, bufferInfo) { idx ->
                    audioTrackIndex = idx
                    checkMuxerStart()
                    audioTrackIndex
                }
                
                // Progress
                val prog = frameIndex.toFloat() / totalFrames
                _progress.value = prog
                onProgress(prog)
                frameIndex++
            }
            
            // Finish
            drainEncoder(videoEncoder, muxer!!, bufferInfo) { videoTrackIndex }
            drainEncoder(audioEncoder, muxer!!, bufferInfo) { audioTrackIndex }
            
            _status.value = ExportStatus.COMPLETE
            _progress.value = 1f
            onProgress(1f)
            Log.d(TAG, "Export Success: $outputPath")
            Result.success(outputPath)
            
        } catch (e: Exception) {
            Log.e(TAG, "Export error", e)
            _status.value = ExportStatus.ERROR
            Result.failure(e)
        } finally {
            try {
                videoEncoder?.stop(); videoEncoder?.release()
                audioEncoder?.stop(); audioEncoder?.release()
                if (muxerStarted) { muxer?.stop() }
                muxer?.release()
                frameBitmap?.recycle()
            } catch (e: Exception) { Log.e(TAG, "Cleanup error", e) }
        }
    }
    
    /**
     * Calculate video dimensions based on resolution and aspect ratio.
     */
    private fun calculateDimensions(resolution: Resolution, aspectRatio: AspectRatio): Pair<Int, Int> {
        return when (aspectRatio) {
            AspectRatio.HORIZONTAL_16_9 -> Pair(resolution.width, resolution.height)
            AspectRatio.VERTICAL_9_16 -> Pair(resolution.height, resolution.width)
            AspectRatio.SQUARE_1_1 -> {
                val size = minOf(resolution.width, resolution.height)
                Pair(size, size)
            }
        }
    }
    
    /**
     * Calculate appropriate bit rate for the video.
     */
    private fun calculateBitRate(width: Int, height: Int, fps: Int): Int {
        // Roughly 4 bits per pixel * fps / 30 as base
        val baseRate = width * height * 4
        return (baseRate * fps / 30).coerceIn(1_000_000, 20_000_000)
    }
    
    /**
     * Render a scene to a bitmap for transition effects.
     */
    private fun renderSceneToBitmap(
        scene: Scene,
        assets: List<SceneAsset>,
        bitmaps: SceneBitmaps,
        progress: Float,
        width: Int,
        height: Int
    ): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        
        animationEngine.renderFrame(
            canvas = canvas,
            scene = scene,
            assets = assets,
            handBitmap = bitmaps.handBitmaps[scene.projectId],
            backgroundBitmap = bitmaps.backgroundBitmaps[scene.projectId],
            assetBitmaps = bitmaps.assetBitmaps,
            progress = progress,
            width = width,
            height = height
        )
        
        return bitmap
    }
    
    /**
     * Encode a single frame using MediaCodec.
     */
    private fun encodeFrame(
        encoder: MediaCodec,
        muxer: MediaMuxer,
        bitmap: Bitmap,
        frameIndex: Int,
        bufferInfo: MediaCodec.BufferInfo,
        fps: Int,
        getTrackIndex: (Int) -> Int
    ) {
        // Get input buffer
        val inputBufferIndex = encoder.dequeueInputBuffer(TIMEOUT_US)
        if (inputBufferIndex >= 0) {
            val inputBuffer = encoder.getInputBuffer(inputBufferIndex) ?: return
            
            // Convert ARGB bitmap to NV21/YUV
            val yuvData = convertBitmapToYuv420(bitmap)
            inputBuffer.clear()
            inputBuffer.put(yuvData)
            
            val presentationTimeUs = (frameIndex * 1_000_000L / fps)
            encoder.queueInputBuffer(inputBufferIndex, 0, yuvData.size, presentationTimeUs, 0)
        }
        
        // Get output
        drainEncoderOutput(encoder, muxer, bufferInfo, getTrackIndex)
    }
    
    /**
     * Drain remaining encoded frames.
     */
    private fun drainEncoder(
        encoder: MediaCodec,
        muxer: MediaMuxer,
        bufferInfo: MediaCodec.BufferInfo,
        getTrackIndex: () -> Int
    ) {
        val inputBufferIndex = encoder.dequeueInputBuffer(TIMEOUT_US)
        if (inputBufferIndex >= 0) {
            encoder.queueInputBuffer(inputBufferIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
        }
        
        var outputDone = false
        while (!outputDone) {
            val outputBufferIndex = encoder.dequeueOutputBuffer(bufferInfo, TIMEOUT_US)
            if (outputBufferIndex >= 0) {
                if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
                    outputDone = true
                }
                
                if (bufferInfo.size > 0) {
                    val outputBuffer = encoder.getOutputBuffer(outputBufferIndex)
                    if (outputBuffer != null) {
                        muxer.writeSampleData(getTrackIndex(), outputBuffer, bufferInfo)
                    }
                }
                
                encoder.releaseOutputBuffer(outputBufferIndex, false)
            } else {
                outputDone = true
            }
        }
    }
    
    /**
     * Drain encoder output buffers.
     */
    private fun drainEncoderOutput(
        encoder: MediaCodec,
        muxer: MediaMuxer,
        bufferInfo: MediaCodec.BufferInfo,
        getTrackIndex: (Int) -> Int
    ) {
        while (true) {
            val outputBufferIndex = encoder.dequeueOutputBuffer(bufferInfo, TIMEOUT_US)
            
            when {
                outputBufferIndex == MediaCodec.INFO_TRY_AGAIN_LATER -> break
                outputBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                    val format = encoder.outputFormat
                    getTrackIndex(0)  // Trigger track addition
                }
                outputBufferIndex >= 0 -> {
                    val outputBuffer = encoder.getOutputBuffer(outputBufferIndex)
                    if (outputBuffer != null && bufferInfo.size > 0) {
                        muxer.writeSampleData(getTrackIndex(0), outputBuffer, bufferInfo)
                    }
                    encoder.releaseOutputBuffer(outputBufferIndex, false)
                    
                    if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
                        break
                    }
                }
            }
        }
    }
    
    /**
     * Convert ARGB bitmap to YUV420 format for encoding.
     */
    private fun convertBitmapToYuv420(bitmap: Bitmap): ByteArray {
        val width = bitmap.width
        val height = bitmap.height
        val argb = IntArray(width * height)
        bitmap.getPixels(argb, 0, width, 0, 0, width, height)
        
        val yuvSize = width * height * 3 / 2
        val yuv = ByteArray(yuvSize)
        
        var yIndex = 0
        var uvIndex = width * height
        
        for (j in 0 until height) {
            for (i in 0 until width) {
                val argbIndex = j * width + i
                val pixel = argb[argbIndex]
                
                val r = (pixel shr 16) and 0xFF
                val g = (pixel shr 8) and 0xFF
                val b = pixel and 0xFF
                
                // Y
                val y = ((66 * r + 129 * g + 25 * b + 128) shr 8) + 16
                yuv[yIndex++] = y.coerceIn(0, 255).toByte()
                
                // UV (sample every 2x2 block)
                if (j % 2 == 0 && i % 2 == 0) {
                    val u = ((-38 * r - 74 * g + 112 * b + 128) shr 8) + 128
                    val v = ((112 * r - 94 * g - 18 * b + 128) shr 8) + 128
                    yuv[uvIndex++] = u.coerceIn(0, 255).toByte()
                    yuv[uvIndex++] = v.coerceIn(0, 255).toByte()
                }
            }
        }
        
        return yuv
    }
    
    /**
     * Cancel ongoing export.
     */
    fun cancel() {
        _status.value = ExportStatus.CANCELLED
    }
    
    /**
     * Reset exporter state.
     */
    fun reset() {
        _status.value = ExportStatus.IDLE
        _progress.value = 0f
    }

    // ============================================================================================
    // Audio Processing Helpers
    // ============================================================================================

    private fun getWavInfo(audioPath: String): WavInfo? {
        val file = File(audioPath)
        if (!file.exists()) return null

        try {
            file.inputStream().use { stream ->
                val header = ByteArray(44)
                if (stream.read(header) != 44) return null

                // Check RIFF
                if (header[0] != 'R'.toByte() || header[1] != 'I'.toByte() || header[2] != 'F'.toByte()) return null

                val channels = ByteBuffer.wrap(header, 22, 2).order(java.nio.ByteOrder.LITTLE_ENDIAN).short.toInt()
                val sampleRate = ByteBuffer.wrap(header, 24, 4).order(java.nio.ByteOrder.LITTLE_ENDIAN).int
                val bitsPerSample = ByteBuffer.wrap(header, 34, 2).order(java.nio.ByteOrder.LITTLE_ENDIAN).short.toInt()

                return WavInfo(sampleRate, channels, bitsPerSample, 44) // 44 byte header fixed
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error reading WAV info: $audioPath", e)
            return null
        }
    }

    private fun loadAndResample(path: String, targetRate: Int): ByteArray? {
        val info = getWavInfo(path) ?: return null
        val file = File(path)
        if (!file.exists()) return null
        
        try {
            val rawBytes = file.readBytes()
            
            // Extract PCM
            if (rawBytes.size <= info.dataOffset) return null
            val pcmBytes = rawBytes.copyOfRange(info.dataOffset, rawBytes.size)
            
            // Convert to Shorts
            val shorts = ShortArray(pcmBytes.size / 2)
            ByteBuffer.wrap(pcmBytes).order(java.nio.ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(shorts)
            
            // Mix to Mono if needed
            val monoShorts = if (info.channels == 2) {
                 ShortArray(shorts.size / 2).apply {
                     for (i in indices) {
                         val s1 = shorts[i*2]
                         val s2 = shorts[i*2+1]
                         this[i] = ((s1 + s2) / 2).toShort()
                     }
                 }
            } else {
                 shorts
            }

            // Resample if needed
            val finalShorts = if (info.sampleRate != targetRate) {
                val ratio = info.sampleRate.toDouble() / targetRate.toDouble()
                val newLength = (monoShorts.size / ratio).toInt()
                val dest = ShortArray(newLength)
                
                for (i in 0 until newLength) {
                    val srcIdx = i * ratio
                    val idx0 = srcIdx.toInt().coerceIn(0, monoShorts.size - 1)
                    val idx1 = (idx0 + 1).coerceIn(0, monoShorts.size - 1)
                    val frac = srcIdx - idx0
                    
                    val v0 = monoShorts[idx0]
                    val v1 = monoShorts[idx1]
                    dest[i] = (v0 + frac * (v1 - v0)).toInt().toShort()
                }
                dest
            } else {
                monoShorts
            }
            
            // Convert back to bytes
            val resBytes = ByteArray(finalShorts.size * 2)
            ByteBuffer.wrap(resBytes).order(java.nio.ByteOrder.LITTLE_ENDIAN).asShortBuffer().put(finalShorts)
            return resBytes
            
        } catch (e: Exception) {
            Log.e(TAG, "Error resampling audio", e)
            return null
        }
    }


/**
 * Container for pre-loaded bitmaps needed for rendering.
 */
data class SceneBitmaps(
    val sceneHandBitmaps: Map<Long, Bitmap> = emptyMap(),
    val sceneBackgroundBitmaps: Map<Long, Bitmap> = emptyMap(),
    val assetBitmaps: Map<Long, Bitmap> = emptyMap()
)
