package com.sucharu.sucharupro.ui.features.inventory.stocktransfer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.inventory.stocktransfer.InventoryStockTransfer
import com.sucharu.sucharupro.domain.model.inventory.stocktransfer.InventoryStockTransferStatus
import com.sucharu.sucharupro.domain.model.user.UserRole
import com.sucharu.sucharupro.domain.repository.InventoryStockTransferRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel orchestrating the stock transfer list screen (Module 07 Step 05).
 *
 * Provides reactive filtered observation with status filter and search query support.
 * Only displays actions permitted by the user's role.
 */
class InventoryStockTransferListViewModel(
    private val repository: InventoryStockTransferRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(InventoryStockTransferListUiState(isLoading = true))
    val uiState: StateFlow<InventoryStockTransferListUiState> = _uiState.asStateFlow()

    fun loadTransfers(projectId: String) {
        _uiState.update { it.copy(isLoading = true, projectId = projectId, errorMessage = null) }
        viewModelScope.launch {
            repository.observeStockTransfers(projectId).collect { list ->
                _uiState.update { current ->
                    val filtered = applyFilters(
                        transfers = list,
                        query = current.searchQuery,
                        statusFilter = current.selectedStatusFilter
                    )
                    current.copy(
                        isLoading = false,
                        transfers = list,
                        filteredTransfers = filtered
                    )
                }
            }
        }
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.update { current ->
            val filtered = applyFilters(
                transfers = current.transfers,
                query = query,
                statusFilter = current.selectedStatusFilter
            )
            current.copy(searchQuery = query, filteredTransfers = filtered)
        }
    }

    fun onStatusFilterChanged(status: InventoryStockTransferStatus?) {
        _uiState.update { current ->
            val filtered = applyFilters(
                transfers = current.transfers,
                query = current.searchQuery,
                statusFilter = status
            )
            current.copy(selectedStatusFilter = status, filteredTransfers = filtered)
        }
    }

    fun cancelTransfer(
        transferId: String,
        actorId: String,
        timestamp: String,
        role: UserRole = UserRole.MANAGER
    ) {
        viewModelScope.launch {
            val res = repository.cancelStockTransfer(transferId, actorId, timestamp, role)
            if (res is DomainResult.Error) {
                _uiState.update { it.copy(errorMessage = res.message) }
            }
        }
    }

    private fun applyFilters(
        transfers: List<InventoryStockTransfer>,
        query: String,
        statusFilter: InventoryStockTransferStatus?
    ): List<InventoryStockTransfer> {
        val q = query.trim().lowercase()
        return transfers.filter { s ->
            val matchesQuery = q.isBlank() ||
                s.transferReference.lowercase().contains(q) ||
                s.fromWarehouseId.lowercase().contains(q) ||
                s.toWarehouseId.lowercase().contains(q)
            val matchesStatus = statusFilter == null || s.status == statusFilter
            matchesQuery && matchesStatus
        }
    }
}
