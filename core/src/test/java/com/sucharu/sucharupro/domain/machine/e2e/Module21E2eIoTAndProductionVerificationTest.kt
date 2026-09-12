package com.sucharu.sucharupro.domain.machine.e2e

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
import com.sucharu.sucharupro.data.datasource.FakeOrderDataSource
import com.sucharu.sucharupro.data.datasource.commercialcommitment.FakeCommercialCommitmentDataSource
import com.sucharu.sucharupro.data.datasource.machine.FakeMachineRegistryDataSource
import com.sucharu.sucharupro.data.datasource.machine.alerts.FakeMachineAlertDataSource
import com.sucharu.sucharupro.data.datasource.machine.events.FakeMachineEventDataSource
import com.sucharu.sucharupro.data.datasource.machine.maintenance.FakeMachineMaintenanceDataSource
import com.sucharu.sucharupro.data.datasource.machine.oee.FakeMachineOeeDataSource
import com.sucharu.sucharupro.data.datasource.machine.telemetry.FakeMachineTelemetryDataSource
import com.sucharu.sucharupro.data.datasource.printingquote.FakePrintingQuoteDataSource
import com.sucharu.sucharupro.data.datasource.productionexecution.FakeProductionExecutionDataSource
import com.sucharu.sucharupro.data.datasource.productionplanning.FakeProductionPlanningDataSource
import com.sucharu.sucharupro.data.persistence.postgres.PostgresRepositoryFactory
import com.sucharu.sucharupro.data.persistence.postgres.TenantContext
import com.sucharu.sucharupro.data.persistence.postgres.TransactionContext
import com.sucharu.sucharupro.data.persistence.postgres.TransactionManager
import com.sucharu.sucharupro.data.repository.NotificationRepositoryImpl
import com.sucharu.sucharupro.data.repository.OrderRepositoryImpl
import com.sucharu.sucharupro.data.repository.commercialcommitment.CommercialCommitmentRepositoryImpl
import com.sucharu.sucharupro.data.repository.machine.MachineRegistryRepositoryImpl
import com.sucharu.sucharupro.data.repository.machine.alerts.MachineAlertRepositoryImpl
import com.sucharu.sucharupro.data.repository.machine.events.MachineEventRepositoryImpl
import com.sucharu.sucharupro.data.repository.machine.maintenance.MachineMaintenanceRepositoryImpl
import com.sucharu.sucharupro.data.repository.machine.oee.MachineOeeRepositoryImpl
import com.sucharu.sucharupro.data.repository.machine.telemetry.MachineTelemetryRepositoryImpl
import com.sucharu.sucharupro.data.repository.printingquote.PrintingQuoteRepositoryImpl
import com.sucharu.sucharupro.data.repository.productionexecution.ProductionExecutionRepositoryImpl
import com.sucharu.sucharupro.data.repository.productionplanning.ProductionPlanningRepositoryImpl
import com.sucharu.sucharupro.domain.machine.MachineEquipment
import com.sucharu.sucharupro.domain.machine.MachineStatus
import com.sucharu.sucharupro.domain.machine.MachineType
import com.sucharu.sucharupro.domain.machine.alerts.MachineAlertStatus
import com.sucharu.sucharupro.domain.machine.alerts.MachineAlertType
import com.sucharu.sucharupro.domain.machine.alerts.MachineOperationalAlert
import com.sucharu.sucharupro.domain.machine.events.*
import com.sucharu.sucharupro.domain.machine.health.MachineOperationalState
import com.sucharu.sucharupro.domain.machine.maintenance.*
import com.sucharu.sucharupro.domain.machine.oee.MachineOeeCalculationRequest
import com.sucharu.sucharupro.domain.machine.oee.MachineOeeCalculator
import com.sucharu.sucharupro.domain.machine.telemetry.MachineTelemetryRecord
import com.sucharu.sucharupro.domain.machine.telemetry.TelemetryMetricType
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.common.Money
import com.sucharu.sucharupro.domain.model.notification.NotificationType
import com.sucharu.sucharupro.domain.model.order.Order
import com.sucharu.sucharupro.domain.model.order.OrderItem
import com.sucharu.sucharupro.domain.model.order.OrderStatusType
import com.sucharu.sucharupro.domain.model.production.ProductionStageType
import com.sucharu.sucharupro.domain.model.productionexecution.*
import com.sucharu.sucharupro.domain.model.productionplanning.ProductionJobSpecification
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
import com.sucharu.sucharupro.domain.service.machine.health.MachineStatusMonitoringService
import com.sucharu.sucharupro.domain.service.machine.health.MachineStatusMonitoringServiceImpl
import com.sucharu.sucharupro.domain.service.machine.maintenance.MachineMaintenanceService
import com.sucharu.sucharupro.domain.service.machine.maintenance.MachineMaintenanceServiceImpl
import com.sucharu.sucharupro.domain.service.machine.oee.MachineOeeService
import com.sucharu.sucharupro.domain.service.machine.oee.MachineOeeServiceImpl
import com.sucharu.sucharupro.domain.service.machine.telemetry.MachineTelemetryIngestionService
import com.sucharu.sucharupro.domain.service.machine.telemetry.MachineTelemetryIngestionServiceImpl
import com.sucharu.sucharupro.domain.service.productionexecution.ProductionExecutionServiceImpl
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

