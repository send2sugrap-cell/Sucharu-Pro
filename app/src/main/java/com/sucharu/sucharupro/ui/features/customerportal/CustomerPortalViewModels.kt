package com.sucharu.sucharupro.ui.features.customerportal

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sucharu.sucharupro.data.api.client.BackendApiClient
import com.sucharu.sucharupro.data.api.model.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class CustomerPortalDashboardViewModel(
    private val apiClient: BackendApiClient
) : ViewModel() {

    private val _uiState = MutableStateFlow<CustomerPortalUiState<CustomerDashboardSummary>>(CustomerPortalUiState.Loading)
    val uiState: StateFlow<CustomerPortalUiState<CustomerDashboardSummary>> = _uiState.asStateFlow()

    fun loadDashboard() {
        viewModelScope.launch {
            _uiState.value = CustomerPortalUiState.Loading
            try {
                val profileRes = apiClient.getCustomerProfile()
                val ordersRes = apiClient.getCustomerOrders()

                if (profileRes is ApiResult.Success && ordersRes is ApiResult.Success) {
                    val profile = profileRes.data
                    val orders = ordersRes.data

                    val activeCount = orders.count { it.status != "DELIVERED" && it.status != "CANCELLED" }
                    val inProdCount = orders.count { it.status == "IN_PRODUCTION" || it.status == "PRINTING" }
                    val readyCount = orders.count { it.status == "READY" }
                    val pendingPaymentCount = orders.count { it.status == "PENDING_PAYMENT" || it.status == "ISSUED" }

                    val summary = CustomerDashboardSummary(
                        profile = profile,
                        activeOrderCount = activeCount,
                        inProductionCount = inProdCount,
                        readyForDeliveryCount = readyCount,
                        pendingPaymentCount = pendingPaymentCount,
                        totalOutstandingBalance = profile.currentBalance,
                        recentOrders = orders.take(5)
                    )
                    _uiState.value = CustomerPortalUiState.Success(summary)
                } else {
                    val msg = (profileRes as? ApiResult.Error)?.errorResponse?.message
                        ?: (ordersRes as? ApiResult.Error)?.errorResponse?.message
                        ?: "Failed to load customer dashboard."
                    _uiState.value = CustomerPortalUiState.Error(msg)
                }
            } catch (e: Exception) {
                _uiState.value = CustomerPortalUiState.Error("Network error: ${e.message ?: "Failed to connect"}")
            }
        }
    }
}

class CustomerPortalOrdersViewModel(
    private val apiClient: BackendApiClient
) : ViewModel() {

    private val _ordersState = MutableStateFlow<CustomerPortalUiState<List<CustomerOrderSummaryDto>>>(CustomerPortalUiState.Loading)
    val ordersState: StateFlow<CustomerPortalUiState<List<CustomerOrderSummaryDto>>> = _ordersState.asStateFlow()

    private val _selectedOrderDetailState = MutableStateFlow<CustomerPortalUiState<CustomerOrderDetailDto>>(CustomerPortalUiState.Empty)
    val selectedOrderDetailState: StateFlow<CustomerPortalUiState<CustomerOrderDetailDto>> = _selectedOrderDetailState.asStateFlow()

    fun loadOrders() {
        viewModelScope.launch {
            _ordersState.value = CustomerPortalUiState.Loading
            try {
                val res = apiClient.getCustomerOrders()
                if (res is ApiResult.Success) {
                    val list = res.data
                    if (list.isEmpty()) {
                        _ordersState.value = CustomerPortalUiState.Empty
                    } else {
                        _ordersState.value = CustomerPortalUiState.Success(list)
                    }
                } else if (res is ApiResult.Error) {
                    _ordersState.value = CustomerPortalUiState.Error(res.errorResponse.message)
                }
            } catch (e: Exception) {
                _ordersState.value = CustomerPortalUiState.Error("Network error: ${e.message ?: "Failed to load orders"}")
            }
        }
    }

    fun loadOrderDetail(orderId: String) {
        viewModelScope.launch {
            _selectedOrderDetailState.value = CustomerPortalUiState.Loading
            try {
                val res = apiClient.getCustomerOrderDetail(orderId)
                if (res is ApiResult.Success) {
                    _selectedOrderDetailState.value = CustomerPortalUiState.Success(res.data)
                } else if (res is ApiResult.Error) {
                    _selectedOrderDetailState.value = CustomerPortalUiState.Error(res.errorResponse.message)
                }
            } catch (e: Exception) {
                _selectedOrderDetailState.value = CustomerPortalUiState.Error("Network error: ${e.message ?: "Failed to load order details"}")
            }
        }
    }
}

