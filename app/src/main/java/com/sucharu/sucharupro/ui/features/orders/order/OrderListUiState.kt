package com.sucharu.sucharupro.ui.features.orders.order

import com.sucharu.sucharupro.domain.model.order.JobHandoffStatus
import com.sucharu.sucharupro.domain.model.order.Order
import com.sucharu.sucharupro.domain.model.order.OrderPriority
import com.sucharu.sucharupro.domain.model.order.OrderStatusType

/**
 * UI State definition for Commercial Customer Orders list, search, and filtering.
 */
sealed interface OrderListUiState {

    /** Initial loading state while fetching customer orders from repository. */
    data object Loading : OrderListUiState

    /** Successfully loaded orders with active search query and filters applied. */
    data class Success(
        val allOrders: List<Order>,
        val visibleOrders: List<Order>,
        val searchQuery: String = "",
        val selectedStatus: OrderStatusType? = null,
        val selectedPriority: OrderPriority? = null,
        val selectedHandoff: JobHandoffStatus? = null,
        val isRefreshing: Boolean = false
    ) : OrderListUiState {
        val totalCount: Int get() = allOrders.size
        val visibleCount: Int get() = visibleOrders.size
        val isFiltered: Boolean get() = searchQuery.isNotBlank() || selectedStatus != null || selectedPriority != null || selectedHandoff != null
    }

    /** Empty state when no order records exist. */
    data class Empty(
        val message: String = "No customer orders recorded yet. Confirmed commercial orders will appear here."
    ) : OrderListUiState

    /** Error state when fetching order records fails. */
    data class Error(
        val errorMessage: String,
        val canRetry: Boolean = true
    ) : OrderListUiState
}
