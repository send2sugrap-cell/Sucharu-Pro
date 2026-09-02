package com.sucharu.sucharupro.ui.features.finance.payment

import com.sucharu.sucharupro.domain.model.finance.CustomerPayment
import com.sucharu.sucharupro.domain.model.finance.CustomerPaymentMethod
import com.sucharu.sucharupro.domain.model.finance.CustomerPaymentStatus

data class CustomerPaymentListUiState(
    val isLoading: Boolean = false,
    val payments: List<CustomerPayment> = emptyList(),
    val selectedStatusFilter: CustomerPaymentStatus? = null,
    val selectedMethodFilter: CustomerPaymentMethod? = null,
    val searchQuery: String = "",
    val errorMessage: String? = null
)
