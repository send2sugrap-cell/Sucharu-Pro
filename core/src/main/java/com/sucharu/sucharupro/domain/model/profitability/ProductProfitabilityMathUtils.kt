package com.sucharu.sucharupro.domain.model.profitability

import java.math.BigDecimal
import java.math.RoundingMode
import java.security.MessageDigest

/**
 * High-precision mathematical and hashing engine for Product Profitability & Unit Economics (Module 16 Step 03).
 */
object ProductProfitabilityMathUtils {

    const val SCALE = 4
    val ROUNDING_MODE = RoundingMode.HALF_UP
    val ZERO_MONEY: BigDecimal = BigDecimal.ZERO.setScale(SCALE, ROUNDING_MODE)
    private val ON_TARGET_TOLERANCE_PERCENT = BigDecimal("2.0000") // ±2%

    fun scaleMoney(amount: BigDecimal?): BigDecimal {
        if (amount == null) return ZERO_MONEY
        return amount.setScale(SCALE, ROUNDING_MODE)
    }

    /**
     * Gross Profit = Recognized Revenue - Total Actual Cost
     */
    fun calculateGrossProfit(revenue: BigDecimal, cost: BigDecimal): BigDecimal {
        val scaledRevenue = scaleMoney(revenue)
        val scaledCost = scaleMoney(cost)
        return scaleMoney(scaledRevenue.subtract(scaledCost))
    }

    /**
     * Gross Margin % = (Gross Profit / Recognized Revenue) * 100
     * Returns null if Revenue is zero or negative (MARGIN_UNAVAILABLE).
     */
    fun calculateGrossMarginPercentage(revenue: BigDecimal, cost: BigDecimal): BigDecimal? {
        val scaledRevenue = scaleMoney(revenue)
        if (scaledRevenue.compareTo(BigDecimal.ZERO) <= 0) {
            return null
        }
        val grossProfit = calculateGrossProfit(scaledRevenue, cost)
        return grossProfit
            .multiply(BigDecimal("100"))
            .divide(scaledRevenue, SCALE, ROUNDING_MODE)
    }

    /**
     * Computes complete Unit Economics breakdown for Finished Products.
     * Guaranteed zero-safe: returns nulls and status "UNIT_METRIC_UNAVAILABLE" when quantity <= 0.
     */
    fun calculateUnitEconomics(
        quantity: Int,
        recognizedRevenue: BigDecimal,
        totalActualCost: BigDecimal,
        components: List<ProductCostBreakdownItem>
    ): ProductUnitEconomics {
        if (quantity <= 0) {
            return ProductUnitEconomics(
                quantity = quantity,
                unitRevenue = null,
                unitActualCost = null,
                unitGrossProfit = null,
                unitMetricStatus = "UNIT_METRIC_UNAVAILABLE"
            )
        }

        val qtyBig = BigDecimal(quantity).setScale(SCALE, ROUNDING_MODE)
        val scaledRevenue = scaleMoney(recognizedRevenue)
        val scaledCost = scaleMoney(totalActualCost)
        val grossProfit = calculateGrossProfit(scaledRevenue, scaledCost)

        val unitRevenue = scaledRevenue.divide(qtyBig, SCALE, ROUNDING_MODE)
        val unitCost = scaledCost.divide(qtyBig, SCALE, ROUNDING_MODE)
        val unitProfit = grossProfit.divide(qtyBig, SCALE, ROUNDING_MODE)

        fun getUnitComp(type: JobCostComponentType): BigDecimal? {
            val item = components.firstOrNull { it.componentType == type }
            return item?.amount?.divide(qtyBig, SCALE, ROUNDING_MODE)
        }

        return ProductUnitEconomics(
            quantity = quantity,
            unitRevenue = unitRevenue,
            unitActualCost = unitCost,
            unitGrossProfit = unitProfit,
            unitMaterialCost = getUnitComp(JobCostComponentType.MATERIAL_COST),
            unitLabourCost = getUnitComp(JobCostComponentType.LABOUR_COST),
            unitMachineCost = getUnitComp(JobCostComponentType.MACHINE_COST),
            unitVendorCost = getUnitComp(JobCostComponentType.VENDOR_OUTSOURCE_COST),
            unitReworkCost = getUnitComp(JobCostComponentType.REWORK_COST),
            unitWastageCost = getUnitComp(JobCostComponentType.WASTAGE_COST),
            unitFinishingCost = getUnitComp(JobCostComponentType.FINISHING_COST),
            unitPackagingCost = getUnitComp(JobCostComponentType.PACKAGING_COST),
            unitTransportCost = getUnitComp(JobCostComponentType.TRANSPORT_COST),
            unitOtherDirectCost = getUnitComp(JobCostComponentType.OTHER_DIRECT_COST),
            unitAllocatedIndirectCost = getUnitComp(JobCostComponentType.ALLOCATED_INDIRECT_COST),
            unitMetricStatus = "AVAILABLE"
        )
    }

