package com.sucharu.sucharupro.ui.features.finance.payment

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sucharu.sucharupro.domain.model.finance.CustomerPaymentMethod
import com.sucharu.sucharupro.domain.model.finance.CustomerPaymentStatus
import com.sucharu.sucharupro.domain.model.user.UserRole
import com.sucharu.sucharupro.domain.repository.CustomerPaymentRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update

class CustomerPaymentListViewModel(
    private val repository: CustomerPaymentRepository,
    private val projectId: String,
    private val currentUserRole: UserRole,
    private val authenticatedCustomerId: String? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(CustomerPaymentListUiState(isLoading = true))
    val uiState: StateFlow<CustomerPaymentListUiState> = _uiState.asStateFlow()

    init {
        loadPayments()
    }

    fun loadPayments() {
        val flow = if (authenticatedCustomerId != null) {
            repository.observeCustomerPayments(projectId, authenticatedCustomerId, currentUserRole, authenticatedCustomerId)
        } else {
            repository.observePayments(projectId, currentUserRole)
        }

        flow.onEach { list ->
            _uiState.update { it.copy(isLoading = false, payments = list, errorMessage = null) }
        }.catch { error ->
            _uiState.update { it.copy(isLoading = false, errorMessage = error.message ?: "Failed to load payments.") }
        }.launchIn(viewModelScope)
    }

    fun onStatusFilterSelected(status: CustomerPaymentStatus?) {
        _uiState.update { it.copy(selectedStatusFilter = status) }
    }

    fun onMethodFilterSelected(method: CustomerPaymentMethod?) {
        _uiState.update { it.copy(selectedMethodFilter = method) }
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}
