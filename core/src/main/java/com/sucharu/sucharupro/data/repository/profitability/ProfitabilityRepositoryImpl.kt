package com.sucharu.sucharupro.data.repository.profitability

import com.sucharu.sucharupro.data.datasource.profitability.ProfitabilityDataSource
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.profitability.*

/**
 * Production implementation of ProfitabilityRepository.
 */
class ProfitabilityRepositoryImpl(
    private val dataSource: ProfitabilityDataSource
) : ProfitabilityRepository {

    override suspend fun saveSnapshot(snapshot: ProfitabilitySnapshot): DomainResult<ProfitabilitySnapshot> {
        return try {
            val saved = dataSource.saveSnapshot(snapshot)
            DomainResult.Success(saved)
        } catch (e: Exception) {
            DomainResult.Error(exception = e, message = e.message ?: "Failed to save profitability snapshot")
        }
    }

    override suspend fun getSnapshotById(
        tenantId: String,
        projectId: String,
        id: String
    ): DomainResult<ProfitabilitySnapshot> {
        return try {
            val snapshot = dataSource.findSnapshotById(tenantId, projectId, id)
            if (snapshot != null) {
                DomainResult.Success(snapshot)
            } else {
                DomainResult.Error(message = "Profitability snapshot not found for id: $id")
            }
        } catch (e: Exception) {
            DomainResult.Error(exception = e, message = e.message ?: "Failed to retrieve profitability snapshot")
        }
    }

    override suspend fun listSnapshots(
        tenantId: String,
        projectId: String,
        scope: ProfitabilityScope?,
        targetEntityId: String?,
        periodId: String?,
        limit: Int,
        offset: Int
    ): DomainResult<List<ProfitabilitySnapshot>> {
        return try {
            val list = dataSource.listSnapshots(
                tenantId = tenantId,
                projectId = projectId,
                scope = scope,
                targetEntityId = targetEntityId,
                periodId = periodId,
                limit = limit,
                offset = offset
            )
            DomainResult.Success(list)
        } catch (e: Exception) {
            DomainResult.Error(exception = e, message = e.message ?: "Failed to list profitability snapshots")
        }
    }

    override suspend fun recordReconciliationEvent(event: ProfitabilityReconciliationEvent): DomainResult<ProfitabilityReconciliationEvent> {
        return try {
            val saved = dataSource.saveReconciliationEvent(event)
            DomainResult.Success(saved)
        } catch (e: Exception) {
            DomainResult.Error(exception = e, message = e.message ?: "Failed to record reconciliation event")
        }
    }

    override suspend fun listReconciliationEvents(
        tenantId: String,
        projectId: String,
        snapshotId: String?,
        limit: Int,
        offset: Int
    ): DomainResult<List<ProfitabilityReconciliationEvent>> {
        return try {
            val list = dataSource.listReconciliationEvents(
                tenantId = tenantId,
                projectId = projectId,
                snapshotId = snapshotId,
                limit = limit,
                offset = offset
            )
            DomainResult.Success(list)
        } catch (e: Exception) {
            DomainResult.Error(exception = e, message = e.message ?: "Failed to list reconciliation events")
        }
    }

    override suspend fun recordAuditEvent(event: ProfitabilityAuditEvent): DomainResult<ProfitabilityAuditEvent> {
        return try {
            val saved = dataSource.recordAuditEvent(event)
            DomainResult.Success(saved)
        } catch (e: Exception) {
            DomainResult.Error(exception = e, message = e.message ?: "Failed to record analytical audit event")
        }
    }

    override suspend fun listAuditEvents(
        tenantId: String,
        projectId: String,
        snapshotId: String?,
        limit: Int,
        offset: Int
    ): DomainResult<List<ProfitabilityAuditEvent>> {
        return try {
            val list = dataSource.listAuditEvents(
                tenantId = tenantId,
                projectId = projectId,
                snapshotId = snapshotId,
                limit = limit,
                offset = offset
            )
            DomainResult.Success(list)
        } catch (e: Exception) {
            DomainResult.Error(exception = e, message = e.message ?: "Failed to list analytical audit events")
        }
    }
}
