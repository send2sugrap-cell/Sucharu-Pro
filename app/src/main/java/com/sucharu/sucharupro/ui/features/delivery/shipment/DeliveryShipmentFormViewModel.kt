package com.sucharu.sucharupro.ui.features.delivery.shipment

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.delivery.dispatch.DispatchExecutionStatus
import com.sucharu.sucharupro.domain.model.delivery.shipment.DeliveryShipment
import com.sucharu.sucharupro.domain.model.delivery.shipment.DeliveryShipmentPriority
import com.sucharu.sucharupro.domain.model.delivery.shipment.DeliveryShipmentStatus
import com.sucharu.sucharupro.domain.model.delivery.shipment.DeliveryShipmentType
import com.sucharu.sucharupro.domain.model.user.UserRole
import com.sucharu.sucharupro.domain.repository.DeliveryShipmentRepository
import com.sucharu.sucharupro.domain.repository.DispatchExecutionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * ViewModel for Creating Delivery Shipments (Module 08 Step 05).
 */
class DeliveryShipmentFormViewModel(
    private val shipmentRepository: DeliveryShipmentRepository,
    private val dispatchRepository: DispatchExecutionRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DeliveryShipmentFormUiState())
    val uiState: StateFlow<DeliveryShipmentFormUiState> = _uiState.asStateFlow()

    fun initialize(projectId: String, preselectedDispatchId: String? = null) {
        val defaultShipmentNo = "SHP-${System.currentTimeMillis() % 100000}"
        _uiState.update { it.copy(isLoading = true, projectId = projectId, shipmentNo = defaultShipmentNo) }

        viewModelScope.launch {
            val allDispatches = dispatchRepository.observeDispatches(projectId).first()
            val eligibleDispatches = allDispatches.filter { it.status == DispatchExecutionStatus.DISPATCHED }

            _uiState.update {
                it.copy(
                    isLoading = false,
                    availableDispatches = eligibleDispatches,
                    selectedDispatchId = preselectedDispatchId ?: eligibleDispatches.firstOrNull()?.dispatchExecutionId ?: ""
                )
            }
        }
    }

    fun onShipmentNoChanged(value: String) = _uiState.update { it.copy(shipmentNo = value) }
    fun onDispatchSelected(dispatchId: String) = _uiState.update { it.copy(selectedDispatchId = dispatchId) }
    fun onTypeChanged(type: DeliveryShipmentType) = _uiState.update { it.copy(shipmentType = type) }
    fun onPriorityChanged(priority: DeliveryShipmentPriority) = _uiState.update { it.copy(priority = priority) }
    fun onCarrierNameChanged(value: String) = _uiState.update { it.copy(carrierName = value) }
    fun onCarrierReferenceChanged(value: String) = _uiState.update { it.copy(carrierReference = value) }
    fun onTrackingNumberChanged(value: String) = _uiState.update { it.copy(trackingNumber = value) }
    fun onDestinationAddressChanged(value: String) = _uiState.update { it.copy(destinationAddress = value) }
    fun onDestinationContactNameChanged(value: String) = _uiState.update { it.copy(destinationContactName = value) }
    fun onDestinationContactPhoneChanged(value: String) = _uiState.update { it.copy(destinationContactPhone = value) }
    fun onDestinationNotesChanged(value: String) = _uiState.update { it.copy(destinationNotes = value) }
    fun onNotesChanged(value: String) = _uiState.update { it.copy(notes = value) }

    fun saveShipment(actorId: String, callerRole: UserRole) {
        val state = _uiState.value
        val now = System.currentTimeMillis()
        val shipmentId = UUID.randomUUID().toString()

        val selectedDispatch = state.availableDispatches.find { it.dispatchExecutionId == state.selectedDispatchId }

        val shipment = DeliveryShipment(
            shipmentId = shipmentId,
            projectId = state.projectId,
            shipmentNo = state.shipmentNo.trim(),
            deliveryOrderId = selectedDispatch?.deliveryOrderId ?: "",
            deliveryChallanId = selectedDispatch?.deliveryChallanId ?: "",
            dispatchExecutionId = state.selectedDispatchId,
            customerId = selectedDispatch?.customerId,
            shipmentType = state.shipmentType,
            priority = state.priority,
            carrierName = state.carrierName.trim().ifBlank { null },
            carrierReference = state.carrierReference.trim().ifBlank { null },
            trackingNumber = state.trackingNumber.trim().ifBlank { null },
            destinationAddress = state.destinationAddress.trim().ifBlank { null },
            destinationContactName = state.destinationContactName.trim().ifBlank { null },
            destinationContactPhone = state.destinationContactPhone.trim().ifBlank { null },
            destinationNotes = state.destinationNotes.trim().ifBlank { null },
            currentStatus = DeliveryShipmentStatus.DRAFT,
            notes = state.notes.trim().ifBlank { null },
            createdBy = actorId,
            createdAt = now,
            updatedAt = now
        )

        _uiState.update { it.copy(isSaving = true, errorMessage = null) }
        viewModelScope.launch {
            when (val result = shipmentRepository.createShipment(shipment, callerRole)) {
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
