package com.sucharu.sucharupro.data.datasource.profitability

import com.sucharu.sucharupro.domain.model.profitability.*
import java.util.concurrent.ConcurrentHashMap

/**
 * In-memory thread-safe Fake DataSource for Profitability testing and lightweight dev runtime.
 */
class FakeProfitabilityDataSource : ProfitabilityDataSource {

    private val snapshots = ConcurrentHashMap<String, ProfitabilitySnapshot>()
    private val reconciliationEvents = ConcurrentHashMap<String, ProfitabilityReconciliationEvent>()
    private val auditEvents = ConcurrentHashMap<String, ProfitabilityAuditEvent>()

    override suspend fun saveSnapshot(snapshot: ProfitabilitySnapshot): ProfitabilitySnapshot {
        snapshots[snapshot.id] = snapshot
        return snapshot
    }

    override suspend fun findSnapshotById(tenantId: String, projectId: String, id: String): ProfitabilitySnapshot? {
        val s = snapshots[id] ?: return null
        if (s.tenantId != tenantId || s.projectId != projectId) return null
        return s
    }

    override suspend fun listSnapshots(
        tenantId: String,
        projectId: String,
        scope: ProfitabilityScope?,
        targetEntityId: String?,
        periodId: String?,
        limit: Int,
        offset: Int
    ): List<ProfitabilitySnapshot> {
        return snapshots.values
            .filter { it.tenantId == tenantId && it.projectId == projectId }
            .filter { scope == null || it.scope == scope }
            .filter { targetEntityId == null || it.targetEntityId == targetEntityId }
            .filter { periodId == null || it.periodId == periodId }
            .sortedByDescending { it.generatedAt }
            .drop(offset)
            .take(limit)
    }

    override suspend fun saveReconciliationEvent(event: ProfitabilityReconciliationEvent): ProfitabilityReconciliationEvent {
        reconciliationEvents[event.id] = event
        return event
    }

    override suspend fun listReconciliationEvents(
        tenantId: String,
        projectId: String,
        snapshotId: String?,
        limit: Int,
        offset: Int
    ): List<ProfitabilityReconciliationEvent> {
        return reconciliationEvents.values
            .filter { it.tenantId == tenantId && it.projectId == projectId }
            .filter { snapshotId == null || it.snapshotId == snapshotId }
            .sortedByDescending { it.checkedAt }
            .drop(offset)
            .take(limit)
    }

    override suspend fun recordAuditEvent(event: ProfitabilityAuditEvent): ProfitabilityAuditEvent {
        auditEvents[event.id] = event
        return event
    }

    override suspend fun listAuditEvents(
        tenantId: String,
        projectId: String,
        snapshotId: String?,
        limit: Int,
        offset: Int
    ): List<ProfitabilityAuditEvent> {
        return auditEvents.values
            .filter { it.tenantId == tenantId && it.projectId == projectId }
            .filter { snapshotId == null || it.snapshotId == snapshotId }
            .sortedByDescending { it.timestamp }
            .drop(offset)
            .take(limit)
    }

    fun clear() {
        snapshots.clear()
        reconciliationEvents.clear()
        auditEvents.clear()
    }
}
