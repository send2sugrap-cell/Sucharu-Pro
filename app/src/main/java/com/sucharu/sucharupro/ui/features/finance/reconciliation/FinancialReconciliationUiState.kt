package com.sucharu.sucharupro.ui.features.finance.reconciliation

import com.sucharu.sucharupro.domain.model.finance.BankReconciliation
import com.sucharu.sucharupro.domain.model.finance.CashReconciliation
import com.sucharu.sucharupro.domain.model.finance.FinancialControlSummary
import com.sucharu.sucharupro.domain.model.finance.FinancialReconciliation
import com.sucharu.sucharupro.domain.model.finance.FinancialReconciliationDiscrepancy
import com.sucharu.sucharupro.domain.model.finance.FinancialReconciliationStatus
import com.sucharu.sucharupro.domain.model.finance.FinancialReconciliationType
import com.sucharu.sucharupro.domain.model.finance.LedgerReconciliationReport

data class FinancialReconciliationDashboardUiState(
    val isLoading: Boolean = false,
    val summary: FinancialControlSummary? = null,
    val reconciliations: List<FinancialReconciliation> = emptyList(),
    val discrepancies: List<FinancialReconciliationDiscrepancy> = emptyList(),
    val selectedTypeFilter: FinancialReconciliationType? = null,
    val selectedStatusFilter: FinancialReconciliationStatus? = null,
    val searchQuery: String = "",
    val errorMessage: String? = null,
    val userFeedbackMessage: String? = null
)

data class FinancialReconciliationExecutionUiState(
    val isLoading: Boolean = false,
    val activeTab: Int = 0, // 0 = Cash, 1 = Bank, 2 = Ledger
    val cashReconciliation: CashReconciliation? = null,
    val bankReconciliation: BankReconciliation? = null,
    val ledgerReport: LedgerReconciliationReport? = null,
    val selectedReconciliation: FinancialReconciliation? = null,
    val isExecuting: Boolean = false,
    val errorMessage: String? = null,
    val actionSuccessMessage: String? = null
)

data class FinancialDiscrepanciesUiState(
    val isLoading: Boolean = false,
    val discrepancies: List<FinancialReconciliationDiscrepancy> = emptyList(),
    val selectedDiscrepancy: FinancialReconciliationDiscrepancy? = null,
    val isResolving: Boolean = false,
    val isWaiving: Boolean = false,
    val resolutionNote: String = "",
    val waiverReason: String = "",
    val errorMessage: String? = null,
    val successMessage: String? = null
)
