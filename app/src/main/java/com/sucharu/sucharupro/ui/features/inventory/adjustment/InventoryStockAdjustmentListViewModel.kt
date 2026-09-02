package com.sucharu.sucharupro.ui.features.inventory.adjustment

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.inventory.adjustment.InventoryStockAdjustment
import com.sucharu.sucharupro.domain.model.inventory.adjustment.InventoryStockAdjustmentStatus
import com.sucharu.sucharupro.domain.model.user.UserRole
import com.sucharu.sucharupro.domain.repository.InventoryStockAdjustmentRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel orchestrating the stock adjustment list screen (Module 07 Step 06).
 *
 * Provides reactive filtered observation with status filter and search query support.
 * Only displays actions permitted by the user's role.
 */
class InventoryStockAdjustmentListViewModel(
    private val repository: InventoryStockAdjustmentRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(InventoryStockAdjustmentListUiState(isLoading = true))
    val uiState: StateFlow<InventoryStockAdjustmentListUiState> = _uiState.asStateFlow()

    fun loadAdjustments(projectId: String) {
        _uiState.update { it.copy(isLoading = true, projectId = projectId, errorMessage = null) }
        viewModelScope.launch {
            repository.observeStockAdjustments(projectId).collect { list ->
                _uiState.update { current ->
                    val filtered = applyFilters(
                        adjustments = list,
                        query = current.searchQuery,
                        statusFilter = current.selectedStatusFilter
                    )
                    current.copy(
                        isLoading = false,
                        adjustments = list,
                        filteredAdjustments = filtered
                    )
                }
            }
        }
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.update { current ->
            val filtered = applyFilters(
                adjustments = current.adjustments,
                query = query,
                statusFilter = current.selectedStatusFilter
            )
            current.copy(searchQuery = query, filteredAdjustments = filtered)
        }
    }

    fun onStatusFilterChanged(status: InventoryStockAdjustmentStatus?) {
        _uiState.update { current ->
            val filtered = applyFilters(
                adjustments = current.adjustments,
                query = current.searchQuery,
                statusFilter = status
            )
            current.copy(selectedStatusFilter = status, filteredAdjustments = filtered)
        }
    }

    fun cancelAdjustment(
        adjustmentId: String,
        actorId: String,
        timestamp: String,
        role: UserRole = UserRole.MANAGER
    ) {
        viewModelScope.launch {
            val res = repository.cancelStockAdjustment(adjustmentId, actorId, timestamp, role)
            if (res is DomainResult.Error) {
                _uiState.update { it.copy(errorMessage = res.message) }
            }
        }
    }

    private fun applyFilters(
        adjustments: List<InventoryStockAdjustment>,
        query: String,
        statusFilter: InventoryStockAdjustmentStatus?
    ): List<InventoryStockAdjustment> {
        val q = query.trim().lowercase()
        return adjustments.filter { s ->
            val matchesQuery = q.isBlank() ||
                s.adjustmentReference.lowercase().contains(q) ||
                s.warehouseId.lowercase().contains(q)
            val matchesStatus = statusFilter == null || s.status == statusFilter
            matchesQuery && matchesStatus
        }
    }
}
