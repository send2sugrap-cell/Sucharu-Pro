package com.sucharu.sucharupro.ui.features.finance.payment

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.finance.CustomerPaymentReceipt
import com.sucharu.sucharupro.domain.model.user.UserRole
import com.sucharu.sucharupro.domain.repository.CustomerPaymentRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CustomerReceiptDetailsUiState(
    val isLoading: Boolean = false,
    val receipt: CustomerPaymentReceipt? = null,
    val errorMessage: String? = null
)

class CustomerReceiptDetailsViewModel(
    private val repository: CustomerPaymentRepository,
    private val receiptId: String,
    private val currentUserRole: UserRole,
    private val authenticatedCustomerId: String? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(CustomerReceiptDetailsUiState(isLoading = true))
    val uiState: StateFlow<CustomerReceiptDetailsUiState> = _uiState.asStateFlow()

    init {
        loadReceipt()
    }

    fun loadReceipt() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val res = repository.getReceiptById(receiptId, currentUserRole, authenticatedCustomerId)
            when (res) {
                is DomainResult.Success -> {
                    _uiState.update { it.copy(isLoading = false, receipt = res.data, errorMessage = null) }
                }
                is DomainResult.Error -> {
                    _uiState.update { it.copy(isLoading = false, errorMessage = res.message) }
                }
                is DomainResult.Loading -> {
                    _uiState.update { it.copy(isLoading = true) }
                }
            }
        }
    }
}
