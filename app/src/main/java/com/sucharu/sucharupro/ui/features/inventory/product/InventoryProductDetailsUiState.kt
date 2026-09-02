package com.sucharu.sucharupro.ui.features.inventory.product

import com.sucharu.sucharupro.domain.model.inventory.InventoryProduct
import com.sucharu.sucharupro.domain.model.inventory.InventoryProductCategory
import com.sucharu.sucharupro.domain.model.inventory.InventoryStockIdentity

/**
 * UI State for the Product Master Details Screen (Module 07 Step 01).
 */
data class InventoryProductDetailsUiState(
    val isLoading: Boolean = false,
    val product: InventoryProduct? = null,
    val category: InventoryProductCategory? = null,
    val stockIdentity: InventoryStockIdentity? = null,
    val errorMessage: String? = null
)
