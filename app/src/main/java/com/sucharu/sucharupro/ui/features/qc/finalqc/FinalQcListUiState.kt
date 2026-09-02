package com.sucharu.sucharupro.ui.features.qc.finalqc

import com.sucharu.sucharupro.domain.model.qc.FinalQcInspection
import com.sucharu.sucharupro.domain.model.qc.FinalQcStatus

/**
 * Filter options for Final QC list screen.
 */
enum class FinalQcFilter(val label: String) {
    ALL("All"),
    PENDING("Pending"),
    ASSIGNED("Assigned"),
    IN_INSPECTION("In Inspection"),
    PASSED("Passed"),
    FAILED("Failed"),
    BLOCKED("Blocked"),
    RELEASED("Released")
}

/**
 * UI state for the Final QC list screen (Module 06 Step 07).
 */
data class FinalQcListUiState(
    val isLoading: Boolean = false,
    val inspections: List<FinalQcInspection> = emptyList(),
    val filteredInspections: List<FinalQcInspection> = emptyList(),
    val selectedFilter: FinalQcFilter = FinalQcFilter.ALL,
    val searchQuery: String = "",
    val errorMessage: String? = null
)
