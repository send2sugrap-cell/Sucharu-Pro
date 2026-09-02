package com.sucharu.sucharupro.data.datasource.businessreconciliation

import com.sucharu.sucharupro.domain.model.businessreconciliation.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

class FakeBusinessFinancialReconciliationDataSource : BusinessFinancialReconciliationDataSource {

    private val runs = ConcurrentHashMap<String, BusinessFinancialReconciliationRun>()
    private val discrepancies = ConcurrentHashMap<String, BusinessFinancialReconciliationDiscrepancy>()
    private val snapshots = ConcurrentHashMap<String, BusinessFinancialReconciliationSnapshot>()
    private val auditEvents = CopyOnWriteArrayList<BusinessFinancialReconciliationAuditEvent>()

    // Runs
    override suspend fun createRun(run: BusinessFinancialReconciliationRun): BusinessFinancialReconciliationRun {
        val existingWithSameNumber = runs.values.find {
            it.tenantId == run.tenantId && it.projectId == run.projectId && it.runNumber == run.runNumber
        }
        if (existingWithSameNumber != null) {
            throw IllegalStateException("Reconciliation run with number '${run.runNumber}' already exists.")
        }
        runs[run.id] = run
        return run
    }

    override suspend fun updateRun(run: BusinessFinancialReconciliationRun): BusinessFinancialReconciliationRun {
        runs[run.id] = run
        return run
    }

    override suspend fun findRunById(id: String, tenantId: String, projectId: String): BusinessFinancialReconciliationRun? {
        val r = runs[id] ?: return null
        return if (r.tenantId == tenantId && r.projectId == projectId) r else null
    }

    override suspend fun findRunByNumber(runNumber: String, tenantId: String, projectId: String): BusinessFinancialReconciliationRun? {
        return runs.values.find { it.tenantId == tenantId && it.projectId == projectId && it.runNumber == runNumber }
    }

    override suspend fun listRuns(tenantId: String, projectId: String, filter: ReconciliationRunFilter): List<BusinessFinancialReconciliationRun> {
        return runs.values
            .filter { it.tenantId == tenantId && it.projectId == projectId }
            .filter { filter.periodId == null || it.periodId == filter.periodId }
            .filter { filter.runType == null || it.runType == filter.runType }
            .filter { filter.status == null || it.status == filter.status }
            .sortedByDescending { it.createdAt }
            .drop(filter.offset)
            .take(filter.limit)
    }

    override suspend fun countRuns(tenantId: String, projectId: String, filter: ReconciliationRunFilter): Long {
        return runs.values
            .filter { it.tenantId == tenantId && it.projectId == projectId }
            .filter { filter.periodId == null || it.periodId == filter.periodId }
            .filter { filter.runType == null || it.runType == filter.runType }
            .filter { filter.status == null || it.status == filter.status }
            .size.toLong()
    }

    // Discrepancies
    override suspend fun createDiscrepancy(discrepancy: BusinessFinancialReconciliationDiscrepancy): BusinessFinancialReconciliationDiscrepancy {
        discrepancies[discrepancy.id] = discrepancy
        return discrepancy
    }

    override suspend fun createDiscrepanciesBatch(discrepanciesList: List<BusinessFinancialReconciliationDiscrepancy>): List<BusinessFinancialReconciliationDiscrepancy> {
        discrepanciesList.forEach { discrepancies[it.id] = it }
        return discrepanciesList
    }

    override suspend fun updateDiscrepancy(discrepancy: BusinessFinancialReconciliationDiscrepancy): BusinessFinancialReconciliationDiscrepancy {
        discrepancies[discrepancy.id] = discrepancy
        return discrepancy
    }

    override suspend fun findDiscrepancyById(id: String, tenantId: String, projectId: String): BusinessFinancialReconciliationDiscrepancy? {
        val d = discrepancies[id] ?: return null
        return if (d.tenantId == tenantId && d.projectId == projectId) d else null
    }

    override suspend fun listDiscrepancies(tenantId: String, projectId: String, filter: DiscrepancyFilter): List<BusinessFinancialReconciliationDiscrepancy> {
        return discrepancies.values
            .filter { it.tenantId == tenantId && it.projectId == projectId }
            .filter { filter.reconciliationRunId == null || it.reconciliationRunId == filter.reconciliationRunId }
            .filter { filter.periodId == null || it.periodId == filter.periodId }
            .filter { filter.severity == null || it.severity == filter.severity }
            .filter { filter.status == null || it.status == filter.status }
            .filter { filter.discrepancyType == null || it.discrepancyType == filter.discrepancyType }
            .filter { filter.assignedTo == null || it.assignedTo == filter.assignedTo }
            .sortedByDescending { it.createdAt }
            .drop(filter.offset)
            .take(filter.limit)
    }

    override suspend fun countDiscrepancies(tenantId: String, projectId: String, filter: DiscrepancyFilter): Long {
        return discrepancies.values
            .filter { it.tenantId == tenantId && it.projectId == projectId }
            .filter { filter.reconciliationRunId == null || it.reconciliationRunId == filter.reconciliationRunId }
            .filter { filter.periodId == null || it.periodId == filter.periodId }
            .filter { filter.severity == null || it.severity == filter.severity }
            .filter { filter.status == null || it.status == filter.status }
            .filter { filter.discrepancyType == null || it.discrepancyType == filter.discrepancyType }
            .filter { filter.assignedTo == null || it.assignedTo == filter.assignedTo }
            .size.toLong()
    }

    // Snapshots
    override suspend fun saveSnapshot(snapshot: BusinessFinancialReconciliationSnapshot): BusinessFinancialReconciliationSnapshot {
        snapshots[snapshot.id] = snapshot
        return snapshot
    }

    override suspend fun findSnapshotByRunId(runId: String, tenantId: String, projectId: String): BusinessFinancialReconciliationSnapshot? {
        return snapshots.values.find { it.reconciliationRunId == runId && it.tenantId == tenantId && it.projectId == projectId }
    }

    // Audit Events
    override suspend fun recordAuditEvent(event: BusinessFinancialReconciliationAuditEvent): BusinessFinancialReconciliationAuditEvent {
        auditEvents.add(event)
        return event
    }

    override suspend fun listAuditEvents(tenantId: String, projectId: String, runId: String?, discrepancyId: String?): List<BusinessFinancialReconciliationAuditEvent> {
        return auditEvents
            .filter { it.tenantId == tenantId && it.projectId == projectId }
            .filter { runId == null || it.reconciliationRunId == runId }
            .filter { discrepancyId == null || it.discrepancyId == discrepancyId }
            .sortedByDescending { it.timestamp }
    }

    fun clear() {
        runs.clear()
        discrepancies.clear()
        snapshots.clear()
        auditEvents.clear()
    }
}
