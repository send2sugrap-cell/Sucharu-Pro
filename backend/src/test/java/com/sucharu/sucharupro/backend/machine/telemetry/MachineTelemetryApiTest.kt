package com.sucharu.sucharupro.backend.machine.telemetry

import com.sucharu.sucharupro.data.api.model.AuthenticatedPrincipal
import com.sucharu.sucharupro.data.api.model.ForbiddenException
import com.sucharu.sucharupro.data.api.model.UserRole
import com.sucharu.sucharupro.data.api.model.machine.telemetry.IngestTelemetryRequestDto
import com.sucharu.sucharupro.data.api.model.machine.telemetry.TelemetryBatchIngestRequestDto
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
import com.sucharu.sucharupro.domain.machine.telemetry.TelemetryMetricType
import com.sucharu.sucharupro.domain.repository.machine.MachineRegistryRepository
import com.sucharu.sucharupro.domain.repository.machine.telemetry.MachineTelemetryRepository
import com.sucharu.sucharupro.domain.service.machine.MachineRegistryService
import com.sucharu.sucharupro.domain.service.machine.MachineRegistryServiceImpl
import com.sucharu.sucharupro.domain.service.machine.telemetry.MachineTelemetryIngestionService
import com.sucharu.sucharupro.domain.service.machine.telemetry.MachineTelemetryIngestionServiceImpl
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

class MachineTelemetryApiTest {

    private lateinit var useCases: BackendUseCases
    private lateinit var customFactory: PostgresRepositoryFactory

    private val projectId = "TENANT-001"
    private val machineId = "MAC-API-PRESS-01"

    private val staffPrincipal = AuthenticatedPrincipal(
        userId = "staff_01",
        projectId = projectId,
        username = "staff_user",
        role = UserRole.STAFF
    )

    private val customerPrincipal = AuthenticatedPrincipal(
        userId = "customer_01",
        projectId = projectId,
        username = "customer_user",
        role = UserRole.CUSTOMER
    )

    @Before
    fun setup() = runBlocking {
        val machineDs = FakeMachineRegistryDataSource()
        val machineRepo = MachineRegistryRepositoryImpl(machineDs)
        val machineService = MachineRegistryServiceImpl(machineRepo)

        val telemetryDs = FakeMachineTelemetryDataSource()
        val telemetryRepo = MachineTelemetryRepositoryImpl(telemetryDs)
        val telemetryService = MachineTelemetryIngestionServiceImpl(telemetryRepo, machineRepo)

        val activeMachine = MachineEquipment(
            machineId = machineId,
            tenantId = projectId,
            assetCode = "EQ-API-01",
            name = "API Test Heidelberg Press",
            type = MachineType.PRINTING_PRESS,
            status = MachineStatus.AVAILABLE
        )
        machineRepo.saveMachine(activeMachine)

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
            override fun createMachineRegistryService(tenantId: String): MachineRegistryService = machineService
            override fun createMachineTelemetryIngestionRepository(tenantId: String): MachineTelemetryRepository = telemetryRepo
            override fun createMachineTelemetryIngestionService(tenantId: String): MachineTelemetryIngestionService = telemetryService
        }

        useCases = BackendUseCases(fakeTxManager, customFactory)
        Unit
    }

    @Test
    fun test01_ingestTelemetry_staff_success() = runBlocking {
        val req = IngestTelemetryRequestDto(
            metricType = TelemetryMetricType.SPEED,
            metricValue = BigDecimal("16500"),
            unit = "IPH",
            sourceDeviceId = "GATEWAY-01"
        )

        val response = useCases.ingestTelemetry(staffPrincipal, machineId, req, customFactory)
        assertNotNull(response.telemetryId)
        assertEquals(machineId, response.machineId)
        assertEquals(TelemetryMetricType.SPEED, response.metricType)
        assertEquals(BigDecimal("16500"), response.metricValue)
    }

    @Test
    fun test02_ingestTelemetry_customerRole_forbidden() = runBlocking {
        val req = IngestTelemetryRequestDto(
            metricType = TelemetryMetricType.SPEED,
            metricValue = BigDecimal("10000")
        )

        try {
            useCases.ingestTelemetry(customerPrincipal, machineId, req, customFactory)
            fail("Expected ForbiddenException for customer ingesting telemetry.")
        } catch (e: ForbiddenException) {
            assertTrue(e.message?.contains("Forbidden") == true || e.message?.contains("Access") == true)
        }
    }

    @Test
    fun test03_ingestTelemetryBatch_success() = runBlocking {
        val batchReq = TelemetryBatchIngestRequestDto(
            readings = listOf(
                IngestTelemetryRequestDto(metricType = TelemetryMetricType.RPM, metricValue = BigDecimal("2800")),
                IngestTelemetryRequestDto(metricType = TelemetryMetricType.TEMPERATURE, metricValue = BigDecimal("45.2"))
            )
        )

        val response = useCases.ingestTelemetryBatch(staffPrincipal, machineId, batchReq, customFactory)
        assertEquals(2, response.ingestedCount)
        assertEquals(2, response.records.size)
    }

    @Test
    fun test04_listMachineTelemetry_success() = runBlocking {
        useCases.ingestTelemetry(
            staffPrincipal,
            machineId,
            IngestTelemetryRequestDto(metricType = TelemetryMetricType.PRESSURE, metricValue = BigDecimal("6.2")),
            customFactory
        )

        val list = useCases.listMachineTelemetry(staffPrincipal, machineId, repositoryFactory = customFactory)
        assertTrue(list.isNotEmpty())
    }
}
