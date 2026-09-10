package com.sucharu.sucharupro.data.machine.health

import com.sucharu.sucharupro.data.datasource.machine.FakeMachineRegistryDataSource
import com.sucharu.sucharupro.data.datasource.machine.telemetry.FakeMachineTelemetryDataSource
import com.sucharu.sucharupro.data.repository.machine.MachineRegistryRepositoryImpl
import com.sucharu.sucharupro.data.repository.machine.telemetry.MachineTelemetryRepositoryImpl
import com.sucharu.sucharupro.domain.machine.MachineEquipment
import com.sucharu.sucharupro.domain.machine.MachineStatus
import com.sucharu.sucharupro.domain.machine.MachineType
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.service.machine.health.MachineStatusMonitoringServiceImpl
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class PostgresMachineStatusSecurityTest {

    private lateinit var machineDataSource: FakeMachineRegistryDataSource
    private lateinit var telemetryDataSource: FakeMachineTelemetryDataSource
    private lateinit var service: MachineStatusMonitoringServiceImpl

    @Before
    fun setUp() = runBlocking {
        machineDataSource = FakeMachineRegistryDataSource()
        telemetryDataSource = FakeMachineTelemetryDataSource()
        val machineRepo = MachineRegistryRepositoryImpl(machineDataSource)
        val telemetryRepo = MachineTelemetryRepositoryImpl(telemetryDataSource)
        service = MachineStatusMonitoringServiceImpl(machineRepo, telemetryRepo)

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
    fun test01_tenantIsolation_crossTenantHealthQuery_fails() = runBlocking {
        // Query health of Tenant A machine using Tenant B ID
        val result = service.evaluateMachineHealth("TENANT-B", "MAC-TENANT-A-01")
        assertTrue(result is DomainResult.Error)
        val err = result as DomainResult.Error
        assertTrue(err.message.contains("not found in registry"))
    }
}
