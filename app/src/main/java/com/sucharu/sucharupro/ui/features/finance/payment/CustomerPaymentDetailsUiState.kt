package com.sucharu.sucharupro.ui.features.finance.payment

import com.sucharu.sucharupro.domain.model.finance.CustomerPayment
import com.sucharu.sucharupro.domain.model.finance.CustomerPaymentActivityEvent
import com.sucharu.sucharupro.domain.model.finance.CustomerPaymentReceipt

data class CustomerPaymentDetailsUiState(
    val isLoading: Boolean = false,
    val isActionInProgress: Boolean = false,
    val payment: CustomerPayment? = null,
    val receipt: CustomerPaymentReceipt? = null,
    val activityEvents: List<CustomerPaymentActivityEvent> = emptyList(),
    val errorMessage: String? = null,
    val actionSuccessMessage: String? = null
)
