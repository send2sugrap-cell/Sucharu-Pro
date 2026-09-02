package com.sucharu.sucharupro.ui.features.delivery.verification

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sucharu.sucharupro.domain.model.delivery.verification.DeliveryItemVerification
import com.sucharu.sucharupro.domain.model.delivery.verification.DeliveryItemVerificationStatus
import com.sucharu.sucharupro.domain.repository.DeliveryItemVerificationRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel for Delivery Item Verification List (Module 08 Step 04).
 */
class DeliveryItemVerificationListViewModel(
    private val repository: DeliveryItemVerificationRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DeliveryItemVerificationListUiState(isLoading = true))
    val uiState: StateFlow<DeliveryItemVerificationListUiState> = _uiState.asStateFlow()

    fun loadVerifications(projectId: String) {
        _uiState.update { it.copy(isLoading = true, projectId = projectId, errorMessage = null) }
        viewModelScope.launch {
            repository.observeVerifications(projectId).collect { list ->
                _uiState.update { current ->
                    val filtered = applyFilters(
                        verifications = list,
                        query = current.searchQuery,
                        status = current.selectedStatusFilter
                    )
                    current.copy(
                        isLoading = false,
                        verifications = list,
                        filteredVerifications = filtered
                    )
                }
            }
        }
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.update { current ->
            val filtered = applyFilters(
                verifications = current.verifications,
                query = query,
                status = current.selectedStatusFilter
            )
            current.copy(searchQuery = query, filteredVerifications = filtered)
        }
    }

    fun onStatusFilterChanged(status: DeliveryItemVerificationStatus?) {
        _uiState.update { current ->
            val filtered = applyFilters(
                verifications = current.verifications,
                query = current.searchQuery,
                status = status
            )
            current.copy(selectedStatusFilter = status, filteredVerifications = filtered)
        }
    }

    private fun applyFilters(
        verifications: List<DeliveryItemVerification>,
        query: String,
        status: DeliveryItemVerificationStatus?
    ): List<DeliveryItemVerification> {
        return verifications.filter { item ->
            val matchesQuery = query.isBlank() ||
                item.verificationNo.contains(query, ignoreCase = true) ||
                item.dispatchExecutionId.contains(query, ignoreCase = true) ||
                item.deliveryChallanId.contains(query, ignoreCase = true) ||
                item.deliveryOrderId.contains(query, ignoreCase = true) ||
                (item.remarks?.contains(query, ignoreCase = true) == true)

            val matchesStatus = status == null || item.status == status

            matchesQuery && matchesStatus
        }
    }
}
