package com.sucharu.sucharupro.ui.features.customer

import com.sucharu.sucharupro.domain.model.customer.Customer
import com.sucharu.sucharupro.domain.model.customer.CustomerStatusType
import com.sucharu.sucharupro.domain.model.customer.CustomerType

/**
 * UI State definition for the Customer Management list and search screen.
 */
sealed interface CustomerListUiState {

    /**
     * Initial loading state while fetching customer records.
     */
    data object Loading : CustomerListUiState

    /**
     * Successfully loaded customer records with current search query and filters applied.
     */
    data class Success(
        val allCustomers: List<Customer>,
        val visibleCustomers: List<Customer>,
        val searchQuery: String = "",
        val selectedType: CustomerType? = null,
        val selectedStatus: CustomerStatusType? = null,
        val isRefreshing: Boolean = false
    ) : CustomerListUiState {
        val totalCount: Int get() = allCustomers.size
        val visibleCount: Int get() = visibleCustomers.size
        val isFiltered: Boolean get() = searchQuery.isNotBlank() || selectedType != null || selectedStatus != null
    }

    /**
     * Empty state when no customer records exist at all.
     */
    data class Empty(
        val message: String = "No customer records recorded yet. Customer profiles will appear here."
    ) : CustomerListUiState

    /**
     * Error state when fetching customer records fails.
     */
    data class Error(
        val errorMessage: String,
        val canRetry: Boolean = true
    ) : CustomerListUiState
}
