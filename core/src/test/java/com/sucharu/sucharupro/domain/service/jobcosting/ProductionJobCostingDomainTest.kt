package com.sucharu.sucharupro.domain.service.jobcosting

import com.sucharu.sucharupro.domain.model.finalqc.*
import com.sucharu.sucharupro.domain.model.jobcosting.*
import com.sucharu.sucharupro.domain.model.production.ProductionStageType
import com.sucharu.sucharupro.domain.model.shopfloortracking.*
import org.junit.Assert.*
import org.junit.Test
import java.math.BigDecimal

class ProductionJobCostingDomainTest {

    private val actualCostEngine = ActualJobCostingEngine()
    private val varianceEngine = ManufacturingVarianceEngine()
    private val scrapReworkEngine = ScrapReworkValuationEngine()
    private val reconciliationEngine = ManufacturingCostReconciliationEngine()

    @Test
    fun `test actual job cost engine calculates material, labor, machine, scrap, and packaging accurately`() {
        val materials = listOf(
            ProductionMaterialConsumptionRecord(
                consumptionId = "MAT-01",
                tenantId = "TENANT-001",
                workOrderId = "WO-01",
                executionJobId = "JOB-101",
                stageType = ProductionStageType.PRINTING,
                materialCode = "SUB-ART-150",
                materialName = "Art Paper 150gsm",
                unitOfMeasure = "SHEETS",
                plannedQuantity = BigDecimal("5500.0000"),
                actualQuantityConsumed = BigDecimal("5600.0000"),
                scrapQuantity = BigDecimal("100.0000"),
                varianceQuantity = BigDecimal("100.0000"),
                variancePercentage = BigDecimal("1.8182"),
                batchLotNumber = "LOT-101",
                recordedBy = "operator"
            )
        )

        val timeRecords = listOf(
            OperatorTimeTrackingRecord(
                recordId = "TIME-01",
                tenantId = "TENANT-001",
                workOrderId = "WO-01",
                executionJobId = "JOB-101",
                orderId = "ORD-101",
                sequenceNumber = 1,
                stageType = ProductionStageType.PRINTING,
                machineId = "PRESS-01",
                machineName = "Heidelberg Speedmaster",
                operatorId = "OP-1",
                operatorName = "Rahim Lead",
                setupMinutes = 60,
                runMinutes = 240,
                downtimeMinutes = 30
            )
        )

        val telemetries = listOf(
            MachineTelemetryLog(
                logId = "TEL-01",
                tenantId = "TENANT-001",
                machineId = "PRESS-01",
                machineName = "Heidelberg Speedmaster",
                workOrderId = "WO-01",
                executionJobId = "JOB-101",
                recordedSpeedUnitsPerHour = BigDecimal("8000.0000"),
                ratedSpeedUnitsPerHour = BigDecimal("10000.0000"),
                speedEfficiencyPercentage = BigDecimal("80.0000"),
                totalImpressions = 5600L,
                currentDowntimeCategory = DowntimeCategory.SETUP_ADJUSTMENT,
                downtimeMinutes = 30,
                loggedBy = "telemetry-agent"
            )
        )


        val defects = listOf(
            ProductionDefectContainmentRecord(
                containmentId = "DEF-01",
                tenantId = "TENANT-001",
                executionJobId = "JOB-101",
                inspectionId = "INSP-01",
                rootCauseStage = ProductionStageType.PRINTING,
                defectType = DefectClassificationType.PRINTING_DEFECT,
                severity = DefectSeverity.MAJOR,
                defectQuantity = BigDecimal("100.0000"),
                disposition = ContainmentDisposition.SCRAPPED,
                quarantineLocation = "BIN-A",
                rootCauseDetails = "Hickey",
                loggedBy = "qc"
            )
        )

        val packaging = listOf(
            ProductionPackagingRecord(
                packagingId = "PKG-01",
                tenantId = "TENANT-001",
                executionJobId = "JOB-101",
                inspectionId = "INSP-01",
                packagingType = PackagingType.CORRUGATED_BOX,
                unitsPerPackage = BigDecimal("500.0000"),
                totalPackageCount = 10,
                totalPackagedQuantity = BigDecimal("5000.0000"),
                packagingSlipBarcode = "PKG-101-10C-1234",
                packagedBy = "packer"
            )
        )

        val cost = actualCostEngine.calculateActualJobCost(
            tenantId = "TENANT-001",
            executionJobId = "JOB-101",
            orderId = "ORD-101",
            manufacturedGoodQuantity = BigDecimal("5000.0000"),
            materialConsumptions = materials,
            operatorTimeRecords = timeRecords,
            machineTelemetryLogs = telemetries,
            defectRecords = defects,
            packagingRecords = packaging,
            standardMaterialRates = mapOf("SUB-ART-150" to BigDecimal("3.0000")),
            standardLaborHourlyRates = mapOf(ProductionStageType.PRINTING to BigDecimal("200.0000")),
            standardMachineHourlyRates = mapOf("PRESS-01" to BigDecimal("500.0000")),
            packagingUnitRate = BigDecimal("20.0000"),
            overheadAllocationRate = BigDecimal("0.1000")
        )

        assertEquals(BigDecimal("16800.0000"), cost.totalMaterialCost) // 5600 * 3.0
        assertEquals(BigDecimal("1000.0000"), cost.totalLaborCost) // (1.0h setup + 4.0h run) * 200
        assertEquals(BigDecimal("375.0000"), cost.totalMachineCost) // 0.5h run * 500 = 250 + 0.5h downtime * 250 = 375
        assertEquals(BigDecimal("1500.0000"), cost.totalQualityScrapCost) // 100 * 15
        assertEquals(BigDecimal("200.0000"), cost.totalPackagingCost) // 10 * 20
        assertTrue(cost.grandTotalActualCost > BigDecimal.ZERO)
        assertTrue(cost.actualUnitCost > BigDecimal.ZERO)
    }

