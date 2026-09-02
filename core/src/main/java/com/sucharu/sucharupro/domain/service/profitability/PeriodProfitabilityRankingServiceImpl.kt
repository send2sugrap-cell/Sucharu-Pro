package com.sucharu.sucharupro.domain.service.profitability

import com.sucharu.sucharupro.domain.model.profitability.*
import java.math.BigDecimal

/**
 * Implementation of period ranking and concentration analysis.
 * Module 16 Step 06.
 */
class PeriodProfitabilityRankingServiceImpl : PeriodProfitabilityRankingService {

    override fun rankPeriods(
        snapshots: List<PeriodProfitabilitySnapshot>,
        criteria: PeriodRankingCriteria,
        ascending: Boolean
    ): List<PeriodRankingItem> {
        val comparator: Comparator<PeriodProfitabilitySnapshot> = when (criteria) {
            PeriodRankingCriteria.GROSS_PROFIT -> compareBy<PeriodProfitabilitySnapshot> { it.grossProfit }
            PeriodRankingCriteria.GROSS_MARGIN -> compareBy<PeriodProfitabilitySnapshot> { it.grossMarginPercentage ?: BigDecimal.ZERO }
            PeriodRankingCriteria.REVENUE -> compareBy<PeriodProfitabilitySnapshot> { it.revenue }
            PeriodRankingCriteria.TOTAL_COST -> compareBy<PeriodProfitabilitySnapshot> { it.totalActualCost }
            PeriodRankingCriteria.CONTRIBUTION_AMOUNT -> compareBy<PeriodProfitabilitySnapshot> { it.contributionAmount }
            PeriodRankingCriteria.CONTRIBUTION_MARGIN -> compareBy<PeriodProfitabilitySnapshot> { it.contributionMarginPercentage ?: BigDecimal.ZERO }
            PeriodRankingCriteria.PROFIT_PER_JOB -> compareBy<PeriodProfitabilitySnapshot> { it.averageProfitPerJob ?: BigDecimal.ZERO }
            PeriodRankingCriteria.PROFIT_PER_UNIT -> compareBy<PeriodProfitabilitySnapshot> { it.averageProfitPerUnit ?: BigDecimal.ZERO }
        }.thenBy { it.revenue }
            .thenBy { it.periodEnd }
            .thenBy { it.periodId }

        val sorted = if (ascending) {
            snapshots.sortedWith(comparator)
        } else {
            snapshots.sortedWith(comparator.reversed())
        }

        return sorted.mapIndexed { index, snap ->
            val (metricVal, metricLabel) = when (criteria) {
                PeriodRankingCriteria.GROSS_PROFIT -> Pair(snap.grossProfit, "Gross Profit")
                PeriodRankingCriteria.GROSS_MARGIN -> Pair(snap.grossMarginPercentage ?: BigDecimal.ZERO, "Gross Margin %")
                PeriodRankingCriteria.REVENUE -> Pair(snap.revenue, "Revenue")
                PeriodRankingCriteria.TOTAL_COST -> Pair(snap.totalActualCost, "Total Cost")
                PeriodRankingCriteria.CONTRIBUTION_AMOUNT -> Pair(snap.contributionAmount, "Contribution")
                PeriodRankingCriteria.CONTRIBUTION_MARGIN -> Pair(snap.contributionMarginPercentage ?: BigDecimal.ZERO, "Contribution Margin %")
                PeriodRankingCriteria.PROFIT_PER_JOB -> Pair(snap.averageProfitPerJob ?: BigDecimal.ZERO, "Profit/Job")
                PeriodRankingCriteria.PROFIT_PER_UNIT -> Pair(snap.averageProfitPerUnit ?: BigDecimal.ZERO, "Profit/Unit")
            }

            PeriodRankingItem(
                rank = index + 1,
                periodId = snap.periodId,
                periodKey = snap.periodKey,
                periodType = snap.periodType,
                periodStart = snap.periodStart,
                periodEnd = snap.periodEnd,
                metricValue = metricVal,
                metricLabel = metricLabel,
                revenue = snap.revenue,
                grossProfit = snap.grossProfit,
                grossMarginPercentage = snap.grossMarginPercentage,
                profitabilityClassification = snap.profitabilityClassification
            )
        }
    }

