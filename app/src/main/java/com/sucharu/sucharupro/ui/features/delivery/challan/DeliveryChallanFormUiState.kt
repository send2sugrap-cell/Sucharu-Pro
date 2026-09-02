package com.sucharu.sucharupro.ui.features.delivery.challan

import com.sucharu.sucharupro.domain.model.delivery.DeliveryOrder
import com.sucharu.sucharupro.domain.model.delivery.DeliveryOrderLine
import com.sucharu.sucharupro.domain.model.delivery.challan.DeliveryChallanType

/**
 * UI State for Delivery Challan Creation Form (Module 08 Step 02).
 */
data class DeliveryChallanFormUiState(
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val projectId: String = "",
    val challanNo: String = "",
    val selectedDeliveryOrderId: String = "",
    val availableOrders: List<DeliveryOrder> = emptyList(),
    val selectedOrderLines: List<DeliveryOrderLine> = emptyList(),
    val allocatedQuantitiesMap: Map<String, Double> = emptyMap(),
    val challanType: DeliveryChallanType = DeliveryChallanType.STANDARD,
    val issueDate: Long = System.currentTimeMillis(),
    val notes: String = "",
    val lines: List<DeliveryChallanLineFormItem> = emptyList(),
    val errorMessage: String? = null,
    val isSavedSuccessfully: Boolean = false
)

data class DeliveryChallanLineFormItem(
    val lineId: String,
    val deliveryOrderLineId: String,
    val productId: String,
    val requestedQuantity: Double,
    val alreadyAllocatedQuantity: Double,
    val remainingQuantity: Double,
    val quantity: Double,
    val notes: String = ""
)
