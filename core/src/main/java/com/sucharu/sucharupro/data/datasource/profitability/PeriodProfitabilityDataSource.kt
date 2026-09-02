package com.sucharu.sucharupro.data.datasource.profitability

import com.sucharu.sucharupro.domain.model.profitability.*

/**
 * Data Source contract for Period Profitability.
 * Module 16 Step 06.
 */
interface PeriodProfitabilityDataSource {
    suspend fun saveSnapshot(snapshot: PeriodProfitabilitySnapshot): PeriodProfitabilitySnapshot
    suspend fun findSnapshotById(tenantId: String, snapshotId: String): PeriodProfitabilitySnapshot?
    suspend fun findLatestSnapshotByPeriodId(tenantId: String, periodId: String): PeriodProfitabilitySnapshot?
    suspend fun listSnapshots(tenantId: String, filter: PeriodProfitabilityFilter): List<PeriodProfitabilitySnapshot>
    suspend fun saveProvenanceRecords(records: List<PeriodProfitabilityProvenanceRecord>)
    suspend fun listProvenanceByPeriodId(tenantId: String, periodId: String): List<PeriodProfitabilityProvenanceRecord>
    suspend fun saveReconciliationEvent(event: PeriodProfitabilityReconciliationEvent): PeriodProfitabilityReconciliationEvent
    suspend fun listReconciliationEventsByPeriodId(tenantId: String, periodId: String): List<PeriodProfitabilityReconciliationEvent>
    suspend fun saveAuditEvent(event: PeriodProfitabilityAuditEvent): PeriodProfitabilityAuditEvent
    suspend fun listAuditEventsByPeriodId(tenantId: String, periodId: String): List<PeriodProfitabilityAuditEvent>
    suspend fun saveUnattributedItems(items: List<PeriodUnattributedItem>)
    suspend fun listUnattributedItems(tenantId: String, periodId: String?): List<PeriodUnattributedItem>
    suspend fun getIdempotentSnapshotId(tenantId: String, idempotencyKey: String): String?
    suspend fun saveIdempotencyRecord(tenantId: String, idempotencyKey: String, snapshotId: String)
}
