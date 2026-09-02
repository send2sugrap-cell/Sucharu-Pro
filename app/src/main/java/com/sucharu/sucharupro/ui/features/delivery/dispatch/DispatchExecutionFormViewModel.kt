package com.sucharu.sucharupro.ui.features.delivery.dispatch

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.delivery.challan.DeliveryChallanStatus
import com.sucharu.sucharupro.domain.model.delivery.dispatch.DispatchExecution
import com.sucharu.sucharupro.domain.model.delivery.dispatch.DispatchExecutionLine
import com.sucharu.sucharupro.domain.model.delivery.dispatch.DispatchExecutionStatus
import com.sucharu.sucharupro.domain.model.delivery.dispatch.DispatchExecutionType
import com.sucharu.sucharupro.domain.model.user.UserRole
import com.sucharu.sucharupro.domain.repository.DeliveryChallanRepository
import com.sucharu.sucharupro.domain.repository.DispatchExecutionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * ViewModel for Creating Dispatch Executions (Module 08 Step 03).
 */
class DispatchExecutionFormViewModel(
    private val dispatchRepository: DispatchExecutionRepository,
    private val challanRepository: DeliveryChallanRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DispatchExecutionFormUiState())
    val uiState: StateFlow<DispatchExecutionFormUiState> = _uiState.asStateFlow()

    fun initialize(projectId: String, preselectedChallanId: String? = null) {
        val defaultDispatchNo = "DISP-${System.currentTimeMillis() % 100000}"
        _uiState.update { it.copy(isLoading = true, projectId = projectId, dispatchNo = defaultDispatchNo) }

        viewModelScope.launch {
            val allChallans = challanRepository.observeChallans(projectId).first()
            val eligibleChallans = allChallans.filter {
                it.status == DeliveryChallanStatus.APPROVED || it.status == DeliveryChallanStatus.READY_FOR_DISPATCH
            }

            _uiState.update { it.copy(isLoading = false, availableChallans = eligibleChallans) }

            val targetChallanId = preselectedChallanId ?: eligibleChallans.firstOrNull()?.challanId
            if (targetChallanId != null) {
                selectDeliveryChallan(targetChallanId)
            }
        }
    }

    fun selectDeliveryChallan(deliveryChallanId: String) {
        viewModelScope.launch {
            val linesResult = challanRepository.getChallanLines(deliveryChallanId)
            val challanLines = if (linesResult is DomainResult.Success) linesResult.data else emptyList()

            val formLines = challanLines.map { line ->
                DispatchExecutionLineFormItem(
                    lineId = UUID.randomUUID().toString(),
                    deliveryChallanLineId = line.lineId,
                    deliveryOrderLineId = line.deliveryOrderLineId,
                    productId = line.productId,
                    requestedQuantity = line.quantity,
                    dispatchQuantity = line.quantity,
                    sourceLocationId = _uiState.value.sourceLocationId,
                    batchId = line.batchId ?: "",
                    lotId = line.lotId ?: ""
                )
            }

            _uiState.update {
                it.copy(
                    selectedDeliveryChallanId = deliveryChallanId,
                    lines = formLines
                )
            }
        }
    }

    fun onDispatchNoChanged(value: String) {
        _uiState.update { it.copy(dispatchNo = value) }
    }

    fun onWarehouseChanged(warehouseId: String) {
        _uiState.update { it.copy(sourceWarehouseId = warehouseId) }
    }

    fun onLocationChanged(locationId: String) {
        _uiState.update { current ->
            val updatedLines = current.lines.map { it.copy(sourceLocationId = locationId) }
            current.copy(sourceLocationId = locationId, lines = updatedLines)
        }
    }

    fun onDispatchTypeChanged(type: DispatchExecutionType) {
        _uiState.update { it.copy(dispatchType = type) }
    }

    fun onNotesChanged(notes: String) {
        _uiState.update { it.copy(notes = notes) }
    }

    fun updateLineQuantity(index: Int, qty: Double) {
        _uiState.update { current ->
            val updated = current.lines.toMutableList()
            if (index in updated.indices) {
                updated[index] = updated[index].copy(dispatchQuantity = qty)
            }
            current.copy(lines = updated)
        }
    }

    fun updateLineBatch(index: Int, batchId: String) {
        _uiState.update { current ->
            val updated = current.lines.toMutableList()
            if (index in updated.indices) {
                updated[index] = updated[index].copy(batchId = batchId)
            }
            current.copy(lines = updated)
        }
    }

    fun updateLineLot(index: Int, lotId: String) {
        _uiState.update { current ->
            val updated = current.lines.toMutableList()
            if (index in updated.indices) {
                updated[index] = updated[index].copy(lotId = lotId)
            }
            current.copy(lines = updated)
        }
    }

    fun saveDispatch(actorId: String, callerRole: UserRole) {
        val state = _uiState.value
        val now = System.currentTimeMillis()
        val dispatchExecutionId = UUID.randomUUID().toString()

        val selectedChallan = state.availableChallans.find { it.challanId == state.selectedDeliveryChallanId }

        val dispatch = DispatchExecution(
            dispatchExecutionId = dispatchExecutionId,
            projectId = state.projectId,
            dispatchNo = state.dispatchNo.trim(),
            deliveryOrderId = selectedChallan?.deliveryOrderId ?: "",
            deliveryChallanId = state.selectedDeliveryChallanId,
            customerId = selectedChallan?.customerId,
            sourceWarehouseId = state.sourceWarehouseId.trim(),
            sourceLocationId = state.sourceLocationId.trim(),
            dispatchType = state.dispatchType,
            status = DispatchExecutionStatus.DRAFT,
            dispatchDate = state.dispatchDate,
            notes = state.notes.trim().ifBlank { null },
            createdBy = actorId,
            createdAt = now,
            updatedAt = now
        )

        val domainLines = state.lines.map { item ->
            DispatchExecutionLine(
                dispatchExecutionLineId = item.lineId,
                projectId = state.projectId,
                dispatchExecutionId = dispatchExecutionId,
                deliveryChallanLineId = item.deliveryChallanLineId,
                deliveryOrderLineId = item.deliveryOrderLineId,
                productId = item.productId,
                requestedQuantity = item.requestedQuantity,
                dispatchQuantity = item.dispatchQuantity,
                batchId = item.batchId.trim().ifBlank { null },
                lotId = item.lotId.trim().ifBlank { null },
                sourceLocationId = item.sourceLocationId.trim(),
                createdAt = now
            )
        }

        _uiState.update { it.copy(isSaving = true, errorMessage = null) }
        viewModelScope.launch {
            when (val result = dispatchRepository.createDispatch(dispatch, domainLines, callerRole)) {
                is DomainResult.Success -> {
                    _uiState.update { it.copy(isSaving = false, isSavedSuccessfully = true) }
                }
                is DomainResult.Error -> {
                    _uiState.update { it.copy(isSaving = false, errorMessage = result.message) }
                }
                is DomainResult.Loading -> {
                    _uiState.update { it.copy(isSaving = true) }
                }
            }
        }
    }
}
