package com.sucharu.sucharupro.ui.features.delivery.reconciliation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.user.UserRole
import com.sucharu.sucharupro.domain.repository.DeliveryReconciliationRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class DeliveryReconciliationFormViewModel(
    private val repository: DeliveryReconciliationRepository,
    private val currentActorId: String = "operator-1",
    private val currentRole: UserRole = UserRole.WAREHOUSE
) : ViewModel() {

    private val _uiState = MutableStateFlow(DeliveryReconciliationFormUiState())
    val uiState: StateFlow<DeliveryReconciliationFormUiState> = _uiState.asStateFlow()

    fun onDeliveryOrderIdChanged(orderId: String) {
        _uiState.update { it.copy(deliveryOrderId = orderId, errorMessage = null) }
    }

    fun onReasonChanged(reason: String) {
        _uiState.update { it.copy(reason = reason) }
    }

    fun onSubmit() {
        val orderId = _uiState.value.deliveryOrderId.trim()
        if (orderId.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Delivery Order ID is required.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true, errorMessage = null) }
            val result = repository.createReconciliation(
                deliveryOrderId = orderId,
                actorId = currentActorId,
                callerRole = currentRole
            )
            when (result) {
                is DomainResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isSubmitting = false,
                            isCreatedSuccessfully = true,
                            createdReconciliationId = result.data.reconciliationId
                        )
                    }
                }
                is DomainResult.Error -> {
                    _uiState.update { it.copy(isSubmitting = false, errorMessage = result.message) }
                }
                is DomainResult.Loading -> {}
            }
        }
    }
}
