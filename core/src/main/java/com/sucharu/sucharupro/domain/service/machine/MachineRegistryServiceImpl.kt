package com.sucharu.sucharupro.domain.service.machine

import com.sucharu.sucharupro.domain.machine.MachineEquipment
import com.sucharu.sucharupro.domain.machine.MachineStatus
import com.sucharu.sucharupro.domain.machine.MachineType
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.repository.machine.MachineRegistryRepository

/**
 * Domain Service implementation for Machine & Equipment Registry orchestrations.
 */
class MachineRegistryServiceImpl(
    private val repository: MachineRegistryRepository
) : MachineRegistryService {

    override suspend fun registerMachine(machine: MachineEquipment): DomainResult<MachineEquipment> {
        val existingResult = repository.getMachineByAssetCode(machine.tenantId, machine.assetCode)
        if (existingResult is DomainResult.Success && existingResult.data != null) {
            val existing = existingResult.data
            if (existing.machineId != machine.machineId) {
                return DomainResult.Error(
                    message = "Machine asset code '${machine.assetCode}' already exists for tenant '${machine.tenantId}'."
                )
            }
        }
        return repository.saveMachine(machine)
    }

    override suspend fun updateMachine(machine: MachineEquipment): DomainResult<MachineEquipment> {
        val existingResult = repository.getMachineById(machine.tenantId, machine.machineId)
        if (existingResult is DomainResult.Error) {
            return existingResult
        }
        if ((existingResult as? DomainResult.Success)?.data == null) {
            return DomainResult.Error(message = "Machine with ID '${machine.machineId}' not found.")
        }
        val assetCheck = repository.getMachineByAssetCode(machine.tenantId, machine.assetCode)
        if (assetCheck is DomainResult.Success && assetCheck.data != null) {
            if (assetCheck.data.machineId != machine.machineId) {
                return DomainResult.Error(
                    message = "Cannot update: asset code '${machine.assetCode}' is used by another machine."
                )
            }
        }
        val updated = machine.copy(updatedAt = System.currentTimeMillis())
        return repository.saveMachine(updated)
    }

    override suspend fun getMachineDetails(tenantId: String, machineId: String): DomainResult<MachineEquipment?> {
        return repository.getMachineById(tenantId, machineId)
    }

    override suspend fun getMachineByAssetCode(tenantId: String, assetCode: String): DomainResult<MachineEquipment?> {
        return repository.getMachineByAssetCode(tenantId, assetCode)
    }

    override suspend fun listMachines(
        tenantId: String,
        type: MachineType?,
        status: MachineStatus?
    ): DomainResult<List<MachineEquipment>> {
        return repository.listMachines(tenantId, type, status)
    }

    override suspend fun changeMachineStatus(
        tenantId: String,
        machineId: String,
        newStatus: MachineStatus,
        updatedBy: String?
    ): DomainResult<MachineEquipment> {
        return repository.updateMachineStatus(tenantId, machineId, newStatus, updatedBy)
    }
}
