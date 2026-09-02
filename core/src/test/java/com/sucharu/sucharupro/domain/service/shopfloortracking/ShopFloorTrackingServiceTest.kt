package com.sucharu.sucharupro.domain.service.shopfloortracking

import com.sucharu.sucharupro.data.datasource.shopfloortracking.FakeShopFloorTrackingDataSource
import com.sucharu.sucharupro.data.repository.shopfloortracking.ShopFloorTrackingRepositoryImpl
import com.sucharu.sucharupro.domain.model.production.ProductionStageType
import com.sucharu.sucharupro.domain.model.shopfloortracking.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

class ShopFloorTrackingServiceTest {

    private lateinit var service: ShopFloorTrackingService
    private val tenantId = "TENANT-001"
    private val jobId = "JOB-001"
    private val workOrderId = "WO-001"

    @Before
    fun setup() {
        val dataSource = FakeShopFloorTrackingDataSource()
        val repository = ShopFloorTrackingRepositoryImpl(dataSource)
        service = ShopFloorTrackingServiceImpl(repository)
    }

    @Test
    fun `test end-to-end shop floor tracking execution lifecycle`() = runBlocking {
        // 1. Start execution in setup mode
        val started = service.startWorkOrderExecution(
            tenantId = tenantId,
            workOrderId = workOrderId,
            executionJobId = jobId,
            orderId = "ORD-001",
            sequenceNumber = 1,
            stageType = ProductionStageType.PRINTING,
            machineId = "PRESS-01",
            machineName = "Heidelberg 4C",
            operatorId = "OP-101",
            operatorName = "Kamal Hossain",
            isSetup = true,
            actor = "operator"
        )
        assertEquals(OperatorTrackingState.SETUP, started.currentState)

        // 2. Pause with downtime category
        val paused = service.pauseWorkOrderExecution(
            tenantId = tenantId,
            workOrderId = workOrderId,
            pauseReason = "Plate adjustment required",
            downtimeCategory = DowntimeCategory.SETUP_ADJUSTMENT,
            actor = "operator"
        )
        assertEquals(OperatorTrackingState.DOWNTIME, paused.currentState)
        assertEquals("Plate adjustment required", paused.pauseReason)

        // 3. Resume execution
        val resumed = service.resumeWorkOrderExecution(
            tenantId = tenantId,
            workOrderId = workOrderId,
            actor = "operator"
        )
        assertEquals(OperatorTrackingState.RUNNING, resumed.currentState)

        // 4. Record material consumption
        val mat = service.recordMaterialConsumption(
            tenantId = tenantId,
            workOrderId = workOrderId,
            executionJobId = jobId,
            stageType = ProductionStageType.PRINTING,
            materialCode = "PAPER-ART-150",
            materialName = "Art Paper 150 GSM",
            unitOfMeasure = "SHEETS",
            plannedQuantity = BigDecimal("5000.0000"),
            actualQuantity = BigDecimal("5100.0000"),
            scrapQuantity = BigDecimal("100.0000"),
            batchLotNumber = "LOT-101",
            notes = "Standard setup waste",
            actor = "operator"
        )
        assertEquals(BigDecimal("100.0000"), mat.varianceQuantity)

        // 5. Log machine telemetry
        val telem = service.logMachineTelemetry(
            tenantId = tenantId,
            machineId = "PRESS-01",
            machineName = "Heidelberg 4C",
            workOrderId = workOrderId,
            executionJobId = jobId,
            recordedSpeedUnitsPerHour = BigDecimal("6500.0000"),
            ratedSpeedUnitsPerHour = BigDecimal("8000.0000"),
            totalImpressions = 5100L,
            downtimeCategory = null,
            downtimeMinutes = 15,
            temperatureCelsius = BigDecimal("24.5000"),
            isRunning = true,
            actor = "sensor-agent"
        )
        assertEquals(BigDecimal("81.2500"), telem.speedEfficiencyPercentage)

        // 6. Record Output & complete
        val completed = service.recordWorkOrderOutput(
            tenantId = tenantId,
            workOrderId = workOrderId,
            additionalGoodQuantity = BigDecimal("5000.0000"),
            additionalScrapQuantity = BigDecimal("100.0000"),
            additionalSetupMinutes = 30,
            additionalRunMinutes = 90,
            additionalDowntimeMinutes = 15,
            isCompleted = true,
            actor = "operator"
        )
        assertEquals(OperatorTrackingState.COMPLETED, completed.currentState)
        assertEquals(BigDecimal("5000.0000"), completed.goodQuantityProduced)
        assertEquals(BigDecimal("100.0000"), completed.scrapQuantityProduced)

        // 7. Initiate and accept stage handover
        val handover = service.createStageHandover(
            tenantId = tenantId,
            executionJobId = jobId,
            fromWorkOrderId = workOrderId,
            fromStage = ProductionStageType.PRINTING,
            toWorkOrderId = "WO-002",
            toStage = ProductionStageType.LAMINATION,
            plannedOutputQuantity = BigDecimal("5000.0000"),
            actualGoodQuantity = BigDecimal("5000.0000"),
            scrapQuantity = BigDecimal("100.0000"),
            discrepancyNotes = null,
            actor = "operator"
        )
        assertEquals(HandoverStatus.PENDING_VERIFICATION, handover.status)

        val acceptedHandover = service.acceptStageHandover(tenantId, handover.handoverId, "lamination-operator")
        assertEquals(HandoverStatus.ACCEPTED, acceptedHandover.status)

        // 8. Reconcile and export AI handoff contract
        val recon = service.reconcileShopFloorExecution(tenantId, jobId)
        assertTrue(recon.isFullyReconciled)

        val handoffContract = service.getAiHandoffContract(tenantId, jobId)
        assertEquals("1.0.0", handoffContract.contractVersion)
        assertEquals(1, handoffContract.totalStagesCount)
        assertEquals(1, handoffContract.completedStagesCount)
        assertTrue(handoffContract.isFullyReconciled)
    }
}
