package com.sucharu.sucharupro.data.api.model.profitability

import com.sucharu.sucharupro.domain.model.profitability.*
import java.math.BigDecimal

/**
 * Request, Response and Filter DTOs for Vendor Profitability & Supplier Economics.
 * Module 16 Step 05.
 */

data class CalculateVendorProfitabilityRequestDto(
    val vendorName: String? = null,
    val vendorCode: String? = null,
    val serviceCategory: String? = null,
    val periodId: String? = null,
    val periodStart: Long? = null,
    val periodEnd: Long? = null,
    val customBaselineCost: BigDecimal? = null,
    val idempotencyKey: String? = null
)

data class VendorProfitabilitySnapshotDto(
    val snapshotId: String,
    val tenantId: String,
    val projectId: String,
    val vendorId: String,
    val vendorName: String,
    val vendorCode: String?,
    val serviceCategory: String?,
    val vendorStatus: String,
    val periodId: String?,
    val periodStart: Long?,
    val periodEnd: Long?,
    val currency: String,
    val generatedAt: Long,
    val totalVendorCost: BigDecimal,
    val directVendorCost: BigDecimal,
    val paidVendorCost: BigDecimal,
    val outstandingExposure: BigDecimal,
    val unbilledEstimateCost: BigDecimal,
    val reworkCost: BigDecimal,
    val baselineCost: BigDecimal?,
    val costVariance: BigDecimal?,
    val costVariancePercentage: BigDecimal?,
    val attributedRevenueContext: BigDecimal,
    val attributedTotalJobCost: BigDecimal,
    val fulfillmentProfitabilityImpact: BigDecimal,
    val costToRevenueContextPercentage: BigDecimal?,
    val vendorCostSharePercentage: BigDecimal?,
    val attributedWorkOrderCount: Int,
    val attributedJobCount: Int,
    val attributedProductCount: Int,
    val attributedCustomerCount: Int,
    val totalAttributedQuantity: Long,
    val costPerJob: BigDecimal?,
    val costPerUnit: BigDecimal?,
    val qualityFailureCount: Int,
    val reworkCount: Int,
    val rejectionCount: Int,
    val disputeCount: Int,
    val qualityFailureRate: BigDecimal?,
    val reworkRate: BigDecimal?,
    val efficiencyScore: BigDecimal,
    val efficiencyFactors: List<String>,
    val riskClassification: String,
    val riskReasons: List<String>,
    val dependencyClassification: String,
    val dependencySharePercentage: BigDecimal?,
    val trendDirection: String,
    val dataReadiness: String,
    val costBreakdown: List<VendorCostBreakdownItemDto>,
    val provenanceFingerprints: List<String>,
    val integrityHash: String,
    val warnings: List<String>
)

data class VendorCostBreakdownItemDto(
    val componentType: String,
    val amount: BigDecimal,
    val percentageOfTotalCost: BigDecimal
)

data class VendorCostAttributionDto(
    val costAttributionId: String,
    val tenantId: String,
    val projectId: String,
    val vendorId: String,
    val workOrderId: String?,
    val jobId: String?,
    val productId: String?,
    val customerId: String?,
    val componentType: String,
    val attributedAmount: BigDecimal,
    val isPaid: Boolean,
    val sourceModule: String,
    val sourceEntityType: String,
    val sourceEntityId: String,
    val sourceTransactionId: String?,
    val attributionMethod: String,
    val provenanceFingerprint: String,
    val createdAt: Long
)

data class VendorRevenueContextAttributionDto(
    val revenueContextId: String,
    val tenantId: String,
    val projectId: String,
    val vendorId: String,
    val jobId: String?,
    val productId: String?,
    val customerId: String?,
    val recognizedRevenueContext: BigDecimal,
    val sourceModule: String,
    val sourceEntityType: String,
    val sourceEntityId: String,
    val createdAt: Long
)

data class VendorRankingItemDto(
    val rank: Int,
    val vendorId: String,
    val vendorName: String,
    val serviceCategory: String?,
    val metricValue: BigDecimal,
    val metricLabel: String,
    val efficiencyScore: BigDecimal,
    val riskClassification: String
)

