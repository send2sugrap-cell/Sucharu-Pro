package com.sucharu.sucharupro.ui.features.delivery.returning

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sucharu.sucharupro.data.datasource.DeliveryOrderDataSource
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.delivery.returning.DeliveryReturn
import com.sucharu.sucharupro.domain.model.delivery.returning.DeliveryReturnLine
import com.sucharu.sucharupro.domain.model.delivery.returning.DeliveryReturnPriority
import com.sucharu.sucharupro.domain.model.delivery.returning.DeliveryReturnReason
import com.sucharu.sucharupro.domain.model.delivery.returning.DeliveryReturnStatus
import com.sucharu.sucharupro.domain.model.delivery.returning.DeliveryReturnType
import com.sucharu.sucharupro.domain.model.user.UserRole
import com.sucharu.sucharupro.domain.repository.DeliveryReturnRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

class DeliveryReturnFormViewModel(
    private val repository: DeliveryReturnRepository,
    private val doDataSource: DeliveryOrderDataSource
) : ViewModel() {

    private val _uiState = MutableStateFlow(DeliveryReturnFormUiState())
    val uiState: StateFlow<DeliveryReturnFormUiState> = _uiState.asStateFlow()

    fun initialize(projectId: String, deliveryOrderId: String) {
        _uiState.update {
            it.copy(
                isLoading = true,
                projectId = projectId,
                deliveryOrderId = deliveryOrderId,
                returnNo = "RET-${System.currentTimeMillis().toString().takeLast(6)}"
            )
        }
        viewModelScope.launch {
            val lines = doDataSource.getDeliveryOrderLines(deliveryOrderId)
            _uiState.update {
                it.copy(
                    isLoading = false,
                    availableOrderLines = lines,
                    selectedDoLineId = lines.firstOrNull()?.lineId ?: ""
                )
            }
        }
    }

    fun onReturnNoChanged(value: String) = _uiState.update { it.copy(returnNo = value) }
    fun onTypeSelected(type: DeliveryReturnType) = _uiState.update { it.copy(returnType = type) }
    fun onReasonSelected(reason: DeliveryReturnReason) = _uiState.update { it.copy(returnReason = reason) }
    fun onPrioritySelected(priority: DeliveryReturnPriority) = _uiState.update { it.copy(priority = priority) }
    fun onLineSelected(lineId: String) = _uiState.update { it.copy(selectedDoLineId = lineId) }
    fun onQuantityChanged(qty: String) = _uiState.update { it.copy(returnQuantityText = qty) }
    fun onNotesChanged(notes: String) = _uiState.update { it.copy(notes = notes) }

    fun submitCreateReturn(actorId: String, callerRole: UserRole) {
        val state = _uiState.value
        val qty = state.returnQuantityText.toDoubleOrNull()
        if (qty == null || qty <= 0) {
            _uiState.update { it.copy(errorMessage = "Please enter a valid positive return quantity.") }
            return
        }

        val selectedLine = state.availableOrderLines.find { it.lineId == state.selectedDoLineId }
        if (selectedLine == null) {
            _uiState.update { it.copy(errorMessage = "Please select a product line to return.") }
            return
        }

        _uiState.update { it.copy(isSaving = true, errorMessage = null) }
        viewModelScope.launch {
            val returnId = UUID.randomUUID().toString()
            val now = System.currentTimeMillis()

            val ret = DeliveryReturn(
                returnId = returnId,
                projectId = state.projectId,
                returnNo = state.returnNo,
                deliveryOrderId = state.deliveryOrderId,
                returnType = state.returnType,
                returnReason = state.returnReason,
                status = DeliveryReturnStatus.DRAFT,
                priority = state.priority,
                requestedBy = actorId,
                notes = state.notes.ifBlank { null },
                createdAt = now,
                updatedAt = now
            )

            val line = DeliveryReturnLine(
                returnLineId = UUID.randomUUID().toString(),
                returnId = returnId,
                projectId = state.projectId,
                deliveryOrderLineId = selectedLine.lineId,
                productId = selectedLine.productId,
                returnedQuantity = qty,
                createdAt = now,
                updatedAt = now
            )

            when (val result = repository.createReturn(ret, listOf(line), actorId, callerRole)) {
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
