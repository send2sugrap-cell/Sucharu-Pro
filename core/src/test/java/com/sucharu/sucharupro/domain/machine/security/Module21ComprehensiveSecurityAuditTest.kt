package com.sucharu.sucharupro.domain.machine.security

import com.sucharu.sucharupro.data.api.model.AuthenticatedPrincipal
import com.sucharu.sucharupro.data.api.model.ForbiddenException
import com.sucharu.sucharupro.data.api.model.UserRole
import com.sucharu.sucharupro.data.api.model.machine.alerts.CreateMachineAlertRequestDto
import com.sucharu.sucharupro.data.api.model.machine.events.CreateFaultEventRequestDto
import com.sucharu.sucharupro.data.api.model.machine.events.StartDowntimeEventRequestDto
import com.sucharu.sucharupro.data.api.model.machine.maintenance.CreateMaintenanceRecordRequestDto
import com.sucharu.sucharupro.data.api.model.machine.oee.CalculateOeeRequestDto
import com.sucharu.sucharupro.data.api.server.*
import com.sucharu.sucharupro.data.datasource.FakeNotificationDataSource
import com.sucharu.sucharupro.data.datasource.machine.FakeMachineRegistryDataSource
import com.sucharu.sucharupro.data.datasource.machine.alerts.FakeMachineAlertDataSource
import com.sucharu.sucharupro.data.datasource.machine.events.FakeMachineEventDataSource
import com.sucharu.sucharupro.data.datasource.machine.maintenance.FakeMachineMaintenanceDataSource
import com.sucharu.sucharupro.data.datasource.machine.oee.FakeMachineOeeDataSource
import com.sucharu.sucharupro.data.datasource.machine.telemetry.FakeMachineTelemetryDataSource
import com.sucharu.sucharupro.data.persistence.postgres.PostgresRepositoryFactory
import com.sucharu.sucharupro.data.persistence.postgres.TenantContext
import com.sucharu.sucharupro.data.persistence.postgres.TransactionContext
import com.sucharu.sucharupro.data.persistence.postgres.TransactionManager
import com.sucharu.sucharupro.data.repository.NotificationRepositoryImpl
import com.sucharu.sucharupro.data.repository.machine.MachineRegistryRepositoryImpl
import com.sucharu.sucharupro.data.repository.machine.alerts.MachineAlertRepositoryImpl
import com.sucharu.sucharupro.data.repository.machine.events.MachineEventRepositoryImpl
import com.sucharu.sucharupro.data.repository.machine.maintenance.MachineMaintenanceRepositoryImpl
import com.sucharu.sucharupro.data.repository.machine.oee.MachineOeeRepositoryImpl
import com.sucharu.sucharupro.data.repository.machine.telemetry.MachineTelemetryRepositoryImpl
import com.sucharu.sucharupro.domain.machine.MachineEquipment
import com.sucharu.sucharupro.domain.machine.MachineStatus
import com.sucharu.sucharupro.domain.machine.MachineType
import com.sucharu.sucharupro.domain.machine.events.DowntimeReasonCategory
import com.sucharu.sucharupro.domain.machine.events.MachineFaultEvent
import com.sucharu.sucharupro.domain.machine.maintenance.MaintenanceRecord
import com.sucharu.sucharupro.domain.machine.maintenance.MaintenanceType
import com.sucharu.sucharupro.domain.machine.oee.MachineOeeCalculationRequest
import com.sucharu.sucharupro.domain.machine.telemetry.MachineTelemetryRecord
import com.sucharu.sucharupro.domain.machine.telemetry.TelemetryMetricType
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.production.ProductionStageType
import com.sucharu.sucharupro.domain.repository.notification.NotificationRepository
import com.sucharu.sucharupro.domain.repository.machine.MachineRegistryRepository
import com.sucharu.sucharupro.domain.repository.machine.alerts.MachineAlertRepository
import com.sucharu.sucharupro.domain.repository.machine.events.MachineEventRepository
import com.sucharu.sucharupro.domain.repository.machine.maintenance.MachineMaintenanceRepository
import com.sucharu.sucharupro.domain.repository.machine.oee.MachineOeeRepository
import com.sucharu.sucharupro.domain.repository.machine.telemetry.MachineTelemetryRepository
import com.sucharu.sucharupro.domain.service.machine.MachineRegistryService
import com.sucharu.sucharupro.domain.service.machine.MachineRegistryServiceImpl
import com.sucharu.sucharupro.domain.service.machine.alerts.MachineAlertService
import com.sucharu.sucharupro.domain.service.machine.alerts.MachineAlertServiceImpl
import com.sucharu.sucharupro.domain.service.machine.events.MachineDowntimeService
import com.sucharu.sucharupro.domain.service.machine.events.MachineDowntimeServiceImpl
import com.sucharu.sucharupro.domain.service.machine.events.MachineFaultEventService
import com.sucharu.sucharupro.domain.service.machine.events.MachineFaultEventServiceImpl
import com.sucharu.sucharupro.domain.service.machine.maintenance.MachineMaintenanceService
import com.sucharu.sucharupro.domain.service.machine.maintenance.MachineMaintenanceServiceImpl
import com.sucharu.sucharupro.domain.service.machine.oee.MachineOeeService
import com.sucharu.sucharupro.domain.service.machine.oee.MachineOeeServiceImpl
import com.sucharu.sucharupro.domain.service.machine.telemetry.MachineTelemetryIngestionService
import com.sucharu.sucharupro.domain.service.machine.telemetry.MachineTelemetryIngestionServiceImpl
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

