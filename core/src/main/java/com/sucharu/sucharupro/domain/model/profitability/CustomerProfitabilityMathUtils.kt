package com.sucharu.sucharupro.domain.model.profitability

import java.math.BigDecimal
import java.math.RoundingMode
import java.security.MessageDigest

/**
 * High-precision mathematical and hashing engine for Customer Profitability & Contribution Analysis (Module 16 Step 04).
 */
object CustomerProfitabilityMathUtils {

    const val SCALE = 4
    val ROUNDING_MODE = RoundingMode.HALF_UP
    val ZERO_MONEY: BigDecimal = BigDecimal.ZERO.setScale(SCALE, ROUNDING_MODE)

    fun scaleMoney(amount: BigDecimal?): BigDecimal {
        if (amount == null) return ZERO_MONEY
        return amount.setScale(SCALE, ROUNDING_MODE)
    }

    fun calculateGrossProfit(revenue: BigDecimal, cost: BigDecimal): BigDecimal {
        val scaledRev = scaleMoney(revenue)
        val scaledCost = scaleMoney(cost)
        return scaleMoney(scaledRev.subtract(scaledCost))
    }

    fun calculateGrossMarginPercentage(revenue: BigDecimal, cost: BigDecimal): BigDecimal? {
        val scaledRev = scaleMoney(revenue)
        if (scaledRev.compareTo(BigDecimal.ZERO) <= 0) {
            return null
        }
        val gp = calculateGrossProfit(scaledRev, cost)
        return gp.multiply(BigDecimal("100"))
            .divide(scaledRev, SCALE, ROUNDING_MODE)
    }

    fun calculateCostToRevenuePercentage(totalCost: BigDecimal, revenue: BigDecimal): BigDecimal? {
        val scaledRev = scaleMoney(revenue)
        if (scaledRev.compareTo(BigDecimal.ZERO) <= 0) {
            return null
        }
        val scaledCost = scaleMoney(totalCost)
        return scaledCost.multiply(BigDecimal("100"))
            .divide(scaledRev, SCALE, ROUNDING_MODE)
    }

    fun calculateContributionAmount(revenue: BigDecimal, variableCost: BigDecimal): BigDecimal {
        val scaledRev = scaleMoney(revenue)
        val scaledVarCost = scaleMoney(variableCost)
        return scaleMoney(scaledRev.subtract(scaledVarCost))
    }

    fun calculateContributionMarginPercentage(revenue: BigDecimal, variableCost: BigDecimal): BigDecimal? {
        val scaledRev = scaleMoney(revenue)
        if (scaledRev.compareTo(BigDecimal.ZERO) <= 0) {
            return null
        }
        val contrib = calculateContributionAmount(scaledRev, variableCost)
        return contrib.multiply(BigDecimal("100"))
            .divide(scaledRev, SCALE, ROUNDING_MODE)
    }

    fun calculateAverageOrderValue(revenue: BigDecimal, orderCount: Int): BigDecimal? {
        if (orderCount <= 0) return null
        val scaledRev = scaleMoney(revenue)
        return scaledRev.divide(BigDecimal(orderCount).setScale(SCALE, ROUNDING_MODE), SCALE, ROUNDING_MODE)
    }

    fun calculateAverageJobValue(revenue: BigDecimal, jobCount: Int): BigDecimal? {
        if (jobCount <= 0) return null
        val scaledRev = scaleMoney(revenue)
        return scaledRev.divide(BigDecimal(jobCount).setScale(SCALE, ROUNDING_MODE), SCALE, ROUNDING_MODE)
    }

    fun calculateAverageRevenuePerUnit(revenue: BigDecimal, quantity: Int): BigDecimal? {
        if (quantity <= 0) return null
        val scaledRev = scaleMoney(revenue)
        return scaledRev.divide(BigDecimal(quantity).setScale(SCALE, ROUNDING_MODE), SCALE, ROUNDING_MODE)
    }

    fun calculateAverageCostPerUnit(totalCost: BigDecimal, quantity: Int): BigDecimal? {
        if (quantity <= 0) return null
        val scaledCost = scaleMoney(totalCost)
        return scaledCost.divide(BigDecimal(quantity).setScale(SCALE, ROUNDING_MODE), SCALE, ROUNDING_MODE)
    }

    fun calculateAverageProfitPerUnit(grossProfit: BigDecimal, quantity: Int): BigDecimal? {
        if (quantity <= 0) return null
        val scaledProfit = scaleMoney(grossProfit)
        return scaledProfit.divide(BigDecimal(quantity).setScale(SCALE, ROUNDING_MODE), SCALE, ROUNDING_MODE)
    }

    fun calculateSharePercentage(part: BigDecimal, total: BigDecimal): BigDecimal? {
        val scaledTotal = scaleMoney(total)
        if (scaledTotal.compareTo(BigDecimal.ZERO) <= 0) return null
        val scaledPart = scaleMoney(part)
        return scaledPart.multiply(BigDecimal("100")).divide(scaledTotal, SCALE, ROUNDING_MODE)
    }

