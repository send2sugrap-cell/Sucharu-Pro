package com.sucharu.sucharupro.ui.features.inventory.analytics

import com.sucharu.sucharupro.domain.model.inventory.analytics.InventoryException
import com.sucharu.sucharupro.domain.model.inventory.analytics.InventoryExceptionType

/**
 * UI State for Inventory Governance (Module 07 Step 10).
 */
data class InventoryGovernanceUiState(
    val isLoading: Boolean = false,
    val projectId: String = "",
    val exceptions: List<InventoryException> = emptyList(),
    val filteredExceptions: List<InventoryException> = emptyList(),
    val selectedSeverity: InventoryException.Severity? = null,
    val selectedType: InventoryExceptionType? = null,
    val errorMessage: String? = null
)
