package com.sucharu.sucharupro.data.api.model.profitability

import com.sucharu.sucharupro.domain.model.profitability.*
import java.math.BigDecimal

/**
 * Request, Response and Filter DTOs for Period Profitability, Financial Trends & Business Performance.
 * Module 16 Step 06.
 */

data class CalculatePeriodProfitabilityRequestDto(
    val periodType: String = "MONTH",
    val periodStart: Long,
    val periodEnd: Long,
    val timezone: String = "Asia/Dhaka",
    val periodKey: String? = null,
    val fiscalPeriodId: String? = null,
    val customBaselineRevenue: BigDecimal? = null,
    val customBaselineCost: BigDecimal? = null,
    val idempotencyKey: String? = null
)

data class PeriodProfitabilitySnapshotDto(
    val snapshotId: String,
    val tenantId: String,
    val projectId: String,
    val periodId: String,
    val periodType: String,
    val periodStart: Long,
    val periodEnd: Long,
    val timezone: String,
    val periodKey: String,
    val fiscalPeriodId: String?,
    val periodStatus: String,
    val currency: String,
    val calculationVersion: String,
    val snapshotVersion: Int,
    val supersedesSnapshotId: String?,
    val supersededBySnapshotId: String?,
    val generatedAt: Long,
    val generatedBy: String,
    val sourceAsOf: Long,
    val revenue: BigDecimal,
    val totalActualCost: BigDecimal,
    val grossProfit: BigDecimal,
    val grossMarginPercentage: BigDecimal?,
    val costToRevenuePercentage: BigDecimal?,
    val directCost: BigDecimal,
    val indirectCost: BigDecimal,
    val contributionAmount: BigDecimal,
    val contributionMarginPercentage: BigDecimal?,
    val baselineRevenue: BigDecimal?,
    val baselineCost: BigDecimal?,
    val revenueVariance: BigDecimal?,
    val revenueVariancePercentage: BigDecimal?,
    val costVariance: BigDecimal?,
    val costVariancePercentage: BigDecimal?,
    val profitVariance: BigDecimal?,
    val profitVariancePercentage: BigDecimal?,
    val jobCount: Int,
    val completedJobCount: Int,
    val productCount: Int,
    val customerCount: Int,
    val vendorCount: Int,
    val totalUnits: Long,
    val averageRevenuePerJob: BigDecimal?,
    val averageProfitPerJob: BigDecimal?,
    val averageRevenuePerUnit: BigDecimal?,
    val averageCostPerUnit: BigDecimal?,
    val averageProfitPerUnit: BigDecimal?,
    val profitabilityClassification: String,
    val trendDirection: String,
    val sourceReadiness: String,
    val costBreakdown: List<PeriodCostBreakdownItemDto>,
    val revenueAttributions: List<PeriodRevenueAttributionItemDto>,
    val provenanceFingerprints: List<String>,
    val integrityHash: String,
    val isCertified: Boolean,
    val certifiedAt: Long?,
    val certificateId: String?,
    val warnings: List<String>
)

data class PeriodCostBreakdownItemDto(
    val componentType: String,
    val amount: BigDecimal,
    val percentageOfTotalCost: BigDecimal,
    val percentageOfRevenue: BigDecimal?,
    val sourceAttributionCount: Int
)

data class PeriodRevenueAttributionItemDto(
    val attributionDimension: String,
    val dimensionId: String,
    val dimensionName: String,
    val amount: BigDecimal,
    val percentageOfTotalRevenue: BigDecimal,
    val sourceModule: String,
    val sourceEntityType: String,
    val sourceEntityId: String
)

data class PeriodRankingItemDto(
    val rank: Int,
    val periodId: String,
    val periodKey: String,
    val periodType: String,
    val periodStart: Long,
    val periodEnd: Long,
    val metricValue: BigDecimal,
    val metricLabel: String,
    val revenue: BigDecimal,
    val grossProfit: BigDecimal,
    val grossMarginPercentage: BigDecimal?,
    val profitabilityClassification: String
)

