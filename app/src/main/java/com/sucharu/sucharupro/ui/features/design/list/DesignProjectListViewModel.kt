package com.sucharu.sucharupro.ui.features.design.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sucharu.sucharupro.domain.model.design.DesignProject
import com.sucharu.sucharupro.domain.model.design.DesignStatus
import com.sucharu.sucharupro.domain.repository.DesignProjectRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

/**
 * ViewModel for Design Project list and queue management.
 */
class DesignProjectListViewModel(
    private val designRepository: DesignProjectRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    private val _selectedStatus = MutableStateFlow<DesignStatus?>(null)
    private val _selectedDesignerId = MutableStateFlow<String?>(null)
    private val _actionState = MutableStateFlow<Triple<Boolean, String?, String?>>(Triple(false, null, null))

    private val _uiState = MutableStateFlow<DesignProjectListUiState>(DesignProjectListUiState.Loading)
    val uiState: StateFlow<DesignProjectListUiState> = _uiState.asStateFlow()

    init {
        observeProjects()
    }

    private fun observeProjects() {
        viewModelScope.launch {
            combine(
                designRepository.observeDesignProjects(),
                _searchQuery,
                _selectedStatus,
                _selectedDesignerId,
                _actionState
            ) { projects, query, status, designerId, action ->
                if (projects.isEmpty()) {
                    DesignProjectListUiState.Empty()
                } else {
                    val filtered = projects.filter { project ->
                        val matchesQuery = query.isBlank() ||
                                project.projectNumber.contains(query, ignoreCase = true) ||
                                project.title.contains(query, ignoreCase = true) ||
                                project.orderNumber.contains(query, ignoreCase = true) ||
                                (project.assignedDesignerName?.contains(query, ignoreCase = true) == true)

                        val matchesStatus = status == null || project.status == status
                        val matchesDesigner = designerId == null || project.assignedDesignerId == designerId

                        matchesQuery && matchesStatus && matchesDesigner
                    }

                    DesignProjectListUiState.Success(
                        allProjects = projects,
                        visibleProjects = filtered,
                        searchQuery = query,
                        selectedStatus = status,
                        selectedDesignerId = designerId,
                        isActionInProgress = action.first,
                        actionMessage = action.second,
                        actionError = action.third
                    )
                }
            }.collect { state ->
                _uiState.value = state
            }
        }
    }

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun onStatusFilterChange(status: DesignStatus?) {
        _selectedStatus.value = status
    }

    fun onDesignerFilterChange(designerId: String?) {
        _selectedDesignerId.value = designerId
    }

    fun clearFilters() {
        _searchQuery.value = ""
        _selectedStatus.value = null
        _selectedDesignerId.value = null
    }
}
