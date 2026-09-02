package com.sucharu.sucharupro.ui.features.delivery.dispatch

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.user.UserRole
import com.sucharu.sucharupro.domain.repository.DeliveryChallanRepository
import com.sucharu.sucharupro.domain.repository.DispatchExecutionRepository
import com.sucharu.sucharupro.domain.repository.InventoryStockOutRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel for Dispatch Execution details and operational actions (Module 08 Step 03).
 */
class DispatchExecutionDetailsViewModel(
    private val dispatchRepository: DispatchExecutionRepository,
    private val challanRepository: DeliveryChallanRepository,
    private val stockOutRepository: InventoryStockOutRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DispatchExecutionDetailsUiState(isLoading = true))
    val uiState: StateFlow<DispatchExecutionDetailsUiState> = _uiState.asStateFlow()

    fun loadDispatchDetails(dispatchExecutionId: String) {
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            dispatchRepository.observeDispatch(dispatchExecutionId).collect { dispatch ->
                if (dispatch == null) {
                    _uiState.update { it.copy(isLoading = false, errorMessage = "Dispatch execution not found.") }
                    return@collect
                }

                val linesResult = dispatchRepository.getDispatchLines(dispatchExecutionId)
                val lines = if (linesResult is DomainResult.Success) linesResult.data else emptyList()

                val challanResult = challanRepository.getChallan(dispatch.deliveryChallanId)
                val challan = if (challanResult is DomainResult.Success) challanResult.data else null

                val activitiesResult = dispatchRepository.getActivityEvents(dispatchExecutionId)
                val activities = if (activitiesResult is DomainResult.Success) activitiesResult.data else emptyList()

                // Check live stock availability for each line
                val availabilityMap = mutableMapOf<String, Int>()
                for (line in lines) {
                    val available = stockOutRepository.getAvailableQuantity(
                        projectId = dispatch.projectId,
                        warehouseId = dispatch.sourceWarehouseId,
                        locationId = line.sourceLocationId,
                        productId = line.productId
                    )
                    availabilityMap[line.dispatchExecutionLineId] = available
                }

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        dispatch = dispatch,
                        lines = lines,
                        challan = challan,
                        stockAvailabilityMap = availabilityMap,
                        activityEvents = activities
                    )
                }
            }
        }
    }

    fun submitDispatch(dispatchExecutionId: String, actorId: String, callerRole: UserRole) {
        performAction { dispatchRepository.submitDispatch(dispatchExecutionId, actorId, callerRole) }
    }

    fun approveDispatch(dispatchExecutionId: String, actorId: String, callerRole: UserRole) {
        performAction { dispatchRepository.approveDispatch(dispatchExecutionId, actorId, callerRole) }
    }

    fun markReadyForExecution(dispatchExecutionId: String, actorId: String, callerRole: UserRole) {
        performAction { dispatchRepository.markReadyForExecution(dispatchExecutionId, actorId, callerRole) }
    }

    fun executeDispatch(dispatchExecutionId: String, actorId: String, callerRole: UserRole) {
        performAction { dispatchRepository.executeDispatch(dispatchExecutionId, actorId, callerRole) }
    }

    fun cancelDispatch(dispatchExecutionId: String, actorId: String, reason: String?, callerRole: UserRole) {
        performAction { dispatchRepository.cancelDispatch(dispatchExecutionId, actorId, reason, callerRole) }
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