data class PeriodConcentrationAnalysisDto(
    val tenantId: String,
    val projectId: String,
    val periodType: String,
    val scopeLabel: String,
    val totalRevenue: BigDecimal,
    val totalProfit: BigDecimal,
    val totalPeriodsEvaluated: Int,
    val top1PeriodId: String?,
    val top1PeriodKey: String?,
    val top1Profit: BigDecimal,
    val top1ProfitSharePercentage: BigDecimal,
    val top3Profit: BigDecimal,
    val top3ProfitSharePercentage: BigDecimal,
    val top5Profit: BigDecimal,
    val top5ProfitSharePercentage: BigDecimal,
    val evaluatedAt: Long
)

data class PeriodComparisonResultDto(
    val currentPeriodId: String,
    val currentPeriodKey: String,
    val comparisonPeriodId: String,
    val comparisonPeriodKey: String,
    val periodType: String,
    val currentRevenue: BigDecimal,
    val previousRevenue: BigDecimal,
    val revenueDelta: BigDecimal,
    val revenueDeltaPercentage: BigDecimal?,
    val currentCost: BigDecimal,
    val previousCost: BigDecimal,
    val costDelta: BigDecimal,
    val costDeltaPercentage: BigDecimal?,
    val currentGrossProfit: BigDecimal,
    val previousGrossProfit: BigDecimal,
    val grossProfitDelta: BigDecimal,
    val grossProfitDeltaPercentage: BigDecimal?,
    val currentGrossMarginPercentage: BigDecimal?,
    val previousGrossMarginPercentage: BigDecimal?,
    val grossMarginPercentageDelta: BigDecimal?,
    val currentContributionAmount: BigDecimal,
    val previousContributionAmount: BigDecimal,
    val contributionDelta: BigDecimal,
    val currentUnits: Long,
    val previousUnits: Long,
    val currentJobCount: Int,
    val previousJobCount: Int,
    val trendDirection: String,
    val trendReason: String
)

data class PeriodReconciliationAssertionDto(
    val assertionName: String,
    val isPassed: Boolean,
    val expectedAmount: BigDecimal,
    val actualAmount: BigDecimal,
    val discrepancyAmount: BigDecimal,
    val details: String
)

data class PeriodReconciliationEventDto(
    val eventId: String,
    val tenantId: String,
    val projectId: String,
    val periodId: String,
    val snapshotId: String?,
    val isBalanced: Boolean,
    val revenueDifference: BigDecimal,
    val costDifference: BigDecimal,
    val profitDifference: BigDecimal,
    val marginDifference: BigDecimal,
    val contributionDifference: BigDecimal,
    val childAggregationDifference: BigDecimal,
    val crossDimensionalDifference: BigDecimal,
    val assertions: List<PeriodReconciliationAssertionDto>,
    val errorDetails: List<String>,
    val timestamp: Long
)

data class PeriodProvenanceRecordDto(
    val provenanceId: String,
    val tenantId: String,
    val projectId: String,
    val periodId: String,
    val sourceModule: String,
    val sourceEntityType: String,
    val sourceEntityId: String,
    val sourceTransactionId: String?,
    val sourceSnapshotId: String?,
    val amount: BigDecimal,
    val componentType: String?,
    val attributionDimension: String,
    val fingerprint: String,
    val createdAt: Long
)

data class PeriodAuditEventDto(
    val auditId: String,
    val tenantId: String,
    val projectId: String,
    val periodId: String,
    val action: String,
    val actorId: String,
    val actorRole: String,
    val snapshotId: String?,
    val calculationVersion: String,
    val previousState: String?,
    val resultingState: String?,
    val details: String?,
    val integrityHash: String?,
    val timestamp: Long
)

data class PeriodUnattributedItemDto(
    val unattributedId: String,
    val tenantId: String,
    val projectId: String,
    val periodId: String,
    val itemType: String,
    val sourceModule: String,
    val sourceEntityType: String,
    val sourceEntityId: String,
    val amount: BigDecimal,
    val reason: String,
    val createdAt: Long
)

