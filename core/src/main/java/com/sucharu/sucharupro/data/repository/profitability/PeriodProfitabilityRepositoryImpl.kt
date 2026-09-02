package com.sucharu.sucharupro.data.repository.profitability

import com.sucharu.sucharupro.data.datasource.profitability.PeriodProfitabilityDataSource
import com.sucharu.sucharupro.domain.model.profitability.*

/**
 * Repository implementation for Period Profitability, delegating to PeriodProfitabilityDataSource.
 * Module 16 Step 06.
 */
class PeriodProfitabilityRepositoryImpl(
    private val dataSource: PeriodProfitabilityDataSource
) : PeriodProfitabilityRepository {

    override suspend fun saveSnapshot(snapshot: PeriodProfitabilitySnapshot): PeriodProfitabilitySnapshot {
        return dataSource.saveSnapshot(snapshot)
    }

    override suspend fun findSnapshotById(tenantId: String, snapshotId: String): PeriodProfitabilitySnapshot? {
        return dataSource.findSnapshotById(tenantId, snapshotId)
    }

    override suspend fun findLatestSnapshotByPeriodId(tenantId: String, periodId: String): PeriodProfitabilitySnapshot? {
        return dataSource.findLatestSnapshotByPeriodId(tenantId, periodId)
    }

    override suspend fun listSnapshots(tenantId: String, filter: PeriodProfitabilityFilter): List<PeriodProfitabilitySnapshot> {
        return dataSource.listSnapshots(tenantId, filter)
    }

    override suspend fun saveProvenanceRecords(records: List<PeriodProfitabilityProvenanceRecord>) {
        dataSource.saveProvenanceRecords(records)
    }

    override suspend fun listProvenanceByPeriodId(tenantId: String, periodId: String): List<PeriodProfitabilityProvenanceRecord> {
        return dataSource.listProvenanceByPeriodId(tenantId, periodId)
    }

    override suspend fun saveReconciliationEvent(event: PeriodProfitabilityReconciliationEvent): PeriodProfitabilityReconciliationEvent {
        return dataSource.saveReconciliationEvent(event)
    }

    override suspend fun listReconciliationEventsByPeriodId(tenantId: String, periodId: String): List<PeriodProfitabilityReconciliationEvent> {
        return dataSource.listReconciliationEventsByPeriodId(tenantId, periodId)
    }

    override suspend fun saveAuditEvent(event: PeriodProfitabilityAuditEvent): PeriodProfitabilityAuditEvent {
        return dataSource.saveAuditEvent(event)
    }

    override suspend fun listAuditEventsByPeriodId(tenantId: String, periodId: String): List<PeriodProfitabilityAuditEvent> {
        return dataSource.listAuditEventsByPeriodId(tenantId, periodId)
    }

    override suspend fun saveUnattributedItems(items: List<PeriodUnattributedItem>) {
        dataSource.saveUnattributedItems(items)
    }

    override suspend fun listUnattributedItems(tenantId: String, periodId: String?): List<PeriodUnattributedItem> {
        return dataSource.listUnattributedItems(tenantId, periodId)
    }

    override suspend fun getIdempotentSnapshotId(tenantId: String, idempotencyKey: String): String? {
        return dataSource.getIdempotentSnapshotId(tenantId, idempotencyKey)
    }

    override suspend fun saveIdempotencyRecord(tenantId: String, idempotencyKey: String, snapshotId: String) {
        dataSource.saveIdempotencyRecord(tenantId, idempotencyKey, snapshotId)
    }
}
