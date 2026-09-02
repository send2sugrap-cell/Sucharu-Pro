package com.sucharu.sucharupro.domain.service.vendor

import com.sucharu.sucharupro.domain.model.vendor.ComplianceRiskLevel
import com.sucharu.sucharupro.domain.model.vendor.ComplianceStatus
import com.sucharu.sucharupro.domain.model.vendor.KpiDirection
import com.sucharu.sucharupro.domain.model.vendor.MeasurementConfidenceState
import com.sucharu.sucharupro.domain.model.vendor.MetricCalculationResult
import com.sucharu.sucharupro.domain.model.vendor.PerformanceRating
import com.sucharu.sucharupro.domain.model.vendor.VendorPerformanceScorecardItem
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Instant
import java.time.temporal.ChronoUnit
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * Deterministic calculation engine for vendor performance metrics and weighted scoring.
 */
object VendorPerformanceCalculator {

    const val ENGINE_VERSION = "1.0"
    private const val DEFAULT_MIN_SAMPLE_SIZE = 3

    /**
     * Deterministically rounds a double to 2 decimal places using HALF_UP.
     */
    fun round(value: Double, decimals: Int = 2): Double {
        if (value.isNaN() || value.isInfinite()) return 0.0
        return BigDecimal.valueOf(value).setScale(decimals, RoundingMode.HALF_UP).toDouble()
    }

    /**
     * On-Time Delivery % = (on-time deliveries / total eligible deliveries) * 100
     */
    fun calculateOnTimeDeliveryRate(
        onTimeDeliveries: Double,
        totalDeliveries: Double,
        minSampleSize: Int = DEFAULT_MIN_SAMPLE_SIZE
    ): MetricCalculationResult {
        if (totalDeliveries <= 0.0) {
            return MetricCalculationResult(
                value = 0.0,
                numerator = 0.0,
                denominator = 0.0,
                unit = "%",
                sampleSize = 0,
                confidenceState = MeasurementConfidenceState.NO_DATA,
                calculationVersion = ENGINE_VERSION
            )
        }
        val sampleSize = totalDeliveries.toInt()
        val rawValue = (min(onTimeDeliveries, totalDeliveries) / totalDeliveries) * 100.0
        val confidence = if (sampleSize < minSampleSize) MeasurementConfidenceState.LOW_SAMPLE_SIZE else MeasurementConfidenceState.SUFFICIENT_DATA
        return MetricCalculationResult(
            value = round(rawValue),
            numerator = onTimeDeliveries,
            denominator = totalDeliveries,
            unit = "%",
            sampleSize = sampleSize,
            confidenceState = confidence,
            calculationVersion = ENGINE_VERSION
        )
    }

    /**
     * Quality Acceptance % = (accepted quantity / inspected quantity) * 100
     */
    fun calculateQualityAcceptanceRate(
        acceptedQuantity: Double,
        inspectedQuantity: Double,
        minSampleSize: Int = DEFAULT_MIN_SAMPLE_SIZE
    ): MetricCalculationResult {
        if (inspectedQuantity <= 0.0) {
            return MetricCalculationResult(
                value = 0.0,
                numerator = 0.0,
                denominator = 0.0,
                unit = "%",
                sampleSize = 0,
                confidenceState = MeasurementConfidenceState.NO_DATA,
                calculationVersion = ENGINE_VERSION
            )
        }
        val sampleSize = inspectedQuantity.toInt()
        val rawValue = (min(acceptedQuantity, inspectedQuantity) / inspectedQuantity) * 100.0
        val confidence = if (sampleSize < minSampleSize) MeasurementConfidenceState.LOW_SAMPLE_SIZE else MeasurementConfidenceState.SUFFICIENT_DATA
        return MetricCalculationResult(
            value = round(rawValue),
            numerator = acceptedQuantity,
            denominator = inspectedQuantity,
            unit = "%",
            sampleSize = sampleSize,
            confidenceState = confidence,
            calculationVersion = ENGINE_VERSION
        )
    }

