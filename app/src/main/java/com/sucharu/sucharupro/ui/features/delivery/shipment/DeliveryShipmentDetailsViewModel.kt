package com.sucharu.sucharupro.ui.features.delivery.shipment

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.delivery.shipment.DeliveryShipmentAttemptStatus
import com.sucharu.sucharupro.domain.model.delivery.shipment.DeliveryShipmentEventType
import com.sucharu.sucharupro.domain.model.user.UserRole
import com.sucharu.sucharupro.domain.repository.DeliveryShipmentRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel for Delivery Shipment Details & Operational Transitions (Module 08 Step 05).
 */
class DeliveryShipmentDetailsViewModel(
    private val repository: DeliveryShipmentRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DeliveryShipmentDetailsUiState(isLoading = true))
    val uiState: StateFlow<DeliveryShipmentDetailsUiState> = _uiState.asStateFlow()

    fun loadShipmentDetails(shipmentId: String) {
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            repository.observeShipment(shipmentId).collect { shipment ->
                if (shipment == null) {
                    _uiState.update { it.copy(isLoading = false, errorMessage = "Shipment not found.") }
                    return@collect
                }

                val eventsResult = repository.getTrackingEvents(shipmentId)
                val events = if (eventsResult is DomainResult.Success) eventsResult.data else emptyList()

                val attemptsResult = repository.getDeliveryAttempts(shipmentId)
                val attempts = if (attemptsResult is DomainResult.Success) attemptsResult.data else emptyList()

                val activitiesResult = repository.getActivityEvents(shipmentId)
                val activities = if (activitiesResult is DomainResult.Success) activitiesResult.data else emptyList()

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        shipment = shipment,
                        trackingEvents = events,
                        deliveryAttempts = attempts,
                        activityEvents = activities
                    )
                }
            }
        }
    }

    fun markReady(shipmentId: String, actorId: String, callerRole: UserRole) {
        performAction { repository.markReady(shipmentId, actorId, callerRole) }
    }

    fun markDispatched(shipmentId: String, actorId: String, callerRole: UserRole) {
        performAction { repository.markDispatched(shipmentId, System.currentTimeMillis(), actorId, callerRole) }
    }

    fun markInTransit(shipmentId: String, locationText: String?, note: String?, actorId: String, callerRole: UserRole) {
        performAction { repository.markInTransit(shipmentId, locationText, note, actorId, callerRole) }
    }

    fun markOutForDelivery(shipmentId: String, locationText: String?, note: String?, actorId: String, callerRole: UserRole) {
        performAction { repository.markOutForDelivery(shipmentId, locationText, note, actorId, callerRole) }
    }

    fun markDelayed(shipmentId: String, reason: String, locationText: String?, actorId: String, callerRole: UserRole) {
        performAction { repository.markDelayed(shipmentId, reason, locationText, actorId, callerRole) }
    }

    fun putOnHold(shipmentId: String, reason: String, locationText: String?, actorId: String, callerRole: UserRole) {
        performAction { repository.putOnHold(shipmentId, reason, locationText, actorId, callerRole) }
    }

    fun recordDeliveryAttempt(
        shipmentId: String,
        status: DeliveryShipmentAttemptStatus,
        reason: String?,
        notes: String?,
        actorId: String,
        callerRole: UserRole
    ) {
        performAction {
            repository.recordDeliveryAttempt(
                shipmentId = shipmentId,
                status = status,
                reason = reason,
                notes = notes,
                actorId = actorId,
                callerRole = callerRole
            )
        }
    }

    fun markDelivered(shipmentId: String, notes: String?, actorId: String, callerRole: UserRole) {
        performAction { repository.markDelivered(shipmentId, System.currentTimeMillis(), notes, actorId, callerRole) }
    }

    fun cancelShipment(shipmentId: String, reason: String?, actorId: String, callerRole: UserRole) {
        performAction { repository.cancelShipment(shipmentId, reason, actorId, callerRole) }
    }

    fun addTrackingEvent(
        shipmentId: String,
        eventType: DeliveryShipmentEventType,
        locationText: String?,
        description: String?,
        actorId: String,
        callerRole: UserRole
    ) {
        performAction {
            repository.addTrackingEvent(
                shipmentId = shipmentId,
                eventType = eventType,
                locationText = locationText,
                description = description,
                actorId = actorId,
                callerRole = callerRole
            )
        }
    }

    private fun performAction(action: suspend () -> DomainResult<*>) {
        _uiState.update { it.copy(isActionInProgress = true, errorMessage = null, successMessage = null) }
        viewModelScope.launch {
            when (val result = action()) {
                is DomainResult.Success -> {
                    _uiState.update { it.copy(isActionInProgress = false, successMessage = "Action completed successfully.") }
                    _uiState.value.shipment?.shipmentId?.let { loadShipmentDetails(it) }
                }
                is DomainResult.Error -> {
                    _uiState.update { it.copy(isActionInProgress = false, errorMessage = result.message) }
                }
                is DomainResult.Loading -> {
                    _uiState.update { it.copy(isActionInProgress = true) }
                }
            }
        }
    }
}
