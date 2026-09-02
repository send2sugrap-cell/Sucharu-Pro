package com.sucharu.sucharupro.ui.features.qc.rework

import com.sucharu.sucharupro.domain.model.qc.ProductionRework
import com.sucharu.sucharupro.domain.model.qc.ReworkActivityEvent
import com.sucharu.sucharupro.domain.model.qc.ReworkAssignment
import com.sucharu.sucharupro.domain.model.qc.ReworkEvidence

/**
 * UI State for QC Rework Details Screen (Module 06 Step 05).
 */
data class ProductionReworkDetailsUiState(
    val rework: ProductionRework? = null,
    val assignments: List<ReworkAssignment> = emptyList(),
    val activityEvents: List<ReworkActivityEvent> = emptyList(),
    val evidenceList: List<ReworkEvidence> = emptyList(),
    val isLoading: Boolean = false,
    val isSubmitting: Boolean = false,
    val actionMessage: String? = null,
    val errorMessage: String? = null
)
