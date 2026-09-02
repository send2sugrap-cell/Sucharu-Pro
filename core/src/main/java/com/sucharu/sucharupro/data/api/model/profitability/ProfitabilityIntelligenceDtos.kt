package com.sucharu.sucharupro.data.api.model.profitability

import com.sucharu.sucharupro.domain.model.profitability.*
import java.math.BigDecimal

data class CalculateProfitabilityIntelligenceRequestDto(
    val periodId: String? = null,
    val scope: String = "FULL_BUSINESS",
    val idempotencyKey: String? = null
)

data class ProfitabilityIntelligenceSnapshotDto(
    val snapshotId: String,
    val tenantId: String,
    val projectId: String,
    val analysisPeriodId: String,
    val scope: String,
    val generatedAt: Long,
    val generatedBy: String,
    val currency: String,
    val calculationVersion: String,
    val snapshotVersion: Int,

    val revenue: BigDecimal,
    val totalCost: BigDecimal,
    val grossProfit: BigDecimal,
    val grossMargin: BigDecimal?,
    val costToRevenuePercentage: BigDecimal?,
    val contributionAmount: BigDecimal,
    val contributionMargin: BigDecimal?,

    val profitabilityClassification: String,
    val healthStatus: String,
    val confidenceStatus: String,
    val sourceReadiness: String,

    val dimensionCount: Int,
    val relationshipCount: Int,
    val driverCount: Int,
    val leakageCount: Int,
    val priorityCount: Int,

    val dimensionInsights: List<DimensionInsightDto> = emptyList(),
    val relationshipInsights: List<ProfitabilityRelationshipInsightDto> = emptyList(),
    val drivers: List<ProfitabilityDriverDto> = emptyList(),
    val leakages: List<ProfitLeakageItemDto> = emptyList(),
    val managementPriorities: List<ManagementPriorityItemDto> = emptyList(),
    val healthScore: ProfitabilityHealthScoreDto? = null,
    val provenanceRecords: List<ProfitabilityIntelligenceProvenanceDto> = emptyList(),

    val integrityHash: String,
    val hashAlgorithm: String,
    val isCertified: Boolean,
    val certifiedAt: Long?,
    val certificateId: String?,
    val warnings: List<String>
)

data class DimensionInsightDto(
    val insightId: String,
    val snapshotId: String,
    val tenantId: String,
    val periodId: String,
    val dimensionType: String,
    val dimensionId: String,
    val dimensionLabel: String,
    val revenue: BigDecimal,
    val cost: BigDecimal,
    val grossProfit: BigDecimal,
    val margin: BigDecimal?,
    val contribution: BigDecimal,
    val contributionMargin: BigDecimal?,
    val unitCount: Long,
    val profitPerUnit: BigDecimal?,
    val rank: Int,
    val shareOfRevenue: BigDecimal,
    val shareOfProfit: BigDecimal,
    val shareOfCost: BigDecimal,
    val trendDirection: String,
    val riskLevel: String,
    val healthStatus: String,
    val confidenceStatus: String
)

data class ProfitabilityRelationshipInsightDto(
    val relationshipId: String,
    val snapshotId: String,
    val tenantId: String,
    val periodId: String,
    val fromDimensionType: String,
    val fromEntityId: String,
    val fromEntityLabel: String,
    val toDimensionType: String,
    val toEntityId: String,
    val toEntityLabel: String,
    val revenue: BigDecimal,
    val cost: BigDecimal,
    val grossProfit: BigDecimal,
    val grossMargin: BigDecimal?,
    val contribution: BigDecimal,
    val contributionMargin: BigDecimal?,
    val quantity: Long,
    val averageRevenuePerUnit: BigDecimal?,
    val averageCostPerUnit: BigDecimal?,
    val averageProfitPerUnit: BigDecimal?,
    val revenueShare: BigDecimal,
    val costShare: BigDecimal,
    val profitShare: BigDecimal,
    val trendDirection: String,
    val riskLevel: String,
    val classification: String,
    val sourceIntegrityStatus: String,
    val provenanceFingerprint: String
)

