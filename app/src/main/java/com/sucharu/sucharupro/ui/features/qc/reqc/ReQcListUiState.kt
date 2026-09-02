package com.sucharu.sucharupro.ui.features.qc.reqc

import com.sucharu.sucharupro.domain.model.qc.ReQcCycleType
import com.sucharu.sucharupro.domain.model.qc.ReQcDecision
import com.sucharu.sucharupro.domain.model.qc.ReQcInspection
import com.sucharu.sucharupro.domain.model.qc.ReQcStatus

/**
 * UI State for Re-QC List Screen (Module 06 Step 06).
 */
data class ReQcListUiState(
    val reQcs: List<ReQcInspection> = emptyList(),
    val selectedStatusFilter: ReQcStatus? = null,
    val selectedCycleTypeFilter: ReQcCycleType? = null,
    val selectedDecisionFilter: ReQcDecision? = null,
    val searchQuery: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null
) {
    val filteredReQcs: List<ReQcInspection>
        get() = reQcs.filter { item ->
            val matchesStatus = selectedStatusFilter == null || item.status == selectedStatusFilter
            val matchesType = selectedCycleTypeFilter == null || item.cycleType == selectedCycleTypeFilter
            val matchesDecision = selectedDecisionFilter == null || item.decision == selectedDecisionFilter
            val matchesQuery = searchQuery.isBlank() ||
                    item.reQcId.contains(searchQuery, ignoreCase = true) ||
                    item.productionJobId.contains(searchQuery, ignoreCase = true) ||
                    item.projectId.contains(searchQuery, ignoreCase = true) ||
                    item.productionReworkId.contains(searchQuery, ignoreCase = true) ||
                    (item.originalDefectId?.contains(searchQuery, ignoreCase = true) == true) ||
                    (item.assignedInspectorName?.contains(searchQuery, ignoreCase = true) == true) ||
                    (item.failureNotes?.contains(searchQuery, ignoreCase = true) == true)
            matchesStatus && matchesType && matchesDecision && matchesQuery
        }
}
