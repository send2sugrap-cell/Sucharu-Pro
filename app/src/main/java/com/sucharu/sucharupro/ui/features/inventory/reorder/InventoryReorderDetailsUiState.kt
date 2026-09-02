package com.sucharu.sucharupro.ui.features.inventory.reorder

import com.sucharu.sucharupro.domain.model.inventory.reorder.InventoryReorderAlert
import com.sucharu.sucharupro.domain.model.inventory.reorder.InventoryStockLevelPolicy
import com.sucharu.sucharupro.domain.model.user.UserRole

/**
 * UI state for the reorder alert details screen (Module 07 Step 08).
 */
data class InventoryReorderDetailsUiState(
    val isLoading: Boolean = false,
    val alert: InventoryReorderAlert? = null,
    val policy: InventoryStockLevelPolicy? = null,
    val currentUserRole: UserRole? = null,
    val operationMessage: String? = null,
    val errorMessage: String? = null
)
