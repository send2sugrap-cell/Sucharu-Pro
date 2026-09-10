package com.sucharu.sucharupro.data.datasource.machine

import com.sucharu.sucharupro.domain.machine.MachineEquipment
import com.sucharu.sucharupro.domain.machine.MachineOwnershipType
import com.sucharu.sucharupro.domain.machine.MachineStatus
import com.sucharu.sucharupro.domain.machine.MachineType
import com.sucharu.sucharupro.domain.model.common.DomainResult
import java.util.concurrent.ConcurrentHashMap

/**
 * In-memory thread-safe fake data source for Machine Registry testing & preview.
 */
class FakeMachineRegistryDataSource : MachineRegistryDataSource {

    private val machines = ConcurrentHashMap<String, MachineEquipment>()

    init {
        val initialMachines = listOf(
            MachineEquipment(
                machineId = "MAC-PRESS-001",
                tenantId = "TENANT-001",
                assetCode = "EQ-OFFSET-01",
                name = "Heidelberg Speedmaster XL 106",
                type = MachineType.PRINTING_PRESS,
                manufacturer = "Heidelberg",
                model = "XL 106 6-Color + Coater",
                serialNumber = "HSM-106-9921",
                description = "High-speed 6-color offset press with inline UV coating",
                status = MachineStatus.AVAILABLE,
                ownershipType = MachineOwnershipType.COMPANY_OWNED,
                locationReference = "Floor A - Offset Bay 1",
                department = "PRESS"
            ),
            MachineEquipment(
                machineId = "MAC-DIGITAL-002",
                tenantId = "TENANT-001",
                assetCode = "EQ-DIGITAL-02",
                name = "HP Indigo 12000 Digital Press",
                type = MachineType.DIGITAL_PRINTER,
                manufacturer = "HP",
                model = "Indigo 12000 B2",
                serialNumber = "HPI-12K-3341",
                description = "B2 format digital press for short-run commercial jobs",
                status = MachineStatus.IN_USE,
                ownershipType = MachineOwnershipType.LEASED,
                locationReference = "Floor B - Digital Printing Center",
                department = "DIGITAL_PRINTING"
            ),
            MachineEquipment(
                machineId = "MAC-CTP-003",
                tenantId = "TENANT-001",
                assetCode = "EQ-CTP-01",
                name = "Screen PlateRite 8600 CTP",
                type = MachineType.CTP,
                manufacturer = "Screen",
                model = "PlateRite 8600 S",
                serialNumber = "SCR-8600-1102",
                description = "8-page thermal CTP plate setter with inline processor",
                status = MachineStatus.AVAILABLE,
                ownershipType = MachineOwnershipType.COMPANY_OWNED,
                locationReference = "Floor A - Prepress Room",
                department = "PREPRESS"
            )
        )
        initialMachines.forEach { saveMachineDirect(it) }
    }

    private fun saveMachineDirect(machine: MachineEquipment) {
        machines["${machine.tenantId}:${machine.machineId}"] = machine
    }

    override suspend fun saveMachine(machine: MachineEquipment): DomainResult<MachineEquipment> {
        val key = "${machine.tenantId}:${machine.machineId}"
        machines[key] = machine
        return DomainResult.Success(machine)
    }

    override suspend fun getMachineById(tenantId: String, machineId: String): DomainResult<MachineEquipment?> {
        val key = "$tenantId:$machineId"
        return DomainResult.Success(machines[key])
    }

    override suspend fun getMachineByAssetCode(tenantId: String, assetCode: String): DomainResult<MachineEquipment?> {
        val found = machines.values.firstOrNull { it.tenantId == tenantId && it.assetCode.equals(assetCode, ignoreCase = true) }
        return DomainResult.Success(found)
    }

    override suspend fun listMachines(
        tenantId: String,
        type: MachineType?,
        status: MachineStatus?
    ): DomainResult<List<MachineEquipment>> {
        val filtered = machines.values.filter { m ->
            m.tenantId == tenantId &&
            (type == null || m.type == type) &&
            (status == null || m.status == status)
        }.sortedBy { it.name }
        return DomainResult.Success(filtered)
    }

    override suspend fun updateMachineStatus(
        tenantId: String,
        machineId: String,
        status: MachineStatus,
        updatedBy: String?
    ): DomainResult<MachineEquipment> {
        val key = "$tenantId:$machineId"
        val existing = machines[key]
            ?: return DomainResult.Error(message = "Machine not found: $machineId")
        val updated = existing.copy(
            status = status,
            updatedAt = System.currentTimeMillis(),
            updatedBy = updatedBy ?: existing.updatedBy
        )
        machines[key] = updated
        return DomainResult.Success(updated)
    }
}
