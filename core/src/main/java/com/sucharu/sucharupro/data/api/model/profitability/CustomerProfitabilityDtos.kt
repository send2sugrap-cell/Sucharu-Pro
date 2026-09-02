package com.sucharu.sucharupro.data.api.model.profitability

import com.sucharu.sucharupro.domain.model.profitability.*
import java.math.BigDecimal

/**
 * Request DTO for calculating customer profitability.
 */
data class CalculateCustomerProfitabilityRequestDto(
    val customerName: String? = null,
    val customerCode: String? = null,
    val periodType: String? = "ALL_TIME",
    val periodStart: Long? = null,
    val periodEnd: Long? = null,
    val customRevenue: List<CustomerRevenueAttributionDto>? = null,
    val customCosts: List<CustomerCostAttributionDto>? = null,
    val previousPeriodMargin: BigDecimal? = null,
    val idempotencyKey: String? = null
)

/**
 * DTO for Revenue Attribution.
 */
data class CustomerRevenueAttributionDto(
    val revenueAttributionId: String,
    val customerId: String,
    val orderId: String? = null,
    val invoiceId: String? = null,
    val invoiceLineId: String? = null,
    val productId: String? = null,
    val quantity: Int = 0,
    val recognizedRevenue: BigDecimal,
    val currency: String = "BDT",
    val sourceModule: String = "MODULE_14",
    val sourceEntityType: String = "CUSTOMER_INVOICE_LINE",
    val sourceEntityId: String,
    val sourceTransactionId: String? = null,
    val provenanceFingerprint: String = ""
)

/**
 * DTO for Cost Attribution.
 */
data class CustomerCostAttributionDto(
    val costAttributionId: String,
    val customerId: String,
    val orderId: String? = null,
    val jobId: String? = null,
    val productId: String? = null,
    val componentType: String,
    val directness: String = "DIRECT",
    val isVariableCost: Boolean = true,
    val attributedAmount: BigDecimal,
    val allocationBasis: String = "DIRECT",
    val numerator: BigDecimal? = null,
    val denominator: BigDecimal? = null,
    val allocationRatio: BigDecimal? = BigDecimal.ONE,
    val priority: String = "PRIORITY_1_DIRECT_CUSTOMER",
    val sourceModule: String = "MODULE_16_STEP_02",
    val sourceEntityType: String = "JOB_COST_SNAPSHOT",
    val sourceEntityId: String,
    val sourceTransactionId: String? = null,
    val provenanceFingerprint: String = ""
)

/**
 * Response DTO for Customer Profitability Snapshot.
 */
data class CustomerProfitabilitySnapshotDto(
    val snapshotId: String,
    val tenantId: String,
    val projectId: String,
    val customerId: String,
    val customerName: String?,
    val customerCode: String?,
    val periodType: String,
    val periodStart: Long?,
    val periodEnd: Long?,
    val recognizedRevenue: BigDecimal,
    val totalActualCost: BigDecimal,
    val grossProfit: BigDecimal,
    val grossMarginPercentage: BigDecimal?,
    val contributionAmount: BigDecimal,
    val contributionMarginPercentage: BigDecimal?,
    val costToRevenuePercentage: BigDecimal?,
    val attributableVariableCost: BigDecimal,
    val attributableFixedCost: BigDecimal,
    val orderCount: Int,
    val jobCount: Int,
    val productCount: Int,
    val totalQuantitySold: Int,
    val averageOrderValue: BigDecimal?,
    val averageJobValue: BigDecimal?,
    val averageRevenuePerUnit: BigDecimal?,
    val averageCostPerUnit: BigDecimal?,
    val averageProfitPerUnit: BigDecimal?,
    val unitEconomicsStatus: String,
    val profitabilityClassification: String,
    val trend: String,
    val concentrationRisk: String,
    val isLossMaking: Boolean,
    val isLowMargin: Boolean,
    val sourceIntegrityStatus: String,
    val isReconciled: Boolean,
    val reconciliationDiscrepancy: BigDecimal,
    val calculationVersion: String,
    val generatedAt: Long,
    val generatedBy: String,
    val integrityHash: String
)

