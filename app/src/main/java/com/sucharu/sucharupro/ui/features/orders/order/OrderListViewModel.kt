package com.sucharu.sucharupro.ui.features.orders.order

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sucharu.sucharupro.data.datasource.FakeOrderDataSource
import com.sucharu.sucharupro.data.repository.OrderRepositoryImpl
import com.sucharu.sucharupro.domain.model.order.JobHandoffStatus
import com.sucharu.sucharupro.domain.model.order.Order
import com.sucharu.sucharupro.domain.model.order.OrderPriority
import com.sucharu.sucharupro.domain.model.order.OrderStatusType
import com.sucharu.sucharupro.domain.repository.OrderRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch

/**
 * ViewModel managing presentation state, search, and filtering for Commercial Customer Orders.
 */
class OrderListViewModel(
    private val repository: OrderRepository = OrderRepositoryImpl(FakeOrderDataSource()),
    private val externalScope: kotlinx.coroutines.CoroutineScope? = null
) : ViewModel() {

    private val scope: kotlinx.coroutines.CoroutineScope
        get() = externalScope ?: viewModelScope

    private val _uiState = MutableStateFlow<OrderListUiState>(OrderListUiState.Loading)
    val uiState: StateFlow<OrderListUiState> = _uiState.asStateFlow()

    private var rawOrders: List<Order> = emptyList()
    private var currentSearchQuery: String = ""
    private var currentStatusFilter: OrderStatusType? = null
    private var currentPriorityFilter: OrderPriority? = null
    private var currentHandoffFilter: JobHandoffStatus? = null

    init {
        loadOrders()
    }

    /** Observes the reactive orders stream from the repository. */
    fun loadOrders() {
        scope.launch {
            repository.getOrders()
                .onStart {
                    _uiState.value = OrderListUiState.Loading
                }
                .catch { exception ->
                    _uiState.value = OrderListUiState.Error(
                        errorMessage = exception.localizedMessage ?: "Failed to load customer orders."
                    )
                }
                .collect { orders ->
                    rawOrders = orders
                    if (orders.isEmpty()) {
                        _uiState.value = OrderListUiState.Empty()
                    } else {
                        updateFilteredState()
                    }
                }
        }
    }

    /** Handles search query text changes. */
    fun onSearchQueryChange(query: String) {
        currentSearchQuery = query
        updateFilteredState()
    }

    /** Handles order status filter changes. */
    fun onStatusFilterChange(status: OrderStatusType?) {
        currentStatusFilter = status
        updateFilteredState()
    }

    /** Handles order priority filter changes. */
    fun onPriorityFilterChange(priority: OrderPriority?) {
        currentPriorityFilter = priority
        updateFilteredState()
    }

    /** Handles job handoff readiness filter changes. */
    fun onHandoffFilterChange(handoff: JobHandoffStatus?) {
        currentHandoffFilter = handoff
        updateFilteredState()
    }

    /** Clears all active filters and search query. */
    fun clearFilters() {
        currentSearchQuery = ""
        currentStatusFilter = null
        currentPriorityFilter = null
        currentHandoffFilter = null
        updateFilteredState()
    }

    /** Retries fetching orders. */
    fun retry() {
        loadOrders()
    }

    private fun updateFilteredState() {
        if (rawOrders.isEmpty()) {
            _uiState.value = OrderListUiState.Empty()
            return
        }

        val trimmedQuery = currentSearchQuery.trim()

        val filtered = rawOrders.filter { order ->
            val matchesSearch = trimmedQuery.isBlank() ||
                order.orderNumber.contains(trimmedQuery, ignoreCase = true) ||
                order.orderId.contains(trimmedQuery, ignoreCase = true) ||
                order.customerId.contains(trimmedQuery, ignoreCase = true) ||
                (order.quotationId?.contains(trimmedQuery, ignoreCase = true) == true) ||
                (order.approvedQuotationRevisionId?.contains(trimmedQuery, ignoreCase = true) == true) ||
                (order.notes?.contains(trimmedQuery, ignoreCase = true) == true) ||
                (order.confirmedBy?.contains(trimmedQuery, ignoreCase = true) == true) ||
                order.items.any { item ->
                    item.description.contains(trimmedQuery, ignoreCase = true) ||
                        (item.specification?.contains(trimmedQuery, ignoreCase = true) == true)
                } ||
                (order.deliveryRequirement?.contactName?.contains(trimmedQuery, ignoreCase = true) == true) ||
                (order.deliveryRequirement?.contactPhone?.contains(trimmedQuery, ignoreCase = true) == true) ||
                (order.deliveryRequirement?.address?.contains(trimmedQuery, ignoreCase = true) == true) ||
                (order.deliveryRequirement?.instructions?.contains(trimmedQuery, ignoreCase = true) == true) ||
                (order.paymentTerms.customDescription?.contains(trimmedQuery, ignoreCase = true) == true)

            val matchesStatus = currentStatusFilter == null || order.status == currentStatusFilter
            val matchesPriority = currentPriorityFilter == null || order.priority == currentPriorityFilter
            val matchesHandoff = currentHandoffFilter == null || order.jobHandoffStatus == currentHandoffFilter

            matchesSearch && matchesStatus && matchesPriority && matchesHandoff
        }

        _uiState.value = OrderListUiState.Success(
            allOrders = rawOrders,
            visibleOrders = filtered,
            searchQuery = currentSearchQuery,
            selectedStatus = currentStatusFilter,
            selectedPriority = currentPriorityFilter,
            selectedHandoff = currentHandoffFilter
        )
    }
}
