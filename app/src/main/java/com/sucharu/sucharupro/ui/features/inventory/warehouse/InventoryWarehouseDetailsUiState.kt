package com.sucharu.sucharupro.ui.features.inventory.warehouse

import com.sucharu.sucharupro.domain.model.inventory.InventoryLocation
import com.sucharu.sucharupro.domain.model.inventory.InventoryWarehouse

/**
 * UI state for warehouse details view (Module 07 Step 02).
 */
data class InventoryWarehouseDetailsUiState(
    val isLoading: Boolean = false,
    val warehouse: InventoryWarehouse? = null,
    val locations: List<InventoryLocation> = emptyList(),
    val errorMessage: String? = null
)
