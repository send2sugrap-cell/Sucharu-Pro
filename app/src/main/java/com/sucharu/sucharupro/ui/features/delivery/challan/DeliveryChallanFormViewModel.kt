package com.sucharu.sucharupro.ui.features.delivery.challan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.delivery.DeliveryOrderStatus
import com.sucharu.sucharupro.domain.model.delivery.challan.DeliveryChallan
import com.sucharu.sucharupro.domain.model.delivery.challan.DeliveryChallanLine
import com.sucharu.sucharupro.domain.model.delivery.challan.DeliveryChallanStatus
import com.sucharu.sucharupro.domain.model.delivery.challan.DeliveryChallanType
import com.sucharu.sucharupro.domain.model.user.UserRole
import com.sucharu.sucharupro.domain.repository.DeliveryChallanRepository
import com.sucharu.sucharupro.domain.repository.DeliveryOrderRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * ViewModel for creating Delivery Challans (Module 08 Step 02).
 */
class DeliveryChallanFormViewModel(
    private val challanRepository: DeliveryChallanRepository,
    private val deliveryOrderRepository: DeliveryOrderRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DeliveryChallanFormUiState())
    val uiState: StateFlow<DeliveryChallanFormUiState> = _uiState.asStateFlow()

    fun initialize(projectId: String, preselectedOrderId: String? = null) {
        val defaultChallanNo = "CHAL-${System.currentTimeMillis() % 100000}"
        _uiState.update { it.copy(isLoading = true, projectId = projectId, challanNo = defaultChallanNo) }

        viewModelScope.launch {
            val allOrders = deliveryOrderRepository.observeDeliveryOrders(projectId).first()
            val eligibleOrders = allOrders.filter {
                it.status == DeliveryOrderStatus.APPROVED || it.status == DeliveryOrderStatus.READY_FOR_DISPATCH
            }

            _uiState.update { it.copy(isLoading = false, availableOrders = eligibleOrders) }

            val targetOrderId = preselectedOrderId ?: eligibleOrders.firstOrNull()?.deliveryOrderId
            if (targetOrderId != null) {
                selectDeliveryOrder(targetOrderId)
            }
        }
    }

    fun selectDeliveryOrder(deliveryOrderId: String) {
        viewModelScope.launch {
            val linesResult = deliveryOrderRepository.getDeliveryOrderLines(deliveryOrderId)
            val doLines = if (linesResult is DomainResult.Success) linesResult.data else emptyList()

            // Calculate allocations for each line
            val allocationMap = mutableMapOf<String, Double>()
            val lineFormItems = mutableListOf<DeliveryChallanLineFormItem>()

            for (line in doLines) {
                val allocated = challanRepository.getAllocatedQuantityForDeliveryOrderLine(line.lineId)
                allocationMap[line.lineId] = allocated
                val remaining = (line.requestedQuantity - allocated).coerceAtLeast(0.0)

                if (remaining > 0) {
                    lineFormItems.add(
                        DeliveryChallanLineFormItem(
                            lineId = UUID.randomUUID().toString(),
                            deliveryOrderLineId = line.lineId,
                            productId = line.productId,
                            requestedQuantity = line.requestedQuantity,
                            alreadyAllocatedQuantity = allocated,
                            remainingQuantity = remaining,
                            quantity = remaining
                        )
                    )
                }
            }

            _uiState.update {
                it.copy(
                    selectedDeliveryOrderId = deliveryOrderId,
                    selectedOrderLines = doLines,
                    allocatedQuantitiesMap = allocationMap,
                    lines = lineFormItems
                )
            }
        }
    }

    fun onChallanNoChanged(value: String) {
        _uiState.update { it.copy(challanNo = value) }
    }

    fun onChallanTypeChanged(type: DeliveryChallanType) {
        _uiState.update { it.copy(challanType = type) }
    }

    fun onNotesChanged(notes: String) {
        _uiState.update { it.copy(notes = notes) }
    }

    fun updateLineQuantity(index: Int, quantity: Double) {
        _uiState.update { current ->
            val updated = current.lines.toMutableList()
            if (index in updated.indices) {
                updated[index] = updated[index].copy(quantity = quantity)
            }
            current.copy(lines = updated)
        }
    }

    fun updateLineNotes(index: Int, notes: String) {
        _uiState.update { current ->
            val updated = current.lines.toMutableList()
            if (index in updated.indices) {
                updated[index] = updated[index].copy(notes = notes)
            }
            current.copy(lines = updated)
        }
    }

    fun removeLine(index: Int) {
        _uiState.update { current ->
            if (current.lines.size <= 1) return@update current
            val updated = current.lines.toMutableList().apply { removeAt(index) }
            current.copy(lines = updated)
        }
    }

    fun saveChallan(actorId: String, callerRole: UserRole) {
        val state = _uiState.value
        val now = System.currentTimeMillis()
        val challanId = UUID.randomUUID().toString()

        val selectedOrder = state.availableOrders.find { it.deliveryOrderId == state.selectedDeliveryOrderId }

        val challan = DeliveryChallan(
            challanId = challanId,
            projectId = state.projectId,
            challanNo = state.challanNo.trim(),
            deliveryOrderId = state.selectedDeliveryOrderId,
            customerId = selectedOrder?.customerId,
            sourceReferenceId = selectedOrder?.sourceReferenceId,
            sourceReferenceType = selectedOrder?.sourceReferenceType,
            challanType = state.challanType,
            status = DeliveryChallanStatus.DRAFT,
            issueDate = state.issueDate,
            notes = state.notes.trim().ifBlank { null },
            createdBy = actorId,
            createdAt = now,
            updatedAt = now
        )

        val domainLines = state.lines.map { item ->
            DeliveryChallanLine(
                lineId = item.lineId,
                challanId = challanId,
                projectId = state.projectId,
                deliveryOrderLineId = item.deliveryOrderLineId,
                productId = item.productId,
                quantity = item.quantity,
                notes = item.notes.trim().ifBlank { null }
            )
        }

        _uiState.update { it.copy(isSaving = true, errorMessage = null) }
        viewModelScope.launch {
            when (val result = challanRepository.createChallan(challan, domainLines, callerRole)) {
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
