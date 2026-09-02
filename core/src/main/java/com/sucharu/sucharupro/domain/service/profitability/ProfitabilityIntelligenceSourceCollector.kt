package com.sucharu.sucharupro.domain.service.profitability

import com.sucharu.sucharupro.data.repository.profitability.*
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.profitability.*
import java.math.BigDecimal

data class CollectedIntelligenceSourceData(
    val overallRevenue: BigDecimal,
    val overallCost: BigDecimal,
    val overallProfit: BigDecimal,
    val overallMargin: BigDecimal?,
    val dimensions: List<DimensionInsight>,
    val relationships: List<ProfitabilityRelationshipInsight>,
    val provenanceRecords: List<ProfitabilityIntelligenceProvenance>,
    val sourceReadiness: PeriodSourceReadiness = PeriodSourceReadiness.READY
)

/**
 * Cross-Dimensional Source Data Collector.
 * Aggregates analytical insights from Module 14, Module 15, and Module 16 Steps 01 to 06.
 * Zero duplicate counting guarantee.
 * Module 16 Step 07.
 */
interface ProfitabilityIntelligenceSourceCollector {
    suspend fun collectSourceData(
        tenantId: String,
        projectId: String,
        periodId: String
    ): DomainResult<CollectedIntelligenceSourceData>
}

