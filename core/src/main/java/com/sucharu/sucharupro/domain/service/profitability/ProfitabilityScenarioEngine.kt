package com.sucharu.sucharupro.domain.service.profitability

import com.sucharu.sucharupro.domain.model.profitability.*
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Scenario Modelling & What-If Analysis Engine.
 * Module 16 Step 08.
 */
interface ProfitabilityScenarioEngine {
    fun generateStandardScenarios(tenantId: String, projectId: String, scope: ProfitabilityForecastScope): List<ProfitabilityScenario>

    fun compareScenarios(
        tenantId: String,
        projectId: String,
        baselineForecast: ProfitabilityForecastSnapshot,
        scenarios: List<ProfitabilityScenario>,
        historicalSeries: HistoricalProfitabilitySeries,
        forecastEngine: ProfitabilityForecastEngine
    ): ProfitabilityScenarioComparison
}

class ProfitabilityScenarioEngineImpl : ProfitabilityScenarioEngine {

    override fun generateStandardScenarios(tenantId: String, projectId: String, scope: ProfitabilityForecastScope): List<ProfitabilityScenario> {
        val baseline = ProfitabilityScenario(
            scenarioId = "scen-baseline-$scope",
            tenantId = tenantId,
            projectId = projectId,
            scenarioName = "Baseline Scenario",
            scenarioType = ProfitabilityScenarioType.BASELINE,
            description = "Unmodified historical trajectory based on standard business baseline.",
            targetScope = scope,
            revenueAdjustmentPercentage = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP),
            volumeAdjustmentPercentage = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP),
            materialCostAdjustmentPercentage = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP),
            labourCostAdjustmentPercentage = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP),
            machineCostAdjustmentPercentage = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP),
            vendorCostAdjustmentPercentage = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP),
            reworkAdjustmentPercentage = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP),
            wastageAdjustmentPercentage = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP),
            indirectCostAdjustmentPercentage = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP),
            isDefault = true
        )

        val optimistic = ProfitabilityScenario(
            scenarioId = "scen-optimistic-$scope",
            tenantId = tenantId,
            projectId = projectId,
            scenarioName = "Optimistic Growth Scenario",
            scenarioType = ProfitabilityScenarioType.OPTIMISTIC,
            description = "Revenue +10%, Volume +15%, Material Cost -5%, Rework & Wastage -10%.",
            targetScope = scope,
            revenueAdjustmentPercentage = BigDecimal("10.0000"),
            volumeAdjustmentPercentage = BigDecimal("15.0000"),
            materialCostAdjustmentPercentage = BigDecimal("-5.0000"),
            labourCostAdjustmentPercentage = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP),
            machineCostAdjustmentPercentage = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP),
            vendorCostAdjustmentPercentage = BigDecimal("-5.0000"),
            reworkAdjustmentPercentage = BigDecimal("-10.0000"),
            wastageAdjustmentPercentage = BigDecimal("-10.0000"),
            indirectCostAdjustmentPercentage = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP),
            isDefault = false
        )

        val pessimistic = ProfitabilityScenario(
            scenarioId = "scen-pessimistic-$scope",
            tenantId = tenantId,
            projectId = projectId,
            scenarioName = "Pessimistic Stress Scenario",
            scenarioType = ProfitabilityScenarioType.PESSIMISTIC,
            description = "Revenue -15%, Material Cost +15%, Vendor Cost +10%, Rework & Wastage +20%.",
            targetScope = scope,
            revenueAdjustmentPercentage = BigDecimal("-15.0000"),
            volumeAdjustmentPercentage = BigDecimal("-10.0000"),
            materialCostAdjustmentPercentage = BigDecimal("15.0000"),
            labourCostAdjustmentPercentage = BigDecimal("10.0000"),
            machineCostAdjustmentPercentage = BigDecimal("5.0000"),
            vendorCostAdjustmentPercentage = BigDecimal("10.0000"),
            reworkAdjustmentPercentage = BigDecimal("20.0000"),
            wastageAdjustmentPercentage = BigDecimal("20.0000"),
            indirectCostAdjustmentPercentage = BigDecimal("5.0000"),
            isDefault = false
        )

        return listOf(baseline, optimistic, pessimistic)
    }

    override fun compareScenarios(
        tenantId: String,
        projectId: String,
        baselineForecast: ProfitabilityForecastSnapshot,
        scenarios: List<ProfitabilityScenario>,
        historicalSeries: HistoricalProfitabilitySeries,
        forecastEngine: ProfitabilityForecastEngine
    ): ProfitabilityScenarioComparison {
        val baseRev = baselineForecast.projectedRevenue
        val baseCost = baselineForecast.projectedTotalCost
        val baseProfit = baselineForecast.projectedGrossProfit
        val baseMargin = baselineForecast.projectedGrossMarginPercentage

        val baselineItem = ProfitabilityScenarioComparisonItem(
            scenarioId = baselineForecast.scenarioId ?: "baseline",
            scenarioName = "Baseline",
            scenarioType = ProfitabilityScenarioType.BASELINE,
            projectedRevenue = baseRev,
            projectedTotalCost = baseCost,
            projectedGrossProfit = baseProfit,
            projectedGrossMarginPercentage = baseMargin,
            projectedContribution = baselineForecast.projectedContribution,
            projectedContributionMarginPercentage = baselineForecast.projectedContributionMarginPercentage,
            projectedUnits = baselineForecast.projectedUnits,
            revenueDeltaFromBaseline = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP),
            costDeltaFromBaseline = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP),
            profitDeltaFromBaseline = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP),
            marginDeltaFromBaselinePercentage = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP),
            financialImpact = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP),
            riskLevel = baselineForecast.riskLevel
        )

        val comparedList = scenarios.map { sc ->
            val compResult = forecastEngine.computeForecast(
                forecastId = "sim-${sc.scenarioId}",
                tenantId = tenantId,
                method = baselineForecast.forecastMethod,
                historicalSeries = historicalSeries,
                horizon = baselineForecast.horizon,
                scenario = sc
            )

            val revDelta = compResult.projectedRevenue.subtract(baseRev)
            val costDelta = compResult.projectedTotalCost.subtract(baseCost)
            val profitDelta = compResult.projectedGrossProfit.subtract(baseProfit)
            val marginDelta = if (compResult.projectedGrossMarginPercentage != null && baseMargin != null) {
                compResult.projectedGrossMarginPercentage.subtract(baseMargin)
            } else null

            val risk = ProfitabilityForecastMathUtils.classifyRiskLevel(
                projectedGrossProfit = compResult.projectedGrossProfit,
                projectedMargin = compResult.projectedGrossMarginPercentage,
                marginDelta = marginDelta,
                costGrowthPercentage = ProfitabilityForecastMathUtils.calculatePercentageChange(baseCost, compResult.projectedTotalCost)
            )

            ProfitabilityScenarioComparisonItem(
                scenarioId = sc.scenarioId,
                scenarioName = sc.scenarioName,
                scenarioType = sc.scenarioType,
                projectedRevenue = compResult.projectedRevenue,
                projectedTotalCost = compResult.projectedTotalCost,
                projectedGrossProfit = compResult.projectedGrossProfit,
                projectedGrossMarginPercentage = compResult.projectedGrossMarginPercentage,
                projectedContribution = compResult.projectedContribution,
                projectedContributionMarginPercentage = compResult.projectedContributionMarginPercentage,
                projectedUnits = compResult.projectedUnits,
                revenueDeltaFromBaseline = revDelta,
                costDeltaFromBaseline = costDelta,
                profitDeltaFromBaseline = profitDelta,
                marginDeltaFromBaselinePercentage = marginDelta,
                financialImpact = profitDelta,
                riskLevel = risk
            )
        }

        return ProfitabilityScenarioComparison(
            comparisonId = "scen-comp-${System.currentTimeMillis()}-${baselineForecast.forecastId}",
            tenantId = tenantId,
            projectId = projectId,
            baselineForecastId = baselineForecast.forecastId,
            targetScope = baselineForecast.targetScope,
            targetEntityId = baselineForecast.targetEntityId,
            horizon = baselineForecast.horizon,
            baselineScenario = baselineItem,
            comparedScenarios = comparedList
        )
    }
}
