package com.sucharu.sucharupro.ui.features.inventory.traceability

import com.sucharu.sucharupro.domain.model.inventory.traceability.InventoryBatch
import com.sucharu.sucharupro.domain.model.inventory.traceability.InventoryLot
import com.sucharu.sucharupro.domain.model.inventory.traceability.InventoryTraceabilityActivityEvent
import com.sucharu.sucharupro.domain.model.inventory.traceability.InventoryTraceabilityRecord

/**
 * UI state for the traceability details screen (Module 07 Step 07).
 */
data class InventoryTraceabilityDetailsUiState(
    val isLoading: Boolean = false,
    val batch: InventoryBatch? = null,
    val lot: InventoryLot? = null,
    val traceHistory: List<Any> = emptyList(), // Mix of InventoryTraceabilityRecord and InventoryTraceabilityActivityEvent
    val totalQuantity: Double = 0.0,
    val unit: String = "Units",
    val errorMessage: String? = null,
    val operationMessage: String? = null
)
