package com.sucharu.sucharupro.ui.features.delivery.partial

import com.sucharu.sucharupro.domain.model.delivery.partial.DeliveryPartialSettlement
import com.sucharu.sucharupro.domain.model.delivery.partial.DeliveryPartialSettlementLine

/**
 * UI State for Recording Partial Delivery (Module 08 Step 06).
 */
data class DeliveryPartialDeliveryFormUiState(
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val settlementId: String = "",
    val settlement: DeliveryPartialSettlement? = null,
    val lines: List<DeliveryPartialSettlementLine> = emptyList(),
    val selectedDoLineId: String = "",
    val quantityToDeliverText: String = "",
    val errorMessage: String? = null,
    val isSavedSuccessfully: Boolean = false
)
