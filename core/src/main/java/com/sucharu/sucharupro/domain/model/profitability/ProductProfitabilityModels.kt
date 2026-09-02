package com.sucharu.sucharupro.domain.model.profitability

import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Profitability aggregation dimensions (Module 16 Step 03).
 */
enum class ProductProfitabilityDimension(val displayName: String) {
    PRODUCT("Product"),
    SKU("SKU"),
    PRODUCT_EDITION("Product Edition"),
    PRODUCT_VERSION("Product Version"),
    JOB("Job"),
    CUSTOMER("Customer"),
    VENDOR("Vendor"),
    PROJECT("Project"),
    PERIOD("Period"),
    BUSINESS("Business");
}

/**
 * Deterministic profitability classification for Finished Products (Module 16 Step 03).
 */
enum class ProductProfitabilityClassification(val displayName: String) {
    HIGHLY_PROFITABLE("Highly Profitable (≥30% Margin)"),
    PROFITABLE("Profitable (15%-30% Margin)"),
    LOW_MARGIN("Low Margin (0%-15% Margin)"),
    BREAK_EVEN("Break Even (0% Margin)"),
    LOSS("Loss (<0% Margin)"),
    SOURCE_INCOMPLETE("Source Incomplete"),
    RECONCILIATION_REQUIRED("Reconciliation Required"),
    INVALID_DATA("Invalid Data");
}

/**
 * Deterministic cost variance classification against planned/estimated product baseline.
 */
enum class ProductVarianceClassification(val displayName: String) {
    UNDER_BUDGET("Under Budget"),
    ON_TARGET("On Target (±2%)"),
    OVER_BUDGET("Over Budget"),
    BASELINE_UNAVAILABLE("Baseline Unavailable"),
    SOURCE_CONFLICT("Source Conflict"),
    SOURCE_INCOMPLETE("Source Incomplete");
}

/**
 * Analytical source integrity status for Product revenue & cost attribution.
 */
enum class ProductSourceIntegrityStatus(val displayName: String) {
    VERIFIED("Verified"),
    DUPLICATE_DETECTED("Duplicate Detected"),
    SOURCE_CONFLICT("Source Conflict"),
    SOURCE_INCOMPLETE("Source Incomplete"),
    UNVERIFIED("Unverified");
}

/**
 * Approved basis used when allocating Job or indirect costs across multiple products.
 */
enum class ProductCostAllocationBasis(val displayName: String) {
    DIRECT("Direct Attribution"),
    PRODUCT_QUANTITY("Product Quantity Ratio"),
    PRODUCT_REVENUE_RATIO("Product Revenue Ratio"),
    DIRECT_COST_RATIO("Direct Cost Ratio"),
    MACHINE_HOURS("Machine Hours Ratio"),
    LABOUR_HOURS("Labour Hours Ratio"),
    CUSTOM_APPROVED_RATIO("Custom Approved Ratio");
}

/**
 * Deterministic Revenue Attribution record for a Finished Product (Module 16 Step 03).
 */
data class ProductRevenueAttribution(
    val revenueAttributionId: String,
    val tenantId: String,
    val projectId: String,
    val productId: String,
    val sku: String? = null,
    val editionId: String? = null,
    val versionId: String? = null,
    val invoiceId: String? = null,
    val orderId: String? = null,
    val customerId: String? = null,
    val quantity: Int = 0,
    val recognizedRevenue: BigDecimal = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP),
    val attributionRatio: BigDecimal = BigDecimal.ONE.setScale(4, RoundingMode.HALF_UP),
    val sourceModule: String = "MODULE_14",
    val sourceEntityType: String = "CUSTOMER_INVOICE_LINE",
    val sourceEntityId: String,
    val sourceTransactionId: String? = null,
    val attributionMethod: String = "CANONICAL_INVOICE",
    val provenanceFingerprint: String = ""
)

/**
 * Deterministic Cost Attribution record for a Finished Product (Module 16 Step 03).
 */
data class ProductCostAttribution(
    val costAttributionId: String,
    val tenantId: String,
    val projectId: String,
    val productId: String,
    val sku: String? = null,
    val editionId: String? = null,
    val versionId: String? = null,
    val jobId: String? = null,
    val componentType: JobCostComponentType,
    val directness: CostDirectness = CostDirectness.DIRECT,
    val attributedAmount: BigDecimal = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP),
    val allocationBasis: ProductCostAllocationBasis = ProductCostAllocationBasis.DIRECT,
    val numerator: BigDecimal? = null,
    val denominator: BigDecimal? = null,
    val allocationRatio: BigDecimal? = BigDecimal.ONE.setScale(4, RoundingMode.HALF_UP),
    val sourceModule: String = "MODULE_16_STEP_02",
    val sourceEntityType: String = "JOB_COST_SNAPSHOT",
    val sourceEntityId: String,
    val sourceTransactionId: String? = null,
    val provenanceFingerprint: String = ""
)

/**
 * Deterministic Unit Economics Model for Finished Products (Module 16 Step 03).
 */
