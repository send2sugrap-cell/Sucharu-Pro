package com.sucharu.sucharupro.domain.service.profitability

import com.sucharu.sucharupro.domain.model.profitability.*

/**
 * Contract for ranking periods and evaluating profitability concentration across periods.
 * Module 16 Step 06.
 */
interface PeriodProfitabilityRankingService {
    fun rankPeriods(
        snapshots: List<PeriodProfitabilitySnapshot>,
        criteria: PeriodRankingCriteria = PeriodRankingCriteria.GROSS_PROFIT,
        ascending: Boolean = false
    ): List<PeriodRankingItem>

    fun analyzeConcentration(
        tenantId: String,
        projectId: String,
        periodType: PeriodType,
        scopeLabel: String,
        snapshots: List<PeriodProfitabilitySnapshot>
    ): PeriodConcentrationAnalysis
}
