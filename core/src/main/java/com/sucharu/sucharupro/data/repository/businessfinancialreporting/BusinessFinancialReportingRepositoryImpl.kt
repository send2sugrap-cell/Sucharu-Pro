package com.sucharu.sucharupro.data.repository.businessfinancialreporting

import com.sucharu.sucharupro.data.datasource.businessfinancialreporting.BusinessFinancialReportingDataSource
import com.sucharu.sucharupro.domain.model.businessfinancialreporting.BusinessFinancialReportAuditEvent
import com.sucharu.sucharupro.domain.model.businessfinancialreporting.BusinessFinancialReportSnapshot
import com.sucharu.sucharupro.domain.model.businessfinancialreporting.BusinessFinancialReportType
import com.sucharu.sucharupro.domain.repository.businessfinancialreporting.BusinessFinancialReportingRepository

/**
 * Repository implementation for Business Financial Report snapshots and audit events.
 */
class BusinessFinancialReportingRepositoryImpl(
    private val dataSource: BusinessFinancialReportingDataSource
) : BusinessFinancialReportingRepository {

    override suspend fun saveSnapshot(snapshot: BusinessFinancialReportSnapshot): BusinessFinancialReportSnapshot {
        return dataSource.saveSnapshot(snapshot)
    }

    override suspend fun findSnapshotById(tenantId: String, snapshotId: String): BusinessFinancialReportSnapshot? {
        return dataSource.findSnapshotById(tenantId, snapshotId)
    }

    override suspend fun listSnapshots(
        tenantId: String,
        projectId: String,
        reportType: BusinessFinancialReportType?,
        periodId: String?,
        limit: Int
    ): List<BusinessFinancialReportSnapshot> {
        return dataSource.listSnapshots(tenantId, projectId, reportType, periodId, limit)
    }

    override suspend fun recordAuditEvent(event: BusinessFinancialReportAuditEvent): BusinessFinancialReportAuditEvent {
        return dataSource.recordAuditEvent(event)
    }

    override suspend fun listAuditEvents(
        tenantId: String,
        projectId: String,
        reportType: BusinessFinancialReportType?,
        limit: Int
    ): List<BusinessFinancialReportAuditEvent> {
        return dataSource.listAuditEvents(tenantId, projectId, reportType, limit)
    }
}
