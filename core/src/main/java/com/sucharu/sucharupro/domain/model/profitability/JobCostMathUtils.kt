package com.sucharu.sucharupro.domain.model.profitability

import java.math.BigDecimal
import java.math.RoundingMode
import java.security.MessageDigest

/**
 * High-precision mathematical and hashing utilities for Job Actual Cost Engine (Module 16 Step 02).
 */
object JobCostMathUtils {

    const val SCALE = 4
    val ROUNDING_MODE = RoundingMode.HALF_UP
    private val ZERO_MONEY = BigDecimal.ZERO.setScale(SCALE, ROUNDING_MODE)
    private val ON_TARGET_THRESHOLD_PERCENT = BigDecimal("2.0000") // ±2%

    fun scaleMoney(amount: BigDecimal?): BigDecimal {
        if (amount == null) return ZERO_MONEY
        return amount.setScale(SCALE, ROUNDING_MODE)
    }

    fun calculateTotalDirectCost(components: List<JobCostComponent>): BigDecimal {
        val sum = components.filter { it.directness == CostDirectness.DIRECT }
            .fold(BigDecimal.ZERO) { acc, c -> acc.add(c.attributedAmount) }
        return scaleMoney(sum)
    }

    fun calculateTotalIndirectCost(components: List<JobCostComponent>): BigDecimal {
        val sum = components.filter { it.directness == CostDirectness.INDIRECT }
            .fold(BigDecimal.ZERO) { acc, c -> acc.add(c.attributedAmount) }
        return scaleMoney(sum)
    }

    fun calculateTotalActualCost(totalDirect: BigDecimal, totalIndirect: BigDecimal): BigDecimal {
        return scaleMoney(totalDirect.add(totalIndirect))
    }

    fun calculateVariance(
        actualCost: BigDecimal,
        estimatedCost: BigDecimal?
    ): JobCostVariance {
        val scaledActual = scaleMoney(actualCost)
        if (estimatedCost == null || estimatedCost.compareTo(BigDecimal.ZERO) <= 0) {
            return JobCostVariance(
                actualCost = scaledActual,
                estimatedCost = null,
                costVariance = null,
                costVariancePercentage = null,
                classification = CostVarianceClassification.BASELINE_UNAVAILABLE,
                explanation = "Estimated baseline cost is not available or is zero."
            )
        }

        val scaledEstimated = scaleMoney(estimatedCost)
        val variance = scaleMoney(scaledActual.subtract(scaledEstimated))
        val variancePercent = variance
            .multiply(BigDecimal("100"))
            .divide(scaledEstimated, SCALE, ROUNDING_MODE)

        val classification = when {
            variancePercent.compareTo(ON_TARGET_THRESHOLD_PERCENT.negate()) < 0 -> CostVarianceClassification.UNDER_BUDGET
            variancePercent.compareTo(ON_TARGET_THRESHOLD_PERCENT) > 0 -> CostVarianceClassification.OVER_BUDGET
            else -> CostVarianceClassification.ON_TARGET
        }

        val explanation = when (classification) {
            CostVarianceClassification.UNDER_BUDGET -> "Actual cost is ${variancePercent.abs()}% below estimated budget."
            CostVarianceClassification.OVER_BUDGET -> "Actual cost is ${variancePercent}% over estimated budget."
            CostVarianceClassification.ON_TARGET -> "Actual cost is within ±2% target tolerance."
            else -> ""
        }

        return JobCostVariance(
            actualCost = scaledActual,
            estimatedCost = scaledEstimated,
            costVariance = variance,
            costVariancePercentage = variancePercent,
            classification = classification,
            explanation = explanation
        )
    }

    fun generateFingerprint(
        sourceModule: String,
        sourceEntityType: String,
        sourceEntityId: String,
        sourceTransactionId: String?,
        costComponentType: JobCostComponentType
    ): String {
        return "${sourceModule.trim().uppercase()}:${sourceEntityType.trim().uppercase()}:${sourceEntityId.trim()}:${sourceTransactionId?.trim().orEmpty()}:${costComponentType.name}"
    }

    fun generateIntegrityHash(
        tenantId: String,
        projectId: String,
        jobId: String,
        calculationVersion: String,
        totalActualCost: BigDecimal,
        totalDirectCost: BigDecimal,
        totalIndirectCost: BigDecimal,
        componentHashes: List<String>
    ): String {
        val normalizedString = buildString {
            append("tenant:").append(tenantId).append("|")
            append("project:").append(projectId).append("|")
            append("job:").append(jobId).append("|")
            append("version:").append(calculationVersion).append("|")
            append("actual:").append(scaleMoney(totalActualCost)).append("|")
            append("direct:").append(scaleMoney(totalDirectCost)).append("|")
            append("indirect:").append(scaleMoney(totalIndirectCost)).append("|")
            append("components:").append(componentHashes.sorted().joinToString(","))
        }

        val digest = MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(normalizedString.toByteArray(Charsets.UTF_8))
        return hashBytes.joinToString("") { "%02x".format(it) }
    }
}