data class ProductUnitEconomics(
    val quantity: Int = 0,
    val unitRevenue: BigDecimal? = null,
    val unitActualCost: BigDecimal? = null,
    val unitGrossProfit: BigDecimal? = null,
    val unitMaterialCost: BigDecimal? = null,
    val unitLabourCost: BigDecimal? = null,
    val unitMachineCost: BigDecimal? = null,
    val unitVendorCost: BigDecimal? = null,
    val unitReworkCost: BigDecimal? = null,
    val unitWastageCost: BigDecimal? = null,
    val unitFinishingCost: BigDecimal? = null,
    val unitPackagingCost: BigDecimal? = null,
    val unitTransportCost: BigDecimal? = null,
    val unitOtherDirectCost: BigDecimal? = null,
    val unitAllocatedIndirectCost: BigDecimal? = null,
    val unitMetricStatus: String = "AVAILABLE" // "AVAILABLE" or "UNIT_METRIC_UNAVAILABLE"
)

/**
 * Breakdown of a specific Cost Component for a Finished Product.
 */
data class ProductCostBreakdownItem(
    val componentType: JobCostComponentType,
    val amount: BigDecimal = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP),
    val unitAmount: BigDecimal? = null,
    val percentageOfTotalCost: BigDecimal = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP),
    val sourceCount: Int = 0,
    val allocationBasis: ProductCostAllocationBasis = ProductCostAllocationBasis.DIRECT,
    val provenanceFingerprints: List<String> = emptyList()
)

/**
 * Canonical Analytical Snapshot for Product Profitability & Unit Economics (Module 16 Step 03).
 */
data class ProductProfitabilitySnapshot(
    val snapshotId: String,
    val tenantId: String,
    val projectId: String,
    val productId: String,
    val sku: String? = null,
    val productName: String? = null,
    val editionId: String? = null,
    val versionId: String? = null,
    val periodId: String? = null,
    val customerId: String? = null,
    val totalQuantity: Int = 0,
    val recognizedRevenue: BigDecimal = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP),
    val totalActualCost: BigDecimal = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP),
    val grossProfit: BigDecimal = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP),
    val grossMarginPercentage: BigDecimal? = null,
    val unitEconomics: ProductUnitEconomics = ProductUnitEconomics(),
    val costBreakdown: List<ProductCostBreakdownItem> = emptyList(),
    val profitabilityClassification: ProductProfitabilityClassification = ProductProfitabilityClassification.BREAK_EVEN,
    val varianceClassification: ProductVarianceClassification = ProductVarianceClassification.BASELINE_UNAVAILABLE,
    val baselineCost: BigDecimal? = null,
    val costVariance: BigDecimal? = null,
    val costVariancePercentage: BigDecimal? = null,
    val sourceIntegrityStatus: ProductSourceIntegrityStatus = ProductSourceIntegrityStatus.VERIFIED,
    val isReconciled: Boolean = true,
    val reconciliationDiscrepancy: BigDecimal = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP),
    val calculationVersion: String = "PRODUCT_PROFITABILITY_V1",
    val generatedAt: Long = System.currentTimeMillis(),
    val generatedBy: String = "SYSTEM",
    val integrityHash: String = ""
)

/**
 * Non-mutating Reconciliation Event for Product Profitability.
 */
data class ProductProfitabilityReconciliationEvent(
    val reconciliationId: String,
    val snapshotId: String,
    val tenantId: String,
    val projectId: String,
    val productId: String,
    val isReconciled: Boolean = true,
    val revenueReconciled: Boolean = true,
    val costReconciled: Boolean = true,
    val unitEconomicsReconciled: Boolean = true,
    val expectedRevenue: BigDecimal = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP),
    val actualRevenue: BigDecimal = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP),
    val expectedCost: BigDecimal = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP),
    val actualCost: BigDecimal = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP),
    val grossProfitDiscrepancy: BigDecimal = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP),
    val discrepancies: List<String> = emptyList(),
    val checkedAt: Long = System.currentTimeMillis(),
    val checkedBy: String = "SYSTEM"
)

/**
 * Append-only Audit Event for Product Profitability operations.
 */
data class ProductProfitabilityAuditEvent(
    val eventId: String,
    val tenantId: String,
    val projectId: String,
    val productId: String,
    val snapshotId: String? = null,
    val action: String,
    val actor: String,
    val actorRole: String = "STAFF",
    val outcome: String = "SUCCESS",
    val details: String = "",
    val correlationId: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Filter criteria for Product Profitability querying and analytics.
 */
data class ProductProfitabilityFilter(
    val productId: String? = null,
    val sku: String? = null,
    val editionId: String? = null,
    val versionId: String? = null,
    val customerId: String? = null,
    val periodId: String? = null,
    val classification: ProductProfitabilityClassification? = null,
    val varianceClassification: ProductVarianceClassification? = null,
    val minMargin: BigDecimal? = null,
    val maxMargin: BigDecimal? = null,
    val limit: Int = 50,
    val offset: Int = 0
)

/**
 * Product Profitability Comparison Item for cross-product analysis.
 */
data class ProductProfitabilityComparisonItem(
    val productId: String,
    val sku: String?,
    val productName: String?,
    val quantity: Int,
    val recognizedRevenue: BigDecimal,
    val totalActualCost: BigDecimal,
    val grossProfit: BigDecimal,
    val grossMarginPercentage: BigDecimal?,
    val unitRevenue: BigDecimal?,
    val unitActualCost: BigDecimal?,
    val unitGrossProfit: BigDecimal?,
    val vendorOutsourceCost: BigDecimal,
    val reworkCost: BigDecimal,
    val wastageCost: BigDecimal,
    val classification: ProductProfitabilityClassification,
    val varianceClassification: ProductVarianceClassification
)
