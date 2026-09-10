package com.sucharu.sucharupro.data.repository.machine.maintenance

import com.sucharu.sucharupro.data.datasource.machine.maintenance.MachineMaintenanceDataSource
import com.sucharu.sucharupro.domain.machine.maintenance.MachineMaintenanceValidator
import com.sucharu.sucharupro.domain.machine.maintenance.MachineServiceHistoryLog
import com.sucharu.sucharupro.domain.machine.maintenance.MaintenanceRecord
import com.sucharu.sucharupro.domain.machine.maintenance.MaintenanceSchedule
import com.sucharu.sucharupro.domain.machine.maintenance.MaintenanceStatus
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.repository.machine.maintenance.MachineMaintenanceRepository

/**
 * Production Repository implementation delegating to MachineMaintenanceDataSource with validation.
 */
class MachineMaintenanceRepositoryImpl(
    private val dataSource: MachineMaintenanceDataSource
) : MachineMaintenanceRepository {

    override suspend fun saveSchedule(schedule: MaintenanceSchedule): DomainResult<MaintenanceSchedule> {
        val validation = MachineMaintenanceValidator.validateSchedule(schedule)
        if (validation is DomainResult.Error) {
            return validation
        }
        return dataSource.saveSchedule(schedule)
    }

    override suspend fun getScheduleById(tenantId: String, scheduleId: String): DomainResult<MaintenanceSchedule?> {
        if (tenantId.isBlank() || scheduleId.isBlank()) {
            return DomainResult.Error(message = "Tenant ID and Schedule ID cannot be blank.")
        }
        return dataSource.getScheduleById(tenantId, scheduleId)
    }

    override suspend fun listSchedulesByMachine(
        tenantId: String,
        machineId: String,
        status: MaintenanceStatus?
    ): DomainResult<List<MaintenanceSchedule>> {
        if (tenantId.isBlank() || machineId.isBlank()) {
            return DomainResult.Error(message = "Tenant ID and Machine ID cannot be blank.")
        }
        return dataSource.listSchedulesByMachine(tenantId, machineId, status)
    }

    override suspend fun saveRecord(record: MaintenanceRecord): DomainResult<MaintenanceRecord> {
        val validation = MachineMaintenanceValidator.validateRecord(record)
        if (validation is DomainResult.Error) {
            return validation
        }
        return dataSource.saveRecord(record)
    }

    override suspend fun getRecordById(tenantId: String, recordId: String): DomainResult<MaintenanceRecord?> {
        if (tenantId.isBlank() || recordId.isBlank()) {
            return DomainResult.Error(message = "Tenant ID and Record ID cannot be blank.")
        }
        return dataSource.getRecordById(tenantId, recordId)
    }

    override suspend fun listRecordsByMachine(
        tenantId: String,
        machineId: String,
        status: MaintenanceStatus?,
        limit: Int
    ): DomainResult<List<MaintenanceRecord>> {
        if (tenantId.isBlank() || machineId.isBlank()) {
            return DomainResult.Error(message = "Tenant ID and Machine ID cannot be blank.")
        }
        return dataSource.listRecordsByMachine(tenantId, machineId, status, limit)
    }

    override suspend fun saveHistoryLog(log: MachineServiceHistoryLog): DomainResult<MachineServiceHistoryLog> {
        if (log.tenantId.isBlank() || log.machineId.isBlank() || log.recordId.isBlank()) {
            return DomainResult.Error(message = "Tenant ID, Machine ID, and Record ID cannot be blank.")
        }
        return dataSource.saveHistoryLog(log)
    }

    override suspend fun listHistoryLogsByRecord(
        tenantId: String,
        recordId: String
    ): DomainResult<List<MachineServiceHistoryLog>> {
        if (tenantId.isBlank() || recordId.isBlank()) {
            return DomainResult.Error(message = "Tenant ID and Record ID cannot be blank.")
        }
        return dataSource.listHistoryLogsByRecord(tenantId, recordId)
    }

    override suspend fun listHistoryLogsByMachine(
        tenantId: String,
        machineId: String,
        limit: Int
    ): DomainResult<List<MachineServiceHistoryLog>> {
        if (tenantId.isBlank() || machineId.isBlank()) {
            return DomainResult.Error(message = "Tenant ID and Machine ID cannot be blank.")
        }
        return dataSource.listHistoryLogsByMachine(tenantId, machineId, limit)
    }
}
