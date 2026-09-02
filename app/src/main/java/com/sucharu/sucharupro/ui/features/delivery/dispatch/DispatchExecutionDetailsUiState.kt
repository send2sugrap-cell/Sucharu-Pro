package com.sucharu.sucharupro.ui.features.delivery.dispatch

import com.sucharu.sucharupro.domain.model.delivery.challan.DeliveryChallan
import com.sucharu.sucharupro.domain.model.delivery.dispatch.DispatchExecution
import com.sucharu.sucharupro.domain.model.delivery.dispatch.DispatchExecutionActivityEvent
import com.sucharu.sucharupro.domain.model.delivery.dispatch.DispatchExecutionLine

/**
 * UI State for Dispatch Execution details screen (Module 08 Step 03).
 */
data class DispatchExecutionDetailsUiState(
    val isLoading: Boolean = false,
    val isActionInProgress: Boolean = false,
    val dispatch: DispatchExecution? = null,
    val lines: List<DispatchExecutionLine> = emptyList(),
    val challan: DeliveryChallan? = null,
    val stockAvailabilityMap: Map<String, Int> = emptyMap(),
    val activityEvents: List<DispatchExecutionActivityEvent> = emptyList(),
    val errorMessage: String? = null,
    val successMessage: String? = null
)
