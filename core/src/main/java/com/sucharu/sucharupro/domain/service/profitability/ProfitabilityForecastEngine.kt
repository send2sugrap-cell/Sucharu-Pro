package com.sucharu.sucharupro.domain.service.profitability

import com.sucharu.sucharupro.domain.model.profitability.*
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Historical Data Container for Forecast Computation.
 */
data class HistoricalProfitabilitySeries(
    val periods: List<String>,
    val revenues: List<BigDecimal>,
    val costs: List<BigDecimal>,
    val grossProfits: List<BigDecimal>,
    val units: List<Long>,
    val componentAverages: Map<JobCostComponentType, BigDecimal> = emptyMap(),
    val isReconciled: Boolean = true,
    val sourceReadiness: PeriodSourceReadiness = PeriodSourceReadiness.READY
)

data class ForecastComputationResult(
    val projectedRevenue: BigDecimal,
    val projectedTotalCost: BigDecimal,
    val projectedGrossProfit: BigDecimal,
    val projectedGrossMarginPercentage: BigDecimal?,
    val projectedContribution: BigDecimal,
    val projectedContributionMarginPercentage: BigDecimal?,
    val projectedUnits: Long,
    val components: List<ProfitabilityForecastComponent>,
    val breakEvenRevenue: BigDecimal?,
    val breakEvenUnits: Long?,
    val marginOfSafetyPercentage: BigDecimal?
)

/**
 * Core Forecasting Engine supporting 6 deterministic methods.
 * Module 16 Step 08.
 */
interface ProfitabilityForecastEngine {
    fun computeForecast(
        forecastId: String,
        tenantId: String,
        method: ProfitabilityForecastMethod,
        historicalSeries: HistoricalProfitabilitySeries,
        horizon: ForecastHorizon,
        scenario: ProfitabilityScenario?
    ): ForecastComputationResult
}

class ProfitabilityForecastEngineImpl : ProfitabilityForecastEngine {

