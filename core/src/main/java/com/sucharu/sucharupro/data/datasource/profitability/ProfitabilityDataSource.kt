package com.sucharu.sucharupro.data.datasource.profitability

import com.sucharu.sucharupro.domain.model.profitability.*

/**
 * Persistence DataSource contract for Module 16 Profitability Snapshots, Reconciliation, and Audits.
 */
interface ProfitabilityDataSource {

    suspend fun saveSnapshot(snapshot: ProfitabilitySnapshot): ProfitabilitySnapshot

    suspend fun findSnapshotById(tenantId: String, projectId: String, id: String): ProfitabilitySnapshot?

    suspend fun listSnapshots(
        tenantId: String,
        projectId: String,
        scope: ProfitabilityScope? = null,
        targetEntityId: String? = null,
        periodId: String? = null,
        limit: Int = 100,
        offset: Int = 0
    ): List<ProfitabilitySnapshot>

    suspend fun saveReconciliationEvent(event: ProfitabilityReconciliationEvent): ProfitabilityReconciliationEvent

    suspend fun listReconciliationEvents(
        tenantId: String,
        projectId: String,
        snapshotId: String? = null,
        limit: Int = 100,
        offset: Int = 0
    ): List<ProfitabilityReconciliationEvent>

    suspend fun recordAuditEvent(event: ProfitabilityAuditEvent): ProfitabilityAuditEvent

    suspend fun listAuditEvents(
        tenantId: String,
        projectId: String,
        snapshotId: String? = null,
        limit: Int = 100,
        offset: Int = 0
    ): List<ProfitabilityAuditEvent>
}
