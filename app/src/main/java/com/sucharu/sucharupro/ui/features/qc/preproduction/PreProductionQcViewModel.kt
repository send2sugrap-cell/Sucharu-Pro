package com.sucharu.sucharupro.ui.features.qc.preproduction

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.qc.PreProductionItemStatus
import com.sucharu.sucharupro.domain.model.qc.PreProductionQcSnapshot
import com.sucharu.sucharupro.domain.model.qc.QcDecision
import com.sucharu.sucharupro.domain.model.user.UserRole
import com.sucharu.sucharupro.domain.repository.ProductionQcRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

/**
 * ViewModel for Pre-Production QC inspection, item checking, and final submission (Module 06 Step 02).
 */
class PreProductionQcViewModel(
    private val qcId: String,
    private val qcRepository: ProductionQcRepository,
    private val currentUserRole: UserRole = UserRole.QC_INSPECTOR
) : ViewModel() {

    private val _actionState = MutableStateFlow<Triple<Boolean, String?, String?>>(Triple(false, null, null))
    private val _uiState = MutableStateFlow<PreProductionQcUiState>(PreProductionQcUiState.Loading)
    val uiState: StateFlow<PreProductionQcUiState> = _uiState.asStateFlow()

    init {
        loadPreProductionQc()
    }

    private fun loadPreProductionQc() {
        viewModelScope.launch {
            // Auto initialize items if needed
            qcRepository.initializePreProductionItems(qcId, currentUserRole)

            combine(
                qcRepository.getQcById(qcId),
                qcRepository.observePreProductionItems(qcId),
                qcRepository.getPreProductionSnapshot(qcId),
                _actionState
            ) { qc, items, snapshot, action ->
                if (qc == null) {
                    PreProductionQcUiState.Error("Pre-Production QC record '$qcId' not found.")
                } else {
                    PreProductionQcUiState.Success(
                        qc = qc,
                        items = items,
                        snapshot = snapshot,
                        isSubmitting = action.first,
                        actionMessage = action.second,
                        actionError = action.third
                    )
                }
            }.collect { state ->
                _uiState.value = state
            }
        }
    }

    fun updateItemStatus(
        itemId: String,
        status: PreProductionItemStatus,
        notes: String?,
        checkedBy: String,
        checkedByName: String?,
        timestamp: String
    ) {
        viewModelScope.launch {
            _actionState.value = Triple(true, "Updating check item...", null)
            val result = qcRepository.updatePreProductionItem(
                itemId = itemId,
                status = status,
                notes = notes,
                checkedBy = checkedBy,
                checkedByName = checkedByName,
                timestamp = timestamp,
                callerRole = currentUserRole
            )
            when (result) {
                is DomainResult.Success -> _actionState.value = Triple(false, "Item updated to ${status.defaultLabel}.", null)
                is DomainResult.Error -> _actionState.value = Triple(false, null, result.message)
                is DomainResult.Loading -> {}
            }
        }
    }

    fun submitQc(
        decision: QcDecision,
        snapshot: PreProductionQcSnapshot?,
        submittedBy: String,
        submittedByName: String?,
        notes: String?,
        timestamp: String
    ) {
        viewModelScope.launch {
            _actionState.value = Triple(true, "Submitting Pre-Production QC...", null)
            val result = qcRepository.submitPreProductionQc(
                qcId = qcId,
                decision = decision,
                snapshot = snapshot,
                submittedBy = submittedBy,
                submittedByName = submittedByName,
                notes = notes,
                timestamp = timestamp,
                callerRole = currentUserRole
            )
            when (result) {
                is DomainResult.Success -> _actionState.value = Triple(false, "Pre-Production QC completed with ${decision.defaultLabel}.", null)
                is DomainResult.Error -> _actionState.value = Triple(false, null, result.message)
                is DomainResult.Loading -> {}
            }
        }
    }
}
