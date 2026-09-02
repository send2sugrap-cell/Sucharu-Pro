package com.sucharu.sucharupro.domain.model.profitability

import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Period classification for Customer Profitability snapshots.
 */
enum class ProfitabilityPeriodType(val displayName: String) {
    DAILY("Daily"),
    WEEKLY("Weekly"),
    MONTHLY("Monthly"),
    QUARTERLY("Quarterly"),
    ANNUAL("Annual"),
    CUSTOM("Custom Date Range"),
    ALL_TIME("All Time");
}

/**
 * Profitability classifications for Customers (Module 16 Step 04).
 */
enum class CustomerProfitabilityClassification(val displayName: String) {
    HIGHLY_PROFITABLE("Highly Profitable (≥30% Margin)"),
    PROFITABLE("Profitable (15%-30% Margin)"),
    LOW_MARGIN("Low Margin (0%-15% Margin)"),
    BREAK_EVEN("Break Even (0% Margin)"),
    LOSS_MAKING("Loss Making (<0% Margin)"),
    NO_REVENUE("No Revenue Recorded"),
    INSUFFICIENT_DATA("Insufficient Data");
}

/**
 * Directional profitability trend classification.
 */
enum class CustomerProfitabilityTrend(val displayName: String) {
    STRONGLY_IMPROVING("Strongly Improving (>+5% Margin Change)"),
    IMPROVING("Improving (+1% to +5% Margin Change)"),
    STABLE("Stable (±1% Margin Change)"),
    DECLINING("Declining (-1% to -5% Margin Change)"),
    STRONGLY_DECLINING("Strongly Declining (<-5% Margin Change)"),
    INSUFFICIENT_DATA("Insufficient Historical Data");
}

/**
 * Business revenue/profit concentration risk rating.
 */
enum class CustomerConcentrationRisk(val displayName: String) {
    CONCENTRATION_LOW("Low Concentration Risk (<10% Business Share)"),
    CONCENTRATION_MODERATE("Moderate Concentration Risk (10%-25% Business Share)"),
    CONCENTRATION_HIGH("High Concentration Risk (>25% Business Share)");
}

/**
 * Source attribution precedence priority for Customer costs.
 */
enum class CustomerCostAttributionPriority {
    PRIORITY_1_DIRECT_CUSTOMER,
    PRIORITY_2_ORDER_LEVEL,
    PRIORITY_3_JOB_LEVEL,
    PRIORITY_4_PRODUCT_LEVEL,
    PRIORITY_5_APPROVED_INDIRECT
}

/**
 * Canonical customer revenue attribution item from Module 14.
 */
data class CustomerRevenueAttribution(
    val revenueAttributionId: String,
    val tenantId: String,
    val projectId: String,
    val customerId: String,
    val orderId: String? = null,
    val invoiceId: String? = null,
    val invoiceLineId: String? = null,
    val productId: String? = null,
    val quantity: Int = 0,
    val recognizedRevenue: BigDecimal = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP),
    val currency: String = "BDT",
    val sourceModule: String = "MODULE_14",
    val sourceEntityType: String = "CUSTOMER_INVOICE_LINE",
    val sourceEntityId: String,
    val sourceTransactionId: String? = null,
    val provenanceFingerprint: String = ""
)

/**
 * Canonical customer cost attribution item from Module 16 Step 02 / Module 15.
 */
data class CustomerCostAttribution(
    val costAttributionId: String,
    val tenantId: String,
    val projectId: String,
    val customerId: String,
    val orderId: String? = null,
    val jobId: String? = null,
    val productId: String? = null,
    val componentType: JobCostComponentType,
    val directness: CostDirectness = CostDirectness.DIRECT,
    val isVariableCost: Boolean = true,
    val attributedAmount: BigDecimal = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP),
    val allocationBasis: String = "DIRECT",
    val numerator: BigDecimal? = null,
    val denominator: BigDecimal? = null,
    val allocationRatio: BigDecimal? = BigDecimal.ONE.setScale(4, RoundingMode.HALF_UP),
    val priority: CustomerCostAttributionPriority = CustomerCostAttributionPriority.PRIORITY_1_DIRECT_CUSTOMER,
    val sourceModule: String = "MODULE_16_STEP_02",
    val sourceEntityType: String = "JOB_COST_SNAPSHOT",
    val sourceEntityId: String,
    val sourceTransactionId: String? = null,
    val provenanceFingerprint: String = ""
)