class Module21E2eIoTAndProductionVerificationTest {

    private lateinit var useCases: BackendUseCases
    private lateinit var customFactory: PostgresRepositoryFactory

    private lateinit var machineRepo: MachineRegistryRepositoryImpl
    private lateinit var telemetryService: MachineTelemetryIngestionServiceImpl
    private lateinit var healthService: MachineStatusMonitoringServiceImpl
    private lateinit var executionService: ProductionExecutionServiceImpl
    private lateinit var maintService: MachineMaintenanceServiceImpl
    private lateinit var faultService: MachineFaultEventServiceImpl
    private lateinit var downtimeService: MachineDowntimeServiceImpl
    private lateinit var alertService: MachineAlertServiceImpl
    private lateinit var oeeService: MachineOeeServiceImpl
    private lateinit var notifRepo: NotificationRepositoryImpl

    private val tenantA = "TENANT-E2E-A"
    private val tenantB = "TENANT-E2E-B"
    private val machineIdA = "E2E-MACHINE-01"
    private val jobIdA = "JOB-E2E-001"
    private val workOrderIdA = "WO-E2E-PRINT-01"

    private val staffPrincipal = AuthenticatedPrincipal(
        userId = "staff_e2e_01",
        projectId = tenantA,
        username = "staff_e2e",
        role = UserRole.STAFF
    )

    private val customerPrincipal = AuthenticatedPrincipal(
        userId = "cust_e2e_01",
        projectId = tenantA,
        username = "cust_e2e",
        role = UserRole.CUSTOMER
    )

