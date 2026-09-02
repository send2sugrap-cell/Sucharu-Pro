package com.sucharu.sucharupro.data.repository.profitability

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.profitability.*

/**
 * Repository interface for Module 16 Profitability domain.
 */
interface ProfitabilityRepository {

    suspend fun saveSnapshot(snapshot: ProfitabilitySnapshot): DomainResult<ProfitabilitySnapshot>

    suspend fun getSnapshotById(tenantId: String, projectId: String, id: String): DomainResult<ProfitabilitySnapshot>

    suspend fun listSnapshots(
        tenantId: String,
        projectId: String,
        scope: ProfitabilityScope? = null,
        targetEntityId: String? = null,
        periodId: String? = null,
        limit: Int = 100,
        offset: Int = 0
    ): DomainResult<List<ProfitabilitySnapshot>>

    suspend fun recordReconciliationEvent(event: ProfitabilityReconciliationEvent): DomainResult<ProfitabilityReconciliationEvent>

    suspend fun listReconciliationEvents(
        tenantId: String,
        projectId: String,
        snapshotId: String? = null,
        limit: Int = 100,
        offset: Int = 0
    ): DomainResult<List<ProfitabilityReconciliationEvent>>

    suspend fun recordAuditEvent(event: ProfitabilityAuditEvent): DomainResult<ProfitabilityAuditEvent>

    suspend fun listAuditEvents(
        tenantId: String,
        projectId: String,
        snapshotId: String? = null,
        limit: Int = 100,
        offset: Int = 0
    ): DomainResult<List<ProfitabilityAuditEvent>>
}