data class ProfitabilityDriverDto(
    val driverId: String,
    val snapshotId: String,
    val tenantId: String,
    val periodId: String,
    val dimensionType: String,
    val entityId: String,
    val entityLabel: String,
    val driverType: String,
    val category: String,
    val severity: String,
    val impactAmount: BigDecimal,
    val impactPercentage: BigDecimal?,
    val rank: Int,
    val explanation: String,
    val sourceReferences: List<String>,
    val fingerprint: String
)

data class ProfitLeakageItemDto(
    val leakageId: String,
    val snapshotId: String,
    val tenantId: String,
    val periodId: String,
    val dimensionType: String,
    val entityId: String,
    val entityLabel: String,
    val category: String,
    val estimatedImpact: BigDecimal,
    val revenueContext: BigDecimal,
    val costContext: BigDecimal,
    val profitImpact: BigDecimal,
    val severity: String,
    val confidence: String,
    val sourceIntegrityStatus: String,
    val recommendedActionCode: String,
    val provenanceReferences: List<String>
)

data class ManagementPriorityItemDto(
    val priorityId: String,
    val snapshotId: String,
    val tenantId: String,
    val periodId: String,
    val dimensionType: String,
    val entityId: String,
    val entityLabel: String,
    val issueTitle: String,
    val issueDescription: String,
    val priorityLevel: String,
    val priorityScore: BigDecimal,
    val financialImpact: BigDecimal,
    val severityWeight: BigDecimal,
    val trendWeight: BigDecimal,
    val concentrationWeight: BigDecimal,
    val frequencyWeight: BigDecimal,
    val trend: String,
    val confidence: String,
    val recommendedActionCode: String,
    val sourceFingerprints: List<String>
)

data class ProfitabilityHealthScoreDto(
    val scoreId: String,
    val snapshotId: String,
    val tenantId: String,
    val periodId: String,
    val overallScore: BigDecimal,
    val marginScore: BigDecimal,
    val trendScore: BigDecimal,
    val costStabilityScore: BigDecimal,
    val revenueStabilityScore: BigDecimal,
    val concentrationScore: BigDecimal,
    val vendorDependencyScore: BigDecimal,
    val dataIntegrityScore: BigDecimal,
    val attributionCompletenessScore: BigDecimal,
    val healthLevel: String,
    val explanation: String,
    val calculatedAt: Long
)

data class ProfitabilityIntelligenceProvenanceDto(
    val provenanceId: String,
    val tenantId: String,
    val projectId: String,
    val periodId: String,
    val sourceModule: String,
    val sourceEntityType: String,
    val sourceEntityId: String,
    val sourceTransactionId: String?,
    val sourceSnapshotId: String?,
    val dimensionType: String,
    val dimensionEntityId: String,
    val metricType: String,
    val amount: BigDecimal,
    val fingerprint: String,
    val createdAt: Long
)

data class ProfitabilityIntelligenceReconciliationEventDto(
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
    val relationshipDifference: BigDecimal,
    val driverImpactDifference: BigDecimal,
    val assertions: List<PeriodReconciliationAssertionDto>,
    val errorDetails: List<String>,
    val timestamp: Long
)

data class ProfitabilityIntelligenceAuditEventDto(
    val auditId: String,
    val tenantId: String,
    val projectId: String,
    val periodId: String,
    val action: String,
    val actorId: String,
    val actorRole: String,
    val snapshotId: String?,
    val scope: String,
    val entityId: String?,
    val resultStatus: String,
    val correlationId: String?,
    val metadata: String?,
    val integrityHash: String?,
    val timestamp: Long
)

data class CrossDimensionRankingResultDto(
    val tenantId: String,
    val periodId: String,
    val criteria: String,
    val dimensionType: String?,
    val rankedItems: List<CrossDimensionRankingItemDto>,
    val evaluatedAt: Long
)