    @Test
    fun `test manufacturing variance engine identifies cost variances and margin impacts`() {
        val costRecord = ProductionActualJobCostRecord(
            costRecordId = "COST-01",
            tenantId = "TENANT-001",
            executionJobId = "JOB-101",
            orderId = "ORD-101",
            manufacturedGoodQuantity = BigDecimal("5000.0000"),
            totalMaterialCost = BigDecimal("16800.0000"),
            totalLaborCost = BigDecimal("1000.0000"),
            totalMachineCost = BigDecimal("400.0000"),
            totalQualityScrapCost = BigDecimal("1500.0000"),
            totalReworkCost = BigDecimal.ZERO,
            totalPackagingCost = BigDecimal("200.0000"),
            totalOverheadAllocatedCost = BigDecimal("1820.0000"),
            grandTotalActualCost = BigDecimal("21720.0000"),
            actualUnitCost = BigDecimal("4.3440")
        )

        val variance = varianceEngine.generateVarianceSummary(
            actualCostRecord = costRecord,
            quotedSellingPrice = BigDecimal("30000.0000"),
            estimatedTotalCost = BigDecimal("20000.0000"),
            estimatedMaterialCost = BigDecimal("15000.0000"),
            estimatedLaborCost = BigDecimal("1200.0000"),
            estimatedMachineCost = BigDecimal("500.0000"),
            orderQuantity = BigDecimal("5000.0000")
        )

        assertEquals(BigDecimal("1720.0000"), variance.totalCostVariance)
        assertEquals(VarianceClassification.UNFAVORABLE, variance.overallCostClassification)
        assertEquals(BigDecimal("1800.0000"), variance.materialVariance) // 16800 - 15000
        assertEquals(BigDecimal("-200.0000"), variance.laborVariance) // 1000 - 1200 (Favorable)
        assertEquals(VarianceClassification.FAVORABLE, variance.laborCostClassification)
        assertEquals(BigDecimal("10000.0000"), variance.estimatedGrossProfit)
        assertEquals(BigDecimal("8280.0000"), variance.actualGrossProfit)
    }

    @Test
    fun `test 8-way manufacturing cost reconciliation and SHA-256 certificate generation`() {
        val costRecord = ProductionActualJobCostRecord(
            costRecordId = "COST-01",
            tenantId = "TENANT-001",
            executionJobId = "JOB-101",
            orderId = "ORD-101",
            manufacturedGoodQuantity = BigDecimal("5000.0000"),
            totalMaterialCost = BigDecimal("15000.0000"),
            totalLaborCost = BigDecimal("1000.0000"),
            totalMachineCost = BigDecimal("500.0000"),
            totalQualityScrapCost = BigDecimal("500.0000"),
            totalReworkCost = BigDecimal.ZERO,
            totalPackagingCost = BigDecimal("200.0000"),
            totalOverheadAllocatedCost = BigDecimal("1650.0000"),
            grandTotalActualCost = BigDecimal("18850.0000"),
            actualUnitCost = BigDecimal("3.7700"),
            materialBreakdown = listOf(
                ActualMaterialCostItem(
                    materialCode = "MAT-1",
                    materialName = "Art Paper",
                    unitOfMeasure = "SHEETS",
                    plannedQuantity = BigDecimal("5000.0000"),
                    actualQuantity = BigDecimal("5000.0000"),
                    quantityVariance = BigDecimal.ZERO,
                    standardUnitPrice = BigDecimal("3.0000"),
                    actualUnitPrice = BigDecimal("3.0000"),
                    priceVariance = BigDecimal.ZERO,
                    plannedCost = BigDecimal("15000.0000"),
                    actualCost = BigDecimal("15000.0000"),
                    totalVariance = BigDecimal.ZERO,
                    varianceClassification = VarianceClassification.NEUTRAL
                )
            )
        )

        val varianceSummary = varianceEngine.generateVarianceSummary(
            actualCostRecord = costRecord,
            quotedSellingPrice = BigDecimal("25000.0000"),
            estimatedTotalCost = BigDecimal("18500.0000"),
            estimatedMaterialCost = BigDecimal("15000.0000"),
            estimatedLaborCost = BigDecimal("1000.0000"),
            estimatedMachineCost = BigDecimal("500.0000"),
            orderQuantity = BigDecimal("5000.0000")
        )

        val reconciliation = reconciliationEngine.reconcile(costRecord, varianceSummary, "cost-auditor")
        assertTrue(reconciliation.isFullyReconciled)
        assertTrue(reconciliation.actualCostMathBalanced)
        assertTrue(reconciliation.varianceIntegrityHashValid)
        assertEquals(64, reconciliation.certificateHash.length)
        assertTrue(reconciliation.discrepancies.isEmpty())
    }
}
