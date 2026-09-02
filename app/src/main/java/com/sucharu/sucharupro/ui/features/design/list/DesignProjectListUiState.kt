package com.sucharu.sucharupro.ui.features.design.list

import com.sucharu.sucharupro.domain.model.design.DesignProject
import com.sucharu.sucharupro.domain.model.design.DesignStatus

/**
 * UI State for Design Project Queue / List Screen.
 */
sealed interface DesignProjectListUiState {
    data object Loading : DesignProjectListUiState

    data class Success(
        val allProjects: List<DesignProject>,
        val visibleProjects: List<DesignProject>,
        val searchQuery: String = "",
        val selectedStatus: DesignStatus? = null,
        val selectedDesignerId: String? = null,
        val isActionInProgress: Boolean = false,
        val actionMessage: String? = null,
        val actionError: String? = null
    ) : DesignProjectListUiState {
        val totalCount: Int get() = allProjects.size
        val visibleCount: Int get() = visibleProjects.size
        val isFiltered: Boolean get() = searchQuery.isNotBlank() || selectedStatus != null || selectedDesignerId != null
    }

    data class Empty(
        val message: String = "No design projects available. Design projects created for production jobs will appear here."
    ) : DesignProjectListUiState

    data class Error(
        val errorMessage: String,
        val canRetry: Boolean = true
    ) : DesignProjectListUiState
}
