package com.sucharu.sucharupro.ui.features.design.approval

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.repository.DesignApprovalRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

/**
 * ViewModel for listing, observing, and creating Approval Requests (Module 05 Step 04).
 */
class ApprovalListViewModel(
    private val proofId: String? = null,
    private val projectId: String? = null,
    private val approvalRepository: DesignApprovalRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<ApprovalListUiState>(ApprovalListUiState.Loading)
    val uiState: StateFlow<ApprovalListUiState> = _uiState.asStateFlow()

    init {
        loadApprovals()
    }

    private fun loadApprovals() {
        val stream = when {
            proofId != null -> approvalRepository.getApprovalsForProof(proofId)
            projectId != null -> approvalRepository.getApprovalsForProject(projectId)
            else -> approvalRepository.observeApprovals()
        }

        viewModelScope.launch {
            stream
                .catch { ex -> _uiState.value = ApprovalListUiState.Error(ex.localizedMessage ?: "Failed to load approvals.") }
                .collect { approvals ->
                    _uiState.value = ApprovalListUiState.Success(approvals = approvals)
                }
        }
    }

    fun submitApprovalRequest(
        targetProofId: String,
        targetVersionNumber: Int,
        comments: String?,
        requestedBy: String,
        requestedByName: String?,
        timestamp: String
    ) {
        viewModelScope.launch {
            val currentState = _uiState.value
            if (currentState is ApprovalListUiState.Success) {
                _uiState.value = currentState.copy(isSubmittingRequest = true, errorMessage = null)
            }

            val result = approvalRepository.createApprovalRequest(
                proofId = targetProofId,
                targetVersionNumber = targetVersionNumber,
                comments = comments,
                requestedBy = requestedBy,
                requestedByName = requestedByName,
                timestamp = timestamp
            )

            if (currentState is ApprovalListUiState.Success) {
                when (result) {
                    is DomainResult.Success -> {
                        _uiState.value = currentState.copy(
                            isSubmittingRequest = false,
                            message = "Approval request submitted successfully!"
                        )
                    }
                    is DomainResult.Error -> {
                        _uiState.value = currentState.copy(
                            isSubmittingRequest = false,
                            errorMessage = result.message
                        )
                    }
                    is DomainResult.Loading -> {}
                }
            }
        }
    }
}
