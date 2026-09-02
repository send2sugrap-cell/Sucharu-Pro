package com.sucharu.sucharupro.ui.features.qc.reqc

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.qc.ReQcCycleType
import com.sucharu.sucharupro.domain.model.qc.ReQcFailureReason
import com.sucharu.sucharupro.domain.model.user.UserRole
import com.sucharu.sucharupro.domain.repository.ProductionReQcRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel for Re-QC Details Screen (Module 06 Step 06).
 */
class ReQcDetailsViewModel(
    private val reQcId: String,
    private val reQcRepository: ProductionReQcRepository,
    private val currentUserRole: UserRole? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(ReQcDetailsUiState(isLoading = true))
    val uiState: StateFlow<ReQcDetailsUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            reQcRepository.observeReQcById(reQcId)
                .catch { ex -> _uiState.update { it.copy(errorMessage = ex.message, isLoading = false) } }
                .collect { reQc ->
                    _uiState.update { it.copy(reQc = reQc, isLoading = false) }
                    if (reQc != null) {
                        loadHistory(reQc.productionJobId)
                    }
                }
        }

        viewModelScope.launch {
            reQcRepository.observeReQcActivity(reQcId)
                .catch { /* ignore */ }
                .collect { events ->
                    _uiState.update { it.copy(activityEvents = events) }
                }
        }

        viewModelScope.launch {
            reQcRepository.observeFailureHistory(reQcId = reQcId)
                .catch { /* ignore */ }
                .collect { records ->
                    _uiState.update { it.copy(failureRecords = records) }
                }
        }
    }

    private fun loadHistory(productionJobId: String) {
        viewModelScope.launch {
            reQcRepository.observeReQcCycles(productionJobId)
                .catch { /* ignore */ }
                .collect { cycles ->
                    _uiState.update { it.copy(cycleHistory = cycles) }
                }
        }
    }

    fun assign(
        inspectorId: String,
        inspectorName: String,
        assignedBy: String,
        assignedByName: String?,
        notes: String?,
        timestamp: String
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true, errorMessage = null) }
            val result = reQcRepository.assignReQc(
                reQcId = reQcId,
                inspectorId = inspectorId,
                inspectorName = inspectorName,
                assignedBy = assignedBy,
                assignedByName = assignedByName,
                notes = notes,
                timestamp = timestamp,
                callerRole = currentUserRole
            )
            when (result) {
                is DomainResult.Success -> _uiState.update { it.copy(isSubmitting = false, actionMessage = "Re-QC assigned.") }
                is DomainResult.Error -> _uiState.update { it.copy(isSubmitting = false, errorMessage = result.message) }
                is DomainResult.Loading -> {}
            }
        }
    }

    fun reassign(
        newInspectorId: String,
        newInspectorName: String,
        reassignedBy: String,
        reassignedByName: String?,
        notes: String?,
        timestamp: String
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true, errorMessage = null) }
            val result = reQcRepository.reassignReQc(
                reQcId = reQcId,
                newInspectorId = newInspectorId,
                newInspectorName = newInspectorName,
                reassignedBy = reassignedBy,
                reassignedByName = reassignedByName,
                notes = notes,
                timestamp = timestamp,
                callerRole = currentUserRole
            )
            when (result) {
                is DomainResult.Success -> _uiState.update { it.copy(isSubmitting = false, actionMessage = "Re-QC reassigned.") }
                is DomainResult.Error -> _uiState.update { it.copy(isSubmitting = false, errorMessage = result.message) }
                is DomainResult.Loading -> {}
            }
        }
    }

    fun unassign(unassignedBy: String, unassignedByName: String?, notes: String?, timestamp: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true, errorMessage = null) }
            val result = reQcRepository.unassignReQc(
                reQcId = reQcId,
                unassignedBy = unassignedBy,
                unassignedByName = unassignedByName,
                notes = notes,
                timestamp = timestamp,
                callerRole = currentUserRole
            )
            when (result) {
                is DomainResult.Success -> _uiState.update { it.copy(isSubmitting = false, actionMessage = "Re-QC unassigned.") }
                is DomainResult.Error -> _uiState.update { it.copy(isSubmitting = false, errorMessage = result.message) }
                is DomainResult.Loading -> {}
            }
        }
    }

    fun startInspection(inspectorId: String, inspectorName: String?, notes: String?, timestamp: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true, errorMessage = null) }
            val result = reQcRepository.startInspection(
                reQcId = reQcId,
                inspectorId = inspectorId,
                inspectorName = inspectorName,
                notes = notes,
                timestamp = timestamp,
                callerRole = currentUserRole
            )
            when (result) {
                is DomainResult.Success -> _uiState.update { it.copy(isSubmitting = false, actionMessage = "Inspection started.") }
                is DomainResult.Error -> _uiState.update { it.copy(isSubmitting = false, errorMessage = result.message) }
                is DomainResult.Loading -> {}
            }
        }
    }

    fun pass(inspectorId: String, inspectorName: String?, passNotes: String?, timestamp: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true, errorMessage = null) }
            val result = reQcRepository.passReQc(
                reQcId = reQcId,
                inspectorId = inspectorId,
                inspectorName = inspectorName,
                passNotes = passNotes,
                timestamp = timestamp,
                callerRole = currentUserRole
            )
            when (result) {
                is DomainResult.Success -> _uiState.update { it.copy(isSubmitting = false, actionMessage = "Re-QC inspection PASSED.") }
                is DomainResult.Error -> _uiState.update { it.copy(isSubmitting = false, errorMessage = result.message) }
                is DomainResult.Loading -> {}
            }
        }
    }

    fun fail(
        failureReason: ReQcFailureReason,
        failureNotes: String,
        affectedQuantity: Int,
        quantityUnit: String,
        failedItemIds: List<String>,
        inspectorId: String,
        inspectorName: String?,
        nextAction: String?,
        timestamp: String
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true, errorMessage = null) }
            val result = reQcRepository.failReQc(
                reQcId = reQcId,
                failureReason = failureReason,
                failureNotes = failureNotes,
                affectedQuantity = affectedQuantity,
                quantityUnit = quantityUnit,
                failedItemIds = failedItemIds,
                inspectorId = inspectorId,
                inspectorName = inspectorName,
                nextAction = nextAction,
                timestamp = timestamp,
                callerRole = currentUserRole
            )
            when (result) {
                is DomainResult.Success -> _uiState.update { it.copy(isSubmitting = false, actionMessage = "Re-QC inspection marked FAILED.") }
                is DomainResult.Error -> _uiState.update { it.copy(isSubmitting = false, errorMessage = result.message) }
                is DomainResult.Loading -> {}
            }
        }
    }

    fun returnToRework(actorId: String, actorName: String?, notes: String?, timestamp: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true, errorMessage = null) }
            val result = reQcRepository.returnToRework(
                reQcId = reQcId,
                actorId = actorId,
                actorName = actorName,
                notes = notes,
                timestamp = timestamp,
                callerRole = currentUserRole
            )
            when (result) {
                is DomainResult.Success -> _uiState.update { it.copy(isSubmitting = false, actionMessage = "Returned to rework.") }
                is DomainResult.Error -> _uiState.update { it.copy(isSubmitting = false, errorMessage = result.message) }
                is DomainResult.Loading -> {}
            }
        }
    }

    fun cancel(reason: String, cancelledBy: String, cancelledByName: String?, timestamp: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true, errorMessage = null) }
            val result = reQcRepository.cancelReQc(
                reQcId = reQcId,
                reason = reason,
                cancelledBy = cancelledBy,
                cancelledByName = cancelledByName,
                timestamp = timestamp,
                callerRole = currentUserRole
            )
            when (result) {
                is DomainResult.Success -> _uiState.update { it.copy(isSubmitting = false, actionMessage = "Re-QC cancelled.") }
                is DomainResult.Error -> _uiState.update { it.copy(isSubmitting = false, errorMessage = result.message) }
                is DomainResult.Loading -> {}
            }
        }
    }

    fun createNextCycle(
        newReworkId: String,
        cycleType: ReQcCycleType = ReQcCycleType.REPEAT_FAILURE,
        createdBy: String,
        createdByName: String?,
        notes: String?,
        timestamp: String
    ) {
        val current = _uiState.value.reQc ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true, errorMessage = null) }
            val result = reQcRepository.createNextCycle(
                projectId = current.projectId,
                productionJobId = current.productionJobId,
                productionReworkId = newReworkId,
                previousReQcId = current.reQcId,
                cycleType = cycleType,
                originalQcId = current.originalQcId,
                originalDefectId = current.originalDefectId,
                checklistId = current.checklistId,
                affectedQuantity = current.affectedQuantity,
                quantityUnit = current.quantityUnit,
                createdBy = createdBy,
                createdByName = createdByName,
                notes = notes,
                timestamp = timestamp,
                callerRole = currentUserRole
            )
            when (result) {
                is DomainResult.Success -> _uiState.update { it.copy(isSubmitting = false, actionMessage = "Next Re-QC cycle created.") }
                is DomainResult.Error -> _uiState.update { it.copy(isSubmitting = false, errorMessage = result.message) }
                is DomainResult.Loading -> {}
            }
        }
    }
}
