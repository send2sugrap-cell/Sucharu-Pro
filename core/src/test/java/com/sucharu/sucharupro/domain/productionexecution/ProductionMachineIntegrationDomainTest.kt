package com.sucharu.sucharupro.domain.productionexecution

import com.sucharu.sucharupro.data.datasource.FakeOrderDataSource
import com.sucharu.sucharupro.data.datasource.commercialcommitment.FakeCommercialCommitmentDataSource
import com.sucharu.sucharupro.data.datasource.machine.FakeMachineRegistryDataSource
import com.sucharu.sucharupro.data.datasource.machine.telemetry.FakeMachineTelemetryDataSource
import com.sucharu.sucharupro.data.datasource.printingquote.FakePrintingQuoteDataSource
import com.sucharu.sucharupro.data.datasource.productionexecution.FakeProductionExecutionDataSource
import com.sucharu.sucharupro.data.datasource.productionplanning.FakeProductionPlanningDataSource
import com.sucharu.sucharupro.data.repository.OrderRepositoryImpl
import com.sucharu.sucharupro.data.repository.commercialcommitment.CommercialCommitmentRepositoryImpl
import com.sucharu.sucharupro.data.repository.machine.MachineRegistryRepositoryImpl
import com.sucharu.sucharupro.data.repository.machine.telemetry.MachineTelemetryRepositoryImpl
import com.sucharu.sucharupro.data.repository.printingquote.PrintingQuoteRepositoryImpl
import com.sucharu.sucharupro.data.repository.productionexecution.ProductionExecutionRepositoryImpl
import com.sucharu.sucharupro.data.repository.productionplanning.ProductionPlanningRepositoryImpl
import com.sucharu.sucharupro.domain.machine.MachineEquipment
import com.sucharu.sucharupro.domain.machine.MachineStatus
import com.sucharu.sucharupro.domain.machine.MachineType
import com.sucharu.sucharupro.domain.machine.health.MachineStatusMonitoringServiceImpl
import com.sucharu.sucharupro.domain.machine.telemetry.MachineTelemetryRecord
import com.sucharu.sucharupro.domain.machine.telemetry.TelemetryMetricType
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.common.Money
import com.sucharu.sucharupro.domain.model.order.*
import com.sucharu.sucharupro.domain.model.production.ProductionStageType
import com.sucharu.sucharupro.domain.model.productionexecution.*
import com.sucharu.sucharupro.domain.model.productionplanning.ProductionJobSpecification
import com.sucharu.sucharupro.domain.service.productionexecution.ProductionExecutionServiceImpl
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

class ProductionMachineIntegrationDomainTest {

    private lateinit var machineDataSource: FakeMachineRegistryDataSource
    private lateinit var machineRepo: MachineRegistryRepositoryImpl
    private lateinit var telemetryDataSource: FakeMachineTelemetryDataSource
    private lateinit var telemetryRepo: MachineTelemetryRepositoryImpl
    private lateinit var healthService: MachineStatusMonitoringServiceImpl

    private lateinit var orderDataSource: FakeOrderDataSource
    private lateinit var orderRepo: OrderRepositoryImpl

    private lateinit var executionDataSource: FakeProductionExecutionDataSource
    private lateinit var executionRepo: ProductionExecutionRepositoryImpl

    private lateinit var planningRepo: ProductionPlanningRepositoryImpl
    private lateinit var commitmentRepo: CommercialCommitmentRepositoryImpl
    private lateinit var quoteRepo: PrintingQuoteRepositoryImpl

    private lateinit var executionService: ProductionExecutionServiceImpl

    private val tenantId = "TENANT-001"
    private val machineId = "MAC-PRESS-001"
    private val jobId = "JOB-001"
    private val workOrderId = "WO-PRINT-01"