class Module21ComprehensiveSecurityAuditTest {

    private lateinit var useCases: BackendUseCases
    private lateinit var customFactory: PostgresRepositoryFactory

    private lateinit var machineRepo: MachineRegistryRepositoryImpl
    private lateinit var telemetryService: MachineTelemetryIngestionServiceImpl
    private lateinit var maintService: MachineMaintenanceServiceImpl
    private lateinit var faultService: MachineFaultEventServiceImpl
    private lateinit var downtimeService: MachineDowntimeServiceImpl
    private lateinit var alertService: MachineAlertServiceImpl
    private lateinit var oeeService: MachineOeeServiceImpl

    private val tenantA = "TENANT-A"
    private val tenantB = "TENANT-B"
    private val machineIdA = "MAC-TENANT-A-01"

    private val staffPrincipal = AuthenticatedPrincipal(
        userId = "staff_01",
        projectId = tenantA,
        username = "staff_user",
        role = UserRole.STAFF
    )

    private val customerPrincipal = AuthenticatedPrincipal(
        userId = "customer_01",
        projectId = tenantA,
        username = "customer_user",
        role = UserRole.CUSTOMER
    )

    private val affiliatePrincipal = AuthenticatedPrincipal(
        userId = "affiliate_01",
        projectId = tenantA,
        username = "affiliate_user",
        role = UserRole.AFFILIATE
    )

    private val aiAgentPrincipal = AuthenticatedPrincipal(
        userId = "ai_agent_01",
        projectId = tenantA,
        username = "ai_agent_user",
        role = UserRole.AI_AGENT
    )

