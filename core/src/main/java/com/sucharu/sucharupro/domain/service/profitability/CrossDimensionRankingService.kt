package com.sucharu.sucharupro.domain.service.profitability

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.profitability.*
import java.math.BigDecimal

/**
 * Cross-Dimensional Ranking, Trend, and Concentration Analytics Service.
 * Module 16 Step 07.
 */
interface CrossDimensionRankingService {
    suspend fun rankEntities(
        tenantId: String,
        periodId: String,
        criteria: CrossDimensionRankingCriteria,
        dimensionType: ProfitabilityDimensionType?,
        dimensions: List<DimensionInsight>
    ): DomainResult<CrossDimensionRankingResult>

    suspend fun analyzeConcentration(
        tenantId: String,
        periodId: String,
        dimensionType: ProfitabilityDimensionType,
        dimensions: List<DimensionInsight>
    ): DomainResult<CrossDimensionConcentrationResult>

    suspend fun compareTrends(
        tenantId: String,
        currentPeriodId: String,
        previousPeriodId: String,
        currentSnapshot: ProfitabilityIntelligenceSnapshot?,
        previousSnapshot: ProfitabilityIntelligenceSnapshot?
    ): DomainResult<CrossDimensionTrendResult>
}

class CrossDimensionRankingServiceImpl : CrossDimensionRankingService {

    override suspend fun rankEntities(
        tenantId: String,
        periodId: String,
        criteria: CrossDimensionRankingCriteria,
        dimensionType: ProfitabilityDimensionType?,
        dimensions: List<DimensionInsight>
    ): DomainResult<CrossDimensionRankingResult> {
        val filtered = if (dimensionType != null) {
            dimensions.filter { it.dimensionType == dimensionType }
        } else {
            dimensions
        }

        val sorted = when (criteria) {
            CrossDimensionRankingCriteria.MOST_PROFITABLE -> {
                filtered.sortedWith(
                    compareByDescending<DimensionInsight> { it.grossProfit }
                        .thenByDescending { it.revenue }
                        .thenBy { it.dimensionId }
                )
            }
            CrossDimensionRankingCriteria.HIGHEST_MARGIN -> {
                filtered.sortedWith(
                    compareByDescending<DimensionInsight> { it.margin ?: BigDecimal.ZERO }
                        .thenByDescending { it.grossProfit }
                        .thenBy { it.dimensionId }
                )
            }
            CrossDimensionRankingCriteria.HIGHEST_REVENUE -> {
                filtered.sortedWith(
                    compareByDescending<DimensionInsight> { it.revenue }
                        .thenByDescending { it.grossProfit }
                        .thenBy { it.dimensionId }
                )
            }
            CrossDimensionRankingCriteria.HIGHEST_COST -> {
                filtered.sortedWith(
                    compareByDescending<DimensionInsight> { it.cost }
                        .thenBy { it.grossProfit }
                        .thenBy { it.dimensionId }
                )
            }
            CrossDimensionRankingCriteria.LOWEST_MARGIN -> {
                filtered.sortedWith(
                    compareBy<DimensionInsight> { it.margin ?: BigDecimal.ZERO }
                        .thenBy { it.grossProfit }
                        .thenBy { it.dimensionId }
                )
            }
            CrossDimensionRankingCriteria.HIGHEST_RISK -> {
                filtered.sortedWith(
                    compareByDescending<DimensionInsight> { it.riskLevel.ordinal }
                        .thenBy { it.grossProfit }
                        .thenBy { it.dimensionId }
                )
            }
            CrossDimensionRankingCriteria.BIGGEST_POSITIVE_DRIVER -> {
                filtered.filter { it.grossProfit.compareTo(BigDecimal.ZERO) > 0 }.sortedWith(
                    compareByDescending<DimensionInsight> { it.grossProfit }
                        .thenByDescending { it.margin ?: BigDecimal.ZERO }
                        .thenBy { it.dimensionId }
                )
            }
            CrossDimensionRankingCriteria.BIGGEST_NEGATIVE_DRIVER -> {
                filtered.sortedWith(
                    compareBy<DimensionInsight> { it.grossProfit }
                        .thenByDescending { it.cost }
                        .thenBy { it.dimensionId }
                )
            }
            else -> {
                filtered.sortedWith(
                    compareByDescending<DimensionInsight> { it.grossProfit }
                        .thenByDescending { it.revenue }
                        .thenBy { it.dimensionId }
                )
            }
        }

        val rankedItems = sorted.mapIndexed { index, item ->
            val metricVal = when (criteria) {
                CrossDimensionRankingCriteria.MOST_PROFITABLE,
                CrossDimensionRankingCriteria.BIGGEST_POSITIVE_DRIVER,
                CrossDimensionRankingCriteria.BIGGEST_NEGATIVE_DRIVER -> item.grossProfit
                CrossDimensionRankingCriteria.HIGHEST_MARGIN,
                CrossDimensionRankingCriteria.LOWEST_MARGIN -> item.margin ?: BigDecimal.ZERO
                CrossDimensionRankingCriteria.HIGHEST_REVENUE -> item.revenue
                CrossDimensionRankingCriteria.HIGHEST_COST -> item.cost
                CrossDimensionRankingCriteria.HIGHEST_RISK -> BigDecimal(item.riskLevel.ordinal)
                else -> item.grossProfit
            }

            CrossDimensionRankingItem(
                rank = index + 1,
                dimensionType = item.dimensionType,
                entityId = item.dimensionId,
                entityLabel = item.dimensionLabel,
                metricValue = metricVal,
                metricLabel = criteria.name,
                revenue = item.revenue,
                cost = item.cost,
                grossProfit = item.grossProfit,
                margin = item.margin,
                riskLevel = item.riskLevel,
                trend = item.trendDirection
            )
        }

        return DomainResult.Success(
            CrossDimensionRankingResult(
                tenantId = tenantId,
                periodId = periodId,
                criteria = criteria,
                dimensionType = dimensionType,
                rankedItems = rankedItems
            )
        )
    }

