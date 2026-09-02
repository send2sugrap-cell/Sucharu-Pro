package com.sucharu.sucharupro.domain.model.profitability

import java.math.BigDecimal
import java.math.RoundingMode
import java.security.MessageDigest

/**
 * High-Precision Deterministic Math & Cryptographic Utilities for Profitability Forecasting.
 * Uses BigDecimal scale = 4, RoundingMode.HALF_UP.
 * Module 16 Step 08.
 */
object ProfitabilityForecastMathUtils {

    private const val DEFAULT_SCALE = 4
    private val ROUNDING_MODE = RoundingMode.HALF_UP
    private val ONE_HUNDRED = BigDecimal("100.0000")

    fun scaleMoney(value: BigDecimal?): BigDecimal {
        return (value ?: BigDecimal.ZERO).setScale(DEFAULT_SCALE, ROUNDING_MODE)
    }

    fun safeDivide(numerator: BigDecimal?, denominator: BigDecimal?): BigDecimal? {
        if (numerator == null || denominator == null) return null
        if (denominator.compareTo(BigDecimal.ZERO) == 0) return null
        return numerator.divide(denominator, DEFAULT_SCALE, ROUNDING_MODE)
    }

    fun calculateGrossProfit(revenue: BigDecimal?, totalCost: BigDecimal?): BigDecimal {
        val rev = scaleMoney(revenue)
        val cost = scaleMoney(totalCost)
        return rev.subtract(cost).setScale(DEFAULT_SCALE, ROUNDING_MODE)
    }

    fun calculateGrossMarginPercentage(revenue: BigDecimal?, totalCost: BigDecimal?): BigDecimal? {
        val rev = scaleMoney(revenue)
        val cost = scaleMoney(totalCost)
        if (rev.compareTo(BigDecimal.ZERO) <= 0) return null
        val profit = rev.subtract(cost)
        return profit.multiply(ONE_HUNDRED).divide(rev, DEFAULT_SCALE, ROUNDING_MODE)
    }

    fun calculateContributionMarginPercentage(revenue: BigDecimal?, contribution: BigDecimal?): BigDecimal? {
        val rev = scaleMoney(revenue)
        val cont = scaleMoney(contribution)
        if (rev.compareTo(BigDecimal.ZERO) <= 0) return null
        return cont.multiply(ONE_HUNDRED).divide(rev, DEFAULT_SCALE, ROUNDING_MODE)
    }

    fun calculatePercentageChange(baseValue: BigDecimal?, newValue: BigDecimal?): BigDecimal? {
        if (baseValue == null || newValue == null) return null
        if (baseValue.compareTo(BigDecimal.ZERO) == 0) {
            return if (newValue.compareTo(BigDecimal.ZERO) > 0) ONE_HUNDRED else if (newValue.compareTo(BigDecimal.ZERO) < 0) ONE_HUNDRED.negate() else BigDecimal.ZERO.setScale(DEFAULT_SCALE, ROUNDING_MODE)
        }
        val delta = newValue.subtract(baseValue)
        return delta.multiply(ONE_HUNDRED).divide(baseValue.abs(), DEFAULT_SCALE, ROUNDING_MODE)
    }

    fun applyAdjustment(base: BigDecimal?, adjustmentPercentage: BigDecimal?, absoluteDelta: BigDecimal?): BigDecimal {
        val b = scaleMoney(base)
        val adjPct = adjustmentPercentage ?: BigDecimal.ZERO
        val absDelta = absoluteDelta ?: BigDecimal.ZERO
        val multiplier = BigDecimal.ONE.add(adjPct.divide(ONE_HUNDRED, DEFAULT_SCALE, ROUNDING_MODE))
        val multiplied = b.multiply(multiplier).setScale(DEFAULT_SCALE, ROUNDING_MODE)
        return multiplied.add(absDelta).setScale(DEFAULT_SCALE, ROUNDING_MODE)
    }

    /**
     * Calculates Simple Rolling Average: SUM(values) / N
     */
    fun calculateRollingAverage(values: List<BigDecimal>): BigDecimal {
        if (values.isEmpty()) return BigDecimal.ZERO.setScale(DEFAULT_SCALE, ROUNDING_MODE)
        val sum = values.fold(BigDecimal.ZERO) { acc, v -> acc.add(v) }
        return sum.divide(BigDecimal(values.size), DEFAULT_SCALE, ROUNDING_MODE)
    }

    /**
     * Calculates Weighted Rolling Average: SUM(value_i * weight_i) / SUM(weight_i)
     */
    fun calculateWeightedRollingAverage(values: List<BigDecimal>, weights: List<BigDecimal>): BigDecimal {
        if (values.isEmpty() || values.size != weights.size) {
            return calculateRollingAverage(values)
        }
        var totalWeightedSum = BigDecimal.ZERO
        var totalWeights = BigDecimal.ZERO
        for (i in values.indices) {
            val w = scaleMoney(weights[i])
            val v = scaleMoney(values[i])
            totalWeightedSum = totalWeightedSum.add(v.multiply(w))
            totalWeights = totalWeights.add(w)
        }
        if (totalWeights.compareTo(BigDecimal.ZERO) == 0) return calculateRollingAverage(values)
        return totalWeightedSum.divide(totalWeights, DEFAULT_SCALE, ROUNDING_MODE)
    }

