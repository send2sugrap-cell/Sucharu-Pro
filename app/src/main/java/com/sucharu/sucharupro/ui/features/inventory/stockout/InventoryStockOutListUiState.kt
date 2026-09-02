package com.sucharu.sucharupro.ui.features.inventory.stockout

import com.sucharu.sucharupro.domain.model.inventory.stockout.InventoryStockOut
import com.sucharu.sucharupro.domain.model.inventory.stockout.InventoryStockOutStatus

/**
 * UI state for the stock-out list screen (Module 07 Step 04).
 */
data class InventoryStockOutListUiState(
    val isLoading: Boolean = false,
    val projectId: String = "",
    val stockOuts: List<InventoryStockOut> = emptyList(),
    val filteredStockOuts: List<InventoryStockOut> = emptyList(),
    val searchQuery: String = "",
    val selectedStatusFilter: InventoryStockOutStatus? = null,
    val errorMessage: String? = null
)
