package com.sucharu.sucharupro.ui.features.inventory.stocktransfer

import com.sucharu.sucharupro.domain.model.inventory.stocktransfer.InventoryStockTransfer
import com.sucharu.sucharupro.domain.model.inventory.stocktransfer.InventoryStockTransferActivityEvent
import com.sucharu.sucharupro.domain.model.inventory.stocktransfer.InventoryStockTransferLine
import com.sucharu.sucharupro.domain.model.inventory.stocktransfer.InventoryStockTransferRecord

/**
 * UI state for the stock transfer details screen (Module 07 Step 05).
 */
data class InventoryStockTransferDetailsUiState(
    val isLoading: Boolean = false,
    val transfer: InventoryStockTransfer? = null,
    val lines: List<InventoryStockTransferLine> = emptyList(),
    val transferRecords: List<InventoryStockTransferRecord> = emptyList(),
    val auditEvents: List<InventoryStockTransferActivityEvent> = emptyList(),
    val errorMessage: String? = null,
    val operationMessage: String? = null
)
