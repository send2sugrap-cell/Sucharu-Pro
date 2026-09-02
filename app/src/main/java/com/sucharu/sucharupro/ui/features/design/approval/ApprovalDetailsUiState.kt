package com.sucharu.sucharupro.ui.features.design.approval

import com.sucharu.sucharupro.domain.model.design.DesignApproval
import com.sucharu.sucharupro.domain.model.design.DesignApprovalDecision

/**
 * UI State for Approval Details Screen.
 */
sealed interface ApprovalDetailsUiState {
    data object Loading : ApprovalDetailsUiState

    data class Success(
        val approval: DesignApproval,
        val history: List<DesignApprovalDecision>,
        val isActionInProgress: Boolean = false,
        val actionMessage: String? = null,
        val actionError: String? = null
    ) : ApprovalDetailsUiState

    data class Error(val errorMessage: String) : ApprovalDetailsUiState
}