    @Before
    fun setUp() = runBlocking {
        val machineDs = FakeMachineRegistryDataSource()
        machineRepo = MachineRegistryRepositoryImpl(machineDs)
        val machineService = MachineRegistryServiceImpl(machineRepo)

        val telemetryDs = FakeMachineTelemetryDataSource()
        val telemetryRepo = MachineTelemetryRepositoryImpl(telemetryDs)
        telemetryService = MachineTelemetryIngestionServiceImpl(telemetryRepo, machineRepo)

        healthService = MachineStatusMonitoringServiceImpl(machineRepo, telemetryRepo)

        val orderDs = FakeOrderDataSource()
        val orderRepo = OrderRepositoryImpl(orderDs)

        val execDs = FakeProductionExecutionDataSource()
        val execRepo = ProductionExecutionRepositoryImpl(execDs)

        val planningRepo = ProductionPlanningRepositoryImpl(FakeProductionPlanningDataSource())
        val commitmentRepo = CommercialCommitmentRepositoryImpl(FakeCommercialCommitmentDataSource())
        val quoteRepo = PrintingQuoteRepositoryImpl(FakePrintingQuoteDataSource())

        val maintDs = FakeMachineMaintenanceDataSource()
        val maintRepo = MachineMaintenanceRepositoryImpl(maintDs)
        maintService = MachineMaintenanceServiceImpl(maintRepo, machineRepo)

        val eventDs = FakeMachineEventDataSource()
        val eventRepo = MachineEventRepositoryImpl(eventDs)
        faultService = MachineFaultEventServiceImpl(eventRepo, machineRepo)
        downtimeService = MachineDowntimeServiceImpl(eventRepo, machineRepo)

        val notifDs = FakeNotificationDataSource()
        notifRepo = NotificationRepositoryImpl(notifDs)

        val alertDs = FakeMachineAlertDataSource()
        val alertRepo = MachineAlertRepositoryImpl(alertDs)
        alertService = MachineAlertServiceImpl(alertRepo, machineRepo, notifRepo)

        val oeeDs = FakeMachineOeeDataSource()
        val oeeRepo = MachineOeeRepositoryImpl(oeeDs)
        oeeService = MachineOeeServiceImpl(oeeRepo, machineRepo, eventRepo, execRepo)

        executionService = ProductionExecutionServiceImpl(
            executionRepository = execRepo,
            orderRepository = orderRepo,
            planningRepository = planningRepo,
            commitmentRepository = commitmentRepo,
            quoteRepository = quoteRepo,
            machineRegistryRepository = machineRepo,
            machineStatusMonitoringService = healthService
        )

        val fakeTxManager = object : TransactionManager {
            override suspend fun <T> inTransaction(tenantContext: TenantContext, block: suspend (TransactionContext) -> T): T {
                throw UnsupportedOperationException("Mock E2E test context")
            }
            override suspend fun <T> inReadOnly(tenantContext: TenantContext, block: suspend (TransactionContext) -> T): T {
                throw UnsupportedOperationException("Mock E2E test context")
            }
        }

        customFactory = object : PostgresRepositoryFactory(fakeTxManager) {
            override fun createMachineRegistryRepository(tenantId: String): MachineRegistryRepository = machineRepo
            override fun createMachineRegistryService(tenantId: String): MachineRegistryService = machineService
            override fun createMachineTelemetryIngestionRepository(tenantId: String): MachineTelemetryRepository = telemetryRepo
            override fun createMachineTelemetryIngestionService(tenantId: String): MachineTelemetryIngestionService = telemetryService
            override fun createMachineStatusMonitoringService(tenantId: String): MachineStatusMonitoringService = healthService
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

        // Seed Machine A
        val machineA = MachineEquipment(
            machineId = machineIdA,
            tenantId = tenantA,
            assetCode = "E2E-EQ-01",
            name = "Heidelberg Speedmaster XL 106 E2E",
            type = MachineType.PRINTING_PRESS,
            status = MachineStatus.AVAILABLE
        )
        machineRepo.saveMachine(machineA)

        // Seed Order & Production Job
        orderDs.insertOrder(
            Order(
                orderId = "ORD-E2E-001",
                orderNumber = "ORD-NUM-E2E-001",
                customerId = "CUST-001",
                status = OrderStatusType.CONFIRMED,
                items = listOf(
                    OrderItem(
                        itemId = "ITEM-01",
                        description = "E2E Printing Catalog",
                        quantity = 5000,
                        unitPrice = Money(BigDecimal("10.00"))
                    )
                ),
                createdAt = "2026-09-10T12:00:00Z",
                updatedAt = "2026-09-10T12:00:00Z"
            )
        )

        val now = System.currentTimeMillis()
        val job = ProductionJobExecution(
            executionJobId = jobIdA,
            tenantId = tenantA,
            projectId = tenantA,
            orderId = "ORD-E2E-001",
            orderNumber = "ORD-NUM-E2E-001",
            orderItemId = "ITEM-01",
            customerId = "CUST-001",
            quotationId = null,
            quotationVersionNumber = null,
            commercialCommitmentId = null,
            planningId = "PLN-E2E-001",
            planningVersion = 1,
            title = "E2E Production Job",
            specification = ProductionJobSpecification(
                specId = "SPEC-E2E-01",
                jobTitle = "E2E Production Job",
                productType = "CATALOG",
                orderedQuantity = 5000L,
                plannedQuantity = 5000L,
                finishedWidthMm = BigDecimal("210"),
                finishedHeightMm = BigDecimal("297"),
                substrateType = "ART_PAPER",
                substrateGsm = 150,
                parentSheetWidthMm = BigDecimal("635"),
                parentSheetHeightMm = BigDecimal("914"),
                pressSheetWidthMm = BigDecimal("635"),
                pressSheetHeightMm = BigDecimal("914"),
                printingMethod = "OFFSET",
                colorsFront = 4,
                colorsBack = 4,
                impositionUps = 8,
                specFingerprint = "SPEC-FP-01"
            ),
            status = ProductionJobExecutionStatus.RELEASED,
            currentStageType = ProductionStageType.PRINTING,
            plannedQuantity = BigDecimal("5000"),
            jobFingerprint = "FP-JOB-E2E-001",
            integrityHash = "HASH-JOB-E2E-001",
            createdAt = now,
            createdBy = "staff_e2e_01",
            updatedAt = now,
            workOrders = listOf(
                ProductionWorkOrder(
                    workOrderId = workOrderIdA,
                    executionJobId = jobIdA,
                    tenantId = tenantA,
                    sequenceNumber = 1,
                    stageType = ProductionStageType.PRINTING,
                    operationCode = "OP-PRINT",
                    operationName = "Offset Printing Operation",
                    targetWorkCenter = "WC-PRESS",
                    status = WorkOrderStatus.PENDING,
                    plannedQuantity = BigDecimal("5000")
                )
            )
        )
        execRepo.saveJobExecution(job)
        Unit
    }

    @Test
    fun testE2E_01_machineRegistration() = runBlocking {
        val machineRes = machineRepo.getMachineById(tenantA, machineIdA)
        assertTrue(machineRes is DomainResult.Success)
        val machine = (machineRes as DomainResult.Success).data
        assertNotNull(machine)
        assertEquals("E2E-EQ-01", machine?.assetCode)
        assertEquals(MachineStatus.AVAILABLE, machine?.status)
    }

    @Test
    fun testE2E_02_telemetryIngestion() = runBlocking {
        val telemetry = MachineTelemetryRecord(
            telemetryId = "TEL-E2E-01",
            tenantId = tenantA,
            machineId = machineIdA,
            metricType = TelemetryMetricType.SPEED,
            metricValue = BigDecimal("8500.00"),
            unit = "IMPRESSIONS_PER_HOUR",
            eventTimestamp = System.currentTimeMillis()
        )

        val result = telemetryService.ingestTelemetry(telemetry)
        assertTrue(result is DomainResult.Success)
        val ingested = (result as DomainResult.Success).data
        assertEquals("TEL-E2E-01", ingested.telemetryId)
    }

    @Test
    fun testE2E_03_healthMonitoring() = runBlocking {
        val healthRes = healthService.evaluateMachineHealth(tenantA, machineIdA)
        assertTrue(healthRes is DomainResult.Success)
        val snapshot = (healthRes as DomainResult.Success).data
        assertNotNull(snapshot)
        assertEquals(machineIdA, snapshot?.machineId)
    }

    @Test
    fun testE2E_04_productionMachineAssignment() = runBlocking {
        val assignRes = executionService.assignMachine(
            tenantId = tenantA,
            executionJobId = jobIdA,
            workOrderId = workOrderIdA,
            machineId = machineIdA,
            machineName = "Override Name",
            assignedBy = "staff_e2e_01"
        )
        assertTrue(assignRes is DomainResult.Success)
        val job = (assignRes as DomainResult.Success).data
        val wo = job.workOrders.first()
        assertEquals(machineIdA, wo.assignedMachineId)
        assertEquals("Heidelberg Speedmaster XL 106 E2E", wo.assignedMachineName)
    }

    @Test
    fun testE2E_05_productionExecution_stageStartAndCompletion() = runBlocking {
        // Assign & Start
        executionService.assignMachine(tenantA, jobIdA, workOrderIdA, machineIdA, "Heidelberg", "staff_e2e_01")
        val startRes = executionService.startStage(tenantA, jobIdA, workOrderIdA, "OPERATOR-01", machineIdA, "staff_e2e_01")
        assertTrue(startRes is DomainResult.Success)

        // Machine becomes IN_USE
        val machineInUse = (machineRepo.getMachineById(tenantA, machineIdA) as DomainResult.Success).data!!
        assertEquals(MachineStatus.IN_USE, machineInUse.status)

        // Complete
        val completeRes = executionService.completeStage(
            tenantId = tenantA,
            executionJobId = jobIdA,
            workOrderId = workOrderIdA,
            goodQuantity = BigDecimal("4950"),
            scrapQuantity = BigDecimal("50"),
            notes = "E2E Stage Completed",
            completedBy = "staff_e2e_01"
        )
        assertTrue(completeRes is DomainResult.Success)

        // Machine returned to AVAILABLE
        val machineAvailable = (machineRepo.getMachineById(tenantA, machineIdA) as DomainResult.Success).data!!
        assertEquals(MachineStatus.AVAILABLE, machineAvailable.status)
    }

    @Test
    fun testE2E_06_faultAndDowntime() = runBlocking {
        // Record Fault
        val fault = MachineFaultEvent(
            faultEventId = "FLT-E2E-01",
            tenantId = tenantA,
            machineId = machineIdA,
            faultType = "MECHANICAL",
            description = "E2E Feeder Jam",
            occurredAt = System.currentTimeMillis()
        )
        val faultRes = faultService.recordFault(fault)
        assertTrue(faultRes is DomainResult.Success)

        // Record Downtime
        val now = System.currentTimeMillis()
        val downtime = MachineDowntimeEvent(
            downtimeId = "DT-E2E-01",
            tenantId = tenantA,
            machineId = machineIdA,
            faultEventId = "FLT-E2E-01",
            reasonCategory = DowntimeReasonCategory.MACHINE_FAULT,
            status = DowntimeStatus.STARTED,
            startedAt = now - 3600000L
        )
        val startDtRes = downtimeService.startDowntime(downtime)
        assertTrue(startDtRes is DomainResult.Success)

        val endDtRes = downtimeService.endDowntime(tenantA, machineIdA, "DT-E2E-01", endedAt = now, actorId = "staff_e2e_01")
        assertTrue(endDtRes is DomainResult.Success)
        assertEquals(3600L, (endDtRes as DomainResult.Success).data.durationSeconds)
    }

    @Test
    fun testE2E_07_maintenanceLifecycleAndProductionGuard() = runBlocking {
        val record = MaintenanceRecord(
            recordId = "REC-E2E-01",
            tenantId = tenantA,
            machineId = machineIdA,
            title = "Scheduled Roller Maintenance",
            maintenanceType = MaintenanceType.PREVENTIVE
        )
        maintService.createRecord(record)

        // Start Maintenance -> Status becomes MAINTENANCE
        maintService.startMaintenance(tenantA, machineIdA, "REC-E2E-01", "TECH-01", "Kamal")
        assertEquals(MachineStatus.MAINTENANCE, (machineRepo.getMachineById(tenantA, machineIdA) as DomainResult.Success).data!!.status)

        // Attempt assignment or stage start while under maintenance -> Fails
        val startFailRes = executionService.startStage(tenantA, jobIdA, workOrderIdA, "OPERATOR-01", machineIdA, "staff_e2e_01")
        assertTrue(startFailRes is DomainResult.Error)

        // Complete Maintenance -> Status returns to AVAILABLE
        maintService.completeMaintenance(tenantA, machineIdA, "REC-E2E-01", "Maintenance complete", "Cleaned rollers", "TECH-01")
        assertEquals(MachineStatus.AVAILABLE, (machineRepo.getMachineById(tenantA, machineIdA) as DomainResult.Success).data!!.status)
    }

    @Test
    fun testE2E_08_performanceAndOeeCalculation() = runBlocking {
        val calcResult = MachineOeeCalculator.calculate(
            plannedProductionSeconds = 28800L,
            downtimeSeconds = 3600L,
            idealRateUnitsPerHour = BigDecimal("1000"),
            actualOutputUnits = BigDecimal("6300"),
            goodOutputUnits = BigDecimal("6000"),
            rejectedOutputUnits = BigDecimal("300")
        )

        assertEquals(BigDecimal("0.8750"), calcResult.availabilityRatio)
        assertEquals(BigDecimal("0.9000"), calcResult.performanceRatio)
        assertEquals(BigDecimal("0.9524"), calcResult.qualityRatio)
        assertEquals(BigDecimal("0.7500"), calcResult.oeeRatio)
    }

    @Test
    fun testE2E_09_alertAndModule10NotificationDispatch() = runBlocking {
        val alert = MachineOperationalAlert(
            alertId = "ALT-E2E-01",
            tenantId = tenantA,
            machineId = machineIdA,
            alertType = MachineAlertType.TELEMETRY_ABNORMAL,
            title = "Overheat Warning",
            description = "Main motor temp 98C",
            createdBy = "staff_e2e_01"
        )

        val alertRes = alertService.raiseAlert(alert)
        assertTrue(alertRes is DomainResult.Success)
        val raised = (alertRes as DomainResult.Success).data
        assertNotNull(raised.notificationId)

        // Verify Module 10 Notification creation
        val notifRes = notifRepo.getNotification(tenantA, raised.notificationId!!, "staff_e2e_01", com.sucharu.sucharupro.domain.model.user.UserRole.STAFF)
        assertTrue(notifRes is DomainResult.Success)
        assertEquals(NotificationType.MACHINE_TELEMETRY_ALERT, (notifRes as DomainResult.Success).data.notificationType)
    }

    @Test
    fun testE2E_10_alertDeduplication_preventsSpam() = runBlocking {
        val alert1 = MachineOperationalAlert(
            alertId = "ALT-DEDUP-01",
            tenantId = tenantA,
            machineId = machineIdA,
            alertType = MachineAlertType.HEALTH_CRITICAL,
            title = "Critical Motor Anomaly",
            description = "Inverter fault signal",
            correlationKey = "$machineIdA:FAULT:INVERTER",
            createdBy = "staff_e2e_01"
        )
        val res1 = alertService.raiseAlert(alert1)
        assertTrue(res1 is DomainResult.Success)

        // Duplicate alert attempt
        val alert2 = alert1.copy(alertId = "ALT-DEDUP-02")
        val res2 = alertService.raiseAlert(alert2)
        assertTrue(res2 is DomainResult.Success)

        // Must return existing active alert ID
        assertEquals((res1 as DomainResult.Success).data.alertId, (res2 as DomainResult.Success).data.alertId)
    }

    @Test
    fun testE2E_11_auditIntegrity() = runBlocking {
        val record = MaintenanceRecord(
            recordId = "REC-AUDIT-01",
            tenantId = tenantA,
            machineId = machineIdA,
            title = "Audit Test Maintenance",
            maintenanceType = MaintenanceType.PREVENTIVE
        )
        maintService.createRecord(record)
        maintService.startMaintenance(tenantA, machineIdA, "REC-AUDIT-01", "TECH-01", "Kamal")

        val historyRes = maintService.listServiceHistory(tenantA, machineIdA)
        assertTrue(historyRes is DomainResult.Success)
        val logs = (historyRes as DomainResult.Success).data
        assertTrue(logs.any { it.recordId == "REC-AUDIT-01" && it.actionType == "STARTED" })
    }

    @Test
    fun testE2E_12_tenantIsolation_crossTenantRejection() = runBlocking {
        val alertB = MachineOperationalAlert(
            alertId = "ALT-CROSS-01",
            tenantId = tenantB,
            machineId = machineIdA, // Cross-tenant target
            alertType = MachineAlertType.WARNING,
            title = "Cross Tenant Attempt",
            description = "Forbidden"
        )

        val result = alertService.raiseAlert(alertB)
        assertTrue(result is DomainResult.Error)
        val err = result as DomainResult.Error
        assertTrue(err.message.contains("not found in registry"))
    }

    @Test
    fun testE2E_13_roleAndCapabilityBoundaries_customerForbidden() = runBlocking {
        val maintReq = CreateMaintenanceRecordRequestDto(
            title = "Customer Forbidden Action",
            maintenanceType = MaintenanceType.CORRECTIVE
        )
        try {
            useCases.createMaintenanceRecord(customerPrincipal, machineIdA, maintReq, customFactory)
            fail("Expected ForbiddenException for Customer role.")
        } catch (e: ForbiddenException) {
            assertTrue(e.message?.contains("Forbidden") == true || e.message?.contains("Access") == true)
        }
    }

    @Test
    fun testE2E_14_idorDefense_machineIdSubstitution() = runBlocking {
        val req = CalculateOeeRequestDto(
            periodStart = System.currentTimeMillis() - 3600000L,
            periodEnd = System.currentTimeMillis()
        )
        try {
            useCases.calculateAndSaveOee(staffPrincipal, "MAC-GHOST-IDOR", req, customFactory)
            fail("Expected IllegalArgumentException for non-existent/cross-tenant machine ID substitution.")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message?.contains("not found in registry") == true)
        }
    }