/**
 * DTO for Customer Cost Breakdown Item.
 */
data class CustomerCostBreakdownItemDto(
    val componentType: String,
    val amount: BigDecimal,
    val percentageOfTotalCost: BigDecimal,
    val isVariableCost: Boolean,
    val sourceCount: Int,
    val allocationBasis: String,
    val provenanceFingerprints: List<String>
)

/**
 * DTO for Customer Order Profitability Summary.
 */
data class CustomerOrderProfitabilitySummaryDto(
    val orderId: String,
    val orderNumber: String?,
    val recognizedRevenue: BigDecimal,
    val actualCost: BigDecimal,
    val grossProfit: BigDecimal,
    val grossMarginPercentage: BigDecimal?,
    val jobCount: Int,
    val totalQuantity: Int
)

/**
 * DTO for Customer Job Profitability Summary.
 */
data class CustomerJobProfitabilitySummaryDto(
    val jobId: String,
    val jobNumber: String?,
    val orderId: String?,
    val productId: String?,
    val actualCost: BigDecimal,
    val recognizedRevenue: BigDecimal,
    val grossProfit: BigDecimal,
    val grossMarginPercentage: BigDecimal?
)

/**
 * DTO for Customer Product Contribution Summary.
 */
data class CustomerProductContributionSummaryDto(
    val productId: String,
    val productName: String?,
    val sku: String?,
    val quantity: Int,
    val recognizedRevenue: BigDecimal,
    val actualCost: BigDecimal,
    val grossProfit: BigDecimal,
    val grossMarginPercentage: BigDecimal?
)

/**
 * DTO for Customer Profitability Ranking Item.
 */
data class CustomerProfitabilityRankingItemDto(
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
    val classification: String
)

/**
 * DTO for Customer Concentration Analysis.
 */
data class CustomerConcentrationAnalysisDto(
    val totalBusinessRevenue: BigDecimal,
    val totalBusinessProfit: BigDecimal,
    val top1RevenueSharePercentage: BigDecimal,
    val top5RevenueSharePercentage: BigDecimal,
    val top10RevenueSharePercentage: BigDecimal,
    val top1ProfitSharePercentage: BigDecimal,
    val top5ProfitSharePercentage: BigDecimal,
    val top10ProfitSharePercentage: BigDecimal,
    val concentrationRisk: String,
    val topCustomers: List<CustomerProfitabilityRankingItemDto>
)

/**
 * DTO for Customer Comparison Request.
 */
data class CustomerComparisonRequestDto(
    val customerIds: List<String>
)

/**
 * DTO for Customer Comparison Item.
 */
data class CustomerProfitabilityComparisonItemDto(
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
    val classification: String,
    val trend: String
)

/**
 * DTO for Reconciliation Event.
 */
data class CustomerProfitabilityReconciliationEventDto(
    val reconciliationId: String,
    val snapshotId: String,
    val tenantId: String,
    val projectId: String,
    val customerId: String,
    val isReconciled: Boolean,
    val revenueReconciled: Boolean,
    val costReconciled: Boolean,
    val profitReconciled: Boolean,
    val contributionReconciled: Boolean,
    val expectedRevenue: BigDecimal,
    val actualRevenue: BigDecimal,
    val expectedCost: BigDecimal,
    val actualCost: BigDecimal,
    val expectedGrossProfit: BigDecimal,
    val actualGrossProfit: BigDecimal,
    val discrepancies: List<String>,
    val checkedAt: Long,
    val checkedBy: String
)

/**
 * DTO for Unattributed Profitability Item.
 */
data class UnattributedProfitabilityItemDto(
    val itemId: String,
    val tenantId: String,
    val projectId: String,
    val itemType: String,
    val amount: BigDecimal,
    val sourceModule: String,
    val sourceEntityId: String,
    val reason: String,
    val timestamp: Long
)

