package com.sucharu.sucharupro.ui.features.qc.reqc

import com.sucharu.sucharupro.domain.model.qc.ReQcActivityEvent
import com.sucharu.sucharupro.domain.model.qc.ReQcFailureRecord
import com.sucharu.sucharupro.domain.model.qc.ReQcInspection

/**
 * UI State for Re-QC Details Screen (Module 06 Step 06).
 */
data class ReQcDetailsUiState(
    val reQc: ReQcInspection? = null,
    val cycleHistory: List<ReQcInspection> = emptyList(),
    val failureRecords: List<ReQcFailureRecord> = emptyList(),
    val activityEvents: List<ReQcActivityEvent> = emptyList(),
    val isLoading: Boolean = false,
    val isSubmitting: Boolean = false,
    val actionMessage: String? = null,
    val errorMessage: String? = null
)
