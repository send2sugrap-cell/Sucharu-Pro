package com.sucharu.sucharupro.ui.features.orders.inquiry.details

import com.sucharu.sucharupro.domain.model.order.Inquiry

/**
 * UI State definition for Customer Inquiry Details screen.
 */
sealed interface InquiryDetailsUiState {

    /** Initial loading state while fetching inquiry record. */
    data object Loading : InquiryDetailsUiState

    /** Successfully loaded inquiry record. */
    data class Success(
        val inquiry: Inquiry
    ) : InquiryDetailsUiState

    /** Inquiry record not found with the provided ID. */
    data class NotFound(
        val inquiryId: String
    ) : InquiryDetailsUiState

    /** Error state when inquiry retrieval fails. */
    data class Error(
        val errorMessage: String,
        val canRetry: Boolean = true
    ) : InquiryDetailsUiState
}
