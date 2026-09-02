package com.sucharu.sucharupro.ui.features.delivery.challan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sucharu.sucharupro.domain.model.delivery.challan.DeliveryChallan
import com.sucharu.sucharupro.domain.model.delivery.challan.DeliveryChallanStatus
import com.sucharu.sucharupro.domain.model.delivery.challan.DeliveryChallanType
import com.sucharu.sucharupro.domain.repository.DeliveryChallanRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel for Delivery Challan List screen (Module 08 Step 02).
 */
class DeliveryChallanListViewModel(
    private val repository: DeliveryChallanRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DeliveryChallanListUiState(isLoading = true))
    val uiState: StateFlow<DeliveryChallanListUiState> = _uiState.asStateFlow()

    fun loadChallans(projectId: String) {
        _uiState.update { it.copy(isLoading = true, projectId = projectId, errorMessage = null) }
        viewModelScope.launch {
            repository.observeChallans(projectId).collect { list ->
                _uiState.update { current ->
                    val filtered = applyFilters(
                        challans = list,
                        query = current.searchQuery,
                        status = current.selectedStatusFilter,
                        type = current.selectedTypeFilter
                    )
                    current.copy(
                        isLoading = false,
                        challans = list,
                        filteredChallans = filtered
                    )
                }
            }
        }
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.update { current ->
            val filtered = applyFilters(
                challans = current.challans,
                query = query,
                status = current.selectedStatusFilter,
                type = current.selectedTypeFilter
            )
            current.copy(searchQuery = query, filteredChallans = filtered)
        }
    }

    fun onStatusFilterChanged(status: DeliveryChallanStatus?) {
        _uiState.update { current ->
            val filtered = applyFilters(
                challans = current.challans,
                query = current.searchQuery,
                status = status,
                type = current.selectedTypeFilter
            )
            current.copy(selectedStatusFilter = status, filteredChallans = filtered)
        }
    }

    fun onTypeFilterChanged(type: DeliveryChallanType?) {
        _uiState.update { current ->
            val filtered = applyFilters(
                challans = current.challans,
                query = current.searchQuery,
                status = current.selectedStatusFilter,
                type = type
            )
            current.copy(selectedTypeFilter = type, filteredChallans = filtered)
        }
    }

    private fun applyFilters(
        challans: List<DeliveryChallan>,
        query: String,
        status: DeliveryChallanStatus?,
        type: DeliveryChallanType?
    ): List<DeliveryChallan> {
        return challans.filter { challan ->
            val matchesQuery = query.isBlank() ||
                challan.challanNo.contains(query, ignoreCase = true) ||
                challan.deliveryOrderId.contains(query, ignoreCase = true) ||
                (challan.customerId?.contains(query, ignoreCase = true) == true) ||
                (challan.notes?.contains(query, ignoreCase = true) == true)

            val matchesStatus = status == null || challan.status == status
            val matchesType = type == null || challan.challanType == type

            matchesQuery && matchesStatus && matchesType
        }
    }
}
