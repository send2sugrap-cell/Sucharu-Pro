package com.sucharu.sucharupro.ui.features.delivery.verification

import com.sucharu.sucharupro.domain.model.delivery.dispatch.DispatchExecution

/**
 * UI State for Delivery Item Verification Form (Module 08 Step 04).
 */
data class DeliveryItemVerificationFormUiState(
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val projectId: String = "",
    val verificationNo: String = "",
    val selectedDispatchId: String = "",
    val availableDispatches: List<DispatchExecution> = emptyList(),
    val remarks: String = "",
    val lines: List<DeliveryItemVerificationLineFormItem> = emptyList(),
    val errorMessage: String? = null,
    val isSavedSuccessfully: Boolean = false
)

data class DeliveryItemVerificationLineFormItem(
    val lineId: String,
    val dispatchExecutionLineId: String,
    val challanLineId: String,
    val deliveryOrderLineId: String,
    val productId: String,
    val expectedQuantity: Double,
    val verifiedQuantity: Double,
    val batchId: String = "",
    val lotId: String = "",
    val isDamaged: Boolean = false,
    val damagedQuantity: Double = 0.0,
    val isMissing: Boolean = false,
    val isProductMismatch: Boolean = false,
    val isBatchMismatch: Boolean = false,
    val isLotMismatch: Boolean = false,
    val remarks: String = ""
)
