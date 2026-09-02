package com.sucharu.sucharupro.data.datasource.profitability

import com.sucharu.sucharupro.domain.model.profitability.*

/**
 * Data Source interface for Job Actual Cost persistence.
 */
interface JobCostDataSource {

    suspend fun saveSnapshot(snapshot: JobCostSnapshot): JobCostSnapshot

    suspend fun findSnapshotById(tenantId: String, projectId: String, snapshotId: String): JobCostSnapshot?

    suspend fun findLatestSnapshotByJobId(tenantId: String, projectId: String, jobId: String): JobCostSnapshot?

    suspend fun listSnapshots(
        tenantId: String,
        projectId: String,
        jobId: String? = null,
        limit: Int = 50,
        offset: Int = 0
    ): List<JobCostSnapshot>

    suspend fun saveReconciliationEvent(event: JobCostReconciliationEvent): JobCostReconciliationEvent

    suspend fun listReconciliationEvents(
        tenantId: String,
        projectId: String,
        jobId: String? = null,
        snapshotId: String? = null,
        limit: Int = 50,
        offset: Int = 0
    ): List<JobCostReconciliationEvent>

    suspend fun recordAuditEvent(event: JobCostAuditEvent): JobCostAuditEvent

    suspend fun listAuditEvents(
        tenantId: String,
        projectId: String,
        jobId: String? = null,
        limit: Int = 50,
        offset: Int = 0
    ): List<JobCostAuditEvent>
}
