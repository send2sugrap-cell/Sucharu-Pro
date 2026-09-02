package com.sucharu.sucharupro.ui.features.delivery.shipment

import com.sucharu.sucharupro.domain.model.delivery.shipment.DeliveryShipment
import com.sucharu.sucharupro.domain.model.delivery.shipment.DeliveryShipmentPriority
import com.sucharu.sucharupro.domain.model.delivery.shipment.DeliveryShipmentStatus
import com.sucharu.sucharupro.domain.model.delivery.shipment.DeliveryShipmentSummary
import com.sucharu.sucharupro.domain.model.delivery.shipment.DeliveryShipmentType

/**
 * UI State for Delivery Shipment List (Module 08 Step 05).
 */
data class DeliveryShipmentListUiState(
    val isLoading: Boolean = false,
    val projectId: String = "",
    val shipments: List<DeliveryShipment> = emptyList(),
    val filteredShipments: List<DeliveryShipment> = emptyList(),
    val summary: DeliveryShipmentSummary = DeliveryShipmentSummary(),
    val searchQuery: String = "",
    val selectedStatusFilter: DeliveryShipmentStatus? = null,
    val selectedTypeFilter: DeliveryShipmentType? = null,
    val selectedPriorityFilter: DeliveryShipmentPriority? = null,
    val errorMessage: String? = null
)
