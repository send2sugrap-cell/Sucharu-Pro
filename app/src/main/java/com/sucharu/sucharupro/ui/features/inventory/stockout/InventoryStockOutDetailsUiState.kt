package com.sucharu.sucharupro.ui.features.inventory.stockout

import com.sucharu.sucharupro.domain.model.inventory.stockout.InventoryStockOut
import com.sucharu.sucharupro.domain.model.inventory.stockout.InventoryStockOutActivityEvent
import com.sucharu.sucharupro.domain.model.inventory.stockout.InventoryStockOutLine
import com.sucharu.sucharupro.domain.model.inventory.stockout.InventoryStockOutRecord

/**
 * UI state for the stock-out details screen (Module 07 Step 04).
 */
data class InventoryStockOutDetailsUiState(
    val isLoading: Boolean = false,
    val stockOut: InventoryStockOut? = null,
    val lines: List<InventoryStockOutLine> = emptyList(),
    val stockOutRecords: List<InventoryStockOutRecord> = emptyList(),
    val auditEvents: List<InventoryStockOutActivityEvent> = emptyList(),
    val errorMessage: String? = null,
    val operationMessage: String? = null
)
