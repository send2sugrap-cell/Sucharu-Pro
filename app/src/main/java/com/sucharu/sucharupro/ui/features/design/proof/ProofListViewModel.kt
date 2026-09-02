package com.sucharu.sucharupro.ui.features.design.proof

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.common.FileReference
import com.sucharu.sucharupro.domain.repository.DesignProofRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

/**
 * ViewModel for listing, observing, and creating Proofs (Module 05 Step 03).
 */
class ProofListViewModel(
    private val artworkId: String? = null,
    private val projectId: String? = null,
    private val proofRepository: DesignProofRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<ProofListUiState>(ProofListUiState.Loading)
    val uiState: StateFlow<ProofListUiState> = _uiState.asStateFlow()

    init {
        loadProofs()
    }

    private fun loadProofs() {
        val stream = when {
            artworkId != null -> proofRepository.getProofsForArtwork(artworkId)
            projectId != null -> proofRepository.getProofsForProject(projectId)
            else -> proofRepository.observeProofs()
        }

        viewModelScope.launch {
            stream
                .catch { ex -> _uiState.value = ProofListUiState.Error(ex.localizedMessage ?: "Failed to load proofs.") }
                .collect { proofs ->
                    _uiState.value = ProofListUiState.Success(proofs = proofs)
                }
        }
    }

    fun createProof(
        targetArtworkId: String,
        title: String,
        initialArtworkVersionId: String? = null,
        initialFile: FileReference? = null,
        notes: String? = null,
        createdBy: String? = null,
        timestamp: String
    ) {
        viewModelScope.launch {
            val currentState = _uiState.value
            if (currentState is ProofListUiState.Success) {
                _uiState.value = currentState.copy(isCreatingProof = true, errorMessage = null)
            }

            val result = proofRepository.createProof(
                artworkId = targetArtworkId,
                title = title,
                initialArtworkVersionId = initialArtworkVersionId,
                initialFile = initialFile,
                notes = notes,
                createdBy = createdBy,
                timestamp = timestamp
            )

            if (currentState is ProofListUiState.Success) {
                when (result) {
                    is DomainResult.Success -> {
                        _uiState.value = currentState.copy(
                            isCreatingProof = false,
                            creationMessage = "Proof '${result.data.title}' created successfully!"
                        )
                    }
                    is DomainResult.Error -> {
                        _uiState.value = currentState.copy(
                            isCreatingProof = false,
                            errorMessage = result.message
                        )
                    }
                    is DomainResult.Loading -> {}
                }
            }
        }
    }
}