    @Test
    fun testE2E_15_productionToOeeConsistency() = runBlocking {
        // Complete stage with 4950 good and 50 scrap
        executionService.assignMachine(tenantA, jobIdA, workOrderIdA, machineIdA, "Heidelberg", "staff_e2e_01")
        executionService.startStage(tenantA, jobIdA, workOrderIdA, "OPERATOR-01", machineIdA, "staff_e2e_01")
        executionService.completeStage(tenantA, jobIdA, workOrderIdA, BigDecimal("4950"), BigDecimal("50"), "Stage done", "staff_e2e_01")

        val req = MachineOeeCalculationRequest(
            tenantId = tenantA,
            machineId = machineIdA,
            periodStart = System.currentTimeMillis() - 28800000L,
            periodEnd = System.currentTimeMillis()
        )

        val oeeRes = oeeService.calculateAndSaveOee(req, "staff_e2e_01")
        assertTrue(oeeRes is DomainResult.Success)
        val metrics = (oeeRes as DomainResult.Success).data
        assertEquals(BigDecimal("5000.00"), metrics.actualOutputUnits)
        assertEquals(BigDecimal("4950.00"), metrics.goodOutputUnits)
        assertEquals(BigDecimal("50.00"), metrics.rejectedOutputUnits)
        assertEquals(BigDecimal("0.9900"), metrics.qualityRatio)
    }

