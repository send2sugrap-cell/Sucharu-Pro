package com.sucharu.sucharupro.ui.features.finance.periodclose

import com.sucharu.sucharupro.domain.model.finance.AccountingPeriod
import com.sucharu.sucharupro.domain.model.finance.FinancialClosingReadiness
import com.sucharu.sucharupro.domain.model.finance.FinancialPeriodClosingSnapshot
import com.sucharu.sucharupro.domain.model.finance.FinancialPeriodReopenRequest

data class AccountingPeriodListUiState(
    val isLoading: Boolean = false,
    val periods: List<AccountingPeriod> = emptyList(),
    val activePeriod: AccountingPeriod? = null,
    val readiness: FinancialClosingReadiness? = null,
    val reopenRequests: List<FinancialPeriodReopenRequest> = emptyList(),
    val closingSnapshots: List<FinancialPeriodClosingSnapshot> = emptyList(),
    val errorMessage: String? = null,
    val actionSuccessMessage: String? = null
)

data class AccountingPeriodDetailsUiState(
    val isLoading: Boolean = false,
    val period: AccountingPeriod? = null,
    val readiness: FinancialClosingReadiness? = null,
    val snapshot: FinancialPeriodClosingSnapshot? = null,
    val reopenRequests: List<FinancialPeriodReopenRequest> = emptyList(),
    val isClosing: Boolean = false,
    val isEvaluating: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null
)
