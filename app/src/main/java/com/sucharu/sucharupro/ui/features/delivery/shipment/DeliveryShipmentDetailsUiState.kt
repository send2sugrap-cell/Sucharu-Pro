package com.sucharu.sucharupro.ui.features.delivery.shipment

import com.sucharu.sucharupro.domain.model.delivery.shipment.DeliveryShipment
import com.sucharu.sucharupro.domain.model.delivery.shipment.DeliveryShipmentActivityEvent
import com.sucharu.sucharupro.domain.model.delivery.shipment.DeliveryShipmentAttempt
import com.sucharu.sucharupro.domain.model.delivery.shipment.DeliveryShipmentEvent

/**
 * UI State for Delivery Shipment Details (Module 08 Step 05).
 */
data class DeliveryShipmentDetailsUiState(
    val isLoading: Boolean = false,
    val isActionInProgress: Boolean = false,
    val shipment: DeliveryShipment? = null,
    val trackingEvents: List<DeliveryShipmentEvent> = emptyList(),
    val deliveryAttempts: List<DeliveryShipmentAttempt> = emptyList(),
    val activityEvents: List<DeliveryShipmentActivityEvent> = emptyList(),
    val errorMessage: String? = null,
    val successMessage: String? = null
)