    @Before
    fun setUp() = runBlocking {
        machineDataSource = FakeMachineRegistryDataSource()
        machineRepo = MachineRegistryRepositoryImpl(machineDataSource)

        telemetryDataSource = FakeMachineTelemetryDataSource()
        telemetryRepo = MachineTelemetryRepositoryImpl(telemetryDataSource)

        healthService = MachineStatusMonitoringServiceImpl(machineRepo, telemetryRepo)

        orderDataSource = FakeOrderDataSource()
        orderRepo = OrderRepositoryImpl(orderDataSource)

        executionDataSource = FakeProductionExecutionDataSource()
        executionRepo = ProductionExecutionRepositoryImpl(executionDataSource)

        planningRepo = ProductionPlanningRepositoryImpl(FakeProductionPlanningDataSource())
        commitmentRepo = CommercialCommitmentRepositoryImpl(FakeCommercialCommitmentDataSource())
        quoteRepo = PrintingQuoteRepositoryImpl(FakePrintingQuoteDataSource())

        executionService = ProductionExecutionServiceImpl(
            executionRepository = executionRepo,
            orderRepository = orderRepo,
            planningRepository = planningRepo,
            commitmentRepository = commitmentRepo,
            quoteRepository = quoteRepo,
            machineRegistryRepository = machineRepo,
            machineStatusMonitoringService = healthService
        )

        val activeMachine = MachineEquipment(
            machineId = machineId,
            tenantId = tenantId,
            assetCode = "EQ-PRESS-01",
            name = "Heidelberg Speedmaster XL 106",
            type = MachineType.PRINTING_PRESS,
            status = MachineStatus.AVAILABLE
        )
        machineRepo.saveMachine(activeMachine)

        // Seed order
        val order = Order(
            orderId = "ORD-001",
            orderNumber = "ORD-NUM-001",
            customerId = "CUST-001",
            status = OrderStatusType.CONFIRMED,
            items = listOf(
                OrderItem(
                    itemId = "ITEM-01",
                    description = "Catalog Printing",
                    quantity = 5000,
                    unitPrice = Money(BigDecimal("10.00"))
                )
            ),
            createdAt = "2026-09-10T12:00:00Z",
            updatedAt = "2026-09-10T12:00:00Z"
        )
        orderDataSource.insertOrder(order)

        val now = System.currentTimeMillis()
        val job = ProductionJobExecution(
            executionJobId = jobId,
            tenantId = tenantId,
            projectId = tenantId,
            orderId = "ORD-001",
            orderNumber = "ORD-NUM-001",
            orderItemId = "ITEM-01",
            customerId = "CUST-001",
            quotationId = null,
            quotationVersionNumber = null,
            commercialCommitmentId = null,
            planningId = "PLN-001",
            planningVersion = 1,
            title = "Test Production Job",
            specification = ProductionJobSpecification(
                specId = "SPEC-01",
                jobTitle = "Test Production Job",
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
            jobFingerprint = "FP-JOB-001",
            integrityHash = "HASH-JOB-001",
            createdAt = now,
            createdBy = "USR-STAFF",
            updatedAt = now,
            workOrders = listOf(
                ProductionWorkOrder(
                    workOrderId = workOrderId,
                    executionJobId = jobId,
                    tenantId = tenantId,
                    sequenceNumber = 1,
                    stageType = ProductionStageType.PRINTING,
                    operationCode = "OP-PRINT",
                    operationName = "Printing Operation",
                    targetWorkCenter = "WC-PRESS",
                    status = WorkOrderStatus.PENDING,
                    plannedQuantity = BigDecimal("5000")
                )
            )
        )
        executionRepo.saveJobExecution(job)
        Unit
    }

    @Test
    fun test01_assignMachine_success_resolvesCanonicalName() = runBlocking {
        val res = executionService.assignMachine(
            tenantId = tenantId,
            executionJobId = jobId,
            workOrderId = workOrderId,
            machineId = machineId,
            machineName = "Old Name Override",
            assignedBy = "USR-STAFF"
        )

        assertTrue(res is DomainResult.Success)
        val job = (res as DomainResult.Success).data
        val wo = job.workOrders.first()
        assertEquals(machineId, wo.assignedMachineId)
        assertEquals("Heidelberg Speedmaster XL 106", wo.assignedMachineName)
    }

    @Test
    fun test02_assignMachine_nonExistentMachine_fails() = runBlocking {
        val res = executionService.assignMachine(
            tenantId = tenantId,
            executionJobId = jobId,
            workOrderId = workOrderId,
            machineId = "MAC-GHOST-999",
            machineName = "Ghost Machine",
            assignedBy = "USR-STAFF"
        )

        assertTrue(res is DomainResult.Error)
        val err = res as DomainResult.Error
        assertTrue(err.message.contains("not found in registry"))
    }

    @Test
    fun test03_assignMachine_crossTenant_fails() = runBlocking {
        val machineB = MachineEquipment(
            machineId = "MAC-TENANT-B",
            tenantId = "TENANT-B",
            assetCode = "EQ-B",
            name = "Tenant B Machine",
            type = MachineType.PRINTING_PRESS
        )
        machineRepo.saveMachine(machineB)

        val res = executionService.assignMachine(
            tenantId = tenantId,
            executionJobId = jobId,
            workOrderId = workOrderId,
            machineId = "MAC-TENANT-B",
            machineName = "Tenant B Machine",
            assignedBy = "USR-STAFF"
        )

        assertTrue(res is DomainResult.Error)
        val err = res as DomainResult.Error
        assertTrue(err.message.contains("not found in registry") || err.message.contains("Cross-tenant"))
    }

    @Test
    fun test04_assignMachine_decommissioned_fails() = runBlocking {
        val decommMachine = MachineEquipment(
            machineId = "MAC-DECOMM-01",
            tenantId = tenantId,
            assetCode = "EQ-OLD-01",
            name = "Decommissioned Press",
            type = MachineType.PRINTING_PRESS,
            status = MachineStatus.DECOMMISSIONED
        )
        machineRepo.saveMachine(decommMachine)

        val res = executionService.assignMachine(
            tenantId = tenantId,
            executionJobId = jobId,
            workOrderId = workOrderId,
            machineId = "MAC-DECOMM-01",
            machineName = "Decommissioned Press",
            assignedBy = "USR-STAFF"
        )

        assertTrue(res is DomainResult.Error)
        val err = res as DomainResult.Error
        assertTrue(err.message.contains("decommissioned"))
    }

    @Test
    fun test05_assignMachine_faultState_fails() = runBlocking {
        // Record telemetry code 50 = FAULT state
        telemetryRepo.saveTelemetryRecord(
            MachineTelemetryRecord(
                telemetryId = "TEL-FAULT-01",
                tenantId = tenantId,
                machineId = machineId,
                metricType = TelemetryMetricType.OPERATIONAL_STATE,
                metricValue = BigDecimal("55"),
                eventTimestamp = System.currentTimeMillis()
            )
        )

        val res = executionService.assignMachine(
            tenantId = tenantId,
            executionJobId = jobId,
            workOrderId = workOrderId,
            machineId = machineId,
            machineName = "Heidelberg Speedmaster XL 106",
            assignedBy = "USR-STAFF"
        )

        assertTrue(res is DomainResult.Error)
        val err = res as DomainResult.Error
        assertTrue(err.message.contains("FAULT state"))
    }

    @Test
    fun test06_startStage_updatesMachineStatusToInUse() = runBlocking {
        // Assign machine first
        executionService.assignMachine(tenantId, jobId, workOrderId, machineId, "Heidelberg", "USR-STAFF")

        // Start stage
        val startRes = executionService.startStage(tenantId, jobId, workOrderId, "OPERATOR-01", machineId, "USR-STAFF")
        assertTrue(startRes is DomainResult.Success)

        // Verify machine status in registry updated to IN_USE
        val machine = (machineRepo.getMachineById(tenantId, machineId) as DomainResult.Success).data!!
        assertEquals(MachineStatus.IN_USE, machine.status)
    }

    @Test
    fun test07_completeStage_updatesMachineStatusToAvailable() = runBlocking {
        // Assign and Start stage
        executionService.assignMachine(tenantId, jobId, workOrderId, machineId, "Heidelberg", "USR-STAFF")
        executionService.startStage(tenantId, jobId, workOrderId, "OPERATOR-01", machineId, "USR-STAFF")

        // Complete stage
        val completeRes = executionService.completeStage(
            tenantId = tenantId,
            executionJobId = jobId,
            workOrderId = workOrderId,
            goodQuantity = BigDecimal("5000"),
            scrapQuantity = BigDecimal("50"),
            notes = "Completed smoothly",
            completedBy = "USR-STAFF"
        )
        assertTrue(completeRes is DomainResult.Success)

        // Verify machine status in registry updated back to AVAILABLE
        val machine = (machineRepo.getMachineById(tenantId, machineId) as DomainResult.Success).data!!
        assertEquals(MachineStatus.AVAILABLE, machine.status)
    }

    @Test
    fun test08_canonicalProductionStageWorkflow_regressionCheck() {
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