data class CrossDimensionRankingItemDto(
    val rank: Int,
    val dimensionType: String,
    val entityId: String,
    val entityLabel: String,
    val metricValue: BigDecimal,
    val metricLabel: String,
    val revenue: BigDecimal,
    val cost: BigDecimal,
    val grossProfit: BigDecimal,
    val margin: BigDecimal?,
    val riskLevel: String,
    val trend: String
)

data class CrossDimensionConcentrationResultDto(
    val tenantId: String,
    val periodId: String,
    val dimensionType: String,
    val totalRevenue: BigDecimal,
    val totalProfit: BigDecimal,
    val totalCost: BigDecimal,
    val totalEntities: Int,
    val top1Share: BigDecimal,
    val top5Share: BigDecimal,
    val top10Share: BigDecimal,
    val dependencyLevel: String,
    val topEntities: List<CrossDimensionConcentrationEntityDto>,
    val evaluatedAt: Long
)

data class CrossDimensionConcentrationEntityDto(
    val rank: Int,
    val entityId: String,
    val entityLabel: String,
    val amount: BigDecimal,
    val sharePercentage: BigDecimal
)

data class CrossDimensionTrendResultDto(
    val tenantId: String,
    val currentPeriodId: String,
    val previousPeriodId: String,
    val currentRevenue: BigDecimal,
    val previousRevenue: BigDecimal,
    val revenueDelta: BigDecimal,
    val revenueDeltaPct: BigDecimal?,
    val currentCost: BigDecimal,
    val previousCost: BigDecimal,
    val costDelta: BigDecimal,
    val costDeltaPct: BigDecimal?,
    val currentProfit: BigDecimal,
    val previousProfit: BigDecimal,
    val profitDelta: BigDecimal,
    val profitDeltaPct: BigDecimal?,
    val currentMargin: BigDecimal?,
    val previousMargin: BigDecimal?,
    val marginDelta: BigDecimal?,
    val trendDirection: String,
    val explanation: String
)

data class Module16Step07ProfitabilityIntelligenceHandoffContractDto(
    val contractVersion: String,
    val tenantId: String,
    val projectId: String,
    val periodId: String,
    val generatedAt: Long,
    val currency: String,
    val overallRevenue: BigDecimal,
    val overallCost: BigDecimal,
    val overallProfit: BigDecimal,
    val overallMargin: BigDecimal?,
    val overallContribution: BigDecimal,
    val overallContributionMargin: BigDecimal?,
    val topProfitableJobs: List<DimensionInsightDto>,
    val topProfitableProducts: List<DimensionInsightDto>,
    val topProfitableCustomers: List<DimensionInsightDto>,
    val topCostlyVendors: List<DimensionInsightDto>,
    val lossMakingEntities: List<DimensionInsightDto>,
    val topPositiveDrivers: List<ProfitabilityDriverDto>,
    val topNegativeDrivers: List<ProfitabilityDriverDto>,
    val topProfitLeakages: List<ProfitLeakageItemDto>,
    val managementPriorities: List<ManagementPriorityItemDto>,
    val profitabilityHealthScore: ProfitabilityHealthScoreDto,
    val dataConfidence: String,
    val integrityStatus: String,
    val integrityHash: String,
    val sourceReadiness: String,
    val reconciliationStatus: String
)

// --- Extension Mappers ---

