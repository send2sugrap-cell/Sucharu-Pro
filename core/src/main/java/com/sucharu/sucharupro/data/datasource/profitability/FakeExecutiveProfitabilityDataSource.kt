package com.sucharu.sucharupro.data.datasource.profitability

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.profitability.*
import java.util.concurrent.ConcurrentHashMap

class FakeExecutiveProfitabilityDataSource : ExecutiveProfitabilityDataSource {

    private val snapshots = ConcurrentHashMap<String, ExecutiveProfitabilitySnapshot>()
    private val provenanceRecords = ConcurrentHashMap<String, MutableList<ExecutiveProvenanceRecord>>()
    private val reconciliations = ConcurrentHashMap<String, ExecutiveReconciliationResult>()

    override suspend fun saveSnapshot(snapshot: ExecutiveProfitabilitySnapshot): DomainResult<ExecutiveProfitabilitySnapshot> {
        val key = "${snapshot.tenantId}:${snapshot.snapshotId}"
        snapshots[key] = snapshot
        return DomainResult.Success(snapshot)
    }

    override suspend fun findSnapshotById(tenantId: String, snapshotId: String): DomainResult<ExecutiveProfitabilitySnapshot> {
        val key = "$tenantId:$snapshotId"
        val found = snapshots[key]
        return if (found != null) {
            DomainResult.Success(found)
        } else {
            DomainResult.Error(message = "Executive Profitability Snapshot not found: $snapshotId")
        }
    }

    override suspend fun findLatestSnapshot(tenantId: String, projectId: String, periodId: String?): DomainResult<ExecutiveProfitabilitySnapshot?> {
        val found = snapshots.values
            .filter { it.tenantId == tenantId && it.projectId == projectId && (periodId == null || it.periodId == periodId) }
            .maxByOrNull { it.generatedAt }
        return DomainResult.Success(found)
    }

    override suspend fun findSnapshotByFingerprint(tenantId: String, fingerprint: String): DomainResult<ExecutiveProfitabilitySnapshot?> {
        val found = snapshots.values.find { it.tenantId == tenantId && it.sourceFingerprint == fingerprint }
        return DomainResult.Success(found)
    }

    override suspend fun listSnapshots(tenantId: String, projectId: String, limit: Int): DomainResult<List<ExecutiveProfitabilitySnapshot>> {
        val list = snapshots.values
            .filter { it.tenantId == tenantId && it.projectId == projectId }
            .sortedByDescending { it.generatedAt }
            .take(limit)
        return DomainResult.Success(list)
    }

    override suspend fun saveProvenance(provenance: ExecutiveProvenanceRecord): DomainResult<ExecutiveProvenanceRecord> {
        val list = provenanceRecords.computeIfAbsent("${provenance.tenantId}:${provenance.snapshotId}") { mutableListOf() }
        list.add(provenance)
        return DomainResult.Success(provenance)
    }

    override suspend fun listProvenance(tenantId: String, snapshotId: String): DomainResult<List<ExecutiveProvenanceRecord>> {
        val list = provenanceRecords["$tenantId:$snapshotId"] ?: emptyList()
        return DomainResult.Success(list)
    }

    override suspend fun saveReconciliation(result: ExecutiveReconciliationResult): DomainResult<ExecutiveReconciliationResult> {
        reconciliations["${result.tenantId}:${result.snapshotId}"] = result
        return DomainResult.Success(result)
    }

    override suspend fun findLatestReconciliation(tenantId: String, projectId: String, periodId: String?): DomainResult<ExecutiveReconciliationResult?> {
        val found = reconciliations.values
            .filter { it.tenantId == tenantId && it.projectId == projectId && (periodId == null || it.periodId == periodId) }
            .maxByOrNull { it.checkedAt }
        return DomainResult.Success(found)
    }
}
