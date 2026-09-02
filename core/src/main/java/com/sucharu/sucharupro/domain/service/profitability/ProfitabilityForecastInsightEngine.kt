package com.sucharu.sucharupro.domain.service.profitability

import com.sucharu.sucharupro.domain.model.profitability.*
import java.math.BigDecimal

/**
 * Forward-Looking Management Insight Engine.
 * Produces structured actionable insights from forecast computations.
 * Module 16 Step 08.
 */
interface ProfitabilityForecastInsightEngine {
    fun generateInsights(
        snapshot: ProfitabilityForecastSnapshot
    ): List<ForecastManagementInsight>
}

class ProfitabilityForecastInsightEngineImpl : ProfitabilityForecastInsightEngine {

    override fun generateInsights(snapshot: ProfitabilityForecastSnapshot): List<ForecastManagementInsight> {
        val insights = mutableListOf<ForecastManagementInsight>()
        val tenantId = snapshot.tenantId
        val forecastId = snapshot.forecastId
        val scope = snapshot.targetScope
        val entityId = snapshot.targetEntityId
        val entityLabel = snapshot.targetEntityLabel

        // 1. Negative Profit Projection
        if (snapshot.projectedGrossProfit.compareTo(BigDecimal.ZERO) < 0) {
            insights.add(
                ForecastManagementInsight(
                    insightId = "ins-neg-profit-$forecastId",
                    forecastId = forecastId,
                    tenantId = tenantId,
                    insightType = ForecastInsightType.NEGATIVE_PROFIT_FORECAST,
                    severity = ForecastInsightSeverity.CRITICAL,
                    dimensionType = scope,
                    targetEntityId = entityId,
                    targetEntityLabel = entityLabel,
                    title = "Projected Loss Making Performance",
                    explanation = "$entityLabel is projected to operate at a loss of ${snapshot.projectedGrossProfit.abs()} over ${snapshot.horizon.label}.",
                    financialImpact = snapshot.projectedGrossProfit.abs(),
                    supportingSourceReferences = listOf("PROJECTED_COST=${snapshot.projectedTotalCost}", "PROJECTED_REV=${snapshot.projectedRevenue}"),
                    recommendedActionCode = "HALT_OR_REPRICE_COMMERCIAL_OFFERING"
                )
            )
        }

        // 2. Projected Margin Decline vs Baseline
        if (snapshot.projectedMarginDeltaPercentage != null && snapshot.projectedMarginDeltaPercentage.compareTo(BigDecimal("-5.0000")) < 0) {
            val impact = if (snapshot.baselineGrossProfit != null) {
                snapshot.baselineGrossProfit.subtract(snapshot.projectedGrossProfit).coerceAtLeast(BigDecimal.ZERO)
            } else BigDecimal.ZERO

            insights.add(
                ForecastManagementInsight(
                    insightId = "ins-margin-decline-$forecastId",
                    forecastId = forecastId,
                    tenantId = tenantId,
                    insightType = ForecastInsightType.PROJECTED_MARGIN_DECLINE,
                    severity = ForecastInsightSeverity.WARNING,
                    dimensionType = scope,
                    targetEntityId = entityId,
                    targetEntityLabel = entityLabel,
                    title = "Projected Margin Erosion",
                    explanation = "Gross margin is projected to contract by ${snapshot.projectedMarginDeltaPercentage.abs()}% compared to historical baseline.",
                    financialImpact = impact,
                    supportingSourceReferences = listOf("BASELINE_MARGIN=${snapshot.baselineGrossMarginPercentage}", "PROJECTED_MARGIN=${snapshot.projectedGrossMarginPercentage}"),
                    recommendedActionCode = "AUDIT_COST_DRIVERS_AND_ADJUST_PRICING"
                )
            )
        }

        // 3. Material Cost Pressure
        val materialComp = snapshot.components.find {
            it.componentType == JobCostComponentType.MATERIAL_COST
        }
        if (materialComp != null && materialComp.percentageOfTotalCost.compareTo(BigDecimal("40.0000")) > 0) {
            insights.add(
                ForecastManagementInsight(
                    insightId = "ins-material-pressure-$forecastId",
                    forecastId = forecastId,
                    tenantId = tenantId,
                    insightType = ForecastInsightType.MATERIAL_COST_PRESSURE,
                    severity = ForecastInsightSeverity.WARNING,
                    dimensionType = scope,
                    targetEntityId = entityId,
                    targetEntityLabel = entityLabel,
                    title = "Heavy Material Cost Concentration",
                    explanation = "Direct material cost represents ${materialComp.percentageOfTotalCost}% of total projected cost.",
                    financialImpact = materialComp.projectedAmount,
                    supportingSourceReferences = listOf("MATERIAL_COST=${materialComp.projectedAmount}"),
                    recommendedActionCode = "LOCK_IN_BULK_PAPER_PROCUREMENT_RATES"
                )
            )
        }

        // 4. Break-Even Warning
        if (snapshot.breakEvenRevenue != null && snapshot.projectedRevenue.compareTo(BigDecimal.ZERO) > 0) {
            val dist = snapshot.projectedRevenue.subtract(snapshot.breakEvenRevenue)
            if (dist.compareTo(BigDecimal.ZERO) < 0) {
                insights.add(
                    ForecastManagementInsight(
                        insightId = "ins-breakeven-risk-$forecastId",
                        forecastId = forecastId,
                        tenantId = tenantId,
                        insightType = ForecastInsightType.BREAK_EVEN_RISK,
                        severity = ForecastInsightSeverity.CRITICAL,
                        dimensionType = scope,
                        targetEntityId = entityId,
                        targetEntityLabel = entityLabel,
                        title = "Projected Revenue Below Break-Even Point",
                        explanation = "Projected revenue of ${snapshot.projectedRevenue} is below the estimated break-even threshold of ${snapshot.breakEvenRevenue}.",
                        financialImpact = dist.abs(),
                        supportingSourceReferences = listOf("BREAK_EVEN_REV=${snapshot.breakEvenRevenue}", "PROJECTED_REV=${snapshot.projectedRevenue}"),
                        recommendedActionCode = "INCREASE_SALES_VOLUME_OR_REDUCE_FIXED_OVERHEAD"
                    )
                )
            }
        }

        // 5. Positive Profitability Improvement
        if (snapshot.projectedProfitDelta != null && snapshot.projectedProfitDelta.compareTo(BigDecimal.ZERO) > 0) {
            insights.add(
                ForecastManagementInsight(
                    insightId = "ins-profit-improvement-$forecastId",
                    forecastId = forecastId,
                    tenantId = tenantId,
                    insightType = ForecastInsightType.PROFITABILITY_IMPROVEMENT,
                    severity = ForecastInsightSeverity.INFO,
                    dimensionType = scope,
                    targetEntityId = entityId,
                    targetEntityLabel = entityLabel,
                    title = "Projected Profitability Expansion",
                    explanation = "Gross profit is forecasted to improve by ${snapshot.projectedProfitDelta} over ${snapshot.horizon.label}.",
                    financialImpact = snapshot.projectedProfitDelta,
                    supportingSourceReferences = listOf("PROJECTED_DELTA=${snapshot.projectedProfitDelta}"),
                    recommendedActionCode = "SCALE_COMMERCIAL_PIPELINE"
                )
            )
        }

        return insights
    }
}
