package com.sucharu.sucharupro.data.persistence.postgres

import com.sucharu.sucharupro.data.datasource.customerfinancialreporting.CustomerFinancialAlertDataSource
import com.sucharu.sucharupro.domain.model.customerfinancialreporting.*
import java.sql.ResultSet

class PostgresCustomerFinancialAlertDataSource(
    private val transactionManager: TransactionManager
) : CustomerFinancialAlertDataSource {

    private fun mapRowToAlert(rs: ResultSet): CustomerFinancialAlert {
        val metaJson = rs.getString("metadata_json")
        val metadata = if (!metaJson.isNullOrBlank()) {
            metaJson.removeSurrounding("{", "}").split(",")
                .filter { it.contains(":") }
                .associate {
                    val parts = it.split(":")
                    parts[0].trim().removeSurrounding("\"") to parts.getOrElse(1) { "" }.trim().removeSurrounding("\"")
                }
        } else emptyMap()

        return CustomerFinancialAlert(
            alertId = rs.getString("alert_id"),
            tenantId = rs.getString("tenant_id"),
            projectId = rs.getString("project_id"),
            customerId = rs.getString("customer_id"),
            alertType = CustomerFinancialAlertType.valueOf(rs.getString("alert_type")),
            severity = CustomerFinancialAlertSeverity.valueOf(rs.getString("severity")),
            status = CustomerFinancialAlertStatus.valueOf(rs.getString("status")),
            title = rs.getString("title"),
            safeMessage = rs.getString("safe_message"),
            sourceType = rs.getString("source_type"),
            sourceId = rs.getString("source_id"),
            detectedAt = rs.getLong("detected_at"),
            dueAt = rs.getObject("due_at")?.let { rs.getLong("due_at") },
            resolvedAt = rs.getObject("resolved_at")?.let { rs.getLong("resolved_at") },
            acknowledgedAt = rs.getObject("acknowledged_at")?.let { rs.getLong("acknowledged_at") },
            acknowledgedBy = rs.getString("acknowledged_by"),
            dismissedAt = rs.getObject("dismissed_at")?.let { rs.getLong("dismissed_at") },
            dismissedBy = rs.getString("dismissed_by"),
            dismissalReason = rs.getString("dismissal_reason"),
            expiresAt = rs.getObject("expires_at")?.let { rs.getLong("expires_at") },
            correlationId = rs.getString("correlation_id"),
            deduplicationKey = rs.getString("deduplication_key"),
            metadata = metadata,
            version = rs.getLong("version")
        )
    }

    private fun mapRowToAudit(rs: ResultSet): CustomerFinancialAlertAuditEvent {
        return CustomerFinancialAlertAuditEvent(
            eventId = rs.getString("event_id"),
            tenantId = rs.getString("tenant_id"),
            projectId = rs.getString("project_id"),
            alertId = rs.getString("alert_id"),
            eventType = CustomerFinancialAlertEventType.valueOf(rs.getString("event_type")),
            actorId = rs.getString("actor_id"),
            actorRole = rs.getString("actor_role"),
            timestamp = rs.getLong("timestamp"),
            detailsJson = rs.getString("details_json") ?: "{}"
        )
    }

    override suspend fun saveAlert(alert: CustomerFinancialAlert) {
        val metaStr = if (alert.metadata.isNotEmpty()) {
            alert.metadata.entries.joinToString(prefix = "{", postfix = "}") {
                "\"${it.key}\":\"${it.value}\""
            }
        } else "{}"

        val sql = """
            INSERT INTO customer_financial_alerts (
                alert_id, tenant_id, project_id, customer_id, alert_type, severity, status,
                title, safe_message, source_type, source_id, detected_at, due_at, resolved_at,
                acknowledged_at, acknowledged_by, dismissed_at, dismissed_by, dismissal_reason,
                expires_at, correlation_id, deduplication_key, metadata_json, version
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT (alert_id) DO UPDATE SET
                status = EXCLUDED.status,
                severity = EXCLUDED.severity,
                title = EXCLUDED.title,
                safe_message = EXCLUDED.safe_message,
                due_at = EXCLUDED.due_at,
                resolved_at = EXCLUDED.resolved_at,
                acknowledged_at = EXCLUDED.acknowledged_at,
                acknowledged_by = EXCLUDED.acknowledged_by,
                dismissed_at = EXCLUDED.dismissed_at,
                dismissed_by = EXCLUDED.dismissed_by,
                dismissal_reason = EXCLUDED.dismissal_reason,
                expires_at = EXCLUDED.expires_at,
                metadata_json = EXCLUDED.metadata_json,
                version = customer_financial_alerts.version + 1
        """.trimIndent()

        transactionManager.inTransaction(TenantContext(alert.projectId)) { tx ->
            tx.connection.prepareStatement(sql).use { stmt ->
                stmt.setString(1, alert.alertId)
                stmt.setString(2, alert.tenantId)
                stmt.setString(3, alert.projectId)
                stmt.setString(4, alert.customerId)
                stmt.setString(5, alert.alertType.name)
                stmt.setString(6, alert.severity.name)
                stmt.setString(7, alert.status.name)
                stmt.setString(8, alert.title)
                stmt.setString(9, alert.safeMessage)
                stmt.setString(10, alert.sourceType)
                stmt.setString(11, alert.sourceId)
                stmt.setLong(12, alert.detectedAt)
                if (alert.dueAt != null) stmt.setLong(13, alert.dueAt) else stmt.setNull(13, java.sql.Types.BIGINT)
                if (alert.resolvedAt != null) stmt.setLong(14, alert.resolvedAt) else stmt.setNull(14, java.sql.Types.BIGINT)
                if (alert.acknowledgedAt != null) stmt.setLong(15, alert.acknowledgedAt) else stmt.setNull(15, java.sql.Types.BIGINT)
                stmt.setString(16, alert.acknowledgedBy)
                if (alert.dismissedAt != null) stmt.setLong(17, alert.dismissedAt) else stmt.setNull(17, java.sql.Types.BIGINT)
                stmt.setString(18, alert.dismissedBy)
                stmt.setString(19, alert.dismissalReason)
                if (alert.expiresAt != null) stmt.setLong(20, alert.expiresAt) else stmt.setNull(20, java.sql.Types.BIGINT)
                stmt.setString(21, alert.correlationId)
                stmt.setString(22, alert.deduplicationKey)
                stmt.setString(23, metaStr)
                stmt.setLong(24, alert.version)
                stmt.executeUpdate()
            }
        }
    }

    override suspend fun getAlertById(
        tenantId: String,
        projectId: String,
        alertId: String
    ): CustomerFinancialAlert? {
        val sql = """
            SELECT * FROM customer_financial_alerts
            WHERE tenant_id = ? AND project_id = ? AND alert_id = ?
        """.trimIndent()

        return transactionManager.inReadOnly(TenantContext(projectId)) { tx ->
            tx.connection.prepareStatement(sql).use { stmt ->
                stmt.setString(1, tenantId)
                stmt.setString(2, projectId)
                stmt.setString(3, alertId)
                stmt.executeQuery().use { rs ->
                    if (rs.next()) mapRowToAlert(rs) else null
                }
            }
        }
    }

    override suspend fun getActiveAlertByDedupKey(
        tenantId: String,
        projectId: String,
        deduplicationKey: String
    ): CustomerFinancialAlert? {
        val sql = """
            SELECT * FROM customer_financial_alerts
            WHERE tenant_id = ? AND project_id = ? AND deduplication_key = ?
            AND status IN ('OPEN', 'ACKNOWLEDGED')
            LIMIT 1
        """.trimIndent()

        return transactionManager.inReadOnly(TenantContext(projectId)) { tx ->
            tx.connection.prepareStatement(sql).use { stmt ->
                stmt.setString(1, tenantId)
                stmt.setString(2, projectId)
                stmt.setString(3, deduplicationKey)
                stmt.executeQuery().use { rs ->
                    if (rs.next()) mapRowToAlert(rs) else null
                }
            }
        }
    }

    override suspend fun listAlerts(
        tenantId: String,
        projectId: String,
        customerId: String?,
        status: CustomerFinancialAlertStatus?,
        severity: CustomerFinancialAlertSeverity?,
        alertType: CustomerFinancialAlertType?,
        limit: Int,
        offset: Int
    ): List<CustomerFinancialAlert> {
        val sqlBuilder = StringBuilder(
            "SELECT * FROM customer_financial_alerts WHERE tenant_id = ? AND project_id = ?"
        )
        val params = mutableListOf<Any>(tenantId, projectId)

        if (!customerId.isNullOrBlank()) {
            sqlBuilder.append(" AND customer_id = ?")
            params.add(customerId)
        }
        if (status != null) {
            sqlBuilder.append(" AND status = ?")
            params.add(status.name)
        }
        if (severity != null) {
            sqlBuilder.append(" AND severity = ?")
            params.add(severity.name)
        }
        if (alertType != null) {
            sqlBuilder.append(" AND alert_type = ?")
            params.add(alertType.name)
        }

        sqlBuilder.append(" ORDER BY detected_at DESC LIMIT ? OFFSET ?")
        params.add(limit)
        params.add(offset)

        return transactionManager.inReadOnly(TenantContext(projectId)) { tx ->
            tx.connection.prepareStatement(sqlBuilder.toString()).use { stmt ->
                params.forEachIndexed { index, param ->
                    when (param) {
                        is String -> stmt.setString(index + 1, param)
                        is Int -> stmt.setInt(index + 1, param)
                        is Long -> stmt.setLong(index + 1, param)
                    }
                }
                stmt.executeQuery().use { rs ->
                    val list = mutableListOf<CustomerFinancialAlert>()
                    while (rs.next()) {
                        list.add(mapRowToAlert(rs))
                    }
                    list
                }
            }
        }
    }

    override suspend fun countAlerts(
        tenantId: String,
        projectId: String,
        customerId: String?,
        status: CustomerFinancialAlertStatus?
    ): Int {
        val sqlBuilder = StringBuilder(
            "SELECT COUNT(*) FROM customer_financial_alerts WHERE tenant_id = ? AND project_id = ?"
        )
        val params = mutableListOf<Any>(tenantId, projectId)

        if (!customerId.isNullOrBlank()) {
            sqlBuilder.append(" AND customer_id = ?")
            params.add(customerId)
        }
        if (status != null) {
            sqlBuilder.append(" AND status = ?")
            params.add(status.name)
        }

        return transactionManager.inReadOnly(TenantContext(projectId)) { tx ->
            tx.connection.prepareStatement(sqlBuilder.toString()).use { stmt ->
                params.forEachIndexed { index, param ->
                    when (param) {
                        is String -> stmt.setString(index + 1, param)
                    }
                }
                stmt.executeQuery().use { rs ->
                    if (rs.next()) rs.getInt(1) else 0
                }
            }
        }
    }

    override suspend fun recordAuditEvent(event: CustomerFinancialAlertAuditEvent) {
        val sql = """
            INSERT INTO customer_financial_alert_audit_events (
                event_id, tenant_id, project_id, alert_id, event_type,
                actor_id, actor_role, timestamp, details_json
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
        """.trimIndent()

        transactionManager.inTransaction(TenantContext(event.projectId)) { tx ->
            tx.connection.prepareStatement(sql).use { stmt ->
                stmt.setString(1, event.eventId)
                stmt.setString(2, event.tenantId)
                stmt.setString(3, event.projectId)
                stmt.setString(4, event.alertId)
                stmt.setString(5, event.eventType.name)
                stmt.setString(6, event.actorId)
                stmt.setString(7, event.actorRole)
                stmt.setLong(8, event.timestamp)
                stmt.setString(9, event.detailsJson)
                stmt.executeUpdate()
            }
        }
    }

    override suspend fun listAuditEvents(
        tenantId: String,
        projectId: String,
        alertId: String
    ): List<CustomerFinancialAlertAuditEvent> {
        val sql = """
            SELECT * FROM customer_financial_alert_audit_events
            WHERE tenant_id = ? AND project_id = ? AND alert_id = ?
            ORDER BY timestamp ASC
        """.trimIndent()

        return transactionManager.inReadOnly(TenantContext(projectId)) { tx ->
            tx.connection.prepareStatement(sql).use { stmt ->
                stmt.setString(1, tenantId)
                stmt.setString(2, projectId)
                stmt.setString(3, alertId)
                stmt.executeQuery().use { rs ->
                    val list = mutableListOf<CustomerFinancialAlertAuditEvent>()
                    while (rs.next()) {
                        list.add(mapRowToAudit(rs))
                    }
                    list
                }
            }
        }
    }
}
