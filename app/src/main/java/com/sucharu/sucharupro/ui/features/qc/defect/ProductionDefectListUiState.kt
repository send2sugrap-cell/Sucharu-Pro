package com.sucharu.sucharupro.ui.features.qc.defect

import com.sucharu.sucharupro.domain.model.qc.DefectCategory
import com.sucharu.sucharupro.domain.model.qc.DefectSeverity
import com.sucharu.sucharupro.domain.model.qc.DefectStatus
import com.sucharu.sucharupro.domain.model.qc.ProductionDefect

/**
 * UI State for QC Defect List Screen (Module 06 Step 04).
 */
data class ProductionDefectListUiState(
    val defects: List<ProductionDefect> = emptyList(),
    val selectedStatusFilter: DefectStatus? = null,
    val selectedSeverityFilter: DefectSeverity? = null,
    val selectedCategoryFilter: DefectCategory? = null,
    val searchQuery: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null
) {
    val filteredDefects: List<ProductionDefect>
        get() = defects.filter { defect ->
            val matchesStatus = selectedStatusFilter == null || defect.status == selectedStatusFilter
            val matchesSeverity = selectedSeverityFilter == null || defect.severity == selectedSeverityFilter
            val matchesCategory = selectedCategoryFilter == null || defect.category == selectedCategoryFilter
            val matchesQuery = searchQuery.isBlank() ||
                    defect.title.contains(searchQuery, ignoreCase = true) ||
                    defect.defectId.contains(searchQuery, ignoreCase = true) ||
                    defect.productionJobId.contains(searchQuery, ignoreCase = true) ||
                    defect.description.contains(searchQuery, ignoreCase = true)
            matchesStatus && matchesSeverity && matchesCategory && matchesQuery
        }
}
