package com.sucharu.sucharupro.ui.features.design.proof

import com.sucharu.sucharupro.domain.model.design.DesignProof

/**
 * UI State for Proof list screen.
 */
sealed interface ProofListUiState {
    data object Loading : ProofListUiState

    data class Success(
        val proofs: List<DesignProof>,
        val isCreatingProof: Boolean = false,
        val creationMessage: String? = null,
        val errorMessage: String? = null
    ) : ProofListUiState

    data class Error(val errorMessage: String) : ProofListUiState
}
