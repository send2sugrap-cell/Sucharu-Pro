package com.sucharu.sucharupro.data.repository.profitability

import com.sucharu.sucharupro.data.datasource.profitability.ExecutiveProfitabilityDataSource
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.profitability.*

interface ExecutiveProfitabilityRepository {
    suspend fun saveSnapshot(snapshot: ExecutiveProfitabilitySnapshot): DomainResult<ExecutiveProfitabilitySnapshot>
    suspend fun findSnapshotById(tenantId: String, snapshotId: String): DomainResult<ExecutiveProfitabilitySnapshot>
    suspend fun findLatestSnapshot(tenantId: String, projectId: String, periodId: String?): DomainResult<ExecutiveProfitabilitySnapshot?>
    suspend fun findSnapshotByFingerprint(tenantId: String, fingerprint: String): DomainResult<ExecutiveProfitabilitySnapshot?>
    suspend fun listSnapshots(tenantId: String, projectId: String, limit: Int = 20): DomainResult<List<ExecutiveProfitabilitySnapshot>>
    suspend fun saveProvenance(provenance: ExecutiveProvenanceRecord): DomainResult<ExecutiveProvenanceRecord>
    suspend fun listProvenance(tenantId: String, snapshotId: String): DomainResult<List<ExecutiveProvenanceRecord>>
    suspend fun saveReconciliation(result: ExecutiveReconciliationResult): DomainResult<ExecutiveReconciliationResult>
    suspend fun findLatestReconciliation(tenantId: String, projectId: String, periodId: String?): DomainResult<ExecutiveReconciliationResult?>
}

class ExecutiveProfitabilityRepositoryImpl(
    private val dataSource: ExecutiveProfitabilityDataSource
) : ExecutiveProfitabilityRepository {

    override suspend fun saveSnapshot(snapshot: ExecutiveProfitabilitySnapshot): DomainResult<ExecutiveProfitabilitySnapshot> {
        return dataSource.saveSnapshot(snapshot)
    }

    override suspend fun findSnapshotById(tenantId: String, snapshotId: String): DomainResult<ExecutiveProfitabilitySnapshot> {
        return dataSource.findSnapshotById(tenantId, snapshotId)
    }

    override suspend fun findLatestSnapshot(tenantId: String, projectId: String, periodId: String?): DomainResult<ExecutiveProfitabilitySnapshot?> {
        return dataSource.findLatestSnapshot(tenantId, projectId, periodId)
    }

    override suspend fun findSnapshotByFingerprint(tenantId: String, fingerprint: String): DomainResult<ExecutiveProfitabilitySnapshot?> {
        return dataSource.findSnapshotByFingerprint(tenantId, fingerprint)
    }

    override suspend fun listSnapshots(tenantId: String, projectId: String, limit: Int): DomainResult<List<ExecutiveProfitabilitySnapshot>> {
        return dataSource.listSnapshots(tenantId, projectId, limit)
    }

    override suspend fun saveProvenance(provenance: ExecutiveProvenanceRecord): DomainResult<ExecutiveProvenanceRecord> {
        return dataSource.saveProvenance(provenance)
    }

    override suspend fun listProvenance(tenantId: String, snapshotId: String): DomainResult<List<ExecutiveProvenanceRecord>> {
        return dataSource.listProvenance(tenantId, snapshotId)
    }

    override suspend fun saveReconciliation(result: ExecutiveReconciliationResult): DomainResult<ExecutiveReconciliationResult> {
        return dataSource.saveReconciliation(result)
    }

    override suspend fun findLatestReconciliation(tenantId: String, projectId: String, periodId: String?): DomainResult<ExecutiveReconciliationResult?> {
        return dataSource.findLatestReconciliation(tenantId, projectId, periodId)
    }
}
