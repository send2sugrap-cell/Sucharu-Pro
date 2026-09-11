package com.sucharu.sucharupro.data.machine.events

import com.sucharu.sucharupro.data.datasource.machine.FakeMachineRegistryDataSource
import com.sucharu.sucharupro.data.datasource.machine.events.FakeMachineEventDataSource
import com.sucharu.sucharupro.data.repository.machine.MachineRegistryRepositoryImpl
import com.sucharu.sucharupro.data.repository.machine.events.MachineEventRepositoryImpl
import com.sucharu.sucharupro.domain.machine.MachineEquipment
import com.sucharu.sucharupro.domain.machine.MachineStatus
import com.sucharu.sucharupro.domain.machine.MachineType
import com.sucharu.sucharupro.domain.machine.events.MachineFaultEvent
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.service.machine.events.MachineFaultEventServiceImpl
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class PostgresMachineEventSecurityTest {

    private lateinit var machineDataSource: FakeMachineRegistryDataSource
    private lateinit var eventDataSource: FakeMachineEventDataSource
    private lateinit var faultService: MachineFaultEventServiceImpl

    @Before
    fun setUp() = runBlocking {
        machineDataSource = FakeMachineRegistryDataSource()
        eventDataSource = FakeMachineEventDataSource()
        val machineRepo = MachineRegistryRepositoryImpl(machineDataSource)
        val eventRepo = MachineEventRepositoryImpl(eventDataSource)
        faultService = MachineFaultEventServiceImpl(eventRepo, machineRepo)

        val machineA = MachineEquipment(
            machineId = "MAC-TENANT-A-01",
            tenantId = "TENANT-A",
            assetCode = "EQ-A1",
            name = "Tenant A Machine",
            type = MachineType.PRINTING_PRESS,
            status = MachineStatus.AVAILABLE
        )
        machineRepo.saveMachine(machineA)
        Unit
    }

    @Test
    fun test01_tenantIsolation_crossTenantFaultRecord_fails() = runBlocking {
        val faultB = MachineFaultEvent(
            faultEventId = "FLT-B-01",
            tenantId = "TENANT-B",
            machineId = "MAC-TENANT-A-01", // Attempting Tenant B fault on Tenant A's machine
            faultType = "ELECTRICAL",
            description = "Unsanctioned cross-tenant fault event"
        )

        val result = faultService.recordFault(faultB)
        assertTrue(result is DomainResult.Error)
        val err = result as DomainResult.Error
        assertTrue(err.message.contains("not found in registry"))
    }
}
