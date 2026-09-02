package com.sucharu.sucharupro.domain.model.profitability

import java.math.BigDecimal
import java.math.RoundingMode
import java.security.MessageDigest

/**
 * Deterministic, mathematically explainable precision calculation utilities for
 * Cross-Dimensional Profitability Intelligence & Management Decision Engine.
 * Module 16 Step 07.
 */
object ProfitabilityIntelligenceMathUtils {

    const val SCALE = 4
    val ROUNDING: RoundingMode = RoundingMode.HALF_UP
    private val HUNDRED = BigDecimal("100.0000")
    private val ZERO = BigDecimal("0.0000")

    fun scaleMoney(value: BigDecimal): BigDecimal = value.setScale(SCALE, ROUNDING)

    fun safeDivide(
        numerator: BigDecimal,
        denominator: BigDecimal,
        fallback: BigDecimal? = null
    ): BigDecimal? {
        val scaledNum = scaleMoney(numerator)
        val scaledDen = scaleMoney(denominator)
        if (scaledDen.compareTo(BigDecimal.ZERO) == 0) return fallback
        return scaledNum.divide(scaledDen, SCALE, ROUNDING)
    }

    fun calculateGrossProfit(revenue: BigDecimal, totalCost: BigDecimal): BigDecimal {
        return scaleMoney(revenue).subtract(scaleMoney(totalCost))
    }

    fun calculateGrossMarginPercentage(revenue: BigDecimal, totalCost: BigDecimal): BigDecimal? {
        val scaledRev = scaleMoney(revenue)
        if (scaledRev.compareTo(BigDecimal.ZERO) == 0) return null
        val profit = calculateGrossProfit(scaledRev, totalCost)
        return profit.multiply(HUNDRED).divide(scaledRev, SCALE, ROUNDING)
    }

    fun calculateContributionAmount(revenue: BigDecimal, directCost: BigDecimal): BigDecimal {
        return scaleMoney(revenue).subtract(scaleMoney(directCost))
    }

    fun calculateContributionMarginPercentage(revenue: BigDecimal, directCost: BigDecimal): BigDecimal? {
        val scaledRev = scaleMoney(revenue)
        if (scaledRev.compareTo(BigDecimal.ZERO) == 0) return null
        val contribution = calculateContributionAmount(scaledRev, directCost)
        return contribution.multiply(HUNDRED).divide(scaledRev, SCALE, ROUNDING)
    }

    fun calculatePercentageChange(current: BigDecimal, previous: BigDecimal): BigDecimal? {
        val scaledPrev = scaleMoney(previous)
        val scaledCurr = scaleMoney(current)
        if (scaledPrev.compareTo(BigDecimal.ZERO) == 0) {
            return if (scaledCurr.compareTo(BigDecimal.ZERO) == 0) ZERO else null
        }
        val delta = scaledCurr.subtract(scaledPrev)
        return delta.multiply(HUNDRED).divide(scaledPrev.abs(), SCALE, ROUNDING)
    }

    fun calculateUnitEconomics(
        totalAmount: BigDecimal,
        unitCount: Long
    ): BigDecimal? {
        if (unitCount <= 0) return null
        return scaleMoney(totalAmount).divide(BigDecimal(unitCount), SCALE, ROUNDING)
    }

    fun calculateSharePercentage(entityAmount: BigDecimal, totalAmount: BigDecimal): BigDecimal {
        val scaledTotal = scaleMoney(totalAmount)
        if (scaledTotal.compareTo(BigDecimal.ZERO) <= 0) return ZERO
        val share = scaleMoney(entityAmount).multiply(HUNDRED).divide(scaledTotal, SCALE, ROUNDING)
        return clampPercentage(share)
    }

    fun clampPercentage(value: BigDecimal): BigDecimal {
        val scaled = scaleMoney(value)
        return when {
            scaled.compareTo(ZERO) < 0 -> ZERO
            scaled.compareTo(HUNDRED) > 0 -> HUNDRED
            else -> scaled
        }
    }

