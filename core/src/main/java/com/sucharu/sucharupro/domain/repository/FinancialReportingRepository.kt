package com.sucharu.sucharupro.domain.repository

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.finance.*
import com.sucharu.sucharupro.domain.model.user.UserRole
import kotlinx.coroutines.flow.Flow

/**
 * Domain repository interface for Financial Reporting & Analytics (Module 09 Step 09).
 *
 * READ/REPORTING FOCUSED. Derives all financial statements and analytics
 * strictly from canonical existing Module 09 financial records.
 * Never creates a secondary ledger or duplicates posting logic.
 */
interface FinancialReportingRepository {

    suspend fun getFinancialDashboard(
        projectId: String,
        filter: FinancialReportFilter,
        actorId: String,
        callerRole: UserRole
    ): DomainResult<FinancialKpiSummary>

    suspend fun getProfitLossReport(
        projectId: String,
        filter: FinancialReportFilter,
        actorId: String,
        callerRole: UserRole
    ): DomainResult<ProfitLossReport>

    suspend fun getBalanceSheetReport(
        projectId: String,
        filter: FinancialReportFilter,
        actorId: String,
        callerRole: UserRole
    ): DomainResult<BalanceSheetReport>

    suspend fun getCashFlowReport(
        projectId: String,
        filter: FinancialReportFilter,
        actorId: String,
        callerRole: UserRole
    ): DomainResult<CashFlowReport>

    suspend fun getTrialBalanceReport(
        projectId: String,
        filter: FinancialReportFilter,
        actorId: String,
        callerRole: UserRole
    ): DomainResult<TrialBalanceReport>

    suspend fun getGeneralLedgerReport(
        projectId: String,
        filter: FinancialReportFilter,
        actorId: String,
        callerRole: UserRole
    ): DomainResult<GeneralLedgerReport>

    suspend fun getAccountsReceivableReport(
        projectId: String,
        filter: FinancialReportFilter,
        actorId: String,
        callerRole: UserRole
    ): DomainResult<AccountsReceivableReport>

    suspend fun getAccountsPayableReport(
        projectId: String,
        filter: FinancialReportFilter,
        actorId: String,
        callerRole: UserRole
    ): DomainResult<AccountsPayableReport>

    suspend fun getExpenseAnalysisReport(
        projectId: String,
        filter: FinancialReportFilter,
        actorId: String,
        callerRole: UserRole
    ): DomainResult<ExpenseAnalysisReport>

    suspend fun getCustomerPaymentReport(
        projectId: String,
        filter: FinancialReportFilter,
        actorId: String,
        callerRole: UserRole
    ): DomainResult<CustomerPaymentReport>

    suspend fun getSupplierPaymentReport(
        projectId: String,
        filter: FinancialReportFilter,
        actorId: String,
        callerRole: UserRole
    ): DomainResult<SupplierPaymentReport>

    suspend fun getFinancialAdjustmentReport(
        projectId: String,
        filter: FinancialReportFilter,
        actorId: String,
        callerRole: UserRole
    ): DomainResult<FinancialAdjustmentReport>

    suspend fun getPeriodComparisonReport(
        projectId: String,
        periodA: FinancialReportPeriod,
        periodB: FinancialReportPeriod,
        actorId: String,
        callerRole: UserRole
    ): DomainResult<FinancialComparisonResult>

    suspend fun getFinancialKpiSummary(
        projectId: String,
        filter: FinancialReportFilter,
        actorId: String,
        callerRole: UserRole
    ): DomainResult<FinancialKpiSummary>

    suspend fun createReportSnapshot(
        projectId: String,
        reportType: FinancialReportType,
        filter: FinancialReportFilter,
        snapshotRequestId: String,
        actorId: String,
        callerRole: UserRole
    ): DomainResult<FinancialReportSnapshot>

    suspend fun getReportSnapshot(
        snapshotId: String,
        callerRole: UserRole
    ): DomainResult<FinancialReportSnapshot>

    fun observeReportSnapshots(
        projectId: String,
        callerRole: UserRole
    ): Flow<List<FinancialReportSnapshot>>

    suspend fun requestExport(
        request: FinancialReportExportRequest,
        callerRole: UserRole
    ): DomainResult<String>

    fun observeActivityEvents(
        projectId: String,
        callerRole: UserRole
    ): Flow<List<FinancialReportActivityEvent>>
}