fun ProfitabilityIntelligenceSnapshot.toDto() = ProfitabilityIntelligenceSnapshotDto(
    snapshotId = snapshotId,
    tenantId = tenantId,
    projectId = projectId,
    analysisPeriodId = analysisPeriodId,
    scope = scope.name,
    generatedAt = generatedAt,
    generatedBy = generatedBy,
    currency = currency,
    calculationVersion = calculationVersion,
    snapshotVersion = snapshotVersion,
    revenue = revenue,
    totalCost = totalCost,
    grossProfit = grossProfit,
    grossMargin = grossMargin,
    costToRevenuePercentage = costToRevenuePercentage,
    contributionAmount = contributionAmount,
    contributionMargin = contributionMargin,
    profitabilityClassification = profitabilityClassification.name,
    healthStatus = healthStatus.name,
    confidenceStatus = confidenceStatus.name,
    sourceReadiness = sourceReadiness.name,
    dimensionCount = dimensionCount,
    relationshipCount = relationshipCount,
    driverCount = driverCount,
    leakageCount = leakageCount,
    priorityCount = priorityCount,
    dimensionInsights = dimensionInsights.map { it.toDto() },
    relationshipInsights = relationshipInsights.map { it.toDto() },
    drivers = drivers.map { it.toDto() },
    leakages = leakages.map { it.toDto() },
    managementPriorities = managementPriorities.map { it.toDto() },
    healthScore = healthScore?.toDto(),
    provenanceRecords = provenanceRecords.map { it.toDto() },
    integrityHash = integrityHash,
    hashAlgorithm = hashAlgorithm,
    isCertified = isCertified,
    certifiedAt = certifiedAt,
    certificateId = certificateId,
    warnings = warnings
)

fun DimensionInsight.toDto() = DimensionInsightDto(
    insightId = insightId,
    snapshotId = snapshotId,
    tenantId = tenantId,
    periodId = periodId,
    dimensionType = dimensionType.name,
    dimensionId = dimensionId,
    dimensionLabel = dimensionLabel,
    revenue = revenue,
    cost = cost,
    grossProfit = grossProfit,
    margin = margin,
    contribution = contribution,
    contributionMargin = contributionMargin,
    unitCount = unitCount,
    profitPerUnit = profitPerUnit,
    rank = rank,
    shareOfRevenue = shareOfRevenue,
    shareOfProfit = shareOfProfit,
    shareOfCost = shareOfCost,
    trendDirection = trendDirection.name,
    riskLevel = riskLevel.name,
    healthStatus = healthStatus.name,
    confidenceStatus = confidenceStatus.name
)

fun ProfitabilityRelationshipInsight.toDto() = ProfitabilityRelationshipInsightDto(
    relationshipId = relationshipId,
    snapshotId = snapshotId,
    tenantId = tenantId,
    periodId = periodId,
    fromDimensionType = fromDimensionType.name,
    fromEntityId = fromEntityId,
    fromEntityLabel = fromEntityLabel,
    toDimensionType = toDimensionType.name,
    toEntityId = toEntityId,
    toEntityLabel = toEntityLabel,
    revenue = revenue,
    cost = cost,
    grossProfit = grossProfit,
    grossMargin = grossMargin,
    contribution = contribution,
    contributionMargin = contributionMargin,
    quantity = quantity,
    averageRevenuePerUnit = averageRevenuePerUnit,
    averageCostPerUnit = averageCostPerUnit,
    averageProfitPerUnit = averageProfitPerUnit,
    revenueShare = revenueShare,
    costShare = costShare,
    profitShare = profitShare,
    trendDirection = trendDirection.name,
    riskLevel = riskLevel.name,
    classification = classification.name,
    sourceIntegrityStatus = sourceIntegrityStatus,
    provenanceFingerprint = provenanceFingerprint
)

fun ProfitabilityDriver.toDto() = ProfitabilityDriverDto(
    driverId = driverId,
    snapshotId = snapshotId,
    tenantId = tenantId,
    periodId = periodId,
    dimensionType = dimensionType.name,
    entityId = entityId,
    entityLabel = entityLabel,
    driverType = driverType.name,
    category = category.name,
    severity = severity.name,
    impactAmount = impactAmount,
    impactPercentage = impactPercentage,
    rank = rank,
    explanation = explanation,
    sourceReferences = sourceReferences,
    fingerprint = fingerprint
)