    fun clampScore(value: BigDecimal): BigDecimal {
        val scaled = scaleMoney(value)
        return when {
            scaled.compareTo(ZERO) < 0 -> ZERO
            scaled.compareTo(HUNDRED) > 0 -> HUNDRED
            else -> scaled
        }
    }

    /**
     * Deterministic Management Priority Scoring Formula:
     * priorityScore = (normalizedFinancialImpact * 0.35) + (severityWeight * 0.25) + (trendWeight * 0.15) + (concentrationWeight * 0.15) + (frequencyWeight * 0.10)
     * Normalized to [0.0000, 100.0000].
     */
    fun calculatePriorityScore(
        financialImpact: BigDecimal,
        totalRevenue: BigDecimal,
        severityLevel: ManagementPriorityLevel,
        trend: PeriodTrendDirection,
        concentrationShare: BigDecimal,
        occurrenceCount: Int
    ): BigDecimal {
        val impactRatio = if (totalRevenue.compareTo(BigDecimal.ZERO) > 0) {
            scaleMoney(financialImpact).multiply(HUNDRED).divide(scaleMoney(totalRevenue), SCALE, ROUNDING)
        } else {
            scaleMoney(financialImpact).min(HUNDRED)
        }
        val normalizedImpact = clampScore(impactRatio)

        val severityScore = when (severityLevel) {
            ManagementPriorityLevel.CRITICAL -> BigDecimal("100.0000")
            ManagementPriorityLevel.HIGH -> BigDecimal("75.0000")
            ManagementPriorityLevel.MEDIUM -> BigDecimal("50.0000")
            ManagementPriorityLevel.LOW -> BigDecimal("25.0000")
            ManagementPriorityLevel.INFORMATIONAL -> BigDecimal("10.0000")
        }

        val trendScore = when (trend) {
            PeriodTrendDirection.STRONGLY_DECLINING -> BigDecimal("100.0000")
            PeriodTrendDirection.DECLINING -> BigDecimal("70.0000")
            PeriodTrendDirection.STABLE -> BigDecimal("40.0000")
            PeriodTrendDirection.IMPROVING -> BigDecimal("20.0000")
            PeriodTrendDirection.STRONGLY_IMPROVING -> BigDecimal("5.0000")
            PeriodTrendDirection.INSUFFICIENT_DATA -> BigDecimal("30.0000")
        }

        val concentrationScore = clampScore(concentrationShare)
        val frequencyScore = clampScore(BigDecimal(occurrenceCount.coerceAtLeast(1) * 10))

        val score = (normalizedImpact.multiply(BigDecimal("0.35")))
            .add(severityScore.multiply(BigDecimal("0.25")))
            .add(trendScore.multiply(BigDecimal("0.15")))
            .add(concentrationScore.multiply(BigDecimal("0.15")))
            .add(frequencyScore.multiply(BigDecimal("0.10")))

        return clampScore(score)
    }

