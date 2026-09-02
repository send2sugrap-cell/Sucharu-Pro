package com.sucharu.sucharupro.data.datasource.profitability

import com.sucharu.sucharupro.domain.model.profitability.*
import java.util.concurrent.ConcurrentHashMap

/**
 * Thread-safe in-memory Fake DataSource for Period Profitability.
 * Module 16 Step 06.
 */
class FakePeriodProfitabilityDataSource : PeriodProfitabilityDataSource {

    private val snapshots = ConcurrentHashMap<String, PeriodProfitabilitySnapshot>()
    private val provenanceRecords = ConcurrentHashMap<String, MutableList<PeriodProfitabilityProvenanceRecord>>()
    private val reconciliationEvents = ConcurrentHashMap<String, MutableList<PeriodProfitabilityReconciliationEvent>>()
    private val auditEvents = ConcurrentHashMap<String, MutableList<PeriodProfitabilityAuditEvent>>()
    private val idempotencyRecords = ConcurrentHashMap<String, String>() // "tenantId|idempotencyKey" -> snapshotId
    private val unattributedRecords = ConcurrentHashMap<String, MutableList<PeriodUnattributedItem>>()

    override suspend fun saveSnapshot(snapshot: PeriodProfitabilitySnapshot): PeriodProfitabilitySnapshot {
        snapshots[snapshot.snapshotId] = snapshot
        return snapshot
    }

    override suspend fun findSnapshotById(tenantId: String, snapshotId: String): PeriodProfitabilitySnapshot? {
        val snap = snapshots[snapshotId]
        return if (snap?.tenantId == tenantId) snap else null
    }

    override suspend fun findLatestSnapshotByPeriodId(tenantId: String, periodId: String): PeriodProfitabilitySnapshot? {
        return snapshots.values
            .filter { it.tenantId == tenantId && it.periodId == periodId }
            .maxByOrNull { it.generatedAt }
    }

    override suspend fun listSnapshots(tenantId: String, filter: PeriodProfitabilityFilter): List<PeriodProfitabilitySnapshot> {
        return snapshots.values
            .filter { it.tenantId == tenantId }
            .filter { filter.periodType == null || it.periodType == filter.periodType }
            .filter { filter.status == null || it.periodStatus == filter.status }
            .filter { filter.periodStartFrom == null || it.periodStart >= filter.periodStartFrom }
            .filter { filter.periodEndTo == null || it.periodEnd <= filter.periodEndTo }
            .sortedByDescending { it.generatedAt }
            .drop(filter.offset)
            .take(filter.limit)
    }

    override suspend fun saveProvenanceRecords(records: List<PeriodProfitabilityProvenanceRecord>) {
        for (r in records) {
            val key = "${r.tenantId}|${r.periodId}"
            provenanceRecords.computeIfAbsent(key) { mutableListOf() }.add(r)
        }
    }

    override suspend fun listProvenanceByPeriodId(tenantId: String, periodId: String): List<PeriodProfitabilityProvenanceRecord> {
        val key = "$tenantId|$periodId"
        return provenanceRecords[key]?.toList() ?: emptyList()
    }

    override suspend fun saveReconciliationEvent(event: PeriodProfitabilityReconciliationEvent): PeriodProfitabilityReconciliationEvent {
        val key = "${event.tenantId}|${event.periodId}"
        reconciliationEvents.computeIfAbsent(key) { mutableListOf() }.add(event)
        return event
    }

    override suspend fun listReconciliationEventsByPeriodId(tenantId: String, periodId: String): List<PeriodProfitabilityReconciliationEvent> {
        val key = "$tenantId|$periodId"
        return reconciliationEvents[key]?.toList() ?: emptyList()
    }

    override suspend fun saveAuditEvent(event: PeriodProfitabilityAuditEvent): PeriodProfitabilityAuditEvent {
        val key = "${event.tenantId}|${event.periodId}"
        auditEvents.computeIfAbsent(key) { mutableListOf() }.add(event)
        return event
    }

    override suspend fun listAuditEventsByPeriodId(tenantId: String, periodId: String): List<PeriodProfitabilityAuditEvent> {
        val key = "$tenantId|$periodId"
        return auditEvents[key]?.toList() ?: emptyList()
    }

    override suspend fun saveUnattributedItems(items: List<PeriodUnattributedItem>) {
        for (item in items) {
            val key = "${item.tenantId}|${item.periodId}"
            unattributedRecords.computeIfAbsent(key) { mutableListOf() }.add(item)
        }
    }

    override suspend fun listUnattributedItems(tenantId: String, periodId: String?): List<PeriodUnattributedItem> {
        return if (periodId != null) {
            val key = "$tenantId|$periodId"
            unattributedRecords[key]?.toList() ?: emptyList()
        } else {
            unattributedRecords.entries
                .filter { it.key.startsWith("$tenantId|") }
                .flatMap { it.value }
        }
    }

    override suspend fun getIdempotentSnapshotId(tenantId: String, idempotencyKey: String): String? {
        return idempotencyRecords["$tenantId|$idempotencyKey"]
    }

    override suspend fun saveIdempotencyRecord(tenantId: String, idempotencyKey: String, snapshotId: String) {
        idempotencyRecords["$tenantId|$idempotencyKey"] = snapshotId
    }

    fun clear() {
        snapshots.clear()
        provenanceRecords.clear()
        reconciliationEvents.clear()
        auditEvents.clear()
        idempotencyRecords.clear()
        unattributedRecords.clear()
    }
}
