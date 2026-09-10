package com.sucharu.sucharupro.data.machine

import com.sucharu.sucharupro.data.datasource.machine.FakeMachineRegistryDataSource
import com.sucharu.sucharupro.domain.machine.MachineEquipment
import com.sucharu.sucharupro.domain.machine.MachineStatus
import com.sucharu.sucharupro.domain.machine.MachineType
import com.sucharu.sucharupro.domain.model.common.DomainResult
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class PostgresMachineRegistrySecurityTest {

    private lateinit var dataSource: FakeMachineRegistryDataSource

    @Before
    fun setUp() {
        dataSource = FakeMachineRegistryDataSource()
    }

    @Test
    fun test01_tenantIsolation_tenantA_cannotAccess_tenantB_machines() = runBlocking {
        val machineA = MachineEquipment(
            machineId = "MAC-TENANT-A-01",
            tenantId = "TENANT-A",
            assetCode = "EQ-A1",
            name = "Tenant A Press",
            type = MachineType.PRINTING_PRESS
        )
        val machineB = MachineEquipment(
            machineId = "MAC-TENANT-B-01",
            tenantId = "TENANT-B",
            assetCode = "EQ-B1",
            name = "Tenant B Press",
            type = MachineType.PRINTING_PRESS
        )

        dataSource.saveMachine(machineA)
        dataSource.saveMachine(machineB)

        // Query Tenant A list
        val listA = dataSource.listMachines("TENANT-A")
        assertTrue(listA is DomainResult.Success)
        val dataA = (listA as DomainResult.Success).data
        assertEquals(1, dataA.size)
        assertEquals("MAC-TENANT-A-01", dataA[0].machineId)

        // Query Tenant B details using Tenant A ID -> should return null
        val crossTenantResult = dataSource.getMachineById("TENANT-A", "MAC-TENANT-B-01")
        assertTrue(crossTenantResult is DomainResult.Success)
        assertNull((crossTenantResult as DomainResult.Success).data)
    }
}