    override fun analyzeConcentration(
        tenantId: String,
        projectId: String,
        periodType: PeriodType,
        scopeLabel: String,
        snapshots: List<PeriodProfitabilitySnapshot>
    ): PeriodConcentrationAnalysis {
        val totalRev = snapshots.fold(BigDecimal.ZERO) { acc, s -> acc + s.revenue }
            .setScale(PeriodProfitabilityMathUtils.SCALE, PeriodProfitabilityMathUtils.ROUNDING)
        val totalProfit = snapshots.fold(BigDecimal.ZERO) { acc, s -> acc + s.grossProfit }
            .setScale(PeriodProfitabilityMathUtils.SCALE, PeriodProfitabilityMathUtils.ROUNDING)

        val sortedByProfitDesc = snapshots.sortedWith(
            compareByDescending<PeriodProfitabilitySnapshot> { it.grossProfit }
                .thenByDescending { it.revenue }
                .thenBy { it.periodId }
        )

        val top1 = sortedByProfitDesc.firstOrNull()
        val top1Profit = top1?.grossProfit ?: BigDecimal.ZERO
        val top1Share = if (totalProfit > BigDecimal.ZERO) {
            ((top1Profit.divide(totalProfit, 8, PeriodProfitabilityMathUtils.ROUNDING)) * PeriodProfitabilityMathUtils.ONE_HUNDRED)
                .setScale(PeriodProfitabilityMathUtils.SCALE, PeriodProfitabilityMathUtils.ROUNDING)
        } else BigDecimal.ZERO

        val top3Profit = sortedByProfitDesc.take(3).fold(BigDecimal.ZERO) { acc, s -> acc + s.grossProfit }
            .setScale(PeriodProfitabilityMathUtils.SCALE, PeriodProfitabilityMathUtils.ROUNDING)
        val top3Share = if (totalProfit > BigDecimal.ZERO) {
            ((top3Profit.divide(totalProfit, 8, PeriodProfitabilityMathUtils.ROUNDING)) * PeriodProfitabilityMathUtils.ONE_HUNDRED)
                .setScale(PeriodProfitabilityMathUtils.SCALE, PeriodProfitabilityMathUtils.ROUNDING)
        } else BigDecimal.ZERO

        val top5Profit = sortedByProfitDesc.take(5).fold(BigDecimal.ZERO) { acc, s -> acc + s.grossProfit }
            .setScale(PeriodProfitabilityMathUtils.SCALE, PeriodProfitabilityMathUtils.ROUNDING)
        val top5Share = if (totalProfit > BigDecimal.ZERO) {
            ((top5Profit.divide(totalProfit, 8, PeriodProfitabilityMathUtils.ROUNDING)) * PeriodProfitabilityMathUtils.ONE_HUNDRED)
                .setScale(PeriodProfitabilityMathUtils.SCALE, PeriodProfitabilityMathUtils.ROUNDING)
        } else BigDecimal.ZERO

        return PeriodConcentrationAnalysis(
            tenantId = tenantId,
            projectId = projectId,
            periodType = periodType,
            scopeLabel = scopeLabel,
            totalRevenue = totalRev,
            totalProfit = totalProfit,
            totalPeriodsEvaluated = snapshots.size,
            top1PeriodId = top1?.periodId,
            top1PeriodKey = top1?.periodKey,
            top1Profit = top1Profit,
            top1ProfitSharePercentage = top1Share,
            top3Profit = top3Profit,
            top3ProfitSharePercentage = top3Share,
            top5Profit = top5Profit,
            top5ProfitSharePercentage = top5Share
        )
    }
}