    override suspend fun analyzeConcentration(
        tenantId: String,
        periodId: String,
        dimensionType: ProfitabilityDimensionType,
        dimensions: List<DimensionInsight>
    ): DomainResult<CrossDimensionConcentrationResult> {
        val filtered = dimensions.filter { it.dimensionType == dimensionType }
        if (filtered.isEmpty()) {
            return DomainResult.Success(
                CrossDimensionConcentrationResult(
                    tenantId = tenantId,
                    periodId = periodId,
                    dimensionType = dimensionType,
                    totalRevenue = BigDecimal.ZERO,
                    totalProfit = BigDecimal.ZERO,
                    totalCost = BigDecimal.ZERO,
                    totalEntities = 0,
                    top1Share = BigDecimal.ZERO,
                    top5Share = BigDecimal.ZERO,
                    top10Share = BigDecimal.ZERO,
                    dependencyLevel = ProfitabilityDependencyLevel.INSUFFICIENT_DATA,
                    topEntities = emptyList()
                )
            )
        }

        val totalRev = filtered.fold(BigDecimal.ZERO) { acc, it -> acc.add(it.revenue) }
        val totalProfit = filtered.fold(BigDecimal.ZERO) { acc, it -> acc.add(it.grossProfit) }
        val totalCost = filtered.fold(BigDecimal.ZERO) { acc, it -> acc.add(it.cost) }

        val sortedByRev = filtered.sortedWith(
            compareByDescending<DimensionInsight> { it.revenue }
                .thenBy { it.dimensionId }
        )

        val top1Rev = sortedByRev.firstOrNull()?.revenue ?: BigDecimal.ZERO
        val top5Rev = sortedByRev.take(5).fold(BigDecimal.ZERO) { acc, it -> acc.add(it.revenue) }
        val top10Rev = sortedByRev.take(10).fold(BigDecimal.ZERO) { acc, it -> acc.add(it.revenue) }

        val top1Share = ProfitabilityIntelligenceMathUtils.calculateSharePercentage(top1Rev, totalRev)
        val top5Share = ProfitabilityIntelligenceMathUtils.calculateSharePercentage(top5Rev, totalRev)
        val top10Share = ProfitabilityIntelligenceMathUtils.calculateSharePercentage(top10Rev, totalRev)

        val dependency = when {
            top1Share.compareTo(BigDecimal("50.0000")) >= 0 -> ProfitabilityDependencyLevel.CRITICAL_DEPENDENCY
            top1Share.compareTo(BigDecimal("30.0000")) >= 0 || top5Share.compareTo(BigDecimal("70.0000")) >= 0 -> ProfitabilityDependencyLevel.HIGH_DEPENDENCY
            top1Share.compareTo(BigDecimal("15.0000")) >= 0 || top5Share.compareTo(BigDecimal("40.0000")) >= 0 -> ProfitabilityDependencyLevel.MODERATE_DEPENDENCY
            else -> ProfitabilityDependencyLevel.LOW_DEPENDENCY
        }

        val topEntities = sortedByRev.take(10).mapIndexed { idx, it ->
            CrossDimensionConcentrationEntity(
                rank = idx + 1,
                entityId = it.dimensionId,
                entityLabel = it.dimensionLabel,
                amount = it.revenue,
                sharePercentage = ProfitabilityIntelligenceMathUtils.calculateSharePercentage(it.revenue, totalRev)
            )
        }

        return DomainResult.Success(
            CrossDimensionConcentrationResult(
                tenantId = tenantId,
                periodId = periodId,
                dimensionType = dimensionType,
                totalRevenue = ProfitabilityIntelligenceMathUtils.scaleMoney(totalRev),
                totalProfit = ProfitabilityIntelligenceMathUtils.scaleMoney(totalProfit),
                totalCost = ProfitabilityIntelligenceMathUtils.scaleMoney(totalCost),
                totalEntities = filtered.size,
                top1Share = top1Share,
                top5Share = top5Share,
                top10Share = top10Share,
                dependencyLevel = dependency,
                topEntities = topEntities
            )
        )
    }

