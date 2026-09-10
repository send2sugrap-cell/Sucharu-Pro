package com.sucharu.sucharupro.data.machine.maintenance

import com.sucharu.sucharupro.data.datasource.machine.FakeMachineRegistryDataSource
import com.sucharu.sucharupro.data.datasource.machine.maintenance.FakeMachineMaintenanceDataSource
import com.sucharu.sucharupro.data.repository.machine.MachineRegistryRepositoryImpl
import com.sucharu.sucharupro.data.repository.machine.maintenance.MachineMaintenanceRepositoryImpl
import com.sucharu.sucharupro.domain.machine.MachineEquipment
import com.sucharu.sucharupro.domain.machine.MachineStatus
import com.sucharu.sucharupro.domain.machine.MachineType
import com.sucharu.sucharupro.domain.machine.maintenance.MaintenanceRecord
import com.sucharu.sucharupro.domain.machine.maintenance.MaintenanceType
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.service.machine.maintenance.MachineMaintenanceServiceImpl
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class PostgresMachineMaintenanceSecurityTest {

    private lateinit var machineDataSource: FakeMachineRegistryDataSource
    private lateinit var maintenanceDataSource: FakeMachineMaintenanceDataSource
    private lateinit var service: MachineMaintenanceServiceImpl

    @Before
    fun setUp() = runBlocking {
        machineDataSource = FakeMachineRegistryDataSource()
        maintenanceDataSource = FakeMachineMaintenanceDataSource()
        val machineRepo = MachineRegistryRepositoryImpl(machineDataSource)
        val maintenanceRepo = MachineMaintenanceRepositoryImpl(maintenanceDataSource)
        service = MachineMaintenanceServiceImpl(maintenanceRepo, machineRepo)

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
    fun test01_tenantIsolation_crossTenantMaintenanceRecord_fails() = runBlocking {
        val recordB = MaintenanceRecord(
            recordId = "REC-B-01",
            tenantId = "TENANT-B",
            machineId = "MAC-TENANT-A-01", // Attempting Tenant B maintenance on Tenant A's machine
            title = "Unsanctioned Maintenance",
            maintenanceType = MaintenanceType.CORRECTIVE
        )

        val result = service.createRecord(recordB)
        assertTrue(result is DomainResult.Error)
        val err = result as DomainResult.Error
        assertTrue(err.message.contains("not found in registry"))
    }
}
