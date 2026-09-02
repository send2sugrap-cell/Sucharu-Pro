package com.sucharu.sucharupro.ui.features.inventory.ledger

/**
 * UI state for the Inventory Balance screen (Module 07 Step 09).
 */
data class InventoryInventoryBalanceUiState(
    val isLoading: Boolean = false,
    val projectId: String = "",
    val balances: List<ProductLocationBalance> = emptyList(),
    val filteredBalances: List<ProductLocationBalance> = emptyList(),
    val searchQuery: String = "",
    val errorMessage: String? = null
)

/**
 * View model for product-location balance summary.
 */
data class ProductLocationBalance(
    val productId: String,
    val locationId: String,
    val openingBalance: Double = 0.0,
    val totalIn: Double = 0.0,
    val totalOut: Double = 0.0,
    val closingBalance: Double = 0.0
)