// Mapping extensions
fun CustomerProfitabilitySnapshot.toDto(): CustomerProfitabilitySnapshotDto = CustomerProfitabilitySnapshotDto(
    snapshotId = this.snapshotId,
    tenantId = this.tenantId,
    projectId = this.projectId,
    customerId = this.customerId,
    customerName = this.customerName,
    customerCode = this.customerCode,
    periodType = this.periodType.name,
    periodStart = this.periodStart,
    periodEnd = this.periodEnd,
    recognizedRevenue = this.recognizedRevenue,
    totalActualCost = this.totalActualCost,
    grossProfit = this.grossProfit,
    grossMarginPercentage = this.grossMarginPercentage,
    contributionAmount = this.contributionMetrics.contributionAmount,
    contributionMarginPercentage = this.contributionMetrics.contributionMarginPercentage,
    costToRevenuePercentage = this.contributionMetrics.costToRevenuePercentage,
    attributableVariableCost = this.contributionMetrics.attributableVariableCost,
    attributableFixedCost = this.contributionMetrics.attributableFixedCost,
    orderCount = this.operationalMetrics.orderCount,
    jobCount = this.operationalMetrics.jobCount,
    productCount = this.operationalMetrics.productCount,
    totalQuantitySold = this.operationalMetrics.totalQuantitySold,
    averageOrderValue = this.operationalMetrics.averageOrderValue,
    averageJobValue = this.operationalMetrics.averageJobValue,
    averageRevenuePerUnit = this.operationalMetrics.averageRevenuePerUnit,
    averageCostPerUnit = this.operationalMetrics.averageCostPerUnit,
    averageProfitPerUnit = this.operationalMetrics.averageProfitPerUnit,
    unitEconomicsStatus = this.operationalMetrics.unitEconomicsStatus,
    profitabilityClassification = this.profitabilityClassification.name,
    trend = this.trend.name,
    concentrationRisk = this.concentrationRisk.name,
    isLossMaking = this.isLossMaking,
    isLowMargin = this.isLowMargin,
    sourceIntegrityStatus = this.sourceIntegrityStatus.name,
    isReconciled = this.isReconciled,
    reconciliationDiscrepancy = this.reconciliationDiscrepancy,
    calculationVersion = this.calculationVersion,
    generatedAt = this.generatedAt,
    generatedBy = this.generatedBy,
    integrityHash = this.integrityHash
)

fun CustomerCostBreakdownItem.toDto(): CustomerCostBreakdownItemDto = CustomerCostBreakdownItemDto(
    componentType = this.componentType.name,
    amount = this.amount,
    percentageOfTotalCost = this.percentageOfTotalCost,
    isVariableCost = this.isVariableCost,
    sourceCount = this.sourceCount,
    allocationBasis = this.allocationBasis,
    provenanceFingerprints = this.provenanceFingerprints
)

fun CustomerOrderProfitabilitySummary.toDto(): CustomerOrderProfitabilitySummaryDto = CustomerOrderProfitabilitySummaryDto(
    orderId = this.orderId,
    orderNumber = this.orderNumber,
    recognizedRevenue = this.recognizedRevenue,
    actualCost = this.actualCost,
    grossProfit = this.grossProfit,
    grossMarginPercentage = this.grossMarginPercentage,
    jobCount = this.jobCount,
    totalQuantity = this.totalQuantity
)

fun CustomerJobProfitabilitySummary.toDto(): CustomerJobProfitabilitySummaryDto = CustomerJobProfitabilitySummaryDto(
    jobId = this.jobId,
    jobNumber = this.jobNumber,
    orderId = this.orderId,
    productId = this.productId,
    actualCost = this.actualCost,
    recognizedRevenue = this.recognizedRevenue,
    grossProfit = this.grossProfit,
    grossMarginPercentage = this.grossMarginPercentage
)

