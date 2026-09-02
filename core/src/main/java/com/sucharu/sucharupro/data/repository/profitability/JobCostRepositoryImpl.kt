package com.sucharu.sucharupro.data.repository.profitability

import com.sucharu.sucharupro.data.datasource.profitability.JobCostDataSource
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.profitability.*

/**
 * Production implementation of JobCostRepository.
 */
class JobCostRepositoryImpl(
    private val dataSource: JobCostDataSource
) : JobCostRepository {

    override suspend fun saveSnapshot(snapshot: JobCostSnapshot): DomainResult<JobCostSnapshot> {
        return try {
            val saved = dataSource.saveSnapshot(snapshot)
            DomainResult.Success(saved)
        } catch (e: Exception) {
            DomainResult.Error(exception = e, message = e.message ?: "Failed to save Job cost snapshot")
        }
    }

    override suspend fun getSnapshotById(
        tenantId: String,
        projectId: String,
        snapshotId: String
    ): DomainResult<JobCostSnapshot> {
        return try {
            val snapshot = dataSource.findSnapshotById(tenantId, projectId, snapshotId)
            if (snapshot != null) {
                DomainResult.Success(snapshot)
            } else {
                DomainResult.Error(message = "Job cost snapshot not found for id: $snapshotId")
            }
        } catch (e: Exception) {
            DomainResult.Error(exception = e, message = e.message ?: "Failed to retrieve Job cost snapshot")
        }
    }

    override suspend fun getLatestSnapshotByJobId(
        tenantId: String,
        projectId: String,
        jobId: String
    ): DomainResult<JobCostSnapshot> {
        return try {
            val snapshot = dataSource.findLatestSnapshotByJobId(tenantId, projectId, jobId)
            if (snapshot != null) {
                DomainResult.Success(snapshot)
            } else {
                DomainResult.Error(message = "Job cost snapshot not found for Job: $jobId")
            }
        } catch (e: Exception) {
            DomainResult.Error(exception = e, message = e.message ?: "Failed to retrieve latest Job cost snapshot")
        }
    }

    override suspend fun listSnapshots(
        tenantId: String,
        projectId: String,
        jobId: String?,
        limit: Int,
        offset: Int
    ): DomainResult<List<JobCostSnapshot>> {
        return try {
            val list = dataSource.listSnapshots(tenantId, projectId, jobId, limit, offset)
            DomainResult.Success(list)
        } catch (e: Exception) {
            DomainResult.Error(exception = e, message = e.message ?: "Failed to list Job cost snapshots")
        }
    }

    override suspend fun recordReconciliationEvent(event: JobCostReconciliationEvent): DomainResult<JobCostReconciliationEvent> {
        return try {
            val saved = dataSource.saveReconciliationEvent(event)
            DomainResult.Success(saved)
        } catch (e: Exception) {
            DomainResult.Error(exception = e, message = e.message ?: "Failed to record Job cost reconciliation event")
        }
    }

    override suspend fun listReconciliationEvents(
        tenantId: String,
        projectId: String,
        jobId: String?,
        snapshotId: String?,
        limit: Int,
        offset: Int
    ): DomainResult<List<JobCostReconciliationEvent>> {
        return try {
            val list = dataSource.listReconciliationEvents(tenantId, projectId, jobId, snapshotId, limit, offset)
            DomainResult.Success(list)
        } catch (e: Exception) {
            DomainResult.Error(exception = e, message = e.message ?: "Failed to list Job cost reconciliation events")
        }
    }

    override suspend fun recordAuditEvent(event: JobCostAuditEvent): DomainResult<JobCostAuditEvent> {
        return try {
            val saved = dataSource.recordAuditEvent(event)
            DomainResult.Success(saved)
        } catch (e: Exception) {
            DomainResult.Error(exception = e, message = e.message ?: "Failed to record Job cost audit event")
        }
    }

    override suspend fun listAuditEvents(
        tenantId: String,
        projectId: String,
        jobId: String?,
        limit: Int,
        offset: Int
    ): DomainResult<List<JobCostAuditEvent>> {
        return try {
            val list = dataSource.listAuditEvents(tenantId, projectId, jobId, limit, offset)
            DomainResult.Success(list)
        } catch (e: Exception) {
            DomainResult.Error(exception = e, message = e.message ?: "Failed to list Job cost audit events")
        }
    }
}
