package com.whiteboard.animator.engine

import android.util.Log
import com.whiteboard.animator.data.model.ApiProvider
import com.whiteboard.animator.data.preferences.PreferenceManager
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

/**
 * API Key Manager with rotation and fallback logic.
 * 
 * Features:
 * - Tracks multiple keys per provider
 * - Rotates to next key on failure
 * - Marks failed keys temporarily
 * - Provides fallback chain (e.g., Gemini -> OpenAI)
 */
@Singleton
class ApiKeyManager @Inject constructor(
    private val preferenceManager: PreferenceManager
) {
    private val mutex = Mutex()
    
    // Track current active key index per provider
    private val activeKeyIndex = mutableMapOf<ApiProvider, Int>()
    
    // Track temporarily failed keys (provider to set of failed keys)
    private val failedKeys = mutableMapOf<ApiProvider, MutableSet<String>>()
    
    companion object {
        private const val TAG = "ApiKeyManager"
        
        // Fallback chains for different services
        val LLM_FALLBACK_CHAIN = listOf(ApiProvider.GEMINI, ApiProvider.OPENAI)
        val IMAGE_FALLBACK_CHAIN = listOf(ApiProvider.HUGGINGFACE)
        val TTS_FALLBACK_CHAIN = listOf(ApiProvider.ELEVENLABS, ApiProvider.AZURE_TTS)
    }
    
    /**
     * Get the next available key for a provider.
     * Skips temporarily failed keys.
     */
    suspend fun getNextKey(provider: ApiProvider): String? = mutex.withLock {
        val allKeys = preferenceManager.getKeysForProvider(provider)
        if (allKeys.isEmpty()) return@withLock null
        
        val failedSet = failedKeys[provider] ?: emptySet()
        val availableKeys = allKeys.filter { it !in failedSet }
        
        if (availableKeys.isEmpty()) {
            // All keys failed, reset and try again
            Log.w(TAG, "All keys for $provider failed, resetting...")
            failedKeys[provider]?.clear()
            return@withLock allKeys.firstOrNull()
        }
        
        val currentIndex = activeKeyIndex[provider] ?: 0
        val safeIndex = currentIndex.coerceIn(0, availableKeys.size - 1)
        activeKeyIndex[provider] = safeIndex
        
        return@withLock availableKeys[safeIndex]
    }
    
    /**
     * Mark a key as temporarily failed for a provider.
     * The key will be skipped until all keys fail and reset occurs.
     */
    suspend fun markKeyFailed(provider: ApiProvider, key: String) = mutex.withLock {
        Log.w(TAG, "Marking key as failed for $provider: ${key.take(8)}...")
        val failedSet = failedKeys.getOrPut(provider) { mutableSetOf() }
        failedSet.add(key)
        
        // Rotate to next key
        rotateKeyInternal(provider)
    }
    
    /**
     * Rotate to the next available key for a provider.
     */
    suspend fun rotateKey(provider: ApiProvider) = mutex.withLock {
        rotateKeyInternal(provider)
    }
    
    private suspend fun rotateKeyInternal(provider: ApiProvider) {
        val allKeys = preferenceManager.getKeysForProvider(provider)
        if (allKeys.isEmpty()) return
        
        val currentIndex = activeKeyIndex[provider] ?: 0
        val nextIndex = (currentIndex + 1) % allKeys.size
        activeKeyIndex[provider] = nextIndex
        Log.d(TAG, "Rotated $provider key to index $nextIndex")
    }
    
    /**
     * Get a working key from a fallback chain.
     * Tries each provider in order until a key is found.
     */
    suspend fun getKeyFromChain(chain: List<ApiProvider>): Pair<ApiProvider, String>? {
        for (provider in chain) {
            val key = getNextKey(provider)
            if (key != null) {
                return provider to key
            }
        }
        return null
    }
    
    /**
     * Check if any keys are available for a provider.
     */
    suspend fun hasKeysForProvider(provider: ApiProvider): Boolean {
        return preferenceManager.getKeysForProvider(provider).isNotEmpty()
    }
    
    /**
     * Get count of available keys for a provider.
     */
    suspend fun getKeyCount(provider: ApiProvider): Int {
        return preferenceManager.getKeysForProvider(provider).size
    }
    
    /**
     * Clear all failed key markers (e.g., on app restart or manual reset).
     */
    fun clearFailedKeys() {
        failedKeys.clear()
        activeKeyIndex.clear()
    }
}
