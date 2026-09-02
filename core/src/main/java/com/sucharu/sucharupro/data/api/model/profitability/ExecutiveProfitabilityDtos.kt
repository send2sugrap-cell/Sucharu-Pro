package com.sucharu.sucharupro.data.api.model.profitability

import com.sucharu.sucharupro.domain.model.profitability.*
import java.math.BigDecimal

data class ExecutiveSnapshotCalculateRequestDto(
    val periodId: String? = null,
    val idempotencyKey: String? = null
)

data class ExecutiveKpiDto(
    val kpiKey: String,
    val kpiName: String,
    val category: String,
    val currentValue: String,
    val previousValue: String? = null,
    val varianceAmount: String? = null,
    val variancePercentage: String? = null,
    val unit: String,
    val direction: String,
    val health: String,
    val confidenceScore: String,
    val explanation: String,
    val sourceLineage: String
)

fun ExecutiveKpi.toDto(): ExecutiveKpiDto = ExecutiveKpiDto(
    kpiKey = kpiKey,
    kpiName = kpiName,
    category = category,
    currentValue = currentValue.toPlainString(),
    previousValue = previousValue?.toPlainString(),
    varianceAmount = varianceAmount?.toPlainString(),
    variancePercentage = variancePercentage?.toPlainString(),
    unit = unit,
    direction = direction.name,
    health = health.name,
    confidenceScore = confidenceScore.toPlainString(),
    explanation = explanation,
    sourceLineage = sourceLineage
)

data class ExecutiveScorecardItemDto(
    val dimension: String,
    val dimensionName: String,
    val weight: String,
    val rawScore: String,
    val weightedScore: String,
    val classification: String,
    val trend: String,
    val keyFindings: List<String>,
    val primaryMetric: String,
    val primaryMetricValue: String
)

fun ExecutiveScorecardItem.toDto(): ExecutiveScorecardItemDto = ExecutiveScorecardItemDto(
    dimension = dimension.name,
    dimensionName = dimensionName,
    weight = weight.toPlainString(),
    rawScore = rawScore.toPlainString(),
    weightedScore = weightedScore.toPlainString(),
    classification = classification.name,
    trend = trend.name,
    keyFindings = keyFindings,
    primaryMetric = primaryMetric,
    primaryMetricValue = primaryMetricValue.toPlainString()
)

data class ExecutiveManagementScorecardDto(
    val overallScore: String,
    val classification: String,
    val overallTrend: String,
    val items: List<ExecutiveScorecardItemDto>,
    val executiveSummary: String,
    val calculationTimestamp: Long
)

fun ExecutiveManagementScorecard.toDto(): ExecutiveManagementScorecardDto = ExecutiveManagementScorecardDto(
    overallScore = overallScore.toPlainString(),
    classification = classification.name,
    overallTrend = overallTrend.name,
    items = items.map { it.toDto() },
    executiveSummary = executiveSummary,
    calculationTimestamp = calculationTimestamp
)

data class ExecutiveRankingItemDto(
    val rank: Int,
    val dimension: String,
    val entityId: String,
    val entityCode: String,
    val entityName: String,
    val revenue: String,
    val cost: String,
    val grossProfit: String,
    val marginPercentage: String,
    val contributionMarginPercentage: String? = null,
    val score: String,
    val highlightReason: String
)

fun ExecutiveRankingItem.toDto(): ExecutiveRankingItemDto = ExecutiveRankingItemDto(
    rank = rank,
    dimension = dimension.name,
    entityId = entityId,
    entityCode = entityCode,
    entityName = entityName,
    revenue = revenue.toPlainString(),
    cost = cost.toPlainString(),
    grossProfit = grossProfit.toPlainString(),
    marginPercentage = marginPercentage.toPlainString(),
    contributionMarginPercentage = contributionMarginPercentage?.toPlainString(),
    score = score.toPlainString(),
    highlightReason = highlightReason
)

data class ExecutiveRankingsPayloadDto(
    val topProfitableJobs: List<ExecutiveRankingItemDto>,
    val lossMakingJobs: List<ExecutiveRankingItemDto>,
    val topProfitableProducts: List<ExecutiveRankingItemDto>,
    val leastProfitableProducts: List<ExecutiveRankingItemDto>,
    val topContributingCustomers: List<ExecutiveRankingItemDto>,
    val lowestMarginCustomers: List<ExecutiveRankingItemDto>,
    val highestSpendVendors: List<ExecutiveRankingItemDto>,
    val highestRiskVendors: List<ExecutiveRankingItemDto>
)

