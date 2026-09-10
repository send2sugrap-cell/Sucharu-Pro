package com.sucharu.sucharupro.domain.repository.machine

import com.sucharu.sucharupro.domain.machine.MachineEquipment
import com.sucharu.sucharupro.domain.machine.MachineStatus
import com.sucharu.sucharupro.domain.machine.MachineType
import com.sucharu.sucharupro.domain.model.common.DomainResult

/**
 * Domain Repository interface for Machine & Equipment Registry operations.
 */
interface MachineRegistryRepository {
    suspend fun saveMachine(machine: MachineEquipment): DomainResult<MachineEquipment>
    suspend fun getMachineById(tenantId: String, machineId: String): DomainResult<MachineEquipment?>
    suspend fun getMachineByAssetCode(tenantId: String, assetCode: String): DomainResult<MachineEquipment?>
    suspend fun listMachines(tenantId: String, type: MachineType? = null, status: MachineStatus? = null): DomainResult<List<MachineEquipment>>
    suspend fun updateMachineStatus(tenantId: String, machineId: String, status: MachineStatus, updatedBy: String? = null): DomainResult<MachineEquipment>
}
