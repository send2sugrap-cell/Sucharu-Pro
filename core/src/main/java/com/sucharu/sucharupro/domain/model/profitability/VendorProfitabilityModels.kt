package com.sucharu.sucharupro.domain.model.profitability

import java.math.BigDecimal

/**
 * Vendor-Wise Profitability, Cost Contribution & Supplier Economics Engine Domain Models.
 * Module 16 Step 05.
 */

enum class VendorRiskClassification {
    LOW_RISK,
    MODERATE_RISK,
    HIGH_RISK,
    CRITICAL_RISK,
    INSUFFICIENT_DATA
}

enum class VendorDependencyClassification {
    LOW_DEPENDENCY,
    MODERATE_DEPENDENCY,
    HIGH_DEPENDENCY,
    CRITICAL_DEPENDENCY,
    INSUFFICIENT_DATA
}

enum class VendorTrendDirection {
    STRONGLY_IMPROVING,
    IMPROVING,
    STABLE,
    DECLINING,
    STRONGLY_DECLINING,
    INSUFFICIENT_DATA
}

enum class VendorSourceReadiness {
    READY,
    PARTIAL,
    MISSING,
    CONFLICT,
    STALE
}

enum class VendorAttributionMethod {
    DIRECT_WORK_ORDER,
    JOB_OUTSOURCE_OPERATION,
    PRODUCTION_JOB_COST,
    PRODUCT_LINE_ATTRIBUTION,
    CUSTOMER_ORDER_ATTRIBUTION,
    PAYABLE_MATCH,
    INDIRECT_ALLOCATION,
    UNATTRIBUTED
}

enum class VendorRankingCriteria {
    TOTAL_COST,
    COST_PER_JOB,
    COST_PER_UNIT,
    COST_VARIANCE,
    EFFICIENCY_SCORE,
    REWORK_COST,
    REWORK_RATE,
    RISK_SCORE,
    DEPENDENCY_SHARE
}

/**
 * Immutable analytical snapshot representing vendor economics and profitability impact.
 */
data class VendorProfitabilitySnapshot(
    val snapshotId: String,
    val tenantId: String,
    val projectId: String,
    val vendorId: String,
    val vendorName: String,
    val vendorCode: String? = null,
    val serviceCategory: String? = null,
    val vendorStatus: String = "ACTIVE",
    val periodId: String? = null,
    val periodStart: Long? = null,
    val periodEnd: Long? = null,
    val currency: String = "BDT",
    val generatedAt: Long = System.currentTimeMillis(),

    // Core Vendor Cost Metrics
    val totalVendorCost: BigDecimal,
    val directVendorCost: BigDecimal,
    val paidVendorCost: BigDecimal,
    val outstandingExposure: BigDecimal,
    val unbilledEstimateCost: BigDecimal = BigDecimal.ZERO,
    val reworkCost: BigDecimal = BigDecimal.ZERO,
    val baselineCost: BigDecimal? = null,
    val costVariance: BigDecimal? = null,
    val costVariancePercentage: BigDecimal? = null,

    // Revenue Context & Fulfillment Profitability Impact
    val attributedRevenueContext: BigDecimal,
    val attributedTotalJobCost: BigDecimal,
    val fulfillmentProfitabilityImpact: BigDecimal,
    val costToRevenueContextPercentage: BigDecimal? = null,
    val vendorCostSharePercentage: BigDecimal? = null,

    // Operational & Volume Metrics
    val attributedWorkOrderCount: Int = 0,
    val attributedJobCount: Int = 0,
    val attributedProductCount: Int = 0,
    val attributedCustomerCount: Int = 0,
    val totalAttributedQuantity: Long = 0L,
    val costPerJob: BigDecimal? = null,
    val costPerUnit: BigDecimal? = null,

    // Quality, QC & Dispute Metrics
    val qualityFailureCount: Int = 0,
    val reworkCount: Int = 0,
    val rejectionCount: Int = 0,
    val disputeCount: Int = 0,
    val qualityFailureRate: BigDecimal? = null,
    val reworkRate: BigDecimal? = null,

    // Strategic & Risk Classifications
    val efficiencyScore: BigDecimal = BigDecimal.ZERO,
    val efficiencyFactors: List<String> = emptyList(),
    val riskClassification: VendorRiskClassification = VendorRiskClassification.INSUFFICIENT_DATA,
    val riskReasons: List<String> = emptyList(),
    val dependencyClassification: VendorDependencyClassification = VendorDependencyClassification.INSUFFICIENT_DATA,
    val dependencySharePercentage: BigDecimal? = null,
    val trendDirection: VendorTrendDirection = VendorTrendDirection.INSUFFICIENT_DATA,

    // Provenance & Integrity
    val costBreakdown: List<VendorCostBreakdownItem> = emptyList(),
    val dataReadiness: VendorSourceReadiness = VendorSourceReadiness.READY,
    val provenanceFingerprints: List<String> = emptyList(),
    val integrityHash: String,
    val warnings: List<String> = emptyList()
)

