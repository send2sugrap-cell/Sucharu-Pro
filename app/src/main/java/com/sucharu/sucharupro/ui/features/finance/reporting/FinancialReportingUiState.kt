package com.sucharu.sucharupro.ui.features.finance.reporting

import com.sucharu.sucharupro.domain.model.finance.*

/**
 * UI State for Financial Reporting & Management Analytics (Module 09 Step 09).
 */
data class FinancialReportingUiState(
    val isLoading: Boolean = false,
    val selectedReportType: FinancialReportType = FinancialReportType.DASHBOARD,
    val selectedPeriod: FinancialReportPeriod = FinancialReportPeriod.CurrentMonth,
    val filter: FinancialReportFilter = FinancialReportFilter(projectId = "PRJ-DEFAULT"),
    val kpiSummary: FinancialKpiSummary? = null,
    val profitLossReport: ProfitLossReport? = null,
    val balanceSheetReport: BalanceSheetReport? = null,
    val cashFlowReport: CashFlowReport? = null,
    val trialBalanceReport: TrialBalanceReport? = null,
    val generalLedgerReport: GeneralLedgerReport? = null,
    val accountsReceivableReport: AccountsReceivableReport? = null,
    val accountsPayableReport: AccountsPayableReport? = null,
    val expenseAnalysisReport: ExpenseAnalysisReport? = null,
    val customerPaymentReport: CustomerPaymentReport? = null,
    val supplierPaymentReport: SupplierPaymentReport? = null,
    val financialAdjustmentReport: FinancialAdjustmentReport? = null,
    val periodComparisonResult: FinancialComparisonResult? = null,
    val snapshots: List<FinancialReportSnapshot> = emptyList(),
    val activityEvents: List<FinancialReportActivityEvent> = emptyList(),
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val isFilterSheetVisible: Boolean = false
)
