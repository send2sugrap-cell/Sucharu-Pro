package com.sucharu.sucharupro.data.persistence.postgres

import com.sucharu.sucharupro.data.datasource.machine.maintenance.MachineMaintenanceDataSource
import com.sucharu.sucharupro.domain.machine.maintenance.MachineServiceHistoryLog
import com.sucharu.sucharupro.domain.machine.maintenance.MaintenanceRecord
import com.sucharu.sucharupro.domain.machine.maintenance.MaintenanceSchedule
import com.sucharu.sucharupro.domain.machine.maintenance.MaintenanceStatus
import com.sucharu.sucharupro.domain.machine.maintenance.MaintenanceType
import com.sucharu.sucharupro.domain.model.common.DomainResult
import java.sql.ResultSet

/**
 * PostgreSQL implementation of MachineMaintenanceDataSource using TransactionManager & RLS.
 */
class PostgresMachineMaintenanceDataSource(
    private val transactionManager: TransactionManager
) : MachineMaintenanceDataSource {

    override suspend fun saveSchedule(schedule: MaintenanceSchedule): DomainResult<MaintenanceSchedule> {
        return try {
            transactionManager.inTransaction(TenantContext(schedule.tenantId)) { ctx ->
                val conn = ctx.connection
                val sql = """
                    INSERT INTO machine_maintenance_schedules (
                        schedule_id, tenant_id, machine_id, title, description,
                        maintenance_type, status, planned_date, recurrence_interval_days,
                        assigned_technician_id, assigned_technician_name, notes,
                        created_at, updated_at, created_by, updated_by
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, to_timestamp(? / 1000.0), ?, ?, ?, ?, to_timestamp(? / 1000.0), to_timestamp(? / 1000.0), ?, ?)
                    ON CONFLICT (schedule_id) DO UPDATE SET
                        title = EXCLUDED.title,
                        description = EXCLUDED.description,
                        maintenance_type = EXCLUDED.maintenance_type,
                        status = EXCLUDED.status,
                        planned_date = EXCLUDED.planned_date,
                        recurrence_interval_days = EXCLUDED.recurrence_interval_days,
                        assigned_technician_id = EXCLUDED.assigned_technician_id,
                        assigned_technician_name = EXCLUDED.assigned_technician_name,
                        notes = EXCLUDED.notes,
                        updated_at = EXCLUDED.updated_at,
                        updated_by = EXCLUDED.updated_by
                """.trimIndent()

                conn.prepareStatement(sql).use { ps ->
                    ps.setString(1, schedule.scheduleId)
                    ps.setString(2, schedule.tenantId)
                    ps.setString(3, schedule.machineId)
                    ps.setString(4, schedule.title)
                    ps.setString(5, schedule.description)
                    ps.setString(6, schedule.maintenanceType.name)
                    ps.setString(7, schedule.status.name)
                    ps.setLong(8, schedule.plannedDate)
                    if (schedule.recurrenceIntervalDays != null) ps.setInt(9, schedule.recurrenceIntervalDays) else ps.setNull(9, java.sql.Types.INTEGER)
                    ps.setString(10, schedule.assignedTechnicianId)
                    ps.setString(11, schedule.assignedTechnicianName)
                    ps.setString(12, schedule.notes)
                    ps.setLong(13, schedule.createdAt)
                    ps.setLong(14, schedule.updatedAt)
                    ps.setString(15, schedule.createdBy)
                    ps.setString(16, schedule.updatedBy)
                    ps.executeUpdate()
                }
            }
            DomainResult.Success(schedule)
        } catch (e: Exception) {
            PostgresErrorTranslator.translate(e, "save maintenance schedule")
        }
    }

    override suspend fun getScheduleById(tenantId: String, scheduleId: String): DomainResult<MaintenanceSchedule?> {
        return try {
            val result = transactionManager.inTransaction(TenantContext(tenantId)) { ctx ->
                val conn = ctx.connection
                val sql = "SELECT * FROM machine_maintenance_schedules WHERE tenant_id = ? AND schedule_id = ?"
                conn.prepareStatement(sql).use { ps ->
                    ps.setString(1, tenantId)
                    ps.setString(2, scheduleId)
                    ps.executeQuery().use { rs ->
                        if (rs.next()) mapSchedule(rs) else null
                    }
                }
            }
            DomainResult.Success(result)
        } catch (e: Exception) {
            PostgresErrorTranslator.translate(e, "get maintenance schedule by id")
        }
    }

    override suspend fun listSchedulesByMachine(
        tenantId: String,
        machineId: String,
        status: MaintenanceStatus?
    ): DomainResult<List<MaintenanceSchedule>> {
        return try {
            val result = transactionManager.inTransaction(TenantContext(tenantId)) { ctx ->
                val conn = ctx.connection
                val sql = StringBuilder("SELECT * FROM machine_maintenance_schedules WHERE tenant_id = ? AND machine_id = ?")
                if (status != null) sql.append(" AND status = ?")
                sql.append(" ORDER BY planned_date ASC")

                conn.prepareStatement(sql.toString()).use { ps ->
                    var idx = 1
                    ps.setString(idx++, tenantId)
                    ps.setString(idx++, machineId)
                    if (status != null) ps.setString(idx, status.name)

                    ps.executeQuery().use { rs ->
                        val list = mutableListOf<MaintenanceSchedule>()
                        while (rs.next()) list.add(mapSchedule(rs))
                        list
                    }
                }
            }
            DomainResult.Success(result)
        } catch (e: Exception) {
            PostgresErrorTranslator.translate(e, "list maintenance schedules by machine")
        }
    }

    override suspend fun saveRecord(record: MaintenanceRecord): DomainResult<MaintenanceRecord> {
        return try {
            transactionManager.inTransaction(TenantContext(record.tenantId)) { ctx ->
                val conn = ctx.connection
                val sql = """
                    INSERT INTO machine_maintenance_records (
                        record_id, tenant_id, machine_id, schedule_id, title,
                        problem_description, work_performed, maintenance_type, status,
                        opened_at, started_at, completed_at, performed_by_id,
                        performed_by_name, resolution_summary, notes, created_at, updated_at,
                        created_by, updated_by
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, to_timestamp(? / 1000.0), ?, ?, ?, ?, ?, ?, to_timestamp(? / 1000.0), to_timestamp(? / 1000.0), ?, ?)
                    ON CONFLICT (record_id) DO UPDATE SET
                        title = EXCLUDED.title,
                        problem_description = EXCLUDED.problem_description,
                        work_performed = EXCLUDED.work_performed,
                        maintenance_type = EXCLUDED.maintenance_type,
                        status = EXCLUDED.status,
                        started_at = EXCLUDED.started_at,
                        completed_at = EXCLUDED.completed_at,
                        performed_by_id = EXCLUDED.performed_by_id,
                        performed_by_name = EXCLUDED.performed_by_name,
                        resolution_summary = EXCLUDED.resolution_summary,
                        notes = EXCLUDED.notes,
                        updated_at = EXCLUDED.updated_at,
                        updated_by = EXCLUDED.updated_by
                """.trimIndent()

                conn.prepareStatement(sql).use { ps ->
                    ps.setString(1, record.recordId)
                    ps.setString(2, record.tenantId)
                    ps.setString(3, record.machineId)
                    ps.setString(4, record.scheduleId)
                    ps.setString(5, record.title)
                    ps.setString(6, record.problemDescription)
                    ps.setString(7, record.workPerformed)
                    ps.setString(8, record.maintenanceType.name)
                    ps.setString(9, record.status.name)
                    ps.setLong(10, record.openedAt)
                    if (record.startedAt != null) ps.setTimestamp(11, java.sql.Timestamp(record.startedAt)) else ps.setNull(11, java.sql.Types.TIMESTAMP)
                    if (record.completedAt != null) ps.setTimestamp(12, java.sql.Timestamp(record.completedAt)) else ps.setNull(12, java.sql.Types.TIMESTAMP)
                    ps.setString(13, record.performedById)
                    ps.setString(14, record.performedByName)
                    ps.setString(15, record.resolutionSummary)
                    ps.setString(16, record.notes)
                    ps.setLong(17, record.createdAt)
                    ps.setLong(18, record.updatedAt)
                    ps.setString(19, record.createdBy)
                    ps.setString(20, record.updatedBy)
                    ps.executeUpdate()
                }
            }
            DomainResult.Success(record)
        } catch (e: Exception) {
            PostgresErrorTranslator.translate(e, "save maintenance record")
        }
    }

    override suspend fun getRecordById(tenantId: String, recordId: String): DomainResult<MaintenanceRecord?> {
        return try {
            val result = transactionManager.inTransaction(TenantContext(tenantId)) { ctx ->
                val conn = ctx.connection
                val sql = "SELECT * FROM machine_maintenance_records WHERE tenant_id = ? AND record_id = ?"
                conn.prepareStatement(sql).use { ps ->
                    ps.setString(1, tenantId)
                    ps.setString(2, recordId)
                    ps.executeQuery().use { rs ->
                        if (rs.next()) mapRecord(rs) else null
                    }
                }
            }
            DomainResult.Success(result)
        } catch (e: Exception) {
            PostgresErrorTranslator.translate(e, "get maintenance record by id")
        }
    }

    override suspend fun listRecordsByMachine(
        tenantId: String,
        machineId: String,
        status: MaintenanceStatus?,
        limit: Int
    ): DomainResult<List<MaintenanceRecord>> {
        return try {
            val result = transactionManager.inTransaction(TenantContext(tenantId)) { ctx ->
                val conn = ctx.connection
                val sql = StringBuilder("SELECT * FROM machine_maintenance_records WHERE tenant_id = ? AND machine_id = ?")
                if (status != null) sql.append(" AND status = ?")
                sql.append(" ORDER BY opened_at DESC LIMIT ?")

                conn.prepareStatement(sql.toString()).use { ps ->
                    var idx = 1
                    ps.setString(idx++, tenantId)
                    ps.setString(idx++, machineId)
                    if (status != null) ps.setString(idx++, status.name)
                    ps.setInt(idx, limit)

                    ps.executeQuery().use { rs ->
                        val list = mutableListOf<MaintenanceRecord>()
                        while (rs.next()) list.add(mapRecord(rs))
                        list
                    }
                }
            }
            DomainResult.Success(result)
        } catch (e: Exception) {
            PostgresErrorTranslator.translate(e, "list maintenance records by machine")
        }
    }

    override suspend fun saveHistoryLog(log: MachineServiceHistoryLog): DomainResult<MachineServiceHistoryLog> {
        return try {
            transactionManager.inTransaction(TenantContext(log.tenantId)) { ctx ->
                val conn = ctx.connection
                val sql = """
                    INSERT INTO machine_service_history_logs (
                        history_id, tenant_id, machine_id, record_id,
                        action_type, performed_by_id, performed_by_name, details_json, recorded_at
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, to_timestamp(? / 1000.0))
                    ON CONFLICT (history_id) DO NOTHING
                """.trimIndent()

                conn.prepareStatement(sql).use { ps ->
                    ps.setString(1, log.historyId)
                    ps.setString(2, log.tenantId)
                    ps.setString(3, log.machineId)
                    ps.setString(4, log.recordId)
                    ps.setString(5, log.actionType)
                    ps.setString(6, log.performedById)
                    ps.setString(7, log.performedByName)
                    ps.setString(8, log.detailsJson)
                    ps.setLong(9, log.recordedAt)
                    ps.executeUpdate()
                }
            }
            DomainResult.Success(log)
        } catch (e: Exception) {
            PostgresErrorTranslator.translate(e, "save service history log")
        }
    }

    override suspend fun listHistoryLogsByRecord(
        tenantId: String,
        recordId: String
    ): DomainResult<List<MachineServiceHistoryLog>> {
        return try {
            val result = transactionManager.inTransaction(TenantContext(tenantId)) { ctx ->
                val conn = ctx.connection
                val sql = "SELECT * FROM machine_service_history_logs WHERE tenant_id = ? AND record_id = ? ORDER BY recorded_at DESC"
                conn.prepareStatement(sql).use { ps ->
                    ps.setString(1, tenantId)
                    ps.setString(2, recordId)
                    ps.executeQuery().use { rs ->
                        val list = mutableListOf<MachineServiceHistoryLog>()
                        while (rs.next()) list.add(mapHistoryLog(rs))
                        list
                    }
                }
            }
            DomainResult.Success(result)
        } catch (e: Exception) {
            PostgresErrorTranslator.translate(e, "list history logs by record")
        }
    }

    override suspend fun listHistoryLogsByMachine(
        tenantId: String,
        machineId: String,
        limit: Int
    ): DomainResult<List<MachineServiceHistoryLog>> {
        return try {
            val result = transactionManager.inTransaction(TenantContext(tenantId)) { ctx ->
                val conn = ctx.connection
                val sql = "SELECT * FROM machine_service_history_logs WHERE tenant_id = ? AND machine_id = ? ORDER BY recorded_at DESC LIMIT ?"
                conn.prepareStatement(sql).use { ps ->
                    ps.setString(1, tenantId)
                    ps.setString(2, machineId)
                    ps.setInt(3, limit)
                    ps.executeQuery().use { rs ->
                        val list = mutableListOf<MachineServiceHistoryLog>()
                        while (rs.next()) list.add(mapHistoryLog(rs))
                        list
                    }
                }
            }
            DomainResult.Success(result)
        } catch (e: Exception) {
            PostgresErrorTranslator.translate(e, "list history logs by machine")
        }
    }

    private fun mapSchedule(rs: ResultSet): MaintenanceSchedule {
        val plannedTs = rs.getTimestamp("planned_date")
        val createdTs = rs.getTimestamp("created_at")
        val updatedTs = rs.getTimestamp("updated_at")
        val recDays = rs.getInt("recurrence_interval_days")
        return MaintenanceSchedule(
            scheduleId = rs.getString("schedule_id"),
            tenantId = rs.getString("tenant_id"),
            machineId = rs.getString("machine_id"),
            title = rs.getString("title"),
            description = rs.getString("description"),
            maintenanceType = try { MaintenanceType.valueOf(rs.getString("maintenance_type")) } catch (_: Exception) { MaintenanceType.PREVENTIVE },
            status = try { MaintenanceStatus.valueOf(rs.getString("status")) } catch (_: Exception) { MaintenanceStatus.PLANNED },
            plannedDate = plannedTs?.time ?: System.currentTimeMillis(),
            recurrenceIntervalDays = if (rs.wasNull()) null else recDays,
            assignedTechnicianId = rs.getString("assigned_technician_id"),
            assignedTechnicianName = rs.getString("assigned_technician_name"),
            notes = rs.getString("notes"),
            createdAt = createdTs?.time ?: System.currentTimeMillis(),
            updatedAt = updatedTs?.time ?: System.currentTimeMillis(),
            createdBy = rs.getString("created_by"),
            updatedBy = rs.getString("updated_by")
        )
    }

    private fun mapRecord(rs: ResultSet): MaintenanceRecord {
        val openedTs = rs.getTimestamp("opened_at")
        val startedTs = rs.getTimestamp("started_at")
        val completedTs = rs.getTimestamp("completed_at")
        val createdTs = rs.getTimestamp("created_at")
        val updatedTs = rs.getTimestamp("updated_at")
        return MaintenanceRecord(
            recordId = rs.getString("record_id"),
            tenantId = rs.getString("tenant_id"),
            machineId = rs.getString("machine_id"),
            scheduleId = rs.getString("schedule_id"),
            title = rs.getString("title"),
            problemDescription = rs.getString("problem_description"),
            workPerformed = rs.getString("work_performed"),
            maintenanceType = try { MaintenanceType.valueOf(rs.getString("maintenance_type")) } catch (_: Exception) { MaintenanceType.CORRECTIVE },
            status = try { MaintenanceStatus.valueOf(rs.getString("status")) } catch (_: Exception) { MaintenanceStatus.PLANNED },
            openedAt = openedTs?.time ?: System.currentTimeMillis(),
            startedAt = startedTs?.time,
            completedAt = completedTs?.time,
            performedById = rs.getString("performed_by_id"),
            performedByName = rs.getString("performed_by_name"),
            resolutionSummary = rs.getString("resolution_summary"),
            notes = rs.getString("notes"),
            createdAt = createdTs?.time ?: System.currentTimeMillis(),
            updatedAt = updatedTs?.time ?: System.currentTimeMillis(),
            createdBy = rs.getString("created_by"),
            updatedBy = rs.getString("updated_by")
        )
    }

    private fun mapHistoryLog(rs: ResultSet): MachineServiceHistoryLog {
        val recordedTs = rs.getTimestamp("recorded_at")
        return MachineServiceHistoryLog(
            historyId = rs.getString("history_id"),
            tenantId = rs.getString("tenant_id"),
            machineId = rs.getString("machine_id"),
            recordId = rs.getString("record_id"),
            actionType = rs.getString("action_type"),
            performedById = rs.getString("performed_by_id"),
            performedByName = rs.getString("performed_by_name"),
            detailsJson = rs.getString("details_json"),
            recordedAt = recordedTs?.time ?: System.currentTimeMillis()
        )
    }
}
