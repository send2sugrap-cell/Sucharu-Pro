package com.sucharu.sucharupro.ui.features.finance.transaction

import com.sucharu.sucharupro.domain.model.finance.FinancialActivityEvent
import com.sucharu.sucharupro.domain.model.finance.FinancialLedgerEntry
import com.sucharu.sucharupro.domain.model.finance.FinancialTransaction

data class FinancialTransactionDetailsUiState(
    val isLoading: Boolean = false,
    val isActionInProgress: Boolean = false,
    val transaction: FinancialTransaction? = null,
    val ledgerEntries: List<FinancialLedgerEntry> = emptyList(),
    val activityEvents: List<FinancialActivityEvent> = emptyList(),
    val errorMessage: String? = null,
    val actionSuccessMessage: String? = null
)
