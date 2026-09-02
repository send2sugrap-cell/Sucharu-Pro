package com.sucharu.sucharupro.ui.features.delivery

import com.sucharu.sucharupro.domain.model.delivery.DeliveryOrderType
import com.sucharu.sucharupro.domain.model.delivery.DeliveryPriority

/**
 * UI State for Delivery Order Creation Form (Module 08 Step 01).
 */
data class DeliveryOrderFormUiState(
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val projectId: String = "",
    val deliveryOrderNo: String = "",
    val customerId: String = "",
    val sourceReferenceId: String = "",
    val deliveryType: DeliveryOrderType = DeliveryOrderType.CUSTOMER_DELIVERY,
    val priority: DeliveryPriority = DeliveryPriority.NORMAL,
    val requestedDeliveryDate: Long = System.currentTimeMillis() + 86400000L * 3, // default 3 days ahead
    val notes: String = "",
    val lines: List<DeliveryLineFormItem> = emptyList(),
    val errorMessage: String? = null,
    val isSavedSuccessfully: Boolean = false
)

data class DeliveryLineFormItem(
    val lineId: String,
    val productId: String,
    val requestedQuantity: Double,
    val notes: String = ""
)
