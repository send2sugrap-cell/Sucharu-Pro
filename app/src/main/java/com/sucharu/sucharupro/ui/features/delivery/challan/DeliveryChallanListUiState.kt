package com.sucharu.sucharupro.ui.features.delivery.challan

import com.sucharu.sucharupro.domain.model.delivery.challan.DeliveryChallan
import com.sucharu.sucharupro.domain.model.delivery.challan.DeliveryChallanStatus
import com.sucharu.sucharupro.domain.model.delivery.challan.DeliveryChallanType

/**
 * UI State for Delivery Challan List (Module 08 Step 02).
 */
data class DeliveryChallanListUiState(
    val isLoading: Boolean = false,
    val projectId: String = "",
    val challans: List<DeliveryChallan> = emptyList(),
    val filteredChallans: List<DeliveryChallan> = emptyList(),
    val searchQuery: String = "",
    val selectedStatusFilter: DeliveryChallanStatus? = null,
    val selectedTypeFilter: DeliveryChallanType? = null,
    val errorMessage: String? = null
)