fun CustomerProductContributionSummary.toDto(): CustomerProductContributionSummaryDto = CustomerProductContributionSummaryDto(
    productId = this.productId,
    productName = this.productName,
    sku = this.sku,
    quantity = this.quantity,
    recognizedRevenue = this.recognizedRevenue,
    actualCost = this.actualCost,
    grossProfit = this.grossProfit,
    grossMarginPercentage = this.grossMarginPercentage
)

fun CustomerProfitabilityRankingItem.toDto(): CustomerProfitabilityRankingItemDto = CustomerProfitabilityRankingItemDto(
    rank = this.rank,
    customerId = this.customerId,
    customerName = this.customerName,
    revenue = this.revenue,
    totalCost = this.totalCost,
    grossProfit = this.grossProfit,
    grossMarginPercentage = this.grossMarginPercentage,
    contributionAmount = this.contributionAmount,
    orderCount = this.orderCount,
    quantity = this.quantity,
    averageOrderValue = this.averageOrderValue,
    classification = this.classification.name
)

fun CustomerConcentrationAnalysis.toDto(): CustomerConcentrationAnalysisDto = CustomerConcentrationAnalysisDto(
    totalBusinessRevenue = this.totalBusinessRevenue,
    totalBusinessProfit = this.totalBusinessProfit,
    top1RevenueSharePercentage = this.top1RevenueSharePercentage,
    top5RevenueSharePercentage = this.top5RevenueSharePercentage,
    top10RevenueSharePercentage = this.top10RevenueSharePercentage,
    top1ProfitSharePercentage = this.top1ProfitSharePercentage,
    top5ProfitSharePercentage = this.top5ProfitSharePercentage,
    top10ProfitSharePercentage = this.top10ProfitSharePercentage,
    concentrationRisk = this.concentrationRisk.name,
    topCustomers = this.topCustomers.map { it.toDto() }
)

fun CustomerProfitabilityComparisonItem.toDto(): CustomerProfitabilityComparisonItemDto = CustomerProfitabilityComparisonItemDto(
    customerId = this.customerId,
    customerName = this.customerName,
    revenue = this.revenue,
    totalCost = this.totalCost,
    grossProfit = this.grossProfit,
    grossMarginPercentage = this.grossMarginPercentage,
    contributionAmount = this.contributionAmount,
    contributionMarginPercentage = this.contributionMarginPercentage,
    orderCount = this.orderCount,
    jobCount = this.jobCount,
    totalQuantity = this.totalQuantity,
    averageOrderValue = this.averageOrderValue,
    classification = this.classification.name,
    trend = this.trend.name
)

fun CustomerProfitabilityReconciliationEvent.toDto(): CustomerProfitabilityReconciliationEventDto = CustomerProfitabilityReconciliationEventDto(
    reconciliationId = this.reconciliationId,
    snapshotId = this.snapshotId,
    tenantId = this.tenantId,
    projectId = this.projectId,
    customerId = this.customerId,
    isReconciled = this.isReconciled,
    revenueReconciled = this.revenueReconciled,
    costReconciled = this.costReconciled,
    profitReconciled = this.profitReconciled,
    contributionReconciled = this.contributionReconciled,
    expectedRevenue = this.expectedRevenue,
    actualRevenue = this.actualRevenue,
    expectedCost = this.expectedCost,
    actualCost = this.actualCost,
    expectedGrossProfit = this.expectedGrossProfit,
    actualGrossProfit = this.actualGrossProfit,
    discrepancies = this.discrepancies,
    checkedAt = this.checkedAt,
    checkedBy = this.checkedBy
)

fun UnattributedProfitabilityItem.toDto(): UnattributedProfitabilityItemDto = UnattributedProfitabilityItemDto(
    itemId = this.itemId,
    tenantId = this.tenantId,
    projectId = this.projectId,
    itemType = this.itemType,
    amount = this.amount,
    sourceModule = this.sourceModule,
    sourceEntityId = this.sourceEntityId,
    reason = this.reason,
    timestamp = this.timestamp
)
