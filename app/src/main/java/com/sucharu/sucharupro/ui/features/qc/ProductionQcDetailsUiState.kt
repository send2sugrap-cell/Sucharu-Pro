package com.sucharu.sucharupro.ui.features.qc

import com.sucharu.sucharupro.domain.model.qc.ProductionQc
import com.sucharu.sucharupro.domain.model.qc.QcActivityEvent
import com.sucharu.sucharupro.domain.model.qc.QcAssignment

/**
 * UI State for Quality Control Details Screen (Module 06 Step 01).
 */
sealed interface ProductionQcDetailsUiState {
    data object Loading : ProductionQcDetailsUiState

    data class Success(
        val qc: ProductionQc,
        val assignments: List<QcAssignment>,
        val activities: List<QcActivityEvent>,
        val isActionInProgress: Boolean = false,
        val actionMessage: String? = null,
        val actionError: String? = null
    ) : ProductionQcDetailsUiState

    data class Error(val errorMessage: String) : ProductionQcDetailsUiState
}