fun PeriodProfitabilitySnapshot.toDto() = PeriodProfitabilitySnapshotDto(
    snapshotId = snapshotId,
    tenantId = tenantId,
    projectId = projectId,
    periodId = periodId,
    periodType = periodType.name,
    periodStart = periodStart,
    periodEnd = periodEnd,
    timezone = timezone,
    periodKey = periodKey,
    fiscalPeriodId = fiscalPeriodId,
    periodStatus = periodStatus.name,
    currency = currency,
    calculationVersion = calculationVersion,
    snapshotVersion = snapshotVersion,
    supersedesSnapshotId = supersedesSnapshotId,
    supersededBySnapshotId = supersededBySnapshotId,
    generatedAt = generatedAt,
    generatedBy = generatedBy,
    sourceAsOf = sourceAsOf,
    revenue = revenue,
    totalActualCost = totalActualCost,
    grossProfit = grossProfit,
    grossMarginPercentage = grossMarginPercentage,
    costToRevenuePercentage = costToRevenuePercentage,
    directCost = directCost,
    indirectCost = indirectCost,
    contributionAmount = contributionAmount,
    contributionMarginPercentage = contributionMarginPercentage,
    baselineRevenue = baselineRevenue,
    baselineCost = baselineCost,
    revenueVariance = revenueVariance,
    revenueVariancePercentage = revenueVariancePercentage,
    costVariance = costVariance,
    costVariancePercentage = costVariancePercentage,
    profitVariance = profitVariance,
    profitVariancePercentage = profitVariancePercentage,
    jobCount = jobCount,
    completedJobCount = completedJobCount,
    productCount = productCount,
    customerCount = customerCount,
    vendorCount = vendorCount,
    totalUnits = totalUnits,
    averageRevenuePerJob = averageRevenuePerJob,
    averageProfitPerJob = averageProfitPerJob,
    averageRevenuePerUnit = averageRevenuePerUnit,
    averageCostPerUnit = averageCostPerUnit,
    averageProfitPerUnit = averageProfitPerUnit,
    profitabilityClassification = profitabilityClassification.name,
    trendDirection = trendDirection.name,
    sourceReadiness = sourceReadiness.name,
    costBreakdown = costBreakdown.map { it.toDto() },
    revenueAttributions = revenueAttributions.map { it.toDto() },
    provenanceFingerprints = provenanceFingerprints,
    integrityHash = integrityHash,
    isCertified = isCertified,
    certifiedAt = certifiedAt,
    certificateId = certificateId,
    warnings = warnings
)

fun PeriodCostBreakdownItem.toDto() = PeriodCostBreakdownItemDto(
    componentType = componentType.name,
    amount = amount,
    percentageOfTotalCost = percentageOfTotalCost,
    percentageOfRevenue = percentageOfRevenue,
    sourceAttributionCount = sourceAttributionCount
)

fun PeriodRevenueAttributionItem.toDto() = PeriodRevenueAttributionItemDto(
    attributionDimension = attributionDimension,
    dimensionId = dimensionId,
    dimensionName = dimensionName,
    amount = amount,
    percentageOfTotalRevenue = percentageOfTotalRevenue,
    sourceModule = sourceModule,
    sourceEntityType = sourceEntityType,
    sourceEntityId = sourceEntityId
)

fun PeriodRankingItem.toDto() = PeriodRankingItemDto(
    rank = rank,
    periodId = periodId,
    periodKey = periodKey,
    periodType = periodType.name,
    periodStart = periodStart,
    periodEnd = periodEnd,
    metricValue = metricValue,
    metricLabel = metricLabel,
    revenue = revenue,
    grossProfit = grossProfit,
    grossMarginPercentage = grossMarginPercentage,
    profitabilityClassification = profitabilityClassification.name
)

fun PeriodConcentrationAnalysis.toDto() = PeriodConcentrationAnalysisDto(
    tenantId = tenantId,
    projectId = projectId,
    periodType = periodType.name,
    scopeLabel = scopeLabel,
    totalRevenue = totalRevenue,
    totalProfit = totalProfit,
    totalPeriodsEvaluated = totalPeriodsEvaluated,
    top1PeriodId = top1PeriodId,
    top1PeriodKey = top1PeriodKey,
    top1Profit = top1Profit,
    top1ProfitSharePercentage = top1ProfitSharePercentage,
    top3Profit = top3Profit,
    top3ProfitSharePercentage = top3ProfitSharePercentage,
    top5Profit = top5Profit,
    top5ProfitSharePercentage = top5ProfitSharePercentage,
    evaluatedAt = evaluatedAt
)

