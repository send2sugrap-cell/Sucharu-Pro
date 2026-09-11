package com.sucharu.sucharupro.backend.machine.alerts

import com.sucharu.sucharupro.data.api.model.AuthenticatedPrincipal
import com.sucharu.sucharupro.data.api.model.ForbiddenException
import com.sucharu.sucharupro.data.api.model.UserRole
import com.sucharu.sucharupro.data.api.model.machine.alerts.*
import com.sucharu.sucharupro.data.api.server.*
import com.sucharu.sucharupro.data.datasource.FakeNotificationDataSource
import com.sucharu.sucharupro.data.datasource.machine.FakeMachineRegistryDataSource
import com.sucharu.sucharupro.data.datasource.machine.alerts.FakeMachineAlertDataSource
import com.sucharu.sucharupro.data.persistence.postgres.PostgresRepositoryFactory
import com.sucharu.sucharupro.data.persistence.postgres.TenantContext
import com.sucharu.sucharupro.data.persistence.postgres.TransactionContext
import com.sucharu.sucharupro.data.persistence.postgres.TransactionManager
import com.sucharu.sucharupro.data.repository.NotificationRepositoryImpl
import com.sucharu.sucharupro.data.repository.machine.MachineRegistryRepositoryImpl
import com.sucharu.sucharupro.data.repository.machine.alerts.MachineAlertRepositoryImpl
import com.sucharu.sucharupro.domain.machine.MachineEquipment
import com.sucharu.sucharupro.domain.machine.MachineStatus
import com.sucharu.sucharupro.domain.machine.MachineType
import com.sucharu.sucharupro.domain.machine.alerts.MachineAlertStatus
import com.sucharu.sucharupro.domain.machine.alerts.MachineAlertType
import com.sucharu.sucharupro.domain.machine.events.FaultSeverity
import com.sucharu.sucharupro.domain.repository.notification.NotificationRepository
import com.sucharu.sucharupro.domain.repository.machine.MachineRegistryRepository
import com.sucharu.sucharupro.domain.repository.machine.alerts.MachineAlertRepository
import com.sucharu.sucharupro.domain.service.machine.MachineRegistryService
import com.sucharu.sucharupro.domain.service.machine.MachineRegistryServiceImpl
import com.sucharu.sucharupro.domain.service.machine.alerts.MachineAlertService
import com.sucharu.sucharupro.domain.service.machine.alerts.MachineAlertServiceImpl
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class MachineAlertApiTest {

    private lateinit var useCases: BackendUseCases
    private lateinit var customFactory: PostgresRepositoryFactory

    private val projectId = "TENANT-001"
    private val machineId = "MAC-ALERT-PRESS-01"

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

        val notifDs = FakeNotificationDataSource()
        val notifRepo = NotificationRepositoryImpl(notifDs)

        val alertDs = FakeMachineAlertDataSource()
        val alertRepo = MachineAlertRepositoryImpl(alertDs)
        val alertService = MachineAlertServiceImpl(alertRepo, machineRepo, notifRepo)

        val activeMachine = MachineEquipment(
            machineId = machineId,
            tenantId = projectId,
            assetCode = "EQ-ALT-01",
            name = "API Alert Test Press",
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
            override fun createNotificationRepository(tenantId: String): NotificationRepository = notifRepo
            override fun createMachineAlertRepository(tenantId: String): MachineAlertRepository = alertRepo
            override fun createMachineAlertService(tenantId: String): MachineAlertService = alertService
        }

        useCases = BackendUseCases(fakeTxManager, customFactory)
        Unit
    }

    @Test
    fun test01_raiseMachineAlert_and_acknowledge_resolve_flow() = runBlocking {
        val createReq = CreateMachineAlertRequestDto(
            alertType = MachineAlertType.TELEMETRY_ABNORMAL,
            severity = FaultSeverity.WARNING,
            title = "Paper Tension Warning",
            description = "Tension fluctuation detected on unwinder"
        )

        val alertResp = useCases.raiseMachineAlert(staffPrincipal, machineId, createReq, customFactory)
        assertNotNull(alertResp.alertId)
        assertEquals(MachineAlertStatus.ACTIVE, alertResp.status)
        assertNotNull(alertResp.notificationId)

        // Acknowledge
        val ackResp = useCases.acknowledgeMachineAlert(staffPrincipal, machineId, alertResp.alertId, customFactory)
        assertEquals(MachineAlertStatus.ACKNOWLEDGED, ackResp.status)

        // Resolve
        val resolveReq = ResolveMachineAlertRequestDto(resolutionNotes = "Tension roller recalibrated")
        val resolveResp = useCases.resolveMachineAlert(staffPrincipal, machineId, alertResp.alertId, resolveReq, customFactory)
        assertEquals(MachineAlertStatus.RESOLVED, resolveResp.status)
        assertEquals("Tension roller recalibrated", resolveResp.resolutionNotes)
    }

    @Test
    fun test02_dismissMachineAlert_flow() = runBlocking {
        val createReq = CreateMachineAlertRequestDto(
            alertType = MachineAlertType.WARNING,
            severity = FaultSeverity.WARNING,
            title = "Transient Sensor Spike",
            description = "Sensor spike cleared automatically"
        )

        val alertResp = useCases.raiseMachineAlert(staffPrincipal, machineId, createReq, customFactory)

        val dismissReq = DismissMachineAlertRequestDto(reason = "False alarm during maintenance test")
        val dismissResp = useCases.dismissMachineAlert(staffPrincipal, machineId, alertResp.alertId, dismissReq, customFactory)
        assertEquals(MachineAlertStatus.DISMISSED, dismissResp.status)
    }

    @Test
    fun test03_raiseMachineAlert_customerRole_forbidden() = runBlocking {
        val req = CreateMachineAlertRequestDto(
            alertType = MachineAlertType.WARNING,
            title = "Unauthorized Alert",
            description = "Customer attempt to raise alert"
        )

        try {
            useCases.raiseMachineAlert(customerPrincipal, machineId, req, customFactory)
            fail("Expected ForbiddenException for customer raising alert.")
        } catch (e: ForbiddenException) {
            assertTrue(e.message?.contains("Forbidden") == true || e.message?.contains("Access") == true)
        }
    }
}
