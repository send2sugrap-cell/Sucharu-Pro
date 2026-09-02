package com.sucharu.sucharupro.domain.model.profitability

import java.math.BigDecimal

/**
 * Domain models, enums, and value objects for Cross-Dimensional Profitability Intelligence
 * & Management Decision Engine.
 * Module 16 Step 07.
 */

enum class ProfitabilityDimensionType {
    BUSINESS,
    PERIOD,
    JOB,
    PRODUCT,
    CUSTOMER,
    VENDOR
}

enum class IntelligenceScope {
    FULL_BUSINESS,
    PERIOD_FOCUSED,
    CUSTOMER_FOCUSED,
    PRODUCT_FOCUSED,
    VENDOR_FOCUSED,
    JOB_FOCUSED,
    CUSTOM
}

enum class ProfitabilityConfidenceStatus {
    HIGH,
    MEDIUM,
    LOW,
    INSUFFICIENT
}

enum class ProfitabilityHealthLevel {
    EXCELLENT,
    HEALTHY,
    MODERATE,
    AT_RISK,
    CRITICAL,
    INSUFFICIENT_DATA
}

enum class ProfitabilityRiskLevel {
    LOW,
    MODERATE,
    HIGH,
    CRITICAL,
    INSUFFICIENT_DATA
}

enum class ProfitabilityDriverType {
    POSITIVE_DRIVER,
    NEGATIVE_DRIVER,
    NEUTRAL_DRIVER
}

enum class ProfitabilityDriverCategory {
    HIGH_REVENUE,
    HIGH_MARGIN,
    LOW_MARGIN,
    HIGH_COST,
    COST_VARIANCE,
    HIGH_VENDOR_COST,
    HIGH_REWORK_COST,
    HIGH_WASTAGE_COST,
    HIGH_TRANSPORT_COST,
    HIGH_INDIRECT_COST,
    CUSTOMER_CONCENTRATION,
    PRODUCT_CONCENTRATION,
    VENDOR_DEPENDENCY,
    JOB_COST_OVERRUN,
    REVENUE_DECLINE,
    PROFIT_DECLINE,
    MARGIN_DECLINE
}

enum class ProfitLeakageCategory {
    JOB_COST_OVERRUN,
    REWORK_COST,
    WASTAGE_COST,
    VENDOR_COST_PRESSURE,
    LOW_MARGIN_CUSTOMER,
    LOW_MARGIN_PRODUCT,
    HIGH_TRANSPORT_COST,
    HIGH_INDIRECT_ALLOCATION,
    DISCOUNT_PRESSURE,
    REVENUE_DECLINE,
    COST_GROWTH,
    MARGIN_COMPRESSION
}

enum class ManagementPriorityLevel {
    CRITICAL,
    HIGH,
    MEDIUM,
    LOW,
    INFORMATIONAL
}

enum class RecommendedActionCode {
    REVIEW_JOB_COST,
    REVIEW_VENDOR_RATE,
    REVIEW_WASTAGE,
    REVIEW_REWORK,
    REVIEW_DISCOUNT,
    REVIEW_CUSTOMER_MARGIN,
    REVIEW_PRODUCT_MARGIN,
    REVIEW_TRANSPORT_COST,
    REVIEW_OVERHEAD_ALLOCATION,
    INSUFFICIENT_DATA
}

enum class CrossDimensionRankingCriteria {
    MOST_PROFITABLE,
    HIGHEST_MARGIN,
    HIGHEST_REVENUE,
    HIGHEST_COST,
    LOWEST_MARGIN,
    HIGHEST_PROFIT_DECLINE,
    HIGHEST_COST_GROWTH,
    HIGHEST_RISK,
    HIGHEST_CONCENTRATION,
    BIGGEST_POSITIVE_DRIVER,
    BIGGEST_NEGATIVE_DRIVER
}

enum class ProfitabilityDependencyLevel {
    LOW_DEPENDENCY,
    MODERATE_DEPENDENCY,
    HIGH_DEPENDENCY,
    CRITICAL_DEPENDENCY,
    INSUFFICIENT_DATA
}

