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
 * ViewModel for Recording Partial Deliveries (Module 08 Step 06).
 */
class DeliveryPartialDeliveryFormViewModel(
    private val repository: DeliveryPartialSettlementRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DeliveryPartialDeliveryFormUiState())
    val uiState: StateFlow<DeliveryPartialDeliveryFormUiState> = _uiState.asStateFlow()

    fun initialize(settlementId: String) {
        _uiState.update { it.copy(isLoading = true, settlementId = settlementId) }
        viewModelScope.launch {
            val settlementRes = repository.getSettlement(settlementId)
            val linesRes = repository.getSettlementLines(settlementId)

            if (settlementRes is DomainResult.Success && linesRes is DomainResult.Success) {
                val eligibleLines = linesRes.data.filter { it.pendingQuantity > 0 }
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        settlement = settlementRes.data,
                        lines = linesRes.data,
                        selectedDoLineId = eligibleLines.firstOrNull()?.deliveryOrderLineId ?: ""
                    )
                }
            } else {
                _uiState.update { it.copy(isLoading = false, errorMessage = "Failed to load settlement lines.") }
            }
        }
    }

    fun onLineSelected(lineId: String) = _uiState.update { it.copy(selectedDoLineId = lineId) }
    fun onQuantityChanged(value: String) = _uiState.update { it.copy(quantityToDeliverText = value) }

    fun submitPartialDelivery(actorId: String, callerRole: UserRole) {
        val state = _uiState.value
        val qty = state.quantityToDeliverText.toDoubleOrNull()
        if (qty == null || qty <= 0) {
            _uiState.update { it.copy(errorMessage = "Please enter a valid positive delivery quantity.") }
            return
        }

        _uiState.update { it.copy(isSaving = true, errorMessage = null) }
        viewModelScope.launch {
            when (val result = repository.recordPartialDelivery(state.settlementId, state.selectedDoLineId, qty, actorId, callerRole)) {
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