    /**
     * Defect Rate % = (defective quantity / inspected quantity) * 100
     */
    fun calculateDefectRate(
        defectiveQuantity: Double,
        inspectedQuantity: Double,
        minSampleSize: Int = DEFAULT_MIN_SAMPLE_SIZE
    ): MetricCalculationResult {
        if (inspectedQuantity <= 0.0) {
            return MetricCalculationResult(
                value = 0.0,
                numerator = 0.0,
                denominator = 0.0,
                unit = "%",
                sampleSize = 0,
                confidenceState = MeasurementConfidenceState.NO_DATA,
                calculationVersion = ENGINE_VERSION
            )
        }
        val sampleSize = inspectedQuantity.toInt()
        val rawValue = (min(defectiveQuantity, inspectedQuantity) / inspectedQuantity) * 100.0
        val confidence = if (sampleSize < minSampleSize) MeasurementConfidenceState.LOW_SAMPLE_SIZE else MeasurementConfidenceState.SUFFICIENT_DATA
        return MetricCalculationResult(
            value = round(rawValue),
            numerator = defectiveQuantity,
            denominator = inspectedQuantity,
            unit = "%",
            sampleSize = sampleSize,
            confidenceState = confidence,
            calculationVersion = ENGINE_VERSION
        )
    }

    /**
     * Rejection Rate % = (rejected quantity / received quantity) * 100
     */
    fun calculateRejectionRate(
        rejectedQuantity: Double,
        receivedQuantity: Double,
        minSampleSize: Int = DEFAULT_MIN_SAMPLE_SIZE
    ): MetricCalculationResult {
        if (receivedQuantity <= 0.0) {
            return MetricCalculationResult(
                value = 0.0,
                numerator = 0.0,
                denominator = 0.0,
                unit = "%",
                sampleSize = 0,
                confidenceState = MeasurementConfidenceState.NO_DATA,
                calculationVersion = ENGINE_VERSION
            )
        }
        val sampleSize = receivedQuantity.toInt()
        val rawValue = (min(rejectedQuantity, receivedQuantity) / receivedQuantity) * 100.0
        val confidence = if (sampleSize < minSampleSize) MeasurementConfidenceState.LOW_SAMPLE_SIZE else MeasurementConfidenceState.SUFFICIENT_DATA
        return MetricCalculationResult(
            value = round(rawValue),
            numerator = rejectedQuantity,
            denominator = receivedQuantity,
            unit = "%",
            sampleSize = sampleSize,
            confidenceState = confidence,
            calculationVersion = ENGINE_VERSION
        )
    }

    /**
     * Dispute Rate % = (disputed orders / total orders) * 100
     */
    fun calculateDisputeRate(
        disputedOrders: Double,
        totalOrders: Double,
        minSampleSize: Int = DEFAULT_MIN_SAMPLE_SIZE
    ): MetricCalculationResult {
        if (totalOrders <= 0.0) {
            return MetricCalculationResult(
                value = 0.0,
                numerator = 0.0,
                denominator = 0.0,
                unit = "%",
                sampleSize = 0,
                confidenceState = MeasurementConfidenceState.NO_DATA,
                calculationVersion = ENGINE_VERSION
            )
        }
        val sampleSize = totalOrders.toInt()
        val rawValue = (min(disputedOrders, totalOrders) / totalOrders) * 100.0
        val confidence = if (sampleSize < minSampleSize) MeasurementConfidenceState.LOW_SAMPLE_SIZE else MeasurementConfidenceState.SUFFICIENT_DATA
        return MetricCalculationResult(
            value = round(rawValue),
            numerator = disputedOrders,
            denominator = totalOrders,
            unit = "%",
            sampleSize = sampleSize,
            confidenceState = confidence,
            calculationVersion = ENGINE_VERSION
        )
    }

    /**
     * PO Fulfillment Rate % = (fulfilled quantity / ordered quantity) * 100
     */
    fun calculatePoFulfillmentRate(
        fulfilledQuantity: Double,
        orderedQuantity: Double,
        minSampleSize: Int = DEFAULT_MIN_SAMPLE_SIZE
    ): MetricCalculationResult {
        if (orderedQuantity <= 0.0) {
            return MetricCalculationResult(
                value = 0.0,
                numerator = 0.0,
                denominator = 0.0,
                unit = "%",
                sampleSize = 0,
                confidenceState = MeasurementConfidenceState.NO_DATA,
                calculationVersion = ENGINE_VERSION
            )
        }
        val sampleSize = orderedQuantity.toInt()
        val rawValue = (min(fulfilledQuantity, orderedQuantity) / orderedQuantity) * 100.0
        val confidence = if (sampleSize < minSampleSize) MeasurementConfidenceState.LOW_SAMPLE_SIZE else MeasurementConfidenceState.SUFFICIENT_DATA
        return MetricCalculationResult(
            value = round(rawValue),
            numerator = fulfilledQuantity,
            denominator = orderedQuantity,
            unit = "%",
            sampleSize = sampleSize,
            confidenceState = confidence,
            calculationVersion = ENGINE_VERSION
        )
    }