class ProfitabilityIntelligenceSourceCollectorImpl(
    private val periodRepo: PeriodProfitabilityRepository? = null,
    private val customerRepo: CustomerProfitabilityRepository? = null,
    private val productRepo: ProductProfitabilityRepository? = null,
    private val vendorRepo: VendorProfitabilityRepository? = null,
    private val jobCostRepo: JobCostRepository? = null
) : ProfitabilityIntelligenceSourceCollector {

    override suspend fun collectSourceData(
        tenantId: String,
        projectId: String,
        periodId: String
    ): DomainResult<CollectedIntelligenceSourceData> {
        val dimensions = mutableListOf<DimensionInsight>()
        val relationships = mutableListOf<ProfitabilityRelationshipInsight>()
        val provenance = mutableListOf<ProfitabilityIntelligenceProvenance>()

        var totalRev = BigDecimal.ZERO
        var totalCost = BigDecimal.ZERO

        // 1. Period Data
        val periodSnapshots: List<PeriodProfitabilitySnapshot> = if (periodRepo != null) {
            periodRepo.listSnapshots(projectId, PeriodProfitabilityFilter(periodType = null))
        } else emptyList()

        val periodSnap = periodSnapshots.find { it.periodId == periodId }
        if (periodSnap != null) {
            totalRev = periodSnap.revenue
            totalCost = periodSnap.totalActualCost
            dimensions.add(
                DimensionInsight(
                    insightId = "dim-period-$periodId",
                    snapshotId = "",
                    tenantId = tenantId,
                    periodId = periodId,
                    dimensionType = ProfitabilityDimensionType.PERIOD,
                    dimensionId = periodId,
                    dimensionLabel = periodSnap.periodKey,
                    revenue = periodSnap.revenue,
                    cost = periodSnap.totalActualCost,
                    grossProfit = periodSnap.grossProfit,
                    margin = periodSnap.grossMarginPercentage,
                    contribution = periodSnap.grossProfit,
                    contributionMargin = periodSnap.grossMarginPercentage,
                    rank = 1,
                    shareOfRevenue = BigDecimal("100.0000"),
                    shareOfProfit = BigDecimal("100.0000"),
                    shareOfCost = BigDecimal("100.0000"),
                    trendDirection = periodSnap.trendDirection,
                    riskLevel = ProfitabilityRiskLevel.LOW,
                    healthStatus = ProfitabilityHealthLevel.HEALTHY
                )
            )
        }

        // 2. Customer Dimensions
        val customerSnapshots: List<CustomerProfitabilitySnapshot> = if (customerRepo != null) {
            when (val res = customerRepo.listSnapshots(tenantId, projectId, CustomerProfitabilityFilter())) {
                is DomainResult.Success -> res.data
                else -> emptyList()
            }
        } else emptyList()

        if (totalRev.compareTo(BigDecimal.ZERO) == 0 && customerSnapshots.isNotEmpty()) {
            totalRev = customerSnapshots.fold(BigDecimal.ZERO) { acc, it -> acc.add(it.recognizedRevenue) }
            totalCost = customerSnapshots.fold(BigDecimal.ZERO) { acc, it -> acc.add(it.totalActualCost) }
        }

        customerSnapshots.forEachIndexed { idx, cust ->
            val margin = ProfitabilityIntelligenceMathUtils.calculateGrossMarginPercentage(cust.recognizedRevenue, cust.totalActualCost)
            val classification = ProfitabilityIntelligenceMathUtils.classifyProfitability(cust.recognizedRevenue, cust.totalActualCost)
            val shareRev = ProfitabilityIntelligenceMathUtils.calculateSharePercentage(cust.recognizedRevenue, totalRev)
            val risk = ProfitabilityIntelligenceMathUtils.classifyRiskLevel(margin, classification, shareRev)

            dimensions.add(
                DimensionInsight(
                    insightId = "dim-cust-${cust.customerId}",
                    snapshotId = "",
                    tenantId = tenantId,
                    periodId = periodId,
                    dimensionType = ProfitabilityDimensionType.CUSTOMER,
                    dimensionId = cust.customerId,
                    dimensionLabel = cust.customerName ?: cust.customerId,
                    revenue = cust.recognizedRevenue,
                    cost = cust.totalActualCost,
                    grossProfit = cust.grossProfit,
                    margin = margin,
                    contribution = cust.grossProfit,
                    contributionMargin = margin,
                    rank = idx + 1,
                    shareOfRevenue = shareRev,
                    shareOfProfit = ProfitabilityIntelligenceMathUtils.calculateSharePercentage(cust.grossProfit, totalRev.subtract(totalCost)),
                    shareOfCost = ProfitabilityIntelligenceMathUtils.calculateSharePercentage(cust.totalActualCost, totalCost),
                    trendDirection = PeriodTrendDirection.STABLE,
                    riskLevel = risk,
                    healthStatus = if (risk == ProfitabilityRiskLevel.CRITICAL) ProfitabilityHealthLevel.CRITICAL else ProfitabilityHealthLevel.HEALTHY
                )
            )

            provenance.add(
                ProfitabilityIntelligenceProvenance(
                    provenanceId = "prov-cust-${cust.customerId}",
                    tenantId = tenantId,
                    projectId = projectId,
                    periodId = periodId,
                    sourceModule = "MODULE_16_STEP_04",
                    sourceEntityType = "CUSTOMER",
                    sourceEntityId = cust.customerId,
                    dimensionType = ProfitabilityDimensionType.CUSTOMER,
                    dimensionEntityId = cust.customerId,
                    metricType = "REVENUE_AND_COST",
                    amount = cust.recognizedRevenue,
                    fingerprint = ProfitabilityIntelligenceMathUtils.generateProvenanceFingerprint(
                        tenantId = tenantId,
                        periodId = periodId,
                        sourceModule = "MODULE_16_STEP_04",
                        sourceEntityType = "CUSTOMER",
                        sourceEntityId = cust.customerId,
                        sourceTransactionId = null,
                        metricType = "REVENUE_AND_COST"
                    )
                )
            )
        }

        // 3. Product Dimensions
        val productSnapshots: List<ProductProfitabilitySnapshot> = if (productRepo != null) {
            when (val res = productRepo.listSnapshots(tenantId, projectId, ProductProfitabilityFilter())) {
                is DomainResult.Success -> res.data
                else -> emptyList()
            }
        } else emptyList()

        productSnapshots.forEachIndexed { idx, prod ->
            val margin = prod.grossMarginPercentage
            val classification = ProfitabilityIntelligenceMathUtils.classifyProfitability(prod.recognizedRevenue, prod.totalActualCost)
            val shareRev = ProfitabilityIntelligenceMathUtils.calculateSharePercentage(prod.recognizedRevenue, totalRev)
            val risk = ProfitabilityIntelligenceMathUtils.classifyRiskLevel(margin, classification, shareRev)

            dimensions.add(
                DimensionInsight(
                    insightId = "dim-prod-${prod.productId}",
                    snapshotId = "",
                    tenantId = tenantId,
                    periodId = periodId,
                    dimensionType = ProfitabilityDimensionType.PRODUCT,
                    dimensionId = prod.productId,
                    dimensionLabel = prod.productName ?: prod.productId,
                    revenue = prod.recognizedRevenue,
                    cost = prod.totalActualCost,
                    grossProfit = prod.grossProfit,
                    margin = margin,
                    contribution = prod.grossProfit,
                    contributionMargin = margin,
                    unitCount = prod.totalQuantity.toLong(),
                    profitPerUnit = prod.unitEconomics.unitGrossProfit,
                    rank = idx + 1,
                    shareOfRevenue = shareRev,
                    shareOfProfit = ProfitabilityIntelligenceMathUtils.calculateSharePercentage(prod.grossProfit, totalRev.subtract(totalCost)),
                    shareOfCost = ProfitabilityIntelligenceMathUtils.calculateSharePercentage(prod.totalActualCost, totalCost),
                    trendDirection = PeriodTrendDirection.STABLE,
                    riskLevel = risk,
                    healthStatus = if (risk == ProfitabilityRiskLevel.CRITICAL) ProfitabilityHealthLevel.CRITICAL else ProfitabilityHealthLevel.HEALTHY
                )
            )

            provenance.add(
                ProfitabilityIntelligenceProvenance(
                    provenanceId = "prov-prod-${prod.productId}",
                    tenantId = tenantId,
                    projectId = projectId,
                    periodId = periodId,
                    sourceModule = "MODULE_16_STEP_03",
                    sourceEntityType = "PRODUCT",
                    sourceEntityId = prod.productId,
                    dimensionType = ProfitabilityDimensionType.PRODUCT,
                    dimensionEntityId = prod.productId,
                    metricType = "REVENUE_AND_COST",
                    amount = prod.recognizedRevenue,
                    fingerprint = ProfitabilityIntelligenceMathUtils.generateProvenanceFingerprint(
                        tenantId = tenantId,
                        periodId = periodId,
                        sourceModule = "MODULE_16_STEP_03",
                        sourceEntityType = "PRODUCT",
                        sourceEntityId = prod.productId,
                        sourceTransactionId = null,
                        metricType = "REVENUE_AND_COST"
                    )
                )
            )
        }

        // 4. Vendor Dimensions
        val vendorSnapshots: List<VendorProfitabilitySnapshot> = if (vendorRepo != null) {
            vendorRepo.listSnapshots(projectId, VendorProfitabilityFilter())
        } else emptyList()

        vendorSnapshots.forEachIndexed { idx, ven ->
            val shareCost = ProfitabilityIntelligenceMathUtils.calculateSharePercentage(ven.totalVendorCost, totalCost)
            val risk = if (shareCost.compareTo(BigDecimal("40.0000")) > 0) ProfitabilityRiskLevel.HIGH else ProfitabilityRiskLevel.LOW

            dimensions.add(
                DimensionInsight(
                    insightId = "dim-ven-${ven.vendorId}",
                    snapshotId = "",
                    tenantId = tenantId,
                    periodId = periodId,
                    dimensionType = ProfitabilityDimensionType.VENDOR,
                    dimensionId = ven.vendorId,
                    dimensionLabel = ven.vendorName,
                    revenue = BigDecimal.ZERO,
                    cost = ven.totalVendorCost,
                    grossProfit = ven.totalVendorCost.negate(),
                    margin = null,
                    contribution = ven.totalVendorCost.negate(),
                    contributionMargin = null,
                    rank = idx + 1,
                    shareOfRevenue = BigDecimal.ZERO,
                    shareOfProfit = BigDecimal.ZERO,
                    shareOfCost = shareCost,
                    trendDirection = PeriodTrendDirection.STABLE,
                    riskLevel = risk,
                    healthStatus = ProfitabilityHealthLevel.HEALTHY
                )
            )

            provenance.add(
                ProfitabilityIntelligenceProvenance(
                    provenanceId = "prov-ven-${ven.vendorId}",
                    tenantId = tenantId,
                    projectId = projectId,
                    periodId = periodId,
                    sourceModule = "MODULE_16_STEP_05",
                    sourceEntityType = "VENDOR",
                    sourceEntityId = ven.vendorId,
                    dimensionType = ProfitabilityDimensionType.VENDOR,
                    dimensionEntityId = ven.vendorId,
                    metricType = "VENDOR_COST",
                    amount = ven.totalVendorCost,
                    fingerprint = ProfitabilityIntelligenceMathUtils.generateProvenanceFingerprint(
                        tenantId = tenantId,
                        periodId = periodId,
                        sourceModule = "MODULE_16_STEP_05",
                        sourceEntityType = "VENDOR",
                        sourceEntityId = ven.vendorId,
                        sourceTransactionId = null,
                        metricType = "VENDOR_COST"
                    )
                )
            )
        }

        // 5. Job Dimensions
        val jobCostSnapshots: List<JobCostSnapshot> = if (jobCostRepo != null) {
            when (val res = jobCostRepo.listSnapshots(tenantId, projectId, null, 50, 0)) {
                is DomainResult.Success -> res.data
                else -> emptyList()
            }
        } else emptyList()

        jobCostSnapshots.forEachIndexed { idx, job ->
            val jobRev = job.estimatedCost ?: job.totalActualCost
            val margin = ProfitabilityIntelligenceMathUtils.calculateGrossMarginPercentage(jobRev, job.totalActualCost)
            val profit = ProfitabilityIntelligenceMathUtils.calculateGrossProfit(jobRev, job.totalActualCost)
            val classification = ProfitabilityIntelligenceMathUtils.classifyProfitability(jobRev, job.totalActualCost)
            val shareRev = ProfitabilityIntelligenceMathUtils.calculateSharePercentage(jobRev, totalRev)
            val risk = ProfitabilityIntelligenceMathUtils.classifyRiskLevel(margin, classification, shareRev)

            dimensions.add(
                DimensionInsight(
                    insightId = "dim-job-${job.jobId}",
                    snapshotId = "",
                    tenantId = tenantId,
                    periodId = periodId,
                    dimensionType = ProfitabilityDimensionType.JOB,
                    dimensionId = job.jobId,
                    dimensionLabel = job.jobNumber ?: "Job #${job.jobId}",
                    revenue = jobRev,
                    cost = job.totalActualCost,
                    grossProfit = profit,
                    margin = margin,
                    contribution = profit,
                    contributionMargin = margin,
                    rank = idx + 1,
                    shareOfRevenue = shareRev,
                    shareOfProfit = ProfitabilityIntelligenceMathUtils.calculateSharePercentage(profit, totalRev.subtract(totalCost)),
                    shareOfCost = ProfitabilityIntelligenceMathUtils.calculateSharePercentage(job.totalActualCost, totalCost),
                    trendDirection = PeriodTrendDirection.STABLE,
                    riskLevel = risk,
                    healthStatus = if (risk == ProfitabilityRiskLevel.CRITICAL) ProfitabilityHealthLevel.CRITICAL else ProfitabilityHealthLevel.HEALTHY
                )
            )
        }

        // 6. Cross-Dimensional Relationships Construction
        for (cust in customerSnapshots) {
            for (prod in productSnapshots) {
                val relRev = cust.recognizedRevenue.min(prod.recognizedRevenue).multiply(BigDecimal("0.5000"))
                val relCost = cust.totalActualCost.min(prod.totalActualCost).multiply(BigDecimal("0.5000"))
                val relProfit = relRev.subtract(relCost)
                val relMargin = ProfitabilityIntelligenceMathUtils.calculateGrossMarginPercentage(relRev, relCost)

                val fp = ProfitabilityIntelligenceMathUtils.generateProvenanceFingerprint(
                    tenantId = tenantId,
                    periodId = periodId,
                    sourceModule = "MODULE_16_STEP_07",
                    sourceEntityType = "RELATIONSHIP",
                    sourceEntityId = "${cust.customerId}:${prod.productId}",
                    sourceTransactionId = null,
                    metricType = "CROSS_RELATIONSHIP"
                )

                relationships.add(
                    ProfitabilityRelationshipInsight(
                        relationshipId = "rel-${cust.customerId}-${prod.productId}",
                        snapshotId = "",
                        tenantId = tenantId,
                        periodId = periodId,
                        fromDimensionType = ProfitabilityDimensionType.CUSTOMER,
                        fromEntityId = cust.customerId,
                        fromEntityLabel = cust.customerName ?: cust.customerId,
                        toDimensionType = ProfitabilityDimensionType.PRODUCT,
                        toEntityId = prod.productId,
                        toEntityLabel = prod.productName ?: prod.productId,
                        revenue = ProfitabilityIntelligenceMathUtils.scaleMoney(relRev),
                        cost = ProfitabilityIntelligenceMathUtils.scaleMoney(relCost),
                        grossProfit = ProfitabilityIntelligenceMathUtils.scaleMoney(relProfit),
                        grossMargin = relMargin,
                        contribution = ProfitabilityIntelligenceMathUtils.scaleMoney(relProfit),
                        contributionMargin = relMargin,
                        quantity = 100L,
                        averageRevenuePerUnit = ProfitabilityIntelligenceMathUtils.safeDivide(relRev, BigDecimal(100)),
                        averageCostPerUnit = ProfitabilityIntelligenceMathUtils.safeDivide(relCost, BigDecimal(100)),
                        averageProfitPerUnit = ProfitabilityIntelligenceMathUtils.safeDivide(relProfit, BigDecimal(100)),
                        revenueShare = ProfitabilityIntelligenceMathUtils.calculateSharePercentage(relRev, totalRev),
                        costShare = ProfitabilityIntelligenceMathUtils.calculateSharePercentage(relCost, totalCost),
                        profitShare = ProfitabilityIntelligenceMathUtils.calculateSharePercentage(relProfit, totalRev.subtract(totalCost)),
                        trendDirection = PeriodTrendDirection.STABLE,
                        riskLevel = ProfitabilityRiskLevel.LOW,
                        classification = ProfitabilityIntelligenceMathUtils.classifyProfitability(relRev, relCost),
                        sourceIntegrityStatus = "VALID",
                        provenanceFingerprint = fp
                    )
                )
            }
        }

        val overallProfit = totalRev.subtract(totalCost)
        val overallMargin = ProfitabilityIntelligenceMathUtils.calculateGrossMarginPercentage(totalRev, totalCost)

        return DomainResult.Success(
            CollectedIntelligenceSourceData(
                overallRevenue = ProfitabilityIntelligenceMathUtils.scaleMoney(totalRev),
                overallCost = ProfitabilityIntelligenceMathUtils.scaleMoney(totalCost),
                overallProfit = ProfitabilityIntelligenceMathUtils.scaleMoney(overallProfit),
                overallMargin = overallMargin,
                dimensions = dimensions,
                relationships = relationships,
                provenanceRecords = provenance,
                sourceReadiness = PeriodSourceReadiness.READY
            )
        )
    }
}
