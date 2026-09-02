package com.sucharu.sucharupro.domain.service.productionscheduling

import com.sucharu.sucharupro.data.datasource.FakeOrderDataSource
import com.sucharu.sucharupro.data.datasource.productionexecution.FakeProductionExecutionDataSource
import com.sucharu.sucharupro.data.datasource.productionplanning.FakeProductionPlanningDataSource
import com.sucharu.sucharupro.data.datasource.productionscheduling.FakeProductionSchedulingDataSource
import com.sucharu.sucharupro.data.repository.OrderRepositoryImpl
import com.sucharu.sucharupro.data.repository.productionexecution.ProductionExecutionRepositoryImpl
import com.sucharu.sucharupro.data.repository.productionplanning.ProductionPlanningRepositoryImpl
import com.sucharu.sucharupro.data.repository.productionscheduling.ProductionSchedulingRepositoryImpl
import com.sucharu.sucharupro.domain.model.order.OrderPriority
import com.sucharu.sucharupro.domain.model.production.ProductionStageType
import com.sucharu.sucharupro.domain.model.productionexecution.ProductionJobExecution
import com.sucharu.sucharupro.domain.model.productionexecution.ProductionJobExecutionStatus
import com.sucharu.sucharupro.domain.model.productionexecution.ProductionWorkOrder
import com.sucharu.sucharupro.domain.model.productionplanning.ProductionJobSpecification
import com.sucharu.sucharupro.domain.model.productionscheduling.DispatchStatus
import com.sucharu.sucharupro.domain.model.productionscheduling.ScheduleStatus
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

class ProductionSchedulingServiceTest {

    private lateinit var schedulingDataSource: FakeProductionSchedulingDataSource
    private lateinit var executionDataSource: FakeProductionExecutionDataSource
    private lateinit var orderDataSource: FakeOrderDataSource
    private lateinit var planningDataSource: FakeProductionPlanningDataSource
    private lateinit var schedulingService: ProductionSchedulingServiceImpl

    private val tenantId = "tenant-gamma"

    private fun createJob(): ProductionJobExecution {
        val spec = ProductionJobSpecification(
            specId = "SPEC-G-001",
            jobTitle = "Flyer Job",
            productType = "FLYER",
            orderedQuantity = 1000L,
            plannedQuantity = 1050L,
            finishedWidthMm = BigDecimal("148.0000"),
            finishedHeightMm = BigDecimal("210.0000"),
            substrateType = "ART_PAPER",
            substrateGsm = 120,
            substrateBrand = null,
            parentSheetWidthMm = BigDecimal("640.0000"),
            parentSheetHeightMm = BigDecimal("900.0000"),
            pressSheetWidthMm = BigDecimal("640.0000"),
            pressSheetHeightMm = BigDecimal("450.0000"),
            impositionUps = 4,
            printingMethod = "DIGITAL",
            colorsFront = 4,
            colorsBack = 4,
            specFingerprint = "SPEC-FP-G-001"
        )

        val workOrders = listOf(
            ProductionWorkOrder(
                workOrderId = "WO-G-1",
                executionJobId = "JOB-G-001",
                tenantId = tenantId,
                sequenceNumber = 1,
                stageType = ProductionStageType.DESIGN,
                operationCode = "PREPRESS",
                operationName = "Digital Artwork Preflight",
                targetWorkCenter = "PREPRESS_DESK",
                estimatedSetupMinutes = 10,
                estimatedRunMinutes = 10,
                plannedQuantity = BigDecimal("1050.0000")
            ),
            ProductionWorkOrder(
                workOrderId = "WO-G-2",
                executionJobId = "JOB-G-001",
                tenantId = tenantId,
                sequenceNumber = 2,
                stageType = ProductionStageType.PRINTING,
                operationCode = "DIGITAL_PRINT",
                operationName = "Digital Production Run",
                targetWorkCenter = "DIGITAL_PRESS_ROOM",
                estimatedSetupMinutes = 15,
                estimatedRunMinutes = 20,
                plannedQuantity = BigDecimal("1050.0000")
            ),
            ProductionWorkOrder(
                workOrderId = "WO-G-3",
                executionJobId = "JOB-G-001",
                tenantId = tenantId,
                sequenceNumber = 3,
                stageType = ProductionStageType.PACKAGING,
                operationCode = "PACK",
                operationName = "Wrap & Box",
                targetWorkCenter = "PACKAGING_ROOM",
                estimatedSetupMinutes = 5,
                estimatedRunMinutes = 10,
                plannedQuantity = BigDecimal("1050.0000")
            )
        )

        return ProductionJobExecution(
            executionJobId = "JOB-G-001",
            tenantId = tenantId,
            projectId = tenantId,
            orderId = "ORD-G-001",
            orderNumber = "SO-2026-G01",
            orderItemId = "ITEM-G-001",
            customerId = "CUST-G-001",
            quotationId = "Q-G-001",
            quotationVersionNumber = 1,
            commercialCommitmentId = "CC-G-001",
            planningId = "PLAN-G-001",
            planningVersion = 1,
            title = "Flyer Job 1000 pcs",
            priority = OrderPriority.NORMAL,
            status = ProductionJobExecutionStatus.READY,
            specification = spec,
            plannedQuantity = BigDecimal("1050.0000"),
            workOrders = workOrders,
            jobFingerprint = "FP-JOB-G-001",
            integrityHash = "HASH-JOB-G-001",
            createdAt = 1700000000000L,
            createdBy = "planner@sucharu.com",
            updatedAt = 1700000000000L
        )
    }