data class VendorCostBreakdownItem(
    val componentType: JobCostComponentType,
    val amount: BigDecimal,
    val percentageOfTotalCost: BigDecimal
)

data class VendorWorkOrderSummary(
    val workOrderId: String,
    val workOrderNumber: String? = null,
    val status: String,
    val estimatedCost: BigDecimal,
    val actualCost: BigDecimal,
    val variance: BigDecimal,
    val serviceCategory: String? = null,
    val completedAt: Long? = null
)

data class VendorJobSummary(
    val jobId: String,
    val jobName: String? = null,
    val workOrderId: String? = null,
    val vendorCost: BigDecimal,
    val totalJobCost: BigDecimal,
    val vendorCostSharePercentage: BigDecimal,
    val attributedRevenueContext: BigDecimal,
    val operationType: String? = null
)

data class VendorProductSummary(
    val productId: String,
    val productName: String? = null,
    val sku: String? = null,
    val attributedQuantity: Long,
    val vendorCost: BigDecimal,
    val vendorCostPerUnit: BigDecimal? = null,
    val recognizedRevenueContext: BigDecimal
)

data class VendorCustomerSummary(
    val customerId: String,
    val customerName: String? = null,
    val attributedOrderCount: Int,
    val vendorCost: BigDecimal,
    val customerRevenueContext: BigDecimal
)