    fun classifyCustomerProfitability(
        revenue: BigDecimal,
        totalCost: BigDecimal,
        grossMarginPercentage: BigDecimal?,
        isSourceReady: Boolean = true
    ): CustomerProfitabilityClassification {
        val scaledRev = scaleMoney(revenue)
        val scaledCost = scaleMoney(totalCost)

        if (!isSourceReady) return CustomerProfitabilityClassification.INSUFFICIENT_DATA
        if (scaledRev.compareTo(BigDecimal.ZERO) == 0 && scaledCost.compareTo(BigDecimal.ZERO) == 0) {
            return CustomerProfitabilityClassification.NO_REVENUE
        }
        if (grossMarginPercentage == null) {
            val gp = calculateGrossProfit(scaledRev, scaledCost)
            return when {
                gp.compareTo(BigDecimal.ZERO) > 0 -> CustomerProfitabilityClassification.LOW_MARGIN
                gp.compareTo(BigDecimal.ZERO) == 0 -> CustomerProfitabilityClassification.BREAK_EVEN
                else -> CustomerProfitabilityClassification.LOSS_MAKING
            }
        }

        return when {
            grossMarginPercentage.compareTo(BigDecimal("30.0000")) >= 0 -> CustomerProfitabilityClassification.HIGHLY_PROFITABLE
            grossMarginPercentage.compareTo(BigDecimal("15.0000")) >= 0 -> CustomerProfitabilityClassification.PROFITABLE
            grossMarginPercentage.compareTo(BigDecimal("0.0000")) > 0 -> CustomerProfitabilityClassification.LOW_MARGIN
            grossMarginPercentage.compareTo(BigDecimal("0.0000")) == 0 -> CustomerProfitabilityClassification.BREAK_EVEN
            else -> CustomerProfitabilityClassification.LOSS_MAKING
        }
    }

    fun calculateTrend(currentMargin: BigDecimal?, previousMargin: BigDecimal?): CustomerProfitabilityTrend {
        if (currentMargin == null || previousMargin == null) {
            return CustomerProfitabilityTrend.INSUFFICIENT_DATA
        }
        val diff = currentMargin.subtract(previousMargin)
        return when {
            diff.compareTo(BigDecimal("5.0000")) > 0 -> CustomerProfitabilityTrend.STRONGLY_IMPROVING
            diff.compareTo(BigDecimal("1.0000")) >= 0 -> CustomerProfitabilityTrend.IMPROVING
            diff.compareTo(BigDecimal("-1.0000")) >= 0 -> CustomerProfitabilityTrend.STABLE
            diff.compareTo(BigDecimal("-5.0000")) >= 0 -> CustomerProfitabilityTrend.DECLINING
            else -> CustomerProfitabilityTrend.STRONGLY_DECLINING
        }
    }

    fun assessConcentrationRisk(
        top1RevenueShare: BigDecimal,
        top5RevenueShare: BigDecimal
    ): CustomerConcentrationRisk {
        return when {
            top1RevenueShare.compareTo(BigDecimal("25.0000")) > 0 || top5RevenueShare.compareTo(BigDecimal("60.0000")) > 0 ->
                CustomerConcentrationRisk.CONCENTRATION_HIGH
            top1RevenueShare.compareTo(BigDecimal("10.0000")) > 0 || top5RevenueShare.compareTo(BigDecimal("30.0000")) > 0 ->
                CustomerConcentrationRisk.CONCENTRATION_MODERATE
            else -> CustomerConcentrationRisk.CONCENTRATION_LOW
        }
    }

    fun generateFingerprint(
        tenantId: String,
        customerId: String,
        sourceModule: String,
        sourceEntityType: String,
        sourceEntityId: String,
        sourceTransactionId: String?,
        componentType: String
    ): String {
        val raw = "${tenantId.trim()}:${customerId.trim()}:${sourceModule.trim().uppercase()}:${sourceEntityType.trim().uppercase()}:${sourceEntityId.trim()}:${sourceTransactionId?.trim().orEmpty()}:${componentType.trim()}"
        return sha256Hex(raw)
    }

    fun generateIntegrityHash(
        tenantId: String,
        projectId: String,
        customerId: String,
        periodType: String,
        calculationVersion: String,
        revenue: BigDecimal,
        cost: BigDecimal,
        grossProfit: BigDecimal,
        contribution: BigDecimal,
        components: List<CustomerCostBreakdownItem>,
        provenanceFingerprints: List<String>
    ): String {
        val sortedComps = components.sortedBy { it.componentType.name }
            .joinToString(";") { "${it.componentType.name}:${scaleMoney(it.amount)}:${scaleMoney(it.percentageOfTotalCost)}" }
        val sortedFps = provenanceFingerprints.sorted().joinToString(",")

        val payload = listOf(
            tenantId.trim(),
            projectId.trim(),
            customerId.trim(),
            periodType.trim(),
            calculationVersion.trim(),
            scaleMoney(revenue).toPlainString(),
            scaleMoney(cost).toPlainString(),
            scaleMoney(grossProfit).toPlainString(),
            scaleMoney(contribution).toPlainString(),
            sortedComps,
            sortedFps
        ).joinToString("|")

        return sha256Hex(payload)
    }

    private fun sha256Hex(input: String): String {
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(input.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }
}
