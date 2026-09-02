package com.sucharu.sucharupro.domain.service.profitability

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.profitability.*

/**
 * Multi-source collector contract for Period Profitability.
 * Module 16 Step 06.
 */
interface PeriodProfitabilitySourceCollector {
    suspend fun collectPeriodData(
        tenantId: String,
        projectId: String,
        periodId: String,
        periodType: PeriodType,
        periodStart: Long,
        periodEnd: Long,
        customRevenueAttributions: List<PeriodRevenueAttributionItem>? = null,
        customCostBreakdown: List<PeriodCostBreakdownItem>? = null,
        customProvenance: List<PeriodProfitabilityProvenanceRecord>? = null
    ): DomainResult<PeriodSourceCollectionResult>
}
