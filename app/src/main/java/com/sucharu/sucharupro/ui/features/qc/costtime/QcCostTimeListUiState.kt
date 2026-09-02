package com.sucharu.sucharupro.ui.features.qc.costtime

import com.sucharu.sucharupro.domain.model.qc.QcCostStatus
import com.sucharu.sucharupro.domain.model.qc.QcCostTimeReconciliation

/**
 * UI state for QC Cost & Time Reconciliation List Screen (Module 06 Step 08).
 */
data class QcCostTimeListUiState(
    val isLoading: Boolean = false,
    val reconciliations: List<QcCostTimeReconciliation> = emptyList(),
    val filteredReconciliations: List<QcCostTimeReconciliation> = emptyList(),
    val searchQuery: String = "",
    val selectedStatusFilter: QcCostStatus? = null,
    val totalReconciledJobs: Int = 0,
    val totalCostOverrunJobs: Int = 0,
    val totalTimeOverrunJobs: Int = 0,
    val errorMessage: String? = null,
    val successMessage: String? = null
)
