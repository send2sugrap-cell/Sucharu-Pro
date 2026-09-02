package com.sucharu.sucharupro.ui.features.customer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sucharu.sucharupro.data.repository.FakeCustomerRepository
import com.sucharu.sucharupro.domain.model.customer.Customer
import com.sucharu.sucharupro.domain.model.customer.CustomerStatusType
import com.sucharu.sucharupro.domain.model.customer.CustomerType
import com.sucharu.sucharupro.domain.repository.CustomerRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch

/**
 * ViewModel managing presentation state, search, and filtering for the Customer List screen.
 */
class CustomerListViewModel(
    private val repository: CustomerRepository = FakeCustomerRepository(),
    private val externalScope: kotlinx.coroutines.CoroutineScope? = null
) : ViewModel() {

    private val scope: kotlinx.coroutines.CoroutineScope
        get() = externalScope ?: viewModelScope

    private val _uiState = MutableStateFlow<CustomerListUiState>(CustomerListUiState.Loading)
    val uiState: StateFlow<CustomerListUiState> = _uiState.asStateFlow()

    private var rawCustomers: List<Customer> = emptyList()
    private var currentSearchQuery: String = ""
    private var currentTypeFilter: CustomerType? = null
    private var currentStatusFilter: CustomerStatusType? = null

    init {
        loadCustomers()
    }

    /**
     * Observes the reactive customer stream from the repository.
     */
    fun loadCustomers() {
        scope.launch {
            repository.getCustomers()
                .onStart {
                    _uiState.value = CustomerListUiState.Loading
                }
                .catch { exception ->
                    _uiState.value = CustomerListUiState.Error(
                        errorMessage = exception.localizedMessage ?: "Failed to load customer records."
                    )
                }
                .collect { customers ->
                    rawCustomers = customers
                    if (customers.isEmpty()) {
                        _uiState.value = CustomerListUiState.Empty()
                    } else {
                        updateFilteredState()
                    }
                }
        }
    }

    /**
     * Handles search text changes.
     */
    fun onSearchQueryChange(query: String) {
        currentSearchQuery = query
        updateFilteredState()
    }

    /**
     * Handles customer type filter changes.
     */
    fun onTypeFilterChange(type: CustomerType?) {
        currentTypeFilter = type
        updateFilteredState()
    }

    /**
     * Handles customer status filter changes.
     */
    fun onStatusFilterChange(status: CustomerStatusType?) {
        currentStatusFilter = status
        updateFilteredState()
    }

    /**
     * Clears all active filters and search query.
     */
    fun clearFilters() {
        currentSearchQuery = ""
        currentTypeFilter = null
        currentStatusFilter = null
        updateFilteredState()
    }

    /**
     * Triggers a manual refresh.
     */
    fun refresh() {
        scope.launch {
            val currentState = _uiState.value
            if (currentState is CustomerListUiState.Success) {
                _uiState.value = currentState.copy(isRefreshing = true)
            }

            val result = repository.refreshCustomers()
            if (result.isFailure) {
                _uiState.value = CustomerListUiState.Error(
                    errorMessage = result.exceptionOrNull()?.localizedMessage
                        ?: "Failed to refresh customer records."
                )
            }
        }
    }

    /**
     * Retries loading data.
     */
    fun retry() {
        loadCustomers()
    }

    private fun updateFilteredState() {
        if (rawCustomers.isEmpty()) {
            _uiState.value = CustomerListUiState.Empty()
            return
        }

        val trimmedQuery = currentSearchQuery.trim()

        val filtered = rawCustomers.filter { customer ->
            val matchesSearch = trimmedQuery.isBlank() ||
                customer.displayName.contains(trimmedQuery, ignoreCase = true) ||
                customer.customerCode.contains(trimmedQuery, ignoreCase = true) ||
                customer.customerId.contains(trimmedQuery, ignoreCase = true) ||
                customer.primaryPhone.contains(trimmedQuery, ignoreCase = true) ||
                (customer.alternatePhone?.contains(trimmedQuery, ignoreCase = true) == true) ||
                (customer.email?.contains(trimmedQuery, ignoreCase = true) == true) ||
                (customer.contactPersonName?.contains(trimmedQuery, ignoreCase = true) == true)

            val matchesType = currentTypeFilter == null || customer.customerType == currentTypeFilter
            val matchesStatus = currentStatusFilter == null || customer.status == currentStatusFilter

            matchesSearch && matchesType && matchesStatus
        }

        _uiState.value = CustomerListUiState.Success(
            allCustomers = rawCustomers,
            visibleCustomers = filtered,
            searchQuery = currentSearchQuery,
            selectedType = currentTypeFilter,
            selectedStatus = currentStatusFilter
        )
    }
}
