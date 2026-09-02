package com.sucharu.sucharupro.domain.model.profitability

import java.math.BigDecimal
import java.math.RoundingMode
import java.security.MessageDigest

/**
 * Mathematical Engine, Zero-Safe Utilities, Explainable Efficiency Scoring, Risk Analysis, and Cryptographic Integrity Hashing for Vendor Profitability.
 * Module 16 Step 05.
 */
object VendorProfitabilityMathUtils {

    const val SCALE = 4
    val ROUNDING = RoundingMode.HALF_UP
    private val HUNDRED = BigDecimal("100.0000")

    fun calculateCostPerJob(totalCost: BigDecimal, jobCount: Int): BigDecimal? {
        if (jobCount <= 0) return null
        return totalCost.divide(BigDecimal.valueOf(jobCount.toLong()), SCALE, ROUNDING)
    }

    fun calculateCostPerUnit(totalCost: BigDecimal, quantity: Long): BigDecimal? {
        if (quantity <= 0L) return null
        return totalCost.divide(BigDecimal.valueOf(quantity), SCALE, ROUNDING)
    }

    fun calculateCostSharePercentage(vendorCost: BigDecimal, totalJobCost: BigDecimal): BigDecimal? {
        if (totalJobCost <= BigDecimal.ZERO) return null
        return vendorCost.multiply(HUNDRED).divide(totalJobCost, SCALE, ROUNDING)
    }

    fun calculateCostToRevenueContextPercentage(vendorCost: BigDecimal, revenueContext: BigDecimal): BigDecimal? {
        if (revenueContext <= BigDecimal.ZERO) return null
        return vendorCost.multiply(HUNDRED).divide(revenueContext, SCALE, ROUNDING)
    }

    fun calculateCostVariance(actualCost: BigDecimal, baselineCost: BigDecimal?): Pair<BigDecimal?, BigDecimal?> {
        if (baselineCost == null || baselineCost <= BigDecimal.ZERO) {
            return Pair(null, null)
        }
        val varianceAmount = actualCost.subtract(baselineCost).setScale(SCALE, ROUNDING)
        val variancePercentage = varianceAmount.multiply(HUNDRED).divide(baselineCost, SCALE, ROUNDING)
        return Pair(varianceAmount, variancePercentage)
    }

    fun calculateFulfillmentProfitabilityImpact(revenueContext: BigDecimal, totalJobCost: BigDecimal): BigDecimal {
        return revenueContext.subtract(totalJobCost).setScale(SCALE, ROUNDING)
    }

    fun calculateReworkRate(reworkCount: Int, jobCount: Int): BigDecimal? {
        if (jobCount <= 0) return null
        return BigDecimal.valueOf(reworkCount.toLong())
            .multiply(HUNDRED)
            .divide(BigDecimal.valueOf(jobCount.toLong()), SCALE, ROUNDING)
    }

    fun calculateQualityFailureRate(failureCount: Int, jobCount: Int): BigDecimal? {
        if (jobCount <= 0) return null
        return BigDecimal.valueOf(failureCount.toLong())
            .multiply(HUNDRED)
            .divide(BigDecimal.valueOf(jobCount.toLong()), SCALE, ROUNDING)
    }

