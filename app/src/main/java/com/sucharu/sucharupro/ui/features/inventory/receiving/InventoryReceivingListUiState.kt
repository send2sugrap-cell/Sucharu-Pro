package com.sucharu.sucharupro.ui.features.inventory.receiving

import com.sucharu.sucharupro.domain.model.inventory.receiving.InventoryReceiving
import com.sucharu.sucharupro.domain.model.inventory.receiving.InventoryReceivingStatus

/**
 * UI state for the receiving list screen (Module 07 Step 03).
 */
data class InventoryReceivingListUiState(
    val isLoading: Boolean = false,
    val projectId: String = "",
    val receivings: List<InventoryReceiving> = emptyList(),
    val filteredReceivings: List<InventoryReceiving> = emptyList(),
    val searchQuery: String = "",
    val selectedStatusFilter: InventoryReceivingStatus? = null,
    val errorMessage: String? = null
)