    /**
     * Deterministic Profitability Health Score (0.0000 - 100.0000):
     * marginScore (25%) + trendScore (15%) + costStabilityScore (15%) + revenueStabilityScore (15%)
     * + concentrationScore (10%) + vendorDependencyScore (10%) + dataIntegrityScore (10%)
     */
    fun calculateHealthScore(
        marginPercentage: BigDecimal?,
        trendDirection: PeriodTrendDirection,
        costVariancePct: BigDecimal?,
        revenueVariancePct: BigDecimal?,
        top1CustomerConcentration: BigDecimal,
        top1VendorConcentration: BigDecimal,
        hasIntegrityIssue: Boolean,
        hasDuplicates: Boolean
    ): ProfitabilityHealthScoreComponents {
        // 1. Margin Score (25%)
        val marginScore = when {
            marginPercentage == null -> BigDecimal("50.0000")
            marginPercentage.compareTo(BigDecimal("30.0000")) >= 0 -> BigDecimal("100.0000")
            marginPercentage.compareTo(BigDecimal("20.0000")) >= 0 -> BigDecimal("80.0000")
            marginPercentage.compareTo(BigDecimal("10.0000")) >= 0 -> BigDecimal("60.0000")
            marginPercentage.compareTo(BigDecimal("0.0000")) >= 0 -> BigDecimal("40.0000")
            marginPercentage.compareTo(BigDecimal("-10.0000")) >= 0 -> BigDecimal("20.0000")
            else -> BigDecimal("0.0000")
        }

        // 2. Trend Score (15%)
        val trendScore = when (trendDirection) {
            PeriodTrendDirection.STRONGLY_IMPROVING -> BigDecimal("100.0000")
            PeriodTrendDirection.IMPROVING -> BigDecimal("80.0000")
            PeriodTrendDirection.STABLE -> BigDecimal("60.0000")
            PeriodTrendDirection.DECLINING -> BigDecimal("35.0000")
            PeriodTrendDirection.STRONGLY_DECLINING -> BigDecimal("10.0000")
            PeriodTrendDirection.INSUFFICIENT_DATA -> BigDecimal("50.0000")
        }

        // 3. Cost Stability Score (15%)
        val costVariance = costVariancePct?.abs() ?: BigDecimal.ZERO
        val costStabilityScore = when {
            costVariance.compareTo(BigDecimal("5.0000")) <= 0 -> BigDecimal("100.0000")
            costVariance.compareTo(BigDecimal("15.0000")) <= 0 -> BigDecimal("75.0000")
            costVariance.compareTo(BigDecimal("30.0000")) <= 0 -> BigDecimal("45.0000")
            else -> BigDecimal("15.0000")
        }

        // 4. Revenue Stability Score (15%)
        val revVariance = revenueVariancePct?.abs() ?: BigDecimal.ZERO
        val revenueStabilityScore = when {
            revVariance.compareTo(BigDecimal("5.0000")) <= 0 -> BigDecimal("100.0000")
            revVariance.compareTo(BigDecimal("15.0000")) <= 0 -> BigDecimal("80.0000")
            revVariance.compareTo(BigDecimal("30.0000")) <= 0 -> BigDecimal("50.0000")
            else -> BigDecimal("20.0000")
        }

        // 5. Concentration Score (10%) (Lower concentration = higher health)
        val concentrationScore = when {
            top1CustomerConcentration.compareTo(BigDecimal("20.0000")) <= 0 -> BigDecimal("100.0000")
            top1CustomerConcentration.compareTo(BigDecimal("40.0000")) <= 0 -> BigDecimal("75.0000")
            top1CustomerConcentration.compareTo(BigDecimal("60.0000")) <= 0 -> BigDecimal("45.0000")
            else -> BigDecimal("15.0000")
        }

        // 6. Vendor Dependency Score (10%)
        val vendorDependencyScore = when {
            top1VendorConcentration.compareTo(BigDecimal("25.0000")) <= 0 -> BigDecimal("100.0000")
            top1VendorConcentration.compareTo(BigDecimal("50.0000")) <= 0 -> BigDecimal("70.0000")
            top1VendorConcentration.compareTo(BigDecimal("75.0000")) <= 0 -> BigDecimal("40.0000")
            else -> BigDecimal("10.0000")
        }

        // 7. Data Integrity Score (10%)
        val dataIntegrityScore = when {
            hasIntegrityIssue -> BigDecimal("0.0000")
            hasDuplicates -> BigDecimal("40.0000")
            else -> BigDecimal("100.0000")
        }

        val attributionCompletenessScore = if (hasIntegrityIssue) BigDecimal("30.0000") else BigDecimal("100.0000")

        val overall = (marginScore.multiply(BigDecimal("0.25")))
            .add(trendScore.multiply(BigDecimal("0.15")))
            .add(costStabilityScore.multiply(BigDecimal("0.15")))
            .add(revenueStabilityScore.multiply(BigDecimal("0.15")))
            .add(concentrationScore.multiply(BigDecimal("0.10")))
            .add(vendorDependencyScore.multiply(BigDecimal("0.10")))
            .add(dataIntegrityScore.multiply(BigDecimal("0.10")))

        val overallClamped = clampScore(overall)

        val healthLevel = when {
            overallClamped.compareTo(BigDecimal("85.0000")) >= 0 -> ProfitabilityHealthLevel.EXCELLENT
            overallClamped.compareTo(BigDecimal("70.0000")) >= 0 -> ProfitabilityHealthLevel.HEALTHY
            overallClamped.compareTo(BigDecimal("50.0000")) >= 0 -> ProfitabilityHealthLevel.MODERATE
            overallClamped.compareTo(BigDecimal("30.0000")) >= 0 -> ProfitabilityHealthLevel.AT_RISK
            else -> ProfitabilityHealthLevel.CRITICAL
        }

        val explanation = "Overall health score $overallClamped derived deterministically: Margin ($marginScore), Trend ($trendScore), Cost Stability ($costStabilityScore), Revenue Stability ($revenueStabilityScore), Concentration ($concentrationScore), Vendor Dependency ($vendorDependencyScore), Integrity ($dataIntegrityScore)."

        return ProfitabilityHealthScoreComponents(
            overallScore = overallClamped,
            marginScore = marginScore,
            trendScore = trendScore,
            costStabilityScore = costStabilityScore,
            revenueStabilityScore = revenueStabilityScore,
            concentrationScore = concentrationScore,
            vendorDependencyScore = vendorDependencyScore,
            dataIntegrityScore = dataIntegrityScore,
            attributionCompletenessScore = attributionCompletenessScore,
            healthLevel = healthLevel,
            explanation = explanation
        )
    }

