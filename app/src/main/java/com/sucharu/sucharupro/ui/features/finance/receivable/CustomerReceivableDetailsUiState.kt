package com.sucharu.sucharupro.ui.features.finance.receivable

import com.sucharu.sucharupro.domain.model.finance.CustomerReceivable
import com.sucharu.sucharupro.domain.model.finance.CustomerReceivableActivityEvent

data class CustomerReceivableDetailsUiState(
    val isLoading: Boolean = false,
    val isActionInProgress: Boolean = false,
    val receivable: CustomerReceivable? = null,
    val activityEvents: List<CustomerReceivableActivityEvent> = emptyList(),
    val errorMessage: String? = null,
    val actionSuccessMessage: String? = null
)
