package com.sucharu.sucharupro.ui.features.finance.payable

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.finance.VendorPayable
import com.sucharu.sucharupro.domain.model.finance.VendorPayableActivityEvent
import com.sucharu.sucharupro.domain.model.user.UserRole
import com.sucharu.sucharupro.domain.repository.VendorPayableRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class VendorPayableDetailsUiState(
    val isLoading: Boolean = true,
    val payable: VendorPayable? = null,
    val activityEvents: List<VendorPayableActivityEvent> = emptyList(),
    val errorMessage: String? = null,
    val actionSuccessMessage: String? = null
)

class VendorPayableDetailsViewModel(
    private val repository: VendorPayableRepository,
    private val payableId: String,
    private val callerRole: UserRole,
    private val authenticatedVendorId: String? = null,
    private val currentActorId: String
) : ViewModel() {

    private val _uiState = MutableStateFlow(VendorPayableDetailsUiState())
    val uiState: StateFlow<VendorPayableDetailsUiState> = _uiState.asStateFlow()

    init {
        loadDetails()
    }

    fun loadDetails() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val payableRes = repository.getPayableById(payableId, callerRole, authenticatedVendorId)
            if (payableRes is DomainResult.Success) {
                val eventsRes = repository.getActivityEvents(payableId, callerRole)
                val events = if (eventsRes is DomainResult.Success) eventsRes.data else emptyList()
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        payable = payableRes.data,
                        activityEvents = events
                    )
                }
            } else if (payableRes is DomainResult.Error) {
                _uiState.update { it.copy(isLoading = false, errorMessage = payableRes.message) }
            }
        }
    }

    fun submitPayable() {
        viewModelScope.launch {
            val res = repository.submitPayable(payableId, currentActorId, callerRole)
            if (res is DomainResult.Success) {
                _uiState.update { it.copy(actionSuccessMessage = "Payable submitted for approval.") }
                loadDetails()
            } else if (res is DomainResult.Error) {
                _uiState.update { it.copy(errorMessage = res.message) }
            }
        }
    }

    fun approvePayable() {
        viewModelScope.launch {
            val res = repository.approvePayable(payableId, currentActorId, callerRole)
            if (res is DomainResult.Success) {
                _uiState.update { it.copy(actionSuccessMessage = "Payable approved successfully.") }
                loadDetails()
            } else if (res is DomainResult.Error) {
                _uiState.update { it.copy(errorMessage = res.message) }
            }
        }
    }

    fun cancelPayable(reason: String) {
        viewModelScope.launch {
            val res = repository.cancelPayable(payableId, reason, currentActorId, callerRole)
            if (res is DomainResult.Success) {
                _uiState.update { it.copy(actionSuccessMessage = "Payable cancelled.") }
                loadDetails()
            } else if (res is DomainResult.Error) {
                _uiState.update { it.copy(errorMessage = res.message) }
            }
        }
    }
}
