package com.sucharu.sucharupro.ui.features.delivery

import com.sucharu.sucharupro.domain.model.delivery.DeliveryDispatchRequest
import com.sucharu.sucharupro.domain.model.delivery.DeliveryPriority
import com.sucharu.sucharupro.domain.model.delivery.DispatchRequestStatus

/**
 * UI State for Dispatch Request List (Module 08 Step 01).
 */
data class DispatchRequestListUiState(
    val isLoading: Boolean = false,
    val projectId: String = "",
    val dispatchRequests: List<DeliveryDispatchRequest> = emptyList(),
    val filteredRequests: List<DeliveryDispatchRequest> = emptyList(),
    val selectedStatusFilter: DispatchRequestStatus? = null,
    val selectedPriorityFilter: DeliveryPriority? = null,
    val errorMessage: String? = null
)
