package com.sucharu.sucharupro.data.datasource.profitability

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.profitability.*
import java.util.concurrent.ConcurrentHashMap

/**
 * Thread-safe In-Memory Fake Data Source for Profitability Intelligence testing and local development.
 * Module 16 Step 07.
 */
class FakeProfitabilityIntelligenceDataSource : ProfitabilityIntelligenceDataSource {

    private val snapshots = ConcurrentHashMap<String, ProfitabilityIntelligenceSnapshot>()
    private val reconciliationEvents = ConcurrentHashMap<String, MutableList<ProfitabilityIntelligenceReconciliationEvent>>()
    private val auditEvents = ConcurrentHashMap<String, MutableList<ProfitabilityIntelligenceAuditEvent>>()
    private val idempotencyMap = ConcurrentHashMap<String, String>()

    override suspend fun saveSnapshot(snapshot: ProfitabilityIntelligenceSnapshot): DomainResult<ProfitabilityIntelligenceSnapshot> {
        val key = "${snapshot.tenantId}:${snapshot.snapshotId}"
        snapshots[key] = snapshot
        return DomainResult.Success(snapshot)
    }

    override suspend fun getLatestSnapshot(tenantId: String, periodId: String): DomainResult<ProfitabilityIntelligenceSnapshot?> {
        val snap = snapshots.values
            .filter { it.tenantId == tenantId && it.analysisPeriodId == periodId }
            .maxByOrNull { it.generatedAt }
        return DomainResult.Success(snap)
    }

    override suspend fun getSnapshotById(tenantId: String, snapshotId: String): DomainResult<ProfitabilityIntelligenceSnapshot?> {
        val snap = snapshots["$tenantId:$snapshotId"]
        return DomainResult.Success(snap)
    }

    override suspend fun listSnapshots(tenantId: String, filter: ProfitabilityIntelligenceFilter): DomainResult<List<ProfitabilityIntelligenceSnapshot>> {
        var list = snapshots.values.filter { it.tenantId == tenantId }
        if (filter.periodId != null) {
            list = list.filter { it.analysisPeriodId == filter.periodId }
        }
        if (filter.scope != null) {
            list = list.filter { it.scope == filter.scope }
        }
        val paged = list.sortedByDescending { it.generatedAt }
            .drop(filter.offset)
            .take(filter.limit)
        return DomainResult.Success(paged)
    }

    override suspend fun getDimensionInsights(tenantId: String, periodId: String, dimensionType: ProfitabilityDimensionType?): DomainResult<List<DimensionInsight>> {
        val snap = snapshots.values
            .filter { it.tenantId == tenantId && it.analysisPeriodId == periodId }
            .maxByOrNull { it.generatedAt }

        val dims = snap?.dimensionInsights ?: emptyList()
        val filtered = if (dimensionType != null) {
            dims.filter { it.dimensionType == dimensionType }
        } else {
            dims
        }
        return DomainResult.Success(filtered)
    }

    override suspend fun getRelationshipInsights(
        tenantId: String,
        periodId: String,
        fromDimension: ProfitabilityDimensionType?,
        toDimension: ProfitabilityDimensionType?
    ): DomainResult<List<ProfitabilityRelationshipInsight>> {
        val snap = snapshots.values
            .filter { it.tenantId == tenantId && it.analysisPeriodId == periodId }
            .maxByOrNull { it.generatedAt }

        var rels = snap?.relationshipInsights ?: emptyList()
        if (fromDimension != null) {
            rels = rels.filter { it.fromDimensionType == fromDimension }
        }
        if (toDimension != null) {
            rels = rels.filter { it.toDimensionType == toDimension }
        }
        return DomainResult.Success(rels)
    }

    override suspend fun getDrivers(tenantId: String, periodId: String, driverType: ProfitabilityDriverType?): DomainResult<List<ProfitabilityDriver>> {
        val snap = snapshots.values
            .filter { it.tenantId == tenantId && it.analysisPeriodId == periodId }
            .maxByOrNull { it.generatedAt }

        var drvs = snap?.drivers ?: emptyList()
        if (driverType != null) {
            drvs = drvs.filter { it.driverType == driverType }
        }
        return DomainResult.Success(drvs)
    }

    override suspend fun getLeakages(tenantId: String, periodId: String): DomainResult<List<ProfitLeakageItem>> {
        val snap = snapshots.values
            .filter { it.tenantId == tenantId && it.analysisPeriodId == periodId }
            .maxByOrNull { it.generatedAt }

        return DomainResult.Success(snap?.leakages ?: emptyList())
    }

    override suspend fun getPriorities(tenantId: String, periodId: String): DomainResult<List<ManagementPriorityItem>> {
        val snap = snapshots.values
            .filter { it.tenantId == tenantId && it.analysisPeriodId == periodId }
            .maxByOrNull { it.generatedAt }

        return DomainResult.Success(snap?.managementPriorities ?: emptyList())
    }

    override suspend fun getHealthScore(tenantId: String, periodId: String): DomainResult<ProfitabilityHealthScore?> {
        val snap = snapshots.values
            .filter { it.tenantId == tenantId && it.analysisPeriodId == periodId }
            .maxByOrNull { it.generatedAt }

        return DomainResult.Success(snap?.healthScore)
    }

    override suspend fun getProvenanceRecords(tenantId: String, periodId: String): DomainResult<List<ProfitabilityIntelligenceProvenance>> {
        val snap = snapshots.values
            .filter { it.tenantId == tenantId && it.analysisPeriodId == periodId }
            .maxByOrNull { it.generatedAt }

        return DomainResult.Success(snap?.provenanceRecords ?: emptyList())
    }

    override suspend fun saveReconciliationEvent(event: ProfitabilityIntelligenceReconciliationEvent): DomainResult<ProfitabilityIntelligenceReconciliationEvent> {
        val list = reconciliationEvents.computeIfAbsent("${event.tenantId}:${event.periodId}") { mutableListOf() }
        synchronized(list) {
            list.add(event)
        }
        return DomainResult.Success(event)
    }

    override suspend fun listReconciliationEvents(tenantId: String, periodId: String): DomainResult<List<ProfitabilityIntelligenceReconciliationEvent>> {
        val list = reconciliationEvents["$tenantId:$periodId"] ?: emptyList()
        return DomainResult.Success(list.toList())
    }

    override suspend fun recordAuditEvent(event: ProfitabilityIntelligenceAuditEvent): DomainResult<Unit> {
        val list = auditEvents.computeIfAbsent("${event.tenantId}:${event.periodId}") { mutableListOf() }
        synchronized(list) {
            list.add(event)
        }
        return DomainResult.Success(Unit)
    }

    override suspend fun listAuditEvents(tenantId: String, periodId: String): DomainResult<List<ProfitabilityIntelligenceAuditEvent>> {
        val list = auditEvents["$tenantId:$periodId"] ?: emptyList()
        return DomainResult.Success(list.toList())
    }

    override suspend fun checkIdempotency(tenantId: String, idempotencyKey: String): DomainResult<String?> {
        return DomainResult.Success(idempotencyMap["$tenantId:$idempotencyKey"])
    }

    override suspend fun saveIdempotencyRecord(tenantId: String, idempotencyKey: String, snapshotId: String): DomainResult<Unit> {
        idempotencyMap["$tenantId:$idempotencyKey"] = snapshotId
        return DomainResult.Success(Unit)
    }

    fun clear() {
        snapshots.clear()
        reconciliationEvents.clear()
        auditEvents.clear()
        idempotencyMap.clear()
    }
}
