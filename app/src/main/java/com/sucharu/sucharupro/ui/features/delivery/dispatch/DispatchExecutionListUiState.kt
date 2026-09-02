package com.sucharu.sucharupro.ui.features.delivery.dispatch

import com.sucharu.sucharupro.domain.model.delivery.dispatch.DispatchExecution
import com.sucharu.sucharupro.domain.model.delivery.dispatch.DispatchExecutionStatus
import com.sucharu.sucharupro.domain.model.delivery.dispatch.DispatchExecutionType

/**
 * UI State for Dispatch Execution list (Module 08 Step 03).
 */
data class DispatchExecutionListUiState(
    val isLoading: Boolean = false,
    val projectId: String = "",
    val dispatches: List<DispatchExecution> = emptyList(),
    val filteredDispatches: List<DispatchExecution> = emptyList(),
    val searchQuery: String = "",
    val selectedStatusFilter: DispatchExecutionStatus? = null,
    val selectedTypeFilter: DispatchExecutionType? = null,
    val errorMessage: String? = null
)
