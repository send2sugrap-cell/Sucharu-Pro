package com.sucharu.sucharupro.domain.service.shopfloortracking

import com.sucharu.sucharupro.domain.model.production.ProductionStageType
import com.sucharu.sucharupro.domain.model.shopfloortracking.*
import org.junit.Assert.*
import org.junit.Test
import java.math.BigDecimal

class ShopFloorTrackingDomainTest {

    private val materialEngine = ProductionMaterialConsumptionEngine()
    private val telemetryEngine = MachineTelemetryEngine()
    private val handoverEngine = StageHandoverEngine()
    private val varianceEngine = ShopFloorVarianceEngine()
    private val reconciliationEngine = ShopFloorTrackingReconciliationEngine()

    @Test
    fun `test material consumption calculates positive variance and over-consumption status`() {
        val record = materialEngine.recordConsumption(
            tenantId = "TENANT-001",
            workOrderId = "WO-101",
            executionJobId = "JOB-001",
            stageType = ProductionStageType.PRINTING,
            materialCode = "PAPER-ART-150",
            materialName = "Art Paper 150 GSM",
            unitOfMeasure = "SHEETS",
            plannedQuantity = BigDecimal("1000.0000"),
            actualQuantity = BigDecimal("1200.0000"), // 20% over
            scrapQuantity = BigDecimal("200.0000"),
            batchLotNumber = "LOT-2026-A",
            recordedBy = "operator-1"
        )

        assertEquals(BigDecimal("200.0000"), record.varianceQuantity)
        assertEquals(BigDecimal("20.0000"), record.variancePercentage)
        assertEquals(MaterialConsumptionStatus.OVER_CONSUMED, record.status)
    }

    @Test
    fun `test machine telemetry calculates correct speed efficiency`() {
        val log = telemetryEngine.logTelemetry(
            tenantId = "TENANT-001",
            machineId = "PRESS-01",
            machineName = "Heidelberg Speedmaster 4C",
            workOrderId = "WO-101",
            executionJobId = "JOB-001",
            recordedSpeed = BigDecimal("6000.0000"),
            ratedSpeed = BigDecimal("8000.0000"),
            totalImpressions = 12000L,
            downtimeCategory = null,
            downtimeMinutes = 0,
            loggedBy = "sensor-agent"
        )

        assertEquals(BigDecimal("75.0000"), log.speedEfficiencyPercentage)
        assertTrue(log.isRunning)
    }

    @Test
    fun `test stage handover calculates yield and generates deterministic SHA-256 integrity hash`() {
        val handover = handoverEngine.createHandover(
            tenantId = "TENANT-001",
            executionJobId = "JOB-001",
            fromWorkOrderId = "WO-101",
            fromStage = ProductionStageType.PRINTING,
            toWorkOrderId = "WO-102",
            toStage = ProductionStageType.LAMINATION,
            plannedOutputQuantity = BigDecimal("5000.0000"),
            actualGoodQuantity = BigDecimal("4800.0000"),
            scrapQuantity = BigDecimal("200.0000"),
            handedOverBy = "operator-offset"
        )

        assertEquals(BigDecimal("96.0000"), handover.yieldPercentage)
        assertEquals(HandoverStatus.PENDING_VERIFICATION, handover.status)
        assertNotNull(handover.integrityHash)
        assertEquals(64, handover.integrityHash.length) // 64 hex chars for SHA-256

        val accepted = handoverEngine.acceptHandover(handover, "operator-lamination")
        assertEquals(HandoverStatus.ACCEPTED, accepted.status)
        assertEquals("operator-lamination", accepted.acceptedBy)
    }

