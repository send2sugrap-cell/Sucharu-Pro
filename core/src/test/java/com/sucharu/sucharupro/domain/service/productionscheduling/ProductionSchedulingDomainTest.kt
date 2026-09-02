package com.sucharu.sucharupro.domain.service.productionscheduling

import com.sucharu.sucharupro.domain.model.order.OrderPriority
import com.sucharu.sucharupro.domain.model.production.ProductionStageType
import com.sucharu.sucharupro.domain.model.productionexecution.*
import com.sucharu.sucharupro.domain.model.productionplanning.ProductionJobSpecification
import com.sucharu.sucharupro.domain.model.productionscheduling.*
import org.junit.Assert.*
import org.junit.Test
import java.math.BigDecimal

class ProductionSchedulingDomainTest {

    private fun createSampleJob(
        priority: OrderPriority = OrderPriority.NORMAL,
        hold: ProductionHold? = null
    ): ProductionJobExecution {
        val spec = ProductionJobSpecification(
            specId = "SPEC-001",
            jobTitle = "Premium Catalog",
            productType = "CATALOG",
            orderedQuantity = 5000L,
            plannedQuantity = 5250L,
            finishedWidthMm = BigDecimal("210.0000"),
            finishedHeightMm = BigDecimal("297.0000"),
            substrateType = "ART_PAPER",
            substrateGsm = 150,
            substrateBrand = null,
            parentSheetWidthMm = BigDecimal("640.0000"),
            parentSheetHeightMm = BigDecimal("900.0000"),
            pressSheetWidthMm = BigDecimal("640.0000"),
            pressSheetHeightMm = BigDecimal("450.0000"),
            impositionUps = 2,
            printingMethod = "OFFSET",
            colorsFront = 4,
            colorsBack = 4,
            specFingerprint = "SPEC-FP-001"
        )

        val workOrders = listOf(
            ProductionWorkOrder(
                workOrderId = "WO-001-1",
                executionJobId = "JOB-001",
                tenantId = "tenant-alpha",
                sequenceNumber = 1,
                stageType = ProductionStageType.DESIGN,
                operationCode = "PREPRESS_IMPOSITION",
                operationName = "Prepress Imposition",
                targetWorkCenter = "PREPRESS_DESK",
                estimatedSetupMinutes = 15,
                estimatedRunMinutes = 15,
                plannedQuantity = BigDecimal("5250.0000")
            ),
            ProductionWorkOrder(
                workOrderId = "WO-001-2",
                executionJobId = "JOB-001",
                tenantId = "tenant-alpha",
                sequenceNumber = 2,
                stageType = ProductionStageType.PRINTING,
                operationCode = "OFFSET_PRINT_4C",
                operationName = "Offset 4-Color Print",
                targetWorkCenter = "OFFSET_PRESS_ROOM",
                estimatedSetupMinutes = 30,
                estimatedRunMinutes = 60,
                plannedQuantity = BigDecimal("5250.0000")
            ),
            ProductionWorkOrder(
                workOrderId = "WO-001-3",
                executionJobId = "JOB-001",
                tenantId = "tenant-alpha",
                sequenceNumber = 3,
                stageType = ProductionStageType.LAMINATION,
                operationCode = "THERMAL_LAM_GLOSS",
                operationName = "Gloss Lamination",
                targetWorkCenter = "LAMINATION_ROOM",
                estimatedSetupMinutes = 15,
                estimatedRunMinutes = 30,
                plannedQuantity = BigDecimal("5250.0000")
            ),
            ProductionWorkOrder(
                workOrderId = "WO-001-4",
                executionJobId = "JOB-001",
                tenantId = "tenant-alpha",
                sequenceNumber = 4,
                stageType = ProductionStageType.PACKAGING,
                operationCode = "CARTON_PACK",
                operationName = "Carton Packaging",
                targetWorkCenter = "PACKAGING_ROOM",
                estimatedSetupMinutes = 10,
                estimatedRunMinutes = 20,
                plannedQuantity = BigDecimal("5250.0000")
            )
        )

        return ProductionJobExecution(
            executionJobId = "JOB-001",
            tenantId = "tenant-alpha",
            projectId = "tenant-alpha",
            orderId = "ORD-001",
            orderNumber = "SO-2026-001",
            orderItemId = "ITEM-001",
            customerId = "CUST-001",
            quotationId = "Q-001",
            quotationVersionNumber = 1,
            commercialCommitmentId = "CC-001",
            planningId = "PLAN-001",
            planningVersion = 1,
            title = "Premium Catalog Job",
            priority = priority,
            status = ProductionJobExecutionStatus.READY,
            specification = spec,
            plannedQuantity = BigDecimal("5250.0000"),
            workOrders = workOrders,
            currentHold = hold,
            jobFingerprint = "FP-JOB-001",
            integrityHash = "HASH-JOB-001",
            createdAt = 1700000000000L,
            createdBy = "planner@sucharu.com",
            updatedAt = 1700000000000L
        )
    }

