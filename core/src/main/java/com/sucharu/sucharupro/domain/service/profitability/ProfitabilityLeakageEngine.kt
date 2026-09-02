package com.sucharu.sucharupro.domain.service.profitability

import com.sucharu.sucharupro.domain.model.profitability.*
import java.math.BigDecimal

/**
 * Profit Leakage Engine for deterministic identification of measurable areas where
 * profitability is being degraded.
 * Module 16 Step 07.
 */
interface ProfitabilityLeakageEngine {
    fun detectLeakages(
        tenantId: String,
        periodId: String,
        totalRevenue: BigDecimal,
        totalCost: BigDecimal,
        dimensions: List<DimensionInsight>,
        relationships: List<ProfitabilityRelationshipInsight>
    ): List<ProfitLeakageItem>
}

class ProfitabilityLeakageEngineImpl : ProfitabilityLeakageEngine {

    override fun detectLeakages(
        tenantId: String,
        periodId: String,
        totalRevenue: BigDecimal,
        totalCost: BigDecimal,
        dimensions: List<DimensionInsight>,
        relationships: List<ProfitabilityRelationshipInsight>
    ): List<ProfitLeakageItem> {
        val leakages = mutableListOf<ProfitLeakageItem>()

        if (dimensions.isEmpty()) {
            return leakages
        }

        // 1. Loss-making Customers
        val lossMakingCustomers = dimensions.filter {
            it.dimensionType == ProfitabilityDimensionType.CUSTOMER && it.grossProfit.compareTo(BigDecimal.ZERO) < 0
        }
        for (cust in lossMakingCustomers) {
            val loss = cust.grossProfit.abs()
            val severity = when {
                loss.compareTo(BigDecimal("50000.0000")) >= 0 -> ManagementPriorityLevel.CRITICAL
                loss.compareTo(BigDecimal("10000.0000")) >= 0 -> ManagementPriorityLevel.HIGH
                else -> ManagementPriorityLevel.MEDIUM
            }

            leakages.add(
                ProfitLeakageItem(
                    leakageId = "leak-cust-${cust.dimensionId}",
                    snapshotId = "",
                    tenantId = tenantId,
                    periodId = periodId,
                    dimensionType = ProfitabilityDimensionType.CUSTOMER,
                    entityId = cust.dimensionId,
                    entityLabel = cust.dimensionLabel,
                    category = ProfitLeakageCategory.LOW_MARGIN_CUSTOMER,
                    estimatedImpact = loss,
                    revenueContext = cust.revenue,
                    costContext = cust.cost,
                    profitImpact = cust.grossProfit,
                    severity = severity,
                    confidence = ProfitabilityConfidenceStatus.HIGH,
                    sourceIntegrityStatus = "VALID",
                    recommendedActionCode = RecommendedActionCode.REVIEW_CUSTOMER_MARGIN,
                    provenanceReferences = listOf("CUSTOMER:${cust.dimensionId}")
                )
            )
        }

        // 2. Loss-making Products
        val lossMakingProducts = dimensions.filter {
            it.dimensionType == ProfitabilityDimensionType.PRODUCT && it.grossProfit.compareTo(BigDecimal.ZERO) < 0
        }
        for (prod in lossMakingProducts) {
            val loss = prod.grossProfit.abs()
            val severity = when {
                loss.compareTo(BigDecimal("50000.0000")) >= 0 -> ManagementPriorityLevel.CRITICAL
                loss.compareTo(BigDecimal("10000.0000")) >= 0 -> ManagementPriorityLevel.HIGH
                else -> ManagementPriorityLevel.MEDIUM
            }

            leakages.add(
                ProfitLeakageItem(
                    leakageId = "leak-prod-${prod.dimensionId}",
                    snapshotId = "",
                    tenantId = tenantId,
                    periodId = periodId,
                    dimensionType = ProfitabilityDimensionType.PRODUCT,
                    entityId = prod.dimensionId,
                    entityLabel = prod.dimensionLabel,
                    category = ProfitLeakageCategory.LOW_MARGIN_PRODUCT,
                    estimatedImpact = loss,
                    revenueContext = prod.revenue,
                    costContext = prod.cost,
                    profitImpact = prod.grossProfit,
                    severity = severity,
                    confidence = ProfitabilityConfidenceStatus.HIGH,
                    sourceIntegrityStatus = "VALID",
                    recommendedActionCode = RecommendedActionCode.REVIEW_PRODUCT_MARGIN,
                    provenanceReferences = listOf("PRODUCT:${prod.dimensionId}")
                )
            )
        }

        // 3. Loss-making Jobs (Job Cost Overruns)
        val lossMakingJobs = dimensions.filter {
            it.dimensionType == ProfitabilityDimensionType.JOB && it.grossProfit.compareTo(BigDecimal.ZERO) < 0
        }
        for (job in lossMakingJobs) {
            val loss = job.grossProfit.abs()
            val severity = when {
                loss.compareTo(BigDecimal("30000.0000")) >= 0 -> ManagementPriorityLevel.CRITICAL
                loss.compareTo(BigDecimal("5000.0000")) >= 0 -> ManagementPriorityLevel.HIGH
                else -> ManagementPriorityLevel.MEDIUM
            }

            leakages.add(
                ProfitLeakageItem(
                    leakageId = "leak-job-${job.dimensionId}",
                    snapshotId = "",
                    tenantId = tenantId,
                    periodId = periodId,
                    dimensionType = ProfitabilityDimensionType.JOB,
                    entityId = job.dimensionId,
                    entityLabel = job.dimensionLabel,
                    category = ProfitLeakageCategory.JOB_COST_OVERRUN,
                    estimatedImpact = loss,
                    revenueContext = job.revenue,
                    costContext = job.cost,
                    profitImpact = job.grossProfit,
                    severity = severity,
                    confidence = ProfitabilityConfidenceStatus.HIGH,
                    sourceIntegrityStatus = "VALID",
                    recommendedActionCode = RecommendedActionCode.REVIEW_JOB_COST,
                    provenanceReferences = listOf("JOB:${job.dimensionId}")
                )
            )
        }

        // 4. High-Cost Vendor Pressure
        val highCostVendors = dimensions.filter {
            it.dimensionType == ProfitabilityDimensionType.VENDOR && it.cost.compareTo(BigDecimal("50000.0000")) >= 0
        }
        for (ven in highCostVendors) {
            val costShare = ven.shareOfCost
            if (costShare.compareTo(BigDecimal("25.0000")) >= 0) {
                leakages.add(
                    ProfitLeakageItem(
                        leakageId = "leak-ven-${ven.dimensionId}",
                        snapshotId = "",
                        tenantId = tenantId,
                        periodId = periodId,
                        dimensionType = ProfitabilityDimensionType.VENDOR,
                        entityId = ven.dimensionId,
                        entityLabel = ven.dimensionLabel,
                        category = ProfitLeakageCategory.VENDOR_COST_PRESSURE,
                        estimatedImpact = ven.cost.multiply(BigDecimal("0.1000")), // Estimated 10% rate optimization impact
                        revenueContext = BigDecimal.ZERO,
                        costContext = ven.cost,
                        profitImpact = ven.cost.negate(),
                        severity = ManagementPriorityLevel.HIGH,
                        confidence = ProfitabilityConfidenceStatus.MEDIUM,
                        sourceIntegrityStatus = "VALID",
                        recommendedActionCode = RecommendedActionCode.REVIEW_VENDOR_RATE,
                        provenanceReferences = listOf("VENDOR:${ven.dimensionId}")
                    )
                )
            }
        }

        // 5. Cross-Dimensional Loss Combinations
        val lossRelations = relationships.filter { it.grossProfit.compareTo(BigDecimal.ZERO) < 0 }
        for (rel in lossRelations) {
            val loss = rel.grossProfit.abs()
            leakages.add(
                ProfitLeakageItem(
                    leakageId = "leak-rel-${rel.relationshipId}",
                    snapshotId = "",
                    tenantId = tenantId,
                    periodId = periodId,
                    dimensionType = rel.fromDimensionType,
                    entityId = "${rel.fromEntityId}:${rel.toEntityId}",
                    entityLabel = "${rel.fromEntityLabel} ➔ ${rel.toEntityLabel}",
                    category = ProfitLeakageCategory.MARGIN_COMPRESSION,
                    estimatedImpact = loss,
                    revenueContext = rel.revenue,
                    costContext = rel.cost,
                    profitImpact = rel.grossProfit,
                    severity = ManagementPriorityLevel.MEDIUM,
                    confidence = ProfitabilityConfidenceStatus.HIGH,
                    sourceIntegrityStatus = "VALID",
                    recommendedActionCode = RecommendedActionCode.REVIEW_DISCOUNT,
                    provenanceReferences = listOf("RELATIONSHIP:${rel.relationshipId}")
                )
            )
        }

        return leakages.sortedWith(
            compareByDescending<ProfitLeakageItem> { it.estimatedImpact }
                .thenBy { it.entityId }
        )
    }
}