    @Test
    fun testE2E_16_telemetryToHealthToAlertChain() = runBlocking {
        // Ingest Fault Telemetry Code (50 = FAULT)
        val telemetry = MachineTelemetryRecord(
            telemetryId = "TEL-FAULT-01",
            tenantId = tenantA,
            machineId = machineIdA,
            metricType = TelemetryMetricType.OPERATIONAL_STATE,
            metricValue = BigDecimal("55.0"),
            eventTimestamp = System.currentTimeMillis()
        )
        telemetryService.ingestTelemetry(telemetry)

        // Evaluate Health -> State becomes FAULT
        val healthRes = healthService.evaluateMachineHealth(tenantA, machineIdA)
        assertTrue(healthRes is DomainResult.Success)
        assertEquals(MachineOperationalState.FAULT, (healthRes as DomainResult.Success).data?.operationalState)

        // Raise Alert for Fault State
        val alert = MachineOperationalAlert(
            alertId = "ALT-HEALTH-01",
            tenantId = tenantA,
            machineId = machineIdA,
            alertType = MachineAlertType.HEALTH_CRITICAL,
            title = "Machine Health Critical",
            description = "Machine operational state is FAULT"
        )
        val alertRes = alertService.raiseAlert(alert)
        assertTrue(alertRes is DomainResult.Success)
        assertNotNull((alertRes as DomainResult.Success).data.notificationId)
    }

