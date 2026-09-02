package com.sucharu.sucharupro.ui.features.finance.supplierpayment

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.finance.SupplierPayment
import com.sucharu.sucharupro.domain.model.finance.SupplierPaymentActivityEvent
import com.sucharu.sucharupro.domain.model.finance.SupplierPaymentSettlement
import com.sucharu.sucharupro.domain.model.user.UserRole
import com.sucharu.sucharupro.domain.repository.SupplierPaymentRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SupplierPaymentDetailsUiState(
    val isLoading: Boolean = true,
    val payment: SupplierPayment? = null,
    val settlements: List<SupplierPaymentSettlement> = emptyList(),
    val activityEvents: List<SupplierPaymentActivityEvent> = emptyList(),
    val errorMessage: String? = null,
    val actionSuccessMessage: String? = null
)

class SupplierPaymentDetailsViewModel(
    private val repository: SupplierPaymentRepository,
    private val paymentId: String,
    private val callerRole: UserRole,
    private val authenticatedVendorId: String? = null,
    private val currentActorId: String
) : ViewModel() {

    private val _uiState = MutableStateFlow(SupplierPaymentDetailsUiState())
    val uiState: StateFlow<SupplierPaymentDetailsUiState> = _uiState.asStateFlow()

    init {
        loadDetails()
    }

    fun loadDetails() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val paymentRes = repository.getPaymentById(paymentId, callerRole, authenticatedVendorId)
            if (paymentRes is DomainResult.Success) {
                val settlementsRes = repository.getSettlementsByPayment(paymentId, callerRole)
                val settlements = if (settlementsRes is DomainResult.Success) settlementsRes.data else emptyList()

                val eventsRes = repository.getActivityEvents(paymentId, callerRole)
                val events = if (eventsRes is DomainResult.Success) eventsRes.data else emptyList()

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        payment = paymentRes.data,
                        settlements = settlements,
                        activityEvents = events
                    )
                }
            } else if (paymentRes is DomainResult.Error) {
                _uiState.update { it.copy(isLoading = false, errorMessage = paymentRes.message) }
            }
        }
    }

    fun submitPayment() {
        viewModelScope.launch {
            val res = repository.submitPayment(paymentId, currentActorId, callerRole)
            if (res is DomainResult.Success) {
                _uiState.update { it.copy(actionSuccessMessage = "Payment submitted for approval.") }
                loadDetails()
            } else if (res is DomainResult.Error) {
                _uiState.update { it.copy(errorMessage = res.message) }
            }
        }
    }

    fun approvePayment() {
        viewModelScope.launch {
            val res = repository.approvePayment(paymentId, currentActorId, callerRole)
            if (res is DomainResult.Success) {
                _uiState.update { it.copy(actionSuccessMessage = "Payment approved.") }
                loadDetails()
            } else if (res is DomainResult.Error) {
                _uiState.update { it.copy(errorMessage = res.message) }
            }
        }
    }

    fun postPayment(accountHead: String? = null) {
        viewModelScope.launch {
            val res = repository.postPayment(paymentId, accountHead, currentActorId, callerRole)
            if (res is DomainResult.Success) {
                _uiState.update { it.copy(actionSuccessMessage = "Payment posted to financial ledger.") }
                loadDetails()
            } else if (res is DomainResult.Error) {
                _uiState.update { it.copy(errorMessage = res.message) }
            }
        }
    }

    fun rejectPayment(reason: String) {
        viewModelScope.launch {
            val res = repository.rejectPayment(paymentId, reason, currentActorId, callerRole)
            if (res is DomainResult.Success) {
                _uiState.update { it.copy(actionSuccessMessage = "Payment rejected.") }
                loadDetails()
            } else if (res is DomainResult.Error) {
                _uiState.update { it.copy(errorMessage = res.message) }
            }
        }
    }

    fun cancelPayment(reason: String) {
        viewModelScope.launch {
            val res = repository.cancelPayment(paymentId, reason, currentActorId, callerRole)
            if (res is DomainResult.Success) {
                _uiState.update { it.copy(actionSuccessMessage = "Payment cancelled.") }
                loadDetails()
            } else if (res is DomainResult.Error) {
                _uiState.update { it.copy(errorMessage = res.message) }
            }
        }
    }
}
