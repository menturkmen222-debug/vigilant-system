package com.whiteboard.animator.data.common

import com.whiteboard.animator.data.model.ApiProvider
import com.whiteboard.animator.data.preferences.PreferenceManager
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages API keys with rotation and fallback support.
 * Keeps track of failed keys in memory to avoid reusing them in the same session.
 */
@Singleton
class ApiKeyManager @Inject constructor(
    private val preferenceManager: PreferenceManager
) {
    private val failedKeys = mutableSetOf<String>()
    private val mutex = Mutex()

    /**
     * Get the next available valid key for the provider.
     * Skips keys marked as failed during this session.
     */
    suspend fun getWorkingKey(provider: ApiProvider): String? {
        val keys = preferenceManager.getKeysForProvider(provider)
        if (keys.isEmpty()) return null
        
        // If we have a legacy single key and no provider keys, it might be handled by PreferenceManager migration or fallback
        // But here we rely on getKeysForProvider returning the list.
        
        mutex.withLock {
            // Return first key that hasn't failed
            return keys.firstOrNull { !failedKeys.contains(it) } 
                ?: keys.firstOrNull() // If all failed, retry from start (or return null?) -> Retry from start gives a chance if it was transient
        }
    }

    /**
     * Mark a key as failed (e.g. 401 Unauthorized or Quota Exceeded).
     */
    suspend fun markKeyAsFailed(key: String) {
        mutex.withLock {
            failedKeys.add(key)
        }
    }

    /**
     * Reset failed keys state (e.g. on app restart or manual reset).
     */
    suspend fun resetFailedKeys() {
        mutex.withLock {
            failedKeys.clear()
        }
    }
}
