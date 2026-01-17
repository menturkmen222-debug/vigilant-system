package com.whiteboard.animator.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.whiteboard.animator.data.model.CharacterReference
import com.whiteboard.animator.data.model.CharacterType
import com.whiteboard.animator.data.repository.ProjectRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CharacterManagerViewModel @Inject constructor(
    private val repository: ProjectRepository
) : ViewModel() {

    private val _characters = MutableStateFlow<List<CharacterReference>>(emptyList())
    val characters: StateFlow<List<CharacterReference>> = _characters

    fun loadCharacters(projectId: Long) {
        viewModelScope.launch {
            repository.getCharacterReferencesFlow(projectId)
                .collect {
                    _characters.value = it
                }
        }
    }

    fun addCharacter(projectId: Long, name: String, type: CharacterType, imagePath: String, description: String) {
        viewModelScope.launch {
            repository.addCharacterReference(projectId, name, type, imagePath, description)
        }
    }

    fun deleteCharacter(character: CharacterReference) {
        viewModelScope.launch {
            repository.deleteCharacterReference(character.id)
        }
    }
}
