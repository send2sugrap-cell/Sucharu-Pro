package com.sucharu.sucharupro.ui.features.qc.preproduction

import com.sucharu.sucharupro.domain.model.qc.PreProductionQcItem
import com.sucharu.sucharupro.domain.model.qc.PreProductionQcSnapshot
import com.sucharu.sucharupro.domain.model.qc.ProductionQc

/**
 * UI State for Pre-Production Quality Control checklist and inspection screen (Module 06 Step 02).
 */
sealed interface PreProductionQcUiState {
    data object Loading : PreProductionQcUiState

    data class Success(
        val qc: ProductionQc,
        val items: List<PreProductionQcItem>,
        val snapshot: PreProductionQcSnapshot? = null,
        val isSubmitting: Boolean = false,
        val actionMessage: String? = null,
        val actionError: String? = null
    ) : PreProductionQcUiState

    data class Error(val errorMessage: String) : PreProductionQcUiState
}
