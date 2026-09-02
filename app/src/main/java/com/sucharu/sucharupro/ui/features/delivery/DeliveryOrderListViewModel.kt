package com.sucharu.sucharupro.ui.features.delivery

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sucharu.sucharupro.domain.model.delivery.DeliveryOrder
import com.sucharu.sucharupro.domain.model.delivery.DeliveryOrderStatus
import com.sucharu.sucharupro.domain.model.delivery.DeliveryOrderType
import com.sucharu.sucharupro.domain.model.delivery.DeliveryPriority
import com.sucharu.sucharupro.domain.repository.DeliveryOrderRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel managing the Delivery Order list state and filters (Module 08 Step 01).
 */
class DeliveryOrderListViewModel(
    private val repository: DeliveryOrderRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DeliveryOrderListUiState(isLoading = true))
    val uiState: StateFlow<DeliveryOrderListUiState> = _uiState.asStateFlow()

    fun loadDeliveryOrders(projectId: String) {
        _uiState.update { it.copy(isLoading = true, projectId = projectId, errorMessage = null) }
        viewModelScope.launch {
            repository.observeDeliveryOrders(projectId).collect { list ->
                _uiState.update { current ->
                    val filtered = applyFilters(
                        orders = list,
                        query = current.searchQuery,
                        status = current.selectedStatusFilter,
                        priority = current.selectedPriorityFilter,
                        type = current.selectedTypeFilter
                    )
                    current.copy(
                        isLoading = false,
                        orders = list,
                        filteredOrders = filtered
                    )
                }
            }
        }
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.update { current ->
            val filtered = applyFilters(
                orders = current.orders,
                query = query,
                status = current.selectedStatusFilter,
                priority = current.selectedPriorityFilter,
                type = current.selectedTypeFilter
            )
            current.copy(searchQuery = query, filteredOrders = filtered)
        }
    }

    fun onStatusFilterChanged(status: DeliveryOrderStatus?) {
        _uiState.update { current ->
            val filtered = applyFilters(
                orders = current.orders,
                query = current.searchQuery,
                status = status,
                priority = current.selectedPriorityFilter,
                type = current.selectedTypeFilter
            )
            current.copy(selectedStatusFilter = status, filteredOrders = filtered)
        }
    }

    fun onPriorityFilterChanged(priority: DeliveryPriority?) {
        _uiState.update { current ->
            val filtered = applyFilters(
                orders = current.orders,
                query = current.searchQuery,
                status = current.selectedStatusFilter,
                priority = priority,
                type = current.selectedTypeFilter
            )
            current.copy(selectedPriorityFilter = priority, filteredOrders = filtered)
        }
    }

    fun onTypeFilterChanged(type: DeliveryOrderType?) {
        _uiState.update { current ->
            val filtered = applyFilters(
                orders = current.orders,
                query = current.searchQuery,
                status = current.selectedStatusFilter,
                priority = current.selectedPriorityFilter,
                type = type
            )
            current.copy(selectedTypeFilter = type, filteredOrders = filtered)
        }
    }

    private fun applyFilters(
        orders: List<DeliveryOrder>,
        query: String,
        status: DeliveryOrderStatus?,
        priority: DeliveryPriority?,
        type: DeliveryOrderType?
    ): List<DeliveryOrder> {
        return orders.filter { order ->
            val matchesQuery = query.isBlank() ||
                order.deliveryOrderNo.contains(query, ignoreCase = true) ||
                (order.customerId?.contains(query, ignoreCase = true) == true) ||
                (order.notes?.contains(query, ignoreCase = true) == true)

            val matchesStatus = status == null || order.status == status
            val matchesPriority = priority == null || order.priority == priority
            val matchesType = type == null || order.deliveryType == type

            matchesQuery && matchesStatus && matchesPriority && matchesType
        }
    }
}
