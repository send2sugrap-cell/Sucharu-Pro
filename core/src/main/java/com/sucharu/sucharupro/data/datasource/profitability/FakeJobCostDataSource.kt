package com.sucharu.sucharupro.data.datasource.profitability

import com.sucharu.sucharupro.domain.model.profitability.*
import java.util.concurrent.ConcurrentHashMap

/**
 * In-memory test double implementation of JobCostDataSource.
 */
class FakeJobCostDataSource : JobCostDataSource {

    private val snapshots = ConcurrentHashMap<String, JobCostSnapshot>()
    private val reconciliationEvents = ConcurrentHashMap<String, JobCostReconciliationEvent>()
    private val auditEvents = ConcurrentHashMap<String, JobCostAuditEvent>()

    override suspend fun saveSnapshot(snapshot: JobCostSnapshot): JobCostSnapshot {
        snapshots[snapshot.snapshotId] = snapshot
        return snapshot
    }

    override suspend fun findSnapshotById(tenantId: String, projectId: String, snapshotId: String): JobCostSnapshot? {
        val s = snapshots[snapshotId] ?: return null
        return if (s.tenantId == tenantId && s.projectId == projectId) s else null
    }

    override suspend fun findLatestSnapshotByJobId(tenantId: String, projectId: String, jobId: String): JobCostSnapshot? {
        return snapshots.values
            .filter { it.tenantId == tenantId && it.projectId == projectId && it.jobId == jobId }
            .maxByOrNull { it.calculationTimestamp }
    }

    override suspend fun listSnapshots(
        tenantId: String,
        projectId: String,
        jobId: String?,
        limit: Int,
        offset: Int
    ): List<JobCostSnapshot> {
        return snapshots.values
            .filter { it.tenantId == tenantId && it.projectId == projectId }
            .filter { jobId == null || it.jobId == jobId }
            .sortedByDescending { it.calculationTimestamp }
            .drop(offset)
            .take(limit)
    }

    override suspend fun saveReconciliationEvent(event: JobCostReconciliationEvent): JobCostReconciliationEvent {
        reconciliationEvents[event.reconciliationId] = event
        return event
    }

    override suspend fun listReconciliationEvents(
        tenantId: String,
        projectId: String,
        jobId: String?,
        snapshotId: String?,
        limit: Int,
        offset: Int
    ): List<JobCostReconciliationEvent> {
        return reconciliationEvents.values
            .filter { it.tenantId == tenantId && it.projectId == projectId }
            .filter { jobId == null || it.jobId == jobId }
            .filter { snapshotId == null || it.snapshotId == snapshotId }
            .sortedByDescending { it.checkedAt }
            .drop(offset)
            .take(limit)
    }

    override suspend fun recordAuditEvent(event: JobCostAuditEvent): JobCostAuditEvent {
        auditEvents[event.eventId] = event
        return event
    }

    override suspend fun listAuditEvents(
        tenantId: String,
        projectId: String,
        jobId: String?,
        limit: Int,
        offset: Int
    ): List<JobCostAuditEvent> {
        return auditEvents.values
            .filter { it.tenantId == tenantId && it.projectId == projectId }
            .filter { jobId == null || it.jobId == jobId }
            .sortedByDescending { it.timestamp }
            .drop(offset)
            .take(limit)
    }
}
