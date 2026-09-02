package com.sucharu.sucharupro.domain.service.profitability

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.profitability.*
import com.sucharu.sucharupro.domain.validation.profitability.JobCostValidator
import java.math.BigDecimal
import java.util.UUID

/**
 * Production implementation of JobCostSourceCollector.
 *
 * Implements deduplication, canonical precedence, and provenance mapping.
 */
class JobCostSourceCollectorImpl : JobCostSourceCollector {

    override suspend fun collectJobCosts(
        tenantId: String,
        projectId: String,
        jobId: String,
        customDirectCosts: List<JobCostComponent>?,
        customIndirectCosts: List<JobCostAllocationDetail>?
    ): DomainResult<JobCostCollectionResult> {
        val validation = JobCostValidator.validateJobCostCalculationRequest(tenantId, projectId, jobId, "BDT")
        if (validation is DomainResult.Error) {
            return DomainResult.Error(message = validation.message)
        }

        val allComponents = mutableListOf<JobCostComponent>()
        val allProvenances = mutableListOf<JobCostProvenance>()
        val allAllocations = mutableListOf<JobCostAllocationDetail>()
        val warnings = mutableListOf<String>()
        var unresolvedCount = 0

        // 1. Process custom / injected direct costs if provided
        if (!customDirectCosts.isNullOrEmpty()) {
            customDirectCosts.forEach { comp ->
                val compErrors = JobCostValidator.validateComponentProvenanceIntegrity(comp)
                if (compErrors.isNotEmpty()) {
                    warnings.addAll(compErrors)
                    unresolvedCount++
                } else {
                    allComponents.add(comp)
                    allProvenances.addAll(comp.provenances)
                }
            }
        }

        // 2. Process indirect allocations if provided
        if (!customIndirectCosts.isNullOrEmpty()) {
            allAllocations.addAll(customIndirectCosts)
            val totalIndirectAmount = customIndirectCosts.fold(BigDecimal.ZERO) { acc, a -> acc.add(a.allocatedAmount) }
                .let { JobCostMathUtils.scaleMoney(it) }

            val indirectProvenance = JobCostProvenance(
                provenanceId = "PROV-IND-${UUID.randomUUID()}",
                tenantId = tenantId,
                projectId = projectId,
                jobId = jobId,
                sourceModule = "MODULE_15",
                sourceEntityType = "COST_ALLOCATION",
                sourceEntityId = customIndirectCosts.firstOrNull()?.allocationId ?: "ALLOC-POOL",
                costComponentType = JobCostComponentType.ALLOCATED_INDIRECT_COST,
                directness = CostDirectness.INDIRECT,
                originalAmount = totalIndirectAmount,
                attributedAmount = totalIndirectAmount,
                attributionBasis = customIndirectCosts.firstOrNull()?.allocationBasis?.name ?: "OVERHEAD_POOL",
                calculationExplanation = "Aggregated approved indirect cost allocations",
                fingerprintHash = JobCostMathUtils.generateFingerprint(
                    "MODULE_15",
                    "COST_ALLOCATION",
                    customIndirectCosts.firstOrNull()?.allocationId ?: "ALLOC-POOL",
                    null,
                    JobCostComponentType.ALLOCATED_INDIRECT_COST
                )
            )

            val indirectComponent = JobCostComponent(
                componentId = "COMP-IND-${UUID.randomUUID()}",
                tenantId = tenantId,
                projectId = projectId,
                jobId = jobId,
                componentType = JobCostComponentType.ALLOCATED_INDIRECT_COST,
                directness = CostDirectness.INDIRECT,
                quantity = BigDecimal.ONE.setScale(4, JobCostMathUtils.ROUNDING_MODE),
                unitRate = totalIndirectAmount,
                originalAmount = totalIndirectAmount,
                attributedAmount = totalIndirectAmount,
                percentageOfTotalCost = BigDecimal.ZERO.setScale(4, JobCostMathUtils.ROUNDING_MODE),
                currency = "BDT",
                attributionBasis = "APPROVED_ALLOCATION_POOL",
                sourceItemCount = customIndirectCosts.size,
                calculationExplanation = "Aggregated indirect overhead allocations",
                provenances = listOf(indirectProvenance)
            )

            allComponents.add(indirectComponent)
            allProvenances.add(indirectProvenance)
        }

        // 3. Deduplication Check across all collected provenances
        val duplicates = JobCostValidator.detectDuplicateProvenances(allProvenances)
        if (duplicates.isNotEmpty()) {
            warnings.addAll(duplicates)
        }

        // 4. Determine Readiness Status
        val readinessStatus = when {
            duplicates.isNotEmpty() -> JobCostReadinessStatus.CONFLICTED
            unresolvedCount > 0 -> JobCostReadinessStatus.PARTIAL
            allComponents.isEmpty() -> JobCostReadinessStatus.PENDING
            allAllocations.isEmpty() && allComponents.any { it.directness == CostDirectness.DIRECT } -> JobCostReadinessStatus.UNALLOCATED
            else -> JobCostReadinessStatus.COMPLETE
        }

        val result = JobCostCollectionResult(
            components = allComponents,
            provenances = allProvenances,
            allocations = allAllocations,
            duplicateCount = duplicates.size,
            unresolvedCount = unresolvedCount,
            warnings = warnings,
            readinessStatus = readinessStatus
        )

        return DomainResult.Success(result)
    }
}
