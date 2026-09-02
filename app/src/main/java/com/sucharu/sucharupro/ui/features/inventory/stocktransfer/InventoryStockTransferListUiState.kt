package com.sucharu.sucharupro.ui.features.inventory.stocktransfer

import com.sucharu.sucharupro.domain.model.inventory.stocktransfer.InventoryStockTransfer
import com.sucharu.sucharupro.domain.model.inventory.stocktransfer.InventoryStockTransferStatus

/**
 * UI state for the stock transfer list screen (Module 07 Step 05).
 */
data class InventoryStockTransferListUiState(
    val isLoading: Boolean = false,
    val projectId: String = "",
    val transfers: List<InventoryStockTransfer> = emptyList(),
    val filteredTransfers: List<InventoryStockTransfer> = emptyList(),
    val searchQuery: String = "",
    val selectedStatusFilter: InventoryStockTransferStatus? = null,
    val errorMessage: String? = null
)
