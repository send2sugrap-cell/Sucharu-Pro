package com.sucharu.sucharupro.domain.service.substratereservation

import com.sucharu.sucharupro.domain.model.printingcalculator.MeasurementUnit
import com.sucharu.sucharupro.domain.model.printingcalculator.PaperStockType
import com.sucharu.sucharupro.domain.model.printingcalculator.PrintingDimension
import com.sucharu.sucharupro.domain.model.substratereservation.*
import org.junit.Assert.*
import org.junit.Test
import java.math.BigDecimal

/**
 * Unit tests for BatchLotSelectionEngine (Module 19 Step 03).
 */
class BatchLotSelectionEngineTest {

    private val tenantId = "TENANT-TEST-001"
    private val standardDimension = PrintingDimension(BigDecimal("635.0000"), BigDecimal("914.4000"), MeasurementUnit.MILLIMETERS) // 25x36 in mm

    private fun createSpec(
        requiredSheets: Long = 1000L,
        dimension: PrintingDimension = standardDimension,
        grain: PaperGrainDirection = PaperGrainDirection.LONG_GRAIN,
        allowRotation: Boolean = true,
        allowMultiBatch: Boolean = true,
        policy: BatchSelectionPolicy = BatchSelectionPolicy.FIFO
    ): BatchLotSelectionSpecification {
        return BatchLotSelectionSpecification(
            selectionId = "SBS-TEST-001",
            tenantId = tenantId,
            orderId = "ORD-101",
            orderItemId = "ITEM-01",
            executionJobId = "JOB-01",
            productId = "PROD-ART-300",
            sku = "SKU-ART-300-25X36",
            requestedMaterialName = "Art Card 300 GSM",
            stockType = PaperStockType.ART_CARD,
            targetGsm = BigDecimal("300.0000"),
            requiredSheetDimension = dimension,
            requiredGrainDirection = grain,
            requiredSheets = requiredSheets,
            allowSheetRotation = allowRotation,
            allowMultiBatchFulfillment = allowMultiBatch,
            selectionPolicy = policy,
            actor = "TEST_RUNNER"
        )
    }

    private fun createCandidate(
        candidateId: String,
        batchNumber: String,
        lotNumber: String,
        dimension: PrintingDimension = standardDimension,
        grain: PaperGrainDirection = PaperGrainDirection.LONG_GRAIN,
        gsm: BigDecimal = BigDecimal("300.0000"),
        usableSheets: Long = 2000L,
        receivedOffsetDays: Long = 0L
    ): BatchLotInventoryCandidate {
        return BatchLotInventoryCandidate(
            candidateId = candidateId,
            tenantId = tenantId,
            warehouseId = "WH-01",
            warehouseName = "Main Warehouse",
            locationId = "LOC-1",
            productId = "PROD-ART-300",
            sku = "SKU-ART-300-25X36",
            productName = "Art Card 300 GSM",
            batchNumber = batchNumber,
            lotNumber = lotNumber,
            stockType = PaperStockType.ART_CARD,
            gsm = gsm,
            sheetDimension = dimension,
            grainDirection = grain,
            onHandPhysicalSheets = usableSheets + 500L,
            reservedSheets = 500L,
            hardAllocatedSheets = 0L,
            usableSheets = usableSheets,
            receivedTimestamp = System.currentTimeMillis() - (receivedOffsetDays * 86400000L)
        )
    }

    @Test
    fun testExactDimensionAndGrainMatch_CalculatesBestScore() {
        val spec = createSpec(requiredSheets = 1000L)
        val candidate = createCandidate("C1", "B1", "L1", usableSheets = 1500L)

        val result = BatchLotSelectionEngine.selectBatches(spec, listOf(candidate))

        assertEquals(BatchLotSelectionStatus.FULLY_SATISFIED, result.status)
        assertEquals(1000L, result.allocatedSheets)
        assertEquals(0L, result.deficitSheets)
        assertTrue(result.isFullySatisfied)
        assertEquals(1, result.selectedBatches.size)
        assertEquals("B1", result.primarySelectedBatchNumber)
        assertEquals("L1", result.primarySelectedLotNumber)
        assertTrue(result.overallCompatibilityScore > BigDecimal("90.0000"))
        assertFalse(result.selectedBatches.first().isRotated)
    }

    @Test
    fun testRotatedDimensionMatch_AppliesGrainInversionRule() {
        // Required: 635 x 914.4 mm, Grain: LONG_GRAIN
        // Candidate: 914.4 x 635 mm (Rotated), Grain: SHORT_GRAIN
        // When a SHORT_GRAIN sheet is rotated 90°, its grain aligns parallel with the LONG press axis, satisfying LONG_GRAIN requirement!
        val spec = createSpec(
            dimension = PrintingDimension(BigDecimal("635.0000"), BigDecimal("914.4000"), MeasurementUnit.MILLIMETERS),
            grain = PaperGrainDirection.LONG_GRAIN,
            allowRotation = true
        )
        val candidate = createCandidate(
            "C1", "B1", "L1",
            dimension = PrintingDimension(BigDecimal("914.4000"), BigDecimal("635.0000"), MeasurementUnit.MILLIMETERS),
            grain = PaperGrainDirection.SHORT_GRAIN,
            usableSheets = 2000L
        )

        val result = BatchLotSelectionEngine.selectBatches(spec, listOf(candidate))

        assertEquals(BatchLotSelectionStatus.FULLY_SATISFIED, result.status)
        assertEquals(1, result.selectedBatches.size)
        assertTrue(result.selectedBatches.first().isRotated)
        assertEquals(PaperGrainDirection.SHORT_GRAIN, result.selectedBatches.first().grainDirection)
    }

