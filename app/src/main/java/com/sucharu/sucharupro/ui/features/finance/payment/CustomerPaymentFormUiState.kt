package com.sucharu.sucharupro.ui.features.finance.payment

import com.sucharu.sucharupro.domain.model.common.Money
import com.sucharu.sucharupro.domain.model.finance.CustomerPaymentMethod

data class CustomerPaymentFormUiState(
    val customerIdInput: String = "",
    val receivableIdInput: String = "",
    val outstandingAmount: Money? = null,
    val amountInput: String = "",
    val paymentMethod: CustomerPaymentMethod = CustomerPaymentMethod.CASH,
    val paymentReferenceInput: String = "",
    val notesInput: String = "",
    val isSubmitting: Boolean = false,
    val successPaymentId: String? = null,
    val errorMessage: String? = null
)
