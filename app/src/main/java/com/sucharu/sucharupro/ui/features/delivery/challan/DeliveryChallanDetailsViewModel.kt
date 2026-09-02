package com.sucharu.sucharupro.ui.features.delivery.challan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.user.UserRole
import com.sucharu.sucharupro.domain.repository.DeliveryChallanRepository
import com.sucharu.sucharupro.domain.repository.DeliveryOrderRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel for Delivery Challan details screen and lifecycle actions (Module 08 Step 02).
 */
class DeliveryChallanDetailsViewModel(
    private val challanRepository: DeliveryChallanRepository,
    private val deliveryOrderRepository: DeliveryOrderRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DeliveryChallanDetailsUiState(isLoading = true))
    val uiState: StateFlow<DeliveryChallanDetailsUiState> = _uiState.asStateFlow()

    fun loadChallanDetails(challanId: String) {
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            challanRepository.observeChallan(challanId).collect { challan ->
                if (challan == null) {
                    _uiState.update { it.copy(isLoading = false, errorMessage = "Challan not found.") }
                    return@collect
                }

                val linesResult = challanRepository.getChallanLines(challanId)
                val lines = if (linesResult is DomainResult.Success) linesResult.data else emptyList()

                val orderResult = deliveryOrderRepository.getDeliveryOrder(challan.deliveryOrderId)
                val deliveryOrder = if (orderResult is DomainResult.Success) orderResult.data else null

                val doLinesResult = deliveryOrderRepository.getDeliveryOrderLines(challan.deliveryOrderId)
                val doLines = if (doLinesResult is DomainResult.Success) doLinesResult.data else emptyList()

                val activitiesResult = challanRepository.getActivityEvents(challanId)
                val activities = if (activitiesResult is DomainResult.Success) activitiesResult.data else emptyList()

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        challan = challan,
                        lines = lines,
                        deliveryOrder = deliveryOrder,
                        deliveryOrderLines = doLines,
                        activityEvents = activities
                    )
                }
            }
        }
    }

    fun submitChallan(challanId: String, actorId: String, callerRole: UserRole) {
        performAction { challanRepository.submitChallan(challanId, actorId, callerRole) }
    }

    fun approveChallan(challanId: String, actorId: String, callerRole: UserRole) {
        performAction { challanRepository.approveChallan(challanId, actorId, callerRole) }
    }

    fun markReadyForDispatch(challanId: String, actorId: String, callerRole: UserRole) {
        performAction { challanRepository.markReadyForDispatch(challanId, actorId, callerRole) }
    }

    fun cancelChallan(challanId: String, actorId: String, reason: String?, callerRole: UserRole) {
        performAction { challanRepository.cancelChallan(challanId, actorId, reason, callerRole) }
    }

    private fun performAction(action: suspend () -> DomainResult<*>) {
        _uiState.update { it.copy(isActionInProgress = true, errorMessage = null, successMessage = null) }
        viewModelScope.launch {
            when (val result = action()) {
                is DomainResult.Success -> {
                    _uiState.update { it.copy(isActionInProgress = false, successMessage = "Action completed successfully.") }
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