    @Test
    fun `test shop floor variance summary aggregates metrics accurately`() {
        val timeRecords = listOf(
            OperatorTimeTrackingRecord(
                recordId = "TIM-1",
                tenantId = "TENANT-001",
                workOrderId = "WO-101",
                executionJobId = "JOB-001",
                orderId = "ORD-001",
                sequenceNumber = 1,
                stageType = ProductionStageType.PRINTING,
                machineId = "PRESS-01",
                machineName = "Heidelberg 4C",
                operatorId = "OP-1",
                operatorName = "Rahim",
                currentState = OperatorTrackingState.COMPLETED,
                setupMinutes = 30,
                runMinutes = 150,
                downtimeMinutes = 20,
                goodQuantityProduced = BigDecimal("4900.0000"),
                scrapQuantityProduced = BigDecimal("100.0000")
            )
        )

        val summary = varianceEngine.generateVarianceSummary(
            executionJobId = "JOB-001",
            tenantId = "TENANT-001",
            plannedDurationMinutes = 180,
            timeRecords = timeRecords,
            materialRecords = emptyList(),
            telemetryLogs = emptyList(),
            plannedOutputQuantity = BigDecimal("5000.0000")
        )

        assertEquals(200, summary.actualDurationMinutes) // 30 + 150 + 20
        assertEquals(20, summary.durationVarianceMinutes) // 200 - 180
        assertEquals(BigDecimal("98.0000"), summary.overallYieldPercentage) // 4900 / 5000 * 100
        assertTrue(summary.isWithinTolerance)
    }

    @Test
    fun `test 8-way reconciliation succeeds on coherent records and flags discrepancies on corrupt hash`() {
        val timeRecords = listOf(
            OperatorTimeTrackingRecord(
                recordId = "TIM-1",
                tenantId = "TENANT-001",
                workOrderId = "WO-101",
                executionJobId = "JOB-001",
                orderId = "ORD-001",
                sequenceNumber = 1,
                stageType = ProductionStageType.PRINTING,
                machineId = "PRESS-01",
                machineName = "Heidelberg 4C",
                operatorId = "OP-1",
                operatorName = "Rahim",
                currentState = OperatorTrackingState.COMPLETED,
                setupMinutes = 30,
                runMinutes = 120,
                downtimeMinutes = 0,
                goodQuantityProduced = BigDecimal("5000.0000"),
                scrapQuantityProduced = BigDecimal("50.0000")
            )
        )

        val telemetryLogs = listOf(
            MachineTelemetryLog(
                logId = "LOG-1",
                tenantId = "TENANT-001",
                machineId = "PRESS-01",
                machineName = "Heidelberg 4C",
                workOrderId = "WO-101",
                executionJobId = "JOB-001",
                recordedSpeedUnitsPerHour = BigDecimal("6000.0000"),
                ratedSpeedUnitsPerHour = BigDecimal("8000.0000"),
                speedEfficiencyPercentage = BigDecimal("75.0000"),
                totalImpressions = 5000L,
                currentDowntimeCategory = null,
                loggedBy = "sensor-1"
            )
        )

        val handover = handoverEngine.createHandover(
            tenantId = "TENANT-001",
            executionJobId = "JOB-001",
            fromWorkOrderId = "WO-101",
            fromStage = ProductionStageType.PRINTING,
            toWorkOrderId = null,
            toStage = null,
            plannedOutputQuantity = BigDecimal("5000.0000"),
            actualGoodQuantity = BigDecimal("5000.0000"),
            scrapQuantity = BigDecimal("50.0000"),
            handedOverBy = "operator-1"
        )

        val cleanReconciliation = reconciliationEngine.reconcile(
            executionJobId = "JOB-001",
            tenantId = "TENANT-001",
            timeRecords = timeRecords,
            materialRecords = emptyList(),
            telemetryLogs = telemetryLogs,
            handovers = listOf(handover)
        )

        assertTrue(cleanReconciliation.isFullyReconciled)
        assertTrue(cleanReconciliation.cryptographicIntegrityPassed)
        assertTrue(cleanReconciliation.discrepancies.isEmpty())

        // Test tampered hash detection
        val tamperedHandover = handover.copy(integrityHash = "invalid-corrupt-hash-1234")
        val tamperedReconciliation = reconciliationEngine.reconcile(
            executionJobId = "JOB-001",
            tenantId = "TENANT-001",
            timeRecords = timeRecords,
            materialRecords = emptyList(),
            telemetryLogs = telemetryLogs,
            handovers = listOf(tamperedHandover)
        )

        assertFalse(tamperedReconciliation.isFullyReconciled)
        assertFalse(tamperedReconciliation.cryptographicIntegrityPassed)
        assertTrue(tamperedReconciliation.discrepancies.any { it.contains("Cryptographic handover integrity") })
    }
}
