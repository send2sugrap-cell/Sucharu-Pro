package com.sucharu.sucharupro.ui.features.delivery

import com.sucharu.sucharupro.domain.model.delivery.DeliveryOrder
import com.sucharu.sucharupro.domain.model.delivery.DeliveryOrderStatus
import com.sucharu.sucharupro.domain.model.delivery.DeliveryOrderType
import com.sucharu.sucharupro.domain.model.delivery.DeliveryPriority

/**
 * UI State for the Delivery Order List screen (Module 08 Step 01).
 */
data class DeliveryOrderListUiState(
    val isLoading: Boolean = false,
    val projectId: String = "",
    val orders: List<DeliveryOrder> = emptyList(),
    val filteredOrders: List<DeliveryOrder> = emptyList(),
    val searchQuery: String = "",
    val selectedStatusFilter: DeliveryOrderStatus? = null,
    val selectedPriorityFilter: DeliveryPriority? = null,
    val selectedTypeFilter: DeliveryOrderType? = null,
    val errorMessage: String? = null
)