    @Before
    fun setUp() = runBlocking {
        val machineDs = FakeMachineRegistryDataSource()
        machineRepo = MachineRegistryRepositoryImpl(machineDs)
        val machineService = MachineRegistryServiceImpl(machineRepo)

        val telemetryDs = FakeMachineTelemetryDataSource()
        val telemetryRepo = MachineTelemetryRepositoryImpl(telemetryDs)
        telemetryService = MachineTelemetryIngestionServiceImpl(telemetryRepo, machineRepo)

        val maintDs = FakeMachineMaintenanceDataSource()
        val maintRepo = MachineMaintenanceRepositoryImpl(maintDs)
        maintService = MachineMaintenanceServiceImpl(maintRepo, machineRepo)

        val eventDs = FakeMachineEventDataSource()
        val eventRepo = MachineEventRepositoryImpl(eventDs)
        faultService = MachineFaultEventServiceImpl(eventRepo, machineRepo)
        downtimeService = MachineDowntimeServiceImpl(eventRepo, machineRepo)

        val notifDs = FakeNotificationDataSource()
        val notifRepo = NotificationRepositoryImpl(notifDs)

        val alertDs = FakeMachineAlertDataSource()
        val alertRepo = MachineAlertRepositoryImpl(alertDs)
        alertService = MachineAlertServiceImpl(alertRepo, machineRepo, notifRepo)

        val oeeDs = FakeMachineOeeDataSource()
        val oeeRepo = MachineOeeRepositoryImpl(oeeDs)
        oeeService = MachineOeeServiceImpl(oeeRepo, machineRepo, eventRepo, null)

        val machineA = MachineEquipment(
            machineId = machineIdA,
            tenantId = tenantA,
            assetCode = "EQ-A1",
            name = "Tenant A Press",
            type = MachineType.PRINTING_PRESS,
            status = MachineStatus.AVAILABLE
        )
        machineRepo.saveMachine(machineA)

        val fakeTxManager = object : TransactionManager {
            override suspend fun <T> inTransaction(tenantContext: TenantContext, block: suspend (TransactionContext) -> T): T {
                throw UnsupportedOperationException("Mock test context")
            }
            override suspend fun <T> inReadOnly(tenantContext: TenantContext, block: suspend (TransactionContext) -> T): T {
                throw UnsupportedOperationException("Mock test context")
            }
        }

        customFactory = object : PostgresRepositoryFactory(fakeTxManager) {
            override fun createMachineRegistryRepository(tenantId: String): MachineRegistryRepository = machineRepo
            override fun createMachineRegistryService(tenantId: String): MachineRegistryService = machineService
            override fun createMachineTelemetryIngestionRepository(tenantId: String): MachineTelemetryRepository = telemetryRepo
            override fun createMachineTelemetryIngestionService(tenantId: String): MachineTelemetryIngestionService = telemetryService
            override fun createMachineMaintenanceRepository(tenantId: String): MachineMaintenanceRepository = maintRepo
            override fun createMachineMaintenanceService(tenantId: String): MachineMaintenanceService = maintService
            override fun createMachineEventRepository(tenantId: String): MachineEventRepository = eventRepo
            override fun createMachineFaultEventService(tenantId: String): MachineFaultEventService = faultService
            override fun createMachineDowntimeService(tenantId: String): MachineDowntimeService = downtimeService
            override fun createNotificationRepository(tenantId: String): NotificationRepository = notifRepo
            override fun createMachineAlertRepository(tenantId: String): MachineAlertRepository = alertRepo
            override fun createMachineAlertService(tenantId: String): MachineAlertService = alertService
            override fun createMachineOeeRepository(tenantId: String): MachineOeeRepository = oeeRepo
            override fun createMachineOeeService(tenantId: String): MachineOeeService = oeeService
        }

        useCases = BackendUseCases(fakeTxManager, customFactory)
        Unit
    }

    @Test
    fun test01_rbac_customerRole_unauthorizedForModule21Management() = runBlocking {
        // Customer attempt to create maintenance record
        val maintReq = CreateMaintenanceRecordRequestDto(
            title = "Customer Unauthorized Maintenance",
            maintenanceType = MaintenanceType.CORRECTIVE
        )
        try {
            useCases.createMaintenanceRecord(customerPrincipal, machineIdA, maintReq, customFactory)
            fail("Expected ForbiddenException for Customer creating maintenance record.")
        } catch (e: ForbiddenException) {
            assertTrue(e.message?.contains("Forbidden") == true || e.message?.contains("Access") == true)
        }

        // Customer attempt to record fault event
        val faultReq = CreateFaultEventRequestDto(
            faultType = "SECURITY",
            description = "Customer Unauthorized Fault"
        )
        try {
            useCases.recordFaultEvent(customerPrincipal, machineIdA, faultReq, customFactory)
            fail("Expected ForbiddenException for Customer recording fault event.")
        } catch (e: ForbiddenException) {
            assertTrue(e.message?.contains("Forbidden") == true || e.message?.contains("Access") == true)
        }

        // Customer attempt to raise alert
        val alertReq = CreateMachineAlertRequestDto(
            title = "Customer Unauthorized Alert",
            description = "Unsanctioned alert"
        )
        try {
            useCases.raiseMachineAlert(customerPrincipal, machineIdA, alertReq, customFactory)
            fail("Expected ForbiddenException for Customer raising alert.")
        } catch (e: ForbiddenException) {
            assertTrue(e.message?.contains("Forbidden") == true || e.message?.contains("Access") == true)
        }
    }

    @Test
    fun test02_rbac_affiliateRole_unauthorizedForModule21Management() = runBlocking {
        val downtimeReq = StartDowntimeEventRequestDto(
            reasonCategory = DowntimeReasonCategory.OPERATOR
        )
        try {
            useCases.startDowntimeEvent(affiliatePrincipal, machineIdA, downtimeReq, customFactory)
            fail("Expected ForbiddenException for Affiliate starting downtime event.")
        } catch (e: ForbiddenException) {
            assertTrue(e.message?.contains("Forbidden") == true || e.message?.contains("Access") == true)
        }
    }

