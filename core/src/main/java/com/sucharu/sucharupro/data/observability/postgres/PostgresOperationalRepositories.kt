package com.sucharu.sucharupro.data.observability.postgres

import com.sucharu.sucharupro.data.persistence.postgres.TenantContext
import com.sucharu.sucharupro.data.persistence.postgres.TransactionManager
import com.sucharu.sucharupro.domain.observability.AlertSeverity
import com.sucharu.sucharupro.domain.observability.AlertStatus
import com.sucharu.sucharupro.domain.observability.OperationalAlert
import java.sql.Timestamp
import java.util.concurrent.ConcurrentHashMap

/**
 * Interface for persisting and querying operational alerts.
 */
interface OperationalAlertRepository {
    suspend fun saveAlert(alert: OperationalAlert, tenantContext: TenantContext)
    suspend fun getAlert(alertId: String, tenantContext: TenantContext): OperationalAlert?
    suspend fun listActiveAlerts(tenantContext: TenantContext): List<OperationalAlert>
    suspend fun resolveAlert(alertId: String, resolutionNotes: String, tenantContext: TenantContext): Boolean
    suspend fun acknowledgeAlert(alertId: String, acknowledgedBy: String, tenantContext: TenantContext): Boolean
}

/**
 * In-memory thread-safe implementation of OperationalAlertRepository.
 */
class InMemoryOperationalAlertRepository : OperationalAlertRepository {
    private val alerts = ConcurrentHashMap<String, OperationalAlert>()

    override suspend fun saveAlert(alert: OperationalAlert, tenantContext: TenantContext) {
        val key = "${tenantContext.projectId}:${alert.alertId}"
        alerts[key] = alert
    }

    override suspend fun getAlert(alertId: String, tenantContext: TenantContext): OperationalAlert? {
        val key = "${tenantContext.projectId}:$alertId"
        return alerts[key]
    }

    override suspend fun listActiveAlerts(tenantContext: TenantContext): List<OperationalAlert> {
        return alerts.values
            .filter { it.projectId == tenantContext.projectId && it.status != AlertStatus.RESOLVED }
            .sortedByDescending { it.lastOccurredAt }
    }

    override suspend fun resolveAlert(alertId: String, resolutionNotes: String, tenantContext: TenantContext): Boolean {
        val key = "${tenantContext.projectId}:$alertId"
        val existing = alerts[key] ?: return false
        alerts[key] = existing.copy(
            status = AlertStatus.RESOLVED,
            resolvedAt = System.currentTimeMillis(),
            resolutionNotes = resolutionNotes
        )
        return true
    }

    override suspend fun acknowledgeAlert(alertId: String, acknowledgedBy: String, tenantContext: TenantContext): Boolean {
        val key = "${tenantContext.projectId}:$alertId"
        val existing = alerts[key] ?: return false
        alerts[key] = existing.copy(
            status = AlertStatus.ACKNOWLEDGED,
            acknowledgedBy = acknowledgedBy
        )
        return true
    }

    fun clear() = alerts.clear()
}

/**
 * PostgreSQL persistent implementation of OperationalAlertRepository with RLS (INFRA-04 Step 09).
 */