data class VendorConcentrationAnalysisDto(
    val tenantId: String,
    val projectId: String,
    val periodId: String?,
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
    val concentrationRisk: String,
    val evaluatedAt: Long
)

data class VendorComparisonItemDto(
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
    val riskClassification: String,
    val dependencyClassification: String,
    val trendDirection: String
)

data class VendorReconciliationEventDto(
    val eventId: String,
    val tenantId: String,
    val projectId: String,
    val vendorId: String,
    val snapshotId: String?,
    val isBalanced: Boolean,
    val totalCostDifference: BigDecimal,
    val componentDifference: BigDecimal,
    val provenanceDifference: BigDecimal,
    val jobDifference: BigDecimal,
    val productDifference: BigDecimal,
    val customerDifference: BigDecimal,
    val paidVsLiabilityValid: Boolean,
    val errorDetails: List<String>,
    val timestamp: Long
)

data class VendorAuditEventDto(
    val auditId: String,
    val tenantId: String,
    val projectId: String,
    val vendorId: String,
    val action: String,
    val actorId: String,
    val actorRole: String,
    val details: String?,
    val integrityHash: String?,
    val timestamp: Long
)

data class VendorUnattributedItemDto(
    val unattributedId: String,
    val tenantId: String,
    val projectId: String,
    val vendorId: String,
    val sourceModule: String,
    val sourceEntityType: String,
    val sourceEntityId: String,
    val amount: BigDecimal,
    val reason: String,
    val createdAt: Long
)

fun VendorProfitabilitySnapshot.toDto(): VendorProfitabilitySnapshotDto {
    return VendorProfitabilitySnapshotDto(
        snapshotId = snapshotId,
        tenantId = tenantId,
        projectId = projectId,
        vendorId = vendorId,
        vendorName = vendorName,
        vendorCode = vendorCode,
        serviceCategory = serviceCategory,
        vendorStatus = vendorStatus,
        periodId = periodId,
        periodStart = periodStart,
        periodEnd = periodEnd,
        currency = currency,
        generatedAt = generatedAt,
        totalVendorCost = totalVendorCost,
        directVendorCost = directVendorCost,
        paidVendorCost = paidVendorCost,
        outstandingExposure = outstandingExposure,
        unbilledEstimateCost = unbilledEstimateCost,
        reworkCost = reworkCost,
        baselineCost = baselineCost,
        costVariance = costVariance,
        costVariancePercentage = costVariancePercentage,
        attributedRevenueContext = attributedRevenueContext,
        attributedTotalJobCost = attributedTotalJobCost,
        fulfillmentProfitabilityImpact = fulfillmentProfitabilityImpact,
        costToRevenueContextPercentage = costToRevenueContextPercentage,
        vendorCostSharePercentage = vendorCostSharePercentage,
        attributedWorkOrderCount = attributedWorkOrderCount,
        attributedJobCount = attributedJobCount,
        attributedProductCount = attributedProductCount,
        attributedCustomerCount = attributedCustomerCount,
        totalAttributedQuantity = totalAttributedQuantity,
        costPerJob = costPerJob,
        costPerUnit = costPerUnit,
        qualityFailureCount = qualityFailureCount,
        reworkCount = reworkCount,
        rejectionCount = rejectionCount,
        disputeCount = disputeCount,
        qualityFailureRate = qualityFailureRate,
        reworkRate = reworkRate,
        efficiencyScore = efficiencyScore,
        efficiencyFactors = efficiencyFactors,
        riskClassification = riskClassification.name,
        riskReasons = riskReasons,
        dependencyClassification = dependencyClassification.name,
        dependencySharePercentage = dependencySharePercentage,
        trendDirection = trendDirection.name,
        dataReadiness = dataReadiness.name,
        costBreakdown = costBreakdown.map { it.toDto() },
        provenanceFingerprints = provenanceFingerprints,
        integrityHash = integrityHash,
        warnings = warnings
    )
}

fun VendorCostBreakdownItem.toDto() = VendorCostBreakdownItemDto(
    componentType = componentType.name,
    amount = amount,
    percentageOfTotalCost = percentageOfTotalCost
)