class CustomerPortalProductionViewModel(
    private val apiClient: BackendApiClient
) : ViewModel() {

    private val _productionState = MutableStateFlow<CustomerPortalUiState<CustomerProductionStatus>>(CustomerPortalUiState.Loading)
    val productionState: StateFlow<CustomerPortalUiState<CustomerProductionStatus>> = _productionState.asStateFlow()

    private val canonicalStages = listOf(
        "DESIGN" to "Design & Prepress",
        "APPROVAL" to "Client Approval",
        "QC" to "Prepress Quality Control",
        "ITEM_APPROVAL" to "Item Approval",
        "CTP" to "CTP Plate Making",
        "PRINTING" to "Offset Printing Press",
        "LAMINATION" to "Lamination & Coating",
        "FOLDING" to "Folding & Die Cutting",
        "BINDING" to "Book Binding & Stitching",
        "FINAL_QC" to "Final Quality Control Inspection",
        "PACKAGING" to "Commercial Packaging",
        "READY" to "Ready for Delivery",
        "DELIVERED" to "Delivered to Customer"
    )

    fun loadProductionStatus(orderId: String) {
        viewModelScope.launch {
            _productionState.value = CustomerPortalUiState.Loading
            try {
                val orderRes = apiClient.getCustomerOrderDetail(orderId)
                if (orderRes is ApiResult.Success) {
                    val order = orderRes.data
                    val currentStageName = when (order.status) {
                        "DRAFT", "PENDING" -> "DESIGN"
                        "CONFIRMED", "APPROVED" -> "APPROVAL"
                        "IN_PRODUCTION" -> "PRINTING"
                        "PRINTING" -> "PRINTING"
                        "READY" -> "READY"
                        "DELIVERED" -> "DELIVERED"
                        else -> "PRINTING"
                    }

                    val currentIndex = canonicalStages.indexOfFirst { it.first == currentStageName }.coerceAtLeast(5)

                    val stagesProgress = canonicalStages.mapIndexed { idx, stage ->
                        CustomerProductionStageProgress(
                            stageNumber = idx + 1,
                            stageName = stage.first,
                            stageLabel = stage.second,
                            isCompleted = idx < currentIndex,
                            isCurrent = idx == currentIndex,
                            completedAt = if (idx < currentIndex) "Completed" else null
                        )
                    }

                    val status = CustomerProductionStatus(
                        orderId = order.orderId,
                        orderNumber = order.orderNumber,
                        jobId = "JOB-${order.orderId.takeLast(6)}",
                        currentStage = currentStageName,
                        currentStageLabel = canonicalStages.find { it.first == currentStageName }?.second ?: "Printing",
                        stages = stagesProgress,
                        updatedAt = System.currentTimeMillis()
                    )
                    _productionState.value = CustomerPortalUiState.Success(status)
                } else {
                    val msg = (orderRes as? ApiResult.Error)?.errorResponse?.message ?: "Failed to load production tracking."
                    _productionState.value = CustomerPortalUiState.Error(msg)
                }
            } catch (e: Exception) {
                _productionState.value = CustomerPortalUiState.Error("Network error: ${e.message ?: "Failed to load tracking"}")
            }
        }
    }
}
