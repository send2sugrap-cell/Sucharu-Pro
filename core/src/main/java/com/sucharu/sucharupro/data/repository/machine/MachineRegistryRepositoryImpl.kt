package com.sucharu.sucharupro.data.repository.machine

import com.sucharu.sucharupro.data.datasource.machine.MachineRegistryDataSource
import com.sucharu.sucharupro.domain.machine.MachineEquipment
import com.sucharu.sucharupro.domain.machine.MachineStatus
import com.sucharu.sucharupro.domain.machine.MachineType
import com.sucharu.sucharupro.domain.machine.MachineValidator
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.repository.machine.MachineRegistryRepository

/**
 * Production Repository implementation delegating to MachineRegistryDataSource with validation.
 */
class MachineRegistryRepositoryImpl(
    private val dataSource: MachineRegistryDataSource
) : MachineRegistryRepository {

    override suspend fun saveMachine(machine: MachineEquipment): DomainResult<MachineEquipment> {
        val validation = MachineValidator.validateMachine(machine)
        if (validation is DomainResult.Error) {
            return DomainResult.Error(message = validation.message)
        }
        return dataSource.saveMachine(machine)
    }

    override suspend fun getMachineById(tenantId: String, machineId: String): DomainResult<MachineEquipment?> {
        if (tenantId.isBlank() || machineId.isBlank()) {
            return DomainResult.Error(message = "Tenant ID and Machine ID must not be blank.")
        }
        return dataSource.getMachineById(tenantId, machineId)
    }

    override suspend fun getMachineByAssetCode(tenantId: String, assetCode: String): DomainResult<MachineEquipment?> {
        if (tenantId.isBlank() || assetCode.isBlank()) {
            return DomainResult.Error(message = "Tenant ID and Asset Code must not be blank.")
        }
        return dataSource.getMachineByAssetCode(tenantId, assetCode)
    }

    override suspend fun listMachines(
        tenantId: String,
        type: MachineType?,
        status: MachineStatus?
    ): DomainResult<List<MachineEquipment>> {
        if (tenantId.isBlank()) {
            return DomainResult.Error(message = "Tenant ID must not be blank.")
        }
        return dataSource.listMachines(tenantId, type, status)
    }

    override suspend fun updateMachineStatus(
        tenantId: String,
        machineId: String,
        status: MachineStatus,
        updatedBy: String?
    ): DomainResult<MachineEquipment> {
        if (tenantId.isBlank() || machineId.isBlank()) {
            return DomainResult.Error(message = "Tenant ID and Machine ID must not be blank.")
        }
        val existingResult = dataSource.getMachineById(tenantId, machineId)
        if (existingResult is DomainResult.Error) {
            return existingResult
        }
        val existing = (existingResult as? DomainResult.Success)?.data
            ?: return DomainResult.Error(message = "Machine not found: $machineId")

        val transitionValidation = MachineValidator.validateStatusTransition(existing.status, status)
        if (transitionValidation is DomainResult.Error) {
            return DomainResult.Error(message = transitionValidation.message)
        }

        return dataSource.updateMachineStatus(tenantId, machineId, status, updatedBy)
    }
}