    @Test
    fun testE2E_17_faultToDowntimeToAvailabilityChain() = runBlocking {
        val start = System.currentTimeMillis() - 28800000L
        val end = System.currentTimeMillis()

        // Fault -> Downtime (2 hours = 7200s)
        val downtime = MachineDowntimeEvent(
            downtimeId = "DT-CHAIN-01",
            tenantId = tenantA,
            machineId = machineIdA,
            reasonCategory = DowntimeReasonCategory.MACHINE_FAULT,
            status = DowntimeStatus.ENDED,
            startedAt = start,
            endedAt = start + 7200000L,
            durationSeconds = 7200L
        )
        downtimeService.startDowntime(downtime)

        val oeeReq = MachineOeeCalculationRequest(
            tenantId = tenantA,
            machineId = machineIdA,
            periodStart = start,
            periodEnd = end,
            customPlannedProductionSeconds = 28800L
        )
        val oeeRes = oeeService.calculateAndSaveOee(oeeReq)
        assertTrue(oeeRes is DomainResult.Success)
        val metrics = (oeeRes as DomainResult.Success).data
        assertEquals(7200L, metrics.downtimeSeconds)
        assertEquals(21600L, metrics.runTimeSeconds)
        assertEquals(BigDecimal("0.7500"), metrics.availabilityRatio)
    }

