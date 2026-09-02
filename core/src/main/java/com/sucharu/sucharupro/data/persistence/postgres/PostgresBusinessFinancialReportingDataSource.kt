package com.sucharu.sucharupro.data.persistence.postgres

import com.sucharu.sucharupro.data.datasource.businessfinancialreporting.BusinessFinancialReportingDataSource
import com.sucharu.sucharupro.domain.model.businessfinancialreporting.BusinessFinancialReportAuditEvent
import com.sucharu.sucharupro.domain.model.businessfinancialreporting.BusinessFinancialReportFormat
import com.sucharu.sucharupro.domain.model.businessfinancialreporting.BusinessFinancialReportSnapshot
import com.sucharu.sucharupro.domain.model.businessfinancialreporting.BusinessFinancialReportType
import java.sql.ResultSet

/**
 * Production PostgreSQL JDBC Data Source for Business Financial Report Snapshots and Audit Logs.
 */
class PostgresBusinessFinancialReportingDataSource(
    private val transactionManager: TransactionManager
) : BusinessFinancialReportingDataSource {

    override suspend fun saveSnapshot(snapshot: BusinessFinancialReportSnapshot): BusinessFinancialReportSnapshot {
        return transactionManager.inTransaction(TenantContext(snapshot.projectId)) { tx ->
            val sql = """
                INSERT INTO business_financial_report_snapshots (
                    snapshot_id, tenant_id, project_id, period_id, report_type,
                    filter_summary, metrics_payload_json, integrity_hash, is_immutable,
                    generated_by, generated_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (snapshot_id) DO NOTHING
            """.trimIndent()
            tx.connection.prepareStatement(sql).use { ps ->
                ps.setString(1, snapshot.snapshotId)
                ps.setString(2, snapshot.tenantId)
                ps.setString(3, snapshot.projectId)
                ps.setString(4, snapshot.periodId)
                ps.setString(5, snapshot.reportType.name)
                ps.setString(6, snapshot.filterSummary)
                ps.setString(7, snapshot.metricsPayloadJson)
                ps.setString(8, snapshot.integrityHash)
                ps.setBoolean(9, snapshot.isImmutable)
                ps.setString(10, snapshot.generatedBy)
                ps.setLong(11, snapshot.generatedAt)
                ps.executeUpdate()
            }
            snapshot
        }
    }

    override suspend fun findSnapshotById(tenantId: String, snapshotId: String): BusinessFinancialReportSnapshot? {
        return transactionManager.inTransaction(TenantContext(tenantId)) { tx ->
            val sql = """
                SELECT * FROM business_financial_report_snapshots
                WHERE tenant_id = ? AND snapshot_id = ?
            """.trimIndent()
            tx.connection.prepareStatement(sql).use { ps ->
                ps.setString(1, tenantId)
                ps.setString(2, snapshotId)
                ps.executeQuery().use { rs ->
                    if (rs.next()) mapSnapshot(rs) else null
                }
            }
        }
    }

    override suspend fun listSnapshots(
        tenantId: String,
        projectId: String,
        reportType: BusinessFinancialReportType?,
        periodId: String?,
        limit: Int
    ): List<BusinessFinancialReportSnapshot> {
        return transactionManager.inTransaction(TenantContext(projectId)) { tx ->
            val sql = StringBuilder(
                """
                SELECT * FROM business_financial_report_snapshots
                WHERE tenant_id = ? AND project_id = ?
                """.trimIndent()
            )
            val params = mutableListOf<Any>(tenantId, projectId)

            if (reportType != null) {
                sql.append(" AND report_type = ?")
                params.add(reportType.name)
            }
            if (periodId != null) {
                sql.append(" AND period_id = ?")
                params.add(periodId)
            }
            sql.append(" ORDER BY generated_at DESC LIMIT ?")
            params.add(limit)

            tx.connection.prepareStatement(sql.toString()).use { ps ->
                params.forEachIndexed { index, param ->
                    when (param) {
                        is String -> ps.setString(index + 1, param)
                        is Int -> ps.setInt(index + 1, param)
                    }
                }
                ps.executeQuery().use { rs ->
                    val list = mutableListOf<BusinessFinancialReportSnapshot>()
                    while (rs.next()) {
                        list.add(mapSnapshot(rs))
                    }
                    list
                }
            }
        }
    }

    override suspend fun recordAuditEvent(event: BusinessFinancialReportAuditEvent): BusinessFinancialReportAuditEvent {
        return transactionManager.inTransaction(TenantContext(event.projectId)) { tx ->
            val sql = """
                INSERT INTO business_financial_report_audit_events (
                    audit_id, tenant_id, project_id, report_type, format,
                    requested_by, generated_at, is_success, correlation_id, error_message
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """.trimIndent()
            tx.connection.prepareStatement(sql).use { ps ->
                ps.setString(1, event.auditId)
                ps.setString(2, event.tenantId)
                ps.setString(3, event.projectId)
                ps.setString(4, event.reportType.name)
                ps.setString(5, event.format.name)
                ps.setString(6, event.requestedBy)
                ps.setLong(7, event.generatedAt)
                ps.setBoolean(8, event.isSuccess)
                ps.setString(9, event.correlationId)
                ps.setString(10, event.errorMessage)
                ps.executeUpdate()
            }
            event
        }
    }

    override suspend fun listAuditEvents(
        tenantId: String,
        projectId: String,
        reportType: BusinessFinancialReportType?,
        limit: Int
    ): List<BusinessFinancialReportAuditEvent> {
        return transactionManager.inTransaction(TenantContext(projectId)) { tx ->
            val sql = StringBuilder(
                """
                SELECT * FROM business_financial_report_audit_events
                WHERE tenant_id = ? AND project_id = ?
                """.trimIndent()
            )
            val params = mutableListOf<Any>(tenantId, projectId)

            if (reportType != null) {
                sql.append(" AND report_type = ?")
                params.add(reportType.name)
            }
            sql.append(" ORDER BY generated_at DESC LIMIT ?")
            params.add(limit)

            tx.connection.prepareStatement(sql.toString()).use { ps ->
                params.forEachIndexed { index, param ->
                    when (param) {
                        is String -> ps.setString(index + 1, param)
                        is Int -> ps.setInt(index + 1, param)
                    }
                }
                ps.executeQuery().use { rs ->
                    val list = mutableListOf<BusinessFinancialReportAuditEvent>()
                    while (rs.next()) {
                        list.add(mapAuditEvent(rs))
                    }
                    list
                }
            }
        }
    }

    private fun mapSnapshot(rs: ResultSet): BusinessFinancialReportSnapshot {
        return BusinessFinancialReportSnapshot(
            snapshotId = rs.getString("snapshot_id"),
            tenantId = rs.getString("tenant_id"),
            projectId = rs.getString("project_id"),
            periodId = rs.getString("period_id"),
            reportType = BusinessFinancialReportType.valueOf(rs.getString("report_type")),
            filterSummary = rs.getString("filter_summary"),
            metricsPayloadJson = rs.getString("metrics_payload_json"),
            integrityHash = rs.getString("integrity_hash"),
            isImmutable = rs.getBoolean("is_immutable"),
            generatedBy = rs.getString("generated_by"),
            generatedAt = rs.getLong("generated_at")
        )
    }

    private fun mapAuditEvent(rs: ResultSet): BusinessFinancialReportAuditEvent {
        return BusinessFinancialReportAuditEvent(
            auditId = rs.getString("audit_id"),
            tenantId = rs.getString("tenant_id"),
            projectId = rs.getString("project_id"),
            reportType = BusinessFinancialReportType.valueOf(rs.getString("report_type")),
            format = BusinessFinancialReportFormat.valueOf(rs.getString("format")),
            requestedBy = rs.getString("requested_by"),
            generatedAt = rs.getLong("generated_at"),
            isSuccess = rs.getBoolean("is_success"),
            correlationId = rs.getString("correlation_id"),
            errorMessage = rs.getString("error_message")
        )
    }
}
