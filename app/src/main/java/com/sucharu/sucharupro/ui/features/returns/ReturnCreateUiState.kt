package com.sucharu.sucharupro.ui.features.returns

import com.sucharu.sucharupro.domain.model.returns.ReturnReason

/**
 * UI State for Return Request Create Form (Module 11 Step 02).
 */
data class ReturnCreateUiState(
    val projectId: String = "",
    val customerId: String = "",
    val originalChallanId: String = "",
    val originalChallanItemId: String = "",
    val productId: String = "",
    val requestedQuantityText: String = "",
    val unit: String = "pcs",
    val reason: ReturnReason = ReturnReason.PRINTING_DEFECT,
    val notes: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isSuccess: Boolean = false,
    val createdReturnId: String? = null
) {
    val isFormValid: Boolean
        get() = projectId.isNotBlank() &&
            customerId.isNotBlank() &&
            productId.isNotBlank() &&
            (requestedQuantityText.toIntOrNull() ?: 0) > 0
}