    @Test
    fun testE2E_18_maintenanceToStatusToProductionGuardChain() = runBlocking {
        val record = MaintenanceRecord(
            recordId = "REC-GUARD-01",
            tenantId = tenantA,
            machineId = machineIdA,
            title = "Urgent Guard Maintenance",
            maintenanceType = MaintenanceType.CORRECTIVE
        )
        maintService.createRecord(record)
        maintService.startMaintenance(tenantA, machineIdA, "REC-GUARD-01", "TECH-01", "Kamal")

        // Status is MAINTENANCE
        assertEquals(MachineStatus.MAINTENANCE, (machineRepo.getMachineById(tenantA, machineIdA) as DomainResult.Success).data!!.status)

        // Production assignment fails for faulted/maintenance machine
        val assignRes = executionService.assignMachine(tenantA, jobIdA, workOrderIdA, machineIdA, "Heidelberg", "staff_e2e_01")
        assertTrue(assignRes is DomainResult.Error)
    }

    @Test
    fun testE2E_19_alertToAuditChain() = runBlocking {
        val alert = MachineOperationalAlert(
            alertId = "ALT-AUDIT-01",
            tenantId = tenantA,
            machineId = machineIdA,
            alertType = MachineAlertType.WARNING,
            title = "Audit Chain Warning",
            description = "Warning for audit trail"
        )
        alertService.raiseAlert(alert)
        alertService.acknowledgeAlert(tenantA, machineIdA, "ALT-AUDIT-01", "staff_e2e_01")

        val alertRes = alertService.getAlertDetails(tenantA, "ALT-AUDIT-01")
        assertTrue(alertRes is DomainResult.Success)
        val details = (alertRes as DomainResult.Success).data
        assertEquals(MachineAlertStatus.ACKNOWLEDGED, details?.status)
        assertEquals("staff_e2e_01", details?.acknowledgedBy)
    }

