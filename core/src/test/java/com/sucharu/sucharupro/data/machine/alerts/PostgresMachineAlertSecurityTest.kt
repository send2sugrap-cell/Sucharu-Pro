package com.sucharu.sucharupro.data.machine.alerts

import com.sucharu.sucharupro.data.datasource.machine.FakeMachineRegistryDataSource
import com.sucharu.sucharupro.data.datasource.machine.alerts.FakeMachineAlertDataSource
import com.sucharu.sucharupro.data.repository.machine.MachineRegistryRepositoryImpl
import com.sucharu.sucharupro.data.repository.machine.alerts.MachineAlertRepositoryImpl
import com.sucharu.sucharupro.domain.machine.MachineEquipment
import com.sucharu.sucharupro.domain.machine.MachineStatus
import com.sucharu.sucharupro.domain.machine.MachineType
import com.sucharu.sucharupro.domain.machine.alerts.MachineAlertType
import com.sucharu.sucharupro.domain.machine.alerts.MachineOperationalAlert
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.service.machine.alerts.MachineAlertServiceImpl
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class PostgresMachineAlertSecurityTest {

    private lateinit var machineDataSource: FakeMachineRegistryDataSource
    private lateinit var alertDataSource: FakeMachineAlertDataSource
    private lateinit var alertService: MachineAlertServiceImpl

    @Before
    fun setUp() = runBlocking {
        machineDataSource = FakeMachineRegistryDataSource()
        alertDataSource = FakeMachineAlertDataSource()
        val machineRepo = MachineRegistryRepositoryImpl(machineDataSource)
        val alertRepo = MachineAlertRepositoryImpl(alertDataSource)
        alertService = MachineAlertServiceImpl(alertRepo, machineRepo)

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
    fun test01_tenantIsolation_crossTenantAlertRaise_fails() = runBlocking {
        val alertB = MachineOperationalAlert(
            alertId = "ALT-B-01",
            tenantId = "TENANT-B",
            machineId = "MAC-TENANT-A-01", // Attempting Tenant B alert on Tenant A's machine
            alertType = MachineAlertType.WARNING,
            title = "Unsanctioned Alert",
            description = "Cross tenant alert creation"
        )

        val result = alertService.raiseAlert(alertB)
        assertTrue(result is DomainResult.Error)
        val err = result as DomainResult.Error
        assertTrue(err.message.contains("not found in registry"))
    }
}
