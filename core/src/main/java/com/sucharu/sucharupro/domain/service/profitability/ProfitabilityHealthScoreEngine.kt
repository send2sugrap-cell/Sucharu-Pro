package com.sucharu.sucharupro.domain.service.profitability

import com.sucharu.sucharupro.domain.model.profitability.*
import java.math.BigDecimal

/**
 * Profitability Health Score Engine for calculating composite business health scores (0.0000 - 100.0000).
 * Module 16 Step 07.
 */
interface ProfitabilityHealthScoreEngine {
    fun calculateHealthScore(
        tenantId: String,
        periodId: String,
        overallMargin: BigDecimal?,
        trendDirection: PeriodTrendDirection,
        costVariancePct: BigDecimal?,
        revenueVariancePct: BigDecimal?,
        top1CustomerConcentration: BigDecimal,
        top1VendorConcentration: BigDecimal,
        hasIntegrityIssue: Boolean,
        hasDuplicates: Boolean
    ): ProfitabilityHealthScore
}

class ProfitabilityHealthScoreEngineImpl : ProfitabilityHealthScoreEngine {

    override fun calculateHealthScore(
        tenantId: String,
        periodId: String,
        overallMargin: BigDecimal?,
        trendDirection: PeriodTrendDirection,
        costVariancePct: BigDecimal?,
        revenueVariancePct: BigDecimal?,
        top1CustomerConcentration: BigDecimal,
        top1VendorConcentration: BigDecimal,
        hasIntegrityIssue: Boolean,
        hasDuplicates: Boolean
    ): ProfitabilityHealthScore {
        val result = ProfitabilityIntelligenceMathUtils.calculateHealthScore(
            marginPercentage = overallMargin,
            trendDirection = trendDirection,
            costVariancePct = costVariancePct,
            revenueVariancePct = revenueVariancePct,
            top1CustomerConcentration = top1CustomerConcentration,
            top1VendorConcentration = top1VendorConcentration,
            hasIntegrityIssue = hasIntegrityIssue,
            hasDuplicates = hasDuplicates
        )

        return ProfitabilityHealthScore(
            scoreId = "health-score-$periodId",
            snapshotId = "",
            tenantId = tenantId,
            periodId = periodId,
            overallScore = result.overallScore,
            marginScore = result.marginScore,
            trendScore = result.trendScore,
            costStabilityScore = result.costStabilityScore,
            revenueStabilityScore = result.revenueStabilityScore,
            concentrationScore = result.concentrationScore,
            vendorDependencyScore = result.vendorDependencyScore,
            dataIntegrityScore = result.dataIntegrityScore,
            attributionCompletenessScore = result.attributionCompletenessScore,
            healthLevel = result.healthLevel,
            explanation = result.explanation
        )
    }
}
