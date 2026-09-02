package com.sucharu.sucharupro.ui.features.delivery.partial

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.user.UserRole
import com.sucharu.sucharupro.domain.repository.DeliveryPartialSettlementRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel for Delivery Settlement Details & Reconciliation Actions (Module 08 Step 06).
 */
class DeliverySettlementDetailsViewModel(
    private val repository: DeliveryPartialSettlementRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DeliverySettlementDetailsUiState(isLoading = true))
    val uiState: StateFlow<DeliverySettlementDetailsUiState> = _uiState.asStateFlow()

    fun loadSettlementDetails(settlementId: String) {
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            repository.observeSettlement(settlementId).collect { settlement ->
                if (settlement == null) {
                    _uiState.update { it.copy(isLoading = false, errorMessage = "Settlement not found.") }
                    return@collect
                }

                val linesResult = repository.getSettlementLines(settlementId)
                val lines = if (linesResult is DomainResult.Success) linesResult.data else emptyList()

                val splitsResult = repository.getSplitDispatches(settlement.deliveryOrderId)
                val splits = if (splitsResult is DomainResult.Success) splitsResult.data else emptyList()

                val eventsResult = repository.getEvents(settlementId)
                val events = if (eventsResult is DomainResult.Success) eventsResult.data else emptyList()

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        settlement = settlement,
                        lines = lines,
                        splitDispatches = splits,
                        events = events
                    )
                }
            }
        }
    }

    fun recalculateSettlement(settlementId: String, actorId: String, callerRole: UserRole) {
        performAction { repository.recalculateSettlement(settlementId, actorId, callerRole) }
    }

    fun finalizeSettlement(settlementId: String, notes: String?, actorId: String, callerRole: UserRole) {
        performAction { repository.finalizeSettlement(settlementId, notes, actorId, callerRole) }
    }

    fun disputeSettlement(settlementId: String, reason: String, actorId: String, callerRole: UserRole) {
        performAction { repository.disputeSettlement(settlementId, reason, actorId, callerRole) }
    }

    fun cancelSettlement(settlementId: String, reason: String?, actorId: String, callerRole: UserRole) {
        performAction { repository.cancelSettlement(settlementId, reason, actorId, callerRole) }
    }

    private fun performAction(action: suspend () -> DomainResult<*>) {
        _uiState.update { it.copy(isActionInProgress = true, errorMessage = null, successMessage = null) }
        viewModelScope.launch {
            when (val result = action()) {
                is DomainResult.Success -> {
                    _uiState.update { it.copy(isActionInProgress = false, successMessage = "Action completed successfully.") }
                    _uiState.value.settlement?.settlementId?.let { loadSettlementDetails(it) }
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
