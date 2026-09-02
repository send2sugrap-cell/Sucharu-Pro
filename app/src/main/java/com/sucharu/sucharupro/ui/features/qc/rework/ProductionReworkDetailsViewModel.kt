package com.sucharu.sucharupro.ui.features.qc.rework

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.common.FileReference
import com.sucharu.sucharupro.domain.model.user.UserRole
import com.sucharu.sucharupro.domain.repository.ProductionReworkRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel for QC Rework Details Screen (Module 06 Step 05).
 */
class ProductionReworkDetailsViewModel(
    private val reworkId: String,
    private val reworkRepository: ProductionReworkRepository,
    private val currentUserRole: UserRole? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProductionReworkDetailsUiState(isLoading = true))
    val uiState: StateFlow<ProductionReworkDetailsUiState> = _uiState.asStateFlow()

    init {
        loadReworkData()
    }

    private fun loadReworkData() {
        viewModelScope.launch {
            reworkRepository.observeReworkById(reworkId)
                .catch { ex -> _uiState.update { it.copy(errorMessage = ex.message, isLoading = false) } }
                .collect { rework ->
                    _uiState.update { it.copy(rework = rework, isLoading = false) }
                }
        }

        viewModelScope.launch {
            reworkRepository.observeAssignments(reworkId)
                .catch { /* ignore */ }
                .collect { assignments ->
                    _uiState.update { it.copy(assignments = assignments) }
                }
        }

        viewModelScope.launch {
            reworkRepository.observeReworkActivity(reworkId)
                .catch { /* ignore */ }
                .collect { activities ->
                    _uiState.update { it.copy(activityEvents = activities) }
                }
        }

        viewModelScope.launch {
            reworkRepository.observeEvidence(reworkId)
                .catch { /* ignore */ }
                .collect { evidenceList ->
                    _uiState.update { it.copy(evidenceList = evidenceList) }
                }
        }
    }

    fun startReview(reviewerId: String, reviewerName: String?, notes: String?, timestamp: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true, errorMessage = null) }
            val result = reworkRepository.startReview(
                reworkId = reworkId,
                reviewerId = reviewerId,
                reviewerName = reviewerName,
                notes = notes,
                timestamp = timestamp,
                callerRole = currentUserRole
            )
            when (result) {
                is DomainResult.Success -> _uiState.update { it.copy(isSubmitting = false, actionMessage = "Rework review started.") }
                is DomainResult.Error -> _uiState.update { it.copy(isSubmitting = false, errorMessage = result.message) }
                is DomainResult.Loading -> {}
            }
        }
    }

    fun approve(approvedBy: String, approvedByName: String?, notes: String?, timestamp: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true, errorMessage = null) }
            val result = reworkRepository.approveRework(
                reworkId = reworkId,
                approvedBy = approvedBy,
                approvedByName = approvedByName,
                notes = notes,
                timestamp = timestamp,
                callerRole = currentUserRole
            )
            when (result) {
                is DomainResult.Success -> _uiState.update { it.copy(isSubmitting = false, actionMessage = "Rework approved.") }
                is DomainResult.Error -> _uiState.update { it.copy(isSubmitting = false, errorMessage = result.message) }
                is DomainResult.Loading -> {}
            }
        }
    }

    fun reject(reason: String, rejectedBy: String, rejectedByName: String?, timestamp: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true, errorMessage = null) }
            val result = reworkRepository.rejectRework(
                reworkId = reworkId,
                reason = reason,
                rejectedBy = rejectedBy,
                rejectedByName = rejectedByName,
                timestamp = timestamp,
                callerRole = currentUserRole
            )
            when (result) {
                is DomainResult.Success -> _uiState.update { it.copy(isSubmitting = false, actionMessage = "Rework rejected.") }
                is DomainResult.Error -> _uiState.update { it.copy(isSubmitting = false, errorMessage = result.message) }
                is DomainResult.Loading -> {}
            }
        }
    }

    fun assign(
        assignedTo: String,
        assignedToName: String,
        assignedBy: String,
        assignedByName: String?,
        notes: String?,
        timestamp: String
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true, errorMessage = null) }
            val result = reworkRepository.assignRework(
                reworkId = reworkId,
                assignedTo = assignedTo,
                assignedToName = assignedToName,
                assignedBy = assignedBy,
                assignedByName = assignedByName,
                notes = notes,
                timestamp = timestamp,
                callerRole = currentUserRole
            )
            when (result) {
                is DomainResult.Success -> _uiState.update { it.copy(isSubmitting = false, actionMessage = "Rework assigned.") }
                is DomainResult.Error -> _uiState.update { it.copy(isSubmitting = false, errorMessage = result.message) }
                is DomainResult.Loading -> {}
            }
        }
    }

    fun reassign(
        newAssignedTo: String,
        newAssignedToName: String,
        reassignedBy: String,
        reassignedByName: String?,
        notes: String?,
        timestamp: String
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true, errorMessage = null) }
            val result = reworkRepository.reassignRework(
                reworkId = reworkId,
                newAssignedTo = newAssignedTo,
                newAssignedToName = newAssignedToName,
                reassignedBy = reassignedBy,
                reassignedByName = reassignedByName,
                notes = notes,
                timestamp = timestamp,
                callerRole = currentUserRole
            )
            when (result) {
                is DomainResult.Success -> _uiState.update { it.copy(isSubmitting = false, actionMessage = "Rework reassigned.") }
                is DomainResult.Error -> _uiState.update { it.copy(isSubmitting = false, errorMessage = result.message) }
                is DomainResult.Loading -> {}
            }
        }
    }

    fun unassign(unassignedBy: String, unassignedByName: String?, notes: String?, timestamp: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true, errorMessage = null) }
            val result = reworkRepository.unassignRework(
                reworkId = reworkId,
                unassignedBy = unassignedBy,
                unassignedByName = unassignedByName,
                notes = notes,
                timestamp = timestamp,
                callerRole = currentUserRole
            )
            when (result) {
                is DomainResult.Success -> _uiState.update { it.copy(isSubmitting = false, actionMessage = "Rework unassigned.") }
                is DomainResult.Error -> _uiState.update { it.copy(isSubmitting = false, errorMessage = result.message) }
                is DomainResult.Loading -> {}
            }
        }
    }

    fun start(startedBy: String, startedByName: String?, timestamp: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true, errorMessage = null) }
            val result = reworkRepository.startRework(
                reworkId = reworkId,
                startedBy = startedBy,
                startedByName = startedByName,
                timestamp = timestamp,
                callerRole = currentUserRole
            )
            when (result) {
                is DomainResult.Success -> _uiState.update { it.copy(isSubmitting = false, actionMessage = "Rework started.") }
                is DomainResult.Error -> _uiState.update { it.copy(isSubmitting = false, errorMessage = result.message) }
                is DomainResult.Loading -> {}
            }
        }
    }

    fun complete(
        correctiveAction: String,
        actualReworkedQuantity: Int,
        completedBy: String,
        completedByName: String?,
        notes: String?,
        timestamp: String
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true, errorMessage = null) }
            val result = reworkRepository.completeRework(
                reworkId = reworkId,
                correctiveAction = correctiveAction,
                actualReworkedQuantity = actualReworkedQuantity,
                completedBy = completedBy,
                completedByName = completedByName,
                notes = notes,
                timestamp = timestamp,
                callerRole = currentUserRole
            )
            when (result) {
                is DomainResult.Success -> _uiState.update { it.copy(isSubmitting = false, actionMessage = "Rework completed.") }
                is DomainResult.Error -> _uiState.update { it.copy(isSubmitting = false, errorMessage = result.message) }
                is DomainResult.Loading -> {}
            }
        }
    }

    fun returnToQc(returnedBy: String, returnedByName: String?, notes: String?, timestamp: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true, errorMessage = null) }
            val result = reworkRepository.returnToQc(
                reworkId = reworkId,
                returnedBy = returnedBy,
                returnedByName = returnedByName,
                notes = notes,
                timestamp = timestamp,
                callerRole = currentUserRole
            )
            when (result) {
                is DomainResult.Success -> _uiState.update { it.copy(isSubmitting = false, actionMessage = "Rework returned to QC.") }
                is DomainResult.Error -> _uiState.update { it.copy(isSubmitting = false, errorMessage = result.message) }
                is DomainResult.Loading -> {}
            }
        }
    }

    fun cancel(reason: String, cancelledBy: String, cancelledByName: String?, timestamp: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true, errorMessage = null) }
            val result = reworkRepository.cancelRework(
                reworkId = reworkId,
                reason = reason,
                cancelledBy = cancelledBy,
                cancelledByName = cancelledByName,
                timestamp = timestamp,
                callerRole = currentUserRole
            )
            when (result) {
                is DomainResult.Success -> _uiState.update { it.copy(isSubmitting = false, actionMessage = "Rework cancelled.") }
                is DomainResult.Error -> _uiState.update { it.copy(isSubmitting = false, errorMessage = result.message) }
                is DomainResult.Loading -> {}
            }
        }
    }

    fun attachEvidence(
        fileReferenceId: String?,
        fileReference: FileReference?,
        description: String?,
        attachedBy: String,
        timestamp: String
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true, errorMessage = null) }
            val result = reworkRepository.attachEvidence(
                reworkId = reworkId,
                fileReferenceId = fileReferenceId,
                fileReference = fileReference,
                description = description,
                attachedBy = attachedBy,
                timestamp = timestamp,
                callerRole = currentUserRole
            )
            when (result) {
                is DomainResult.Success -> _uiState.update { it.copy(isSubmitting = false, actionMessage = "Evidence attached.") }
                is DomainResult.Error -> _uiState.update { it.copy(isSubmitting = false, errorMessage = result.message) }
                is DomainResult.Loading -> {}
            }
        }
    }
}
