package com.sucharu.sucharupro.ui.features.finance.payment

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.common.toMoney
import com.sucharu.sucharupro.domain.model.finance.CustomerPaymentMethod
import com.sucharu.sucharupro.domain.model.user.UserRole
import com.sucharu.sucharupro.domain.repository.CustomerPaymentRepository
import com.sucharu.sucharupro.domain.repository.CustomerReceivableRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class CustomerPaymentFormViewModel(
    private val paymentRepository: CustomerPaymentRepository,
    private val receivableRepository: CustomerReceivableRepository,
    private val projectId: String,
    private val currentUserId: String,
    private val currentUserRole: UserRole,
    initialReceivableId: String? = null,
    initialCustomerId: String? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        CustomerPaymentFormUiState(
            receivableIdInput = initialReceivableId ?: "",
            customerIdInput = initialCustomerId ?: ""
        )
    )
    val uiState: StateFlow<CustomerPaymentFormUiState> = _uiState.asStateFlow()

    init {
        if (!initialReceivableId.isNullOrBlank()) {
            loadReceivableDetails(initialReceivableId)
        }
    }

    fun onCustomerIdChanged(customerId: String) {
        _uiState.update { it.copy(customerIdInput = customerId) }
    }

    fun onReceivableIdChanged(receivableId: String) {
        _uiState.update { it.copy(receivableIdInput = receivableId) }
        loadReceivableDetails(receivableId)
    }

    fun onAmountChanged(amount: String) {
        _uiState.update { it.copy(amountInput = amount) }
    }

    fun onPaymentMethodChanged(method: CustomerPaymentMethod) {
        _uiState.update { it.copy(paymentMethod = method) }
    }

    fun onPaymentReferenceChanged(ref: String) {
        _uiState.update { it.copy(paymentReferenceInput = ref) }
    }

    fun onNotesChanged(notes: String) {
        _uiState.update { it.copy(notesInput = notes) }
    }

    private fun loadReceivableDetails(receivableId: String) {
        viewModelScope.launch {
            val recRes = receivableRepository.getReceivableById(receivableId.trim(), currentUserRole)
            if (recRes is DomainResult.Success) {
                val rec = recRes.data
                _uiState.update {
                    it.copy(
                        customerIdInput = rec.customerId,
                        outstandingAmount = rec.outstandingAmount,
                        errorMessage = null
                    )
                }
            }
        }
    }

    fun submitForm() {
        val state = _uiState.value
        val amountDouble = state.amountInput.toDoubleOrNull()
        if (amountDouble == null || amountDouble <= 0.0) {
            _uiState.update { it.copy(errorMessage = "Please enter a valid positive payment amount.") }
            return
        }
        if (state.customerIdInput.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Customer ID cannot be blank.") }
            return
        }
        if (state.receivableIdInput.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Receivable ID cannot be blank.") }
            return
        }
        if (state.paymentMethod.requiresReference && state.paymentReferenceInput.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Payment reference is required for ${state.paymentMethod.defaultLabel}.") }
            return
        }

        val moneyAmount = amountDouble.toMoney()
        if (state.outstandingAmount != null && moneyAmount > state.outstandingAmount) {
            _uiState.update {
                it.copy(errorMessage = "Payment amount (${moneyAmount.formatted()}) exceeds outstanding due (${state.outstandingAmount.formatted()}).")
            }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true, errorMessage = null) }
            val result = paymentRepository.createPayment(
                projectId = projectId,
                customerId = state.customerIdInput.trim(),
                receivableId = state.receivableIdInput.trim(),
                amount = moneyAmount,
                currency = "BDT",
                paymentMethod = state.paymentMethod,
                paymentReference = state.paymentReferenceInput.trim().ifEmpty { null },
                paymentDate = System.currentTimeMillis(),
                idempotencyKey = null,
                notes = state.notesInput.trim().ifEmpty { null },
                actorId = currentUserId,
                callerRole = currentUserRole
            )

            when (result) {
                is DomainResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isSubmitting = false,
                            successPaymentId = result.data.paymentId,
                            errorMessage = null
                        )
                    }
                }
                is DomainResult.Error -> {
                    _uiState.update { it.copy(isSubmitting = false, errorMessage = result.message) }
                }
                is DomainResult.Loading -> {
                    _uiState.update { it.copy(isSubmitting = true) }
                }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}
