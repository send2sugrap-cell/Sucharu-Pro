package com.sucharu.sucharupro.domain.service.profitability

import com.sucharu.sucharupro.domain.model.profitability.*
import java.math.BigDecimal

/**
 * Implementation of period trend and comparison service.
 * Module 16 Step 06.
 */
class PeriodProfitabilityTrendServiceImpl : PeriodProfitabilityTrendService {

    override fun comparePeriods(
        currentSnapshot: PeriodProfitabilitySnapshot,
        previousSnapshot: PeriodProfitabilitySnapshot
    ): PeriodComparisonResult {
        val (revDelta, revDeltaPct) = PeriodProfitabilityMathUtils.calculateVariance(
            currentSnapshot.revenue,
            previousSnapshot.revenue
        )
        val (costDelta, costDeltaPct) = PeriodProfitabilityMathUtils.calculateVariance(
            currentSnapshot.totalActualCost,
            previousSnapshot.totalActualCost
        )
        val (profitDelta, profitDeltaPct) = PeriodProfitabilityMathUtils.calculateVariance(
            currentSnapshot.grossProfit,
            previousSnapshot.grossProfit
        )

        val marginDelta = if (currentSnapshot.grossMarginPercentage != null && previousSnapshot.grossMarginPercentage != null) {
            (currentSnapshot.grossMarginPercentage - previousSnapshot.grossMarginPercentage)
                .setScale(PeriodProfitabilityMathUtils.SCALE, PeriodProfitabilityMathUtils.ROUNDING)
        } else null

        val contribDelta = (currentSnapshot.contributionAmount - previousSnapshot.contributionAmount)
            .setScale(PeriodProfitabilityMathUtils.SCALE, PeriodProfitabilityMathUtils.ROUNDING)

        val (trendDir, trendReason) = PeriodProfitabilityMathUtils.determineTrend(
            currentProfit = currentSnapshot.grossProfit,
            previousProfit = previousSnapshot.grossProfit,
            currentMargin = currentSnapshot.grossMarginPercentage,
            previousMargin = previousSnapshot.grossMarginPercentage
        )

        return PeriodComparisonResult(
            currentPeriodId = currentSnapshot.periodId,
            currentPeriodKey = currentSnapshot.periodKey,
            comparisonPeriodId = previousSnapshot.periodId,
            comparisonPeriodKey = previousSnapshot.periodKey,
            periodType = currentSnapshot.periodType,
            currentRevenue = currentSnapshot.revenue,
            previousRevenue = previousSnapshot.revenue,
            revenueDelta = revDelta ?: BigDecimal.ZERO,
            revenueDeltaPercentage = revDeltaPct,
            currentCost = currentSnapshot.totalActualCost,
            previousCost = previousSnapshot.totalActualCost,
            costDelta = costDelta ?: BigDecimal.ZERO,
            costDeltaPercentage = costDeltaPct,
            currentGrossProfit = currentSnapshot.grossProfit,
            previousGrossProfit = previousSnapshot.grossProfit,
            grossProfitDelta = profitDelta ?: BigDecimal.ZERO,
            grossProfitDeltaPercentage = profitDeltaPct,
            currentGrossMarginPercentage = currentSnapshot.grossMarginPercentage,
            previousGrossMarginPercentage = previousSnapshot.grossMarginPercentage,
            grossMarginPercentageDelta = marginDelta,
            currentContributionAmount = currentSnapshot.contributionAmount,
            previousContributionAmount = previousSnapshot.contributionAmount,
            contributionDelta = contribDelta,
            currentUnits = currentSnapshot.totalUnits,
            previousUnits = previousSnapshot.totalUnits,
            currentJobCount = currentSnapshot.jobCount,
            previousJobCount = previousSnapshot.jobCount,
            trendDirection = trendDir,
            trendReason = trendReason
        )
    }
}
