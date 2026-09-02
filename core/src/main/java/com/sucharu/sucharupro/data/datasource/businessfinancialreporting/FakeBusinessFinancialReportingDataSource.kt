package com.sucharu.sucharupro.data.datasource.businessfinancialreporting

import com.sucharu.sucharupro.domain.model.businessfinancialreporting.BusinessFinancialReportAuditEvent
import com.sucharu.sucharupro.domain.model.businessfinancialreporting.BusinessFinancialReportSnapshot
import com.sucharu.sucharupro.domain.model.businessfinancialreporting.BusinessFinancialReportType
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

/**
 * In-memory thread-safe fake data source for report snapshots and audit logging.
 */
class FakeBusinessFinancialReportingDataSource : BusinessFinancialReportingDataSource {

    private val snapshots = ConcurrentHashMap<String, BusinessFinancialReportSnapshot>()
    private val auditEvents = CopyOnWriteArrayList<BusinessFinancialReportAuditEvent>()

    override suspend fun saveSnapshot(snapshot: BusinessFinancialReportSnapshot): BusinessFinancialReportSnapshot {
        val compositeKey = "${snapshot.tenantId}#${snapshot.snapshotId}"
        snapshots[compositeKey] = snapshot
        return snapshot
    }

    override suspend fun findSnapshotById(tenantId: String, snapshotId: String): BusinessFinancialReportSnapshot? {
        val compositeKey = "$tenantId#$snapshotId"
        return snapshots[compositeKey]
    }

    override suspend fun listSnapshots(
        tenantId: String,
        projectId: String,
        reportType: BusinessFinancialReportType?,
        periodId: String?,
        limit: Int
    ): List<BusinessFinancialReportSnapshot> {
        return snapshots.values
            .filter { it.tenantId == tenantId && it.projectId == projectId }
            .filter { reportType == null || it.reportType == reportType }
            .filter { periodId == null || it.periodId == periodId }
            .sortedByDescending { it.generatedAt }
            .take(limit)
    }

    override suspend fun recordAuditEvent(event: BusinessFinancialReportAuditEvent): BusinessFinancialReportAuditEvent {
        auditEvents.add(event)
        return event
    }

    override suspend fun listAuditEvents(
        tenantId: String,
        projectId: String,
        reportType: BusinessFinancialReportType?,
        limit: Int
    ): List<BusinessFinancialReportAuditEvent> {
        return auditEvents
            .filter { it.tenantId == tenantId && it.projectId == projectId }
            .filter { reportType == null || it.reportType == reportType }
            .sortedByDescending { it.generatedAt }
            .take(limit)
    }

    fun clear() {
        snapshots.clear()
        auditEvents.clear()
    }
}
