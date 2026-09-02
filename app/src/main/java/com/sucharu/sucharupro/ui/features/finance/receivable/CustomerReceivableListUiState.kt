package com.sucharu.sucharupro.ui.features.finance.receivable

import com.sucharu.sucharupro.domain.model.finance.CustomerDueSummary
import com.sucharu.sucharupro.domain.model.finance.CustomerReceivable
import com.sucharu.sucharupro.domain.model.finance.CustomerReceivableStatus
import com.sucharu.sucharupro.domain.model.finance.ReceivableAgingBucket

data class CustomerReceivableListUiState(
    val isLoading: Boolean = false,
    val summary: CustomerDueSummary? = null,
    val receivables: List<CustomerReceivable> = emptyList(),
    val selectedStatusFilter: CustomerReceivableStatus? = null,
    val selectedAgingFilter: ReceivableAgingBucket? = null,
    val searchQuery: String = "",
    val errorMessage: String? = null
)
