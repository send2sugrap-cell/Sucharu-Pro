package com.sucharu.sucharupro.domain.model.profitability

import java.math.BigDecimal
import java.math.RoundingMode
import java.security.MessageDigest

/**
 * Precise, zero-safe financial math and cryptographic hashing for Period Profitability.
 * Scale: 4, RoundingMode: HALF_UP.
 * Module 16 Step 06.
 */
object PeriodProfitabilityMathUtils {

    const val SCALE = 4
    val ROUNDING = RoundingMode.HALF_UP
    val ONE_HUNDRED = BigDecimal("100.0000")

    fun calculateGrossProfit(revenue: BigDecimal, totalActualCost: BigDecimal): BigDecimal {
        return (revenue - totalActualCost).setScale(SCALE, ROUNDING)
    }

    fun calculateGrossMarginPercentage(grossProfit: BigDecimal, revenue: BigDecimal): BigDecimal? {
        if (revenue <= BigDecimal.ZERO) return null
        return ((grossProfit.divide(revenue, 8, ROUNDING)) * ONE_HUNDRED).setScale(SCALE, ROUNDING)
    }

    fun calculateCostToRevenuePercentage(totalActualCost: BigDecimal, revenue: BigDecimal): BigDecimal? {
        if (revenue <= BigDecimal.ZERO) return null
        return ((totalActualCost.divide(revenue, 8, ROUNDING)) * ONE_HUNDRED).setScale(SCALE, ROUNDING)
    }

    fun calculateContributionAmount(revenue: BigDecimal, directCost: BigDecimal): BigDecimal {
        return (revenue - directCost).setScale(SCALE, ROUNDING)
    }

    fun calculateContributionMarginPercentage(contributionAmount: BigDecimal, revenue: BigDecimal): BigDecimal? {
        if (revenue <= BigDecimal.ZERO) return null
        return ((contributionAmount.divide(revenue, 8, ROUNDING)) * ONE_HUNDRED).setScale(SCALE, ROUNDING)
    }

    fun calculateAverageRevenuePerJob(revenue: BigDecimal, jobCount: Int): BigDecimal? {
        if (jobCount <= 0) return null
        return revenue.divide(BigDecimal.valueOf(jobCount.toLong()), SCALE, ROUNDING)
    }

    fun calculateAverageProfitPerJob(grossProfit: BigDecimal, jobCount: Int): BigDecimal? {
        if (jobCount <= 0) return null
        return grossProfit.divide(BigDecimal.valueOf(jobCount.toLong()), SCALE, ROUNDING)
    }

    fun calculateAverageRevenuePerUnit(revenue: BigDecimal, totalUnits: Long): BigDecimal? {
        if (totalUnits <= 0L) return null
        return revenue.divide(BigDecimal.valueOf(totalUnits), SCALE, ROUNDING)
    }

    fun calculateAverageCostPerUnit(totalActualCost: BigDecimal, totalUnits: Long): BigDecimal? {
        if (totalUnits <= 0L) return null
        return totalActualCost.divide(BigDecimal.valueOf(totalUnits), SCALE, ROUNDING)
    }

    fun calculateAverageProfitPerUnit(grossProfit: BigDecimal, totalUnits: Long): BigDecimal? {
        if (totalUnits <= 0L) return null
        return grossProfit.divide(BigDecimal.valueOf(totalUnits), SCALE, ROUNDING)
    }

    fun calculateVariance(current: BigDecimal, baseline: BigDecimal?): Pair<BigDecimal?, BigDecimal?> {
        if (baseline == null) return Pair(null, null)
        val varianceAmount = (current - baseline).setScale(SCALE, ROUNDING)
        val variancePct = if (baseline > BigDecimal.ZERO) {
            ((varianceAmount.divide(baseline, 8, ROUNDING)) * ONE_HUNDRED).setScale(SCALE, ROUNDING)
        } else null
        return Pair(varianceAmount, variancePct)
    }

