package com.sucharu.sucharupro.ui.features.delivery

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.delivery.DeliveryOrder
import com.sucharu.sucharupro.domain.model.delivery.DeliveryOrderLine
import com.sucharu.sucharupro.domain.model.delivery.DeliveryOrderStatus
import com.sucharu.sucharupro.domain.model.delivery.DeliveryOrderType
import com.sucharu.sucharupro.domain.model.delivery.DeliveryPriority
import com.sucharu.sucharupro.domain.model.user.UserRole
import com.sucharu.sucharupro.domain.repository.DeliveryOrderRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * ViewModel for Delivery Order creation (Module 08 Step 01).
 */
class DeliveryOrderFormViewModel(
    private val repository: DeliveryOrderRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DeliveryOrderFormUiState())
    val uiState: StateFlow<DeliveryOrderFormUiState> = _uiState.asStateFlow()

    fun initialize(projectId: String) {
        val defaultDoNo = "DO-${System.currentTimeMillis() % 100000}"
        _uiState.update {
            it.copy(
                projectId = projectId,
                deliveryOrderNo = defaultDoNo,
                lines = listOf(
                    DeliveryLineFormItem(
                        lineId = UUID.randomUUID().toString(),
                        productId = "",
                        requestedQuantity = 1.0
                    )
                )
            )
        }
    }

    fun onDeliveryOrderNoChanged(value: String) {
        _uiState.update { it.copy(deliveryOrderNo = value) }
    }

    fun onCustomerIdChanged(value: String) {
        _uiState.update { it.copy(customerId = value) }
    }

    fun onSourceReferenceIdChanged(value: String) {
        _uiState.update { it.copy(sourceReferenceId = value) }
    }

    fun onDeliveryTypeChanged(value: DeliveryOrderType) {
        _uiState.update { it.copy(deliveryType = value) }
    }

    fun onPriorityChanged(value: DeliveryPriority) {
        _uiState.update { it.copy(priority = value) }
    }

    fun onRequestedDeliveryDateChanged(value: Long) {
        _uiState.update { it.copy(requestedDeliveryDate = value) }
    }

    fun onNotesChanged(value: String) {
        _uiState.update { it.copy(notes = value) }
    }

    fun addLine() {
        _uiState.update { current ->
            val newLines = current.lines.toMutableList().apply {
                add(
                    DeliveryLineFormItem(
                        lineId = UUID.randomUUID().toString(),
                        productId = "",
                        requestedQuantity = 1.0
                    )
                )
            }
            current.copy(lines = newLines)
        }
    }

    fun removeLine(index: Int) {
        _uiState.update { current ->
            if (current.lines.size <= 1) return@update current
            val newLines = current.lines.toMutableList().apply { removeAt(index) }
            current.copy(lines = newLines)
        }
    }

    fun updateLine(index: Int, productId: String, quantity: Double, notes: String) {
        _uiState.update { current ->
            val newLines = current.lines.toMutableList()
            if (index in newLines.indices) {
                newLines[index] = newLines[index].copy(
                    productId = productId,
                    requestedQuantity = quantity,
                    notes = notes
                )
            }
            current.copy(lines = newLines)
        }
    }

    fun saveDeliveryOrder(actorId: String, callerRole: UserRole) {
        val state = _uiState.value
        val now = System.currentTimeMillis()
        val orderId = UUID.randomUUID().toString()

        val order = DeliveryOrder(
            deliveryOrderId = orderId,
            projectId = state.projectId,
            deliveryOrderNo = state.deliveryOrderNo.trim(),
            customerId = state.customerId.trim().ifBlank { null },
            sourceReferenceId = state.sourceReferenceId.trim().ifBlank { null },
            sourceReferenceType = if (state.sourceReferenceId.isNotBlank()) "ORDER" else null,
            deliveryType = state.deliveryType,
            priority = state.priority,
            status = DeliveryOrderStatus.DRAFT,
            requestedDeliveryDate = state.requestedDeliveryDate,
            notes = state.notes.trim().ifBlank { null },
            createdBy = actorId,
            createdAt = now,
            updatedAt = now
        )

        val domainLines = state.lines.map { item ->
            DeliveryOrderLine(
                lineId = item.lineId,
                deliveryOrderId = orderId,
                projectId = state.projectId,
                productId = item.productId.trim(),
                requestedQuantity = item.requestedQuantity,
                notes = item.notes.trim().ifBlank { null }
            )
        }

        _uiState.update { it.copy(isSaving = true, errorMessage = null) }
        viewModelScope.launch {
            when (val result = repository.createDeliveryOrder(order, domainLines, callerRole)) {
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
