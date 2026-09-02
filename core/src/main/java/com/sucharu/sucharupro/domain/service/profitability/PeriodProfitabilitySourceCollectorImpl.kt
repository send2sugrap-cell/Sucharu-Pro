package com.sucharu.sucharupro.domain.service.profitability

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.profitability.*
import java.math.BigDecimal

/**
 * Implementation of multi-source collector for Period Profitability.
 * Module 16 Step 06.
 */
class PeriodProfitabilitySourceCollectorImpl : PeriodProfitabilitySourceCollector {

    override suspend fun collectPeriodData(
        tenantId: String,
        projectId: String,
        periodId: String,
        periodType: PeriodType,
        periodStart: Long,
        periodEnd: Long,
        customRevenueAttributions: List<PeriodRevenueAttributionItem>?,
        customCostBreakdown: List<PeriodCostBreakdownItem>?,
        customProvenance: List<PeriodProfitabilityProvenanceRecord>?
    ): DomainResult<PeriodSourceCollectionResult> {
        val warnings = mutableListOf<String>()
        val unattributed = mutableListOf<PeriodUnattributedItem>()

        // 1. Revenue Attributions & Total Revenue
        val revenues = customRevenueAttributions ?: emptyList()
        val totalRevenue = revenues.fold(BigDecimal.ZERO) { acc, item ->
            acc + item.amount
        }.setScale(PeriodProfitabilityMathUtils.SCALE, PeriodProfitabilityMathUtils.ROUNDING)

        // 2. Cost Components & Total Cost
        val costItems = customCostBreakdown ?: emptyList()
        val totalCost = costItems.fold(BigDecimal.ZERO) { acc, item ->
            acc + item.amount
        }.setScale(PeriodProfitabilityMathUtils.SCALE, PeriodProfitabilityMathUtils.ROUNDING)

        // 3. Direct vs Indirect Cost
        val directCost = costItems.filter { it.componentType != JobCostComponentType.ALLOCATED_INDIRECT_COST }
            .fold(BigDecimal.ZERO) { acc, item -> acc + item.amount }
            .setScale(PeriodProfitabilityMathUtils.SCALE, PeriodProfitabilityMathUtils.ROUNDING)

        val indirectCost = costItems.filter { it.componentType == JobCostComponentType.ALLOCATED_INDIRECT_COST }
            .fold(BigDecimal.ZERO) { acc, item -> acc + item.amount }
            .setScale(PeriodProfitabilityMathUtils.SCALE, PeriodProfitabilityMathUtils.ROUNDING)

        // 4. Provenance Records Deduplication & Verification
        val rawProvenance = customProvenance ?: emptyList()
        val seenFingerprints = mutableSetOf<String>()
        val deduplicatedProvenance = mutableListOf<PeriodProfitabilityProvenanceRecord>()

        for (rec in rawProvenance) {
            val fp = rec.fingerprint.ifBlank {
                PeriodProfitabilityMathUtils.generateProvenanceFingerprint(
                    tenantId = rec.tenantId,
                    periodId = rec.periodId,
                    sourceModule = rec.sourceModule,
                    sourceEntityType = rec.sourceEntityType,
                    sourceEntityId = rec.sourceEntityId,
                    amount = rec.amount,
                    componentType = rec.componentType?.name
                )
            }
            if (seenFingerprints.add(fp)) {
                deduplicatedProvenance.add(rec.copy(fingerprint = fp))
            } else {
                warnings.add("Duplicate provenance detected for source ${rec.sourceEntityType} (${rec.sourceEntityId}), deduplicated.")
            }
        }

        // 5. Volume metrics
        val jobCount = revenues.filter { it.attributionDimension == "JOB" }.distinctBy { it.dimensionId }.size
        val productCount = revenues.filter { it.attributionDimension == "PRODUCT" }.distinctBy { it.dimensionId }.size
        val customerCount = revenues.filter { it.attributionDimension == "CUSTOMER" }.distinctBy { it.dimensionId }.size
        val vendorCount = costItems.filter { it.componentType == JobCostComponentType.VENDOR_OUTSOURCE_COST }.size

        val readiness = when {
            totalRevenue == BigDecimal.ZERO && totalCost == BigDecimal.ZERO -> PeriodSourceReadiness.PARTIAL
            warnings.isNotEmpty() -> PeriodSourceReadiness.PARTIAL
            else -> PeriodSourceReadiness.READY
        }

        return DomainResult.Success(
            PeriodSourceCollectionResult(
                recognizedRevenue = totalRevenue,
                directCost = directCost,
                indirectCost = indirectCost,
                totalCost = totalCost,
                costBreakdown = costItems,
                revenueAttributions = revenues,
                provenanceRecords = deduplicatedProvenance,
                unattributedItems = unattributed,
                jobCount = jobCount,
                completedJobCount = jobCount,
                productCount = productCount,
                customerCount = customerCount,
                vendorCount = vendorCount,
                totalUnits = 0L,
                sourceReadiness = readiness,
                warnings = warnings
            )
        )
    }
}
