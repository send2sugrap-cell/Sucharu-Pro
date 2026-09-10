package com.sucharu.sucharupro.domain.machine

import com.sucharu.sucharupro.domain.model.common.DomainResult

/**
 * Domain Validator for Machine / Equipment Master Identity records.
 */
object MachineValidator {

    private val ASSET_CODE_REGEX = Regex("^[A-Za-z0-9_\\-]+$")

    fun validateMachine(machine: MachineEquipment): DomainResult<Unit> {
        if (machine.machineId.isBlank()) {
            return DomainResult.Error(message = "Machine ID cannot be blank.")
        }
        if (machine.tenantId.isBlank()) {
            return DomainResult.Error(message = "Tenant ID cannot be blank.")
        }
        if (machine.assetCode.isBlank()) {
            return DomainResult.Error(message = "Asset code cannot be blank.")
        }
        if (!ASSET_CODE_REGEX.matches(machine.assetCode)) {
            return DomainResult.Error(message = "Asset code must contain only letters, numbers, hyphens, and underscores.")
        }
        if (machine.name.isBlank() || machine.name.trim().length < 2) {
            return DomainResult.Error(message = "Machine name must be at least 2 characters long.")
        }
        return DomainResult.Success(Unit)
    }

    fun validateStatusTransition(currentStatus: MachineStatus, newStatus: MachineStatus): DomainResult<Unit> {
        if (currentStatus == MachineStatus.DECOMMISSIONED && newStatus != MachineStatus.DECOMMISSIONED) {
            return DomainResult.Error(
                message = "Decommissioned machines cannot transition directly to operational status."
            )
        }
        return DomainResult.Success(Unit)
    }
}
