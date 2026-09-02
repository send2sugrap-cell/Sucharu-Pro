package com.sucharu.sucharupro.ui.features.inventory.location

import com.sucharu.sucharupro.domain.model.inventory.InventoryLocation

/**
 * UI state for storage location details screen (Module 07 Step 02).
 */
data class InventoryLocationDetailsUiState(
    val isLoading: Boolean = false,
    val location: InventoryLocation? = null,
    val parentLocation: InventoryLocation? = null,
    val childLocations: List<InventoryLocation> = emptyList(),
    val errorMessage: String? = null
)
