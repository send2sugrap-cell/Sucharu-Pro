package com.sucharu.sucharupro.ui.features.inventory.stockout

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.inventory.stockout.InventoryStockOut
import com.sucharu.sucharupro.domain.model.inventory.stockout.InventoryStockOutStatus
import com.sucharu.sucharupro.domain.model.user.UserRole
import com.sucharu.sucharupro.domain.repository.InventoryStockOutRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel orchestrating the stock-out list screen (Module 07 Step 04).
 *
 * Provides reactive filtered observation with status filter and search query support.
 * Only displays actions permitted by the user's role.
 */
class InventoryStockOutListViewModel(
    private val repository: InventoryStockOutRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(InventoryStockOutListUiState(isLoading = true))
    val uiState: StateFlow<InventoryStockOutListUiState> = _uiState.asStateFlow()

    fun loadStockOuts(projectId: String) {
        _uiState.update { it.copy(isLoading = true, projectId = projectId, errorMessage = null) }
        viewModelScope.launch {
            repository.observeStockOuts(projectId).collect { list ->
                _uiState.update { current ->
                    val filtered = applyFilters(
                        stockOuts = list,
                        query = current.searchQuery,
                        statusFilter = current.selectedStatusFilter
                    )
                    current.copy(
                        isLoading = false,
                        stockOuts = list,
                        filteredStockOuts = filtered
                    )
                }
            }
        }
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.update { current ->
            val filtered = applyFilters(
                stockOuts = current.stockOuts,
                query = query,
                statusFilter = current.selectedStatusFilter
            )
            current.copy(searchQuery = query, filteredStockOuts = filtered)
        }
    }

    fun onStatusFilterChanged(status: InventoryStockOutStatus?) {
        _uiState.update { current ->
            val filtered = applyFilters(
                stockOuts = current.stockOuts,
                query = current.searchQuery,
                statusFilter = status
            )
            current.copy(selectedStatusFilter = status, filteredStockOuts = filtered)
        }
    }

    fun cancelStockOut(
        stockOutId: String,
        actorId: String,
        timestamp: String,
        role: UserRole = UserRole.MANAGER
    ) {
        viewModelScope.launch {
            val res = repository.cancelStockOut(stockOutId, actorId, timestamp, role)
            if (res is DomainResult.Error) {
                _uiState.update { it.copy(errorMessage = res.message) }
            }
        }
    }

    private fun applyFilters(
        stockOuts: List<InventoryStockOut>,
        query: String,
        statusFilter: InventoryStockOutStatus?
    ): List<InventoryStockOut> {
        val q = query.trim().lowercase()
        return stockOuts.filter { s ->
            val matchesQuery = q.isBlank() ||
                s.stockOutReference.lowercase().contains(q) ||
                s.warehouseId.lowercase().contains(q) ||
                (s.sourceReference?.lowercase()?.contains(q) ?: false)
            val matchesStatus = statusFilter == null || s.status == statusFilter
            matchesQuery && matchesStatus
        }
    }
}