    /**
     * Deterministic, explainable vendor efficiency score (0.0000 to 100.0000).
     * Dimensions:
     * - Cost variance (30% weight): 30 pts if on/under budget, penalized for overrun.
     * - Rework rate (25% weight): 25 pts if 0% rework, penalized as rework rate grows.
     * - Quality failure rate (20% weight): 20 pts if 0% failure, penalized for failures.
     * - Dispute frequency (15% weight): 15 pts if 0 disputes, -5 per dispute.
     * - Payment exposure/SLA (10% weight): 10 pts baseline.
     */
    fun calculateEfficiencyScoreBreakdown(
        costVariancePercentage: BigDecimal?,
        reworkRate: BigDecimal?,
        qualityFailureRate: BigDecimal?,
        disputeCount: Int,
        outstandingExposure: BigDecimal,
        totalVendorCost: BigDecimal
    ): VendorEfficiencyScoreBreakdown {
        val explanations = mutableListOf<String>()

        // 1. Cost Variance (Max 30)
        var varScore = BigDecimal("30.0000")
        if (costVariancePercentage != null) {
            if (costVariancePercentage > BigDecimal.ZERO) {
                val penalty = costVariancePercentage.multiply(BigDecimal("0.5000")).setScale(SCALE, ROUNDING)
                varScore = varScore.subtract(penalty).coerceAtLeast(BigDecimal.ZERO)
                explanations.add("Cost variance overrun penalty: -${penalty.stripTrailingZeros().toPlainString()} pts")
            } else {
                explanations.add("Cost variance on/under budget: +30.00 pts")
            }
        } else {
            varScore = BigDecimal("25.0000")
            explanations.add("Baseline cost unavailable, standard default score: 25.00 pts")
        }

        // 2. Rework Rate (Max 25)
        var reworkScore = BigDecimal("25.0000")
        if (reworkRate != null) {
            val penalty = reworkRate.multiply(BigDecimal("2.5000")).setScale(SCALE, ROUNDING)
            reworkScore = reworkScore.subtract(penalty).coerceAtLeast(BigDecimal.ZERO)
            if (penalty > BigDecimal.ZERO) {
                explanations.add("Rework rate (${reworkRate.stripTrailingZeros().toPlainString()}%) penalty: -${penalty.stripTrailingZeros().toPlainString()} pts")
            } else {
                explanations.add("Zero rework incidents: +25.00 pts")
            }
        } else {
            explanations.add("No job rework recorded: +25.00 pts")
        }

        // 3. Quality Failure Rate (Max 20)
        var qualityScore = BigDecimal("20.0000")
        if (qualityFailureRate != null) {
            val penalty = qualityFailureRate.multiply(BigDecimal("2.0000")).setScale(SCALE, ROUNDING)
            qualityScore = qualityScore.subtract(penalty).coerceAtLeast(BigDecimal.ZERO)
            if (penalty > BigDecimal.ZERO) {
                explanations.add("Quality failure rate (${qualityFailureRate.stripTrailingZeros().toPlainString()}%) penalty: -${penalty.stripTrailingZeros().toPlainString()} pts")
            } else {
                explanations.add("Zero quality failures: +20.00 pts")
            }
        } else {
            explanations.add("No quality failures recorded: +20.00 pts")
        }

        // 4. Dispute Frequency (Max 15)
        var disputeScore = BigDecimal("15.0000")
        if (disputeCount > 0) {
            val penalty = BigDecimal.valueOf(disputeCount.toLong()).multiply(BigDecimal("5.0000")).setScale(SCALE, ROUNDING)
            disputeScore = disputeScore.subtract(penalty).coerceAtLeast(BigDecimal.ZERO)
            explanations.add("$disputeCount dispute(s) detected: -${penalty.stripTrailingZeros().toPlainString()} pts")
        } else {
            explanations.add("Zero financial disputes: +15.00 pts")
        }

        // 5. Payment Exposure (Max 10)
        var exposureScore = BigDecimal("10.0000")
        if (totalVendorCost > BigDecimal.ZERO && outstandingExposure > totalVendorCost.multiply(BigDecimal("0.7500"))) {
            exposureScore = BigDecimal("5.0000")
            explanations.add("High outstanding liability exposure (>75%): 5.00 pts")
        } else {
            explanations.add("Healthy liability and settlement profile: +10.00 pts")
        }

        val total = varScore.add(reworkScore).add(qualityScore).add(disputeScore).add(exposureScore)
            .coerceIn(BigDecimal.ZERO, HUNDRED)
            .setScale(SCALE, ROUNDING)

        return VendorEfficiencyScoreBreakdown(
            totalScore = total,
            costVarianceScore = varScore,
            unitCostScore = varScore,
            reworkRateScore = reworkScore,
            qualityFailureScore = qualityScore,
            disputeScore = disputeScore,
            deliverySlaScore = BigDecimal("10.0000"),
            paymentExposureScore = exposureScore,
            explanations = explanations
        )
    }

    fun classifyRisk(
        efficiencyScore: BigDecimal,
        costVariancePercentage: BigDecimal?,
        reworkCount: Int,
        disputeCount: Int,
        qualityFailureCount: Int
    ): Pair<VendorRiskClassification, List<String>> {
        val reasons = mutableListOf<String>()

        if (disputeCount >= 3 || (costVariancePercentage != null && costVariancePercentage >= BigDecimal("30.0000")) || efficiencyScore < BigDecimal("40.0000")) {
            if (disputeCount >= 3) reasons.add("Critical dispute threshold reached ($disputeCount disputes)")
            if (costVariancePercentage != null && costVariancePercentage >= BigDecimal("30.0000")) reasons.add("Severe cost overrun (${costVariancePercentage.stripTrailingZeros().toPlainString()}%)")
            if (efficiencyScore < BigDecimal("40.0000")) reasons.add("Low efficiency score (${efficiencyScore.stripTrailingZeros().toPlainString()}/100)")
            return Pair(VendorRiskClassification.CRITICAL_RISK, reasons)
        }

        if (disputeCount >= 1 || reworkCount >= 3 || qualityFailureCount >= 2 || (costVariancePercentage != null && costVariancePercentage >= BigDecimal("15.0000")) || efficiencyScore < BigDecimal("65.0000")) {
            if (disputeCount >= 1) reasons.add("Active financial disputes ($disputeCount)")
            if (reworkCount >= 3) reasons.add("Elevated rework instances ($reworkCount)")
            if (qualityFailureCount >= 2) reasons.add("Repeated quality failures ($qualityFailureCount)")
            if (costVariancePercentage != null && costVariancePercentage >= BigDecimal("15.0000")) reasons.add("Moderate cost overrun (${costVariancePercentage.stripTrailingZeros().toPlainString()}%)")
            if (efficiencyScore < BigDecimal("65.0000")) reasons.add("Moderate efficiency score (${efficiencyScore.stripTrailingZeros().toPlainString()}/100)")
            return Pair(VendorRiskClassification.HIGH_RISK, reasons)
        }

        if (reworkCount >= 1 || qualityFailureCount >= 1 || (costVariancePercentage != null && costVariancePercentage >= BigDecimal("5.0000")) || efficiencyScore < BigDecimal("80.0000")) {
            if (reworkCount >= 1) reasons.add("Minor rework recorded")
            if (qualityFailureCount >= 1) reasons.add("Minor quality failure recorded")
            if (costVariancePercentage != null && costVariancePercentage >= BigDecimal("5.0000")) reasons.add("Minor cost overrun (${costVariancePercentage.stripTrailingZeros().toPlainString()}%)")
            return Pair(VendorRiskClassification.MODERATE_RISK, reasons)
        }

        reasons.add("Stable operations, on-budget fulfillment, and high efficiency score")
        return Pair(VendorRiskClassification.LOW_RISK, reasons)
    }

