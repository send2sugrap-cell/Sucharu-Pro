package com.sucharu.sucharupro.domain.machine.events

import com.sucharu.sucharupro.domain.model.common.DomainResult

/**
 * Domain Validator for Machine Fault Events and Downtime Events.
 */
object MachineEventValidator {

    fun validateFaultEvent(event: MachineFaultEvent): DomainResult<Unit> {
        if (event.faultEventId.isBlank()) {
            return DomainResult.Error(message = "Fault event ID cannot be blank.")
        }
        if (event.tenantId.isBlank()) {
            return DomainResult.Error(message = "Tenant ID cannot be blank.")
        }
        if (event.machineId.isBlank()) {
            return DomainResult.Error(message = "Machine ID cannot be blank.")
        }
        if (event.description.isBlank() || event.description.trim().length < 2) {
            return DomainResult.Error(message = "Description must be at least 2 characters long.")
        }
        if (event.resolvedAt != null && event.occurredAt > 0 && event.resolvedAt < event.occurredAt) {
            return DomainResult.Error(message = "Resolution timestamp cannot be earlier than occurrence timestamp.")
        }
        return DomainResult.Success(Unit)
    }

    fun validateDowntimeEvent(event: MachineDowntimeEvent): DomainResult<Unit> {
        if (event.downtimeId.isBlank()) {
            return DomainResult.Error(message = "Downtime ID cannot be blank.")
        }
        if (event.tenantId.isBlank()) {
            return DomainResult.Error(message = "Tenant ID cannot be blank.")
        }
        if (event.machineId.isBlank()) {
            return DomainResult.Error(message = "Machine ID cannot be blank.")
        }
        if (event.startedAt <= 0) {
            return DomainResult.Error(message = "Start timestamp must be a valid positive timestamp.")
        }
        if (event.endedAt != null && event.endedAt < event.startedAt) {
            return DomainResult.Error(message = "End timestamp cannot be earlier than start timestamp.")
        }
        return DomainResult.Success(Unit)
    }

    fun validateFaultStatusTransition(currentStatus: FaultStatus, newStatus: FaultStatus): DomainResult<Unit> {
        if (currentStatus.isTerminal) {
            return DomainResult.Error(message = "Terminal fault status '${currentStatus.name}' cannot be modified or transitioned.")
        }
        return DomainResult.Success(Unit)
    }
}
