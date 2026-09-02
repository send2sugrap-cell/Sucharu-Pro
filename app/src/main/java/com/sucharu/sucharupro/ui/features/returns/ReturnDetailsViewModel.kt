package com.sucharu.sucharupro.ui.features.returns

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.common.Money
import com.sucharu.sucharupro.domain.model.returns.ReturnReason
import com.sucharu.sucharupro.domain.model.returns.ReturnReceivingInfo
import com.sucharu.sucharupro.domain.model.returns.ReturnReconciliationResult
import com.sucharu.sucharupro.domain.model.returns.ReturnResolutionType
import com.sucharu.sucharupro.domain.model.returns.ReturnSettlement
import com.sucharu.sucharupro.domain.model.returns.ReturnSettlementStatus
import com.sucharu.sucharupro.domain.model.returns.ReturnStatus
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
 * ViewModel managing the Return Request Details Screen (Module 11 Step 02, 03, 04, 05).
 */
class ReturnDetailsViewModel(
    private val repository: ReturnRepository,
    private val coroutineScope: CoroutineScope? = null
) : ViewModel() {

    private val scope get() = coroutineScope ?: viewModelScope

    private val _uiState = MutableStateFlow(ReturnDetailsUiState(isLoading = true))
    val uiState: StateFlow<ReturnDetailsUiState> = _uiState.asStateFlow()

    fun loadDetails(
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

            val auditRes = repository.getAuditHistory(returnId, callerRole, callerProjectId)
            val audit = if (auditRes is DomainResult.Success) auditRes.data else emptyList()

            val inspectionRes = repository.getInspection(returnId, callerRole, callerProjectId)
            val inspection = if (inspectionRes is DomainResult.Success) inspectionRes.data else null

            val receivingRes = repository.getReceiving(returnId, callerRole, callerProjectId)
            val receiving = if (receivingRes is DomainResult.Success) receivingRes.data else null

            val reconciliationRes = repository.getReconciliationResult(returnId, callerRole, callerProjectId)
            val reconciliation = if (reconciliationRes is DomainResult.Success) reconciliationRes.data else null

            val settlementRes = repository.getSettlement(returnId, callerRole, callerProjectId)
            val settlement = if (settlementRes is DomainResult.Success) settlementRes.data else null

            _uiState.update {
                it.copy(
                    isLoading = false,
                    returnRequest = request,
                    items = items,
                    inspection = inspection,
                    receivingInfo = receiving,
                    reconciliationResult = reconciliation,
                    settlement = settlement,
                    auditEvents = audit
                )
            }
        }
    }

    fun submitForInspection(
        actorId: String,
        callerRole: UserRole? = null,
        callerProjectId: String? = null
    ) {
        val current = _uiState.value.returnRequest ?: return
        _uiState.update { it.copy(isSubmittingAction = true, errorMessage = null) }

        scope.launch {
            val res = repository.submitForInspection(
                returnId = current.returnId,
                actorId = actorId,
                expectedVersion = current.version,
                callerRole = callerRole,
                callerProjectId = callerProjectId
            )
            when (res) {
                is DomainResult.Success -> {
                    val auditRes = repository.getAuditHistory(current.returnId, callerRole, callerProjectId)
                    val audit = if (auditRes is DomainResult.Success) auditRes.data else _uiState.value.auditEvents
                    _uiState.update {
                        it.copy(
                            isSubmittingAction = false,
                            returnRequest = res.data,
                            auditEvents = audit,
                            actionSuccessMessage = "Return submitted for inspection successfully."
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

    fun cancelReturn(
        actorId: String,
        callerRole: UserRole? = null,
        callerProjectId: String? = null
    ) {
        val current = _uiState.value.returnRequest ?: return
        _uiState.update { it.copy(isSubmittingAction = true, errorMessage = null) }

        scope.launch {
            val res = repository.cancelReturnRequest(
                returnId = current.returnId,
                actorId = actorId,
                expectedVersion = current.version,
                callerRole = callerRole,
                callerProjectId = callerProjectId
            )
            when (res) {
                is DomainResult.Success -> {
                    val auditRes = repository.getAuditHistory(current.returnId, callerRole, callerProjectId)
                    val audit = if (auditRes is DomainResult.Success) auditRes.data else _uiState.value.auditEvents
                    _uiState.update {
                        it.copy(
                            isSubmittingAction = false,
                            returnRequest = res.data,
                            auditEvents = audit,
                            actionSuccessMessage = "Return request cancelled."
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

    fun updateReasonAndDescription(
        newReason: ReturnReason,
        newDescription: String?,
        actorId: String,
        callerRole: UserRole? = null,
        callerProjectId: String? = null
    ) {
        val current = _uiState.value.returnRequest ?: return
        val items = _uiState.value.items
        _uiState.update { it.copy(isSubmittingAction = true, errorMessage = null) }

        scope.launch {
            val updated = current.copy(reason = newReason, description = newDescription)
            val res = repository.updateReturnRequest(
                request = updated,
                items = items,
                actorId = actorId,
                callerRole = callerRole,
                callerProjectId = callerProjectId
            )
            when (res) {
                is DomainResult.Success -> {
                    val auditRes = repository.getAuditHistory(current.returnId, callerRole, callerProjectId)
                    val audit = if (auditRes is DomainResult.Success) auditRes.data else _uiState.value.auditEvents
                    _uiState.update {
                        it.copy(
                            isSubmittingAction = false,
                            returnRequest = res.data,
                            auditEvents = audit,
                            actionSuccessMessage = "Return request details updated."
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

    // =========================================================================
    // Dialog Controls (Module 11 Step 04 Chunk 05)
    // =========================================================================

    fun openReceiveDialog() {
        _uiState.update { it.copy(showReceiveDialog = true, errorMessage = null) }
    }

    fun closeReceiveDialog() {
        _uiState.update { it.copy(showReceiveDialog = false) }
    }

    fun openReconcileDialog() {
        _uiState.update { it.copy(showReconcileDialog = true, errorMessage = null) }
    }

    fun closeReconcileDialog() {
        _uiState.update { it.copy(showReconcileDialog = false) }
    }

    // =========================================================================
    // Step 04 Receiving & Reconciliation Operations
    // =========================================================================

    fun receiveReturn(
        actualQty: Int,
        acceptedQty: Int,
        rejectedQty: Int,
        damagedQty: Int,
        remarks: String? = null,
        condition: String? = null,
        packaging: String? = null,
        damageNotes: String? = null,
        actorId: String,
        callerRole: UserRole? = null,
        callerProjectId: String? = null
    ) {
        val current = _uiState.value.returnRequest ?: return
        if (_uiState.value.isSubmittingAction) return
        _uiState.update { it.copy(isSubmittingAction = true, errorMessage = null) }

        val approvedQty = _uiState.value.items.sumOf { it.acceptedQuantity }.let { if (it > 0) it else actualQty }
        val receivingInfo = ReturnReceivingInfo(
            receivingEventId = UUID.randomUUID().toString(),
            returnId = current.returnId,
            projectId = current.projectId,
            receiverId = actorId,
            receivedAt = System.currentTimeMillis(),
            approvedQty = approvedQty,
            actualQty = actualQty,
            acceptedQty = acceptedQty,
            rejectedQty = rejectedQty,
            damagedQty = damagedQty,
            mismatchFlag = approvedQty != actualQty,
            condition = condition,
            packaging = packaging,
            damageNotes = damageNotes ?: remarks,
            version = current.version,
            idempotencyKey = "REC-${current.returnId}-${current.version}"
        )

        scope.launch {
            val res = repository.receiveReturn(
                receivingInfo = receivingInfo,
                actorId = actorId,
                expectedVersion = current.version,
                callerCustomerId = null,
                callerRole = callerRole,
                callerProjectId = callerProjectId
            )
            when (res) {
                is DomainResult.Success -> {
                    val receivingRes = repository.getReceiving(current.returnId, callerRole, callerProjectId)
                    val receiving = if (receivingRes is DomainResult.Success) receivingRes.data else receivingInfo

                    val auditRes = repository.getAuditHistory(current.returnId, callerRole, callerProjectId)
                    val audit = if (auditRes is DomainResult.Success) auditRes.data else _uiState.value.auditEvents

                    _uiState.update {
                        it.copy(
                            isSubmittingAction = false,
                            showReceiveDialog = false,
                            returnRequest = res.data,
                            receivingInfo = receiving,
                            auditEvents = audit,
                            actionSuccessMessage = "Return received successfully.",
                            errorMessage = null
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

    fun reconcileInventoryAndProcess(
        warehouseId: String,
        locationId: String,
        actorId: String,
        callerRole: UserRole? = null,
        callerProjectId: String? = null
    ) {
        val current = _uiState.value.returnRequest ?: return
        if (_uiState.value.isSubmittingAction) return
        _uiState.update { it.copy(isSubmittingAction = true, errorMessage = null) }

        scope.launch {
            val res = repository.reconcileInventoryAndProcess(
                returnId = current.returnId,
                warehouseId = warehouseId,
                locationId = locationId,
                actorId = actorId,
                expectedVersion = current.version,
                callerCustomerId = null,
                callerRole = callerRole,
                callerProjectId = callerProjectId
            )
            when (res) {
                is DomainResult.Success -> {
                    val updatedReturnRes = repository.getReturn(current.returnId, callerRole, callerProjectId)
                    val updatedReturn = if (updatedReturnRes is DomainResult.Success) {
                        updatedReturnRes.data
                    } else {
                        current.copy(status = ReturnStatus.PROCESSED, version = current.version + 1)
                    }

                    val receivingRes = repository.getReceiving(current.returnId, callerRole, callerProjectId)
                    val receiving = if (receivingRes is DomainResult.Success) receivingRes.data else _uiState.value.receivingInfo

                    val auditRes = repository.getAuditHistory(current.returnId, callerRole, callerProjectId)
                    val audit = if (auditRes is DomainResult.Success) auditRes.data else _uiState.value.auditEvents

                    _uiState.update {
                        it.copy(
                            isSubmittingAction = false,
                            showReconcileDialog = false,
                            returnRequest = updatedReturn,
                            reconciliationResult = res.data,
                            receivingInfo = receiving,
                            auditEvents = audit,
                            actionSuccessMessage = "Inventory reconciled and return processed successfully.",
                            errorMessage = null
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

    fun setSettleDialogVisible(visible: Boolean) {
        _uiState.update { it.copy(showSettleDialog = visible, errorMessage = null) }
    }

    fun settleReturn(
        resolutionType: ReturnResolutionType,
        amount: Money,
        creditNoteId: String? = null,
        replacementOrderId: String? = null,
        reworkId: String? = null,
        notes: String? = null,
        actorId: String,
        callerRole: UserRole? = null,
        callerProjectId: String? = null
    ) {
        val current = _uiState.value.returnRequest ?: return
        if (_uiState.value.isSubmittingAction) return
        _uiState.update { it.copy(isSubmittingAction = true, errorMessage = null) }

        val settlement = ReturnSettlement(
            settlementId = UUID.randomUUID().toString(),
            returnId = current.returnId,
            projectId = current.projectId,
            customerId = current.customerId,
            resolutionType = resolutionType,
            amount = amount,
            status = ReturnSettlementStatus.COMPLETED,
            creditNoteId = creditNoteId,
            replacementOrderId = replacementOrderId,
            reworkId = reworkId,
            notes = notes,
            settledBy = actorId,
            settledAt = System.currentTimeMillis(),
            version = 1L,
            idempotencyKey = UUID.randomUUID().toString()
        )

        scope.launch {
            val res = repository.settleReturn(
                settlement = settlement,
                actorId = actorId,
                expectedVersion = current.version,
                callerCustomerId = null,
                callerRole = callerRole,
                callerProjectId = callerProjectId
            )
            when (res) {
                is DomainResult.Success -> {
                    val updatedReturnRes = repository.getReturn(current.returnId, callerRole, callerProjectId)
                    val updatedReturn = if (updatedReturnRes is DomainResult.Success) {
                        updatedReturnRes.data
                    } else {
                        current.copy(version = current.version + 1)
                    }

                    val auditRes = repository.getAuditHistory(current.returnId, callerRole, callerProjectId)
                    val audit = if (auditRes is DomainResult.Success) auditRes.data else _uiState.value.auditEvents

                    _uiState.update {
                        it.copy(
                            isSubmittingAction = false,
                            showSettleDialog = false,
                            returnRequest = updatedReturn,
                            settlement = res.data,
                            auditEvents = audit,
                            actionSuccessMessage = "Return settled successfully via ${res.data.resolutionType.displayName}.",
                            errorMessage = null
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
}
