package com.sucharu.sucharupro.ui.features.qc

import com.sucharu.sucharupro.domain.model.qc.ProductionQc

/**
 * UI State for Quality Control List Screen (Module 06 Step 01).
 */
sealed interface ProductionQcListUiState {
    data object Loading : ProductionQcListUiState

    data class Success(
        val qcList: List<ProductionQc>,
        val isCreating: Boolean = false,
        val message: String? = null,
        val errorMessage: String? = null
    ) : ProductionQcListUiState

    data class Error(val errorMessage: String) : ProductionQcListUiState
}
