package com.sucharu.sucharupro.ui.features.orders.quotation.details

import com.sucharu.sucharupro.domain.model.order.Quotation
import com.sucharu.sucharupro.domain.model.order.QuotationRevision

/**
 * UI State definition for Commercial Quotation Details screen.
 */
sealed interface QuotationDetailsUiState {

    /** Initial loading state while fetching quotation and revisions. */
    data object Loading : QuotationDetailsUiState

    /** Successfully loaded quotation with revision history and currently selected revision snapshot. */
    data class Success(
        val quotation: Quotation,
        val revisions: List<QuotationRevision>,
        val selectedRevisionId: String? = null,
        val linkedOrders: List<com.sucharu.sucharupro.domain.model.order.Order> = emptyList(),
        val isActionInProgress: Boolean = false,
        val actionMessage: String? = null,
        val actionError: String? = null
    ) : QuotationDetailsUiState {
        /** Currently viewed revision snapshot (defaults to quotation's current revision). */
        val activeRevision: QuotationRevision?
            get() = if (selectedRevisionId != null) {
                revisions.find { it.revisionId == selectedRevisionId } ?: quotation.currentRevision
            } else {
                quotation.currentRevision
            }

        val isViewingHistoricalRevision: Boolean
            get() = activeRevision != null && activeRevision?.revisionNumber != quotation.currentRevisionNumber

        /** Indicates whether this quotation has already been converted into a confirmed order. */
        val hasLinkedOrder: Boolean
            get() = linkedOrders.isNotEmpty()
    }

    /** Quotation record not found with the provided ID. */
    data class NotFound(
        val quotationId: String
    ) : QuotationDetailsUiState

    /** Error state when quotation retrieval fails. */
    data class Error(
        val errorMessage: String,
        val canRetry: Boolean = true
    ) : QuotationDetailsUiState
}
