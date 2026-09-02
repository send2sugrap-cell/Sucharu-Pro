package com.sucharu.sucharupro.ui.features.qc.defect

import com.sucharu.sucharupro.domain.model.qc.DefectAssignment
import com.sucharu.sucharupro.domain.model.qc.ProductionDefect
import com.sucharu.sucharupro.domain.model.qc.QcDefectActivityEvent

/**
 * UI State for QC Defect Details Screen (Module 06 Step 04).
 */
data class ProductionDefectDetailsUiState(
    val defect: ProductionDefect? = null,
    val assignments: List<DefectAssignment> = emptyList(),
    val activityEvents: List<QcDefectActivityEvent> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isSubmitting: Boolean = false,
    val actionMessage: String? = null
)
