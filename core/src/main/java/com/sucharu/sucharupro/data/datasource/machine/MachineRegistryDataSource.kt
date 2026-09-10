package com.sucharu.sucharupro.data.datasource.machine

import com.sucharu.sucharupro.domain.machine.MachineEquipment
import com.sucharu.sucharupro.domain.machine.MachineStatus
import com.sucharu.sucharupro.domain.machine.MachineType
import com.sucharu.sucharupro.domain.model.common.DomainResult

/**
 * Data Source abstraction for Machine Registry persistence.
 */
interface MachineRegistryDataSource {
    suspend fun saveMachine(machine: MachineEquipment): DomainResult<MachineEquipment>
    suspend fun getMachineById(tenantId: String, machineId: String): DomainResult<MachineEquipment?>
    suspend fun getMachineByAssetCode(tenantId: String, assetCode: String): DomainResult<MachineEquipment?>
    suspend fun listMachines(tenantId: String, type: MachineType? = null, status: MachineStatus? = null): DomainResult<List<MachineEquipment>>
    suspend fun updateMachineStatus(tenantId: String, machineId: String, status: MachineStatus, updatedBy: String? = null): DomainResult<MachineEquipment>
}
