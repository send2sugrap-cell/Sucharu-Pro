package com.sucharu.sucharupro.data.repository.profitability

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.profitability.*

/**
 * Domain Repository interface for Job Actual Cost snapshots and events.
 */
interface JobCostRepository {

    suspend fun saveSnapshot(snapshot: JobCostSnapshot): DomainResult<JobCostSnapshot>

    suspend fun getSnapshotById(tenantId: String, projectId: String, snapshotId: String): DomainResult<JobCostSnapshot>

    suspend fun getLatestSnapshotByJobId(tenantId: String, projectId: String, jobId: String): DomainResult<JobCostSnapshot>

    suspend fun listSnapshots(
        tenantId: String,
        projectId: String,
        jobId: String? = null,
        limit: Int = 50,
        offset: Int = 0
    ): DomainResult<List<JobCostSnapshot>>

    suspend fun recordReconciliationEvent(event: JobCostReconciliationEvent): DomainResult<JobCostReconciliationEvent>

    suspend fun listReconciliationEvents(
        tenantId: String,
        projectId: String,
        jobId: String? = null,
        snapshotId: String? = null,
        limit: Int = 50,
        offset: Int = 0
    ): DomainResult<List<JobCostReconciliationEvent>>

    suspend fun recordAuditEvent(event: JobCostAuditEvent): DomainResult<JobCostAuditEvent>

    suspend fun listAuditEvents(
        tenantId: String,
        projectId: String,
        jobId: String? = null,
        limit: Int = 50,
        offset: Int = 0
    ): DomainResult<List<JobCostAuditEvent>>
}