    fun classifyProfitability(
        revenue: BigDecimal,
        grossProfit: BigDecimal,
        grossMarginPercentage: BigDecimal?
    ): ProfitabilityClassification {
        if (revenue <= BigDecimal.ZERO && grossProfit == BigDecimal.ZERO) {
            return ProfitabilityClassification.NO_REVENUE
        }
        if (grossProfit < BigDecimal.ZERO) {
            return ProfitabilityClassification.LOSS_MAKING
        }
        if (grossProfit == BigDecimal.ZERO) {
            return ProfitabilityClassification.BREAK_EVEN
        }
        val margin = grossMarginPercentage ?: BigDecimal.ZERO
        return when {
            margin >= BigDecimal("30.0000") -> ProfitabilityClassification.HIGHLY_PROFITABLE
            margin >= BigDecimal("10.0000") -> ProfitabilityClassification.PROFITABLE
            else -> ProfitabilityClassification.LOW_MARGIN
        }
    }

    fun determineTrend(
        currentProfit: BigDecimal,
        previousProfit: BigDecimal?,
        currentMargin: BigDecimal?,
        previousMargin: BigDecimal?
    ): Pair<PeriodTrendDirection, String> {
        if (previousProfit == null) {
            return Pair(PeriodTrendDirection.INSUFFICIENT_DATA, "No previous period baseline available")
        }

        val profitDelta = currentProfit - previousProfit
        val marginDelta = if (currentMargin != null && previousMargin != null) {
            currentMargin - previousMargin
        } else BigDecimal.ZERO

        return when {
            profitDelta > BigDecimal.ZERO && marginDelta >= BigDecimal("5.0000") ->
                Pair(PeriodTrendDirection.STRONGLY_IMPROVING, "Profit grew by BDT $profitDelta with strong margin expansion of $marginDelta%")
            profitDelta > BigDecimal.ZERO ->
                Pair(PeriodTrendDirection.IMPROVING, "Profit grew by BDT $profitDelta compared to previous period")
            profitDelta == BigDecimal.ZERO ->
                Pair(PeriodTrendDirection.STABLE, "Profit remained identical to previous period")
            profitDelta < BigDecimal.ZERO && marginDelta <= BigDecimal("-5.0000") ->
                Pair(PeriodTrendDirection.STRONGLY_DECLINING, "Profit declined by BDT ${profitDelta.abs()} with severe margin contraction of ${marginDelta.abs()}%")
            else ->
                Pair(PeriodTrendDirection.DECLINING, "Profit declined by BDT ${profitDelta.abs()} compared to previous period")
        }
    }

    fun generateProvenanceFingerprint(
        tenantId: String,
        periodId: String,
        sourceModule: String,
        sourceEntityType: String,
        sourceEntityId: String,
        amount: BigDecimal,
        componentType: String?
    ): String {
        val raw = "$tenantId|$periodId|$sourceModule|$sourceEntityType|$sourceEntityId|${amount.setScale(SCALE, ROUNDING)}|${componentType ?: "N/A"}"
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(raw.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }

    fun generateSnapshotIntegrityHash(
        tenantId: String,
        projectId: String,
        periodId: String,
        periodType: String,
        periodStart: Long,
        periodEnd: Long,
        revenue: BigDecimal,
        totalCost: BigDecimal,
        grossProfit: BigDecimal,
        calculationVersion: String,
        provenanceFingerprints: List<String>
    ): String {
        val sortedFingerprints = provenanceFingerprints.sorted().joinToString(";")
        val raw = "$tenantId|$projectId|$periodId|$periodType|$periodStart|$periodEnd|${revenue.setScale(SCALE, ROUNDING)}|${totalCost.setScale(SCALE, ROUNDING)}|${grossProfit.setScale(SCALE, ROUNDING)}|$calculationVersion|$sortedFingerprints"
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(raw.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }
}
