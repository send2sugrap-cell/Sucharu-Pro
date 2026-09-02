package com.sucharu.sucharupro.ui.features.qc.defect

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.user.UserRole
import com.sucharu.sucharupro.domain.repository.ProductionDefectRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel for QC Defect Details Screen (Module 06 Step 04).
 */
class ProductionDefectDetailsViewModel(
    private val defectId: String,
    private val defectRepository: ProductionDefectRepository,
    private val currentUserRole: UserRole? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProductionDefectDetailsUiState(isLoading = true))
    val uiState: StateFlow<ProductionDefectDetailsUiState> = _uiState.asStateFlow()

    init {
        loadDefectData()
    }

    private fun loadDefectData() {
        viewModelScope.launch {
            defectRepository.observeDefectById(defectId)
                .catch { ex -> _uiState.update { it.copy(errorMessage = ex.message, isLoading = false) } }
                .collect { defect ->
                    _uiState.update { it.copy(defect = defect, isLoading = false) }
                }
        }

        viewModelScope.launch {
            defectRepository.observeAssignments(defectId)
                .catch { /* ignore */ }
                .collect { assignments ->
                    _uiState.update { it.copy(assignments = assignments) }
                }
        }

        viewModelScope.launch {
            defectRepository.observeDefectActivity(defectId)
                .catch { /* ignore */ }
                .collect { activities ->
                    _uiState.update { it.copy(activityEvents = activities) }
                }
        }
    }

    fun acknowledge(acknowledgedBy: String, acknowledgedByName: String?, timestamp: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true, errorMessage = null) }
            val result = defectRepository.acknowledgeDefect(
                defectId = defectId,
                acknowledgedBy = acknowledgedBy,
                acknowledgedByName = acknowledgedByName,
                timestamp = timestamp,
                callerRole = currentUserRole
            )
            when (result) {
                is DomainResult.Success -> _uiState.update { it.copy(isSubmitting = false, actionMessage = "Defect acknowledged.") }
                is DomainResult.Error -> _uiState.update { it.copy(isSubmitting = false, errorMessage = result.message) }
                is DomainResult.Loading -> {}
            }
        }
    }

    fun investigate(investigatorId: String, investigatorName: String?, timestamp: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true, errorMessage = null) }
            val result = defectRepository.investigateDefect(
                defectId = defectId,
                investigatorId = investigatorId,
                investigatorName = investigatorName,
                timestamp = timestamp,
                callerRole = currentUserRole
            )
            when (result) {
                is DomainResult.Success -> _uiState.update { it.copy(isSubmitting = false, actionMessage = "Investigation started.") }
                is DomainResult.Error -> _uiState.update { it.copy(isSubmitting = false, errorMessage = result.message) }
                is DomainResult.Loading -> {}
            }
        }
    }

    fun contain(containmentNotes: String, containedBy: String, containedByName: String?, timestamp: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true, errorMessage = null) }
            val result = defectRepository.containDefect(
                defectId = defectId,
                containmentNotes = containmentNotes,
                containedBy = containedBy,
                containedByName = containedByName,
                timestamp = timestamp,
                callerRole = currentUserRole
            )
            when (result) {
                is DomainResult.Success -> _uiState.update { it.copy(isSubmitting = false, actionMessage = "Defect contained.") }
                is DomainResult.Error -> _uiState.update { it.copy(isSubmitting = false, errorMessage = result.message) }
                is DomainResult.Loading -> {}
            }
        }
    }

    fun resolve(resolutionNotes: String, resolvedBy: String, resolvedByName: String?, timestamp: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true, errorMessage = null) }
            val result = defectRepository.resolveDefect(
                defectId = defectId,
                resolutionNotes = resolutionNotes,
                resolvedBy = resolvedBy,
                resolvedByName = resolvedByName,
                timestamp = timestamp,
                callerRole = currentUserRole
            )
            when (result) {
                is DomainResult.Success -> _uiState.update { it.copy(isSubmitting = false, actionMessage = "Defect resolved.") }
                is DomainResult.Error -> _uiState.update { it.copy(isSubmitting = false, errorMessage = result.message) }
                is DomainResult.Loading -> {}
            }
        }
    }

    fun close(closedBy: String, closedByName: String?, timestamp: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true, errorMessage = null) }
            val result = defectRepository.closeDefect(
                defectId = defectId,
                closedBy = closedBy,
                closedByName = closedByName,
                timestamp = timestamp,
                callerRole = currentUserRole
            )
            when (result) {
                is DomainResult.Success -> _uiState.update { it.copy(isSubmitting = false, actionMessage = "Defect closed.") }
                is DomainResult.Error -> _uiState.update { it.copy(isSubmitting = false, errorMessage = result.message) }
                is DomainResult.Loading -> {}
            }
        }
    }

    fun cancel(reason: String, cancelledBy: String, cancelledByName: String?, timestamp: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true, errorMessage = null) }
            val result = defectRepository.cancelDefect(
                defectId = defectId,
                reason = reason,
                cancelledBy = cancelledBy,
                cancelledByName = cancelledByName,
                timestamp = timestamp,
                callerRole = currentUserRole
            )
            when (result) {
                is DomainResult.Success -> _uiState.update { it.copy(isSubmitting = false, actionMessage = "Defect cancelled.") }
                is DomainResult.Error -> _uiState.update { it.copy(isSubmitting = false, errorMessage = result.message) }
                is DomainResult.Loading -> {}
            }
        }
    }
}
