package com.sucharu.sucharupro.ui.features.delivery.returning

import com.sucharu.sucharupro.domain.model.delivery.DeliveryOrderLine
import com.sucharu.sucharupro.domain.model.delivery.returning.DeliveryReturnPriority
import com.sucharu.sucharupro.domain.model.delivery.returning.DeliveryReturnReason
import com.sucharu.sucharupro.domain.model.delivery.returning.DeliveryReturnType

data class DeliveryReturnFormUiState(
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val projectId: String = "",
    val deliveryOrderId: String = "",
    val returnNo: String = "",
    val returnType: DeliveryReturnType = DeliveryReturnType.CUSTOMER_RETURN,
    val returnReason: DeliveryReturnReason = DeliveryReturnReason.CUSTOMER_REQUEST,
    val priority: DeliveryReturnPriority = DeliveryReturnPriority.NORMAL,
    val availableOrderLines: List<DeliveryOrderLine> = emptyList(),
    val selectedDoLineId: String = "",
    val returnQuantityText: String = "",
    val notes: String = "",
    val errorMessage: String? = null,
    val isSavedSuccessfully: Boolean = false
)