fun ProfitLeakageItem.toDto() = ProfitLeakageItemDto(
    leakageId = leakageId,
    snapshotId = snapshotId,
    tenantId = tenantId,
    periodId = periodId,
    dimensionType = dimensionType.name,
    entityId = entityId,
    entityLabel = entityLabel,
    category = category.name,
    estimatedImpact = estimatedImpact,
    revenueContext = revenueContext,
    costContext = costContext,
    profitImpact = profitImpact,
    severity = severity.name,
    confidence = confidence.name,
    sourceIntegrityStatus = sourceIntegrityStatus,
    recommendedActionCode = recommendedActionCode.name,
    provenanceReferences = provenanceReferences
)

fun ManagementPriorityItem.toDto() = ManagementPriorityItemDto(
    priorityId = priorityId,
    snapshotId = snapshotId,
    tenantId = tenantId,
    periodId = periodId,
    dimensionType = dimensionType.name,
    entityId = entityId,
    entityLabel = entityLabel,
    issueTitle = issueTitle,
    issueDescription = issueDescription,
    priorityLevel = priorityLevel.name,
    priorityScore = priorityScore,
    financialImpact = financialImpact,
    severityWeight = severityWeight,
    trendWeight = trendWeight,
    concentrationWeight = concentrationWeight,
    frequencyWeight = frequencyWeight,
    trend = trend.name,
    confidence = confidence.name,
    recommendedActionCode = recommendedActionCode.name,
    sourceFingerprints = sourceFingerprints
)

fun ProfitabilityHealthScore.toDto() = ProfitabilityHealthScoreDto(
    scoreId = scoreId,
    snapshotId = snapshotId,
    tenantId = tenantId,
    periodId = periodId,
    overallScore = overallScore,
    marginScore = marginScore,
    trendScore = trendScore,
    costStabilityScore = costStabilityScore,
    revenueStabilityScore = revenueStabilityScore,
    concentrationScore = concentrationScore,
    vendorDependencyScore = vendorDependencyScore,
    dataIntegrityScore = dataIntegrityScore,
    attributionCompletenessScore = attributionCompletenessScore,
    healthLevel = healthLevel.name,
    explanation = explanation,
    calculatedAt = calculatedAt
)

fun ProfitabilityIntelligenceProvenance.toDto() = ProfitabilityIntelligenceProvenanceDto(
    provenanceId = provenanceId,
    tenantId = tenantId,
    projectId = projectId,
    periodId = periodId,
    sourceModule = sourceModule,
    sourceEntityType = sourceEntityType,
    sourceEntityId = sourceEntityId,
    sourceTransactionId = sourceTransactionId,
    sourceSnapshotId = sourceSnapshotId,
    dimensionType = dimensionType.name,
    dimensionEntityId = dimensionEntityId,
    metricType = metricType,
    amount = amount,
    fingerprint = fingerprint,
    createdAt = createdAt
)

fun ProfitabilityIntelligenceReconciliationEvent.toDto() = ProfitabilityIntelligenceReconciliationEventDto(
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
    relationshipDifference = relationshipDifference,
    driverImpactDifference = driverImpactDifference,
    assertions = assertions.map { it.toDto() },
    errorDetails = errorDetails,
    timestamp = timestamp
)

fun ProfitabilityIntelligenceAuditEvent.toDto() = ProfitabilityIntelligenceAuditEventDto(
    auditId = auditId,
    tenantId = tenantId,
    projectId = projectId,
    periodId = periodId,
    action = action,
    actorId = actorId,
    actorRole = actorRole,
    snapshotId = snapshotId,
    scope = scope.name,
    entityId = entityId,
    resultStatus = resultStatus,
    correlationId = correlationId,
    metadata = metadata,
    integrityHash = integrityHash,
    timestamp = timestamp
)

