package com.sucharu.sucharupro.domain.service.profitability

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.profitability.*

/**
 * Result of aggregating raw canonical cost sources for a Job.
 */
data class JobCostCollectionResult(
    val components: List<JobCostComponent>,
    val provenances: List<JobCostProvenance>,
    val allocations: List<JobCostAllocationDetail>,
    val duplicateCount: Int,
    val unresolvedCount: Int,
    val warnings: List<String>,
    val readinessStatus: JobCostReadinessStatus
)

/**
 * Interface for discovering, extracting, and attributing canonical operational and financial
 * cost items for an individual Job.
 */
interface JobCostSourceCollector {

    /**
     * Collects and attributes all available canonical costs for a specific Job.
     */
    suspend fun collectJobCosts(
        tenantId: String,
        projectId: String,
        jobId: String,
        customDirectCosts: List<JobCostComponent>? = null,
        customIndirectCosts: List<JobCostAllocationDetail>? = null
    ): DomainResult<JobCostCollectionResult>
}