    data class ProfitabilityHealthScoreComponents(
        val overallScore: BigDecimal,
        val marginScore: BigDecimal,
        val trendScore: BigDecimal,
        val costStabilityScore: BigDecimal,
        val revenueStabilityScore: BigDecimal,
        val concentrationScore: BigDecimal,
        val vendorDependencyScore: BigDecimal,
        val dataIntegrityScore: BigDecimal,
        val attributionCompletenessScore: BigDecimal,
        val healthLevel: ProfitabilityHealthLevel,
        val explanation: String
    )

    fun classifyProfitability(revenue: BigDecimal, totalCost: BigDecimal): ProfitabilityClassification {
        val rev = scaleMoney(revenue)
        val cost = scaleMoney(totalCost)
        val profit = rev.subtract(cost)
        val margin = calculateGrossMarginPercentage(rev, cost)

        return when {
            rev.compareTo(BigDecimal.ZERO) == 0 && cost.compareTo(BigDecimal.ZERO) == 0 -> ProfitabilityClassification.INSUFFICIENT_DATA
            rev.compareTo(BigDecimal.ZERO) == 0 && cost.compareTo(BigDecimal.ZERO) > 0 -> ProfitabilityClassification.LOSS_MAKING
            profit.compareTo(BigDecimal.ZERO) < 0 -> ProfitabilityClassification.LOSS_MAKING
            profit.compareTo(BigDecimal.ZERO) == 0 -> ProfitabilityClassification.BREAK_EVEN
            margin != null && margin.compareTo(BigDecimal("10.0000")) < 0 -> ProfitabilityClassification.LOW_MARGIN
            margin != null && margin.compareTo(BigDecimal("25.0000")) >= 0 -> ProfitabilityClassification.HIGHLY_PROFITABLE
            else -> ProfitabilityClassification.PROFITABLE
        }
    }

    fun classifyRiskLevel(
        margin: BigDecimal?,
        profitabilityClassification: ProfitabilityClassification,
        concentrationShare: BigDecimal
    ): ProfitabilityRiskLevel {
        return when {
            profitabilityClassification == ProfitabilityClassification.LOSS_MAKING -> ProfitabilityRiskLevel.CRITICAL
            margin != null && margin.compareTo(BigDecimal("5.0000")) < 0 -> ProfitabilityRiskLevel.HIGH
            concentrationShare.compareTo(BigDecimal("50.0000")) > 0 -> ProfitabilityRiskLevel.HIGH
            concentrationShare.compareTo(BigDecimal("30.0000")) > 0 -> ProfitabilityRiskLevel.MODERATE
            profitabilityClassification == ProfitabilityClassification.LOW_MARGIN -> ProfitabilityRiskLevel.MODERATE
            profitabilityClassification == ProfitabilityClassification.HIGHLY_PROFITABLE -> ProfitabilityRiskLevel.LOW
            else -> ProfitabilityRiskLevel.LOW
        }
    }