fun VendorCostAttribution.toDto() = VendorCostAttributionDto(
    costAttributionId = costAttributionId,
    tenantId = tenantId,
    projectId = projectId,
    vendorId = vendorId,
    workOrderId = workOrderId,
    jobId = jobId,
    productId = productId,
    customerId = customerId,
    componentType = componentType.name,
    attributedAmount = attributedAmount,
    isPaid = isPaid,
    sourceModule = sourceModule,
    sourceEntityType = sourceEntityType,
    sourceEntityId = sourceEntityId,
    sourceTransactionId = sourceTransactionId,
    attributionMethod = attributionMethod.name,
    provenanceFingerprint = provenanceFingerprint,
    createdAt = createdAt
)

fun VendorRevenueContextAttribution.toDto() = VendorRevenueContextAttributionDto(
    revenueContextId = revenueContextId,
    tenantId = tenantId,
    projectId = projectId,
    vendorId = vendorId,
    jobId = jobId,
    productId = productId,
    customerId = customerId,
    recognizedRevenueContext = recognizedRevenueContext,
    sourceModule = sourceModule,
    sourceEntityType = sourceEntityType,
    sourceEntityId = sourceEntityId,
    createdAt = createdAt
)

fun VendorRankingItem.toDto() = VendorRankingItemDto(
    rank = rank,
    vendorId = vendorId,
    vendorName = vendorName,
    serviceCategory = serviceCategory,
    metricValue = metricValue,
    metricLabel = metricLabel,
    efficiencyScore = efficiencyScore,
    riskClassification = riskClassification.name
)

fun VendorConcentrationAnalysis.toDto() = VendorConcentrationAnalysisDto(
    tenantId = tenantId,
    projectId = projectId,
    periodId = periodId,
    totalVendorSpend = totalVendorSpend,
    totalVendorCount = totalVendorCount,
    top1Spend = top1Spend,
    top1SharePercentage = top1SharePercentage,
    top1VendorId = top1VendorId,
    top1VendorName = top1VendorName,
    top5Spend = top5Spend,
    top5SharePercentage = top5SharePercentage,
    top10Spend = top10Spend,
    top10SharePercentage = top10SharePercentage,
    concentrationRisk = concentrationRisk.name,
    evaluatedAt = evaluatedAt
)

fun VendorComparisonItem.toDto() = VendorComparisonItemDto(
    vendorId = vendorId,
    vendorName = vendorName,
    serviceCategory = serviceCategory,
    totalVendorCost = totalVendorCost,
    paidVendorCost = paidVendorCost,
    outstandingExposure = outstandingExposure,
    costPerJob = costPerJob,
    costPerUnit = costPerUnit,
    costVariancePercentage = costVariancePercentage,
    reworkCost = reworkCost,
    qualityFailureCount = qualityFailureCount,
    efficiencyScore = efficiencyScore,
    riskClassification = riskClassification.name,
    dependencyClassification = dependencyClassification.name,
    trendDirection = trendDirection.name
)

fun VendorProfitabilityReconciliationEvent.toDto() = VendorReconciliationEventDto(
    eventId = eventId,
    tenantId = tenantId,
    projectId = projectId,
    vendorId = vendorId,
    snapshotId = snapshotId,
    isBalanced = isBalanced,
    totalCostDifference = totalCostDifference,
    componentDifference = componentDifference,
    provenanceDifference = provenanceDifference,
    jobDifference = jobDifference,
    productDifference = productDifference,
    customerDifference = customerDifference,
    paidVsLiabilityValid = paidVsLiabilityValid,
    errorDetails = errorDetails,
    timestamp = timestamp
)

fun VendorProfitabilityAuditEvent.toDto() = VendorAuditEventDto(
    auditId = auditId,
    tenantId = tenantId,
    projectId = projectId,
    vendorId = vendorId,
    action = action,
    actorId = actorId,
    actorRole = actorRole,
    details = details,
    integrityHash = integrityHash,
    timestamp = timestamp
)

fun VendorUnattributedItem.toDto() = VendorUnattributedItemDto(
    unattributedId = unattributedId,
    tenantId = tenantId,
    projectId = projectId,
    vendorId = vendorId,
    sourceModule = sourceModule,
    sourceEntityType = sourceEntityType,
    sourceEntityId = sourceEntityId,
    amount = amount,
    reason = reason,
    createdAt = createdAt
)
