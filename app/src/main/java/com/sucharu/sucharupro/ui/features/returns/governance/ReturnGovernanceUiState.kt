package com.sucharu.sucharupro.ui.features.returns.governance

import com.sucharu.sucharupro.domain.model.returns.ReturnException
import com.sucharu.sucharupro.domain.model.returns.ReturnExceptionStatus

/**
 * UI State for Return Governance Center (Module 11 Step 06).
 */
data class ReturnGovernanceUiState(
    val isLoading: Boolean = false,
    val isActionInProgress: Boolean = false,
    val exceptions: List<ReturnException> = emptyList(),
    val statusFilter: ReturnExceptionStatus? = null,
    val selectedException: ReturnException? = null,
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val projectId: String = ""
)