data class ProfitabilityIntelligenceSnapshot(
    val snapshotId: String,
    val tenantId: String,
    val projectId: String,
    val analysisPeriodId: String,
    val scope: IntelligenceScope = IntelligenceScope.FULL_BUSINESS,
    val generatedAt: Long = System.currentTimeMillis(),
    val generatedBy: String = "SYSTEM",
    val currency: String = "BDT",
    val calculationVersion: String = "MODULE16_INTELLIGENCE_V1",
    val snapshotVersion: Int = 1,

    // Aggregated Financial Core
    val revenue: BigDecimal = BigDecimal.ZERO,
    val totalCost: BigDecimal = BigDecimal.ZERO,
    val grossProfit: BigDecimal = BigDecimal.ZERO,
    val grossMargin: BigDecimal? = null,
    val costToRevenuePercentage: BigDecimal? = null,
    val contributionAmount: BigDecimal = BigDecimal.ZERO,
    val contributionMargin: BigDecimal? = null,

    // Status & Health
    val profitabilityClassification: ProfitabilityClassification = ProfitabilityClassification.INSUFFICIENT_DATA,
    val healthStatus: ProfitabilityHealthLevel = ProfitabilityHealthLevel.INSUFFICIENT_DATA,
    val confidenceStatus: ProfitabilityConfidenceStatus = ProfitabilityConfidenceStatus.HIGH,
    val sourceReadiness: PeriodSourceReadiness = PeriodSourceReadiness.READY,

    // Counts
    val dimensionCount: Int = 0,
    val relationshipCount: Int = 0,
    val driverCount: Int = 0,
    val leakageCount: Int = 0,
    val priorityCount: Int = 0,

    // Dimensions, Relationships, Drivers, Leakage, Priorities
    val dimensionInsights: List<DimensionInsight> = emptyList(),
    val relationshipInsights: List<ProfitabilityRelationshipInsight> = emptyList(),
    val drivers: List<ProfitabilityDriver> = emptyList(),
    val leakages: List<ProfitLeakageItem> = emptyList(),
    val managementPriorities: List<ManagementPriorityItem> = emptyList(),
    val healthScore: ProfitabilityHealthScore? = null,
    val provenanceRecords: List<ProfitabilityIntelligenceProvenance> = emptyList(),

    // Security & Integrity
    val integrityHash: String = "",
    val hashAlgorithm: String = "SHA-256",
    val isCertified: Boolean = false,
    val certifiedAt: Long? = null,
    val certificateId: String? = null,
    val warnings: List<String> = emptyList()
)

data class DimensionInsight(
    val insightId: String,
    val snapshotId: String,
    val tenantId: String,
    val periodId: String,
    val dimensionType: ProfitabilityDimensionType,
    val dimensionId: String,
    val dimensionLabel: String,
    val revenue: BigDecimal = BigDecimal.ZERO,
    val cost: BigDecimal = BigDecimal.ZERO,
    val grossProfit: BigDecimal = BigDecimal.ZERO,
    val margin: BigDecimal? = null,
    val contribution: BigDecimal = BigDecimal.ZERO,
    val contributionMargin: BigDecimal? = null,
    val unitCount: Long = 0L,
    val profitPerUnit: BigDecimal? = null,
    val rank: Int = 1,
    val shareOfRevenue: BigDecimal = BigDecimal.ZERO,
    val shareOfProfit: BigDecimal = BigDecimal.ZERO,
    val shareOfCost: BigDecimal = BigDecimal.ZERO,
    val trendDirection: PeriodTrendDirection = PeriodTrendDirection.INSUFFICIENT_DATA,
    val riskLevel: ProfitabilityRiskLevel = ProfitabilityRiskLevel.LOW,
    val healthStatus: ProfitabilityHealthLevel = ProfitabilityHealthLevel.HEALTHY,
    val confidenceStatus: ProfitabilityConfidenceStatus = ProfitabilityConfidenceStatus.HIGH
)

data class ProfitabilityRelationshipInsight(
    val relationshipId: String,
    val snapshotId: String,
    val tenantId: String,
    val periodId: String,
    val fromDimensionType: ProfitabilityDimensionType,
    val fromEntityId: String,
    val fromEntityLabel: String,
    val toDimensionType: ProfitabilityDimensionType,
    val toEntityId: String,
    val toEntityLabel: String,
    val revenue: BigDecimal = BigDecimal.ZERO,
    val cost: BigDecimal = BigDecimal.ZERO,
    val grossProfit: BigDecimal = BigDecimal.ZERO,
    val grossMargin: BigDecimal? = null,
    val contribution: BigDecimal = BigDecimal.ZERO,
    val contributionMargin: BigDecimal? = null,
    val quantity: Long = 0L,
    val averageRevenuePerUnit: BigDecimal? = null,
    val averageCostPerUnit: BigDecimal? = null,
    val averageProfitPerUnit: BigDecimal? = null,
    val revenueShare: BigDecimal = BigDecimal.ZERO,
    val costShare: BigDecimal = BigDecimal.ZERO,
    val profitShare: BigDecimal = BigDecimal.ZERO,
    val trendDirection: PeriodTrendDirection = PeriodTrendDirection.INSUFFICIENT_DATA,
    val riskLevel: ProfitabilityRiskLevel = ProfitabilityRiskLevel.LOW,
    val classification: ProfitabilityClassification = ProfitabilityClassification.PROFITABLE,
    val sourceIntegrityStatus: String = "VALID",
    val provenanceFingerprint: String = ""
)

