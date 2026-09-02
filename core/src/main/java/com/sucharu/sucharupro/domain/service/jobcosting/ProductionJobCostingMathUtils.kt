package com.sucharu.sucharupro.domain.service.jobcosting

import com.sucharu.sucharupro.domain.model.jobcosting.VarianceClassification
import java.math.BigDecimal
import java.math.RoundingMode
import java.security.MessageDigest

object ProductionJobCostingMathUtils {

    fun roundScale4(value: BigDecimal): BigDecimal {
        return value.setScale(4, RoundingMode.HALF_UP)
    }

    fun calculateVariance(actual: BigDecimal, planned: BigDecimal): BigDecimal {
        // Variance = Actual - Planned. If Actual > Planned for Cost, it is UNFAVORABLE (positive).
        return roundScale4(actual.subtract(planned))
    }

    fun calculateVariancePercentage(actual: BigDecimal, planned: BigDecimal): BigDecimal {
        if (planned.compareTo(BigDecimal.ZERO) == 0) {
            return if (actual.compareTo(BigDecimal.ZERO) == 0) roundScale4(BigDecimal.ZERO) else roundScale4(BigDecimal("100.0000"))
        }
        val diff = actual.subtract(planned)
        return roundScale4(diff.divide(planned, 6, RoundingMode.HALF_UP).multiply(BigDecimal("100.0000")))
    }

    fun classifyCostVariance(actual: BigDecimal, planned: BigDecimal): VarianceClassification {
        val diff = actual.subtract(planned)
        return when {
            diff.compareTo(BigDecimal("0.0010")) > 0 -> VarianceClassification.UNFAVORABLE // Higher cost than planned
            diff.compareTo(BigDecimal("-0.0010")) < 0 -> VarianceClassification.FAVORABLE // Lower cost than planned
            else -> VarianceClassification.NEUTRAL
        }
    }

    fun classifyRevenueOrProfitVariance(actual: BigDecimal, planned: BigDecimal): VarianceClassification {
        val diff = actual.subtract(planned)
        return when {
            diff.compareTo(BigDecimal("0.0010")) > 0 -> VarianceClassification.FAVORABLE // Higher profit than planned
            diff.compareTo(BigDecimal("-0.0010")) < 0 -> VarianceClassification.UNFAVORABLE // Lower profit than planned
            else -> VarianceClassification.NEUTRAL
        }
    }

    fun calculateUnitCost(totalCost: BigDecimal, quantity: BigDecimal): BigDecimal {
        if (quantity.compareTo(BigDecimal.ZERO) <= 0) return roundScale4(BigDecimal.ZERO)
        return roundScale4(totalCost.divide(quantity, 6, RoundingMode.HALF_UP))
    }

    fun calculateGrossMarginPercentage(sellingPrice: BigDecimal, totalCost: BigDecimal): BigDecimal {
        if (sellingPrice.compareTo(BigDecimal.ZERO) <= 0) return roundScale4(BigDecimal.ZERO)
        val profit = sellingPrice.subtract(totalCost)
        return roundScale4(profit.divide(sellingPrice, 6, RoundingMode.HALF_UP).multiply(BigDecimal("100.0000")))
    }

    fun generateJobCostCertificateHash(
        tenantId: String,
        executionJobId: String,
        orderId: String,
        actualTotalCost: BigDecimal,
        estimatedTotalCost: BigDecimal,
        totalCostVariance: BigDecimal,
        actualUnitCost: BigDecimal,
        reconciledAt: Long,
        reconciledBy: String
    ): String {
        val payload = "$tenantId|$executionJobId|$orderId|${roundScale4(actualTotalCost)}|${roundScale4(estimatedTotalCost)}|${roundScale4(totalCostVariance)}|${roundScale4(actualUnitCost)}|$reconciledAt|$reconciledBy"
        val bytes = MessageDigest.getInstance("SHA-256").digest(payload.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