fun ExecutiveRankingsPayload.toDto(): ExecutiveRankingsPayloadDto = ExecutiveRankingsPayloadDto(
    topProfitableJobs = topProfitableJobs.map { it.toDto() },
    lossMakingJobs = lossMakingJobs.map { it.toDto() },
    topProfitableProducts = topProfitableProducts.map { it.toDto() },
    leastProfitableProducts = leastProfitableProducts.map { it.toDto() },
    topContributingCustomers = topContributingCustomers.map { it.toDto() },
    lowestMarginCustomers = lowestMarginCustomers.map { it.toDto() },
    highestSpendVendors = highestSpendVendors.map { it.toDto() },
    highestRiskVendors = highestRiskVendors.map { it.toDto() }
)

data class ConcentrationMetricDto(
    val dimension: String,
    val top1SharePercentage: String,
    val top5SharePercentage: String,
    val top10SharePercentage: String,
    val totalEntitiesCount: Int,
    val riskLevel: String,
    val explanation: String
)

fun ConcentrationMetric.toDto(): ConcentrationMetricDto = ConcentrationMetricDto(
    dimension = dimension.name,
    top1SharePercentage = top1SharePercentage.toPlainString(),
    top5SharePercentage = top5SharePercentage.toPlainString(),
    top10SharePercentage = top10SharePercentage.toPlainString(),
    totalEntitiesCount = totalEntitiesCount,
    riskLevel = riskLevel.name,
    explanation = explanation
)

data class ExecutiveConcentrationSummaryDto(
    val customerRevenueConcentration: ConcentrationMetricDto,
    val customerProfitConcentration: ConcentrationMetricDto,
    val productRevenueConcentration: ConcentrationMetricDto,
    val vendorSpendConcentration: ConcentrationMetricDto,
    val overallConcentrationRisk: String
)

fun ExecutiveConcentrationSummary.toDto(): ExecutiveConcentrationSummaryDto = ExecutiveConcentrationSummaryDto(
    customerRevenueConcentration = customerRevenueConcentration.toDto(),
    customerProfitConcentration = customerProfitConcentration.toDto(),
    productRevenueConcentration = productRevenueConcentration.toDto(),
    vendorSpendConcentration = vendorSpendConcentration.toDto(),
    overallConcentrationRisk = overallConcentrationRisk.name
)

data class ExecutiveProfitabilityDriverDto(
    val driverId: String,
    val driverName: String,
    val category: String,
    val impactAmount: String,
    val impactPercentage: String,
    val direction: String,
    val severity: String,
    val affectedEntitiesCount: Int,
    val description: String,
    val sourceLineage: String
)

fun ExecutiveProfitabilityDriver.toDto(): ExecutiveProfitabilityDriverDto = ExecutiveProfitabilityDriverDto(
    driverId = driverId,
    driverName = driverName,
    category = category,
    impactAmount = impactAmount.toPlainString(),
    impactPercentage = impactPercentage.toPlainString(),
    direction = direction.name,
    severity = severity.name,
    affectedEntitiesCount = affectedEntitiesCount,
    description = description,
    sourceLineage = sourceLineage
)

data class ExecutiveLeakageSummaryDto(
    val totalLeakageAmount: String,
    val leakagePercentageOfRevenue: String,
    val directMaterialWastageLeakage: String,
    val reworkCostLeakage: String,
    val unallocatedOverheadLeakage: String,
    val pricingErosionLeakage: String,
    val vendorCostSurgeLeakage: String,
    val primaryMitigationRecommendation: String
)

fun ExecutiveLeakageSummary.toDto(): ExecutiveLeakageSummaryDto = ExecutiveLeakageSummaryDto(
    totalLeakageAmount = totalLeakageAmount.toPlainString(),
    leakagePercentageOfRevenue = leakagePercentageOfRevenue.toPlainString(),
    directMaterialWastageLeakage = directMaterialWastageLeakage.toPlainString(),
    reworkCostLeakage = reworkCostLeakage.toPlainString(),
    unallocatedOverheadLeakage = unallocatedOverheadLeakage.toPlainString(),
    pricingErosionLeakage = pricingErosionLeakage.toPlainString(),
    vendorCostSurgeLeakage = vendorCostSurgeLeakage.toPlainString(),
    primaryMitigationRecommendation = primaryMitigationRecommendation
)

data class ExecutivePriorityItemDto(
    val priorityRank: Int,
    val priorityId: String,
    val title: String,
    val category: String,
    val dimension: String,
    val entityId: String?,
    val entityLabel: String?,
    val financialImpact: String,
    val priorityScore: String,
    val severity: String,
    val urgencyLevel: String,
    val recommendedActionCode: String?,
    val recommendedActionTitle: String,
    val sourceModule: String,
    val sourceStep: String,
    val sourceReferenceId: String,
    val currentStatus: String
)

