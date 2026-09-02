package com.sucharu.sucharupro.ui.features.delivery

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.delivery.DeliveryDispatchRequest
import com.sucharu.sucharupro.domain.model.delivery.DeliveryPriority
import com.sucharu.sucharupro.domain.model.delivery.DispatchRequestStatus
import com.sucharu.sucharupro.domain.model.user.UserRole
import com.sucharu.sucharupro.domain.repository.DeliveryOrderRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * ViewModel managing the Delivery Order details screen lifecycle actions (Module 08 Step 01).
 */
class DeliveryOrderDetailsViewModel(
    private val repository: DeliveryOrderRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DeliveryOrderDetailsUiState(isLoading = true))
    val uiState: StateFlow<DeliveryOrderDetailsUiState> = _uiState.asStateFlow()

    fun loadOrderDetails(deliveryOrderId: String) {
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            repository.observeDeliveryOrder(deliveryOrderId).collect { order ->
                if (order == null) {
                    _uiState.update { it.copy(isLoading = false, errorMessage = "Delivery order not found.") }
                    return@collect
                }

                val linesResult = repository.getDeliveryOrderLines(deliveryOrderId)
                val lines = if (linesResult is DomainResult.Success) linesResult.data else emptyList()

                val dispatchResult = repository.getDispatchRequestForOrder(deliveryOrderId)
                val dispatchRequest = if (dispatchResult is DomainResult.Success) dispatchResult.data else null

                val activityResult = repository.getActivityEvents(deliveryOrderId)
                val activities = if (activityResult is DomainResult.Success) activityResult.data else emptyList()

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        order = order,
                        lines = lines,
                        dispatchRequest = dispatchRequest,
                        activityEvents = activities
                    )
                }
            }
        }
    }

    fun submitOrder(deliveryOrderId: String, actorId: String, callerRole: UserRole) {
        performAction { repository.submitDeliveryOrder(deliveryOrderId, actorId, callerRole) }
    }

    fun approveOrder(deliveryOrderId: String, actorId: String, callerRole: UserRole) {
        performAction { repository.approveDeliveryOrder(deliveryOrderId, actorId, callerRole) }
    }

    fun markReadyForDispatch(deliveryOrderId: String, actorId: String, callerRole: UserRole) {
        performAction { repository.markReadyForDispatch(deliveryOrderId, actorId, callerRole) }
    }

    fun cancelOrder(deliveryOrderId: String, actorId: String, reason: String?, callerRole: UserRole) {
        performAction { repository.cancelDeliveryOrder(deliveryOrderId, actorId, reason, callerRole) }
    }

    fun createDispatchRequest(
        deliveryOrderId: String,
        projectId: String,
        priority: DeliveryPriority,
        notes: String?,
        requestedBy: String,
        callerRole: UserRole
    ) {
        val request = DeliveryDispatchRequest(
            dispatchRequestId = UUID.randomUUID().toString(),
            projectId = projectId,
            deliveryOrderId = deliveryOrderId,
            requestedBy = requestedBy,
            requestedAt = System.currentTimeMillis(),
            priority = priority,
            status = DispatchRequestStatus.REQUESTED,
            notes = notes
        )
        performAction { repository.createDispatchRequest(request, callerRole) }
    }

    private fun performAction(action: suspend () -> DomainResult<*>) {
        _uiState.update { it.copy(isActionInProgress = true, errorMessage = null, successMessage = null) }
        viewModelScope.launch {
            when (val result = action()) {
                is DomainResult.Success -> {
                    _uiState.update {
                        it.copy(isActionInProgress = false, successMessage = "Operation successful.")
                    }
                }
                is DomainResult.Error -> {
                    _uiState.update {
                        it.copy(isActionInProgress = false, errorMessage = result.message)
                    }
                }
                is DomainResult.Loading -> {
                    _uiState.update { it.copy(isActionInProgress = true) }
                }
            }
        }
    }
}