data class ProfitabilityDriver(
    val driverId: String,
    val snapshotId: String,
    val tenantId: String,
    val periodId: String,
    val dimensionType: ProfitabilityDimensionType,
    val entityId: String,
    val entityLabel: String,
    val driverType: ProfitabilityDriverType,
    val category: ProfitabilityDriverCategory,
    val severity: ManagementPriorityLevel,
    val impactAmount: BigDecimal,
    val impactPercentage: BigDecimal?,
    val rank: Int,
    val explanation: String,
    val sourceReferences: List<String> = emptyList(),
    val fingerprint: String
)

data class ProfitLeakageItem(
    val leakageId: String,
    val snapshotId: String,
    val tenantId: String,
    val periodId: String,
    val dimensionType: ProfitabilityDimensionType,
    val entityId: String,
    val entityLabel: String,
    val category: ProfitLeakageCategory,
    val estimatedImpact: BigDecimal,
    val revenueContext: BigDecimal,
    val costContext: BigDecimal,
    val profitImpact: BigDecimal,
    val severity: ManagementPriorityLevel,
    val confidence: ProfitabilityConfidenceStatus,
    val sourceIntegrityStatus: String = "VALID",
    val recommendedActionCode: RecommendedActionCode,
    val provenanceReferences: List<String> = emptyList()
)

data class ManagementPriorityItem(
    val priorityId: String,
    val snapshotId: String,
    val tenantId: String,
    val periodId: String,
    val dimensionType: ProfitabilityDimensionType,
    val entityId: String,
    val entityLabel: String,
    val issueTitle: String,
    val issueDescription: String,
    val priorityLevel: ManagementPriorityLevel,
    val priorityScore: BigDecimal, // 0.0000 to 100.0000
    val financialImpact: BigDecimal,
    val severityWeight: BigDecimal,
    val trendWeight: BigDecimal,
    val concentrationWeight: BigDecimal,
    val frequencyWeight: BigDecimal,
    val trend: PeriodTrendDirection = PeriodTrendDirection.INSUFFICIENT_DATA,
    val confidence: ProfitabilityConfidenceStatus = ProfitabilityConfidenceStatus.HIGH,
    val recommendedActionCode: RecommendedActionCode,
    val sourceFingerprints: List<String> = emptyList()
)

data class ProfitabilityHealthScore(
    val scoreId: String,
    val snapshotId: String,
    val tenantId: String,
    val periodId: String,
    val overallScore: BigDecimal, // 0.0000 to 100.0000
    val marginScore: BigDecimal,
    val trendScore: BigDecimal,
    val costStabilityScore: BigDecimal,
    val revenueStabilityScore: BigDecimal,
    val concentrationScore: BigDecimal,
    val vendorDependencyScore: BigDecimal,
    val dataIntegrityScore: BigDecimal,
    val attributionCompletenessScore: BigDecimal,
    val healthLevel: ProfitabilityHealthLevel,
    val explanation: String,
    val calculatedAt: Long = System.currentTimeMillis()
)

data class ProfitabilityIntelligenceProvenance(
    val provenanceId: String,
    val tenantId: String,
    val projectId: String,
    val periodId: String,
    val sourceModule: String,
    val sourceEntityType: String,
    val sourceEntityId: String,
    val sourceTransactionId: String? = null,
    val sourceSnapshotId: String? = null,
    val dimensionType: ProfitabilityDimensionType,
    val dimensionEntityId: String,
    val metricType: String,
    val amount: BigDecimal,
    val fingerprint: String,
    val createdAt: Long = System.currentTimeMillis()
)

data class ProfitabilityIntelligenceReconciliationEvent(
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
    val assertions: List<PeriodReconciliationAssertion> = emptyList(),
    val errorDetails: List<String> = emptyList(),
    val timestamp: Long = System.currentTimeMillis()
)

