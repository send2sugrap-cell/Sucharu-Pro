package com.sucharu.sucharupro.ui.features.delivery.shipment

import com.sucharu.sucharupro.domain.model.delivery.dispatch.DispatchExecution
import com.sucharu.sucharupro.domain.model.delivery.shipment.DeliveryShipmentPriority
import com.sucharu.sucharupro.domain.model.delivery.shipment.DeliveryShipmentType

/**
 * UI State for Delivery Shipment Form (Module 08 Step 05).
 */
data class DeliveryShipmentFormUiState(
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val projectId: String = "",
    val shipmentNo: String = "",
    val selectedDispatchId: String = "",
    val availableDispatches: List<DispatchExecution> = emptyList(),
    val shipmentType: DeliveryShipmentType = DeliveryShipmentType.STANDARD,
    val priority: DeliveryShipmentPriority = DeliveryShipmentPriority.NORMAL,
    val carrierName: String = "",
    val carrierReference: String = "",
    val trackingNumber: String = "",
    val destinationAddress: String = "",
    val destinationContactName: String = "",
    val destinationContactPhone: String = "",
    val destinationNotes: String = "",
    val notes: String = "",
    val errorMessage: String? = null,
    val isSavedSuccessfully: Boolean = false
)
