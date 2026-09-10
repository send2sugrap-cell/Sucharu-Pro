package com.sucharu.sucharupro.domain.service.machine.maintenance

import com.sucharu.sucharupro.domain.machine.maintenance.MachineServiceHistoryLog
import com.sucharu.sucharupro.domain.machine.maintenance.MaintenanceRecord
import com.sucharu.sucharupro.domain.machine.maintenance.MaintenanceSchedule
import com.sucharu.sucharupro.domain.machine.maintenance.MaintenanceStatus
import com.sucharu.sucharupro.domain.model.common.DomainResult

/**
 * Domain Service interface for Machine Maintenance orchestrations.
 */
interface MachineMaintenanceService {
    suspend fun createSchedule(schedule: MaintenanceSchedule): DomainResult<MaintenanceSchedule>
    suspend fun listSchedules(tenantId: String, machineId: String, status: MaintenanceStatus? = null): DomainResult<List<MaintenanceSchedule>>
    suspend fun createRecord(record: MaintenanceRecord): DomainResult<MaintenanceRecord>
    suspend fun startMaintenance(tenantId: String, machineId: String, recordId: String, performedById: String?, performedByName: String?): DomainResult<MaintenanceRecord>
    suspend fun completeMaintenance(tenantId: String, machineId: String, recordId: String, resolutionSummary: String?, workPerformed: String?, completedBy: String?): DomainResult<MaintenanceRecord>
    suspend fun cancelMaintenance(tenantId: String, machineId: String, recordId: String, reason: String?, cancelledBy: String?): DomainResult<MaintenanceRecord>
    suspend fun getMaintenanceRecordDetails(tenantId: String, recordId: String): DomainResult<MaintenanceRecord?>
    suspend fun listMaintenanceRecords(tenantId: String, machineId: String, status: MaintenanceStatus? = null, limit: Int = 100): DomainResult<List<MaintenanceRecord>>
    suspend fun listServiceHistory(tenantId: String, machineId: String, limit: Int = 100): DomainResult<List<MachineServiceHistoryLog>>
}