    @Test
    fun testDeterministicScheduleGeneration() {
        val job = createSampleJob()
        val baseStart = 1700000000000L
        val schedule1 = ProductionSchedulingEngine.generateSchedule(job, baseStart, null, "admin")
        val schedule2 = ProductionSchedulingEngine.generateSchedule(job, baseStart, null, "admin")

        assertEquals(schedule1.scheduleFingerprint, schedule2.scheduleFingerprint)
        assertEquals(schedule1.slots.size, schedule2.slots.size)
        assertEquals(4, schedule1.slots.size)

        // Verify slot sequence and changeover buffers
        val slot1 = schedule1.slots[0]
        val slot2 = schedule1.slots[1]
        val slot3 = schedule1.slots[2]
        val slot4 = schedule1.slots[3]

        assertEquals(ProductionStageType.DESIGN, slot1.stageType)
        assertEquals(ProductionStageType.PRINTING, slot2.stageType)
        assertEquals(ProductionStageType.LAMINATION, slot3.stageType)
        assertEquals(ProductionStageType.PACKAGING, slot4.stageType)

        // Slot 1 duration = 15 + 15 = 30m
        assertEquals(30 * 60000L, slot1.scheduledEndTimestamp - slot1.scheduledStartTimestamp)
        // Slot 2 starts 10 minutes after Slot 1 ends (changeover buffer)
        assertEquals(10 * 60000L, slot2.scheduledStartTimestamp - slot1.scheduledEndTimestamp)
    }

    @Test
    fun testCapacityWindowCalculationAndUtilization() {
        val job = createSampleJob()
        val baseStart = 1700000000000L
        val schedule = ProductionSchedulingEngine.generateSchedule(job, baseStart, null, "admin")

        assertTrue(schedule.capacityWindows.isNotEmpty())
        val offsetPressWindow = schedule.capacityWindows.firstOrNull { it.machineId == "PRESS-OFFSET-4C-01" }
        assertNotNull(offsetPressWindow)

        // Press slot duration: 30 + 60 = 90 minutes
        assertEquals(BigDecimal("90.0000"), offsetPressWindow!!.allocatedMinutes)
        // Total capacity: 16h * 60m = 960m
        assertEquals(BigDecimal("960.0000"), offsetPressWindow.totalCapacityMinutes)
        assertEquals(BigDecimal("870.0000"), offsetPressWindow.availableMinutes)
        // Utilization: 90 / 960 = 0.0938
        assertEquals(BigDecimal("0.0938"), offsetPressWindow.utilizationRate)
    }

    @Test
    fun testPriorityCalculation() {
        val urgentJob = createSampleJob(priority = OrderPriority.URGENT)
        val normalJob = createSampleJob(priority = OrderPriority.NORMAL)

        val baseStart = 1700000000000L
        val urgentSchedule = ProductionSchedulingEngine.generateSchedule(urgentJob, baseStart, null, "admin")
        val normalSchedule = ProductionSchedulingEngine.generateSchedule(normalJob, baseStart, null, "admin")

        assertTrue(urgentSchedule.slots.first().priorityScore > normalSchedule.slots.first().priorityScore)
    }

    @Test
    fun testConflictDetectionOnActiveHold() {
        val hold = ProductionHold(
            holdId = "HOLD-001",
            executionJobId = "JOB-001",
            workOrderId = "WO-001-2",
            tenantId = "tenant-alpha",
            category = HoldCategory.MATERIAL_SHORTAGE,
            reason = "Paper stock awaiting delivery",
            heldAt = 1700000000000L,
            heldBy = "operator"
        )
        val job = createSampleJob(hold = hold)
        val schedule = ProductionSchedulingEngine.generateSchedule(job, 1700000000000L, null, "admin")

        assertTrue(schedule.hasBlockingConflicts)
        val holdConflict = schedule.conflicts.firstOrNull { it.conflictType == ScheduleConflictType.HOLD_CONFLICT }
        assertNotNull(holdConflict)
        assertTrue(holdConflict!!.isBlocking)
    }

    @Test
    fun testSupersedeScheduleCreatesNewVersion() {
        val job = createSampleJob()
        val initialSchedule = ProductionSchedulingEngine.generateSchedule(job, 1700000000000L, null, "admin")

        val (oldSuperseded, newSchedule) = ProductionSchedulingEngine.supersedeSchedule(
            existing = initialSchedule,
            job = job,
            reason = "Machine calibration delay",
            actor = "manager@sucharu.com",
            newStartTime = 1700003600000L
        )

        assertFalse(oldSuperseded.isCurrent)
        assertEquals(ScheduleStatus.SUPERSEDED, oldSuperseded.status)
        assertEquals(newSchedule.scheduleId, oldSuperseded.supersededByScheduleId)

        assertTrue(newSchedule.isCurrent)
        assertEquals(2, newSchedule.version)
        assertEquals("SCHED-JOB-001-V2", newSchedule.scheduleId)
    }

    @Test
    fun testDispatchQueueItemGeneration() {
        val job = createSampleJob()
        val schedule = ProductionSchedulingEngine.generateSchedule(job, 1700000000000L, null, "admin")
        val queue = ProductionSchedulingEngine.buildDispatchQueueItems(schedule, job)

        assertEquals(4, queue.size)
        assertEquals(DispatchStatus.READY, queue[0].dispatchStatus)
        assertEquals(DispatchStatus.QUEUED, queue[1].dispatchStatus)
        assertEquals(DispatchStatus.QUEUED, queue[2].dispatchStatus)
        assertEquals(DispatchStatus.QUEUED, queue[3].dispatchStatus)
    }
}
