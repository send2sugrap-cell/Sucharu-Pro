package com.sucharu.sucharupro.domain.model.profitability

import java.math.BigDecimal
import java.math.RoundingMode
import java.security.MessageDigest

/**
 * Precision Mathematics, Hashing & Deterministic Scoring for Executive Profitability Engine.
 * Module 16 Step 10.
 */
object ExecutiveProfitabilityMathUtils {

    val ZERO: BigDecimal = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP)
    val ONE_HUNDRED: BigDecimal = BigDecimal("100.0000").setScale(4, RoundingMode.HALF_UP)

    fun scale4(value: BigDecimal?): BigDecimal {
        return (value ?: ZERO).setScale(4, RoundingMode.HALF_UP)
    }

    fun calculateMargin(profit: BigDecimal, revenue: BigDecimal): BigDecimal {
        val sProfit = scale4(profit)
        val sRevenue = scale4(revenue)
        if (sRevenue.compareTo(ZERO) <= 0) return ZERO
        return sProfit.multiply(ONE_HUNDRED).divide(sRevenue, 4, RoundingMode.HALF_UP)
    }

    fun calculateVariance(current: BigDecimal, previous: BigDecimal?): BigDecimal {
        val sCurr = scale4(current)
        val sPrev = scale4(previous ?: ZERO)
        return sCurr.subtract(sPrev)
    }

    fun calculateVariancePercentage(current: BigDecimal, previous: BigDecimal?): BigDecimal? {
        if (previous == null || scale4(previous).compareTo(ZERO) == 0) return null
        val diff = scale4(current).subtract(scale4(previous))
        return diff.multiply(ONE_HUNDRED).divide(scale4(previous).abs(), 4, RoundingMode.HALF_UP)
    }

    fun determineVarianceDirection(variancePercentage: BigDecimal?, isHigherBetter: Boolean = true): KpiDirection {
        if (variancePercentage == null) return KpiDirection.NEUTRAL
        val threshold = BigDecimal("0.5000") // 0.5% threshold for stability
        return when {
            variancePercentage.abs() < threshold -> KpiDirection.STABLE
            variancePercentage > ZERO -> if (isHigherBetter) KpiDirection.IMPROVING else KpiDirection.DETERIORATING
            else -> if (isHigherBetter) KpiDirection.DETERIORATING else KpiDirection.IMPROVING
        }
    }

    fun classifyMarginHealth(marginPercentage: BigDecimal): KpiHealthClassification {
        val m = scale4(marginPercentage)
        return when {
            m >= BigDecimal("30.0000") -> KpiHealthClassification.EXCELLENT
            m >= BigDecimal("20.0000") -> KpiHealthClassification.HEALTHY
            m >= BigDecimal("10.0000") -> KpiHealthClassification.STABLE
            m >= BigDecimal("5.0000") -> KpiHealthClassification.WATCH
            m >= ZERO -> KpiHealthClassification.WARNING
            else -> KpiHealthClassification.CRITICAL
        }
    }

    fun classifyScoreHealth(score: BigDecimal): KpiHealthClassification {
        val s = clampScore(score)
        return when {
            s >= BigDecimal("85.0000") -> KpiHealthClassification.EXCELLENT
            s >= BigDecimal("70.0000") -> KpiHealthClassification.HEALTHY
            s >= BigDecimal("55.0000") -> KpiHealthClassification.STABLE
            s >= BigDecimal("40.0000") -> KpiHealthClassification.WATCH
            s >= BigDecimal("25.0000") -> KpiHealthClassification.WARNING
            else -> KpiHealthClassification.CRITICAL
        }
    }

    fun clampScore(value: BigDecimal): BigDecimal {
        val s = scale4(value)
        return s.coerceIn(ZERO, ONE_HUNDRED)
    }

    fun calculateWeightedScore(rawScore: BigDecimal, weight: BigDecimal): BigDecimal {
        val sRaw = clampScore(rawScore)
        val sWeight = scale4(weight)
        return sRaw.multiply(sWeight).setScale(4, RoundingMode.HALF_UP)
    }

    fun calculateConcentrationRisk(top1: BigDecimal, top5: BigDecimal): ForecastRiskLevel {
        val sTop1 = scale4(top1)
        val sTop5 = scale4(top5)
        return when {
            sTop1 >= BigDecimal("40.0000") || sTop5 >= BigDecimal("80.0000") -> ForecastRiskLevel.VERY_HIGH
            sTop1 >= BigDecimal("25.0000") || sTop5 >= BigDecimal("65.0000") -> ForecastRiskLevel.HIGH
            sTop1 >= BigDecimal("15.0000") || sTop5 >= BigDecimal("50.0000") -> ForecastRiskLevel.MODERATE
            else -> ForecastRiskLevel.LOW
        }
    }

    fun sha256(input: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    fun generateExecutiveSnapshotFingerprint(
        tenantId: String,
        projectId: String,
        periodId: String?,
        revenue: BigDecimal,
        cost: BigDecimal,
        profit: BigDecimal
    ): String {
        val raw = "EXEC-FP:$tenantId:$projectId:${periodId ?: "ALL"}:${scale4(revenue)}:${scale4(cost)}:${scale4(profit)}"
        return sha256(raw)
    }

    fun generateExecutiveSnapshotIntegrityHash(
        snapshotId: String,
        tenantId: String,
        projectId: String,
        periodId: String?,
        revenue: BigDecimal,
        cost: BigDecimal,
        profit: BigDecimal,
        overallScore: BigDecimal,
        health: KpiHealthClassification,
        fingerprint: String
    ): String {
        val raw = "EXEC-HASH:$snapshotId:$tenantId:$projectId:${periodId ?: "ALL"}:${scale4(revenue)}:${scale4(cost)}:${scale4(profit)}:${scale4(overallScore)}:${health.name}:$fingerprint"
        return sha256(raw)
    }

    fun generateReportIntegrityHash(
        reportId: String,
        tenantId: String,
        projectId: String,
        periodId: String?,
        generatedAt: Long,
        overallScore: BigDecimal
    ): String {
        val raw = "EXEC-REP-HASH:$reportId:$tenantId:$projectId:${periodId ?: "ALL"}:$generatedAt:${scale4(overallScore)}"
        return sha256(raw)
    }

    fun generateHandoffIntegrityHash(
        handoffId: String,
        tenantId: String,
        projectId: String,
        periodId: String?,
        overallScore: BigDecimal,
        health: KpiHealthClassification,
        generatedAt: Long
    ): String {
        val raw = "EXEC-HANDOFF-HASH:$handoffId:$tenantId:$projectId:${periodId ?: "ALL"}:${scale4(overallScore)}:${health.name}:$generatedAt"
        return sha256(raw)
    }
}
