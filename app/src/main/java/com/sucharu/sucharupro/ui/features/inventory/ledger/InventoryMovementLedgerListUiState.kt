package com.sucharu.sucharupro.ui.features.inventory.ledger

import com.sucharu.sucharupro.domain.model.inventory.ledger.InventoryMovementLedgerEntry
import com.sucharu.sucharupro.domain.model.inventory.ledger.InventoryMovementLedgerType

/**
 * UI state for the inventory movement ledger list screen (Module 07 Step 09).
 */
data class InventoryMovementLedgerListUiState(
    val isLoading: Boolean = false,
    val projectId: String = "",
    val entries: List<InventoryMovementLedgerEntry> = emptyList(),
    val filteredEntries: List<InventoryMovementLedgerEntry> = emptyList(),
    val searchQuery: String = "",
    val selectedTypeFilter: InventoryMovementLedgerType? = null,
    val selectedProductId: String? = null,
    val selectedLocationId: String? = null,
    val errorMessage: String? = null
)
