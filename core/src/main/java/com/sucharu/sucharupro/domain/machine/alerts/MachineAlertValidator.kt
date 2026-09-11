package com.sucharu.sucharupro.domain.machine.alerts

import com.sucharu.sucharupro.domain.model.common.DomainResult

/**
 * Domain Validator for Machine Operational Alerts.
 */
object MachineAlertValidator {

    fun validateAlert(alert: MachineOperationalAlert): DomainResult<Unit> {
        if (alert.alertId.isBlank()) {
            return DomainResult.Error(message = "Alert ID cannot be blank.")
        }
        if (alert.tenantId.isBlank()) {
            return DomainResult.Error(message = "Tenant ID cannot be blank.")
        }
        if (alert.machineId.isBlank()) {
            return DomainResult.Error(message = "Machine ID cannot be blank.")
        }
        if (alert.title.isBlank() || alert.title.trim().length < 2) {
            return DomainResult.Error(message = "Title must be at least 2 characters long.")
        }
        if (alert.description.isBlank() || alert.description.trim().length < 2) {
            return DomainResult.Error(message = "Description must be at least 2 characters long.")
        }
        return DomainResult.Success(Unit)
    }

    fun validateStatusTransition(currentStatus: MachineAlertStatus, newStatus: MachineAlertStatus): DomainResult<Unit> {
        if (currentStatus.isTerminal) {
            return DomainResult.Error(message = "Terminal alert status '${currentStatus.name}' cannot be modified or transitioned.")
        }
        return DomainResult.Success(Unit)
    }
}