    /**
     * Calculates Linear Trend Projection using Ordinary Least Squares (OLS) slope.
     * y = a + b * x
     */
    fun calculateLinearTrendProjection(historicalValues: List<BigDecimal>, periodsAhead: Int): BigDecimal {
        if (historicalValues.isEmpty()) return BigDecimal.ZERO.setScale(DEFAULT_SCALE, ROUNDING_MODE)
        if (historicalValues.size == 1) return scaleMoney(historicalValues.first())

        val n = historicalValues.size
        var sumX = BigDecimal.ZERO
        var sumY = BigDecimal.ZERO
        var sumXY = BigDecimal.ZERO
        var sumX2 = BigDecimal.ZERO

        for (i in 0 until n) {
            val x = BigDecimal(i + 1)
            val y = scaleMoney(historicalValues[i])
            sumX = sumX.add(x)
            sumY = sumY.add(y)
            sumXY = sumXY.add(x.multiply(y))
            sumX2 = sumX2.add(x.multiply(x))
        }

        val bigN = BigDecimal(n)
        val denominator = bigN.multiply(sumX2).subtract(sumX.multiply(sumX))
        if (denominator.compareTo(BigDecimal.ZERO) == 0) {
            return calculateRollingAverage(historicalValues)
        }

        val numeratorSlope = bigN.multiply(sumXY).subtract(sumX.multiply(sumY))
        val slope = numeratorSlope.divide(denominator, DEFAULT_SCALE, ROUNDING_MODE)
        val intercept = sumY.subtract(slope.multiply(sumX)).divide(bigN, DEFAULT_SCALE, ROUNDING_MODE)

        val targetX = BigDecimal(n + periodsAhead)
        val projected = intercept.add(slope.multiply(targetX)).setScale(DEFAULT_SCALE, ROUNDING_MODE)
        // Guard against negative projected revenue or cost if baseline is non-negative
        return if (projected.compareTo(BigDecimal.ZERO) < 0 && historicalValues.all { it.compareTo(BigDecimal.ZERO) >= 0 }) {
            BigDecimal.ZERO.setScale(DEFAULT_SCALE, ROUNDING_MODE)
        } else {
            projected
        }
    }

    fun calculateBreakEvenRevenue(fixedCost: BigDecimal?, contributionMarginRatio: BigDecimal?): BigDecimal? {
        val fc = scaleMoney(fixedCost)
        val cmr = contributionMarginRatio ?: return null
        if (cmr.compareTo(BigDecimal.ZERO) <= 0) return null
        val ratio = cmr.divide(ONE_HUNDRED, DEFAULT_SCALE, ROUNDING_MODE)
        if (ratio.compareTo(BigDecimal.ZERO) == 0) return null
        return fc.divide(ratio, DEFAULT_SCALE, ROUNDING_MODE)
    }

    /**
     * Deterministic Confidence Score (0.0000 - 100.0000).
     * Evaluates data depth (40%), reconciliation status (25%), source readiness (20%), and volatility stability (15%).
     */
    fun calculateConfidenceScore(
        historicalPeriodCount: Int,
        isFullyReconciled: Boolean,
        sourceReadiness: PeriodSourceReadiness,
        volatilityPercentage: BigDecimal?
    ): BigDecimal {
        var score = BigDecimal.ZERO

        // 1. Historical Depth (Max 40.0000 pts)
        val depthPoints = when {
            historicalPeriodCount >= 12 -> BigDecimal("40.0000")
            historicalPeriodCount >= 6 -> BigDecimal("30.0000")
            historicalPeriodCount >= 3 -> BigDecimal("20.0000")
            historicalPeriodCount >= 1 -> BigDecimal("10.0000")
            else -> BigDecimal.ZERO
        }
        score = score.add(depthPoints)

        // 2. Reconciliation Status (Max 25.0000 pts)
        val reconPoints = if (isFullyReconciled) BigDecimal("25.0000") else BigDecimal("5.0000")
        score = score.add(reconPoints)

        // 3. Source Readiness (Max 20.0000 pts)
        val readyPoints = when (sourceReadiness) {
            PeriodSourceReadiness.READY -> BigDecimal("20.0000")
            PeriodSourceReadiness.PARTIAL -> BigDecimal("10.0000")
            PeriodSourceReadiness.DEGRADED -> BigDecimal("5.0000")
            else -> BigDecimal.ZERO
        }
        score = score.add(readyPoints)

        // 4. Volatility Stability (Max 15.0000 pts)
        val vol = volatilityPercentage ?: BigDecimal.ZERO
        val volPoints = when {
            vol.compareTo(BigDecimal("10.0000")) <= 0 -> BigDecimal("15.0000")
            vol.compareTo(BigDecimal("25.0000")) <= 0 -> BigDecimal("10.0000")
            vol.compareTo(BigDecimal("50.0000")) <= 0 -> BigDecimal("5.0000")
            else -> BigDecimal.ZERO
        }
        score = score.add(volPoints)

        return clampScore(score)
    }

