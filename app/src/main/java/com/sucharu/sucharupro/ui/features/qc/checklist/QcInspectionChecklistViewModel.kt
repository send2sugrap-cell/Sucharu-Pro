package com.sucharu.sucharupro.ui.features.qc.checklist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.qc.QcDecision
import com.sucharu.sucharupro.domain.model.qc.QcResponseStatus
import com.sucharu.sucharupro.domain.model.user.UserRole
import com.sucharu.sucharupro.domain.repository.QcChecklistRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch

/**
 * ViewModel for executing and responding to a QC Inspection Checklist (Module 06 Step 03).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class QcInspectionChecklistViewModel(
    private val inspectionChecklistId: String,
    private val repository: QcChecklistRepository,
    private val currentUserRole: UserRole = UserRole.QC_INSPECTOR
) : ViewModel() {

    private val _actionState = MutableStateFlow<Triple<Boolean, String?, String?>>(Triple(false, null, null))
    private val _uiState = MutableStateFlow<QcInspectionChecklistUiState>(QcInspectionChecklistUiState.Loading)
    val uiState: StateFlow<QcInspectionChecklistUiState> = _uiState.asStateFlow()

    init {
        loadChecklist()
    }

    private fun loadChecklist() {
        viewModelScope.launch {
            repository.observeInspectionChecklist(inspectionChecklistId)
                .flatMapLatest { checklist ->
                    if (checklist == null) {
                        flowOf(QcInspectionChecklistUiState.Error("Checklist not found: $inspectionChecklistId"))
                    } else {
                        combine(
                            repository.observeItems(checklist.checklistTemplateId),
                            repository.observeResponses(checklist.inspectionId),
                            _actionState
                        ) { items, responses, action ->
                            QcInspectionChecklistUiState.Success(
                                checklist = checklist,
                                items = items,
                                responses = responses,
                                isSubmitting = action.first,
                                actionMessage = action.second,
                                actionError = action.third
                            )
                        }
                    }
                }.collect { state ->
                    _uiState.value = state
                }
        }
    }

    fun startChecklist(timestamp: String) {
        viewModelScope.launch {
            _actionState.value = Triple(true, "Starting checklist...", null)
            val result = repository.startChecklist(inspectionChecklistId, timestamp, currentUserRole)
            when (result) {
                is DomainResult.Success -> _actionState.value = Triple(false, "Checklist started.", null)
                is DomainResult.Error -> _actionState.value = Triple(false, null, result.message)
                is DomainResult.Loading -> {}
            }
        }
    }

    fun saveResponse(
        inspectionId: String,
        checklistItemId: String,
        status: QcResponseStatus,
        value: String?,
        numericValue: Double?,
        selectedValue: String?,
        remarks: String?,
        respondedBy: String,
        respondedByName: String?,
        timestamp: String
    ) {
        viewModelScope.launch {
            _actionState.value = Triple(true, "Saving response...", null)
            val result = repository.saveResponse(
                inspectionId = inspectionId,
                checklistItemId = checklistItemId,
                status = status,
                value = value,
                numericValue = numericValue,
                selectedValue = selectedValue,
                remarks = remarks,
                respondedBy = respondedBy,
                respondedByName = respondedByName,
                timestamp = timestamp,
                callerRole = currentUserRole
            )
            when (result) {
                is DomainResult.Success -> _actionState.value = Triple(false, "Response recorded.", null)
                is DomainResult.Error -> _actionState.value = Triple(false, null, result.message)
                is DomainResult.Loading -> {}
            }
        }
    }

    fun completeChecklist(
        decision: QcDecision,
        completedBy: String,
        completedByName: String?,
        notes: String?,
        timestamp: String
    ) {
        viewModelScope.launch {
            _actionState.value = Triple(true, "Completing checklist...", null)
            val result = repository.completeInspectionChecklist(
                inspectionChecklistId = inspectionChecklistId,
                decision = decision,
                completedBy = completedBy,
                completedByName = completedByName,
                notes = notes,
                timestamp = timestamp,
                callerRole = currentUserRole
            )
            when (result) {
                is DomainResult.Success -> _actionState.value = Triple(false, "Checklist completed with ${decision.defaultLabel}.", null)
                is DomainResult.Error -> _actionState.value = Triple(false, null, result.message)
                is DomainResult.Loading -> {}
            }
        }
    }
}
