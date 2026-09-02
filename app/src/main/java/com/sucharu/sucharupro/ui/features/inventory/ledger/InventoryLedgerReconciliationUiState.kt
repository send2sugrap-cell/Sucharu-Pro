package com.sucharu.sucharupro.ui.features.inventory.ledger

import com.sucharu.sucharupro.domain.model.inventory.ledger.InventoryLedgerReconciliationResult

/**
 * UI state for the Inventory Ledger Reconciliation screen (Module 07 Step 09).
 */
data class InventoryLedgerReconciliationUiState(
    val isLoading: Boolean = false,
    val projectId: String = "",
    val results: List<InventoryLedgerReconciliationResult> = emptyList(),
    val filteredResults: List<InventoryLedgerReconciliationResult> = emptyList(),
    val searchQuery: String = "",
    val errorMessage: String? = null
)