class PostgresOperationalAlertRepository(
    private val transactionManager: TransactionManager
) : OperationalAlertRepository {

    override suspend fun saveAlert(
        alert: OperationalAlert,
        tenantContext: TenantContext
    ) = transactionManager.inTransaction(tenantContext) { tx ->
        val sql = """
            INSERT INTO operational_alerts (
                alert_id, project_id, alert_key, deduplication_key, title,
                summary, severity, status, subsystem, failure_class,
                occurrences, first_occurred_at, last_occurred_at, resolved_at,
                acknowledged_by, resolution_notes
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT (project_id, deduplication_key)
            DO UPDATE SET
                occurrences = operational_alerts.occurrences + 1,
                last_occurred_at = EXCLUDED.last_occurred_at,
                summary = EXCLUDED.summary,
                severity = EXCLUDED.severity,
                status = CASE WHEN operational_alerts.status = 'RESOLVED' THEN 'OPEN' ELSE operational_alerts.status END
        """.trimIndent()

        tx.connection.prepareStatement(sql).use { stmt ->
            stmt.setString(1, alert.alertId)
            stmt.setString(2, alert.projectId)
            stmt.setString(3, alert.alertKey)
            stmt.setString(4, alert.deduplicationKey)
            stmt.setString(5, alert.title)
            stmt.setString(6, alert.summary)
            stmt.setString(7, alert.severity.name)
            stmt.setString(8, alert.status.name)
            stmt.setString(9, alert.subsystem)
            stmt.setString(10, alert.failureClass)
            stmt.setInt(11, alert.occurrences)
            stmt.setTimestamp(12, Timestamp(alert.firstOccurredAt))
            stmt.setTimestamp(13, Timestamp(alert.lastOccurredAt))
            stmt.setObject(14, alert.resolvedAt?.let { Timestamp(it) })
            stmt.setString(15, alert.acknowledgedBy)
            stmt.setString(16, alert.resolutionNotes)
            stmt.executeUpdate()
        }
        Unit
    }

    override suspend fun getAlert(
        alertId: String,
        tenantContext: TenantContext
    ): OperationalAlert? = transactionManager.inReadOnly(tenantContext) { tx ->
        val sql = """
            SELECT alert_id, project_id, alert_key, deduplication_key, title,
                   summary, severity, status, subsystem, failure_class,
                   occurrences, first_occurred_at, last_occurred_at, resolved_at,
                   acknowledged_by, resolution_notes
            FROM operational_alerts
            WHERE project_id = ? AND alert_id = ?
        """.trimIndent()

        tx.connection.prepareStatement(sql).use { stmt ->
            stmt.setString(1, tenantContext.projectId)
            stmt.setString(2, alertId)
            val rs = stmt.executeQuery()
            if (rs.next()) {
                OperationalAlert(
                    alertId = rs.getString("alert_id"),
                    projectId = rs.getString("project_id"),
                    alertKey = rs.getString("alert_key"),
                    deduplicationKey = rs.getString("deduplication_key"),
                    title = rs.getString("title"),
                    summary = rs.getString("summary"),
                    severity = AlertSeverity.valueOf(rs.getString("severity")),
                    status = AlertStatus.valueOf(rs.getString("status")),
                    subsystem = rs.getString("subsystem"),
                    failureClass = rs.getString("failure_class"),
                    occurrences = rs.getInt("occurrences"),
                    firstOccurredAt = rs.getTimestamp("first_occurred_at").time,
                    lastOccurredAt = rs.getTimestamp("last_occurred_at").time,
                    resolvedAt = rs.getTimestamp("resolved_at")?.time,
                    acknowledgedBy = rs.getString("acknowledged_by"),
                    resolutionNotes = rs.getString("resolution_notes")
                )
            } else null
        }
    }

    override suspend fun listActiveAlerts(
        tenantContext: TenantContext
    ): List<OperationalAlert> = transactionManager.inReadOnly(tenantContext) { tx ->
        val sql = """
            SELECT alert_id, project_id, alert_key, deduplication_key, title,
                   summary, severity, status, subsystem, failure_class,
                   occurrences, first_occurred_at, last_occurred_at, resolved_at,
                   acknowledged_by, resolution_notes
            FROM operational_alerts
            WHERE project_id = ? AND status != 'RESOLVED'
            ORDER BY last_occurred_at DESC
        """.trimIndent()

        val list = mutableListOf<OperationalAlert>()
        tx.connection.prepareStatement(sql).use { stmt ->
            stmt.setString(1, tenantContext.projectId)
            val rs = stmt.executeQuery()
            while (rs.next()) {
                list.add(
                    OperationalAlert(
                        alertId = rs.getString("alert_id"),
                        projectId = rs.getString("project_id"),
                        alertKey = rs.getString("alert_key"),
                        deduplicationKey = rs.getString("deduplication_key"),
                        title = rs.getString("title"),
                        summary = rs.getString("summary"),
                        severity = AlertSeverity.valueOf(rs.getString("severity")),
                        status = AlertStatus.valueOf(rs.getString("status")),
                        subsystem = rs.getString("subsystem"),
                        failureClass = rs.getString("failure_class"),
                        occurrences = rs.getInt("occurrences"),
                        firstOccurredAt = rs.getTimestamp("first_occurred_at").time,
                        lastOccurredAt = rs.getTimestamp("last_occurred_at").time,
                        resolvedAt = rs.getTimestamp("resolved_at")?.time,
                        acknowledgedBy = rs.getString("acknowledged_by"),
                        resolutionNotes = rs.getString("resolution_notes")
                    )
                )
            }
        }
        list
    }

    override suspend fun resolveAlert(
        alertId: String,
        resolutionNotes: String,
        tenantContext: TenantContext
    ): Boolean = transactionManager.inTransaction(tenantContext) { tx ->
        val sql = """
            UPDATE operational_alerts
            SET status = 'RESOLVED', resolved_at = NOW(), resolution_notes = ?
            WHERE project_id = ? AND alert_id = ? AND status != 'RESOLVED'
        """.trimIndent()

        tx.connection.prepareStatement(sql).use { stmt ->
            stmt.setString(1, resolutionNotes)
            stmt.setString(2, tenantContext.projectId)
            stmt.setString(3, alertId)
            stmt.executeUpdate() > 0
        }
    }

    override suspend fun acknowledgeAlert(
        alertId: String,
        acknowledgedBy: String,
        tenantContext: TenantContext
    ): Boolean = transactionManager.inTransaction(tenantContext) { tx ->
        val sql = """
            UPDATE operational_alerts
            SET status = 'ACKNOWLEDGED', acknowledged_by = ?
            WHERE project_id = ? AND alert_id = ? AND status = 'OPEN'
        """.trimIndent()

        tx.connection.prepareStatement(sql).use { stmt ->
            stmt.setString(1, acknowledgedBy)
            stmt.setString(2, tenantContext.projectId)
            stmt.setString(3, alertId)
            stmt.executeUpdate() > 0
        }
    }
}
