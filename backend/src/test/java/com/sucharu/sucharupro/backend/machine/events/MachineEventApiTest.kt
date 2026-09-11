package com.sucharu.sucharupro.backend.machine.events

import com.sucharu.sucharupro.data.api.model.AuthenticatedPrincipal
import com.sucharu.sucharupro.data.api.model.ForbiddenException
import com.sucharu.sucharupro.data.api.model.UserRole
import com.sucharu.sucharupro.data.api.model.machine.events.*
import com.sucharu.sucharupro.data.api.server.*
import com.sucharu.sucharupro.data.datasource.machine.FakeMachineRegistryDataSource
import com.sucharu.sucharupro.data.datasource.machine.events.FakeMachineEventDataSource
import com.sucharu.sucharupro.data.persistence.postgres.PostgresRepositoryFactory
import com.sucharu.sucharupro.data.persistence.postgres.TenantContext
import com.sucharu.sucharupro.data.persistence.postgres.TransactionContext
import com.sucharu.sucharupro.data.persistence.postgres.TransactionManager
import com.sucharu.sucharupro.data.repository.machine.MachineRegistryRepositoryImpl
import com.sucharu.sucharupro.data.repository.machine.events.MachineEventRepositoryImpl
import com.sucharu.sucharupro.domain.machine.MachineEquipment
import com.sucharu.sucharupro.domain.machine.MachineStatus
import com.sucharu.sucharupro.domain.machine.MachineType
import com.sucharu.sucharupro.domain.machine.events.DowntimeReasonCategory
import com.sucharu.sucharupro.domain.machine.events.DowntimeStatus
import com.sucharu.sucharupro.domain.machine.events.FaultSeverity
import com.sucharu.sucharupro.domain.machine.events.FaultStatus
import com.sucharu.sucharupro.domain.repository.machine.MachineRegistryRepository
import com.sucharu.sucharupro.domain.repository.machine.events.MachineEventRepository
import com.sucharu.sucharupro.domain.service.machine.MachineRegistryService
import com.sucharu.sucharupro.domain.service.machine.MachineRegistryServiceImpl
import com.sucharu.sucharupro.domain.service.machine.events.MachineDowntimeService
import com.sucharu.sucharupro.domain.service.machine.events.MachineDowntimeServiceImpl
import com.sucharu.sucharupro.domain.service.machine.events.MachineFaultEventService
import com.sucharu.sucharupro.domain.service.machine.events.MachineFaultEventServiceImpl
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class MachineEventApiTest {

    private lateinit var useCases: BackendUseCases
    private lateinit var customFactory: PostgresRepositoryFactory

    private val projectId = "TENANT-001"
    private val machineId = "MAC-EVENT-PRESS-01"

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

        val eventDs = FakeMachineEventDataSource()
        val eventRepo = MachineEventRepositoryImpl(eventDs)
        val faultService = MachineFaultEventServiceImpl(eventRepo, machineRepo)
        val downtimeService = MachineDowntimeServiceImpl(eventRepo, machineRepo)

        val activeMachine = MachineEquipment(
            machineId = machineId,
            tenantId = projectId,
            assetCode = "EQ-EVT-01",
            name = "API Event Test Press",
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
            override fun createMachineEventRepository(tenantId: String): MachineEventRepository = eventRepo
            override fun createMachineFaultEventService(tenantId: String): MachineFaultEventService = faultService
            override fun createMachineDowntimeService(tenantId: String): MachineDowntimeService = downtimeService
        }

        useCases = BackendUseCases(fakeTxManager, customFactory)
        Unit
    }

    @Test
    fun test01_recordFaultEvent_and_acknowledge_resolve_flow() = runBlocking {
        val createReq = CreateFaultEventRequestDto(
            faultCode = "ERR-101",
            faultType = "MECHANICAL",
            severity = FaultSeverity.CRITICAL,
            description = "Plate cylinder jam"
        )

        val faultResp = useCases.recordFaultEvent(staffPrincipal, machineId, createReq, customFactory)
        assertNotNull(faultResp.faultEventId)
        assertEquals(FaultStatus.OPEN, faultResp.status)

        // Acknowledge
        val ackResp = useCases.acknowledgeFaultEvent(staffPrincipal, machineId, faultResp.faultEventId, customFactory)
        assertEquals(FaultStatus.ACKNOWLEDGED, ackResp.status)

        // Resolve
        val resolveReq = ResolveFaultEventRequestDto(resolutionNotes = "Cleared jam and reset sensor")
        val resolveResp = useCases.resolveFaultEvent(staffPrincipal, machineId, faultResp.faultEventId, resolveReq, customFactory)
        assertEquals(FaultStatus.RESOLVED, resolveResp.status)
        assertEquals("Cleared jam and reset sensor", resolveResp.resolutionNotes)
    }

    @Test
    fun test02_downtimeEvent_startAndEnd_flow() = runBlocking {
        val startReq = StartDowntimeEventRequestDto(
            reasonCategory = DowntimeReasonCategory.MACHINE_FAULT,
            reasonDetails = "Unscheduled roller cleaning"
        )

        val downtimeResp = useCases.startDowntimeEvent(staffPrincipal, machineId, startReq, customFactory)
        assertNotNull(downtimeResp.downtimeId)
        assertEquals(DowntimeStatus.STARTED, downtimeResp.status)

        val endResp = useCases.endDowntimeEvent(staffPrincipal, machineId, downtimeResp.downtimeId, customFactory)
        assertEquals(DowntimeStatus.ENDED, endResp.status)
        assertNotNull(endResp.endedAt)
    }

    @Test
    fun test03_recordFaultEvent_customerRole_forbidden() = runBlocking {
        val req = CreateFaultEventRequestDto(
            faultType = "SECURITY",
            description = "Unauthorized fault event attempt"
        )

        try {
            useCases.recordFaultEvent(customerPrincipal, machineId, req, customFactory)
            fail("Expected ForbiddenException for customer recording fault event.")
        } catch (e: ForbiddenException) {
            assertTrue(e.message?.contains("Forbidden") == true || e.message?.contains("Access") == true)
        }
    }
}