    /**
     * Deterministic SHA-256 Provenance Fingerprint.
     */
    fun generateProvenanceFingerprint(
        tenantId: String,
        periodId: String,
        sourceModule: String,
        sourceEntityType: String,
        sourceEntityId: String,
        sourceTransactionId: String?,
        metricType: String
    ): String {
        val canonical = listOf(
            tenantId.trim(),
            periodId.trim(),
            sourceModule.trim(),
            sourceEntityType.trim(),
            sourceEntityId.trim(),
            sourceTransactionId?.trim() ?: "NONE",
            metricType.trim()
        ).joinToString(":")

        return sha256(canonical)
    }

    /**
     * Deterministic SHA-256 Integrity Hash across snapshot payload.
     */
    fun generateIntegrityHash(
        tenantId: String,
        periodId: String,
        revenue: BigDecimal,
        totalCost: BigDecimal,
        grossProfit: BigDecimal,
        dimensionInsights: List<DimensionInsight>,
        relationshipInsights: List<ProfitabilityRelationshipInsight>,
        drivers: List<ProfitabilityDriver>,
        leakages: List<ProfitLeakageItem>,
        priorities: List<ManagementPriorityItem>,
        healthScore: BigDecimal?
    ): String {
        val sortedDims = dimensionInsights.sortedWith(
            compareBy<DimensionInsight> { it.dimensionType.name }
                .thenBy { it.dimensionId }
        ).joinToString("|") { "${it.dimensionType}:${it.dimensionId}:${scaleMoney(it.revenue)}:${scaleMoney(it.cost)}" }

        val sortedRels = relationshipInsights.sortedWith(
            compareBy<ProfitabilityRelationshipInsight> { it.fromDimensionType.name }
                .thenBy { it.fromEntityId }
                .thenBy { it.toDimensionType.name }
                .thenBy { it.toEntityId }
        ).joinToString("|") { "${it.fromDimensionType}:${it.fromEntityId}->${it.toDimensionType}:${it.toEntityId}:${scaleMoney(it.grossProfit)}" }

        val sortedDrivers = drivers.sortedWith(
            compareBy<ProfitabilityDriver> { it.driverType.name }
                .thenBy { it.rank }
                .thenBy { it.entityId }
        ).joinToString("|") { "${it.driverId}:${it.category}:${scaleMoney(it.impactAmount)}" }

        val sortedLeakages = leakages.sortedWith(
            compareBy<ProfitLeakageItem> { it.category.name }
                .thenBy { it.entityId }
        ).joinToString("|") { "${it.leakageId}:${scaleMoney(it.estimatedImpact)}" }

        val sortedPriorities = priorities.sortedWith(
            compareBy<ManagementPriorityItem> { it.priorityLevel.name }
                .thenByDescending { it.priorityScore }
                .thenBy { it.entityId }
        ).joinToString("|") { "${it.priorityId}:${scaleMoney(it.priorityScore)}" }

        val canonical = listOf(
            tenantId.trim(),
            periodId.trim(),
            scaleMoney(revenue).toPlainString(),
            scaleMoney(totalCost).toPlainString(),
            scaleMoney(grossProfit).toPlainString(),
            healthScore?.let { scaleMoney(it).toPlainString() } ?: "0.0000",
            sortedDims,
            sortedRels,
            sortedDrivers,
            sortedLeakages,
            sortedPriorities
        ).joinToString("#")

        return sha256(canonical)
    }

    private fun sha256(input: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(input.toByteArray(Charsets.UTF_8))
        return hash.joinToString("") { "%02x".format(it) }
    }
}
