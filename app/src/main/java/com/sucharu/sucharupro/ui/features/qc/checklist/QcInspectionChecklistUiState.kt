package com.sucharu.sucharupro.ui.features.qc.checklist

import com.sucharu.sucharupro.domain.model.qc.QcChecklistItem
import com.sucharu.sucharupro.domain.model.qc.QcInspectionChecklist
import com.sucharu.sucharupro.domain.model.qc.QcInspectionResponse

/**
 * UI State for active QC Inspection Checklist execution (Module 06 Step 03).
 */
sealed interface QcInspectionChecklistUiState {
    data object Loading : QcInspectionChecklistUiState

    data class Success(
        val checklist: QcInspectionChecklist,
        val items: List<QcChecklistItem>,
        val responses: List<QcInspectionResponse>,
        val isSubmitting: Boolean = false,
        val actionMessage: String? = null,
        val actionError: String? = null
    ) : QcInspectionChecklistUiState

    data class Error(val errorMessage: String) : QcInspectionChecklistUiState
}
