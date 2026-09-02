package com.sucharu.sucharupro.ui.features.qc.finalqc

import com.sucharu.sucharupro.domain.model.qc.FinalQcActivityEvent
import com.sucharu.sucharupro.domain.model.qc.FinalQcEligibilityResult
import com.sucharu.sucharupro.domain.model.qc.FinalQcInspection
import com.sucharu.sucharupro.domain.model.qc.FinalQcReleaseAuthorization

/**
 * UI state for the Final QC & Production Release details screen (Module 06 Step 07).
 */
data class FinalQcDetailsUiState(
    val isLoading: Boolean = false,
    val isActionInProgress: Boolean = false,
    val inspection: FinalQcInspection? = null,
    val releaseAuthorization: FinalQcReleaseAuthorization? = null,
    val eligibilityResult: FinalQcEligibilityResult? = null,
    val activityEvents: List<FinalQcActivityEvent> = emptyList(),
    val showPassDialog: Boolean = false,
    val showFailDialog: Boolean = false,
    val showReleaseDialog: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null
)
