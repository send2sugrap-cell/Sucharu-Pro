package com.sucharu.sucharupro.data.persistence.postgres

import com.sucharu.sucharupro.data.datasource.machine.alerts.MachineAlertDataSource
import com.sucharu.sucharupro.domain.machine.alerts.MachineAlertStatus
import com.sucharu.sucharupro.domain.machine.alerts.MachineAlertType
import com.sucharu.sucharupro.domain.machine.alerts.MachineOperationalAlert
import com.sucharu.sucharupro.domain.machine.events.FaultSeverity
import com.sucharu.sucharupro.domain.model.common.DomainResult
import java.sql.ResultSet

/**
 * PostgreSQL implementation of MachineAlertDataSource using TransactionManager & RLS.
 */
class PostgresMachineAlertDataSource(
    private val transactionManager: TransactionManager
) : MachineAlertDataSource {

    override suspend fun saveAlert(alert: MachineOperationalAlert): DomainResult<MachineOperationalAlert> {
        return try {
            transactionManager.inTransaction(TenantContext(alert.tenantId)) { ctx ->
                val conn = ctx.connection
                val sql = """
                    INSERT INTO machine_operational_alerts (
                        alert_id, tenant_id, machine_id, alert_type, severity,
                        status, source, telemetry_record_id, fault_event_id,
                        maintenance_schedule_id, maintenance_record_id, title,
                        description, correlation_key, notification_id, occurred_at,
                        acknowledged_at, acknowledged_by, resolved_at, resolved_by,
                        resolution_notes, created_at, updated_at, created_by, updated_by
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, to_timestamp(? / 1000.0), ?, ?, ?, ?, ?, to_timestamp(? / 1000.0), to_timestamp(? / 1000.0), ?, ?)
                    ON CONFLICT (alert_id) DO UPDATE SET
                        status = EXCLUDED.status,
                        severity = EXCLUDED.severity,
                        title = EXCLUDED.title,
                        description = EXCLUDED.description,
                        notification_id = EXCLUDED.notification_id,
                        acknowledged_at = EXCLUDED.acknowledged_at,
                        acknowledged_by = EXCLUDED.acknowledged_by,
                        resolved_at = EXCLUDED.resolved_at,
                        resolved_by = EXCLUDED.resolved_by,
                        resolution_notes = EXCLUDED.resolution_notes,
                        updated_at = EXCLUDED.updated_at,
                        updated_by = EXCLUDED.updated_by
                """.trimIndent()

                conn.prepareStatement(sql).use { ps ->
                    ps.setString(1, alert.alertId)
                    ps.setString(2, alert.tenantId)
                    ps.setString(3, alert.machineId)
                    ps.setString(4, alert.alertType.name)
                    ps.setString(5, alert.severity.name)
                    ps.setString(6, alert.status.name)
                    ps.setString(7, alert.source)
                    ps.setString(8, alert.telemetryRecordId)
                    ps.setString(9, alert.faultEventId)
                    ps.setString(10, alert.maintenanceScheduleId)
                    ps.setString(11, alert.maintenanceRecordId)
                    ps.setString(12, alert.title)
                    ps.setString(13, alert.description)
                    ps.setString(14, alert.correlationKey)
                    ps.setString(15, alert.notificationId)
                    ps.setLong(16, alert.occurredAt)
                    if (alert.acknowledgedAt != null) ps.setTimestamp(17, java.sql.Timestamp(alert.acknowledgedAt)) else ps.setNull(17, java.sql.Types.TIMESTAMP)
                    ps.setString(18, alert.acknowledgedBy)
                    if (alert.resolvedAt != null) ps.setTimestamp(19, java.sql.Timestamp(alert.resolvedAt)) else ps.setNull(19, java.sql.Types.TIMESTAMP)
                    ps.setString(20, alert.resolvedBy)
                    ps.setString(21, alert.resolutionNotes)
                    ps.setLong(22, alert.createdAt)
                    ps.setLong(23, alert.updatedAt)
                    ps.setString(24, alert.createdBy)
                    ps.setString(25, alert.updatedBy)
                    ps.executeUpdate()
                }
            }
            DomainResult.Success(alert)
        } catch (e: Exception) {
            PostgresErrorTranslator.translate(e, "save machine operational alert")
        }
    }

    override suspend fun getAlertById(tenantId: String, alertId: String): DomainResult<MachineOperationalAlert?> {
        return try {
            val result = transactionManager.inTransaction(TenantContext(tenantId)) { ctx ->
                val conn = ctx.connection
                val sql = "SELECT * FROM machine_operational_alerts WHERE tenant_id = ? AND alert_id = ?"
                conn.prepareStatement(sql).use { ps ->
                    ps.setString(1, tenantId)
                    ps.setString(2, alertId)
                    ps.executeQuery().use { rs ->
                        if (rs.next()) mapAlert(rs) else null
                    }
                }
            }
            DomainResult.Success(result)
        } catch (e: Exception) {
            PostgresErrorTranslator.translate(e, "get machine alert by id")
        }
    }

    override suspend fun getAlertByCorrelationKey(
        tenantId: String,
        correlationKey: String
    ): DomainResult<MachineOperationalAlert?> {
        return try {
            val result = transactionManager.inTransaction(TenantContext(tenantId)) { ctx ->
                val conn = ctx.connection
                val sql = "SELECT * FROM machine_operational_alerts WHERE tenant_id = ? AND correlation_key = ? AND status NOT IN ('RESOLVED', 'DISMISSED') ORDER BY occurred_at DESC LIMIT 1"
                conn.prepareStatement(sql).use { ps ->
                    ps.setString(1, tenantId)
                    ps.setString(2, correlationKey)
                    ps.executeQuery().use { rs ->
                        if (rs.next()) mapAlert(rs) else null
                    }
                }
            }
            DomainResult.Success(result)
        } catch (e: Exception) {
            PostgresErrorTranslator.translate(e, "get machine alert by correlation key")
        }
    }

    override suspend fun listAlertsByMachine(
        tenantId: String,
        machineId: String,
        status: MachineAlertStatus?,
        limit: Int
    ): DomainResult<List<MachineOperationalAlert>> {
        return try {
            val result = transactionManager.inTransaction(TenantContext(tenantId)) { ctx ->
                val conn = ctx.connection
                val sql = StringBuilder("SELECT * FROM machine_operational_alerts WHERE tenant_id = ? AND machine_id = ?")
                if (status != null) sql.append(" AND status = ?")
                sql.append(" ORDER BY occurred_at DESC LIMIT ?")

                conn.prepareStatement(sql.toString()).use { ps ->
                    var idx = 1
                    ps.setString(idx++, tenantId)
                    ps.setString(idx++, machineId)
                    if (status != null) ps.setString(idx++, status.name)
                    ps.setInt(idx, limit)

                    ps.executeQuery().use { rs ->
                        val list = mutableListOf<MachineOperationalAlert>()
                        while (rs.next()) list.add(mapAlert(rs))
                        list
                    }
                }
            }
            DomainResult.Success(result)
        } catch (e: Exception) {
            PostgresErrorTranslator.translate(e, "list machine alerts")
        }
    }

    private fun mapAlert(rs: ResultSet): MachineOperationalAlert {
        val occurredTs = rs.getTimestamp("occurred_at")
        val ackTs = rs.getTimestamp("acknowledged_at")
        val resTs = rs.getTimestamp("resolved_at")
        val createdTs = rs.getTimestamp("created_at")
        val updatedTs = rs.getTimestamp("updated_at")
        return MachineOperationalAlert(
            alertId = rs.getString("alert_id"),
            tenantId = rs.getString("tenant_id"),
            machineId = rs.getString("machine_id"),
            alertType = try { MachineAlertType.valueOf(rs.getString("alert_type")) } catch (_: Exception) { MachineAlertType.WARNING },
            severity = try { FaultSeverity.valueOf(rs.getString("severity")) } catch (_: Exception) { FaultSeverity.WARNING },
            status = try { MachineAlertStatus.valueOf(rs.getString("status")) } catch (_: Exception) { MachineAlertStatus.ACTIVE },
            source = rs.getString("source") ?: "SYSTEM",
            telemetryRecordId = rs.getString("telemetry_record_id"),
            faultEventId = rs.getString("fault_event_id"),
            maintenanceScheduleId = rs.getString("maintenance_schedule_id"),
            maintenanceRecordId = rs.getString("maintenance_record_id"),
            title = rs.getString("title"),
            description = rs.getString("description"),
            correlationKey = rs.getString("correlation_key"),
            notificationId = rs.getString("notification_id"),
            occurredAt = occurredTs?.time ?: System.currentTimeMillis(),
            acknowledgedAt = ackTs?.time,
            acknowledgedBy = rs.getString("acknowledged_by"),
            resolvedAt = resTs?.time,
            resolvedBy = rs.getString("resolved_by"),
            resolutionNotes = rs.getString("resolution_notes"),
            createdAt = createdTs?.time ?: System.currentTimeMillis(),
            updatedAt = updatedTs?.time ?: System.currentTimeMillis(),
            createdBy = rs.getString("created_by"),
            updatedBy = rs.getString("updated_by")
        )
    }
}