fun PeriodComparisonResult.toDto() = PeriodComparisonResultDto(
    currentPeriodId = currentPeriodId,
    currentPeriodKey = currentPeriodKey,
    comparisonPeriodId = comparisonPeriodId,
    comparisonPeriodKey = comparisonPeriodKey,
    periodType = periodType.name,
    currentRevenue = currentRevenue,
    previousRevenue = previousRevenue,
    revenueDelta = revenueDelta,
    revenueDeltaPercentage = revenueDeltaPercentage,
    currentCost = currentCost,
    previousCost = previousCost,
    costDelta = costDelta,
    costDeltaPercentage = costDeltaPercentage,
    currentGrossProfit = currentGrossProfit,
    previousGrossProfit = previousGrossProfit,
    grossProfitDelta = grossProfitDelta,
    grossProfitDeltaPercentage = grossProfitDeltaPercentage,
    currentGrossMarginPercentage = currentGrossMarginPercentage,
    previousGrossMarginPercentage = previousGrossMarginPercentage,
    grossMarginPercentageDelta = grossMarginPercentageDelta,
    currentContributionAmount = currentContributionAmount,
    previousContributionAmount = previousContributionAmount,
    contributionDelta = contributionDelta,
    currentUnits = currentUnits,
    previousUnits = previousUnits,
    currentJobCount = currentJobCount,
    previousJobCount = previousJobCount,
    trendDirection = trendDirection.name,
    trendReason = trendReason
)

fun PeriodReconciliationAssertion.toDto() = PeriodReconciliationAssertionDto(
    assertionName = assertionName,
    isPassed = isPassed,
    expectedAmount = expectedAmount,
    actualAmount = actualAmount,
    discrepancyAmount = discrepancyAmount,
    details = details
)

fun PeriodProfitabilityReconciliationEvent.toDto() = PeriodReconciliationEventDto(
    eventId = eventId,
    tenantId = tenantId,
    projectId = projectId,
    periodId = periodId,
    snapshotId = snapshotId,
    isBalanced = isBalanced,
    revenueDifference = revenueDifference,
    costDifference = costDifference,
    profitDifference = profitDifference,
    marginDifference = marginDifference,
    contributionDifference = contributionDifference,
    childAggregationDifference = childAggregationDifference,
    crossDimensionalDifference = crossDimensionalDifference,
    assertions = assertions.map { it.toDto() },
    errorDetails = errorDetails,
    timestamp = timestamp
)

fun PeriodProfitabilityProvenanceRecord.toDto() = PeriodProvenanceRecordDto(
    provenanceId = provenanceId,
    tenantId = tenantId,
    projectId = projectId,
    periodId = periodId,
    sourceModule = sourceModule,
    sourceEntityType = sourceEntityType,
    sourceEntityId = sourceEntityId,
    sourceTransactionId = sourceTransactionId,
    sourceSnapshotId = sourceSnapshotId,
    amount = amount,
    componentType = componentType?.name,
    attributionDimension = attributionDimension,
    fingerprint = fingerprint,
    createdAt = createdAt
)

fun PeriodProfitabilityAuditEvent.toDto() = PeriodAuditEventDto(
    auditId = auditId,
    tenantId = tenantId,
    projectId = projectId,
    periodId = periodId,
    action = action,
    actorId = actorId,
    actorRole = actorRole,
    snapshotId = snapshotId,
    calculationVersion = calculationVersion,
    previousState = previousState,
    resultingState = resultingState,
    details = details,
    integrityHash = integrityHash,
    timestamp = timestamp
)

fun PeriodUnattributedItem.toDto() = PeriodUnattributedItemDto(
    unattributedId = unattributedId,
    tenantId = tenantId,
    projectId = projectId,
    periodId = periodId,
    itemType = itemType,
    sourceModule = sourceModule,
    sourceEntityType = sourceEntityType,
    sourceEntityId = sourceEntityId,
    amount = amount,
    reason = reason,
    createdAt = createdAt
)
