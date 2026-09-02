package com.sucharu.sucharupro.domain.repository.businessfinancialreporting

import com.sucharu.sucharupro.domain.model.businessfinancialreporting.BusinessFinancialReportAuditEvent
import com.sucharu.sucharupro.domain.model.businessfinancialreporting.BusinessFinancialReportSnapshot
import com.sucharu.sucharupro.domain.model.businessfinancialreporting.BusinessFinancialReportType

/**
 * Domain Repository interface for Business Financial Reporting snapshots and audit records.
 * Note: Reports themselves are read-only aggregations computed over canonical financial sources.
 */
interface BusinessFinancialReportingRepository {
    suspend fun saveSnapshot(snapshot: BusinessFinancialReportSnapshot): BusinessFinancialReportSnapshot
    suspend fun findSnapshotById(tenantId: String, snapshotId: String): BusinessFinancialReportSnapshot?
    suspend fun listSnapshots(
        tenantId: String,
        projectId: String,
        reportType: BusinessFinancialReportType? = null,
        periodId: String? = null,
        limit: Int = 50
    ): List<BusinessFinancialReportSnapshot>

    suspend fun recordAuditEvent(event: BusinessFinancialReportAuditEvent): BusinessFinancialReportAuditEvent
    suspend fun listAuditEvents(
        tenantId: String,
        projectId: String,
        reportType: BusinessFinancialReportType? = null,
        limit: Int = 50
    ): List<BusinessFinancialReportAuditEvent>
}
