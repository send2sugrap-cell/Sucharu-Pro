package com.sucharu.sucharupro.data.datasource.machine.maintenance

import com.sucharu.sucharupro.domain.machine.maintenance.MachineServiceHistoryLog
import com.sucharu.sucharupro.domain.machine.maintenance.MaintenanceRecord
import com.sucharu.sucharupro.domain.machine.maintenance.MaintenanceSchedule
import com.sucharu.sucharupro.domain.machine.maintenance.MaintenanceStatus
import com.sucharu.sucharupro.domain.model.common.DomainResult
import java.util.concurrent.ConcurrentHashMap

/**
 * Thread-safe fake in-memory data source for Machine Maintenance testing.
 */
class FakeMachineMaintenanceDataSource : MachineMaintenanceDataSource {

    private val schedules = ConcurrentHashMap<String, MaintenanceSchedule>()
    private val records = ConcurrentHashMap<String, MaintenanceRecord>()
    private val historyLogs = ConcurrentHashMap<String, MachineServiceHistoryLog>()

    override suspend fun saveSchedule(schedule: MaintenanceSchedule): DomainResult<MaintenanceSchedule> {
        val key = "${schedule.tenantId}:${schedule.scheduleId}"
        schedules[key] = schedule
        return DomainResult.Success(schedule)
    }

    override suspend fun getScheduleById(tenantId: String, scheduleId: String): DomainResult<MaintenanceSchedule?> {
        val key = "$tenantId:$scheduleId"
        return DomainResult.Success(schedules[key])
    }

    override suspend fun listSchedulesByMachine(
        tenantId: String,
        machineId: String,
        status: MaintenanceStatus?
    ): DomainResult<List<MaintenanceSchedule>> {
        val filtered = schedules.values.filter { s ->
            s.tenantId == tenantId &&
            s.machineId == machineId &&
            (status == null || s.status == status)
        }.sortedBy { it.plannedDate }
        return DomainResult.Success(filtered)
    }

    override suspend fun saveRecord(record: MaintenanceRecord): DomainResult<MaintenanceRecord> {
        val key = "${record.tenantId}:${record.recordId}"
        records[key] = record
        return DomainResult.Success(record)
    }

    override suspend fun getRecordById(tenantId: String, recordId: String): DomainResult<MaintenanceRecord?> {
        val key = "$tenantId:$recordId"
        return DomainResult.Success(records[key])
    }

    override suspend fun listRecordsByMachine(
        tenantId: String,
        machineId: String,
        status: MaintenanceStatus?,
        limit: Int
    ): DomainResult<List<MaintenanceRecord>> {
        val filtered = records.values.filter { r ->
            r.tenantId == tenantId &&
            r.machineId == machineId &&
            (status == null || r.status == status)
        }.sortedByDescending { it.openedAt }.take(limit)
        return DomainResult.Success(filtered)
    }

    override suspend fun saveHistoryLog(log: MachineServiceHistoryLog): DomainResult<MachineServiceHistoryLog> {
        val key = "${log.tenantId}:${log.historyId}"
        historyLogs[key] = log
        return DomainResult.Success(log)
    }

    override suspend fun listHistoryLogsByRecord(
        tenantId: String,
        recordId: String
    ): DomainResult<List<MachineServiceHistoryLog>> {
        val filtered = historyLogs.values.filter { l ->
            l.tenantId == tenantId && l.recordId == recordId
        }.sortedByDescending { it.recordedAt }
        return DomainResult.Success(filtered)
    }

    override suspend fun listHistoryLogsByMachine(
        tenantId: String,
        machineId: String,
        limit: Int
    ): DomainResult<List<MachineServiceHistoryLog>> {
        val filtered = historyLogs.values.filter { l ->
            l.tenantId == tenantId && l.machineId == machineId
        }.sortedByDescending { it.recordedAt }.take(limit)
        return DomainResult.Success(filtered)
    }
}