/**
 * Breakdown of customer cost by the 12 canonical cost components.
 */
data class CustomerCostBreakdownItem(
    val componentType: JobCostComponentType,
    val amount: BigDecimal = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP),
    val percentageOfTotalCost: BigDecimal = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP),
    val isVariableCost: Boolean = true,
    val sourceCount: Int = 0,
    val allocationBasis: String = "DIRECT",
    val provenanceFingerprints: List<String> = emptyList()
)

/**
 * Contribution and Margin Analysis metrics for a customer.
 */
data class CustomerContributionMetrics(
    val attributableVariableCost: BigDecimal = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP),
    val attributableFixedCost: BigDecimal = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP),
    val contributionAmount: BigDecimal = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP),
    val contributionMarginPercentage: BigDecimal? = null,
    val costToRevenuePercentage: BigDecimal? = null,
    val revenueContributionToBusinessPercentage: BigDecimal? = null,
    val profitContributionToBusinessPercentage: BigDecimal? = null
)

/**
 * Operational volumetric metrics for a customer.
 */
data class CustomerOperationalMetrics(
    val orderCount: Int = 0,
    val jobCount: Int = 0,
    val productCount: Int = 0,
    val totalQuantitySold: Int = 0,
    val averageOrderValue: BigDecimal? = null,
    val averageJobValue: BigDecimal? = null,
    val averageRevenuePerUnit: BigDecimal? = null,
    val averageCostPerUnit: BigDecimal? = null,
    val averageProfitPerUnit: BigDecimal? = null,
    val unitEconomicsStatus: String = "AVAILABLE"
)

/**
 * Order-level profitability attribution rollup for a customer.
 */
data class CustomerOrderProfitabilitySummary(
    val orderId: String,
    val orderNumber: String? = null,
    val recognizedRevenue: BigDecimal,
    val actualCost: BigDecimal,
    val grossProfit: BigDecimal,
    val grossMarginPercentage: BigDecimal?,
    val jobCount: Int,
    val totalQuantity: Int
)

/**
 * Job-level profitability attribution rollup for a customer.
 */
data class CustomerJobProfitabilitySummary(
    val jobId: String,
    val jobNumber: String? = null,
    val orderId: String? = null,
    val productId: String? = null,
    val actualCost: BigDecimal,
    val recognizedRevenue: BigDecimal = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP),
    val grossProfit: BigDecimal = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP),
    val grossMarginPercentage: BigDecimal? = null
)

/**
 * Product-level contribution summary to a customer's profitability.
 */
data class CustomerProductContributionSummary(
    val productId: String,
    val productName: String? = null,
    val sku: String? = null,
    val quantity: Int,
    val recognizedRevenue: BigDecimal,
    val actualCost: BigDecimal,
    val grossProfit: BigDecimal,
    val grossMarginPercentage: BigDecimal?
)

/**
 * Canonical Customer Profitability Snapshot (Module 16 Step 04).
 */
data class CustomerProfitabilitySnapshot(
    val snapshotId: String,
    val tenantId: String,
    val projectId: String,
    val customerId: String,
    val customerName: String? = null,
    val customerCode: String? = null,
    val periodType: ProfitabilityPeriodType = ProfitabilityPeriodType.ALL_TIME,
    val periodStart: Long? = null,
    val periodEnd: Long? = null,
    val recognizedRevenue: BigDecimal = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP),
    val totalActualCost: BigDecimal = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP),
    val grossProfit: BigDecimal = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP),
    val grossMarginPercentage: BigDecimal? = null,
    val contributionMetrics: CustomerContributionMetrics = CustomerContributionMetrics(),
    val operationalMetrics: CustomerOperationalMetrics = CustomerOperationalMetrics(),
    val costBreakdown: List<CustomerCostBreakdownItem> = emptyList(),
    val profitabilityClassification: CustomerProfitabilityClassification = CustomerProfitabilityClassification.BREAK_EVEN,
    val trend: CustomerProfitabilityTrend = CustomerProfitabilityTrend.STABLE,
    val concentrationRisk: CustomerConcentrationRisk = CustomerConcentrationRisk.CONCENTRATION_LOW,
    val isLossMaking: Boolean = false,
    val isLowMargin: Boolean = false,
    val sourceIntegrityStatus: ProductSourceIntegrityStatus = ProductSourceIntegrityStatus.VERIFIED,
    val isReconciled: Boolean = true,
    val reconciliationDiscrepancy: BigDecimal = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP),
    val calculationVersion: String = "CUSTOMER_PROFITABILITY_V1",
    val generatedAt: Long = System.currentTimeMillis(),
    val generatedBy: String = "SYSTEM",
    val integrityHash: String = ""
)

