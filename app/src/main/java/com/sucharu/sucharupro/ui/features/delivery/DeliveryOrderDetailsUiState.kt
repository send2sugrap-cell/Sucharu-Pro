package com.sucharu.sucharupro.ui.features.delivery

import com.sucharu.sucharupro.domain.model.delivery.DeliveryActivityEvent
import com.sucharu.sucharupro.domain.model.delivery.DeliveryDispatchRequest
import com.sucharu.sucharupro.domain.model.delivery.DeliveryOrder
import com.sucharu.sucharupro.domain.model.delivery.DeliveryOrderLine

/**
 * UI State for the Delivery Order Details screen (Module 08 Step 01).
 */
data class DeliveryOrderDetailsUiState(
    val isLoading: Boolean = false,
    val isActionInProgress: Boolean = false,
    val order: DeliveryOrder? = null,
    val lines: List<DeliveryOrderLine> = emptyList(),
    val dispatchRequest: DeliveryDispatchRequest? = null,
    val activityEvents: List<DeliveryActivityEvent> = emptyList(),
    val errorMessage: String? = null,
    val successMessage: String? = null
)
