package com.whiteboard.animator.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.whiteboard.animator.data.model.Project
import com.whiteboard.animator.data.repository.ProjectRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for Home Screen.
 * Handles project listing, searching, and deletion.
 */
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: ProjectRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _projects = repository.allProjects
    
    // Combine projects with search query
    val uiState: StateFlow<HomeUiState> = combine(_projects, _searchQuery) { projects, query ->
        if (query.isBlank()) {
            HomeUiState(projects = projects)
        } else {
            val filtered = projects.filter { it.name.contains(query, ignoreCase = true) }
            HomeUiState(projects = filtered)
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = HomeUiState(isLoading = true)
    )

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun deleteProject(projectId: Long) {
        viewModelScope.launch {
            repository.deleteProject(projectId)
        }
    }
}

data class HomeUiState(
    val projects: List<Project> = emptyList(),
    val isLoading: Boolean = false
)
