package com.sucharu.sucharupro.ui.features.inventory.ledger

import com.sucharu.sucharupro.domain.model.inventory.ledger.InventoryValuationMethod
import com.sucharu.sucharupro.domain.model.inventory.ledger.InventoryValuationSnapshot

/**
 * UI state for the Inventory Valuation Summary screen (Module 07 Step 09).
 */
data class InventoryValuationSummaryUiState(
    val isLoading: Boolean = false,
    val projectId: String = "",
    val snapshots: List<InventoryValuationSnapshot> = emptyList(),
    val selectedMethod: InventoryValuationMethod = InventoryValuationMethod.FIFO,
    val totalInventoryValue: Double = 0.0,
    val hasFinancialAccess: Boolean = false,
    val errorMessage: String? = null
)
