package com.sucharu.sucharupro.data.machine.oee

import com.sucharu.sucharupro.data.datasource.machine.FakeMachineRegistryDataSource
import com.sucharu.sucharupro.data.datasource.machine.oee.FakeMachineOeeDataSource
import com.sucharu.sucharupro.data.repository.machine.MachineRegistryRepositoryImpl
import com.sucharu.sucharupro.data.repository.machine.oee.MachineOeeRepositoryImpl
import com.sucharu.sucharupro.domain.machine.MachineEquipment
import com.sucharu.sucharupro.domain.machine.MachineStatus
import com.sucharu.sucharupro.domain.machine.MachineType
import com.sucharu.sucharupro.domain.machine.oee.MachineOeeCalculationRequest
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.service.machine.oee.MachineOeeServiceImpl
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class PostgresMachineOeeSecurityTest {

    private lateinit var machineDataSource: FakeMachineRegistryDataSource
    private lateinit var oeeDataSource: FakeMachineOeeDataSource
    private lateinit var oeeService: MachineOeeServiceImpl

    @Before
    fun setUp() = runBlocking {
        machineDataSource = FakeMachineRegistryDataSource()
        oeeDataSource = FakeMachineOeeDataSource()
        val machineRepo = MachineRegistryRepositoryImpl(machineDataSource)
        val oeeRepo = MachineOeeRepositoryImpl(oeeDataSource)
        oeeService = MachineOeeServiceImpl(oeeRepo, machineRepo)

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
    fun test01_tenantIsolation_crossTenantOeeCalculation_fails() = runBlocking {
        val reqB = MachineOeeCalculationRequest(
            tenantId = "TENANT-B",
            machineId = "MAC-TENANT-A-01", // Attempting Tenant B OEE on Tenant A's machine
            periodStart = System.currentTimeMillis() - 3600000L,
            periodEnd = System.currentTimeMillis()
        )

        val result = oeeService.calculateAndSaveOee(reqB)
        assertTrue(result is DomainResult.Error)
        val err = result as DomainResult.Error
        assertTrue(err.message.contains("not found in registry"))
    }
}
