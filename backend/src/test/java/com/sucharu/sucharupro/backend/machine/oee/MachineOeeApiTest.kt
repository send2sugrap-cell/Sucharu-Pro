package com.sucharu.sucharupro.backend.machine.oee

import com.sucharu.sucharupro.data.api.model.AuthenticatedPrincipal
import com.sucharu.sucharupro.data.api.model.ForbiddenException
import com.sucharu.sucharupro.data.api.model.UserRole
import com.sucharu.sucharupro.data.api.model.machine.oee.*
import com.sucharu.sucharupro.data.api.server.*
import com.sucharu.sucharupro.data.datasource.machine.FakeMachineRegistryDataSource
import com.sucharu.sucharupro.data.datasource.machine.oee.FakeMachineOeeDataSource
import com.sucharu.sucharupro.data.persistence.postgres.PostgresRepositoryFactory
import com.sucharu.sucharupro.data.persistence.postgres.TenantContext
import com.sucharu.sucharupro.data.persistence.postgres.TransactionContext
import com.sucharu.sucharupro.data.persistence.postgres.TransactionManager
import com.sucharu.sucharupro.data.repository.machine.MachineRegistryRepositoryImpl
import com.sucharu.sucharupro.data.repository.machine.oee.MachineOeeRepositoryImpl
import com.sucharu.sucharupro.domain.machine.MachineEquipment
import com.sucharu.sucharupro.domain.machine.MachineStatus
import com.sucharu.sucharupro.domain.machine.MachineType
import com.sucharu.sucharupro.domain.repository.machine.MachineRegistryRepository
import com.sucharu.sucharupro.domain.repository.machine.oee.MachineOeeRepository
import com.sucharu.sucharupro.domain.service.machine.MachineRegistryService
import com.sucharu.sucharupro.domain.service.machine.MachineRegistryServiceImpl
import com.sucharu.sucharupro.domain.service.machine.oee.MachineOeeService
import com.sucharu.sucharupro.domain.service.machine.oee.MachineOeeServiceImpl
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

class MachineOeeApiTest {

    private lateinit var useCases: BackendUseCases
    private lateinit var customFactory: PostgresRepositoryFactory

    private val projectId = "TENANT-001"
    private val machineId = "MAC-OEE-PRESS-01"

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

        val oeeDs = FakeMachineOeeDataSource()
        val oeeRepo = MachineOeeRepositoryImpl(oeeDs)
        val oeeService = MachineOeeServiceImpl(oeeRepo, machineRepo)

        val activeMachine = MachineEquipment(
            machineId = machineId,
            tenantId = projectId,
            assetCode = "EQ-OEE-01",
            name = "API OEE Test Press",
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
            override fun createMachineOeeRepository(tenantId: String): MachineOeeRepository = oeeRepo
            override fun createMachineOeeService(tenantId: String): MachineOeeService = oeeService
        }

        useCases = BackendUseCases(fakeTxManager, customFactory)
        Unit
    }

    @Test
    fun test01_calculateAndSaveOee_staff_success() = runBlocking {
        val now = System.currentTimeMillis()
        val start = now - 28800000L

        val req = CalculateOeeRequestDto(
            periodStart = start,
            periodEnd = now,
            customPlannedProductionSeconds = 28800L,
            customIdealRateUnitsPerHour = BigDecimal("1000")
        )

        val oeeResp = useCases.calculateAndSaveOee(staffPrincipal, machineId, req, customFactory)
        assertNotNull(oeeResp.metricId)
        assertEquals(28800L, oeeResp.plannedProductionSeconds)
        assertEquals(BigDecimal("1.0000"), oeeResp.availabilityRatio)

        // Get Latest Summary
        val summary = useCases.getLatestOeeSummary(staffPrincipal, machineId, customFactory)
        assertNotNull(summary)
        assertEquals(oeeResp.metricId, summary?.metricId)
    }

    @Test
    fun test02_calculateAndSaveOee_customerRole_forbidden() = runBlocking {
        val req = CalculateOeeRequestDto(
            periodStart = System.currentTimeMillis() - 3600000L,
            periodEnd = System.currentTimeMillis()
        )

        try {
            useCases.calculateAndSaveOee(customerPrincipal, machineId, req, customFactory)
            fail("Expected ForbiddenException for customer calculating OEE.")
        } catch (e: ForbiddenException) {
            assertTrue(e.message?.contains("Forbidden") == true || e.message?.contains("Access") == true)
        }
    }
}
