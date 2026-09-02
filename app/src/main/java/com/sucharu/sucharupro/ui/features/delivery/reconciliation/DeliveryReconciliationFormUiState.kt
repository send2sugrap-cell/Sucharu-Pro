package com.sucharu.sucharupro.ui.features.delivery.reconciliation

data class DeliveryReconciliationFormUiState(
    val deliveryOrderId: String = "",
    val reason: String = "",
    val isSubmitting: Boolean = false,
    val errorMessage: String? = null,
    val isCreatedSuccessfully: Boolean = false,
    val createdReconciliationId: String? = null
)