    override fun computeForecast(
        forecastId: String,
        tenantId: String,
        method: ProfitabilityForecastMethod,
        historicalSeries: HistoricalProfitabilitySeries,
        horizon: ForecastHorizon,
        scenario: ProfitabilityScenario?
    ): ForecastComputationResult {
        val periodsAhead = horizon.periodCount

        // 1. Raw Baseline Projections based on Method
        val (rawRev, rawCost, rawUnits) = when (method) {
            ProfitabilityForecastMethod.HISTORICAL_BASELINE -> {
                val lastRev = historicalSeries.revenues.lastOrNull() ?: BigDecimal.ZERO
                val lastCost = historicalSeries.costs.lastOrNull() ?: BigDecimal.ZERO
                val lastUnits = historicalSeries.units.lastOrNull() ?: 0L
                Triple(
                    lastRev.multiply(BigDecimal(periodsAhead)),
                    lastCost.multiply(BigDecimal(periodsAhead)),
                    lastUnits * periodsAhead
                )
            }
            ProfitabilityForecastMethod.ROLLING_AVERAGE -> {
                val avgRev = ProfitabilityForecastMathUtils.calculateRollingAverage(historicalSeries.revenues)
                val avgCost = ProfitabilityForecastMathUtils.calculateRollingAverage(historicalSeries.costs)
                val avgUnits = if (historicalSeries.units.isNotEmpty()) historicalSeries.units.sum() / historicalSeries.units.size else 0L
                Triple(
                    avgRev.multiply(BigDecimal(periodsAhead)),
                    avgCost.multiply(BigDecimal(periodsAhead)),
                    avgUnits * periodsAhead
                )
            }
            ProfitabilityForecastMethod.WEIGHTED_ROLLING_AVERAGE -> {
                val count = historicalSeries.revenues.size
                val weights = if (count > 0) (1..count).map { BigDecimal(it) } else emptyList()
                val wAvgRev = ProfitabilityForecastMathUtils.calculateWeightedRollingAverage(historicalSeries.revenues, weights)
                val wAvgCost = ProfitabilityForecastMathUtils.calculateWeightedRollingAverage(historicalSeries.costs, weights)
                val avgUnits = if (historicalSeries.units.isNotEmpty()) historicalSeries.units.sum() / historicalSeries.units.size else 0L
                Triple(
                    wAvgRev.multiply(BigDecimal(periodsAhead)),
                    wAvgCost.multiply(BigDecimal(periodsAhead)),
                    avgUnits * periodsAhead
                )
            }
            ProfitabilityForecastMethod.TREND_BASED -> {
                val trendRev = ProfitabilityForecastMathUtils.calculateLinearTrendProjection(historicalSeries.revenues, periodsAhead)
                val trendCost = ProfitabilityForecastMathUtils.calculateLinearTrendProjection(historicalSeries.costs, periodsAhead)
                val avgUnits = if (historicalSeries.units.isNotEmpty()) historicalSeries.units.sum() / historicalSeries.units.size else 0L
                Triple(
                    trendRev.multiply(BigDecimal(periodsAhead)),
                    trendCost.multiply(BigDecimal(periodsAhead)),
                    avgUnits * periodsAhead
                )
            }
            ProfitabilityForecastMethod.DRIVER_BASED,
            ProfitabilityForecastMethod.SCENARIO_BASED -> {
                val avgRev = ProfitabilityForecastMathUtils.calculateRollingAverage(historicalSeries.revenues)
                val avgCost = ProfitabilityForecastMathUtils.calculateRollingAverage(historicalSeries.costs)
                val avgUnits = if (historicalSeries.units.isNotEmpty()) historicalSeries.units.sum() / historicalSeries.units.size else 0L
                Triple(
                    avgRev.multiply(BigDecimal(periodsAhead)),
                    avgCost.multiply(BigDecimal(periodsAhead)),
                    avgUnits * periodsAhead
                )
            }
        }

        // 2. Scenario & Assumption Transformation (if scenario supplied)
        val finalRev: BigDecimal
        val finalCost: BigDecimal
        val finalUnits: Long
        val components = mutableListOf<ProfitabilityForecastComponent>()

        // Normalize baseline cost components so they sum exactly to rawCost
        val componentBaseMap = mutableMapOf<JobCostComponentType, BigDecimal>()
        val knownSum = historicalSeries.componentAverages.values.fold(BigDecimal.ZERO) { a, b -> a.add(b) }
        if (knownSum.compareTo(BigDecimal.ZERO) > 0 && rawCost.compareTo(BigDecimal.ZERO) > 0) {
            val scaleFactor = rawCost.divide(knownSum, 6, RoundingMode.HALF_UP)
            JobCostComponentType.values().forEach { compType ->
                val base = historicalSeries.componentAverages[compType] ?: BigDecimal.ZERO
                componentBaseMap[compType] = base.multiply(scaleFactor).setScale(4, RoundingMode.HALF_UP)
            }
        } else {
            val oneTwelfth = ProfitabilityForecastMathUtils.safeDivide(rawCost, BigDecimal(12)) ?: BigDecimal.ZERO
            JobCostComponentType.values().forEach { compType ->
                componentBaseMap[compType] = oneTwelfth
            }
        }

        // Adjust residual rounding difference on the primary component
        val currentCompSum = componentBaseMap.values.fold(BigDecimal.ZERO) { a, b -> a.add(b) }
        val diff = rawCost.subtract(currentCompSum)
        if (diff.compareTo(BigDecimal.ZERO) != 0) {
            val primaryComp = JobCostComponentType.MATERIAL_COST
            componentBaseMap[primaryComp] = (componentBaseMap[primaryComp] ?: BigDecimal.ZERO).add(diff)
        }

        if (scenario != null) {
            val volAdj = scenario.volumeAdjustmentPercentage
            val revAdj = scenario.revenueAdjustmentPercentage

            finalUnits = if (volAdj.compareTo(BigDecimal.ZERO) != 0) {
                val mult = BigDecimal.ONE.add(volAdj.divide(BigDecimal("100.0000"), 4, RoundingMode.HALF_UP))
                (BigDecimal(rawUnits).multiply(mult)).toLong().coerceAtLeast(0L)
            } else rawUnits

            finalRev = ProfitabilityForecastMathUtils.applyAdjustment(rawRev, revAdj, null)

            var totalAdjustedCost = BigDecimal.ZERO
            JobCostComponentType.values().forEach { compType ->
                val baseComp = componentBaseMap[compType] ?: BigDecimal.ZERO
                val compAdj = when (compType) {
                    JobCostComponentType.MATERIAL_COST -> scenario.materialCostAdjustmentPercentage
                    JobCostComponentType.LABOUR_COST -> scenario.labourCostAdjustmentPercentage
                    JobCostComponentType.MACHINE_COST, JobCostComponentType.PRODUCTION_OPERATION_COST -> scenario.machineCostAdjustmentPercentage
                    JobCostComponentType.VENDOR_OUTSOURCE_COST -> scenario.vendorCostAdjustmentPercentage
                    JobCostComponentType.REWORK_COST -> scenario.reworkAdjustmentPercentage
                    JobCostComponentType.WASTAGE_COST -> scenario.wastageAdjustmentPercentage
                    else -> scenario.indirectCostAdjustmentPercentage
                }
                val adjComp = ProfitabilityForecastMathUtils.applyAdjustment(baseComp.multiply(BigDecimal(periodsAhead)), compAdj, null)
                totalAdjustedCost = totalAdjustedCost.add(adjComp)

                components.add(
                    ProfitabilityForecastComponent(
                        componentId = "comp-$compType-$forecastId",
                        forecastId = forecastId,
                        tenantId = tenantId,
                        componentType = compType,
                        projectedAmount = adjComp,
                        percentageOfTotalCost = BigDecimal.ZERO, // updated below
                        baselineAmount = baseComp.multiply(BigDecimal(periodsAhead)),
                        deltaAmount = adjComp.subtract(baseComp.multiply(BigDecimal(periodsAhead)))
                    )
                )
            }
            finalCost = totalAdjustedCost
        } else {
            finalRev = rawRev
            finalCost = rawCost
            finalUnits = rawUnits

            JobCostComponentType.values().forEach { compType ->
                val baseComp = componentBaseMap[compType] ?: BigDecimal.ZERO
                val projectedComp = baseComp.multiply(BigDecimal(periodsAhead))
                components.add(
                    ProfitabilityForecastComponent(
                        componentId = "comp-$compType-$forecastId",
                        forecastId = forecastId,
                        tenantId = tenantId,
                        componentType = compType,
                        projectedAmount = projectedComp,
                        percentageOfTotalCost = BigDecimal.ZERO,
                        baselineAmount = projectedComp,
                        deltaAmount = BigDecimal.ZERO
                    )
                )
            }
        }

        // Recompute component percentage shares
        val updatedComponents = components.map { c ->
            val share = if (finalCost.compareTo(BigDecimal.ZERO) > 0) {
                c.projectedAmount.multiply(BigDecimal("100.0000")).divide(finalCost, 4, RoundingMode.HALF_UP)
            } else BigDecimal.ZERO
            c.copy(percentageOfTotalCost = share)
        }

        val grossProfit = ProfitabilityForecastMathUtils.calculateGrossProfit(finalRev, finalCost)
        val grossMargin = ProfitabilityForecastMathUtils.calculateGrossMarginPercentage(finalRev, finalCost)
        val contribution = grossProfit // Contribution projection anchored to gross profit
        val contributionMargin = grossMargin

        // 3. Break-Even Projection
        val fixedCosts = updatedComponents.filter {
            it.componentType == JobCostComponentType.ALLOCATED_INDIRECT_COST ||
                    it.componentType == JobCostComponentType.OTHER_DIRECT_COST
        }.fold(BigDecimal.ZERO) { acc, c -> acc.add(c.projectedAmount) }

        val breakEvenRev = ProfitabilityForecastMathUtils.calculateBreakEvenRevenue(fixedCosts, grossMargin)
        val breakEvenUnits = if (breakEvenRev != null && finalUnits > 0 && finalRev.compareTo(BigDecimal.ZERO) > 0) {
            val avgPrice = finalRev.divide(BigDecimal(finalUnits), 4, RoundingMode.HALF_UP)
            if (avgPrice.compareTo(BigDecimal.ZERO) > 0) {
                breakEvenRev.divide(avgPrice, 0, RoundingMode.CEILING).toLong()
            } else null
        } else null

        val marginOfSafety = if (finalRev.compareTo(BigDecimal.ZERO) > 0 && breakEvenRev != null) {
            finalRev.subtract(breakEvenRev).multiply(BigDecimal("100.0000")).divide(finalRev, 4, RoundingMode.HALF_UP)
        } else null

        return ForecastComputationResult(
            projectedRevenue = ProfitabilityForecastMathUtils.scaleMoney(finalRev),
            projectedTotalCost = ProfitabilityForecastMathUtils.scaleMoney(finalCost),
            projectedGrossProfit = grossProfit,
            projectedGrossMarginPercentage = grossMargin,
            projectedContribution = contribution,
            projectedContributionMarginPercentage = contributionMargin,
            projectedUnits = finalUnits,
            components = updatedComponents,
            breakEvenRevenue = breakEvenRev?.let { ProfitabilityForecastMathUtils.scaleMoney(it) },
            breakEvenUnits = breakEvenUnits,
            marginOfSafetyPercentage = marginOfSafety
        )
    }
}
