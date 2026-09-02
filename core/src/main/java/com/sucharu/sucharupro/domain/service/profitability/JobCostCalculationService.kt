package com.sucharu.sucharupro.domain.service.profitability

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.profitability.*
import java.math.BigDecimal

/**
 * Authoritative Job Actual Cost Calculation Service (Module 16 Step 02).
 */
interface JobCostCalculationService {

    /**
     * Executes the complete Job actual cost calculation pipeline.
     */
    suspend fun calculateJobActualCost(
        tenantId: String,
        projectId: String,
        jobId: String,
        jobNumber: String? = null,
        customerId: String? = null,
        productId: String? = null,
        jobQuantity: Int = 0,
        customDirectCosts: List<JobCostComponent>? = null,
        customIndirectCosts: List<JobCostAllocationDetail>? = null,
        customEstimatedCost: BigDecimal? = null,
        idempotencyKey: String? = null,
        actor: String = "SYSTEM"
    ): DomainResult<JobCostSnapshot>

    suspend fun getJobActualCostSnapshot(
        tenantId: String,
        projectId: String,
        jobId: String
    ): DomainResult<JobCostSnapshot>

    suspend fun getJobCostSnapshotById(
        tenantId: String,
        projectId: String,
        snapshotId: String
    ): DomainResult<JobCostSnapshot>

    suspend fun listJobCostSnapshots(
        tenantId: String,
        projectId: String,
        jobId: String? = null,
        limit: Int = 50,
        offset: Int = 0
    ): DomainResult<List<JobCostSnapshot>>

    suspend fun reconcileJobCost(
        tenantId: String,
        projectId: String,
        snapshotId: String,
        actor: String = "SYSTEM"
    ): DomainResult<JobCostReconciliationEvent>

    suspend fun listReconciliationEvents(
        tenantId: String,
        projectId: String,
        jobId: String? = null,
        snapshotId: String? = null,
        limit: Int = 50,
        offset: Int = 0
    ): DomainResult<List<JobCostReconciliationEvent>>

    suspend fun listAuditEvents(
        tenantId: String,
        projectId: String,
        jobId: String? = null,
        limit: Int = 50,
        offset: Int = 0
    ): DomainResult<List<JobCostAuditEvent>>
}
