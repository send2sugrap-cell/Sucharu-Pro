package com.sucharu.sucharupro.ui.features.delivery.challan

import com.sucharu.sucharupro.domain.model.delivery.DeliveryOrder
import com.sucharu.sucharupro.domain.model.delivery.DeliveryOrderLine
import com.sucharu.sucharupro.domain.model.delivery.challan.DeliveryChallan
import com.sucharu.sucharupro.domain.model.delivery.challan.DeliveryChallanActivityEvent
import com.sucharu.sucharupro.domain.model.delivery.challan.DeliveryChallanLine

/**
 * UI State for Delivery Challan Details screen (Module 08 Step 02).
 */
data class DeliveryChallanDetailsUiState(
    val isLoading: Boolean = false,
    val isActionInProgress: Boolean = false,
    val challan: DeliveryChallan? = null,
    val lines: List<DeliveryChallanLine> = emptyList(),
    val deliveryOrder: DeliveryOrder? = null,
    val deliveryOrderLines: List<DeliveryOrderLine> = emptyList(),
    val activityEvents: List<DeliveryChallanActivityEvent> = emptyList(),
    val errorMessage: String? = null,
    val successMessage: String? = null
)
