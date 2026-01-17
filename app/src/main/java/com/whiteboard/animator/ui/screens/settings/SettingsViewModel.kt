package com.whiteboard.animator.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.whiteboard.animator.data.model.ApiProvider
import com.whiteboard.animator.data.model.VideoStyle
import com.whiteboard.animator.data.preferences.PreferenceManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val preferenceManager: PreferenceManager
) : ViewModel() {

    val apiKey = preferenceManager.apiKey
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    val isDarkMode = preferenceManager.isDarkMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val language = preferenceManager.language
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "en")

    val defaultVideoStyle = preferenceManager.defaultVideoStyle
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), VideoStyle.WHITEBOARD)

    val apiKeysMap = preferenceManager.apiKeysMap
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    fun updateApiKey(key: String) {
        viewModelScope.launch {
            preferenceManager.setApiKey(key)
        }
    }
    
    fun setDarkMode(enabled: Boolean) {
        viewModelScope.launch {
            preferenceManager.setDarkMode(enabled)
        }
    }

    fun setLanguage(lang: String) {
        viewModelScope.launch {
            preferenceManager.setLanguage(lang)
        }
    }

    fun setDefaultVideoStyle(style: VideoStyle) {
        viewModelScope.launch {
            preferenceManager.setDefaultVideoStyle(style)
        }
    }

    fun addApiKey(provider: ApiProvider, key: String) {
        viewModelScope.launch {
            preferenceManager.addApiKey(provider, key)
        }
    }

    fun removeApiKey(provider: ApiProvider, key: String) {
        viewModelScope.launch {
            preferenceManager.removeApiKey(provider, key)
        }
    }
}

