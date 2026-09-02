package com.sucharu.sucharupro.ui.features.design.proof

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.common.FileReference
import com.sucharu.sucharupro.domain.model.design.RevisionReason
import com.sucharu.sucharupro.domain.repository.DesignProofRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

/**
 * ViewModel for Proof Details, Version History, Revision Requests, and Resubmission (Module 05 Step 03).
 */
class ProofDetailsViewModel(
    private val proofId: String,
    private val proofRepository: DesignProofRepository
) : ViewModel() {

    private val _actionState = MutableStateFlow<Triple<Boolean, String?, String?>>(Triple(false, null, null))
    private val _uiState = MutableStateFlow<ProofDetailsUiState>(ProofDetailsUiState.Loading)
    val uiState: StateFlow<ProofDetailsUiState> = _uiState.asStateFlow()

    init {
        loadProofDetails()
    }

    private fun loadProofDetails() {
        viewModelScope.launch {
            combine(
                proofRepository.getProofById(proofId),
                proofRepository.getProofVersions(proofId),
                proofRepository.getRevisionRequests(proofId),
                _actionState
            ) { proof, versions, revisions, action ->
                if (proof == null) {
                    ProofDetailsUiState.Error("Proof with ID '$proofId' not found.")
                } else {
                    ProofDetailsUiState.Success(
                        proof = proof,
                        versions = versions,
                        revisions = revisions,
                        isActionInProgress = action.first,
                        actionMessage = action.second,
                        actionError = action.third
                    )
                }
            }.collect { state ->
                _uiState.value = state
            }
        }
    }

    fun submitProofForReview(actorId: String?, timestamp: String) {
        viewModelScope.launch {
            _actionState.value = Triple(true, "Submitting proof for review...", null)
            val result = proofRepository.submitProofForReview(
                proofId = proofId,
                actorId = actorId,
                timestamp = timestamp
            )
            when (result) {
                is DomainResult.Success -> _actionState.value = Triple(false, "Proof submitted for review!", null)
                is DomainResult.Error -> _actionState.value = Triple(false, null, result.message)
                is DomainResult.Loading -> {}
            }
        }
    }

    fun requestRevision(
        targetVersionNumber: Int,
        reason: RevisionReason,
        notes: String,
        requesterId: String,
        requesterName: String?,
        timestamp: String
    ) {
        viewModelScope.launch {
            _actionState.value = Triple(true, "Requesting revision...", null)
            val result = proofRepository.requestRevision(
                proofId = proofId,
                targetVersionNumber = targetVersionNumber,
                reason = reason,
                notes = notes,
                requesterId = requesterId,
                requesterName = requesterName,
                timestamp = timestamp
            )
            when (result) {
                is DomainResult.Success -> _actionState.value = Triple(false, "Revision requested successfully!", null)
                is DomainResult.Error -> _actionState.value = Triple(false, null, result.message)
                is DomainResult.Loading -> {}
            }
        }
    }

    fun startRevision(actorId: String?, timestamp: String) {
        viewModelScope.launch {
            _actionState.value = Triple(true, "Starting revision work...", null)
            val result = proofRepository.startRevision(
                proofId = proofId,
                actorId = actorId,
                timestamp = timestamp
            )
            when (result) {
                is DomainResult.Success -> _actionState.value = Triple(false, "Revision work started.", null)
                is DomainResult.Error -> _actionState.value = Triple(false, null, result.message)
                is DomainResult.Loading -> {}
            }
        }
    }

    fun resubmitProof(
        artworkVersionId: String,
        fileReference: FileReference,
        notes: String?,
        createdBy: String?,
        timestamp: String
    ) {
        viewModelScope.launch {
            _actionState.value = Triple(true, "Resubmitting revised proof...", null)
            val result = proofRepository.resubmitProof(
                proofId = proofId,
                artworkVersionId = artworkVersionId,
                fileReference = fileReference,
                notes = notes,
                createdBy = createdBy,
                timestamp = timestamp
            )
            when (result) {
                is DomainResult.Success -> _actionState.value = Triple(false, "Proof ${result.data.versionTag} resubmitted!", null)
                is DomainResult.Error -> _actionState.value = Triple(false, null, result.message)
                is DomainResult.Loading -> {}
            }
        }
    }
}
