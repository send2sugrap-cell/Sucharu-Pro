package com.sucharu.sucharupro.ui.features.orders.inquiry

import com.sucharu.sucharupro.domain.model.order.Inquiry
import com.sucharu.sucharupro.domain.model.order.InquiryStatusType

/**
 * UI State definition for Customer Inquiries list, search, and filtering.
 */
sealed interface InquiryListUiState {

    /** Initial loading state while fetching customer inquiries from repository. */
    data object Loading : InquiryListUiState

    /** Successfully loaded inquiries with active search query and status filter applied. */
    data class Success(
        val allInquiries: List<Inquiry>,
        val visibleInquiries: List<Inquiry>,
        val searchQuery: String = "",
        val selectedStatus: InquiryStatusType? = null,
        val isRefreshing: Boolean = false
    ) : InquiryListUiState {
        val totalCount: Int get() = allInquiries.size
        val visibleCount: Int get() = visibleInquiries.size
        val isFiltered: Boolean get() = searchQuery.isNotBlank() || selectedStatus != null
    }

    /** Empty state when no inquiry records exist. */
    data class Empty(
        val message: String = "No customer inquiries recorded yet. Customer requirement captures will appear here."
    ) : InquiryListUiState

    /** Error state when fetching inquiry records fails. */
    data class Error(
        val errorMessage: String,
        val canRetry: Boolean = true
    ) : InquiryListUiState
}
