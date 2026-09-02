package com.sucharu.sucharupro.domain.service.profitability

import com.sucharu.sucharupro.domain.model.profitability.*
import java.math.BigDecimal

/**
 * Driver Engine for deterministic identification of positive and negative profitability contributors.
 * Module 16 Step 07.
 */
interface ProfitabilityDriverEngine {
    fun evaluateDrivers(
        tenantId: String,
        periodId: String,
        totalRevenue: BigDecimal,
        totalCost: BigDecimal,
        dimensions: List<DimensionInsight>,
        relationships: List<ProfitabilityRelationshipInsight>
    ): List<ProfitabilityDriver>
}

class ProfitabilityDriverEngineImpl : ProfitabilityDriverEngine {

    override fun evaluateDrivers(
        tenantId: String,
        periodId: String,
        totalRevenue: BigDecimal,
        totalCost: BigDecimal,
        dimensions: List<DimensionInsight>,
        relationships: List<ProfitabilityRelationshipInsight>
    ): List<ProfitabilityDriver> {
        val drivers = mutableListOf<ProfitabilityDriver>()
        var currentRank = 1

        val scaledRevenue = ProfitabilityIntelligenceMathUtils.scaleMoney(totalRevenue)
        val scaledCost = ProfitabilityIntelligenceMathUtils.scaleMoney(totalCost)

        // 1. Positive Drivers: High Margin / High Profit Entities
        val positiveEntities = dimensions
            .filter { it.grossProfit.compareTo(BigDecimal.ZERO) > 0 }
            .sortedWith(
                compareByDescending<DimensionInsight> { it.grossProfit }
                    .thenByDescending { it.margin ?: BigDecimal.ZERO }
                    .thenBy { it.dimensionId }
            )

        for (item in positiveEntities) {
            val impactPct = ProfitabilityIntelligenceMathUtils.calculateSharePercentage(item.grossProfit, scaledRevenue.max(BigDecimal.ONE))
            val category = if ((item.margin ?: BigDecimal.ZERO).compareTo(BigDecimal("30.0000")) >= 0) {
                ProfitabilityDriverCategory.HIGH_MARGIN
            } else {
                ProfitabilityDriverCategory.HIGH_REVENUE
            }

            val fingerprint = ProfitabilityIntelligenceMathUtils.generateProvenanceFingerprint(
                tenantId = tenantId,
                periodId = periodId,
                sourceModule = "MODULE_16_STEP_07",
                sourceEntityType = item.dimensionType.name,
                sourceEntityId = item.dimensionId,
                sourceTransactionId = "DRIVER_POS_${item.dimensionId}",
                metricType = "GROSS_PROFIT_CONTRIBUTION"
            )

            drivers.add(
                ProfitabilityDriver(
                    driverId = "drv-pos-${item.dimensionType.name.lowercase()}-${item.dimensionId}",
                    snapshotId = "",
                    tenantId = tenantId,
                    periodId = periodId,
                    dimensionType = item.dimensionType,
                    entityId = item.dimensionId,
                    entityLabel = item.dimensionLabel,
                    driverType = ProfitabilityDriverType.POSITIVE_DRIVER,
                    category = category,
                    severity = ManagementPriorityLevel.INFORMATIONAL,
                    impactAmount = item.grossProfit,
                    impactPercentage = impactPct,
                    rank = currentRank++,
                    explanation = "${item.dimensionType} '${item.dimensionLabel}' generated ৳ ${item.grossProfit} profit with ${item.margin ?: BigDecimal.ZERO}% margin.",
                    sourceReferences = listOf("DIMENSION:${item.dimensionType}:${item.dimensionId}"),
                    fingerprint = fingerprint
                )
            )
        }

        // 2. Negative Drivers: Loss-Making or Low-Margin Entities & High Cost Vendors
        val negativeEntities = dimensions
            .filter { it.grossProfit.compareTo(BigDecimal.ZERO) < 0 || (it.dimensionType == ProfitabilityDimensionType.VENDOR && it.cost.compareTo(BigDecimal.ZERO) > 0) }
            .sortedWith(
                compareBy<DimensionInsight> { it.grossProfit }
                    .thenByDescending { it.cost }
                    .thenBy { it.dimensionId }
            )

        var negRank = 1
        for (item in negativeEntities) {
            val impactAmount = if (item.grossProfit.compareTo(BigDecimal.ZERO) < 0) item.grossProfit.abs() else item.cost
            val impactPct = ProfitabilityIntelligenceMathUtils.calculateSharePercentage(impactAmount, scaledCost.max(BigDecimal.ONE))

            val category = when {
                item.grossProfit.compareTo(BigDecimal.ZERO) < 0 -> ProfitabilityDriverCategory.PROFIT_DECLINE
                item.dimensionType == ProfitabilityDimensionType.VENDOR -> ProfitabilityDriverCategory.HIGH_VENDOR_COST
                (item.margin ?: BigDecimal.ZERO).compareTo(BigDecimal("10.0000")) < 0 -> ProfitabilityDriverCategory.LOW_MARGIN
                else -> ProfitabilityDriverCategory.HIGH_COST
            }

            val severity = when {
                impactAmount.compareTo(BigDecimal("100000.0000")) >= 0 -> ManagementPriorityLevel.CRITICAL
                impactAmount.compareTo(BigDecimal("50000.0000")) >= 0 -> ManagementPriorityLevel.HIGH
                impactAmount.compareTo(BigDecimal("10000.0000")) >= 0 -> ManagementPriorityLevel.MEDIUM
                else -> ManagementPriorityLevel.LOW
            }

            val fingerprint = ProfitabilityIntelligenceMathUtils.generateProvenanceFingerprint(
                tenantId = tenantId,
                periodId = periodId,
                sourceModule = "MODULE_16_STEP_07",
                sourceEntityType = item.dimensionType.name,
                sourceEntityId = item.dimensionId,
                sourceTransactionId = "DRIVER_NEG_${item.dimensionId}",
                metricType = "COST_PRESSURE"
            )

            drivers.add(
                ProfitabilityDriver(
                    driverId = "drv-neg-${item.dimensionType.name.lowercase()}-${item.dimensionId}",
                    snapshotId = "",
                    tenantId = tenantId,
                    periodId = periodId,
                    dimensionType = item.dimensionType,
                    entityId = item.dimensionId,
                    entityLabel = item.dimensionLabel,
                    driverType = ProfitabilityDriverType.NEGATIVE_DRIVER,
                    category = category,
                    severity = severity,
                    impactAmount = impactAmount,
                    impactPercentage = impactPct,
                    rank = negRank++,
                    explanation = "${item.dimensionType} '${item.dimensionLabel}' creates cost/margin drag of ৳ $impactAmount (${item.margin ?: BigDecimal.ZERO}% margin).",
                    sourceReferences = listOf("DIMENSION:${item.dimensionType}:${item.dimensionId}"),
                    fingerprint = fingerprint
                )
            )
        }

        // 3. Cross-Dimensional Relationship Drivers (e.g. Customer + Product combos with high cost / low margin)
        val negativeRelationships = relationships
            .filter { it.grossProfit.compareTo(BigDecimal.ZERO) < 0 }
            .sortedWith(
                compareBy<ProfitabilityRelationshipInsight> { it.grossProfit }
                    .thenBy { it.fromEntityId }
            )

        for (rel in negativeRelationships) {
            val impact = rel.grossProfit.abs()
            val impactPct = ProfitabilityIntelligenceMathUtils.calculateSharePercentage(impact, scaledCost.max(BigDecimal.ONE))

            val fingerprint = ProfitabilityIntelligenceMathUtils.generateProvenanceFingerprint(
                tenantId = tenantId,
                periodId = periodId,
                sourceModule = "MODULE_16_STEP_07",
                sourceEntityType = "RELATIONSHIP",
                sourceEntityId = rel.relationshipId,
                sourceTransactionId = "DRIVER_REL_${rel.relationshipId}",
                metricType = "RELATIONSHIP_LOSS"
            )

            drivers.add(
                ProfitabilityDriver(
                    driverId = "drv-rel-${rel.relationshipId}",
                    snapshotId = "",
                    tenantId = tenantId,
                    periodId = periodId,
                    dimensionType = rel.fromDimensionType,
                    entityId = "${rel.fromEntityId}:${rel.toEntityId}",
                    entityLabel = "${rel.fromEntityLabel} ➔ ${rel.toEntityLabel}",
                    driverType = ProfitabilityDriverType.NEGATIVE_DRIVER,
                    category = ProfitabilityDriverCategory.LOW_MARGIN,
                    severity = ManagementPriorityLevel.HIGH,
                    impactAmount = impact,
                    impactPercentage = impactPct,
                    rank = negRank++,
                    explanation = "Relationship ${rel.fromEntityLabel} ➔ ${rel.toEntityLabel} resulted in net loss of ৳ $impact (${rel.grossMargin ?: BigDecimal.ZERO}% margin).",
                    sourceReferences = listOf("RELATIONSHIP:${rel.relationshipId}"),
                    fingerprint = fingerprint
                )
            )
        }

        return drivers
    }
}