    fun classifyDependency(vendorSpend: BigDecimal, totalSpend: BigDecimal): Pair<VendorDependencyClassification, BigDecimal?> {
        if (totalSpend <= BigDecimal.ZERO) return Pair(VendorDependencyClassification.INSUFFICIENT_DATA, null)
        val share = vendorSpend.multiply(HUNDRED).divide(totalSpend, SCALE, ROUNDING)
        val classification = when {
            share >= BigDecimal("25.0000") -> VendorDependencyClassification.CRITICAL_DEPENDENCY
            share >= BigDecimal("15.0000") -> VendorDependencyClassification.HIGH_DEPENDENCY
            share >= BigDecimal("5.0000") -> VendorDependencyClassification.MODERATE_DEPENDENCY
            else -> VendorDependencyClassification.LOW_DEPENDENCY
        }
        return Pair(classification, share)
    }

    fun determineTrend(currentCost: BigDecimal, previousCost: BigDecimal?): VendorTrendDirection {
        if (previousCost == null || previousCost <= BigDecimal.ZERO) {
            return VendorTrendDirection.INSUFFICIENT_DATA
        }
        val diff = currentCost.subtract(previousCost)
        val changePct = diff.multiply(HUNDRED).divide(previousCost, SCALE, ROUNDING)

        // For cost: declining cost is improving, increasing cost is declining
        return when {
            changePct <= BigDecimal("-5.0000") -> VendorTrendDirection.STRONGLY_IMPROVING
            changePct <= BigDecimal("-1.0000") -> VendorTrendDirection.IMPROVING
            changePct >= BigDecimal("5.0000") -> VendorTrendDirection.STRONGLY_DECLINING
            changePct >= BigDecimal("1.0000") -> VendorTrendDirection.DECLINING
            else -> VendorTrendDirection.STABLE
        }
    }

    fun generateProvenanceFingerprint(
        sourceModule: String,
        sourceEntityType: String,
        sourceEntityId: String,
        sourceTransactionId: String?,
        vendorId: String,
        componentType: JobCostComponentType
    ): String {
        val payload = "$sourceModule:$sourceEntityType:$sourceEntityId:${sourceTransactionId ?: ""}:$vendorId:${componentType.name}"
        return sha256(payload)
    }

    fun calculateSnapshotIntegrityHash(
        tenantId: String,
        projectId: String,
        vendorId: String,
        periodId: String?,
        totalVendorCost: BigDecimal,
        paidVendorCost: BigDecimal,
        outstandingExposure: BigDecimal,
        attributedRevenueContext: BigDecimal,
        efficiencyScore: BigDecimal,
        fingerprints: List<String>
    ): String {
        val sortedFp = fingerprints.sorted().joinToString(",")
        val payload = listOf(
            tenantId,
            projectId,
            vendorId,
            periodId ?: "ALL",
            totalVendorCost.setScale(SCALE, ROUNDING).toPlainString(),
            paidVendorCost.setScale(SCALE, ROUNDING).toPlainString(),
            outstandingExposure.setScale(SCALE, ROUNDING).toPlainString(),
            attributedRevenueContext.setScale(SCALE, ROUNDING).toPlainString(),
            efficiencyScore.setScale(SCALE, ROUNDING).toPlainString(),
            sortedFp
        ).joinToString("|")
        return sha256(payload)
    }

    private fun sha256(input: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(input.toByteArray(Charsets.UTF_8))
        return hash.joinToString("") { "%02x".format(it) }
    }
}
