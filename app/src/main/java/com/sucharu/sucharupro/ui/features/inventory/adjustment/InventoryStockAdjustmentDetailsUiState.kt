package com.sucharu.sucharupro.ui.features.inventory.adjustment

import com.sucharu.sucharupro.domain.model.inventory.adjustment.InventoryStockAdjustment
import com.sucharu.sucharupro.domain.model.inventory.adjustment.InventoryStockAdjustmentActivityEvent
import com.sucharu.sucharupro.domain.model.inventory.adjustment.InventoryStockAdjustmentLine
import com.sucharu.sucharupro.domain.model.inventory.adjustment.InventoryStockAdjustmentRecord

/**
 * UI state for the stock adjustment details screen (Module 07 Step 06).
 */
data class InventoryStockAdjustmentDetailsUiState(
    val isLoading: Boolean = false,
    val adjustment: InventoryStockAdjustment? = null,
    val lines: List<InventoryStockAdjustmentLine> = emptyList(),
    val adjustmentRecords: List<InventoryStockAdjustmentRecord> = emptyList(),
    val auditEvents: List<InventoryStockAdjustmentActivityEvent> = emptyList(),
    val errorMessage: String? = null,
    val operationMessage: String? = null
)