    /**
     * Deterministic Profitability Classification according to canonical ERP business thresholds.
     */
    fun classifyProfitability(
        recognizedRevenue: BigDecimal,
        totalActualCost: BigDecimal,
        grossMarginPercentage: BigDecimal?,
        sourceIntegrity: ProductSourceIntegrityStatus = ProductSourceIntegrityStatus.VERIFIED,
        isReconciled: Boolean = true
    ): ProductProfitabilityClassification {
        val scaledRevenue = scaleMoney(recognizedRevenue)
        val scaledCost = scaleMoney(totalActualCost)

        if (scaledRevenue.compareTo(BigDecimal.ZERO) < 0 || scaledCost.compareTo(BigDecimal.ZERO) < 0) {
            return ProductProfitabilityClassification.INVALID_DATA
        }
        if (sourceIntegrity == ProductSourceIntegrityStatus.SOURCE_INCOMPLETE) {
            return ProductProfitabilityClassification.SOURCE_INCOMPLETE
        }
        if (!isReconciled) {
            return ProductProfitabilityClassification.RECONCILIATION_REQUIRED
        }

        if (grossMarginPercentage == null) {
            val gp = calculateGrossProfit(scaledRevenue, scaledCost)
            return when {
                gp.compareTo(BigDecimal.ZERO) > 0 -> ProductProfitabilityClassification.LOW_MARGIN
                gp.compareTo(BigDecimal.ZERO) == 0 -> ProductProfitabilityClassification.BREAK_EVEN
                else -> ProductProfitabilityClassification.LOSS
            }
        }

        return when {
            grossMarginPercentage.compareTo(BigDecimal("30.0000")) >= 0 -> ProductProfitabilityClassification.HIGHLY_PROFITABLE
            grossMarginPercentage.compareTo(BigDecimal("15.0000")) >= 0 -> ProductProfitabilityClassification.PROFITABLE
            grossMarginPercentage.compareTo(BigDecimal("0.0000")) > 0 -> ProductProfitabilityClassification.LOW_MARGIN
            grossMarginPercentage.compareTo(BigDecimal("0.0000")) == 0 -> ProductProfitabilityClassification.BREAK_EVEN
            else -> ProductProfitabilityClassification.LOSS
        }
    }

    /**
     * Product Cost Variance Analysis against baseline estimation.
     */
    fun calculateVariance(
        actualCost: BigDecimal,
        baselineCost: BigDecimal?
    ): Pair<ProductVarianceClassification, Pair<BigDecimal?, BigDecimal?>> {
        val scaledActual = scaleMoney(actualCost)
        if (baselineCost == null || baselineCost.compareTo(BigDecimal.ZERO) <= 0) {
            return Pair(ProductVarianceClassification.BASELINE_UNAVAILABLE, Pair(null, null))
        }

        val scaledBaseline = scaleMoney(baselineCost)
        val variance = scaleMoney(scaledActual.subtract(scaledBaseline))
        val variancePercent = variance
            .multiply(BigDecimal("100"))
            .divide(scaledBaseline, SCALE, ROUNDING_MODE)

        val classification = when {
            variancePercent.compareTo(ON_TARGET_TOLERANCE_PERCENT.negate()) < 0 -> ProductVarianceClassification.UNDER_BUDGET
            variancePercent.compareTo(ON_TARGET_TOLERANCE_PERCENT) > 0 -> ProductVarianceClassification.OVER_BUDGET
            else -> ProductVarianceClassification.ON_TARGET
        }

        return Pair(classification, Pair(variance, variancePercent))
    }

    /**
     * Deterministic SHA-256 Provenance Fingerprint.
     */
    fun generateFingerprint(
        sourceModule: String,
        sourceEntityType: String,
        sourceEntityId: String,
        sourceTransactionId: String?,
        productId: String,
        componentType: String
    ): String {
        val raw = "${sourceModule.trim().uppercase()}:${sourceEntityType.trim().uppercase()}:${sourceEntityId.trim()}:${sourceTransactionId?.trim().orEmpty()}:${productId.trim()}:${componentType.trim()}"
        return sha256Hex(raw)
    }

    /**
     * Deterministic SHA-256 Integrity Hash for a Product Profitability Snapshot.
     */
    fun generateIntegrityHash(
        tenantId: String,
        projectId: String,
        productId: String,
        calculationVersion: String,
        quantity: Int,
        recognizedRevenue: BigDecimal,
        totalActualCost: BigDecimal,
        grossProfit: BigDecimal,
        components: List<ProductCostBreakdownItem>,
        provenanceFingerprints: List<String>
    ): String {
        val sortedComponents = components.sortedBy { it.componentType.name }
            .joinToString(";") { "${it.componentType.name}:${scaleMoney(it.amount)}:${scaleMoney(it.percentageOfTotalCost)}" }
        val sortedFingerprints = provenanceFingerprints.sorted().joinToString(",")

        val payload = listOf(
            tenantId.trim(),
            projectId.trim(),
            productId.trim(),
            calculationVersion.trim(),
            quantity.toString(),
            scaleMoney(recognizedRevenue).toPlainString(),
            scaleMoney(totalActualCost).toPlainString(),
            scaleMoney(grossProfit).toPlainString(),
            sortedComponents,
            sortedFingerprints
        ).joinToString("|")

        return sha256Hex(payload)
    }

    private fun sha256Hex(input: String): String {
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(input.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }
}
