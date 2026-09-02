package com.sucharu.sucharupro.domain.service.profitability

import com.sucharu.sucharupro.domain.model.profitability.*
import java.math.BigDecimal

/**
 * Management Priority Engine for deterministic issue prioritization and actionable decision queuing.
 * Module 16 Step 07.
 */
interface ManagementPriorityEngine {
    fun evaluatePriorities(
        tenantId: String,
        periodId: String,
        totalRevenue: BigDecimal,
        leakages: List<ProfitLeakageItem>,
        drivers: List<ProfitabilityDriver>,
        dimensions: List<DimensionInsight>
    ): List<ManagementPriorityItem>
}

class ManagementPriorityEngineImpl : ManagementPriorityEngine {

    override fun evaluatePriorities(
        tenantId: String,
        periodId: String,
        totalRevenue: BigDecimal,
        leakages: List<ProfitLeakageItem>,
        drivers: List<ProfitabilityDriver>,
        dimensions: List<DimensionInsight>
    ): List<ManagementPriorityItem> {
        val priorities = mutableListOf<ManagementPriorityItem>()

        // 1. Convert Critical Leakages to Management Priorities
        for (leak in leakages) {
            val dim = dimensions.find { it.dimensionType == leak.dimensionType && it.dimensionId == leak.entityId }
            val trend = dim?.trendDirection ?: PeriodTrendDirection.INSUFFICIENT_DATA
            val concentration = dim?.shareOfRevenue ?: BigDecimal.ZERO

            val score = ProfitabilityIntelligenceMathUtils.calculatePriorityScore(
                financialImpact = leak.estimatedImpact,
                totalRevenue = totalRevenue,
                severityLevel = leak.severity,
                trend = trend,
                concentrationShare = concentration,
                occurrenceCount = 1
            )

            val level = when {
                score.compareTo(BigDecimal("80.0000")) >= 0 -> ManagementPriorityLevel.CRITICAL
                score.compareTo(BigDecimal("60.0000")) >= 0 -> ManagementPriorityLevel.HIGH
                score.compareTo(BigDecimal("35.0000")) >= 0 -> ManagementPriorityLevel.MEDIUM
                score.compareTo(BigDecimal("15.0000")) >= 0 -> ManagementPriorityLevel.LOW
                else -> ManagementPriorityLevel.INFORMATIONAL
            }

            priorities.add(
                ManagementPriorityItem(
                    priorityId = "prio-${leak.leakageId}",
                    snapshotId = "",
                    tenantId = tenantId,
                    periodId = periodId,
                    dimensionType = leak.dimensionType,
                    entityId = leak.entityId,
                    entityLabel = leak.entityLabel,
                    issueTitle = "Resolve ${leak.category.name.replace('_', ' ')} on ${leak.entityLabel}",
                    issueDescription = "Estimated profitability impact: ৳ ${leak.estimatedImpact}. Recommended action: ${leak.recommendedActionCode.name.replace('_', ' ')}.",
                    priorityLevel = level,
                    priorityScore = score,
                    financialImpact = leak.estimatedImpact,
                    severityWeight = BigDecimal("25.0000"),
                    trendWeight = BigDecimal("15.0000"),
                    concentrationWeight = BigDecimal("15.0000"),
                    frequencyWeight = BigDecimal("10.0000"),
                    trend = trend,
                    confidence = leak.confidence,
                    recommendedActionCode = leak.recommendedActionCode,
                    sourceFingerprints = listOf(leak.leakageId)
                )
            )
        }

        // 2. Convert High-Severity Negative Drivers to Priorities
        for (drv in drivers.filter { it.driverType == ProfitabilityDriverType.NEGATIVE_DRIVER && (it.severity == ManagementPriorityLevel.CRITICAL || it.severity == ManagementPriorityLevel.HIGH) }) {
            // Avoid duplicate priority if already covered by leakage
            if (priorities.none { it.dimensionType == drv.dimensionType && it.entityId == drv.entityId }) {
                val dim = dimensions.find { it.dimensionType == drv.dimensionType && it.dimensionId == drv.entityId }
                val trend = dim?.trendDirection ?: PeriodTrendDirection.INSUFFICIENT_DATA
                val concentration = dim?.shareOfCost ?: BigDecimal.ZERO

                val score = ProfitabilityIntelligenceMathUtils.calculatePriorityScore(
                    financialImpact = drv.impactAmount,
                    totalRevenue = totalRevenue,
                    severityLevel = drv.severity,
                    trend = trend,
                    concentrationShare = concentration,
                    occurrenceCount = 1
                )

                val recAction = when (drv.category) {
                    ProfitabilityDriverCategory.HIGH_VENDOR_COST -> RecommendedActionCode.REVIEW_VENDOR_RATE
                    ProfitabilityDriverCategory.LOW_MARGIN -> RecommendedActionCode.REVIEW_PRODUCT_MARGIN
                    ProfitabilityDriverCategory.JOB_COST_OVERRUN -> RecommendedActionCode.REVIEW_JOB_COST
                    else -> RecommendedActionCode.REVIEW_OVERHEAD_ALLOCATION
                }

                priorities.add(
                    ManagementPriorityItem(
                        priorityId = "prio-${drv.driverId}",
                        snapshotId = "",
                        tenantId = tenantId,
                        periodId = periodId,
                        dimensionType = drv.dimensionType,
                        entityId = drv.entityId,
                        entityLabel = drv.entityLabel,
                        issueTitle = "Address Cost Pressure on ${drv.entityLabel}",
                        issueDescription = drv.explanation,
                        priorityLevel = drv.severity,
                        priorityScore = score,
                        financialImpact = drv.impactAmount,
                        severityWeight = BigDecimal("25.0000"),
                        trendWeight = BigDecimal("15.0000"),
                        concentrationWeight = BigDecimal("15.0000"),
                        frequencyWeight = BigDecimal("10.0000"),
                        trend = trend,
                        confidence = ProfitabilityConfidenceStatus.HIGH,
                        recommendedActionCode = recAction,
                        sourceFingerprints = listOf(drv.fingerprint)
                    )
                )
            }
        }

        return priorities.sortedWith(
            compareByDescending<ManagementPriorityItem> { it.priorityScore }
                .thenBy { it.entityId }
        )
    }
}
