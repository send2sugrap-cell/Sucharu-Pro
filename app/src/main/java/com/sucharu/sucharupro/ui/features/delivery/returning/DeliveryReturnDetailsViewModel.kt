package com.sucharu.sucharupro.ui.features.delivery.returning

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.user.UserRole
import com.sucharu.sucharupro.domain.repository.DeliveryReturnRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class DeliveryReturnDetailsViewModel(
    private val repository: DeliveryReturnRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DeliveryReturnDetailsUiState())
    val uiState: StateFlow<DeliveryReturnDetailsUiState> = _uiState.asStateFlow()

    fun loadDetails(returnId: String) {
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            repository.observeReturn(returnId)
                .catch { ex -> _uiState.update { it.copy(isLoading = false, errorMessage = ex.message) } }
                .collect { ret ->
                    if (ret != null) {
                        val linesRes = repository.getReturnLines(returnId)
                        val lines = if (linesRes is DomainResult.Success) linesRes.data else emptyList()
                        val eventsRes = repository.getEvents(returnId)
                        val events = if (eventsRes is DomainResult.Success) eventsRes.data else emptyList()

                        _uiState.update { state ->
                            state.copy(
                                isLoading = false,
                                returnItem = ret,
                                lines = lines,
                                events = events
                            )
                        }
                    } else {
                        _uiState.update { it.copy(isLoading = false, errorMessage = "Delivery return not found.") }
                    }
                }
        }
    }

    fun submitReturn(returnId: String, actorId: String, callerRole: UserRole) = executeAction {
        repository.submitReturn(returnId, actorId, callerRole)
    }

    fun approveReturn(returnId: String, actorId: String, callerRole: UserRole) = executeAction {
        repository.approveReturn(returnId, actorId, callerRole)
    }

    fun rejectReturn(returnId: String, reason: String, actorId: String, callerRole: UserRole) = executeAction {
        repository.rejectReturn(returnId, reason, actorId, callerRole)
    }

    fun startReceiving(returnId: String, actorId: String, callerRole: UserRole) = executeAction {
        repository.startReceiving(returnId, actorId, callerRole)
    }

    fun receiveReturn(returnId: String, actorId: String, callerRole: UserRole) = executeAction {
        repository.receiveReturn(returnId, emptyMap(), actorId, callerRole)
    }

    fun startInspection(returnId: String, actorId: String, callerRole: UserRole) = executeAction {
        repository.startInspection(returnId, actorId, callerRole)
    }

    fun completeInspection(returnId: String, actorId: String, callerRole: UserRole) = executeAction {
        repository.completeInspection(returnId, actorId, callerRole)
    }

    fun processAllRestock(returnId: String, defaultWarehouseId: String, defaultLocationId: String, actorId: String, callerRole: UserRole) = executeAction {
        repository.processAllRestock(returnId, defaultWarehouseId, defaultLocationId, actorId, callerRole)
    }

    fun completeReturn(returnId: String, actorId: String, callerRole: UserRole) = executeAction {
        repository.completeReturn(returnId, actorId, callerRole)
    }

    fun cancelReturn(returnId: String, reason: String, actorId: String, callerRole: UserRole) = executeAction {
        repository.cancelReturn(returnId, reason, actorId, callerRole)
    }

    private fun executeAction(block: suspend () -> DomainResult<*>) {
        _uiState.update { it.copy(isActionInProgress = true, errorMessage = null, actionSuccessMessage = null) }
        viewModelScope.launch {
            when (val result = block()) {
                is DomainResult.Success -> {
                    _uiState.update { it.copy(isActionInProgress = false, actionSuccessMessage = "Action completed successfully.") }
                    val currentId = _uiState.value.returnItem?.returnId
                    if (currentId != null) loadDetails(currentId)
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
