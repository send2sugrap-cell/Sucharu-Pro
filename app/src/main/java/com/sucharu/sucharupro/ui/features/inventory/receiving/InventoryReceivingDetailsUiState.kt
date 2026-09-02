package com.sucharu.sucharupro.ui.features.inventory.receiving

import com.sucharu.sucharupro.domain.model.inventory.receiving.InventoryReceiving
import com.sucharu.sucharupro.domain.model.inventory.receiving.InventoryReceivingActivityEvent
import com.sucharu.sucharupro.domain.model.inventory.receiving.InventoryReceivingLine
import com.sucharu.sucharupro.domain.model.inventory.receiving.InventoryStockInRecord

/**
 * UI state for the receiving details screen (Module 07 Step 03).
 */
data class InventoryReceivingDetailsUiState(
    val isLoading: Boolean = false,
    val receiving: InventoryReceiving? = null,
    val lines: List<InventoryReceivingLine> = emptyList(),
    val stockInRecords: List<InventoryStockInRecord> = emptyList(),
    val auditEvents: List<InventoryReceivingActivityEvent> = emptyList(),
    val errorMessage: String? = null,
    val operationMessage: String? = null
)
