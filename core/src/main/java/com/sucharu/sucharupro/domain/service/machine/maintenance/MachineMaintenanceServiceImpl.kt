package com.sucharu.sucharupro.domain.service.machine.maintenance

import com.sucharu.sucharupro.domain.machine.MachineStatus
import com.sucharu.sucharupro.domain.machine.maintenance.MachineMaintenanceValidator
import com.sucharu.sucharupro.domain.machine.maintenance.MachineServiceHistoryLog
import com.sucharu.sucharupro.domain.machine.maintenance.MaintenanceRecord
import com.sucharu.sucharupro.domain.machine.maintenance.MaintenanceSchedule
import com.sucharu.sucharupro.domain.machine.maintenance.MaintenanceStatus
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.repository.machine.MachineRegistryRepository
import com.sucharu.sucharupro.domain.repository.machine.maintenance.MachineMaintenanceRepository
import java.util.UUID

/**
 * Domain Service implementation for Machine Maintenance orchestrations.
 */
class MachineMaintenanceServiceImpl(
    private val maintenanceRepository: MachineMaintenanceRepository,
    private val machineRegistryRepository: MachineRegistryRepository
) : MachineMaintenanceService {

    override suspend fun createSchedule(schedule: MaintenanceSchedule): DomainResult<MaintenanceSchedule> {
        val machineCheck = machineRegistryRepository.getMachineById(schedule.tenantId, schedule.machineId)
        if (machineCheck is DomainResult.Error) return machineCheck
        val machine = (machineCheck as? DomainResult.Success)?.data
            ?: return DomainResult.Error(message = "Machine '${schedule.machineId}' not found in registry.")

        if (machine.status == MachineStatus.DECOMMISSIONED) {
            return DomainResult.Error(message = "Cannot schedule maintenance for decommissioned machine '${schedule.machineId}'.")
        }

        return maintenanceRepository.saveSchedule(schedule)
    }

    override suspend fun listSchedules(
        tenantId: String,
        machineId: String,
        status: MaintenanceStatus?
    ): DomainResult<List<MaintenanceSchedule>> {
        return maintenanceRepository.listSchedulesByMachine(tenantId, machineId, status)
    }

    override suspend fun createRecord(record: MaintenanceRecord): DomainResult<MaintenanceRecord> {
        val machineCheck = machineRegistryRepository.getMachineById(record.tenantId, record.machineId)
        if (machineCheck is DomainResult.Error) return machineCheck
        val machine = (machineCheck as? DomainResult.Success)?.data
            ?: return DomainResult.Error(message = "Machine '${record.machineId}' not found in registry.")

        if (machine.status == MachineStatus.DECOMMISSIONED) {
            return DomainResult.Error(message = "Cannot create maintenance record for decommissioned machine '${record.machineId}'.")
        }

        val saveRes = maintenanceRepository.saveRecord(record)
        if (saveRes is DomainResult.Success) {
            // Record audit log
            maintenanceRepository.saveHistoryLog(
                MachineServiceHistoryLog(
                    historyId = "HIST-" + UUID.randomUUID().toString().take(8).uppercase(),
                    tenantId = record.tenantId,
                    machineId = record.machineId,
                    recordId = record.recordId,
                    actionType = "CREATED",
                    performedById = record.createdBy,
                    detailsJson = "{\"title\":\"${record.title}\",\"type\":\"${record.maintenanceType.name}\"}"
                )
            )
        }
        return saveRes
    }

    override suspend fun startMaintenance(
        tenantId: String,
        machineId: String,
        recordId: String,
        performedById: String?,
        performedByName: String?
    ): DomainResult<MaintenanceRecord> {
        val recordCheck = maintenanceRepository.getRecordById(tenantId, recordId)
        if (recordCheck is DomainResult.Error) return recordCheck
        val existing = (recordCheck as? DomainResult.Success)?.data
            ?: return DomainResult.Error(message = "Maintenance record '$recordId' not found.")

        if (existing.machineId != machineId) {
            return DomainResult.Error(message = "Machine ID mismatch for record '$recordId'.")
        }

        val transitionValidation = MachineMaintenanceValidator.validateStatusTransition(existing.status, MaintenanceStatus.IN_PROGRESS)
        if (transitionValidation is DomainResult.Error) return transitionValidation

        val now = System.currentTimeMillis()
        val updatedRecord = existing.copy(
            status = MaintenanceStatus.IN_PROGRESS,
            startedAt = existing.startedAt ?: now,
            performedById = performedById ?: existing.performedById,
            performedByName = performedByName ?: existing.performedByName,
            updatedAt = now,
            updatedBy = performedById ?: existing.updatedBy
        )

        val saveRes = maintenanceRepository.saveRecord(updatedRecord)
        if (saveRes is DomainResult.Success) {
            // Update machine registry status to MAINTENANCE
            machineRegistryRepository.updateMachineStatus(tenantId, machineId, MachineStatus.MAINTENANCE, performedById)

            // Save history log
            maintenanceRepository.saveHistoryLog(
                MachineServiceHistoryLog(
                    historyId = "HIST-" + UUID.randomUUID().toString().take(8).uppercase(),
                    tenantId = tenantId,
                    machineId = machineId,
                    recordId = recordId,
                    actionType = "STARTED",
                    performedById = performedById,
                    performedByName = performedByName,
                    detailsJson = "{\"status\":\"IN_PROGRESS\"}"
                )
            )
        }
        return saveRes
    }

    override suspend fun completeMaintenance(
        tenantId: String,
        machineId: String,
        recordId: String,
        resolutionSummary: String?,
        workPerformed: String?,
        completedBy: String?
    ): DomainResult<MaintenanceRecord> {
        val recordCheck = maintenanceRepository.getRecordById(tenantId, recordId)
        if (recordCheck is DomainResult.Error) return recordCheck
        val existing = (recordCheck as? DomainResult.Success)?.data
            ?: return DomainResult.Error(message = "Maintenance record '$recordId' not found.")

        if (existing.machineId != machineId) {
            return DomainResult.Error(message = "Machine ID mismatch for record '$recordId'.")
        }

        val transitionValidation = MachineMaintenanceValidator.validateStatusTransition(existing.status, MaintenanceStatus.COMPLETED)
        if (transitionValidation is DomainResult.Error) return transitionValidation

        val now = System.currentTimeMillis()
        val updatedRecord = existing.copy(
            status = MaintenanceStatus.COMPLETED,
            completedAt = now,
            resolutionSummary = resolutionSummary ?: existing.resolutionSummary,
            workPerformed = workPerformed ?: existing.workPerformed,
            updatedAt = now,
            updatedBy = completedBy ?: existing.updatedBy
        )

        val saveRes = maintenanceRepository.saveRecord(updatedRecord)
        if (saveRes is DomainResult.Success) {
            // Update machine registry status back to AVAILABLE
            machineRegistryRepository.updateMachineStatus(tenantId, machineId, MachineStatus.AVAILABLE, completedBy)

            // If this record was linked to a schedule, mark the schedule as COMPLETED
            if (!existing.scheduleId.isNullOrBlank()) {
                val schedCheck = maintenanceRepository.getScheduleById(tenantId, existing.scheduleId)
                if (schedCheck is DomainResult.Success && schedCheck.data != null) {
                    val sched = schedCheck.data
                    maintenanceRepository.saveSchedule(sched.copy(status = MaintenanceStatus.COMPLETED, updatedAt = now, updatedBy = completedBy))
                }
            }

            // Save history log
            maintenanceRepository.saveHistoryLog(
                MachineServiceHistoryLog(
                    historyId = "HIST-" + UUID.randomUUID().toString().take(8).uppercase(),
                    tenantId = tenantId,
                    machineId = machineId,
                    recordId = recordId,
                    actionType = "COMPLETED",
                    performedById = completedBy,
                    detailsJson = "{\"status\":\"COMPLETED\",\"resolution\":\"${resolutionSummary ?: ""}\"}"
                )
            )
        }
        return saveRes
    }

    override suspend fun cancelMaintenance(
        tenantId: String,
        machineId: String,
        recordId: String,
        reason: String?,
        cancelledBy: String?
    ): DomainResult<MaintenanceRecord> {
        val recordCheck = maintenanceRepository.getRecordById(tenantId, recordId)
        if (recordCheck is DomainResult.Error) return recordCheck
        val existing = (recordCheck as? DomainResult.Success)?.data
            ?: return DomainResult.Error(message = "Maintenance record '$recordId' not found.")

        val transitionValidation = MachineMaintenanceValidator.validateStatusTransition(existing.status, MaintenanceStatus.CANCELLED)
        if (transitionValidation is DomainResult.Error) return transitionValidation

        val now = System.currentTimeMillis()
        val updatedRecord = existing.copy(
            status = MaintenanceStatus.CANCELLED,
            notes = if (!reason.isNullOrBlank()) "Cancelled: $reason" else existing.notes,
            updatedAt = now,
            updatedBy = cancelledBy ?: existing.updatedBy
        )

        val saveRes = maintenanceRepository.saveRecord(updatedRecord)
        if (saveRes is DomainResult.Success) {
            // Return machine status to AVAILABLE if it was in MAINTENANCE
            val machineCheck = machineRegistryRepository.getMachineById(tenantId, machineId)
            if (machineCheck is DomainResult.Success && machineCheck.data?.status == MachineStatus.MAINTENANCE) {
                machineRegistryRepository.updateMachineStatus(tenantId, machineId, MachineStatus.AVAILABLE, cancelledBy)
            }

            maintenanceRepository.saveHistoryLog(
                MachineServiceHistoryLog(
                    historyId = "HIST-" + UUID.randomUUID().toString().take(8).uppercase(),
                    tenantId = tenantId,
                    machineId = machineId,
                    recordId = recordId,
                    actionType = "CANCELLED",
                    performedById = cancelledBy,
                    detailsJson = "{\"status\":\"CANCELLED\",\"reason\":\"${reason ?: ""}\"}"
                )
            )
        }
        return saveRes
    }

    override suspend fun getMaintenanceRecordDetails(
        tenantId: String,
        recordId: String
    ): DomainResult<MaintenanceRecord?> {
        return maintenanceRepository.getRecordById(tenantId, recordId)
    }

    override suspend fun listMaintenanceRecords(
        tenantId: String,
        machineId: String,
        status: MaintenanceStatus?,
        limit: Int
    ): DomainResult<List<MaintenanceRecord>> {
        return maintenanceRepository.listRecordsByMachine(tenantId, machineId, status, limit)
    }

    override suspend fun listServiceHistory(
        tenantId: String,
        machineId: String,
        limit: Int
    ): DomainResult<List<MachineServiceHistoryLog>> {
        return maintenanceRepository.listHistoryLogsByMachine(tenantId, machineId, limit)
    }
}
