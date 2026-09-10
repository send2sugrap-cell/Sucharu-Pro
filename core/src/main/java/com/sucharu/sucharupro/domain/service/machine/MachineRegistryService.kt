package com.sucharu.sucharupro.domain.service.machine

import com.sucharu.sucharupro.domain.machine.MachineEquipment
import com.sucharu.sucharupro.domain.machine.MachineStatus
import com.sucharu.sucharupro.domain.machine.MachineType
import com.sucharu.sucharupro.domain.model.common.DomainResult

/**
 * Domain Service interface for Machine & Equipment Registry business orchestrations.
 */
interface MachineRegistryService {
    suspend fun registerMachine(machine: MachineEquipment): DomainResult<MachineEquipment>
    suspend fun updateMachine(machine: MachineEquipment): DomainResult<MachineEquipment>
    suspend fun getMachineDetails(tenantId: String, machineId: String): DomainResult<MachineEquipment?>
    suspend fun getMachineByAssetCode(tenantId: String, assetCode: String): DomainResult<MachineEquipment?>
    suspend fun listMachines(tenantId: String, type: MachineType? = null, status: MachineStatus? = null): DomainResult<List<MachineEquipment>>
    suspend fun changeMachineStatus(tenantId: String, machineId: String, newStatus: MachineStatus, updatedBy: String? = null): DomainResult<MachineEquipment>
}
