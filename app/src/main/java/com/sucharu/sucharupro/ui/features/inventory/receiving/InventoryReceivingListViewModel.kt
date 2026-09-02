package com.sucharu.sucharupro.ui.features.inventory.receiving

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.inventory.receiving.InventoryReceiving
import com.sucharu.sucharupro.domain.model.inventory.receiving.InventoryReceivingStatus
import com.sucharu.sucharupro.domain.model.user.UserRole
import com.sucharu.sucharupro.domain.repository.InventoryReceivingRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel orchestrating the receiving list screen (Module 07 Step 03).
 *
 * Provides reactive filtered observation with status filter and search query support.
 * Only displays actions permitted by the user's role.
 */
class InventoryReceivingListViewModel(
    private val repository: InventoryReceivingRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(InventoryReceivingListUiState(isLoading = true))
    val uiState: StateFlow<InventoryReceivingListUiState> = _uiState.asStateFlow()

    fun loadReceivings(projectId: String) {
        _uiState.update { it.copy(isLoading = true, projectId = projectId, errorMessage = null) }
        viewModelScope.launch {
            repository.observeReceivings(projectId).collect { list ->
                _uiState.update { current ->
                    val filtered = applyFilters(
                        receivings = list,
                        query = current.searchQuery,
                        statusFilter = current.selectedStatusFilter
                    )
                    current.copy(
                        isLoading = false,
                        receivings = list,
                        filteredReceivings = filtered
                    )
                }
            }
        }
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.update { current ->
            val filtered = applyFilters(
                receivings = current.receivings,
                query = query,
                statusFilter = current.selectedStatusFilter
            )
            current.copy(searchQuery = query, filteredReceivings = filtered)
        }
    }

    fun onStatusFilterChanged(status: InventoryReceivingStatus?) {
        _uiState.update { current ->
            val filtered = applyFilters(
                receivings = current.receivings,
                query = current.searchQuery,
                statusFilter = status
            )
            current.copy(selectedStatusFilter = status, filteredReceivings = filtered)
        }
    }

    fun cancelReceiving(
        receivingId: String,
        actorId: String,
        timestamp: String,
        role: UserRole = UserRole.MANAGER
    ) {
        viewModelScope.launch {
            val res = repository.cancelReceiving(receivingId, actorId, timestamp, role)
            if (res is DomainResult.Error) {
                _uiState.update { it.copy(errorMessage = res.message) }
            }
        }
    }

    private fun applyFilters(
        receivings: List<InventoryReceiving>,
        query: String,
        statusFilter: InventoryReceivingStatus?
    ): List<InventoryReceiving> {
        val q = query.trim().lowercase()
        return receivings.filter { r ->
            val matchesQuery = q.isBlank() ||
                r.receivingReference.lowercase().contains(q) ||
                r.warehouseId.lowercase().contains(q)
            val matchesStatus = statusFilter == null || r.status == statusFilter
            matchesQuery && matchesStatus
        }
    }
}
