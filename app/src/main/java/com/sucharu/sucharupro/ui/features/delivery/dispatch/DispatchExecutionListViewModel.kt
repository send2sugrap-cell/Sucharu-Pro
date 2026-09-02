package com.sucharu.sucharupro.ui.features.delivery.dispatch

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sucharu.sucharupro.domain.model.delivery.dispatch.DispatchExecution
import com.sucharu.sucharupro.domain.model.delivery.dispatch.DispatchExecutionStatus
import com.sucharu.sucharupro.domain.model.delivery.dispatch.DispatchExecutionType
import com.sucharu.sucharupro.domain.repository.DispatchExecutionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel for Dispatch Execution List (Module 08 Step 03).
 */
class DispatchExecutionListViewModel(
    private val repository: DispatchExecutionRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DispatchExecutionListUiState(isLoading = true))
    val uiState: StateFlow<DispatchExecutionListUiState> = _uiState.asStateFlow()

    fun loadDispatches(projectId: String) {
        _uiState.update { it.copy(isLoading = true, projectId = projectId, errorMessage = null) }
        viewModelScope.launch {
            repository.observeDispatches(projectId).collect { list ->
                _uiState.update { current ->
                    val filtered = applyFilters(
                        dispatches = list,
                        query = current.searchQuery,
                        status = current.selectedStatusFilter,
                        type = current.selectedTypeFilter
                    )
                    current.copy(
                        isLoading = false,
                        dispatches = list,
                        filteredDispatches = filtered
                    )
                }
            }
        }
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.update { current ->
            val filtered = applyFilters(
                dispatches = current.dispatches,
                query = query,
                status = current.selectedStatusFilter,
                type = current.selectedTypeFilter
            )
            current.copy(searchQuery = query, filteredDispatches = filtered)
        }
    }

    fun onStatusFilterChanged(status: DispatchExecutionStatus?) {
        _uiState.update { current ->
            val filtered = applyFilters(
                dispatches = current.dispatches,
                query = current.searchQuery,
                status = status,
                type = current.selectedTypeFilter
            )
            current.copy(selectedStatusFilter = status, filteredDispatches = filtered)
        }
    }

    fun onTypeFilterChanged(type: DispatchExecutionType?) {
        _uiState.update { current ->
            val filtered = applyFilters(
                dispatches = current.dispatches,
                query = current.searchQuery,
                status = current.selectedStatusFilter,
                type = type
            )
            current.copy(selectedTypeFilter = type, filteredDispatches = filtered)
        }
    }

    private fun applyFilters(
        dispatches: List<DispatchExecution>,
        query: String,
        status: DispatchExecutionStatus?,
        type: DispatchExecutionType?
    ): List<DispatchExecution> {
        return dispatches.filter { dispatch ->
            val matchesQuery = query.isBlank() ||
                dispatch.dispatchNo.contains(query, ignoreCase = true) ||
                dispatch.deliveryChallanId.contains(query, ignoreCase = true) ||
                dispatch.deliveryOrderId.contains(query, ignoreCase = true) ||
                (dispatch.customerId?.contains(query, ignoreCase = true) == true) ||
                (dispatch.notes?.contains(query, ignoreCase = true) == true)

            val matchesStatus = status == null || dispatch.status == status
            val matchesType = type == null || dispatch.dispatchType == type

            matchesQuery && matchesStatus && matchesType
        }
    }
}
