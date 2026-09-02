package com.sucharu.sucharupro.ui.features.design.approval

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.design.RevisionReason
import com.sucharu.sucharupro.domain.repository.DesignApprovalRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

/**
 * ViewModel for Approval Details, Decisions, and Final Locking (Module 05 Step 04).
 */
class ApprovalDetailsViewModel(
    private val approvalId: String,
    private val approvalRepository: DesignApprovalRepository
) : ViewModel() {

    private val _actionState = MutableStateFlow<Triple<Boolean, String?, String?>>(Triple(false, null, null))
    private val _uiState = MutableStateFlow<ApprovalDetailsUiState>(ApprovalDetailsUiState.Loading)
    val uiState: StateFlow<ApprovalDetailsUiState> = _uiState.asStateFlow()

    init {
        loadApprovalDetails()
    }

    private fun loadApprovalDetails() {
        viewModelScope.launch {
            combine(
                approvalRepository.getApprovalById(approvalId),
                approvalRepository.getApprovalHistory(approvalId),
                _actionState
            ) { approval, history, action ->
                if (approval == null) {
                    ApprovalDetailsUiState.Error("Approval '$approvalId' not found.")
                } else {
                    ApprovalDetailsUiState.Success(
                        approval = approval,
                        history = history,
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

    fun startReview(reviewerId: String, reviewerName: String?, timestamp: String) {
        viewModelScope.launch {
            _actionState.value = Triple(true, "Starting review...", null)
            val result = approvalRepository.startReview(
                approvalId = approvalId,
                reviewerId = reviewerId,
                reviewerName = reviewerName,
                timestamp = timestamp
            )
            when (result) {
                is DomainResult.Success -> _actionState.value = Triple(false, "Review started.", null)
                is DomainResult.Error -> _actionState.value = Triple(false, null, result.message)
                is DomainResult.Loading -> {}
            }
        }
    }

    fun approve(comments: String?, reviewerId: String, reviewerName: String?, timestamp: String) {
        viewModelScope.launch {
            _actionState.value = Triple(true, "Approving proof...", null)
            val result = approvalRepository.approve(
                approvalId = approvalId,
                comments = comments,
                reviewerId = reviewerId,
                reviewerName = reviewerName,
                timestamp = timestamp
            )
            when (result) {
                is DomainResult.Success -> _actionState.value = Triple(false, "Proof approved!", null)
                is DomainResult.Error -> _actionState.value = Triple(false, null, result.message)
                is DomainResult.Loading -> {}
            }
        }
    }

    fun requestRevision(reason: RevisionReason, comments: String, reviewerId: String, reviewerName: String?, timestamp: String) {
        viewModelScope.launch {
            _actionState.value = Triple(true, "Requesting revision...", null)
            val result = approvalRepository.requestRevision(
                approvalId = approvalId,
                reason = reason,
                comments = comments,
                reviewerId = reviewerId,
                reviewerName = reviewerName,
                timestamp = timestamp
            )
            when (result) {
                is DomainResult.Success -> _actionState.value = Triple(false, "Revision requested & Step 03 cycle opened.", null)
                is DomainResult.Error -> _actionState.value = Triple(false, null, result.message)
                is DomainResult.Loading -> {}
            }
        }
    }

    fun reject(comments: String, reviewerId: String, reviewerName: String?, timestamp: String) {
        viewModelScope.launch {
            _actionState.value = Triple(true, "Rejecting approval...", null)
            val result = approvalRepository.reject(
                approvalId = approvalId,
                comments = comments,
                reviewerId = reviewerId,
                reviewerName = reviewerName,
                timestamp = timestamp
            )
            when (result) {
                is DomainResult.Success -> _actionState.value = Triple(false, "Proof rejected.", null)
                is DomainResult.Error -> _actionState.value = Triple(false, null, result.message)
                is DomainResult.Loading -> {}
            }
        }
    }

    fun lockFinalApproval(lockedBy: String, timestamp: String) {
        viewModelScope.launch {
            _actionState.value = Triple(true, "Applying Final Lock...", null)
            val result = approvalRepository.lockFinalApproval(
                approvalId = approvalId,
                lockedBy = lockedBy,
                timestamp = timestamp
            )
            when (result) {
                is DomainResult.Success -> _actionState.value = Triple(false, "Final Lock applied successfully!", null)
                is DomainResult.Error -> _actionState.value = Triple(false, null, result.message)
                is DomainResult.Loading -> {}
            }
        }
    }
}
