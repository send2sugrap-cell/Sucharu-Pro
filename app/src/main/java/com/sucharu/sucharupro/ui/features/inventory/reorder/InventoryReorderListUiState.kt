package com.sucharu.sucharupro.ui.features.inventory.reorder

import com.sucharu.sucharupro.domain.model.inventory.reorder.InventoryReorderAlert
import com.sucharu.sucharupro.domain.model.inventory.reorder.InventoryReorderAlertStatus

/**
 * UI state for the reorder alert list screen (Module 07 Step 08).
 */
data class InventoryReorderListUiState(
    val isLoading: Boolean = false,
    val projectId: String = "",
    val alerts: List<InventoryReorderAlert> = emptyList(),
    val filteredAlerts: List<InventoryReorderAlert> = emptyList(),
    val searchQuery: String = "",
    val selectedStatusFilter: InventoryReorderAlertStatus? = null,
    val errorMessage: String? = null
)