    @Test
    fun test03_crossTenantIsolation_telemetry_crossTenantIngestion_rejected() = runBlocking {
        val telemetry = MachineTelemetryRecord(
            telemetryId = "TEL-B-01",
            tenantId = tenantB, // Tenant B attempting to ingest telemetry for Tenant A's machine
            machineId = machineIdA,
            metricType = TelemetryMetricType.OPERATIONAL_STATE,
            metricValue = BigDecimal("1.0"),
            eventTimestamp = System.currentTimeMillis()
        )

        val result = telemetryService.ingestTelemetry(telemetry)
        assertTrue(result is DomainResult.Error)
        val err = result as DomainResult.Error
        assertTrue(err.message.contains("not found in registry"))
    }

    @Test
    fun test04_crossTenantIsolation_maintenance_crossTenantRecord_rejected() = runBlocking {
        val recordB = MaintenanceRecord(
            recordId = "REC-B-01",
            tenantId = tenantB,
            machineId = machineIdA,
            title = "Tenant B Maintenance",
            maintenanceType = MaintenanceType.PREVENTIVE
        )

        val result = maintService.createRecord(recordB)
        assertTrue(result is DomainResult.Error)
        val err = result as DomainResult.Error
        assertTrue(err.message.contains("not found in registry"))
    }

    @Test
    fun test05_crossTenantIsolation_faultEvents_crossTenantRecord_rejected() = runBlocking {
        val faultB = MachineFaultEvent(
            faultEventId = "FLT-B-01",
            tenantId = tenantB,
            machineId = machineIdA,
            faultType = "ELECTRICAL",
            description = "Tenant B Fault"
        )

        val result = faultService.recordFault(faultB)
        assertTrue(result is DomainResult.Error)
        val err = result as DomainResult.Error
        assertTrue(err.message.contains("not found in registry"))
    }

    @Test
    fun test06_crossTenantIsolation_oee_crossTenantCalculation_rejected() = runBlocking {
        val reqB = MachineOeeCalculationRequest(
            tenantId = tenantB,
            machineId = machineIdA,
            periodStart = System.currentTimeMillis() - 3600000L,
            periodEnd = System.currentTimeMillis()
        )

        val result = oeeService.calculateAndSaveOee(reqB)
        assertTrue(result is DomainResult.Error)
        val err = result as DomainResult.Error
        assertTrue(err.message.contains("not found in registry"))
    }

    @Test
    fun test07_idorDefense_machineIdSubstitution_failsAcrossTenants() = runBlocking {
        // Staff Principal from Tenant A attempts to calculate OEE for non-existent or Tenant B machine ID
        val req = CalculateOeeRequestDto(
            periodStart = System.currentTimeMillis() - 3600000L,
            periodEnd = System.currentTimeMillis()
        )

        try {
            useCases.calculateAndSaveOee(staffPrincipal, "MAC-GHOST-TENANT-B", req, customFactory)
            fail("Expected IllegalArgumentException for IDOR machine ID substitution.")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message?.contains("not found in registry") == true)
        }
    }

    @Test
    fun test08_aiAgentBoundary_explicitCapabilitiesRequired_noImplicitAdmin() = runBlocking {
        // AI_AGENT attempting to calculate OEE (allowed for reading metrics)
        val res = useCases.getLatestOeeSummary(aiAgentPrincipal, machineIdA, customFactory)
        assertNull(res) // No metrics yet, but capability allowed cleanly

        // AI_AGENT attempting to create maintenance (requires STAFF/MANAGER/ADMIN management capability)
        val maintReq = CreateMaintenanceRecordRequestDto(
            title = "AI Agent Maintenance Attempt",
            maintenanceType = MaintenanceType.CORRECTIVE
        )
        try {
            useCases.createMaintenanceRecord(aiAgentPrincipal, machineIdA, maintReq, customFactory)
            fail("Expected ForbiddenException for AI_AGENT attempting management mutation without explicit role.")
        } catch (e: ForbiddenException) {
            assertTrue(e.message?.contains("Forbidden") == true || e.message?.contains("Access") == true)
        }
    }

    @Test
    fun test09_canonicalProductionWorkflow_regressionCheck() {
        val expectedSequence = listOf(
            ProductionStageType.DESIGN,
            ProductionStageType.APPROVAL,
            ProductionStageType.QC,
            ProductionStageType.ITEM_APPROVAL,
            ProductionStageType.CTP,
            ProductionStageType.PRINTING,
            ProductionStageType.LAMINATION,
            ProductionStageType.FOLDING,
            ProductionStageType.BINDING,
            ProductionStageType.FINAL_QC,
            ProductionStageType.PACKAGING,
            ProductionStageType.READY,
            ProductionStageType.DELIVERED
        )
        assertEquals(13, ProductionStageType.entries.size)
        assertEquals(expectedSequence, ProductionStageType.entries)
    }
}