fun CrossDimensionRankingResult.toDto() = CrossDimensionRankingResultDto(
    tenantId = tenantId,
    periodId = periodId,
    criteria = criteria.name,
    dimensionType = dimensionType?.name,
    rankedItems = rankedItems.map { it.toDto() },
    evaluatedAt = evaluatedAt
)

fun CrossDimensionRankingItem.toDto() = CrossDimensionRankingItemDto(
    rank = rank,
    dimensionType = dimensionType.name,
    entityId = entityId,
    entityLabel = entityLabel,
    metricValue = metricValue,
    metricLabel = metricLabel,
    revenue = revenue,
    cost = cost,
    grossProfit = grossProfit,
    margin = margin,
    riskLevel = riskLevel.name,
    trend = trend.name
)

fun CrossDimensionConcentrationResult.toDto() = CrossDimensionConcentrationResultDto(
    tenantId = tenantId,
    periodId = periodId,
    dimensionType = dimensionType.name,
    totalRevenue = totalRevenue,
    totalProfit = totalProfit,
    totalCost = totalCost,
    totalEntities = totalEntities,
    top1Share = top1Share,
    top5Share = top5Share,
    top10Share = top10Share,
    dependencyLevel = dependencyLevel.name,
    topEntities = topEntities.map { it.toDto() },
    evaluatedAt = evaluatedAt
)

fun CrossDimensionConcentrationEntity.toDto() = CrossDimensionConcentrationEntityDto(
    rank = rank,
    entityId = entityId,
    entityLabel = entityLabel,
    amount = amount,
    sharePercentage = sharePercentage
)

fun CrossDimensionTrendResult.toDto() = CrossDimensionTrendResultDto(
    tenantId = tenantId,
    currentPeriodId = currentPeriodId,
    previousPeriodId = previousPeriodId,
    currentRevenue = currentRevenue,
    previousRevenue = previousRevenue,
    revenueDelta = revenueDelta,
    revenueDeltaPct = revenueDeltaPct,
    currentCost = currentCost,
    previousCost = previousCost,
    costDelta = costDelta,
    costDeltaPct = costDeltaPct,
    currentProfit = currentProfit,
    previousProfit = previousProfit,
    profitDelta = profitDelta,
    profitDeltaPct = profitDeltaPct,
    currentMargin = currentMargin,
    previousMargin = previousMargin,
    marginDelta = marginDelta,
    trendDirection = trendDirection.name,
    explanation = explanation
)

fun Module16Step07ProfitabilityIntelligenceHandoffContract.toDto() = Module16Step07ProfitabilityIntelligenceHandoffContractDto(
    contractVersion = contractVersion,
    tenantId = tenantId,
    projectId = projectId,
    periodId = periodId,
    generatedAt = generatedAt,
    currency = currency,
    overallRevenue = overallRevenue,
    overallCost = overallCost,
    overallProfit = overallProfit,
    overallMargin = overallMargin,
    overallContribution = overallContribution,
    overallContributionMargin = overallContributionMargin,
    topProfitableJobs = topProfitableJobs.map { it.toDto() },
    topProfitableProducts = topProfitableProducts.map { it.toDto() },
    topProfitableCustomers = topProfitableCustomers.map { it.toDto() },
    topCostlyVendors = topCostlyVendors.map { it.toDto() },
    lossMakingEntities = lossMakingEntities.map { it.toDto() },
    topPositiveDrivers = topPositiveDrivers.map { it.toDto() },
    topNegativeDrivers = topNegativeDrivers.map { it.toDto() },
    topProfitLeakages = topProfitLeakages.map { it.toDto() },
    managementPriorities = managementPriorities.map { it.toDto() },
    profitabilityHealthScore = profitabilityHealthScore.toDto(),
    dataConfidence = dataConfidence.name,
    integrityStatus = integrityStatus,
    integrityHash = integrityHash,
    sourceReadiness = sourceReadiness.name,
    reconciliationStatus = reconciliationStatus
)
