package com.sucharu.sucharupro.ui.features.delivery.partial

import com.sucharu.sucharupro.domain.model.delivery.partial.DeliveryPartialSettlement
import com.sucharu.sucharupro.domain.model.delivery.partial.DeliveryPartialSettlementSummary
import com.sucharu.sucharupro.domain.model.delivery.partial.DeliverySettlementStatus

/**
 * UI State for Delivery Settlement List (Module 08 Step 06).
 */
data class DeliverySettlementListUiState(
    val isLoading: Boolean = false,
    val projectId: String = "",
    val settlements: List<DeliveryPartialSettlement> = emptyList(),
    val filteredSettlements: List<DeliveryPartialSettlement> = emptyList(),
    val summary: DeliveryPartialSettlementSummary = DeliveryPartialSettlementSummary(),
    val searchQuery: String = "",
    val selectedStatusFilter: DeliverySettlementStatus? = null,
    val errorMessage: String? = null
)
