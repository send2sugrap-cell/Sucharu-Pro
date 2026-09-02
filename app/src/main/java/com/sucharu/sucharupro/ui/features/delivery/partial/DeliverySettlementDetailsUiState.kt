package com.sucharu.sucharupro.ui.features.delivery.partial

import com.sucharu.sucharupro.domain.model.delivery.partial.DeliveryPartialSettlement
import com.sucharu.sucharupro.domain.model.delivery.partial.DeliveryPartialSettlementLine
import com.sucharu.sucharupro.domain.model.delivery.partial.DeliverySettlementEvent
import com.sucharu.sucharupro.domain.model.delivery.partial.DeliverySplitDispatch

/**
 * UI State for Delivery Settlement Details (Module 08 Step 06).
 */
data class DeliverySettlementDetailsUiState(
    val isLoading: Boolean = false,
    val isActionInProgress: Boolean = false,
    val settlement: DeliveryPartialSettlement? = null,
    val lines: List<DeliveryPartialSettlementLine> = emptyList(),
    val splitDispatches: List<DeliverySplitDispatch> = emptyList(),
    val events: List<DeliverySettlementEvent> = emptyList(),
    val errorMessage: String? = null,
    val successMessage: String? = null
)
