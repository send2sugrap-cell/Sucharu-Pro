package com.sucharu.sucharupro.ui.features.inventory.product

import com.sucharu.sucharupro.domain.model.inventory.InventoryProduct
import com.sucharu.sucharupro.domain.model.inventory.InventoryProductCategory
import com.sucharu.sucharupro.domain.model.inventory.InventoryProductType

/**
 * UI State for the Inventory Product List Screen (Module 07 Step 01).
 */
data class InventoryProductListUiState(
    val isLoading: Boolean = false,
    val products: List<InventoryProduct> = emptyList(),
    val filteredProducts: List<InventoryProduct> = emptyList(),
    val categories: List<InventoryProductCategory> = emptyList(),
    val searchQuery: String = "",
    val selectedCategoryFilter: String? = null,
    val selectedTypeFilter: InventoryProductType? = null,
    val showInactiveOnly: Boolean = false,
    val errorMessage: String? = null
)
