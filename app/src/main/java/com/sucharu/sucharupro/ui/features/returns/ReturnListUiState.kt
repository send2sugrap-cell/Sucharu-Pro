package com.sucharu.sucharupro.ui.features.returns

import com.sucharu.sucharupro.domain.model.returns.ReturnReason
import com.sucharu.sucharupro.domain.model.returns.ReturnRequest
import com.sucharu.sucharupro.domain.model.returns.ReturnStatus

/**
 * UI State for the Return Request List Screen (Module 11 Step 02).
 */
data class ReturnListUiState(
    val isLoading: Boolean = false,
    val projectId: String = "",
    val returns: List<ReturnRequest> = emptyList(),
    val filteredReturns: List<ReturnRequest> = emptyList(),
    val searchQuery: String = "",
    val selectedStatusFilter: ReturnStatus? = null,
    val selectedReasonFilter: ReturnReason? = null,
    val errorMessage: String? = null
)
