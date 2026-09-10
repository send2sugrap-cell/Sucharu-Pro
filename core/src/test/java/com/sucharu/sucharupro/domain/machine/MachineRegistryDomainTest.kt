package com.sucharu.sucharupro.domain.machine

import com.sucharu.sucharupro.data.datasource.machine.FakeMachineRegistryDataSource
import com.sucharu.sucharupro.data.repository.machine.MachineRegistryRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.service.machine.MachineRegistryServiceImpl
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class MachineRegistryDomainTest {

    private lateinit var dataSource: FakeMachineRegistryDataSource
    private lateinit var repository: MachineRegistryRepositoryImpl
    private lateinit var service: MachineRegistryServiceImpl

    @Before
    fun setUp() {
        dataSource = FakeMachineRegistryDataSource()
        repository = MachineRegistryRepositoryImpl(dataSource)
        service = MachineRegistryServiceImpl(repository)
    }

    @Test
    fun test01_registerMachine_success() = runBlocking {
        val newMachine = MachineEquipment(
            machineId = "MAC-TEST-100",
            tenantId = "TENANT-001",
            assetCode = "EQ-CUT-01",
            name = "Polar 115 High-Speed Cutter",
            type = MachineType.CUTTING,
            manufacturer = "Polar Mohr",
            model = "N 115 Plus",
            serialNumber = "PLR-115-8820",
            status = MachineStatus.AVAILABLE,
            ownershipType = MachineOwnershipType.COMPANY_OWNED,
            locationReference = "Floor B - Finishing Dept",
            department = "FINISHING"
        )

        val result = service.registerMachine(newMachine)
        assertTrue(result is DomainResult.Success)
        val registered = (result as DomainResult.Success).data
        assertEquals("MAC-TEST-100", registered.machineId)
        assertEquals("EQ-CUT-01", registered.assetCode)
        assertEquals(MachineType.CUTTING, registered.type)
    }

    @Test
    fun test02_registerMachine_duplicateAssetCode_fails() = runBlocking {
        val machine1 = MachineEquipment(
            machineId = "MAC-101",
            tenantId = "TENANT-001",
            assetCode = "EQ-DUP-01",
            name = "Machine One",
            type = MachineType.FOLDING
        )
        val machine2 = MachineEquipment(
            machineId = "MAC-102",
            tenantId = "TENANT-001",
            assetCode = "EQ-DUP-01",
            name = "Machine Two",
            type = MachineType.FOLDING
        )

        val res1 = service.registerMachine(machine1)
        assertTrue(res1 is DomainResult.Success)

        val res2 = service.registerMachine(machine2)
        assertTrue(res2 is DomainResult.Error)
        val err = res2 as DomainResult.Error
        assertTrue(err.message.contains("already exists"))
    }

    @Test
    fun test03_validateMachine_blankName_fails() = runBlocking {
        val invalidMachine = MachineEquipment(
            machineId = "MAC-103",
            tenantId = "TENANT-001",
            assetCode = "EQ-INV-01",
            name = " ",
            type = MachineType.LAMINATION
        )

        val res = service.registerMachine(invalidMachine)
        assertTrue(res is DomainResult.Error)
        assertTrue((res as DomainResult.Error).message.contains("at least 2 characters"))
    }

    @Test
    fun test04_changeMachineStatus_success() = runBlocking {
        val res = service.changeMachineStatus("TENANT-001", "MAC-PRESS-001", MachineStatus.MAINTENANCE, "USR-ADMIN")
        assertTrue(res is DomainResult.Success)
        val updated = (res as DomainResult.Success).data
        assertEquals(MachineStatus.MAINTENANCE, updated.status)
        assertEquals("USR-ADMIN", updated.updatedBy)
    }

    @Test
    fun test05_decommissionedStatusTransition_ruleEnforced() = runBlocking {
        // Change to decommissioned
        service.changeMachineStatus("TENANT-001", "MAC-PRESS-001", MachineStatus.DECOMMISSIONED, "USR-ADMIN")

        // Transition directly back to IN_USE should be rejected
        val res = service.changeMachineStatus("TENANT-001", "MAC-PRESS-001", MachineStatus.IN_USE, "USR-ADMIN")
        assertTrue(res is DomainResult.Error)
        assertTrue((res as DomainResult.Error).message.contains("Decommissioned machines cannot transition"))
    }
}
