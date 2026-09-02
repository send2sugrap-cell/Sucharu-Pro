package com.sucharu.sucharupro.ui.features.delivery

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sucharu.sucharupro.domain.model.delivery.DeliveryDispatchRequest
import com.sucharu.sucharupro.domain.model.delivery.DeliveryPriority
import com.sucharu.sucharupro.domain.model.delivery.DispatchRequestStatus
import com.sucharu.sucharupro.domain.repository.DeliveryOrderRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel for Dispatch Request list screen (Module 08 Step 01).
 */
class DispatchRequestListViewModel(
    private val repository: DeliveryOrderRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DispatchRequestListUiState(isLoading = true))
    val uiState: StateFlow<DispatchRequestListUiState> = _uiState.asStateFlow()

    fun loadDispatchRequests(projectId: String) {
        _uiState.update { it.copy(isLoading = true, projectId = projectId, errorMessage = null) }
        viewModelScope.launch {
            repository.observeDispatchRequests(projectId).collect { list ->
                _uiState.update { current ->
                    val filtered = applyFilters(
                        requests = list,
                        status = current.selectedStatusFilter,
                        priority = current.selectedPriorityFilter
                    )
                    current.copy(
                        isLoading = false,
                        dispatchRequests = list,
                        filteredRequests = filtered
                    )
                }
            }
        }
    }

    fun onStatusFilterChanged(status: DispatchRequestStatus?) {
        _uiState.update { current ->
            val filtered = applyFilters(
                requests = current.dispatchRequests,
                status = status,
                priority = current.selectedPriorityFilter
            )
            current.copy(selectedStatusFilter = status, filteredRequests = filtered)
        }
    }

    fun onPriorityFilterChanged(priority: DeliveryPriority?) {
        _uiState.update { current ->
            val filtered = applyFilters(
                requests = current.dispatchRequests,
                status = current.selectedStatusFilter,
                priority = priority
            )
            current.copy(selectedPriorityFilter = priority, filteredRequests = filtered)
        }
    }

    private fun applyFilters(
        requests: List<DeliveryDispatchRequest>,
        status: DispatchRequestStatus?,
        priority: DeliveryPriority?
    ): List<DeliveryDispatchRequest> {
        return requests.filter { req ->
            val matchesStatus = status == null || req.status == status
            val matchesPriority = priority == null || req.priority == priority
            matchesStatus && matchesPriority
        }
    }
}
