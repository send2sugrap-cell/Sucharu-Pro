package com.sucharu.sucharupro.ui.features.inventory.analytics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sucharu.sucharupro.domain.model.inventory.analytics.InventoryException
import com.sucharu.sucharupro.domain.model.inventory.analytics.InventoryExceptionStatus
import com.sucharu.sucharupro.domain.model.inventory.analytics.InventoryExceptionType
import com.sucharu.sucharupro.domain.repository.InventoryAnalyticsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel for Inventory Governance & Exception management (Module 07 Step 10).
 */
class InventoryGovernanceViewModel(
    private val repository: InventoryAnalyticsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(InventoryGovernanceUiState(isLoading = true))
    val uiState: StateFlow<InventoryGovernanceUiState> = _uiState.asStateFlow()

    fun loadGovernance(projectId: String) {
        _uiState.update { it.copy(isLoading = true, projectId = projectId, errorMessage = null) }
        viewModelScope.launch {
            repository.observeExceptions(projectId).collect { list ->
                _uiState.update { current ->
                    val filtered = applyFilters(
                        list,
                        current.selectedSeverity,
                        current.selectedType
                    )
                    current.copy(
                        isLoading = false,
                        exceptions = list,
                        filteredExceptions = filtered
                    )
                }
            }
        }
    }

    fun onSeverityFilterChanged(severity: InventoryException.Severity?) {
        _uiState.update { current ->
            val filtered = applyFilters(current.exceptions, severity, current.selectedType)
            current.copy(selectedSeverity = severity, filteredExceptions = filtered)
        }
    }

    fun onTypeFilterChanged(type: InventoryExceptionType?) {
        _uiState.update { current ->
            val filtered = applyFilters(current.exceptions, current.selectedSeverity, type)
            current.copy(selectedType = type, filteredExceptions = filtered)
        }
    }

    private fun applyFilters(
        list: List<InventoryException>,
        severity: InventoryException.Severity?,
        type: InventoryExceptionType?
    ): List<InventoryException> {
        return list.filter { e ->
            (severity == null || e.severity == severity) &&
            (type == null || e.type == type) &&
            (e.status == InventoryExceptionStatus.OPEN || e.status == InventoryExceptionStatus.ACKNOWLEDGED)
        }
    }

    fun runGovernanceCheck() {
        val projectId = _uiState.value.projectId
        if (projectId.isBlank()) return
        
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            repository.executeGovernanceCheck(projectId, "SYSTEM_ACTOR") // Simplified actor
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    fun acknowledgeException(exceptionId: String) {
        // TODO: Implement repository call to update status to ACKNOWLEDGED
    }

    fun resolveException(exceptionId: String) {
        // TODO: Implement repository call to update status to RESOLVED
    }
}
