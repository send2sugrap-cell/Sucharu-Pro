package com.sucharu.sucharupro.backend.machine.health

import com.sucharu.sucharupro.data.api.model.AuthenticatedPrincipal
import com.sucharu.sucharupro.data.api.model.UserRole
import com.sucharu.sucharupro.data.api.server.*
import com.sucharu.sucharupro.data.datasource.machine.FakeMachineRegistryDataSource
import com.sucharu.sucharupro.data.datasource.machine.telemetry.FakeMachineTelemetryDataSource
import com.sucharu.sucharupro.data.persistence.postgres.PostgresRepositoryFactory
import com.sucharu.sucharupro.data.persistence.postgres.TenantContext
import com.sucharu.sucharupro.data.persistence.postgres.TransactionContext
import com.sucharu.sucharupro.data.persistence.postgres.TransactionManager
import com.sucharu.sucharupro.data.repository.machine.MachineRegistryRepositoryImpl
import com.sucharu.sucharupro.data.repository.machine.telemetry.MachineTelemetryRepositoryImpl
import com.sucharu.sucharupro.domain.machine.MachineEquipment
import com.sucharu.sucharupro.domain.machine.MachineStatus
import com.sucharu.sucharupro.domain.machine.MachineType
import com.sucharu.sucharupro.domain.machine.health.MachineHealthCondition
import com.sucharu.sucharupro.domain.machine.health.MachineOperationalState
import com.sucharu.sucharupro.domain.machine.telemetry.MachineTelemetryRecord
import com.sucharu.sucharupro.domain.machine.telemetry.TelemetryMetricType
import com.sucharu.sucharupro.domain.repository.machine.MachineRegistryRepository
import com.sucharu.sucharupro.domain.repository.machine.telemetry.MachineTelemetryRepository
import com.sucharu.sucharupro.domain.service.machine.health.MachineStatusMonitoringService
import com.sucharu.sucharupro.domain.service.machine.health.MachineStatusMonitoringServiceImpl
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

class MachineStatusApiTest {

    private lateinit var useCases: BackendUseCases
    private lateinit var customFactory: PostgresRepositoryFactory

    private val projectId = "TENANT-001"
    private val machineId = "MAC-HEALTH-PRESS-01"

    private val staffPrincipal = AuthenticatedPrincipal(
        userId = "staff_01",
        projectId = projectId,
        username = "staff_user",
        role = UserRole.STAFF
    )

    @Before
    fun setup() = runBlocking {
        val machineDs = FakeMachineRegistryDataSource()
        val machineRepo = MachineRegistryRepositoryImpl(machineDs)

        val telemetryDs = FakeMachineTelemetryDataSource()
        val telemetryRepo = MachineTelemetryRepositoryImpl(telemetryDs)

        val healthService = MachineStatusMonitoringServiceImpl(machineRepo, telemetryRepo)

        val activeMachine = MachineEquipment(
            machineId = machineId,
            tenantId = projectId,
            assetCode = "EQ-HLT-01",
            name = "Health Test Heidelberg Press",
            type = MachineType.PRINTING_PRESS,
            status = MachineStatus.AVAILABLE
        )
        machineRepo.saveMachine(activeMachine)

        val now = System.currentTimeMillis()
        telemetryRepo.saveTelemetryRecord(
            MachineTelemetryRecord(
                telemetryId = "TEL-HLT-01",
                tenantId = projectId,
                machineId = machineId,
                metricType = TelemetryMetricType.SPEED,
                metricValue = BigDecimal("12500"),
                unit = "IPH",
                eventTimestamp = now
            )
        )

        val fakeTxManager = object : TransactionManager {
            override suspend fun <T> inTransaction(tenantContext: TenantContext, block: suspend (TransactionContext) -> T): T {
                throw UnsupportedOperationException("Not required for mock tests")
            }
            override suspend fun <T> inReadOnly(tenantContext: TenantContext, block: suspend (TransactionContext) -> T): T {
                throw UnsupportedOperationException("Not required for mock tests")
            }
        }

        customFactory = object : PostgresRepositoryFactory(fakeTxManager) {
            override fun createMachineRegistryRepository(tenantId: String): MachineRegistryRepository = machineRepo
            override fun createMachineTelemetryIngestionRepository(tenantId: String): MachineTelemetryRepository = telemetryRepo
            override fun createMachineStatusMonitoringService(tenantId: String): MachineStatusMonitoringService = healthService
        }

        useCases = BackendUseCases(fakeTxManager, customFactory)
        Unit
    }

    @Test
    fun test01_evaluateMachineHealth_success() = runBlocking {
        val response = useCases.evaluateMachineHealth(staffPrincipal, machineId, customFactory)
        assertNotNull(response)
        assertEquals(machineId, response.machineId)
        assertEquals(MachineOperationalState.RUNNING, response.operationalState)
        assertEquals(MachineHealthCondition.HEALTHY, response.healthCondition)
        assertTrue(response.isFresh)
    }

    @Test
    fun test02_listMachinesHealth_success() = runBlocking {
        val listResponse = useCases.listMachinesHealth(staffPrincipal, repositoryFactory = customFactory)
        assertNotNull(listResponse)
        assertTrue(listResponse.machines.isNotEmpty())
        assertEquals(1, listResponse.totalCount)
        assertEquals(MachineOperationalState.RUNNING, listResponse.machines.first().operationalState)
    }
}
