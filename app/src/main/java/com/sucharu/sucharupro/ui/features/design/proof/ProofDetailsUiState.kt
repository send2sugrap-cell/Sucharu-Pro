package com.sucharu.sucharupro.ui.features.design.proof

import com.sucharu.sucharupro.domain.model.design.DesignProof
import com.sucharu.sucharupro.domain.model.design.DesignProofVersion
import com.sucharu.sucharupro.domain.model.design.DesignRevisionRequest

/**
 * UI State for Proof Details Screen.
 */
sealed interface ProofDetailsUiState {
    data object Loading : ProofDetailsUiState

    data class Success(
        val proof: DesignProof,
        val versions: List<DesignProofVersion>,
        val revisions: List<DesignRevisionRequest>,
        val isActionInProgress: Boolean = false,
        val actionMessage: String? = null,
        val actionError: String? = null
    ) : ProofDetailsUiState

    data class Error(val errorMessage: String) : ProofDetailsUiState
}
