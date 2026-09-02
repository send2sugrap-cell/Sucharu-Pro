package com.sucharu.sucharupro.ui.features.inventory.location

import com.sucharu.sucharupro.domain.model.inventory.InventoryLocation
import com.sucharu.sucharupro.domain.model.inventory.InventoryLocationStatus
import com.sucharu.sucharupro.domain.model.inventory.InventoryLocationType

/**
 * UI state for storage location list screen (Module 07 Step 02).
 */
data class InventoryLocationListUiState(
    val isLoading: Boolean = false,
    val projectId: String = "",
    val warehouseId: String? = null,
    val locations: List<InventoryLocation> = emptyList(),
    val filteredLocations: List<InventoryLocation> = emptyList(),
    val searchQuery: String = "",
    val selectedTypeFilter: InventoryLocationType? = null,
    val selectedStatusFilter: InventoryLocationStatus? = null,
    val errorMessage: String? = null
)
