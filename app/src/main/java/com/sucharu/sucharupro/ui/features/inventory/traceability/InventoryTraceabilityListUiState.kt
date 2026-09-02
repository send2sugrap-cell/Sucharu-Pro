package com.sucharu.sucharupro.ui.features.inventory.traceability

import com.sucharu.sucharupro.domain.model.inventory.traceability.InventoryBatch
import com.sucharu.sucharupro.domain.model.inventory.traceability.InventoryLot
import com.sucharu.sucharupro.domain.model.inventory.traceability.InventoryTraceabilityStatus

/**
 * UI state for the traceability list screen (Module 07 Step 07).
 */
data class InventoryTraceabilityListUiState(
    val isLoading: Boolean = false,
    val projectId: String = "",
    val viewMode: TraceabilityViewMode = TraceabilityViewMode.BATCH,
    val batches: List<InventoryBatch> = emptyList(),
    val lots: List<InventoryLot> = emptyList(),
    val filteredBatches: List<InventoryBatch> = emptyList(),
    val filteredLots: List<InventoryLot> = emptyList(),
    val searchQuery: String = "",
    val selectedStatusFilter: InventoryTraceabilityStatus? = null,
    val errorMessage: String? = null
)

enum class TraceabilityViewMode {
    BATCH, LOT
}
