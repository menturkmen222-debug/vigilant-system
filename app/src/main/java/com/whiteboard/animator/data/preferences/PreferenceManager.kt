package com.whiteboard.animator.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.whiteboard.animator.data.model.ApiProvider
import com.whiteboard.animator.data.model.VideoStyle
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

@Singleton
class PreferenceManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val dataStore = context.dataStore
    private val json = Json { ignoreUnknownKeys = true }

    companion object {
        val KEY_ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
        val KEY_API_KEY = stringPreferencesKey("api_key") // Legacy single key
        val KEY_DARK_MODE = booleanPreferencesKey("dark_mode")
        val KEY_LANGUAGE = stringPreferencesKey("language") // en, uz, tk
        val KEY_DEFAULT_STYLE = stringPreferencesKey("default_video_style")
        val KEY_API_KEYS_JSON = stringPreferencesKey("api_keys_json") // JSON map of provider->List<key>
    }

    // ========== Onboarding ==========
    val isOnboardingCompleted: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[KEY_ONBOARDING_COMPLETED] ?: false
    }

    suspend fun setOnboardingCompleted(completed: Boolean) {
        dataStore.edit { preferences ->
            preferences[KEY_ONBOARDING_COMPLETED] = completed
        }
    }

    // ========== Legacy Single API Key (backward compatibility) ==========
    val apiKey: Flow<String> = dataStore.data.map { preferences ->
        preferences[KEY_API_KEY] ?: ""
    }

    suspend fun setApiKey(key: String) {
        dataStore.edit { preferences ->
            preferences[KEY_API_KEY] = key
        }
    }

    // ========== Dark Mode ==========
    val isDarkMode: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[KEY_DARK_MODE] ?: false
    }

    suspend fun setDarkMode(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[KEY_DARK_MODE] = enabled
        }
    }

    // ========== Language ==========
    val language: Flow<String> = dataStore.data.map { preferences ->
        preferences[KEY_LANGUAGE] ?: "en"
    }

    suspend fun setLanguage(lang: String) {
        dataStore.edit { preferences ->
            preferences[KEY_LANGUAGE] = lang
        }
    }

    // ========== Default Video Style ==========
    val defaultVideoStyle: Flow<VideoStyle> = dataStore.data.map { preferences ->
        val styleName = preferences[KEY_DEFAULT_STYLE] ?: VideoStyle.WHITEBOARD.name
        try { VideoStyle.valueOf(styleName) } catch (e: Exception) { VideoStyle.WHITEBOARD }
    }

    suspend fun setDefaultVideoStyle(style: VideoStyle) {
        dataStore.edit { preferences ->
            preferences[KEY_DEFAULT_STYLE] = style.name
        }
    }

    // ========== Multi-API Keys ==========
    
    /**
     * Get all API keys as a map of provider to list of keys.
     */
    val apiKeysMap: Flow<Map<ApiProvider, List<String>>> = dataStore.data.map { preferences ->
        val jsonString = preferences[KEY_API_KEYS_JSON] ?: "{}"
        try {
            val rawMap: Map<String, List<String>> = json.decodeFromString(jsonString)
            rawMap.mapKeys { (key, _) -> 
                try { ApiProvider.valueOf(key) } catch (e: Exception) { null }
            }.filterKeys { it != null }.mapKeys { it.key!! }
        } catch (e: Exception) {
            emptyMap()
        }
    }

    /**
     * Get keys for a specific provider.
     */
    suspend fun getKeysForProvider(provider: ApiProvider): List<String> {
        return apiKeysMap.first()[provider] ?: emptyList()
    }

    /**
     * Add a key for a provider.
     */
    suspend fun addApiKey(provider: ApiProvider, key: String) {
        val current = apiKeysMap.first().toMutableMap()
        val providerKeys = current[provider]?.toMutableList() ?: mutableListOf()
        if (!providerKeys.contains(key)) {
            providerKeys.add(key)
            current[provider] = providerKeys
            saveApiKeysMap(current)
        }
    }

    /**
     * Remove a key for a provider.
     */
    suspend fun removeApiKey(provider: ApiProvider, key: String) {
        val current = apiKeysMap.first().toMutableMap()
        val providerKeys = current[provider]?.toMutableList() ?: return
        providerKeys.remove(key)
        current[provider] = providerKeys
        saveApiKeysMap(current)
    }

    /**
     * Get the first available key for a provider.
     */
    suspend fun getActiveKey(provider: ApiProvider): String? {
        return getKeysForProvider(provider).firstOrNull()
    }

    private suspend fun saveApiKeysMap(map: Map<ApiProvider, List<String>>) {
        val rawMap = map.mapKeys { it.key.name }
        val jsonString = json.encodeToString(rawMap)
        dataStore.edit { preferences ->
            preferences[KEY_API_KEYS_JSON] = jsonString
        }
    }
}
