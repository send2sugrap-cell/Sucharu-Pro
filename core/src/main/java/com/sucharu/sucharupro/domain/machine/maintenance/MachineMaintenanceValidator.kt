package com.sucharu.sucharupro.domain.machine.maintenance

import com.sucharu.sucharupro.domain.model.common.DomainResult

/**
 * Domain Validator for Machine Maintenance Schedules and Service Records.
 */
object MachineMaintenanceValidator {

    fun validateSchedule(schedule: MaintenanceSchedule): DomainResult<Unit> {
        if (schedule.scheduleId.isBlank()) {
            return DomainResult.Error(message = "Schedule ID cannot be blank.")
        }
        if (schedule.tenantId.isBlank()) {
            return DomainResult.Error(message = "Tenant ID cannot be blank.")
        }
        if (schedule.machineId.isBlank()) {
            return DomainResult.Error(message = "Machine ID cannot be blank.")
        }
        if (schedule.title.isBlank() || schedule.title.trim().length < 2) {
            return DomainResult.Error(message = "Title must be at least 2 characters long.")
        }
        if (schedule.plannedDate <= 0) {
            return DomainResult.Error(message = "Planned date must be a valid positive timestamp.")
        }
        return DomainResult.Success(Unit)
    }

    fun validateRecord(record: MaintenanceRecord): DomainResult<Unit> {
        if (record.recordId.isBlank()) {
            return DomainResult.Error(message = "Record ID cannot be blank.")
        }
        if (record.tenantId.isBlank()) {
            return DomainResult.Error(message = "Tenant ID cannot be blank.")
        }
        if (record.machineId.isBlank()) {
            return DomainResult.Error(message = "Machine ID cannot be blank.")
        }
        if (record.title.isBlank() || record.title.trim().length < 2) {
            return DomainResult.Error(message = "Title must be at least 2 characters long.")
        }
        if (record.completedAt != null && record.startedAt != null && record.completedAt < record.startedAt) {
            return DomainResult.Error(message = "Completion timestamp cannot be earlier than start timestamp.")
        }
        return DomainResult.Success(Unit)
    }

    fun validateStatusTransition(currentStatus: MaintenanceStatus, newStatus: MaintenanceStatus): DomainResult<Unit> {
        if (currentStatus.isTerminal) {
            return DomainResult.Error(message = "Terminal maintenance status '${currentStatus.name}' cannot be modified or transitioned.")
        }
        return DomainResult.Success(Unit)
    }
}
