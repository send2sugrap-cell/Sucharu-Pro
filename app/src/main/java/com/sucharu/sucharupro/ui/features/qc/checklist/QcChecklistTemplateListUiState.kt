package com.sucharu.sucharupro.ui.features.qc.checklist

import com.sucharu.sucharupro.domain.model.qc.QcChecklistTemplate

/**
 * UI State for QC Checklist Template List Screen (Module 06 Step 03).
 */
sealed interface QcChecklistTemplateListUiState {
    data object Loading : QcChecklistTemplateListUiState

    data class Success(
        val templates: List<QcChecklistTemplate>,
        val isCreating: Boolean = false,
        val message: String? = null,
        val errorMessage: String? = null
    ) : QcChecklistTemplateListUiState

    data class Error(val errorMessage: String) : QcChecklistTemplateListUiState
}
