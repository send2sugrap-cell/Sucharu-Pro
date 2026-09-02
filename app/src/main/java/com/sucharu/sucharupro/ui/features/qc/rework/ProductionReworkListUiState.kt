package com.sucharu.sucharupro.ui.features.qc.rework

import com.sucharu.sucharupro.domain.model.qc.ProductionRework
import com.sucharu.sucharupro.domain.model.qc.ReworkReason
import com.sucharu.sucharupro.domain.model.qc.ReworkStatus
import com.sucharu.sucharupro.domain.model.qc.ReworkType

/**
 * UI State for QC Rework List Screen (Module 06 Step 05).
 */
data class ProductionReworkListUiState(
    val reworks: List<ProductionRework> = emptyList(),
    val selectedStatusFilter: ReworkStatus? = null,
    val selectedTypeFilter: ReworkType? = null,
    val selectedReasonFilter: ReworkReason? = null,
    val searchQuery: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null
) {
    val filteredReworks: List<ProductionRework>
        get() = reworks.filter { rework ->
            val matchesStatus = selectedStatusFilter == null || rework.status == selectedStatusFilter
            val matchesType = selectedTypeFilter == null || rework.reworkType == selectedTypeFilter
            val matchesReason = selectedReasonFilter == null || rework.reason == selectedReasonFilter
            val matchesQuery = searchQuery.isBlank() ||
                    rework.reworkId.contains(searchQuery, ignoreCase = true) ||
                    rework.productionJobId.contains(searchQuery, ignoreCase = true) ||
                    rework.projectId.contains(searchQuery, ignoreCase = true) ||
                    (rework.defectId?.contains(searchQuery, ignoreCase = true) == true) ||
                    rework.description.contains(searchQuery, ignoreCase = true) ||
                    (rework.assignedToName?.contains(searchQuery, ignoreCase = true) == true)
            matchesStatus && matchesType && matchesReason && matchesQuery
        }
}
