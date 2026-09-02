package com.sucharu.sucharupro.domain.service.profitability

import com.sucharu.sucharupro.domain.model.profitability.*
import java.math.BigDecimal

data class ConfidenceAndRiskEvaluation(
    val confidenceScore: BigDecimal,
    val confidenceLevel: ForecastConfidenceLevel,
    val riskLevel: ForecastRiskLevel
)

/**
 * Confidence and Forward-Looking Risk Evaluation Engine.
 * Module 16 Step 08.
 */
interface ProfitabilityForecastConfidenceAndRiskEngine {
    fun evaluate(
        historicalSeries: HistoricalProfitabilitySeries,
        computation: ForecastComputationResult,
        baselineRevenue: BigDecimal?,
        baselineCost: BigDecimal?
    ): ConfidenceAndRiskEvaluation
}

class ProfitabilityForecastConfidenceAndRiskEngineImpl : ProfitabilityForecastConfidenceAndRiskEngine {

    override fun evaluate(
        historicalSeries: HistoricalProfitabilitySeries,
        computation: ForecastComputationResult,
        baselineRevenue: BigDecimal?,
        baselineCost: BigDecimal?
    ): ConfidenceAndRiskEvaluation {
        val periodCount = historicalSeries.periods.size
        val isReconciled = historicalSeries.isReconciled
        val sourceReadiness = historicalSeries.sourceReadiness

        // Volatility Estimation
        val volatility = if (historicalSeries.revenues.size >= 2) {
            val avg = ProfitabilityForecastMathUtils.calculateRollingAverage(historicalSeries.revenues)
            if (avg.compareTo(BigDecimal.ZERO) > 0) {
                val maxDiff = historicalSeries.revenues.map { it.subtract(avg).abs() }.maxOrNull() ?: BigDecimal.ZERO
                ProfitabilityForecastMathUtils.safeDivide(maxDiff.multiply(BigDecimal("100.0000")), avg)
            } else null
        } else null

        val confidenceScore = ProfitabilityForecastMathUtils.calculateConfidenceScore(
            historicalPeriodCount = periodCount,
            isFullyReconciled = isReconciled,
            sourceReadiness = sourceReadiness,
            volatilityPercentage = volatility
        )

        val confidenceLevel = ProfitabilityForecastMathUtils.classifyConfidenceLevel(confidenceScore)

        val marginDelta = if (computation.projectedGrossMarginPercentage != null && baselineRevenue != null && baselineCost != null) {
            val baseMargin = ProfitabilityForecastMathUtils.calculateGrossMarginPercentage(baselineRevenue, baselineCost)
            if (baseMargin != null) computation.projectedGrossMarginPercentage.subtract(baseMargin) else null
        } else null

        val costGrowth = if (baselineCost != null) {
            ProfitabilityForecastMathUtils.calculatePercentageChange(baselineCost, computation.projectedTotalCost)
        } else null

        val riskLevel = ProfitabilityForecastMathUtils.classifyRiskLevel(
            projectedGrossProfit = computation.projectedGrossProfit,
            projectedMargin = computation.projectedGrossMarginPercentage,
            marginDelta = marginDelta,
            costGrowthPercentage = costGrowth
        )

        return ConfidenceAndRiskEvaluation(
            confidenceScore = confidenceScore,
            confidenceLevel = confidenceLevel,
            riskLevel = riskLevel
        )
    }
}
