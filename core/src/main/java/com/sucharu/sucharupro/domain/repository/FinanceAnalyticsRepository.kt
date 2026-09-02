package com.sucharu.sucharupro.domain.repository

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.finance.*
import com.sucharu.sucharupro.domain.model.user.UserRole
import kotlinx.coroutines.flow.Flow

/**
 * Domain repository contract for Finance Analytics & Governance (Module 09 Step 10).
 *
 * READ/ANALYTICS & GOVERNANCE FOCUSED.
 * Transforms verified canonical records into strategic management intelligence.
 */
interface FinanceAnalyticsRepository {

    suspend fun getDashboard(
        projectId: String,
        filter: AnalyticsFilter,
        actorId: String,
        callerRole: UserRole
    ): DomainResult<FinanceAnalyticsDashboard>

    suspend fun getSummary(
        projectId: String,
        filter: AnalyticsFilter,
        actorId: String,
        callerRole: UserRole
    ): DomainResult<FinanceAnalyticsSummary>

    suspend fun getProfitabilityAnalytics(
        projectId: String,
        filter: AnalyticsFilter,
        actorId: String,
        callerRole: UserRole
    ): DomainResult<ProfitabilityAnalytics>

    suspend fun getCashFlowAnalytics(
        projectId: String,
        filter: AnalyticsFilter,
        actorId: String,
        callerRole: UserRole
    ): DomainResult<CashFlowAnalytics>

    suspend fun getReceivableAnalytics(
        projectId: String,
        filter: AnalyticsFilter,
        actorId: String,
        callerRole: UserRole
    ): DomainResult<ReceivableAnalytics>

    suspend fun getPayableAnalytics(
        projectId: String,
        filter: AnalyticsFilter,
        actorId: String,
        callerRole: UserRole
    ): DomainResult<PayableAnalytics>

    suspend fun getExpenseAnalytics(
        projectId: String,
        filter: AnalyticsFilter,
        actorId: String,
        callerRole: UserRole
    ): DomainResult<ExpenseAnalytics>

    suspend fun getCollectionPerformance(
        projectId: String,
        filter: AnalyticsFilter,
        actorId: String,
        callerRole: UserRole
    ): DomainResult<CollectionPerformanceAnalytics>

    suspend fun getSupplierPaymentAnalytics(
        projectId: String,
        filter: AnalyticsFilter,
        actorId: String,
        callerRole: UserRole
    ): DomainResult<SupplierPaymentAnalytics>

    suspend fun calculateFinancialHealth(
        projectId: String,
        filter: AnalyticsFilter,
        actorId: String,
        callerRole: UserRole
    ): DomainResult<FinancialHealthScore>

    suspend fun detectRisks(
        projectId: String,
        filter: AnalyticsFilter,
        actorId: String,
        callerRole: UserRole
    ): DomainResult<List<FinancialRiskIndicator>>

    suspend fun detectAnomalies(
        projectId: String,
        filter: AnalyticsFilter,
        actorId: String,
        callerRole: UserRole
    ): DomainResult<List<FinancialAnomaly>>

    suspend fun runGovernanceControls(
        projectId: String,
        filter: AnalyticsFilter,
        actorId: String,
        callerRole: UserRole
    ): DomainResult<List<AnalyticsControlResult>>

    suspend fun comparePeriods(
        projectId: String,
        periodA: FinancialReportPeriod,
        periodB: FinancialReportPeriod,
        actorId: String,
        callerRole: UserRole
    ): DomainResult<FinancialPeriodComparison>

    suspend fun generateForecast(
        projectId: String,
        method: ForecastMethod,
        actorId: String,
        callerRole: UserRole
    ): DomainResult<FinancialForecastSummary>

    suspend fun createSnapshot(
        projectId: String,
        filter: AnalyticsFilter,
        snapshotRequestId: String,
        actorId: String,
        callerRole: UserRole
    ): DomainResult<FinancialAnalyticsSnapshot>

    suspend fun getSnapshot(
        snapshotId: String,
        callerRole: UserRole
    ): DomainResult<FinancialAnalyticsSnapshot>

    fun observeSnapshots(
        projectId: String,
        callerRole: UserRole
    ): Flow<List<FinancialAnalyticsSnapshot>>

    fun observeActivityEvents(
        projectId: String,
        callerRole: UserRole
    ): Flow<List<FinanceGovernanceActivityEvent>>
}
