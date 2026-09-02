package com.sucharu.sucharupro.ui.features.returns

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.returns.InspectionChecklistItem
import com.sucharu.sucharupro.domain.model.returns.ReturnDecision
import com.sucharu.sucharupro.domain.model.returns.ReturnInspection
import com.sucharu.sucharupro.domain.model.returns.ReturnInspectionStatus
import com.sucharu.sucharupro.domain.model.returns.ReturnItem
import com.sucharu.sucharupro.domain.model.user.UserRole
import com.sucharu.sucharupro.domain.repository.ReturnRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * ViewModel managing Return Inspection & Decision Screen (Module 11 Step 03).
 */
class ReturnInspectionViewModel(
    private val repository: ReturnRepository,
    private val coroutineScope: CoroutineScope? = null
) : ViewModel() {

    private val scope get() = coroutineScope ?: viewModelScope

    private val _uiState = MutableStateFlow(ReturnInspectionUiState(isLoading = true))
    val uiState: StateFlow<ReturnInspectionUiState> = _uiState.asStateFlow()

    private val defaultChecklist = listOf(
        InspectionChecklistItem("chk-1", "Physical package & seal integrity verified", false),
        InspectionChecklistItem("chk-2", "Item condition matches declared return reason", false),
        InspectionChecklistItem("chk-3", "SKU, batch code and quantity verified against delivery order", false),
        InspectionChecklistItem("chk-4", "Defect/damage severity assessed and photographed", false)
    )

    fun loadInspection(
        returnId: String,
        callerRole: UserRole? = null,
        callerProjectId: String? = null
    ) {
        _uiState.update { it.copy(isLoading = true, errorMessage = null, actionSuccessMessage = null) }
        scope.launch {
            val returnRes = repository.getReturn(returnId, callerRole, callerProjectId)
            if (returnRes is DomainResult.Error) {
                _uiState.update { it.copy(isLoading = false, errorMessage = returnRes.message) }
                return@launch
            }

            val request = (returnRes as DomainResult.Success).data
            val itemsRes = repository.getReturnItems(returnId, callerRole, callerProjectId)
            val items = if (itemsRes is DomainResult.Success) itemsRes.data else emptyList()

            val inspectionRes = repository.getInspection(returnId, callerRole, callerProjectId)
            val inspection = if (inspectionRes is DomainResult.Success) inspectionRes.data else null

            val auditRes = repository.getAuditHistory(returnId, callerRole, callerProjectId)
            val audit = if (auditRes is DomainResult.Success) auditRes.data else emptyList()

            val checklist = if (inspection != null && inspection.checklist.isNotEmpty()) {
                inspection.checklist
            } else {
                defaultChecklist
            }

            val initialAccepted = items.associate { it.returnItemId to it.acceptedQuantity }
            val initialRejected = items.associate { it.returnItemId to it.rejectedQuantity }

            _uiState.update {
                it.copy(
                    isLoading = false,
                    returnRequest = request,
                    items = items,
                    inspection = inspection,
                    checklist = checklist,
                    findings = inspection?.findings ?: "",
                    decision = inspection?.decision,
                    decisionReason = inspection?.decisionReason ?: "",
                    itemAcceptedQuantities = initialAccepted,
                    itemRejectedQuantities = initialRejected,
                    auditEvents = audit
                )
            }
        }
    }

    fun toggleChecklistItem(itemId: String) {
        _uiState.update { state ->
            val updated = state.checklist.map {
                if (it.itemId == itemId) it.copy(isPassed = !it.isPassed) else it
            }
            state.copy(checklist = updated)
        }
    }

    fun updateFindings(findings: String) {
        _uiState.update { it.copy(findings = findings) }
    }

    fun updateAcceptedQuantity(returnItemId: String, qty: Int) {
        _uiState.update { state ->
            val current = state.itemAcceptedQuantities.toMutableMap()
            current[returnItemId] = qty.coerceAtLeast(0)
            state.copy(itemAcceptedQuantities = current)
        }
    }

    fun updateRejectedQuantity(returnItemId: String, qty: Int) {
        _uiState.update { state ->
            val current = state.itemRejectedQuantities.toMutableMap()
            current[returnItemId] = qty.coerceAtLeast(0)
            state.copy(itemRejectedQuantities = current)
        }
    }

    fun saveDraftInspection(
        actorId: String,
        callerRole: UserRole? = null,
        callerProjectId: String? = null
    ) {
        val req = _uiState.value.returnRequest ?: return
        val currentInspection = _uiState.value.inspection
        val inspection = ReturnInspection(
            inspectionId = currentInspection?.inspectionId ?: UUID.randomUUID().toString(),
            returnId = req.returnId,
            projectId = req.projectId,
            inspectorId = actorId,
            status = ReturnInspectionStatus.IN_PROGRESS,
            checklist = _uiState.value.checklist,
            findings = _uiState.value.findings.ifBlank { null },
            decision = null,
            decisionReason = null,
            version = currentInspection?.version ?: 1L
        )

        _uiState.update { it.copy(isSubmittingAction = true, errorMessage = null) }
        scope.launch {
            val res = repository.recordInspection(
                inspection = inspection,
                actorId = actorId,
                callerRole = callerRole,
                callerProjectId = callerProjectId
            )
            when (res) {
                is DomainResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isSubmittingAction = false,
                            inspection = res.data,
                            actionSuccessMessage = "Inspection draft saved successfully."
                        )
                    }
                }
                is DomainResult.Error -> {
                    _uiState.update {
                        it.copy(
                            isSubmittingAction = false,
                            errorMessage = res.message
                        )
                    }
                }
                is DomainResult.Loading -> {
                    _uiState.update { it.copy(isSubmittingAction = true) }
                }
            }
        }
    }

    fun approveReturn(
        actorId: String,
        callerRole: UserRole? = null,
        callerProjectId: String? = null
    ) {
        val req = _uiState.value.returnRequest ?: return
        val items = buildUpdatedItems()
        val currentInspection = _uiState.value.inspection
        val inspection = ReturnInspection(
            inspectionId = currentInspection?.inspectionId ?: UUID.randomUUID().toString(),
            returnId = req.returnId,
            projectId = req.projectId,
            inspectorId = actorId,
            status = ReturnInspectionStatus.COMPLETED,
            checklist = _uiState.value.checklist,
            findings = _uiState.value.findings.ifBlank { null },
            decision = ReturnDecision.APPROVE,
            decisionReason = null,
            version = currentInspection?.version ?: 1L
        )

        _uiState.update { it.copy(isSubmittingAction = true, errorMessage = null) }
        scope.launch {
            val res = repository.approveReturn(
                returnId = req.returnId,
                actorId = actorId,
                expectedVersion = req.version,
                inspection = inspection,
                items = items,
                callerRole = callerRole,
                callerProjectId = callerProjectId
            )
            when (res) {
                is DomainResult.Success -> {
                    val auditRes = repository.getAuditHistory(req.returnId, callerRole, callerProjectId)
                    val audit = if (auditRes is DomainResult.Success) auditRes.data else _uiState.value.auditEvents
                    _uiState.update {
                        it.copy(
                            isSubmittingAction = false,
                            returnRequest = res.data,
                            inspection = inspection,
                            auditEvents = audit,
                            actionSuccessMessage = "Return request APPROVED successfully."
                        )
                    }
                }
                is DomainResult.Error -> {
                    _uiState.update {
                        it.copy(
                            isSubmittingAction = false,
                            errorMessage = res.message
                        )
                    }
                }
                is DomainResult.Loading -> {
                    _uiState.update { it.copy(isSubmittingAction = true) }
                }
            }
        }
    }

    fun rejectReturn(
        rejectionReason: String,
        actorId: String,
        callerRole: UserRole? = null,
        callerProjectId: String? = null
    ) {
        if (rejectionReason.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Rejection reason cannot be blank.") }
            return
        }

        val req = _uiState.value.returnRequest ?: return
        val items = buildUpdatedItems()
        val currentInspection = _uiState.value.inspection
        val inspection = ReturnInspection(
            inspectionId = currentInspection?.inspectionId ?: UUID.randomUUID().toString(),
            returnId = req.returnId,
            projectId = req.projectId,
            inspectorId = actorId,
            status = ReturnInspectionStatus.COMPLETED,
            checklist = _uiState.value.checklist,
            findings = _uiState.value.findings.ifBlank { null },
            decision = ReturnDecision.REJECT,
            decisionReason = rejectionReason,
            version = currentInspection?.version ?: 1L
        )

        _uiState.update { it.copy(isSubmittingAction = true, errorMessage = null) }
        scope.launch {
            val res = repository.rejectReturn(
                returnId = req.returnId,
                actorId = actorId,
                expectedVersion = req.version,
                rejectionReason = rejectionReason,
                inspection = inspection,
                items = items,
                callerRole = callerRole,
                callerProjectId = callerProjectId
            )
            when (res) {
                is DomainResult.Success -> {
                    val auditRes = repository.getAuditHistory(req.returnId, callerRole, callerProjectId)
                    val audit = if (auditRes is DomainResult.Success) auditRes.data else _uiState.value.auditEvents
                    _uiState.update {
                        it.copy(
                            isSubmittingAction = false,
                            returnRequest = res.data,
                            inspection = inspection,
                            auditEvents = audit,
                            actionSuccessMessage = "Return request REJECTED."
                        )
                    }
                }
                is DomainResult.Error -> {
                    _uiState.update {
                        it.copy(
                            isSubmittingAction = false,
                            errorMessage = res.message
                        )
                    }
                }
                is DomainResult.Loading -> {
                    _uiState.update { it.copy(isSubmittingAction = true) }
                }
            }
        }
    }

    private fun buildUpdatedItems(): List<ReturnItem> {
        val current = _uiState.value.items
        val acceptedMap = _uiState.value.itemAcceptedQuantities
        val rejectedMap = _uiState.value.itemRejectedQuantities

        return current.map { item ->
            val accepted = acceptedMap[item.returnItemId] ?: item.acceptedQuantity
            val rejected = rejectedMap[item.returnItemId] ?: item.rejectedQuantity
            item.copy(acceptedQuantity = accepted, rejectedQuantity = rejected)
        }
    }
}
