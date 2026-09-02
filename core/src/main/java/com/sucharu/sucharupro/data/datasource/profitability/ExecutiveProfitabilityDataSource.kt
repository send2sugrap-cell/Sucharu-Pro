package com.sucharu.sucharupro.data.datasource.profitability

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.profitability.*

interface ExecutiveProfitabilityDataSource {
    suspend fun saveSnapshot(snapshot: ExecutiveProfitabilitySnapshot): DomainResult<ExecutiveProfitabilitySnapshot>
    suspend fun findSnapshotById(tenantId: String, snapshotId: String): DomainResult<ExecutiveProfitabilitySnapshot>
    suspend fun findLatestSnapshot(tenantId: String, projectId: String, periodId: String?): DomainResult<ExecutiveProfitabilitySnapshot?>
    suspend fun findSnapshotByFingerprint(tenantId: String, fingerprint: String): DomainResult<ExecutiveProfitabilitySnapshot?>
    suspend fun listSnapshots(tenantId: String, projectId: String, limit: Int): DomainResult<List<ExecutiveProfitabilitySnapshot>>
    suspend fun saveProvenance(provenance: ExecutiveProvenanceRecord): DomainResult<ExecutiveProvenanceRecord>
    suspend fun listProvenance(tenantId: String, snapshotId: String): DomainResult<List<ExecutiveProvenanceRecord>>
    suspend fun saveReconciliation(result: ExecutiveReconciliationResult): DomainResult<ExecutiveReconciliationResult>
    suspend fun findLatestReconciliation(tenantId: String, projectId: String, periodId: String?): DomainResult<ExecutiveReconciliationResult?>
}