data class ProfitabilityIntelligenceAuditEvent(
    val auditId: String,
    val tenantId: String,
    val projectId: String,
    val periodId: String,
    val action: String,
    val actorId: String,
    val actorRole: String,
    val snapshotId: String? = null,
    val scope: IntelligenceScope = IntelligenceScope.FULL_BUSINESS,
    val entityId: String? = null,
    val resultStatus: String = "SUCCESS",
    val correlationId: String? = null,
    val metadata: String? = null,
    val integrityHash: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

data class CrossDimensionRankingResult(
    val tenantId: String,
    val periodId: String,
    val criteria: CrossDimensionRankingCriteria,
    val dimensionType: ProfitabilityDimensionType?,
    val rankedItems: List<CrossDimensionRankingItem>,
    val evaluatedAt: Long = System.currentTimeMillis()
)

data class CrossDimensionRankingItem(
    val rank: Int,
    val dimensionType: ProfitabilityDimensionType,
    val entityId: String,
    val entityLabel: String,
    val metricValue: BigDecimal,
    val metricLabel: String,
    val revenue: BigDecimal,
    val cost: BigDecimal,
    val grossProfit: BigDecimal,
    val margin: BigDecimal?,
    val riskLevel: ProfitabilityRiskLevel,
    val trend: PeriodTrendDirection
)

data class CrossDimensionConcentrationResult(
    val tenantId: String,
    val periodId: String,
    val dimensionType: ProfitabilityDimensionType,
    val totalRevenue: BigDecimal,
    val totalProfit: BigDecimal,
    val totalCost: BigDecimal,
    val totalEntities: Int,
    val top1Share: BigDecimal,
    val top5Share: BigDecimal,
    val top10Share: BigDecimal,
    val dependencyLevel: ProfitabilityDependencyLevel,
    val topEntities: List<CrossDimensionConcentrationEntity>,
    val evaluatedAt: Long = System.currentTimeMillis()
)

data class CrossDimensionConcentrationEntity(
    val rank: Int,
    val entityId: String,
    val entityLabel: String,
    val amount: BigDecimal,
    val sharePercentage: BigDecimal
)

data class CrossDimensionTrendResult(
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
    val trendDirection: PeriodTrendDirection,
    val explanation: String,
    val dimensionTrends: List<DimensionTrendSummary> = emptyList()
)

data class DimensionTrendSummary(
    val dimensionType: ProfitabilityDimensionType,
    val entityId: String,
    val entityLabel: String,
    val currentProfit: BigDecimal,
    val previousProfit: BigDecimal,
    val profitDelta: BigDecimal,
    val profitDeltaPct: BigDecimal?,
    val trendDirection: PeriodTrendDirection
)

data class ProfitabilityIntelligenceFilter(
    val periodId: String? = null,
    val scope: IntelligenceScope? = null,
    val dimensionType: ProfitabilityDimensionType? = null,
    val entityId: String? = null,
    val riskLevel: ProfitabilityRiskLevel? = null,
    val priorityLevel: ManagementPriorityLevel? = null,
    val limit: Int = 50,
    val offset: Int = 0
)

/**
 * Verified Read-Only Downstream Handoff Contract for Module 16 Step 07.
 * Consumable by Module 01 Dashboard, Module 24 Analytics, n8n Automation, and Sucharu AI Agent.
 */
data class Module16Step07ProfitabilityIntelligenceHandoffContract(
    val contractVersion: String = "MODULE16_STEP07_V1",
    val tenantId: String,
    val projectId: String,
    val periodId: String,
    val generatedAt: Long,
    val currency: String = "BDT",

    // Executive Core Financials
    val overallRevenue: BigDecimal,
    val overallCost: BigDecimal,
    val overallProfit: BigDecimal,
    val overallMargin: BigDecimal?,
    val overallContribution: BigDecimal,
    val overallContributionMargin: BigDecimal?,

    // Top Entities by Category
    val topProfitableJobs: List<DimensionInsight>,
    val topProfitableProducts: List<DimensionInsight>,
    val topProfitableCustomers: List<DimensionInsight>,
    val topCostlyVendors: List<DimensionInsight>,
    val lossMakingEntities: List<DimensionInsight>,

    // Key Drivers & Leakages
    val topPositiveDrivers: List<ProfitabilityDriver>,
    val topNegativeDrivers: List<ProfitabilityDriver>,
    val topProfitLeakages: List<ProfitLeakageItem>,
    val managementPriorities: List<ManagementPriorityItem>,

    // Health, Readiness & Integrity
    val profitabilityHealthScore: ProfitabilityHealthScore,
    val dataConfidence: ProfitabilityConfidenceStatus,
    val integrityStatus: String,
    val integrityHash: String,
    val sourceReadiness: PeriodSourceReadiness,
    val reconciliationStatus: String
)
