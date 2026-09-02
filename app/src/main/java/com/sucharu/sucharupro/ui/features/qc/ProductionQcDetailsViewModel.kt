package com.sucharu.sucharupro.ui.features.qc

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.qc.QcDecision
import com.sucharu.sucharupro.domain.repository.ProductionQcRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

/**
 * ViewModel for Quality Control inspection details, assignment, inspection start, and completion (Module 06 Step 01).
 */
class ProductionQcDetailsViewModel(
    private val qcId: String,
    private val qcRepository: ProductionQcRepository
) : ViewModel() {

    private val _actionState = MutableStateFlow<Triple<Boolean, String?, String?>>(Triple(false, null, null))
    private val _uiState = MutableStateFlow<ProductionQcDetailsUiState>(ProductionQcDetailsUiState.Loading)
    val uiState: StateFlow<ProductionQcDetailsUiState> = _uiState.asStateFlow()

    init {
        loadQcDetails()
    }

    private fun loadQcDetails() {
        viewModelScope.launch {
            combine(
                qcRepository.getQcById(qcId),
                qcRepository.observeAssignments(qcId),
                qcRepository.observeActivityEvents(qcId),
                _actionState
            ) { qc, assignments, activities, action ->
                if (qc == null) {
                    ProductionQcDetailsUiState.Error("QC record '$qcId' not found.")
                } else {
                    ProductionQcDetailsUiState.Success(
                        qc = qc,
                        assignments = assignments,
                        activities = activities,
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

    fun assignInspector(inspectorId: String, inspectorName: String, assignedBy: String?, reason: String?, timestamp: String) {
        viewModelScope.launch {
            _actionState.value = Triple(true, "Assigning inspector...", null)
            val result = qcRepository.assignInspector(
                qcId = qcId,
                inspectorId = inspectorId,
                inspectorName = inspectorName,
                assignedBy = assignedBy,
                reason = reason,
                timestamp = timestamp
            )
            when (result) {
                is DomainResult.Success -> _actionState.value = Triple(false, "Inspector assigned.", null)
                is DomainResult.Error -> _actionState.value = Triple(false, null, result.message)
                is DomainResult.Loading -> {}
            }
        }
    }

    fun startInspection(inspectorId: String, inspectorName: String?, notes: String?, timestamp: String) {
        viewModelScope.launch {
            _actionState.value = Triple(true, "Starting inspection...", null)
            val result = qcRepository.startInspection(
                qcId = qcId,
                inspectorId = inspectorId,
                inspectorName = inspectorName,
                notes = notes,
                timestamp = timestamp
            )
            when (result) {
                is DomainResult.Success -> _actionState.value = Triple(false, "Inspection started.", null)
                is DomainResult.Error -> _actionState.value = Triple(false, null, result.message)
                is DomainResult.Loading -> {}
            }
        }
    }

    fun completeInspection(decision: QcDecision, notes: String?, inspectorId: String, inspectorName: String?, timestamp: String) {
        viewModelScope.launch {
            _actionState.value = Triple(true, "Completing inspection...", null)
            val result = qcRepository.completeInspection(
                qcId = qcId,
                decision = decision,
                notes = notes,
                inspectorId = inspectorId,
                inspectorName = inspectorName,
                timestamp = timestamp
            )
            when (result) {
                is DomainResult.Success -> _actionState.value = Triple(false, "Inspection completed with decision: ${decision.defaultLabel}", null)
                is DomainResult.Error -> _actionState.value = Triple(false, null, result.message)
                is DomainResult.Loading -> {}
            }
        }
    }
}
