package com.sucharu.sucharupro.ui.features.finance.transaction

import com.sucharu.sucharupro.domain.model.finance.FinancialTransaction
import com.sucharu.sucharupro.domain.model.finance.FinancialTransactionStatus
import com.sucharu.sucharupro.domain.model.finance.FinancialTransactionType

data class FinancialTransactionListUiState(
    val isLoading: Boolean = false,
    val transactions: List<FinancialTransaction> = emptyList(),
    val selectedStatusFilter: FinancialTransactionStatus? = null,
    val selectedTypeFilter: FinancialTransactionType? = null,
    val searchQuery: String = "",
    val errorMessage: String? = null
)
