package com.sucharu.sucharupro.domain.service.profitability

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.profitability.*
import java.math.BigDecimal

/**
 * Service contract for Period Profitability, Financial Trends & Business Performance.
 * Module 16 Step 06.
 */
interface PeriodProfitabilityService {
    suspend fun calculatePeriodProfitability(
        tenantId: String,
        projectId: String,
        periodId: String,
        periodType: PeriodType,
        periodStart: Long,
        periodEnd: Long,
        timezone: String = "Asia/Dhaka",
        periodKey: String = "",
        fiscalPeriodId: String? = null,
        customBaselineRevenue: BigDecimal? = null,
        customBaselineCost: BigDecimal? = null,
        customRevenueAttributions: List<PeriodRevenueAttributionItem>? = null,
        customCostBreakdown: List<PeriodCostBreakdownItem>? = null,
        customProvenance: List<PeriodProfitabilityProvenanceRecord>? = null,
        idempotencyKey: String? = null,
        actorId: String = "SYSTEM",
        actorRole: String = "SYSTEM"
    ): DomainResult<PeriodProfitabilitySnapshot>

    suspend fun getLatestSnapshot(tenantId: String, periodId: String): DomainResult<PeriodProfitabilitySnapshot?>
    suspend fun getSnapshotById(tenantId: String, snapshotId: String): DomainResult<PeriodProfitabilitySnapshot?>
    suspend fun listSnapshots(tenantId: String, filter: PeriodProfitabilityFilter): DomainResult<List<PeriodProfitabilitySnapshot>>
    suspend fun getCostBreakdown(tenantId: String, periodId: String): DomainResult<List<PeriodCostBreakdownItem>>
    suspend fun getRevenueBreakdown(tenantId: String, periodId: String): DomainResult<List<PeriodRevenueAttributionItem>>
    suspend fun getProvenance(tenantId: String, periodId: String): DomainResult<List<PeriodProfitabilityProvenanceRecord>>
    suspend fun reconcile(tenantId: String, projectId: String, periodId: String, snapshotId: String? = null): DomainResult<PeriodProfitabilityReconciliationEvent>
    suspend fun comparePeriods(tenantId: String, currentPeriodId: String, previousPeriodId: String): DomainResult<PeriodComparisonResult>
    suspend fun rankPeriods(tenantId: String, criteria: PeriodRankingCriteria, periodType: PeriodType? = null): DomainResult<List<PeriodRankingItem>>
    suspend fun analyzeConcentration(tenantId: String, projectId: String, periodType: PeriodType, scopeLabel: String): DomainResult<PeriodConcentrationAnalysis>
    suspend fun listAuditEvents(tenantId: String, periodId: String): DomainResult<List<PeriodProfitabilityAuditEvent>>
    suspend fun listUnattributedItems(tenantId: String, periodId: String?): DomainResult<List<PeriodUnattributedItem>>
}
