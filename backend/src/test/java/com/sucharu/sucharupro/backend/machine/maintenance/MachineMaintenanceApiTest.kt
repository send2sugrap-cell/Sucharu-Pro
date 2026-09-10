package com.sucharu.sucharupro.backend.machine.maintenance

import com.sucharu.sucharupro.data.api.model.AuthenticatedPrincipal
import com.sucharu.sucharupro.data.api.model.ForbiddenException
import com.sucharu.sucharupro.data.api.model.UserRole
import com.sucharu.sucharupro.data.api.model.machine.maintenance.*
import com.sucharu.sucharupro.data.api.server.*
import com.sucharu.sucharupro.data.datasource.machine.FakeMachineRegistryDataSource
import com.sucharu.sucharupro.data.datasource.machine.maintenance.FakeMachineMaintenanceDataSource
import com.sucharu.sucharupro.data.persistence.postgres.PostgresRepositoryFactory
import com.sucharu.sucharupro.data.persistence.postgres.TenantContext
import com.sucharu.sucharupro.data.persistence.postgres.TransactionContext
import com.sucharu.sucharupro.data.persistence.postgres.TransactionManager
import com.sucharu.sucharupro.data.repository.machine.MachineRegistryRepositoryImpl
import com.sucharu.sucharupro.data.repository.machine.maintenance.MachineMaintenanceRepositoryImpl
import com.sucharu.sucharupro.domain.machine.MachineEquipment
import com.sucharu.sucharupro.domain.machine.MachineStatus
import com.sucharu.sucharupro.domain.machine.MachineType
import com.sucharu.sucharupro.domain.machine.maintenance.MaintenanceStatus
import com.sucharu.sucharupro.domain.machine.maintenance.MaintenanceType
import com.sucharu.sucharupro.domain.repository.machine.MachineRegistryRepository
import com.sucharu.sucharupro.domain.repository.machine.maintenance.MachineMaintenanceRepository
import com.sucharu.sucharupro.domain.service.machine.MachineRegistryService
import com.sucharu.sucharupro.domain.service.machine.MachineRegistryServiceImpl
import com.sucharu.sucharupro.domain.service.machine.maintenance.MachineMaintenanceService
import com.sucharu.sucharupro.domain.service.machine.maintenance.MachineMaintenanceServiceImpl
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class MachineMaintenanceApiTest {

    private lateinit var useCases: BackendUseCases
    private lateinit var customFactory: PostgresRepositoryFactory

    private val projectId = "TENANT-001"
    private val machineId = "MAC-MAINT-PRESS-01"

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

        val maintDs = FakeMachineMaintenanceDataSource()
        val maintRepo = MachineMaintenanceRepositoryImpl(maintDs)
        val maintService = MachineMaintenanceServiceImpl(maintRepo, machineRepo)

        val activeMachine = MachineEquipment(
            machineId = machineId,
            tenantId = projectId,
            assetCode = "EQ-MNT-01",
            name = "API Maintenance Test Press",
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
            override fun createMachineMaintenanceRepository(tenantId: String): MachineMaintenanceRepository = maintRepo
            override fun createMachineMaintenanceService(tenantId: String): MachineMaintenanceService = maintService
        }

        useCases = BackendUseCases(fakeTxManager, customFactory)
        Unit
    }

    @Test
    fun test01_createSchedule_staff_success() = runBlocking {
        val req = CreateMaintenanceScheduleRequestDto(
            title = "Monthly Lubrication",
            maintenanceType = MaintenanceType.PREVENTIVE,
            plannedDate = System.currentTimeMillis() + 86400000L
        )

        val response = useCases.createMaintenanceSchedule(staffPrincipal, machineId, req, customFactory)
        assertNotNull(response.scheduleId)
        assertEquals("Monthly Lubrication", response.title)
        assertEquals(MaintenanceStatus.PLANNED, response.status)
    }

    @Test
    fun test02_createRecord_and_start_complete_flow() = runBlocking {
        val createReq = CreateMaintenanceRecordRequestDto(
            title = "Replace Hydraulic Hose",
            problemDescription = "Minor fluid leak detected",
            maintenanceType = MaintenanceType.CORRECTIVE
        )

        val recordResp = useCases.createMaintenanceRecord(staffPrincipal, machineId, createReq, customFactory)
        assertNotNull(recordResp.recordId)

        // Start
        val startResp = useCases.startMaintenanceRecord(staffPrincipal, machineId, recordResp.recordId, customFactory)
        assertEquals(MaintenanceStatus.IN_PROGRESS, startResp.status)

        // Complete
        val completeReq = CompleteMaintenanceRecordRequestDto(
            resolutionSummary = "Hose replaced & pressure calibrated",
            workPerformed = "Replaced fitting and refilled hydraulic fluid"
        )
        val completeResp = useCases.completeMaintenanceRecord(staffPrincipal, machineId, recordResp.recordId, completeReq, customFactory)
        assertEquals(MaintenanceStatus.COMPLETED, completeResp.status)
        assertEquals("Hose replaced & pressure calibrated", completeResp.resolutionSummary)
    }

    @Test
    fun test03_createSchedule_customerRole_forbidden() = runBlocking {
        val req = CreateMaintenanceScheduleRequestDto(
            title = "Unauthorized Schedule",
            plannedDate = System.currentTimeMillis()
        )

        try {
            useCases.createMaintenanceSchedule(customerPrincipal, machineId, req, customFactory)
            fail("Expected ForbiddenException for customer creating maintenance schedule.")
        } catch (e: ForbiddenException) {
            assertTrue(e.message?.contains("Forbidden") == true || e.message?.contains("Access") == true)
        }
    }
}