    @Test
    fun testGrainConflict_RejectsOrBlocksIncompatibleCandidates() {
        // Required: LONG_GRAIN (Unrotated)
        // Candidate: SHORT_GRAIN (Unrotated dimension)
        val spec = createSpec(
            dimension = standardDimension,
            grain = PaperGrainDirection.LONG_GRAIN,
            allowRotation = false
        )
        val candidate = createCandidate(
            "C1", "B1", "L1",
            dimension = standardDimension,
            grain = PaperGrainDirection.SHORT_GRAIN,
            usableSheets = 2000L
        )

        val result = BatchLotSelectionEngine.selectBatches(spec, listOf(candidate))

        assertEquals(BatchLotSelectionStatus.BLOCKED_BY_GRAIN, result.status)
        assertEquals(0L, result.allocatedSheets)
        assertFalse(result.isFullySatisfied)
    }

    @Test
    fun testUndersizedCandidate_StrictlyRejectsUndersizedSheets() {
        // Required: 635 x 914.4 mm
        // Candidate: 600 x 900 mm (Undersized)
        val spec = createSpec(requiredSheets = 1000L)
        val candidate = createCandidate(
            "C1", "B1", "L1",
            dimension = PrintingDimension(BigDecimal("600.0000"), BigDecimal("900.0000"), MeasurementUnit.MILLIMETERS),
            usableSheets = 5000L
        )

        val result = BatchLotSelectionEngine.selectBatches(spec, listOf(candidate))

        assertEquals(BatchLotSelectionStatus.BLOCKED_BY_DIMENSION, result.status)
        assertEquals(0L, result.allocatedSheets)
        assertEquals(1000L, result.deficitSheets)
    }

    @Test
    fun testOversizedCandidate_AcceptsWithCuttableFlag() {
        // Required: 635 x 914.4 mm (25x36)
        // Candidate: 711.2 x 1016.0 mm (28x40 Oversized)
        val spec = createSpec(requiredSheets = 1000L)
        val candidate = createCandidate(
            "C1", "B1", "L1",
            dimension = PrintingDimension(BigDecimal("711.2000"), BigDecimal("1016.0000"), MeasurementUnit.MILLIMETERS),
            usableSheets = 2000L
        )

        val result = BatchLotSelectionEngine.selectBatches(spec, listOf(candidate))

        assertEquals(BatchLotSelectionStatus.FULLY_SATISFIED, result.status)
        assertEquals(1000L, result.allocatedSheets)
    }

    @Test
    fun testInsufficientUsableStock_FlagsShortageAndDeficit() {
        val spec = createSpec(requiredSheets = 5000L)
        val candidate = createCandidate("C1", "B1", "L1", usableSheets = 2000L)

        val result = BatchLotSelectionEngine.selectBatches(spec, listOf(candidate))

        assertEquals(BatchLotSelectionStatus.PARTIALLY_SATISFIED, result.status)
        assertEquals(2000L, result.allocatedSheets)
        assertEquals(3000L, result.deficitSheets)
        assertFalse(result.isFullySatisfied)
    }

    @Test
    fun testMultiBatchSplitting_AllocatesAcrossMultipleLots() {
        val spec = createSpec(requiredSheets = 5000L, allowMultiBatch = true)
        val cand1 = createCandidate("C1", "B1", "L1", usableSheets = 3000L, receivedOffsetDays = 10) // Older
        val cand2 = createCandidate("C2", "B2", "L2", usableSheets = 4000L, receivedOffsetDays = 5)  // Newer

        val result = BatchLotSelectionEngine.selectBatches(spec, listOf(cand1, cand2))

        assertEquals(BatchLotSelectionStatus.FULLY_SATISFIED, result.status)
        assertEquals(5000L, result.allocatedSheets)
        assertEquals(0L, result.deficitSheets)
        assertTrue(result.isMultiBatchFulfillment)
        assertEquals(2, result.selectedBatches.size)

        // FIFO: Cand1 should be allocated first (3000 sh), then Cand2 (2000 sh)
        assertEquals("B1", result.selectedBatches[0].batchNumber)
        assertEquals(3000L, result.selectedBatches[0].allocatedSheets)
        assertEquals("B2", result.selectedBatches[1].batchNumber)
        assertEquals(2000L, result.selectedBatches[1].allocatedSheets)
    }

    @Test
    fun testDeterministicTieBreaking_ProducesIdenticalSelectionAndHash() {
        val spec = createSpec(requiredSheets = 1000L)
        val cand1 = createCandidate("C1", "B1", "L1", usableSheets = 2000L)
        val cand2 = createCandidate("C2", "B2", "L2", usableSheets = 2000L)

        val result1 = BatchLotSelectionEngine.selectBatches(spec, listOf(cand1, cand2))
        val result2 = BatchLotSelectionEngine.selectBatches(spec, listOf(cand1, cand2))

        assertEquals(result1.masterIntegrityHash, result2.masterIntegrityHash)
        assertEquals(result1.primarySelectedLotNumber, result2.primarySelectedLotNumber)
    }
}
