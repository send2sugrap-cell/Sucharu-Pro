package com.sucharu.sucharupro.ui.features.delivery.partial

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.delivery.partial.DeliveryPartialSettlement
import com.sucharu.sucharupro.domain.model.delivery.partial.DeliverySettlementStatus
import com.sucharu.sucharupro.domain.repository.DeliveryPartialSettlementRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel for Delivery Settlement List & Filtering (Module 08 Step 06).
 */
class DeliverySettlementListViewModel(
    private val repository: DeliveryPartialSettlementRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DeliverySettlementListUiState(isLoading = true))
    val uiState: StateFlow<DeliverySettlementListUiState> = _uiState.asStateFlow()

    fun loadSettlements(projectId: String) {
        _uiState.update { it.copy(isLoading = true, projectId = projectId, errorMessage = null) }
        viewModelScope.launch {
            repository.observeSettlements(projectId).collect { list ->
                val summaryResult = repository.getSettlementSummary(projectId)
                val summary = if (summaryResult is DomainResult.Success) summaryResult.data else _uiState.value.summary

                _uiState.update { current ->
                    val filtered = applyFilters(
                        settlements = list,
                        query = current.searchQuery,
                        status = current.selectedStatusFilter
                    )
                    current.copy(
                        isLoading = false,
                        settlements = list,
                        filteredSettlements = filtered,
                        summary = summary
                    )
                }
            }
        }
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.update { current ->
            val filtered = applyFilters(
                settlements = current.settlements,
                query = query,
                status = current.selectedStatusFilter
            )
            current.copy(searchQuery = query, filteredSettlements = filtered)
        }
    }

    fun onStatusFilterChanged(status: DeliverySettlementStatus?) {
        _uiState.update { current ->
            val filtered = applyFilters(
                settlements = current.settlements,
                query = current.searchQuery,
                status = status
            )
            current.copy(selectedStatusFilter = status, filteredSettlements = filtered)
        }
    }

    private fun applyFilters(
        settlements: List<DeliveryPartialSettlement>,
        query: String,
        status: DeliverySettlementStatus?
    ): List<DeliveryPartialSettlement> {
        return settlements.filter { item ->
            val matchesQuery = query.isBlank() ||
                item.deliveryOrderId.contains(query, ignoreCase = true) ||
                (item.customerId?.contains(query, ignoreCase = true) == true)

            val matchesStatus = status == null || item.status == status

            matchesQuery && matchesStatus
        }
    }
}
