package com.sucharu.sucharupro.ui.features.delivery.returning

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sucharu.sucharupro.domain.model.delivery.returning.DeliveryReturn
import com.sucharu.sucharupro.domain.model.delivery.returning.DeliveryReturnStatus
import com.sucharu.sucharupro.domain.model.delivery.returning.DeliveryReturnType
import com.sucharu.sucharupro.domain.repository.DeliveryReturnRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class DeliveryReturnListViewModel(
    private val repository: DeliveryReturnRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DeliveryReturnListUiState())
    val uiState: StateFlow<DeliveryReturnListUiState> = _uiState.asStateFlow()

    fun loadReturns(projectId: String) {
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            repository.observeReturns(projectId)
                .catch { ex -> _uiState.update { it.copy(isLoading = false, errorMessage = ex.message) } }
                .collect { list ->
                    _uiState.update { state ->
                        state.copy(
                            isLoading = false,
                            returns = list,
                            filteredReturns = applyFilters(list, state.searchQuery, state.selectedStatusFilter, state.selectedTypeFilter)
                        )
                    }
                }
        }
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.update { state ->
            state.copy(
                searchQuery = query,
                filteredReturns = applyFilters(state.returns, query, state.selectedStatusFilter, state.selectedTypeFilter)
            )
        }
    }

    fun onStatusFilterSelected(status: DeliveryReturnStatus?) {
        _uiState.update { state ->
            state.copy(
                selectedStatusFilter = status,
                filteredReturns = applyFilters(state.returns, state.searchQuery, status, state.selectedTypeFilter)
            )
        }
    }

    fun onTypeFilterSelected(type: DeliveryReturnType?) {
        _uiState.update { state ->
            state.copy(
                selectedTypeFilter = type,
                filteredReturns = applyFilters(state.returns, state.searchQuery, state.selectedStatusFilter, type)
            )
        }
    }

    private fun applyFilters(
        list: List<DeliveryReturn>,
        query: String,
        status: DeliveryReturnStatus?,
        type: DeliveryReturnType?
    ): List<DeliveryReturn> {
        return list.filter { item ->
            val matchesQuery = query.isBlank() ||
                    item.returnNo.contains(query, ignoreCase = true) ||
                    item.deliveryOrderId.contains(query, ignoreCase = true) ||
                    (item.customerId?.contains(query, ignoreCase = true) == true)

            val matchesStatus = status == null || item.status == status
            val matchesType = type == null || item.returnType == type

            matchesQuery && matchesStatus && matchesType
        }
    }
}
