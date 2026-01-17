package com.whiteboard.animator.engine

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

/**
 * Text-to-Speech Manager for voiceover generation.
 * 
 * Supports:
 * - Offline (device TTS)
 * - Online (ElevenLabs, Azure - to be implemented in API layer)
 * 
 * Generates audio files per scene that are stored locally.
 */
@Singleton
class TTSManager @Inject constructor(
    private val context: Context
) {
    private var tts: TextToSpeech? = null
    private var isInitialized = false
    private var initializationError: String? = null
    
    companion object {
        private const val TAG = "TTSManager"
    }
    
    /**
     * Initialize the TTS engine.
     * Must be called before generating audio.
     */
    suspend fun initialize(): Boolean = suspendCancellableCoroutine { continuation ->
        if (isInitialized) {
            continuation.resume(true)
            return@suspendCancellableCoroutine
        }
        
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                isInitialized = true
                tts?.language = Locale.getDefault()
                Log.d(TAG, "TTS initialized successfully")
                continuation.resume(true)
            } else {
                initializationError = "TTS initialization failed with status: $status"
                Log.e(TAG, initializationError!!)
                continuation.resume(false)
            }
        }
        
        continuation.invokeOnCancellation {
            // Don't shutdown on init cancellation
        }
    }
    
    /**
     * Check if TTS is available on this device.
     */
    fun isAvailable(): Boolean = isInitialized && tts != null
    
    /**
     * Get available offline voices.
     */
    fun getAvailableVoices(): List<Voice> {
        return tts?.voices?.filter { !it.isNetworkConnectionRequired }?.toList() ?: emptyList()
    }
    
    /**
     * Set the voice for TTS.
     * @param voiceId Voice name from getAvailableVoices()
     */
    fun setVoice(voiceId: String): Boolean {
        val voice = tts?.voices?.find { it.name == voiceId } ?: return false
        tts?.voice = voice
        return true
    }
    
    /**
     * Set speech rate.
     * @param rate Speed multiplier (0.5 to 2.0, default 1.0)
     */
    fun setSpeechRate(rate: Float) {
        tts?.setSpeechRate(rate.coerceIn(0.5f, 2.0f))
    }
    
    /**
     * Set speech pitch.
     * @param pitch Pitch multiplier (0.5 to 2.0, default 1.0)
     */
    fun setPitch(pitch: Float) {
        tts?.setPitch(pitch.coerceIn(0.5f, 2.0f))
    }
    
    /**
     * Generate audio file from text.
     * 
     * @param text Text to synthesize
     * @param outputDir Directory to save the audio file
     * @param fileName Name for the audio file (without extension)
     * @return Result with file path and duration, or error
     */
    suspend fun generateAudio(
        text: String,
        outputDir: File,
        fileName: String
    ): Result<TTSResult> = withContext(Dispatchers.IO) {
        if (!isInitialized) {
            val initialized = initialize()
            if (!initialized) {
                return@withContext Result.failure(Exception("TTS not available"))
            }
        }
        
        val ttsEngine = tts ?: return@withContext Result.failure(Exception("TTS engine is null"))
        
        try {
            // Ensure output directory exists
            if (!outputDir.exists()) {
                outputDir.mkdirs()
            }
            
            val outputFile = File(outputDir, "$fileName.wav")
            val utteranceId = "tts_${System.currentTimeMillis()}"
            
            // Use suspendCancellableCoroutine for async synthesis
            val result = suspendCancellableCoroutine { continuation ->
                ttsEngine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {
                        Log.d(TAG, "TTS synthesis started: $utteranceId")
                    }
                    
                    override fun onDone(utteranceId: String?) {
                        Log.d(TAG, "TTS synthesis done: $utteranceId")
                        if (continuation.isActive) {
                            // Calculate duration (rough estimate from file size)
                            val durationMs = estimateDuration(text)
                            continuation.resume(
                                Result.success(TTSResult(outputFile.absolutePath, durationMs))
                            )
                        }
                    }
                    
                    @Deprecated("Deprecated in API 21")
                    override fun onError(utteranceId: String?) {
                        if (continuation.isActive) {
                            continuation.resume(Result.failure(Exception("TTS synthesis error")))
                        }
                    }
                    
                    override fun onError(utteranceId: String?, errorCode: Int) {
                        if (continuation.isActive) {
                            continuation.resume(
                                Result.failure(Exception("TTS error code: $errorCode"))
                            )
                        }
                    }
                })
                
                val synthesizeResult = ttsEngine.synthesizeToFile(
                    text,
                    null,
                    outputFile,
                    utteranceId
                )
                
                if (synthesizeResult != TextToSpeech.SUCCESS) {
                    if (continuation.isActive) {
                        continuation.resume(
                            Result.failure(Exception("Synthesis request failed: $synthesizeResult"))
                        )
                    }
                }
                
                continuation.invokeOnCancellation {
                    ttsEngine.stop()
                }
            }
            
            result
        } catch (e: Exception) {
            Log.e(TAG, "Error generating audio", e)
            Result.failure(e)
        }
    }
    
    /**
     * Get estimated duration for text in milliseconds.
     * Useful for timeline calculation before actual synthesis.
     */
    fun estimateDuration(text: String): Long {
        val wordCount = text.split(Regex("\\s+")).size
        val wordsPerMinute = 150  // Average speaking rate
        return (wordCount.toFloat() / wordsPerMinute * 60 * 1000).toLong()
    }
    
    /**
     * Stop any ongoing synthesis.
     */
    fun stop() {
        tts?.stop()
    }
    
    /**
     * Release TTS resources.
     * Should be called when done with TTS.
     */
    fun shutdown() {
        tts?.shutdown()
        tts = null
        isInitialized = false
    }
    
    /**
     * Preview text by speaking it (not saving to file).
     */
    fun speak(text: String) {
        if (isInitialized) {
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "preview")
        }
    }
    
    /**
     * Stop preview playback.
     */
    fun stopPreview() {
        tts?.stop()
    }
}

/**
 * Result of TTS synthesis.
 */
data class TTSResult(
    val filePath: String,
    val durationMs: Long
)
