package com.sucharu.sucharupro.data.persistence.postgres

import com.sucharu.sucharupro.data.datasource.machine.events.MachineEventDataSource
import com.sucharu.sucharupro.domain.machine.events.DowntimeReasonCategory
import com.sucharu.sucharupro.domain.machine.events.DowntimeStatus
import com.sucharu.sucharupro.domain.machine.events.FaultSeverity
import com.sucharu.sucharupro.domain.machine.events.FaultStatus
import com.sucharu.sucharupro.domain.machine.events.MachineDowntimeEvent
import com.sucharu.sucharupro.domain.machine.events.MachineFaultEvent
import com.sucharu.sucharupro.domain.model.common.DomainResult
import java.sql.ResultSet

/**
 * PostgreSQL implementation of MachineEventDataSource using TransactionManager & RLS.
 */
class PostgresMachineEventDataSource(
    private val transactionManager: TransactionManager
) : MachineEventDataSource {

    override suspend fun saveFaultEvent(event: MachineFaultEvent): DomainResult<MachineFaultEvent> {
        return try {
            transactionManager.inTransaction(TenantContext(event.tenantId)) { ctx ->
                val conn = ctx.connection
                val sql = """
                    INSERT INTO machine_fault_events (
                        fault_event_id, tenant_id, machine_id, fault_code, fault_type,
                        severity, status, description, source, occurred_at, detected_at,
                        acknowledged_at, acknowledged_by, resolved_at, resolved_by,
                        resolution_notes, maintenance_record_id, metadata_json, correlation_id,
                        created_at, updated_at, created_by, updated_by
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, to_timestamp(? / 1000.0), to_timestamp(? / 1000.0), ?, ?, ?, ?, ?, ?, ?, ?, to_timestamp(? / 1000.0), to_timestamp(? / 1000.0), ?, ?)
                    ON CONFLICT (fault_event_id) DO UPDATE SET
                        status = EXCLUDED.status,
                        severity = EXCLUDED.severity,
                        description = EXCLUDED.description,
                        acknowledged_at = EXCLUDED.acknowledged_at,
                        acknowledged_by = EXCLUDED.acknowledged_by,
                        resolved_at = EXCLUDED.resolved_at,
                        resolved_by = EXCLUDED.resolved_by,
                        resolution_notes = EXCLUDED.resolution_notes,
                        maintenance_record_id = EXCLUDED.maintenance_record_id,
                        metadata_json = EXCLUDED.metadata_json,
                        updated_at = EXCLUDED.updated_at,
                        updated_by = EXCLUDED.updated_by
                """.trimIndent()

                conn.prepareStatement(sql).use { ps ->
                    ps.setString(1, event.faultEventId)
                    ps.setString(2, event.tenantId)
                    ps.setString(3, event.machineId)
                    ps.setString(4, event.faultCode)
                    ps.setString(5, event.faultType)
                    ps.setString(6, event.severity.name)
                    ps.setString(7, event.status.name)
                    ps.setString(8, event.description)
                    ps.setString(9, event.source)
                    ps.setLong(10, event.occurredAt)
                    ps.setLong(11, event.detectedAt)
                    if (event.acknowledgedAt != null) ps.setTimestamp(12, java.sql.Timestamp(event.acknowledgedAt)) else ps.setNull(12, java.sql.Types.TIMESTAMP)
                    ps.setString(13, event.acknowledgedBy)
                    if (event.resolvedAt != null) ps.setTimestamp(14, java.sql.Timestamp(event.resolvedAt)) else ps.setNull(14, java.sql.Types.TIMESTAMP)
                    ps.setString(15, event.resolvedBy)
                    ps.setString(16, event.resolutionNotes)
                    ps.setString(17, event.maintenanceRecordId)
                    ps.setString(18, event.metadataJson)
                    ps.setString(19, event.correlationId)
                    ps.setLong(20, event.createdAt)
                    ps.setLong(21, event.updatedAt)
                    ps.setString(22, event.createdBy)
                    ps.setString(23, event.updatedBy)
                    ps.executeUpdate()
                }
            }
            DomainResult.Success(event)
        } catch (e: Exception) {
            PostgresErrorTranslator.translate(e, "save fault event")
        }
    }

    override suspend fun getFaultEventById(tenantId: String, faultEventId: String): DomainResult<MachineFaultEvent?> {
        return try {
            val result = transactionManager.inTransaction(TenantContext(tenantId)) { ctx ->
                val conn = ctx.connection
                val sql = "SELECT * FROM machine_fault_events WHERE tenant_id = ? AND fault_event_id = ?"
                conn.prepareStatement(sql).use { ps ->
                    ps.setString(1, tenantId)
                    ps.setString(2, faultEventId)
                    ps.executeQuery().use { rs ->
                        if (rs.next()) mapFaultEvent(rs) else null
                    }
                }
            }
            DomainResult.Success(result)
        } catch (e: Exception) {
            PostgresErrorTranslator.translate(e, "get fault event by id")
        }
    }

    override suspend fun listFaultEventsByMachine(
        tenantId: String,
        machineId: String,
        status: FaultStatus?,
        limit: Int
    ): DomainResult<List<MachineFaultEvent>> {
        return try {
            val result = transactionManager.inTransaction(TenantContext(tenantId)) { ctx ->
                val conn = ctx.connection
                val sql = StringBuilder("SELECT * FROM machine_fault_events WHERE tenant_id = ? AND machine_id = ?")
                if (status != null) sql.append(" AND status = ?")
                sql.append(" ORDER BY occurred_at DESC LIMIT ?")

                conn.prepareStatement(sql.toString()).use { ps ->
                    var idx = 1
                    ps.setString(idx++, tenantId)
                    ps.setString(idx++, machineId)
                    if (status != null) ps.setString(idx++, status.name)
                    ps.setInt(idx, limit)

                    ps.executeQuery().use { rs ->
                        val list = mutableListOf<MachineFaultEvent>()
                        while (rs.next()) list.add(mapFaultEvent(rs))
                        list
                    }
                }
            }
            DomainResult.Success(result)
        } catch (e: Exception) {
            PostgresErrorTranslator.translate(e, "list fault events by machine")
        }
    }

    override suspend fun saveDowntimeEvent(event: MachineDowntimeEvent): DomainResult<MachineDowntimeEvent> {
        return try {
            transactionManager.inTransaction(TenantContext(event.tenantId)) { ctx ->
                val conn = ctx.connection
                val sql = """
                    INSERT INTO machine_downtime_events (
                        downtime_id, tenant_id, machine_id, fault_event_id,
                        execution_job_id, work_order_id, reason_category, reason_details,
                        status, started_at, ended_at, duration_seconds,
                        created_at, updated_at, created_by, updated_by
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, to_timestamp(? / 1000.0), ?, ?, to_timestamp(? / 1000.0), to_timestamp(? / 1000.0), ?, ?)
                    ON CONFLICT (downtime_id) DO UPDATE SET
                        reason_category = EXCLUDED.reason_category,
                        reason_details = EXCLUDED.reason_details,
                        status = EXCLUDED.status,
                        ended_at = EXCLUDED.ended_at,
                        duration_seconds = EXCLUDED.duration_seconds,
                        updated_at = EXCLUDED.updated_at,
                        updated_by = EXCLUDED.updated_by
                """.trimIndent()

                conn.prepareStatement(sql).use { ps ->
                    ps.setString(1, event.downtimeId)
                    ps.setString(2, event.tenantId)
                    ps.setString(3, event.machineId)
                    ps.setString(4, event.faultEventId)
                    ps.setString(5, event.executionJobId)
                    ps.setString(6, event.workOrderId)
                    ps.setString(7, event.reasonCategory.name)
                    ps.setString(8, event.reasonDetails)
                    ps.setString(9, event.status.name)
                    ps.setLong(10, event.startedAt)
                    if (event.endedAt != null) ps.setTimestamp(11, java.sql.Timestamp(event.endedAt)) else ps.setNull(11, java.sql.Types.TIMESTAMP)
                    ps.setLong(12, event.durationSeconds)
                    ps.setLong(13, event.createdAt)
                    ps.setLong(14, event.updatedAt)
                    ps.setString(15, event.createdBy)
                    ps.setString(16, event.updatedBy)
                    ps.executeUpdate()
                }
            }
            DomainResult.Success(event)
        } catch (e: Exception) {
            PostgresErrorTranslator.translate(e, "save downtime event")
        }
    }

    override suspend fun getDowntimeEventById(tenantId: String, downtimeId: String): DomainResult<MachineDowntimeEvent?> {
        return try {
            val result = transactionManager.inTransaction(TenantContext(tenantId)) { ctx ->
                val conn = ctx.connection
                val sql = "SELECT * FROM machine_downtime_events WHERE tenant_id = ? AND downtime_id = ?"
                conn.prepareStatement(sql).use { ps ->
                    ps.setString(1, tenantId)
                    ps.setString(2, downtimeId)
                    ps.executeQuery().use { rs ->
                        if (rs.next()) mapDowntimeEvent(rs) else null
                    }
                }
            }
            DomainResult.Success(result)
        } catch (e: Exception) {
            PostgresErrorTranslator.translate(e, "get downtime event by id")
        }
    }

    override suspend fun listDowntimeEventsByMachine(
        tenantId: String,
        machineId: String,
        status: DowntimeStatus?,
        limit: Int
    ): DomainResult<List<MachineDowntimeEvent>> {
        return try {
            val result = transactionManager.inTransaction(TenantContext(tenantId)) { ctx ->
                val conn = ctx.connection
                val sql = StringBuilder("SELECT * FROM machine_downtime_events WHERE tenant_id = ? AND machine_id = ?")
                if (status != null) sql.append(" AND status = ?")
                sql.append(" ORDER BY started_at DESC LIMIT ?")

                conn.prepareStatement(sql.toString()).use { ps ->
                    var idx = 1
                    ps.setString(idx++, tenantId)
                    ps.setString(idx++, machineId)
                    if (status != null) ps.setString(idx++, status.name)
                    ps.setInt(idx, limit)

                    ps.executeQuery().use { rs ->
                        val list = mutableListOf<MachineDowntimeEvent>()
                        while (rs.next()) list.add(mapDowntimeEvent(rs))
                        list
                    }
                }
            }
            DomainResult.Success(result)
        } catch (e: Exception) {
            PostgresErrorTranslator.translate(e, "list downtime events by machine")
        }
    }

    private fun mapFaultEvent(rs: ResultSet): MachineFaultEvent {
        val occurredTs = rs.getTimestamp("occurred_at")
        val detectedTs = rs.getTimestamp("detected_at")
        val ackTs = rs.getTimestamp("acknowledged_at")
        val resTs = rs.getTimestamp("resolved_at")
        val createdTs = rs.getTimestamp("created_at")
        val updatedTs = rs.getTimestamp("updated_at")
        return MachineFaultEvent(
            faultEventId = rs.getString("fault_event_id"),
            tenantId = rs.getString("tenant_id"),
            machineId = rs.getString("machine_id"),
            faultCode = rs.getString("fault_code"),
            faultType = rs.getString("fault_type"),
            severity = try { FaultSeverity.valueOf(rs.getString("severity")) } catch (_: Exception) { FaultSeverity.FAULT },
            status = try { FaultStatus.valueOf(rs.getString("status")) } catch (_: Exception) { FaultStatus.OPEN },
            description = rs.getString("description"),
            source = rs.getString("source") ?: "MANUAL",
            occurredAt = occurredTs?.time ?: System.currentTimeMillis(),
            detectedAt = detectedTs?.time ?: System.currentTimeMillis(),
            acknowledgedAt = ackTs?.time,
            acknowledgedBy = rs.getString("acknowledged_by"),
            resolvedAt = resTs?.time,
            resolvedBy = rs.getString("resolved_by"),
            resolutionNotes = rs.getString("resolution_notes"),
            maintenanceRecordId = rs.getString("maintenance_record_id"),
            metadataJson = rs.getString("metadata_json"),
            correlationId = rs.getString("correlation_id"),
            createdAt = createdTs?.time ?: System.currentTimeMillis(),
            updatedAt = updatedTs?.time ?: System.currentTimeMillis(),
            createdBy = rs.getString("created_by"),
            updatedBy = rs.getString("updated_by")
        )
    }

    private fun mapDowntimeEvent(rs: ResultSet): MachineDowntimeEvent {
        val startedTs = rs.getTimestamp("started_at")
        val endedTs = rs.getTimestamp("ended_at")
        val createdTs = rs.getTimestamp("created_at")
        val updatedTs = rs.getTimestamp("updated_at")
        return MachineDowntimeEvent(
            downtimeId = rs.getString("downtime_id"),
            tenantId = rs.getString("tenant_id"),
            machineId = rs.getString("machine_id"),
            faultEventId = rs.getString("fault_event_id"),
            executionJobId = rs.getString("execution_job_id"),
            workOrderId = rs.getString("work_order_id"),
            reasonCategory = try { DowntimeReasonCategory.valueOf(rs.getString("reason_category")) } catch (_: Exception) { DowntimeReasonCategory.OTHER },
            reasonDetails = rs.getString("reason_details"),
            status = try { DowntimeStatus.valueOf(rs.getString("status")) } catch (_: Exception) { DowntimeStatus.STARTED },
            startedAt = startedTs?.time ?: System.currentTimeMillis(),
            endedAt = endedTs?.time,
            durationSeconds = rs.getLong("duration_seconds"),
            createdAt = createdTs?.time ?: System.currentTimeMillis(),
            updatedAt = updatedTs?.time ?: System.currentTimeMillis(),
            createdBy = rs.getString("created_by"),
            updatedBy = rs.getString("updated_by")
        )
    }
}
