package com.sucharu.sucharupro.ui.features.inventory.adjustment

import com.sucharu.sucharupro.domain.model.inventory.adjustment.InventoryStockAdjustment
import com.sucharu.sucharupro.domain.model.inventory.adjustment.InventoryStockAdjustmentStatus

/**
 * UI state for the stock adjustment list screen (Module 07 Step 06).
 */
data class InventoryStockAdjustmentListUiState(
    val isLoading: Boolean = false,
    val projectId: String = "",
    val adjustments: List<InventoryStockAdjustment> = emptyList(),
    val filteredAdjustments: List<InventoryStockAdjustment> = emptyList(),
    val searchQuery: String = "",
    val selectedStatusFilter: InventoryStockAdjustmentStatus? = null,
    val errorMessage: String? = null
)