fun ExecutivePriorityItem.toDto(): ExecutivePriorityItemDto = ExecutivePriorityItemDto(
    priorityRank = priorityRank,
    priorityId = priorityId,
    title = title,
    category = category,
    dimension = dimension.name,
    entityId = entityId,
    entityLabel = entityLabel,
    financialImpact = financialImpact.toPlainString(),
    priorityScore = priorityScore.toPlainString(),
    severity = severity.name,
    urgencyLevel = urgencyLevel.name,
    recommendedActionCode = recommendedActionCode?.name,
    recommendedActionTitle = recommendedActionTitle,
    sourceModule = sourceModule,
    sourceStep = sourceStep,
    sourceReferenceId = sourceReferenceId,
    currentStatus = currentStatus
)

data class ExecutiveReportSectionDto(
    val sectionKey: String,
    val sectionTitle: String,
    val orderIndex: Int,
    val summaryNarrative: String,
    val keyMetrics: Map<String, String>,
    val highlights: List<String>,
    val warnings: List<String>
)

fun ExecutiveReportSection.toDto(): ExecutiveReportSectionDto = ExecutiveReportSectionDto(
    sectionKey = sectionKey.name,
    sectionTitle = sectionTitle,
    orderIndex = orderIndex,
    summaryNarrative = summaryNarrative,
    keyMetrics = keyMetrics,
    highlights = highlights,
    warnings = warnings
)

data class ExecutiveProfitabilityReportDto(
    val reportId: String,
    val tenantId: String,
    val projectId: String,
    val periodId: String?,
    val generatedAt: Long,
    val executiveSummary: String,
    val scorecard: ExecutiveManagementScorecardDto,
    val kpis: List<ExecutiveKpiDto>,
    val sections: List<ExecutiveReportSectionDto>,
    val priorities: List<ExecutivePriorityItemDto>,
    val reportIntegrityHash: String,
    val contractVersion: String
)

fun ExecutiveProfitabilityReport.toDto(): ExecutiveProfitabilityReportDto = ExecutiveProfitabilityReportDto(
    reportId = reportId,
    tenantId = tenantId,
    projectId = projectId,
    periodId = periodId,
    generatedAt = generatedAt,
    executiveSummary = executiveSummary,
    scorecard = scorecard.toDto(),
    kpis = kpis.map { it.toDto() },
    sections = sections.map { it.toDto() },
    priorities = priorities.map { it.toDto() },
    reportIntegrityHash = reportIntegrityHash,
    contractVersion = contractVersion
)

data class ExecutiveProfitabilitySnapshotDto(
    val snapshotId: String,
    val tenantId: String,
    val projectId: String,
    val periodId: String?,
    val generatedAt: Long,
    val totalGrossRevenue: String,
    val totalNetRevenue: String,
    val totalActualCost: String,
    val totalGrossProfit: String,
    val grossMarginPercentage: String,
    val totalContributionAmount: String,
    val contributionMarginPercentage: String,
    val forecastRevenue: String?,
    val forecastGrossProfit: String?,
    val forecastGrossMargin: String?,
    val activeAlertsCount: Int,
    val criticalAlertsCount: Int,
    val pendingActionsCount: Int,
    val overallHealth: String,
    val overallScore: String,
    val sourceFingerprint: String,
    val integrityHash: String,
    val calculationVersion: String
)

fun ExecutiveProfitabilitySnapshot.toDto(): ExecutiveProfitabilitySnapshotDto = ExecutiveProfitabilitySnapshotDto(
    snapshotId = snapshotId,
    tenantId = tenantId,
    projectId = projectId,
    periodId = periodId,
    generatedAt = generatedAt,
    totalGrossRevenue = totalGrossRevenue.toPlainString(),
    totalNetRevenue = totalNetRevenue.toPlainString(),
    totalActualCost = totalActualCost.toPlainString(),
    totalGrossProfit = totalGrossProfit.toPlainString(),
    grossMarginPercentage = grossMarginPercentage.toPlainString(),
    totalContributionAmount = totalContributionAmount.toPlainString(),
    contributionMarginPercentage = contributionMarginPercentage.toPlainString(),
    forecastRevenue = forecastRevenue?.toPlainString(),
    forecastGrossProfit = forecastGrossProfit?.toPlainString(),
    forecastGrossMargin = forecastGrossMargin?.toPlainString(),
    activeAlertsCount = activeAlertsCount,
    criticalAlertsCount = criticalAlertsCount,
    pendingActionsCount = pendingActionsCount,
    overallHealth = overallHealth.name,
    overallScore = overallScore.toPlainString(),
    sourceFingerprint = sourceFingerprint,
    integrityHash = integrityHash,
    calculationVersion = calculationVersion
)

