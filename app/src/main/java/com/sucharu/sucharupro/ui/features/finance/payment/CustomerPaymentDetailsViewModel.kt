package com.sucharu.sucharupro.ui.features.finance.payment

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.user.UserRole
import com.sucharu.sucharupro.domain.repository.CustomerPaymentRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class CustomerPaymentDetailsViewModel(
    private val repository: CustomerPaymentRepository,
    private val paymentId: String,
    private val currentUserId: String,
    private val currentUserRole: UserRole,
    private val authenticatedCustomerId: String? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(CustomerPaymentDetailsUiState(isLoading = true))
    val uiState: StateFlow<CustomerPaymentDetailsUiState> = _uiState.asStateFlow()

    init {
        loadDetails()
    }

    fun loadDetails() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val payRes = repository.getPaymentById(paymentId, currentUserRole, authenticatedCustomerId)
            if (payRes is DomainResult.Error) {
                _uiState.update { it.copy(isLoading = false, errorMessage = payRes.message) }
                return@launch
            }

            val payment = (payRes as DomainResult.Success).data
            val rId = payment.receiptId
            val receiptRes = if (!rId.isNullOrBlank()) {
                repository.getReceiptById(rId, currentUserRole, authenticatedCustomerId)
            } else {
                null
            }
            val receipt = if (receiptRes is DomainResult.Success) receiptRes.data else null
            val eventsRes = repository.getActivityEvents(paymentId, currentUserRole)
            val events = if (eventsRes is DomainResult.Success) eventsRes.data else emptyList()

            _uiState.update {
                it.copy(
                    isLoading = false,
                    payment = payment,
                    receipt = receipt,
                    activityEvents = events,
                    errorMessage = null
                )
            }
        }
    }

    fun submitPayment() {
        viewModelScope.launch {
            _uiState.update { it.copy(isActionInProgress = true, errorMessage = null) }
            val result = repository.submitPayment(paymentId, currentUserId, currentUserRole)
            when (result) {
                is DomainResult.Success -> {
                    _uiState.update { it.copy(isActionInProgress = false, actionSuccessMessage = "Payment submitted for posting.") }
                    loadDetails()
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

    fun postPayment(accountHead: String? = null) {
        viewModelScope.launch {
            _uiState.update { it.copy(isActionInProgress = true, errorMessage = null) }
            val result = repository.postPayment(paymentId, accountHead, currentUserId, currentUserRole)
            when (result) {
                is DomainResult.Success -> {
                    _uiState.update { it.copy(isActionInProgress = false, actionSuccessMessage = "Payment posted and receipt issued.") }
                    loadDetails()
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

    fun rejectPayment(reason: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isActionInProgress = true, errorMessage = null) }
            val result = repository.rejectPayment(paymentId, reason, currentUserId, currentUserRole)
            when (result) {
                is DomainResult.Success -> {
                    _uiState.update { it.copy(isActionInProgress = false, actionSuccessMessage = "Payment rejected.") }
                    loadDetails()
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

    fun cancelPayment(reason: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isActionInProgress = true, errorMessage = null) }
            val result = repository.cancelPayment(paymentId, reason, currentUserId, currentUserRole)
            when (result) {
                is DomainResult.Success -> {
                    _uiState.update { it.copy(isActionInProgress = false, actionSuccessMessage = "Payment cancelled.") }
                    loadDetails()
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

    fun clearMessages() {
        _uiState.update { it.copy(errorMessage = null, actionSuccessMessage = null) }
    }
}