    @Before
    fun setup() {
        schedulingDataSource = FakeProductionSchedulingDataSource()
        executionDataSource = FakeProductionExecutionDataSource()
        orderDataSource = FakeOrderDataSource(emptyList())
        planningDataSource = FakeProductionPlanningDataSource()

        val schedulingRepo = ProductionSchedulingRepositoryImpl(schedulingDataSource)
        val executionRepo = ProductionExecutionRepositoryImpl(executionDataSource)
        val orderRepo = OrderRepositoryImpl(orderDataSource)
        val planningRepo = ProductionPlanningRepositoryImpl(planningDataSource)

        schedulingService = ProductionSchedulingServiceImpl(
            schedulingRepository = schedulingRepo,
            executionRepository = executionRepo,
            orderRepository = orderRepo,
            planningRepository = planningRepo
        )
    }

    @Test
    fun testCreateApproveAndDispatchLifecycle() = runBlocking {
        val job = createJob()
        executionDataSource.saveJobExecution(job)

        // 1. Create schedule
        val schedule = schedulingService.createScheduleForJob(
            tenantId = tenantId,
            executionJobId = job.executionJobId,
            actor = "planner"
        )
        assertNotNull(schedule)
        assertEquals(ScheduleStatus.PROPOSED, schedule.status)
        assertEquals(3, schedule.slots.size)

        // 2. Approve schedule
        val approved = schedulingService.approveSchedule(
            tenantId = tenantId,
            scheduleId = schedule.scheduleId,
            actor = "manager"
        )
        assertEquals(ScheduleStatus.APPROVED, approved.status)
        assertNotNull(approved.approvedAt)

        // Verify underlying job updated to SCHEDULED
        val updatedJob = executionDataSource.getJobExecutionById(tenantId, job.executionJobId)
        assertEquals(ProductionJobExecutionStatus.SCHEDULED, updatedJob?.status)

        // Verify dispatch queue populated
        val queue = schedulingService.listDispatchQueue(tenantId, schedule.scheduleId)
        assertEquals(3, queue.size)
        val firstItem = queue[0]
        assertEquals(DispatchStatus.READY, firstItem.dispatchStatus)

        // 3. Dispatch item
        val dispatchedItem = schedulingService.dispatchQueueItem(tenantId, firstItem.queueItemId, "dispatcher")
        assertEquals(DispatchStatus.DISPATCHED, dispatchedItem.dispatchStatus)
        assertNotNull(dispatchedItem.dispatchedAt)

        // 4. Acknowledge item on shop floor
        val ackedItem = schedulingService.acknowledgeQueueItem(tenantId, firstItem.queueItemId, "operator-rahim")
        assertEquals(DispatchStatus.ACKNOWLEDGED, ackedItem.dispatchStatus)
        assertNotNull(ackedItem.acknowledgedAt)
    }

    @Test
    fun testReconciliationAndAiHandoff() = runBlocking {
        val job = createJob()
        executionDataSource.saveJobExecution(job)

        val schedule = schedulingService.createScheduleForJob(
            tenantId = tenantId,
            executionJobId = job.executionJobId,
            actor = "planner"
        )
        schedulingService.approveSchedule(tenantId, schedule.scheduleId, "manager")

        // 1. Reconcile
        val recon = schedulingService.reconcileSchedule(tenantId, schedule.scheduleId)
        assertTrue(recon.isFullyReconciled)
        assertTrue(recon.executionJobMatch)
        assertTrue(recon.slotsComplete)
        assertTrue(recon.zeroBlockingConflicts)

        // 2. AI Handoff Contract
        val handoff = schedulingService.getAiHandoffContract(tenantId, schedule.scheduleId)
        assertEquals("1.0.0", handoff.contractVersion)
        assertEquals(schedule.scheduleId, handoff.scheduleId)
        assertEquals(3, handoff.slotsCount)
        assertTrue(handoff.isFullyReconciled)
        assertTrue(handoff.integrityHash.isNotBlank())
    }
}