    @Test
    fun testE2E_20_consolidatedCompleteModule21EcosystemFlow() = runBlocking {
        // 1. Verify Machine Registered
        val machine = (machineRepo.getMachineById(tenantA, machineIdA) as DomainResult.Success).data!!
        assertEquals(MachineStatus.AVAILABLE, machine.status)

        // 2. Ingest Telemetry
        telemetryService.ingestTelemetry(
            MachineTelemetryRecord(
                telemetryId = "TEL-CONS-01",
                tenantId = tenantA,
                machineId = machineIdA,
                metricType = TelemetryMetricType.SPEED,
                metricValue = BigDecimal("10000.00"),
                eventTimestamp = System.currentTimeMillis()
            )
        )

        // 3. Evaluate Health
        val health = (healthService.evaluateMachineHealth(tenantA, machineIdA) as DomainResult.Success).data!!
        assertNotNull(health)

        // 4. Production Execution & Stage Complete
        executionService.assignMachine(tenantA, jobIdA, workOrderIdA, machineIdA, "Heidelberg", "staff_e2e_01")
        executionService.startStage(tenantA, jobIdA, workOrderIdA, "OPERATOR-01", machineIdA, "staff_e2e_01")
        executionService.completeStage(tenantA, jobIdA, workOrderIdA, BigDecimal("5000"), BigDecimal("0"), "Done", "staff_e2e_01")

        // 5. OEE Calculation
        val oeeRes = oeeService.calculateAndSaveOee(
            MachineOeeCalculationRequest(
                tenantId = tenantA,
                machineId = machineIdA,
                periodStart = System.currentTimeMillis() - 28800000L,
                periodEnd = System.currentTimeMillis()
            ),
            actorId = "staff_e2e_01"
        )
        assertTrue(oeeRes is DomainResult.Success)
        val oee = (oeeRes as DomainResult.Success).data
        assertEquals(BigDecimal("1.0000"), oee.qualityRatio)

        // 6. Raise Alert
        val alertRes = alertService.raiseAlert(
            MachineOperationalAlert(
                alertId = "ALT-CONS-01",
                tenantId = tenantA,
                machineId = machineIdA,
                alertType = MachineAlertType.WARNING,
                title = "Consolidated Flow Completed",
                description = "E2E complete flow successfully verified"
            )
        )
        assertTrue(alertRes is DomainResult.Success)
        assertNotNull((alertRes as DomainResult.Success).data.notificationId)

        // 7. Canonical Production Workflow Regression Check
        assertEquals(13, ProductionStageType.entries.size)
    }
}