    /**
     * Invoice Match Rate % = (matched invoice lines / eligible invoice lines) * 100
     */
    fun calculateInvoiceMatchRate(
        matchedLines: Double,
        eligibleLines: Double,
        minSampleSize: Int = DEFAULT_MIN_SAMPLE_SIZE
    ): MetricCalculationResult {
        if (eligibleLines <= 0.0) {
            return MetricCalculationResult(
                value = 0.0,
                numerator = 0.0,
                denominator = 0.0,
                unit = "%",
                sampleSize = 0,
                confidenceState = MeasurementConfidenceState.NO_DATA,
                calculationVersion = ENGINE_VERSION
            )
        }
        val sampleSize = eligibleLines.toInt()
        val rawValue = (min(matchedLines, eligibleLines) / eligibleLines) * 100.0
        val confidence = if (sampleSize < minSampleSize) MeasurementConfidenceState.LOW_SAMPLE_SIZE else MeasurementConfidenceState.SUFFICIENT_DATA
        return MetricCalculationResult(
            value = round(rawValue),
            numerator = matchedLines,
            denominator = eligibleLines,
            unit = "%",
            sampleSize = sampleSize,
            confidenceState = confidence,
            calculationVersion = ENGINE_VERSION
        )
    }

    /**
     * Price Variance % = (absolute price variance / baseline amount) * 100
     */
    fun calculatePriceVarianceRate(
        varianceAmount: Double,
        referenceAmount: Double,
        minSampleSize: Int = 1
    ): MetricCalculationResult {
        if (referenceAmount <= 0.0) {
            return MetricCalculationResult(
                value = 0.0,
                numerator = 0.0,
                denominator = 0.0,
                unit = "%",
                sampleSize = 0,
                confidenceState = MeasurementConfidenceState.NO_DATA,
                calculationVersion = ENGINE_VERSION
            )
        }
        val rawValue = (abs(varianceAmount) / referenceAmount) * 100.0
        return MetricCalculationResult(
            value = round(rawValue),
            numerator = abs(varianceAmount),
            denominator = referenceAmount,
            unit = "%",
            sampleSize = minSampleSize,
            confidenceState = MeasurementConfidenceState.SUFFICIENT_DATA,
            calculationVersion = ENGINE_VERSION
        )
    }

    /**
     * Corrective Action Closure % = (closed actions / due actions) * 100
     */
    fun calculateCorrectiveActionClosureRate(
        closedActions: Double,
        dueActions: Double,
        minSampleSize: Int = 1
    ): MetricCalculationResult {
        if (dueActions <= 0.0) {
            return MetricCalculationResult(
                value = 100.0, // No due corrective actions means 100% compliance
                numerator = 0.0,
                denominator = 0.0,
                unit = "%",
                sampleSize = 0,
                confidenceState = MeasurementConfidenceState.SUFFICIENT_DATA,
                calculationVersion = ENGINE_VERSION
            )
        }
        val rawValue = (min(closedActions, dueActions) / dueActions) * 100.0
        return MetricCalculationResult(
            value = round(rawValue),
            numerator = closedActions,
            denominator = dueActions,
            unit = "%",
            sampleSize = dueActions.toInt(),
            confidenceState = MeasurementConfidenceState.SUFFICIENT_DATA,
            calculationVersion = ENGINE_VERSION
        )
    }

    /**
     * Compliance Completion % = (verified mandatory requirements / total mandatory requirements) * 100
     */
    fun calculateComplianceCompletionRate(
        verifiedMandatory: Double,
        totalMandatory: Double
    ): MetricCalculationResult {
        if (totalMandatory <= 0.0) {
            return MetricCalculationResult(
                value = 100.0,
                numerator = 0.0,
                denominator = 0.0,
                unit = "%",
                sampleSize = 0,
                confidenceState = MeasurementConfidenceState.SUFFICIENT_DATA,
                calculationVersion = ENGINE_VERSION
            )
        }
        val rawValue = (min(verifiedMandatory, totalMandatory) / totalMandatory) * 100.0
        return MetricCalculationResult(
            value = round(rawValue),
            numerator = verifiedMandatory,
            denominator = totalMandatory,
            unit = "%",
            sampleSize = totalMandatory.toInt(),
            confidenceState = MeasurementConfidenceState.SUFFICIENT_DATA,
            calculationVersion = ENGINE_VERSION
        )
    }

