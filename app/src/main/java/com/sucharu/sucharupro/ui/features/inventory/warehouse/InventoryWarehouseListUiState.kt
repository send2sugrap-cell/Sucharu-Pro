package com.sucharu.sucharupro.ui.features.inventory.warehouse

import com.sucharu.sucharupro.domain.model.inventory.InventoryWarehouse
import com.sucharu.sucharupro.domain.model.inventory.InventoryWarehouseStatus
import com.sucharu.sucharupro.domain.model.inventory.InventoryWarehouseType

/**
 * UI state for physical warehouse list screen (Module 07 Step 02).
 */
data class InventoryWarehouseListUiState(
    val isLoading: Boolean = false,
    val projectId: String = "",
    val warehouses: List<InventoryWarehouse> = emptyList(),
    val filteredWarehouses: List<InventoryWarehouse> = emptyList(),
    val searchQuery: String = "",
    val selectedTypeFilter: InventoryWarehouseType? = null,
    val selectedStatusFilter: InventoryWarehouseStatus? = null,
    val errorMessage: String? = null
)