    override suspend fun compareTrends(
        tenantId: String,
        currentPeriodId: String,
        previousPeriodId: String,
        currentSnapshot: ProfitabilityIntelligenceSnapshot?,
        previousSnapshot: ProfitabilityIntelligenceSnapshot?
    ): DomainResult<CrossDimensionTrendResult> {
        if (currentSnapshot == null || previousSnapshot == null) {
            return DomainResult.Success(
                CrossDimensionTrendResult(
                    tenantId = tenantId,
                    currentPeriodId = currentPeriodId,
                    previousPeriodId = previousPeriodId,
                    currentRevenue = currentSnapshot?.revenue ?: BigDecimal.ZERO,
                    previousRevenue = previousSnapshot?.revenue ?: BigDecimal.ZERO,
                    revenueDelta = BigDecimal.ZERO,
                    revenueDeltaPct = null,
                    currentCost = currentSnapshot?.totalCost ?: BigDecimal.ZERO,
                    previousCost = previousSnapshot?.totalCost ?: BigDecimal.ZERO,
                    costDelta = BigDecimal.ZERO,
                    costDeltaPct = null,
                    currentProfit = currentSnapshot?.grossProfit ?: BigDecimal.ZERO,
                    previousProfit = previousSnapshot?.grossProfit ?: BigDecimal.ZERO,
                    profitDelta = BigDecimal.ZERO,
                    profitDeltaPct = null,
                    currentMargin = currentSnapshot?.grossMargin,
                    previousMargin = previousSnapshot?.grossMargin,
                    marginDelta = null,
                    trendDirection = PeriodTrendDirection.INSUFFICIENT_DATA,
                    explanation = "Insufficient snapshot data to evaluate cross-period trends."
                )
            )
        }

        val revDelta = currentSnapshot.revenue.subtract(previousSnapshot.revenue)
        val revDeltaPct = ProfitabilityIntelligenceMathUtils.calculatePercentageChange(currentSnapshot.revenue, previousSnapshot.revenue)

        val costDelta = currentSnapshot.totalCost.subtract(previousSnapshot.totalCost)
        val costDeltaPct = ProfitabilityIntelligenceMathUtils.calculatePercentageChange(currentSnapshot.totalCost, previousSnapshot.totalCost)

        val profitDelta = currentSnapshot.grossProfit.subtract(previousSnapshot.grossProfit)
        val profitDeltaPct = ProfitabilityIntelligenceMathUtils.calculatePercentageChange(currentSnapshot.grossProfit, previousSnapshot.grossProfit)

        val marginDelta = if (currentSnapshot.grossMargin != null && previousSnapshot.grossMargin != null) {
            currentSnapshot.grossMargin.subtract(previousSnapshot.grossMargin)
        } else null

        val trendDirection = when {
            profitDeltaPct != null && profitDeltaPct.compareTo(BigDecimal("15.0000")) >= 0 -> PeriodTrendDirection.STRONGLY_IMPROVING
            profitDeltaPct != null && profitDeltaPct.compareTo(BigDecimal("5.0000")) >= 0 -> PeriodTrendDirection.IMPROVING
            profitDeltaPct != null && profitDeltaPct.compareTo(BigDecimal("-15.0000")) <= 0 -> PeriodTrendDirection.STRONGLY_DECLINING
            profitDeltaPct != null && profitDeltaPct.compareTo(BigDecimal("-5.0000")) <= 0 -> PeriodTrendDirection.DECLINING
            else -> PeriodTrendDirection.STABLE
        }

        val explanation = "Revenue moved by $revDelta ($revDeltaPct%), Total Cost moved by $costDelta ($costDeltaPct%), Net Profit moved by $profitDelta ($profitDeltaPct%). Overall trend: $trendDirection."

        return DomainResult.Success(
            CrossDimensionTrendResult(
                tenantId = tenantId,
                currentPeriodId = currentPeriodId,
                previousPeriodId = previousPeriodId,
                currentRevenue = currentSnapshot.revenue,
                previousRevenue = previousSnapshot.revenue,
                revenueDelta = ProfitabilityIntelligenceMathUtils.scaleMoney(revDelta),
                revenueDeltaPct = revDeltaPct,
                currentCost = currentSnapshot.totalCost,
                previousCost = previousSnapshot.totalCost,
                costDelta = ProfitabilityIntelligenceMathUtils.scaleMoney(costDelta),
                costDeltaPct = costDeltaPct,
                currentProfit = currentSnapshot.grossProfit,
                previousProfit = previousSnapshot.grossProfit,
                profitDelta = ProfitabilityIntelligenceMathUtils.scaleMoney(profitDelta),
                profitDeltaPct = profitDeltaPct,
                currentMargin = currentSnapshot.grossMargin,
                previousMargin = previousSnapshot.grossMargin,
                marginDelta = marginDelta?.let { ProfitabilityIntelligenceMathUtils.scaleMoney(it) },
                trendDirection = trendDirection,
                explanation = explanation
            )
        )
    }
}
