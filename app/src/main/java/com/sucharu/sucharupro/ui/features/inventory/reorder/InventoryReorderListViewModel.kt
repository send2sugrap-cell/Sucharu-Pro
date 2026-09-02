package com.sucharu.sucharupro.ui.features.inventory.reorder

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sucharu.sucharupro.domain.model.inventory.reorder.InventoryReorderAlert
import com.sucharu.sucharupro.domain.model.inventory.reorder.InventoryReorderAlertStatus
import com.sucharu.sucharupro.domain.repository.InventoryReorderRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel orchestrating the reorder alert list screen (Module 07 Step 08).
 *
 * Provides reactive filtered observation with status filter and search query support.
 */
class InventoryReorderListViewModel(
    private val repository: InventoryReorderRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(InventoryReorderListUiState(isLoading = true))
    val uiState: StateFlow<InventoryReorderListUiState> = _uiState.asStateFlow()

    fun loadAlerts(projectId: String) {
        _uiState.update { it.copy(isLoading = true, projectId = projectId, errorMessage = null) }
        viewModelScope.launch {
            repository.observeAlerts(projectId).collect { list ->
                _uiState.update { current ->
                    val filtered = applyFilters(
                        alerts = list,
                        query = current.searchQuery,
                        statusFilter = current.selectedStatusFilter
                    )
                    current.copy(
                        isLoading = false,
                        alerts = list,
                        filteredAlerts = filtered
                    )
                }
            }
        }
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.update { current ->
            val filtered = applyFilters(
                alerts = current.alerts,
                query = query,
                statusFilter = current.selectedStatusFilter
            )
            current.copy(searchQuery = query, filteredAlerts = filtered)
        }
    }

    fun onStatusFilterChanged(status: InventoryReorderAlertStatus?) {
        _uiState.update { current ->
            val filtered = applyFilters(
                alerts = current.alerts,
                query = current.searchQuery,
                statusFilter = status
            )
            current.copy(selectedStatusFilter = status, filteredAlerts = filtered)
        }
    }

    private fun applyFilters(
        alerts: List<InventoryReorderAlert>,
        query: String,
        statusFilter: InventoryReorderAlertStatus?
    ): List<InventoryReorderAlert> {
        val q = query.trim().lowercase()
        return alerts.filter { s ->
            val matchesQuery = q.isBlank() ||
                s.productId.lowercase().contains(q) ||
                s.locationId.lowercase().contains(q)
            val matchesStatus = statusFilter == null || s.status == statusFilter
            matchesQuery && matchesStatus
        }
    }
}