    fun classifyConfidenceLevel(score: BigDecimal): ForecastConfidenceLevel {
        val s = scaleMoney(score)
        return when {
            s.compareTo(BigDecimal("80.0000")) >= 0 -> ForecastConfidenceLevel.HIGH
            s.compareTo(BigDecimal("50.0000")) >= 0 -> ForecastConfidenceLevel.MODERATE
            s.compareTo(BigDecimal("20.0000")) >= 0 -> ForecastConfidenceLevel.LOW
            else -> ForecastConfidenceLevel.INSUFFICIENT_DATA
        }
    }

    /**
     * Deterministic Forward-Looking Risk Classification.
     */
    fun classifyRiskLevel(
        projectedGrossProfit: BigDecimal,
        projectedMargin: BigDecimal?,
        marginDelta: BigDecimal?,
        costGrowthPercentage: BigDecimal?
    ): ForecastRiskLevel {
        if (projectedGrossProfit.compareTo(BigDecimal.ZERO) < 0) {
            return ForecastRiskLevel.VERY_HIGH
        }
        val margin = projectedMargin ?: return ForecastRiskLevel.DATA_INSUFFICIENT
        val mDelta = marginDelta ?: BigDecimal.ZERO
        val costGrowth = costGrowthPercentage ?: BigDecimal.ZERO

        return when {
            margin.compareTo(BigDecimal("5.0000")) < 0 || mDelta.compareTo(BigDecimal("-15.0000")) < 0 || costGrowth.compareTo(BigDecimal("30.0000")) > 0 -> ForecastRiskLevel.HIGH
            margin.compareTo(BigDecimal("15.0000")) < 0 || mDelta.compareTo(BigDecimal("-5.0000")) < 0 || costGrowth.compareTo(BigDecimal("15.0000")) > 0 -> ForecastRiskLevel.MODERATE
            margin.compareTo(BigDecimal("30.0000")) >= 0 && mDelta.compareTo(BigDecimal.ZERO) >= 0 -> ForecastRiskLevel.VERY_LOW
            else -> ForecastRiskLevel.LOW
        }
    }

    fun clampScore(score: BigDecimal): BigDecimal {
        return when {
            score.compareTo(BigDecimal.ZERO) < 0 -> BigDecimal.ZERO.setScale(DEFAULT_SCALE, ROUNDING_MODE)
            score.compareTo(ONE_HUNDRED) > 0 -> ONE_HUNDRED
            else -> score.setScale(DEFAULT_SCALE, ROUNDING_MODE)
        }
    }

    /**
     * Generates SHA-256 Provenance Fingerprint.
     */
    fun generateProvenanceFingerprint(
        sourceModule: String,
        sourceEntityType: String,
        sourceEntityId: String,
        sourceTransactionId: String?,
        metricType: String,
        forecastMethod: String
    ): String {
        val payload = "$sourceModule:$sourceEntityType:$sourceEntityId:${sourceTransactionId ?: "NONE"}:$metricType:$forecastMethod"
        return sha256(payload)
    }

    /**
     * Generates SHA-256 Snapshot Integrity Hash over all projected and supporting components.
     */
    fun generateSnapshotIntegrityHash(
        forecastId: String,
        tenantId: String,
        projectId: String,
        targetScope: ProfitabilityForecastScope,
        targetEntityId: String,
        forecastMethod: ProfitabilityForecastMethod,
        scenarioType: ProfitabilityScenarioType,
        projectedRevenue: BigDecimal,
        projectedTotalCost: BigDecimal,
        projectedGrossProfit: BigDecimal,
        components: List<ProfitabilityForecastComponent>,
        assumptions: List<ProfitabilityScenarioAssumption>,
        provenanceRecords: List<ProfitabilityForecastProvenance>
    ): String {
        val sortedComponents = components.sortedBy { it.componentType.name }.joinToString(";") {
            "${it.componentType.name}:${scaleMoney(it.projectedAmount)}"
        }
        val sortedAssumptions = assumptions.sortedBy { it.parameterKey }.joinToString(";") {
            "${it.parameterKey}:${scaleMoney(it.adjustmentPercentage)}"
        }
        val sortedProvenance = provenanceRecords.sortedBy { it.fingerprint }.joinToString(";") {
            it.fingerprint
        }

        val raw = buildString {
            append(forecastId).append("|")
            append(tenantId).append("|")
            append(projectId).append("|")
            append(targetScope.name).append("|")
            append(targetEntityId).append("|")
            append(forecastMethod.name).append("|")
            append(scenarioType.name).append("|")
            append(scaleMoney(projectedRevenue)).append("|")
            append(scaleMoney(projectedTotalCost)).append("|")
            append(scaleMoney(projectedGrossProfit)).append("|")
            append(sortedComponents).append("|")
            append(sortedAssumptions).append("|")
            append(sortedProvenance)
        }
        return sha256(raw)
    }

    private fun sha256(input: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
