package com.sucharu.sucharupro.data.repository.businessreconciliation

import com.sucharu.sucharupro.data.datasource.businessreconciliation.*
import com.sucharu.sucharupro.domain.model.businessreconciliation.*

class BusinessFinancialReconciliationRepositoryImpl(
    private val dataSource: BusinessFinancialReconciliationDataSource
) : BusinessFinancialReconciliationRepository {

    override suspend fun createRun(run: BusinessFinancialReconciliationRun): BusinessFinancialReconciliationRun {
        return dataSource.createRun(run)
    }

    override suspend fun updateRun(run: BusinessFinancialReconciliationRun): BusinessFinancialReconciliationRun {
        return dataSource.updateRun(run)
    }

    override suspend fun findRunById(id: String, tenantId: String, projectId: String): BusinessFinancialReconciliationRun? {
        return dataSource.findRunById(id, tenantId, projectId)
    }

    override suspend fun findRunByNumber(runNumber: String, tenantId: String, projectId: String): BusinessFinancialReconciliationRun? {
        return dataSource.findRunByNumber(runNumber, tenantId, projectId)
    }

    override suspend fun listRuns(tenantId: String, projectId: String, filter: ReconciliationRunFilter): List<BusinessFinancialReconciliationRun> {
        return dataSource.listRuns(tenantId, projectId, filter)
    }

    override suspend fun countRuns(tenantId: String, projectId: String, filter: ReconciliationRunFilter): Long {
        return dataSource.countRuns(tenantId, projectId, filter)
    }

    override suspend fun createDiscrepancy(discrepancy: BusinessFinancialReconciliationDiscrepancy): BusinessFinancialReconciliationDiscrepancy {
        return dataSource.createDiscrepancy(discrepancy)
    }

    override suspend fun createDiscrepanciesBatch(discrepancies: List<BusinessFinancialReconciliationDiscrepancy>): List<BusinessFinancialReconciliationDiscrepancy> {
        return dataSource.createDiscrepanciesBatch(discrepancies)
    }

    override suspend fun updateDiscrepancy(discrepancy: BusinessFinancialReconciliationDiscrepancy): BusinessFinancialReconciliationDiscrepancy {
        return dataSource.updateDiscrepancy(discrepancy)
    }

    override suspend fun findDiscrepancyById(id: String, tenantId: String, projectId: String): BusinessFinancialReconciliationDiscrepancy? {
        return dataSource.findDiscrepancyById(id, tenantId, projectId)
    }

    override suspend fun listDiscrepancies(tenantId: String, projectId: String, filter: DiscrepancyFilter): List<BusinessFinancialReconciliationDiscrepancy> {
        return dataSource.listDiscrepancies(tenantId, projectId, filter)
    }

    override suspend fun countDiscrepancies(tenantId: String, projectId: String, filter: DiscrepancyFilter): Long {
        return dataSource.countDiscrepancies(tenantId, projectId, filter)
    }

    override suspend fun saveSnapshot(snapshot: BusinessFinancialReconciliationSnapshot): BusinessFinancialReconciliationSnapshot {
        return dataSource.saveSnapshot(snapshot)
    }

    override suspend fun findSnapshotByRunId(runId: String, tenantId: String, projectId: String): BusinessFinancialReconciliationSnapshot? {
        return dataSource.findSnapshotByRunId(runId, tenantId, projectId)
    }

    override suspend fun recordAuditEvent(event: BusinessFinancialReconciliationAuditEvent): BusinessFinancialReconciliationAuditEvent {
        return dataSource.recordAuditEvent(event)
    }

    override suspend fun listAuditEvents(tenantId: String, projectId: String, runId: String?, discrepancyId: String?): List<BusinessFinancialReconciliationAuditEvent> {
        return dataSource.listAuditEvents(tenantId, projectId, runId, discrepancyId)
    }
}