/**
 * Non-mutating Reconciliation Event for Customer Profitability.
 */
data class CustomerProfitabilityReconciliationEvent(
    val reconciliationId: String,
    val snapshotId: String,
    val tenantId: String,
    val projectId: String,
    val customerId: String,
    val isReconciled: Boolean = true,
    val revenueReconciled: Boolean = true,
    val costReconciled: Boolean = true,
    val profitReconciled: Boolean = true,
    val contributionReconciled: Boolean = true,
    val expectedRevenue: BigDecimal = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP),
    val actualRevenue: BigDecimal = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP),
    val expectedCost: BigDecimal = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP),
    val actualCost: BigDecimal = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP),
    val expectedGrossProfit: BigDecimal = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP),
    val actualGrossProfit: BigDecimal = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP),
    val discrepancies: List<String> = emptyList(),
    val checkedAt: Long = System.currentTimeMillis(),
    val checkedBy: String = "SYSTEM"
)

/**
 * Unattributed Revenue / Cost diagnostic record.
 */
data class UnattributedProfitabilityItem(
    val itemId: String,
    val tenantId: String,
    val projectId: String,
    val itemType: String, // "UNATTRIBUTED_REVENUE" or "UNATTRIBUTED_COST"
    val amount: BigDecimal = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP),
    val sourceModule: String,
    val sourceEntityId: String,
    val reason: String,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Append-only Customer Profitability Audit Event.
 */
data class CustomerProfitabilityAuditEvent(
    val eventId: String,
    val tenantId: String,
    val projectId: String,
    val customerId: String,
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
 * Customer ranking item model.
 */
data class CustomerProfitabilityRankingItem(
    val rank: Int,
    val customerId: String,
    val customerName: String?,
    val revenue: BigDecimal,
    val totalCost: BigDecimal,
    val grossProfit: BigDecimal,
    val grossMarginPercentage: BigDecimal?,
    val contributionAmount: BigDecimal,
    val orderCount: Int,
    val quantity: Int,
    val averageOrderValue: BigDecimal?,
    val classification: CustomerProfitabilityClassification
)

/**
 * Customer concentration intelligence model.
 */
data class CustomerConcentrationAnalysis(
    val totalBusinessRevenue: BigDecimal,
    val totalBusinessProfit: BigDecimal,
    val top1RevenueSharePercentage: BigDecimal,
    val top5RevenueSharePercentage: BigDecimal,
    val top10RevenueSharePercentage: BigDecimal,
    val top1ProfitSharePercentage: BigDecimal,
    val top5ProfitSharePercentage: BigDecimal,
    val top10ProfitSharePercentage: BigDecimal,
    val concentrationRisk: CustomerConcentrationRisk,
    val topCustomers: List<CustomerProfitabilityRankingItem>
)

/**
 * Filter criteria for querying Customer Profitability snapshots.
 */
data class CustomerProfitabilityFilter(
    val customerId: String? = null,
    val periodType: ProfitabilityPeriodType? = null,
    val classification: CustomerProfitabilityClassification? = null,
    val isLossMaking: Boolean? = null,
    val isLowMargin: Boolean? = null,
    val minMargin: BigDecimal? = null,
    val maxMargin: BigDecimal? = null,
    val limit: Int = 50,
    val offset: Int = 0
)

/**
 * Customer comparison item model.
 */
data class CustomerProfitabilityComparisonItem(
    val customerId: String,
    val customerName: String?,
    val revenue: BigDecimal,
    val totalCost: BigDecimal,
    val grossProfit: BigDecimal,
    val grossMarginPercentage: BigDecimal?,
    val contributionAmount: BigDecimal,
    val contributionMarginPercentage: BigDecimal?,
    val orderCount: Int,
    val jobCount: Int,
    val totalQuantity: Int,
    val averageOrderValue: BigDecimal?,
    val classification: CustomerProfitabilityClassification,
    val trend: CustomerProfitabilityTrend
)