data class ExecutiveProvenanceRecordDto(
    val provenanceId: String,
    val snapshotId: String,
    val tenantId: String,
    val kpiOrSectionKey: String,
    val sourceModule: String,
    val sourceStep: String,
    val sourceEntityType: String,
    val sourceEntityId: String,
    val sourceSnapshotId: String?,
    val metricKey: String,
    val metricValue: String,
    val calculationTimestamp: Long,
    val provenanceHash: String
)

fun ExecutiveProvenanceRecord.toDto(): ExecutiveProvenanceRecordDto = ExecutiveProvenanceRecordDto(
    provenanceId = provenanceId,
    snapshotId = snapshotId,
    tenantId = tenantId,
    kpiOrSectionKey = kpiOrSectionKey,
    sourceModule = sourceModule,
    sourceStep = sourceStep,
    sourceEntityType = sourceEntityType,
    sourceEntityId = sourceEntityId,
    sourceSnapshotId = sourceSnapshotId,
    metricKey = metricKey,
    metricValue = metricValue.toPlainString(),
    calculationTimestamp = calculationTimestamp,
    provenanceHash = provenanceHash
)

data class ExecutiveReconciliationResultDto(
    val reconciliationId: String,
    val tenantId: String,
    val projectId: String,
    val periodId: String?,
    val snapshotId: String,
    val checkedAt: Long,
    val isBalanced: Boolean,
    val revenueMatches: Boolean,
    val costMatches: Boolean,
    val profitMatches: Boolean,
    val forecastMatches: Boolean,
    val alertCountsMatch: Boolean,
    val discrepancies: List<String>,
    val integrityHash: String
)

fun ExecutiveReconciliationResult.toDto(): ExecutiveReconciliationResultDto = ExecutiveReconciliationResultDto(
    reconciliationId = reconciliationId,
    tenantId = tenantId,
    projectId = projectId,
    periodId = periodId,
    snapshotId = snapshotId,
    checkedAt = checkedAt,
    isBalanced = isBalanced,
    revenueMatches = revenueMatches,
    costMatches = costMatches,
    profitMatches = profitMatches,
    forecastMatches = forecastMatches,
    alertCountsMatch = alertCountsMatch,
    discrepancies = discrepancies,
    integrityHash = integrityHash
)

data class Module16Step10ExecutiveProfitabilityHandoffContractDto(
    val handoffId: String,
    val tenantId: String,
    val projectId: String,
    val periodId: String?,
    val generatedAt: Long,
    val contractVersion: String,
    val overallHealth: String,
    val overallScorecardScore: String,
    val keyExecutiveKpis: List<ExecutiveKpiDto>,
    val topProfitabilityDrivers: List<ExecutiveProfitabilityDriverDto>,
    val leakageSummary: ExecutiveLeakageSummaryDto,
    val concentrationRisks: ExecutiveConcentrationSummaryDto,
    val topPriorityDecisions: List<ExecutivePriorityItemDto>,
    val reconciliationStatus: ExecutiveReconciliationResultDto,
    val sourceSnapshotReferences: List<String>,
    val isReadOnly: Boolean,
    val handoffIntegrityHash: String
)

fun Module16Step10ExecutiveProfitabilityHandoffContract.toDto(): Module16Step10ExecutiveProfitabilityHandoffContractDto = Module16Step10ExecutiveProfitabilityHandoffContractDto(
    handoffId = handoffId,
    tenantId = tenantId,
    projectId = projectId,
    periodId = periodId,
    generatedAt = generatedAt,
    contractVersion = contractVersion,
    overallHealth = overallHealth.name,
    overallScorecardScore = overallScorecardScore.toPlainString(),
    keyExecutiveKpis = keyExecutiveKpis.map { it.toDto() },
    topProfitabilityDrivers = topProfitabilityDrivers.map { it.toDto() },
    leakageSummary = leakageSummary.toDto(),
    concentrationRisks = concentrationRisks.toDto(),
    topPriorityDecisions = topPriorityDecisions.map { it.toDto() },
    reconciliationStatus = reconciliationStatus.toDto(),
    sourceSnapshotReferences = sourceSnapshotReferences,
    isReadOnly = isReadOnly,
    handoffIntegrityHash = handoffIntegrityHash
)
