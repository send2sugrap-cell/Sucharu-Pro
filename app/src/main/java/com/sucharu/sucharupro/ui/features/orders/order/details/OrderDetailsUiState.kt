package com.sucharu.sucharupro.ui.features.orders.order.details

import com.sucharu.sucharupro.domain.model.order.Order

/**
 * UI State definition for Commercial Order Details / Profile screen.
 */
sealed interface OrderDetailsUiState {

    /** Initial loading state while fetching order record. */
    data object Loading : OrderDetailsUiState

    /** Successfully loaded commercial order record with immutable snapshot data and action feedback. */
    data class Success(
        val order: Order,
        val handoff: com.sucharu.sucharupro.domain.model.handoff.OrderJobHandoff? = null,
        val isActionInProgress: Boolean = false,
        val actionMessage: String? = null,
        val actionError: String? = null
    ) : OrderDetailsUiState

    /** Order record not found with the provided ID. */
    data class NotFound(
        val orderId: String
    ) : OrderDetailsUiState

    /** Error state when order retrieval fails. */
    data class Error(
        val errorMessage: String,
        val canRetry: Boolean = true
    ) : OrderDetailsUiState
}
