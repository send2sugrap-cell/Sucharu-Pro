package com.sucharu.sucharupro.data.datasource.machine.maintenance

import com.sucharu.sucharupro.domain.machine.maintenance.MachineServiceHistoryLog
import com.sucharu.sucharupro.domain.machine.maintenance.MaintenanceRecord
import com.sucharu.sucharupro.domain.machine.maintenance.MaintenanceSchedule
import com.sucharu.sucharupro.domain.machine.maintenance.MaintenanceStatus
import com.sucharu.sucharupro.domain.model.common.DomainResult

/**
 * Data Source abstraction for Machine Maintenance persistence.
 */
interface MachineMaintenanceDataSource {
    suspend fun saveSchedule(schedule: MaintenanceSchedule): DomainResult<MaintenanceSchedule>
    suspend fun getScheduleById(tenantId: String, scheduleId: String): DomainResult<MaintenanceSchedule?>
    suspend fun listSchedulesByMachine(tenantId: String, machineId: String, status: MaintenanceStatus? = null): DomainResult<List<MaintenanceSchedule>>
    suspend fun saveRecord(record: MaintenanceRecord): DomainResult<MaintenanceRecord>
    suspend fun getRecordById(tenantId: String, recordId: String): DomainResult<MaintenanceRecord?>
    suspend fun listRecordsByMachine(tenantId: String, machineId: String, status: MaintenanceStatus? = null, limit: Int = 100): DomainResult<List<MaintenanceRecord>>
    suspend fun saveHistoryLog(log: MachineServiceHistoryLog): DomainResult<MachineServiceHistoryLog>
    suspend fun listHistoryLogsByRecord(tenantId: String, recordId: String): DomainResult<List<MachineServiceHistoryLog>>
    suspend fun listHistoryLogsByMachine(tenantId: String, machineId: String, limit: Int = 100): DomainResult<List<MachineServiceHistoryLog>>
}
