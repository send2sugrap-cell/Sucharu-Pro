package com.sucharu.sucharupro.data.datasource.businessreconciliation

import com.sucharu.sucharupro.domain.model.businessreconciliation.*

data class ReconciliationRunFilter(
    val periodId: String? = null,
    val runType: ReconciliationRunType? = null,
    val status: ReconciliationRunStatus? = null,
    val limit: Int = 100,
    val offset: Int = 0
)

data class DiscrepancyFilter(
    val reconciliationRunId: String? = null,
    val periodId: String? = null,
    val severity: DiscrepancySeverity? = null,
    val status: DiscrepancyStatus? = null,
    val discrepancyType: FinancialDiscrepancyType? = null,
    val assignedTo: String? = null,
    val limit: Int = 100,
    val offset: Int = 0
)

interface BusinessFinancialReconciliationDataSource {
    // Runs
    suspend fun createRun(run: BusinessFinancialReconciliationRun): BusinessFinancialReconciliationRun
    suspend fun updateRun(run: BusinessFinancialReconciliationRun): BusinessFinancialReconciliationRun
    suspend fun findRunById(id: String, tenantId: String, projectId: String): BusinessFinancialReconciliationRun?
    suspend fun findRunByNumber(runNumber: String, tenantId: String, projectId: String): BusinessFinancialReconciliationRun?
    suspend fun listRuns(tenantId: String, projectId: String, filter: ReconciliationRunFilter): List<BusinessFinancialReconciliationRun>
    suspend fun countRuns(tenantId: String, projectId: String, filter: ReconciliationRunFilter): Long

    // Discrepancies
    suspend fun createDiscrepancy(discrepancy: BusinessFinancialReconciliationDiscrepancy): BusinessFinancialReconciliationDiscrepancy
    suspend fun createDiscrepanciesBatch(discrepancies: List<BusinessFinancialReconciliationDiscrepancy>): List<BusinessFinancialReconciliationDiscrepancy>
    suspend fun updateDiscrepancy(discrepancy: BusinessFinancialReconciliationDiscrepancy): BusinessFinancialReconciliationDiscrepancy
    suspend fun findDiscrepancyById(id: String, tenantId: String, projectId: String): BusinessFinancialReconciliationDiscrepancy?
    suspend fun listDiscrepancies(tenantId: String, projectId: String, filter: DiscrepancyFilter): List<BusinessFinancialReconciliationDiscrepancy>
    suspend fun countDiscrepancies(tenantId: String, projectId: String, filter: DiscrepancyFilter): Long

    // Snapshots
    suspend fun saveSnapshot(snapshot: BusinessFinancialReconciliationSnapshot): BusinessFinancialReconciliationSnapshot
    suspend fun findSnapshotByRunId(runId: String, tenantId: String, projectId: String): BusinessFinancialReconciliationSnapshot?

    // Audit Events
    suspend fun recordAuditEvent(event: BusinessFinancialReconciliationAuditEvent): BusinessFinancialReconciliationAuditEvent
    suspend fun listAuditEvents(tenantId: String, projectId: String, runId: String?, discrepancyId: String?): List<BusinessFinancialReconciliationAuditEvent>
}
