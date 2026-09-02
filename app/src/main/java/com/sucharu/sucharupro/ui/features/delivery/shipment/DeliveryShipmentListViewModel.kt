package com.sucharu.sucharupro.ui.features.delivery.shipment

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.delivery.shipment.DeliveryShipment
import com.sucharu.sucharupro.domain.model.delivery.shipment.DeliveryShipmentPriority
import com.sucharu.sucharupro.domain.model.delivery.shipment.DeliveryShipmentStatus
import com.sucharu.sucharupro.domain.model.delivery.shipment.DeliveryShipmentType
import com.sucharu.sucharupro.domain.repository.DeliveryShipmentRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel for Delivery Shipment List & Filtering (Module 08 Step 05).
 */
class DeliveryShipmentListViewModel(
    private val repository: DeliveryShipmentRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DeliveryShipmentListUiState(isLoading = true))
    val uiState: StateFlow<DeliveryShipmentListUiState> = _uiState.asStateFlow()

    fun loadShipments(projectId: String) {
        _uiState.update { it.copy(isLoading = true, projectId = projectId, errorMessage = null) }
        viewModelScope.launch {
            repository.observeShipments(projectId).collect { list ->
                val summaryResult = repository.getShipmentSummary(projectId)
                val summary = if (summaryResult is DomainResult.Success) summaryResult.data else _uiState.value.summary

                _uiState.update { current ->
                    val filtered = applyFilters(
                        shipments = list,
                        query = current.searchQuery,
                        status = current.selectedStatusFilter,
                        type = current.selectedTypeFilter,
                        priority = current.selectedPriorityFilter
                    )
                    current.copy(
                        isLoading = false,
                        shipments = list,
                        filteredShipments = filtered,
                        summary = summary
                    )
                }
            }
        }
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.update { current ->
            val filtered = applyFilters(
                shipments = current.shipments,
                query = query,
                status = current.selectedStatusFilter,
                type = current.selectedTypeFilter,
                priority = current.selectedPriorityFilter
            )
            current.copy(searchQuery = query, filteredShipments = filtered)
        }
    }

    fun onStatusFilterChanged(status: DeliveryShipmentStatus?) {
        _uiState.update { current ->
            val filtered = applyFilters(
                shipments = current.shipments,
                query = current.searchQuery,
                status = status,
                type = current.selectedTypeFilter,
                priority = current.selectedPriorityFilter
            )
            current.copy(selectedStatusFilter = status, filteredShipments = filtered)
        }
    }

    fun onTypeFilterChanged(type: DeliveryShipmentType?) {
        _uiState.update { current ->
            val filtered = applyFilters(
                shipments = current.shipments,
                query = current.searchQuery,
                status = current.selectedStatusFilter,
                type = type,
                priority = current.selectedPriorityFilter
            )
            current.copy(selectedTypeFilter = type, filteredShipments = filtered)
        }
    }

    fun onPriorityFilterChanged(priority: DeliveryShipmentPriority?) {
        _uiState.update { current ->
            val filtered = applyFilters(
                shipments = current.shipments,
                query = current.searchQuery,
                status = current.selectedStatusFilter,
                type = current.selectedTypeFilter,
                priority = priority
            )
            current.copy(selectedPriorityFilter = priority, filteredShipments = filtered)
        }
    }

    private fun applyFilters(
        shipments: List<DeliveryShipment>,
        query: String,
        status: DeliveryShipmentStatus?,
        type: DeliveryShipmentType?,
        priority: DeliveryShipmentPriority?
    ): List<DeliveryShipment> {
        return shipments.filter { item ->
            val matchesQuery = query.isBlank() ||
                item.shipmentNo.contains(query, ignoreCase = true) ||
                (item.trackingNumber?.contains(query, ignoreCase = true) == true) ||
                (item.carrierName?.contains(query, ignoreCase = true) == true) ||
                (item.destinationAddress?.contains(query, ignoreCase = true) == true) ||
                (item.destinationContactName?.contains(query, ignoreCase = true) == true)

            val matchesStatus = status == null || item.currentStatus == status
            val matchesType = type == null || item.shipmentType == type
            val matchesPriority = priority == null || item.priority == priority

            matchesQuery && matchesStatus && matchesType && matchesPriority
        }
    }
}
