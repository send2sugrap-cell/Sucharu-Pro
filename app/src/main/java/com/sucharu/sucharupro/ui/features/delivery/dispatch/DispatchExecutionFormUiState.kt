package com.sucharu.sucharupro.ui.features.delivery.dispatch

import com.sucharu.sucharupro.domain.model.delivery.challan.DeliveryChallan
import com.sucharu.sucharupro.domain.model.delivery.challan.DeliveryChallanLine
import com.sucharu.sucharupro.domain.model.delivery.dispatch.DispatchExecutionType

/**
 * UI State for Dispatch Execution creation form (Module 08 Step 03).
 */
data class DispatchExecutionFormUiState(
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val projectId: String = "",
    val dispatchNo: String = "",
    val selectedDeliveryChallanId: String = "",
    val availableChallans: List<DeliveryChallan> = emptyList(),
    val sourceWarehouseId: String = "MAIN-WH",
    val sourceLocationId: String = "MAIN-DISPATCH",
    val dispatchType: DispatchExecutionType = DispatchExecutionType.STANDARD,
    val dispatchDate: Long = System.currentTimeMillis(),
    val notes: String = "",
    val lines: List<DispatchExecutionLineFormItem> = emptyList(),
    val errorMessage: String? = null,
    val isSavedSuccessfully: Boolean = false
)

data class DispatchExecutionLineFormItem(
    val lineId: String,
    val deliveryChallanLineId: String,
    val deliveryOrderLineId: String,
    val productId: String,
    val requestedQuantity: Double,
    val dispatchQuantity: Double,
    val sourceLocationId: String = "MAIN-DISPATCH",
    val batchId: String = "",
    val lotId: String = ""
)