data class VendorCostAttribution(
    val costAttributionId: String,
    val tenantId: String,
    val projectId: String,
    val vendorId: String,
    val workOrderId: String? = null,
    val jobId: String? = null,
    val productId: String? = null,
    val customerId: String? = null,
    val componentType: JobCostComponentType = JobCostComponentType.VENDOR_OUTSOURCE_COST,
    val attributedAmount: BigDecimal,
    val isPaid: Boolean = false,
    val sourceModule: String = "MODULE_12",
    val sourceEntityType: String = "VENDOR_WORK_ORDER",
    val sourceEntityId: String,
    val sourceTransactionId: String? = null,
    val attributionMethod: VendorAttributionMethod = VendorAttributionMethod.DIRECT_WORK_ORDER,
    val provenanceFingerprint: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

data class VendorRevenueContextAttribution(
    val revenueContextId: String,
    val tenantId: String,
    val projectId: String,
    val vendorId: String,
    val jobId: String? = null,
    val productId: String? = null,
    val customerId: String? = null,
    val recognizedRevenueContext: BigDecimal,
    val sourceModule: String = "MODULE_14",
    val sourceEntityType: String = "INVOICE_LINE",
    val sourceEntityId: String,
    val createdAt: Long = System.currentTimeMillis()
)

data class VendorUnattributedItem(
    val unattributedId: String,
    val tenantId: String,
    val projectId: String,
    val vendorId: String,
    val sourceModule: String,
    val sourceEntityType: String,
    val sourceEntityId: String,
    val amount: BigDecimal,
    val reason: String,
    val createdAt: Long = System.currentTimeMillis()
)

data class VendorEfficiencyScoreBreakdown(
    val totalScore: BigDecimal,
    val costVarianceScore: BigDecimal,
    val unitCostScore: BigDecimal,
    val reworkRateScore: BigDecimal,
    val qualityFailureScore: BigDecimal,
    val disputeScore: BigDecimal,
    val deliverySlaScore: BigDecimal,
    val paymentExposureScore: BigDecimal,
    val explanations: List<String>
)

data class VendorConcentrationAnalysis(
    val tenantId: String,
    val projectId: String,
    val periodId: String? = null,
    val totalVendorSpend: BigDecimal,
    val totalVendorCount: Int,
    val top1Spend: BigDecimal,
    val top1SharePercentage: BigDecimal,
    val top1VendorId: String?,
    val top1VendorName: String?,
    val top5Spend: BigDecimal,
    val top5SharePercentage: BigDecimal,
    val top10Spend: BigDecimal,
    val top10SharePercentage: BigDecimal,
    val concentrationRisk: VendorDependencyClassification,
    val evaluatedAt: Long = System.currentTimeMillis()
)

data class VendorComparisonItem(
    val vendorId: String,
    val vendorName: String,
    val serviceCategory: String?,
    val totalVendorCost: BigDecimal,
    val paidVendorCost: BigDecimal,
    val outstandingExposure: BigDecimal,
    val costPerJob: BigDecimal?,
    val costPerUnit: BigDecimal?,
    val costVariancePercentage: BigDecimal?,
    val reworkCost: BigDecimal,
    val qualityFailureCount: Int,
    val efficiencyScore: BigDecimal,
    val riskClassification: VendorRiskClassification,
    val dependencyClassification: VendorDependencyClassification,
    val trendDirection: VendorTrendDirection
)

data class VendorRankingItem(
    val rank: Int,
    val vendorId: String,
    val vendorName: String,
    val serviceCategory: String?,
    val metricValue: BigDecimal,
    val metricLabel: String,
    val efficiencyScore: BigDecimal,
    val riskClassification: VendorRiskClassification
)

data class VendorSourceCollectionResult(
    val costAttributions: List<VendorCostAttribution>,
    val revenueContextAttributions: List<VendorRevenueContextAttribution>,
    val unattributedItems: List<VendorUnattributedItem>,
    val totalVendorCost: BigDecimal,
    val directVendorCost: BigDecimal,
    val paidVendorCost: BigDecimal,
    val outstandingExposure: BigDecimal,
    val unbilledEstimateCost: BigDecimal,
    val reworkCost: BigDecimal,
    val attributedRevenueContext: BigDecimal,
    val attributedTotalJobCost: BigDecimal,
    val workOrderSummaries: List<VendorWorkOrderSummary>,
    val jobSummaries: List<VendorJobSummary>,
    val productSummaries: List<VendorProductSummary>,
    val customerSummaries: List<VendorCustomerSummary>,
    val costBreakdown: List<VendorCostBreakdownItem>,
    val totalQuantity: Long,
    val qualityFailureCount: Int,
    val reworkCount: Int,
    val rejectionCount: Int,
    val disputeCount: Int,
    val provenanceFingerprints: List<String>,
    val sourceReadiness: VendorSourceReadiness,
    val warnings: List<String>
)

data class VendorProfitabilityReconciliationEvent(
    val eventId: String,
    val tenantId: String,
    val projectId: String,
    val vendorId: String,
    val snapshotId: String? = null,
    val isBalanced: Boolean,
    val totalCostDifference: BigDecimal,
    val componentDifference: BigDecimal,
    val provenanceDifference: BigDecimal,
    val jobDifference: BigDecimal,
    val productDifference: BigDecimal,
    val customerDifference: BigDecimal,
    val paidVsLiabilityValid: Boolean,
    val errorDetails: List<String> = emptyList(),
    val timestamp: Long = System.currentTimeMillis()
)

data class VendorProfitabilityAuditEvent(
    val auditId: String,
    val tenantId: String,
    val projectId: String,
    val vendorId: String,
    val action: String,
    val actorId: String,
    val actorRole: String,
    val details: String? = null,
    val integrityHash: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

data class VendorProfitabilityFilter(
    val vendorId: String? = null,
    val serviceCategory: String? = null,
    val periodId: String? = null,
    val riskClassification: VendorRiskClassification? = null,
    val dependencyClassification: VendorDependencyClassification? = null,
    val isHighRisk: Boolean? = null,
    val isOverBudget: Boolean? = null,
    val minSpend: BigDecimal? = null,
    val maxSpend: BigDecimal? = null,
    val limit: Int = 50,
    val offset: Int = 0
)

/**
 * Future Module 17 contract interface.
 */
interface VendorCostEstimationBaselineProvider {
    suspend fun getEstimatedVendorBaselineCost(
        tenantId: String,
        projectId: String,
        vendorId: String,
        periodId: String?
    ): BigDecimal?
}
