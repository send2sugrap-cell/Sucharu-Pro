package com.sucharu.sucharupro.ui.features.orders.quotation

import com.sucharu.sucharupro.domain.model.order.Quotation
import com.sucharu.sucharupro.domain.model.order.QuotationStatusType

/**
 * UI State definition for Commercial Quotations list, search, and filtering.
 */
sealed interface QuotationListUiState {

    /** Initial loading state while fetching commercial quotations from repository. */
    data object Loading : QuotationListUiState

    /** Successfully loaded quotations with active search query and status filter applied. */
    data class Success(
        val allQuotations: List<Quotation>,
        val visibleQuotations: List<Quotation>,
        val searchQuery: String = "",
        val selectedStatus: QuotationStatusType? = null,
        val isRefreshing: Boolean = false
    ) : QuotationListUiState {
        val totalCount: Int get() = allQuotations.size
        val visibleCount: Int get() = visibleQuotations.size
        val isFiltered: Boolean get() = searchQuery.isNotBlank() || selectedStatus != null
    }

    /** Empty state when no quotation records exist. */
    data class Empty(
        val message: String = "No commercial quotations recorded yet. Created quotations will appear here."
    ) : QuotationListUiState

    /** Error state when fetching quotation records fails. */
    data class Error(
        val errorMessage: String,
        val canRetry: Boolean = true
    ) : QuotationListUiState
}
