package com.sucharu.sucharupro.ui.features.substratereservation

import com.sucharu.sucharupro.data.api.model.substratereservation.SubstrateReleaseGovernanceResponseDto

/**
 * UI State for Substrate Release & Revision Governance Command Center.
 * Module 19 Step 05.
 */
data class SubstrateReleaseGovernanceUiState(
    val isLoading: Boolean = false,
    val isEvaluating: Boolean = false,
    val isApproving: Boolean = false,
    val isExecuting: Boolean = false,
    val selectedTab: Int = 0, // 0: Overview, 1: Cancellation, 2: Revision Delta, 3: Release Execution, 4: Audit & AI Handoff
    val currentRecord: SubstrateReleaseGovernanceResponseDto? = null,
    val records: List<SubstrateReleaseGovernanceResponseDto> = emptyList(),
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val showEvaluateDialog: Boolean = false,
    val showApproveDialog: Boolean = false,
    val showExecuteDialog: Boolean = false,
    val jsonHandoffPreview: String? = null
)
