package com.sucharu.sucharupro.ui.features.design.approval

import com.sucharu.sucharupro.domain.model.design.DesignApproval

/**
 * UI State for Approval List screen.
 */
sealed interface ApprovalListUiState {
    data object Loading : ApprovalListUiState

    data class Success(
        val approvals: List<DesignApproval>,
        val isSubmittingRequest: Boolean = false,
        val message: String? = null,
        val errorMessage: String? = null
    ) : ApprovalListUiState

    data class Error(val errorMessage: String) : ApprovalListUiState
}
