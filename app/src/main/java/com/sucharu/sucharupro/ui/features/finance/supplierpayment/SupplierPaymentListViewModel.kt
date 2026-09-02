package com.sucharu.sucharupro.ui.features.finance.supplierpayment

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sucharu.sucharupro.domain.model.finance.SupplierPayment
import com.sucharu.sucharupro.domain.model.finance.SupplierPaymentMethod
import com.sucharu.sucharupro.domain.model.finance.SupplierPaymentStatus
import com.sucharu.sucharupro.domain.model.user.UserRole
import com.sucharu.sucharupro.domain.repository.SupplierPaymentRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SupplierPaymentListUiState(
    val isLoading: Boolean = true,
    val payments: List<SupplierPayment> = emptyList(),
    val filteredPayments: List<SupplierPayment> = emptyList(),
    val searchQuery: String = "",
    val selectedStatus: SupplierPaymentStatus? = null,
    val selectedMethod: SupplierPaymentMethod? = null,
    val errorMessage: String? = null
)

class SupplierPaymentListViewModel(
    private val repository: SupplierPaymentRepository,
    private val projectId: String,
    private val callerRole: UserRole,
    private val authenticatedVendorId: String? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(SupplierPaymentListUiState())
    val uiState: StateFlow<SupplierPaymentListUiState> = _uiState.asStateFlow()

    init {
        loadPayments()
    }

    fun loadPayments() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val flow = if (callerRole == UserRole.VENDOR && authenticatedVendorId != null) {
                repository.observeVendorPayments(projectId, authenticatedVendorId, callerRole, authenticatedVendorId)
            } else {
                repository.observePayments(projectId, callerRole)
            }

            flow.catch { e ->
                _uiState.update { it.copy(isLoading = false, errorMessage = e.message ?: "Failed to load supplier payments") }
            }.collect { list ->
                _uiState.update { state ->
                    state.copy(
                        isLoading = false,
                        payments = list,
                        filteredPayments = applyFilters(list, state.searchQuery, state.selectedStatus, state.selectedMethod)
                    )
                }
            }
        }
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.update { state ->
            state.copy(
                searchQuery = query,
                filteredPayments = applyFilters(state.payments, query, state.selectedStatus, state.selectedMethod)
            )
        }
    }

    fun onStatusSelected(status: SupplierPaymentStatus?) {
        _uiState.update { state ->
            val newStatus = if (state.selectedStatus == status) null else status
            state.copy(
                selectedStatus = newStatus,
                filteredPayments = applyFilters(state.payments, state.searchQuery, newStatus, state.selectedMethod)
            )
        }
    }

    fun onMethodSelected(method: SupplierPaymentMethod?) {
        _uiState.update { state ->
            val newMethod = if (state.selectedMethod == method) null else method
            state.copy(
                selectedMethod = newMethod,
                filteredPayments = applyFilters(state.payments, state.searchQuery, state.selectedStatus, newMethod)
            )
        }
    }

    private fun applyFilters(
        list: List<SupplierPayment>,
        query: String,
        status: SupplierPaymentStatus?,
        method: SupplierPaymentMethod?
    ): List<SupplierPayment> {
        return list.filter { payment ->
            val matchesQuery = query.isBlank() ||
                    payment.paymentNo.contains(query, ignoreCase = true) ||
                    payment.vendorId.contains(query, ignoreCase = true) ||
                    (payment.paymentReference?.contains(query, ignoreCase = true) == true) ||
                    (payment.notes?.contains(query, ignoreCase = true) == true)

            val matchesStatus = status == null || payment.status == status
            val matchesMethod = method == null || payment.paymentMethod == method

            matchesQuery && matchesStatus && matchesMethod
        }
    }
}