    /**
     * Normalizes an actual KPI value into a 0.0 .. 100.0 score based on direction and targets.
     */
    fun normalizeScore(
        actual: Double,
        target: Double,
        direction: KpiDirection,
        minimumAcceptable: Double? = null,
        maximumAcceptable: Double? = null
    ): Double {
        val score = when (direction) {
            KpiDirection.HIGHER_IS_BETTER -> {
                if (minimumAcceptable != null && actual <= minimumAcceptable) {
                    0.0
                } else if (actual >= target) {
                    100.0
                } else if (minimumAcceptable != null && target > minimumAcceptable) {
                    ((actual - minimumAcceptable) / (target - minimumAcceptable)) * 100.0
                } else if (target > 0.0) {
                    (actual / target) * 100.0
                } else {
                    100.0
                }
            }
            KpiDirection.LOWER_IS_BETTER -> {
                if (maximumAcceptable != null && actual >= maximumAcceptable) {
                    0.0
                } else if (actual <= target) {
                    100.0
                } else if (maximumAcceptable != null && maximumAcceptable > target) {
                    ((maximumAcceptable - actual) / (maximumAcceptable - target)) * 100.0
                } else if (target > 0.0) {
                    max(0.0, 100.0 - ((actual - target) / target * 100.0))
                } else {
                    max(0.0, 100.0 - actual)
                }
            }
            KpiDirection.TARGET_IS_BEST -> {
                val diff = abs(actual - target)
                val tolerance = (maximumAcceptable ?: (target * 1.2)) - target
                if (tolerance <= 0.0) {
                    if (diff < 0.001) 100.0 else 0.0
                } else {
                    max(0.0, 100.0 - (diff / tolerance * 100.0))
                }
            }
        }
        return round(max(0.0, min(100.0, score)))
    }

    /**
     * Computes the overall weighted score from scorecard items:
     * Overall Score = SUM(Normalized Score * Weight) / SUM(Weight)
     */
    fun calculateOverallScore(items: List<VendorPerformanceScorecardItem>): Double {
        if (items.isEmpty()) return 0.0
        var totalWeight = 0.0
        var totalWeightedScore = 0.0
        for (item in items) {
            // Only consider items with non-zero weight
            if (item.weight > 0.0) {
                totalWeight += item.weight
                totalWeightedScore += item.normalizedScore * item.weight
            }
        }
        if (totalWeight <= 0.0) return 0.0
        return round(totalWeightedScore / totalWeight)
    }

    /**
     * Maps overall score (0.0 .. 100.0) to standard performance rating band.
     */
    fun mapScoreToRating(score: Double): PerformanceRating {
        return when {
            score >= 90.0 -> PerformanceRating.EXCELLENT
            score >= 75.0 -> PerformanceRating.GOOD
            score >= 60.0 -> PerformanceRating.ACCEPTABLE
            score >= 40.0 -> PerformanceRating.NEEDS_IMPROVEMENT
            else -> PerformanceRating.CRITICAL
        }
    }

    /**
     * Determines compliance status and risk level based on expiry date and threshold.
     */
    fun determineComplianceStatusAndRisk(
        expiryDate: Instant?,
        now: Instant = Instant.now(),
        warningThresholdDays: Long = 30L
    ): Pair<ComplianceStatus, ComplianceRiskLevel> {
        if (expiryDate == null) {
            return Pair(ComplianceStatus.VERIFIED, ComplianceRiskLevel.LOW)
        }
        val daysUntilExpiry = ChronoUnit.DAYS.between(now, expiryDate)
        return when {
            daysUntilExpiry < 0 -> Pair(ComplianceStatus.EXPIRED, ComplianceRiskLevel.CRITICAL)
            daysUntilExpiry <= warningThresholdDays -> Pair(ComplianceStatus.EXPIRING_SOON, ComplianceRiskLevel.HIGH)
            daysUntilExpiry <= 60 -> Pair(ComplianceStatus.VERIFIED, ComplianceRiskLevel.MEDIUM)
            else -> Pair(ComplianceStatus.VERIFIED, ComplianceRiskLevel.LOW)
        }
    }
}
