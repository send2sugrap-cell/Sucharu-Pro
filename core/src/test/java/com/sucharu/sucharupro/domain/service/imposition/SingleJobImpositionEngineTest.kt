package com.sucharu.sucharupro.domain.service.imposition

import com.sucharu.sucharupro.domain.model.imposition.*
import com.sucharu.sucharupro.domain.model.printingcalculator.MeasurementUnit
import com.sucharu.sucharupro.domain.model.printingcalculator.PrintingDimension
import org.junit.Assert.*
import org.junit.Test
import java.math.BigDecimal

class SingleJobImpositionEngineTest {

    private val engine = SingleJobImpositionEngine()

    @Test
    fun testStandardA4On25x36Sheet_CalculatesOptimalLayout() {
        // A4: 210mm x 297mm
        // Sheet: 25" x 36" = 635mm x 914.4mm
        // Margins: 10mm all sides -> Usable: 615mm x 894.4mm
        // Spacing: bleed 3mm, gutter 4mm
        val itemDim = PrintingDimension(BigDecimal("210.0000"), BigDecimal("297.0000"), MeasurementUnit.MILLIMETERS)
        val sheetDim = PrintingDimension(BigDecimal("635.0000"), BigDecimal("914.4000"), MeasurementUnit.MILLIMETERS)

        val spec = engine.calculateOptimalLayout(
            tenantId = "TENANT-001",
            jobId = "JOB-001",
            orderId = "ORD-001",
            orderItemId = "ITEM-001",
            productName = "A4 Brochure",
            finishedItemDimension = itemDim,
            parentSheetDimension = sheetDim,
            orientationPolicy = ImpositionOrientationPolicy.AUTO_OPTIMAL,
            requiredQuantity = 1000L
        )

        assertNotNull(spec)
        assertEquals("TENANT-001", spec.tenantId)
        assertEquals("JOB-001", spec.jobId)
        assertTrue(spec.copiesPerSheet > 0)
        assertTrue(spec.requiredSheets > 0L)
        assertTrue(spec.yieldPercentage > BigDecimal.ZERO)
        assertTrue(spec.yieldPercentage <= BigDecimal("100.0000"))
        assertEquals(spec.requiredSheets * spec.copiesPerSheet.toLong(), spec.totalProducedCapacity)
        assertTrue(spec.totalProducedCapacity >= spec.requiredQuantity)
        assertEquals(spec.totalProducedCapacity - spec.requiredQuantity, spec.overageQuantity)
        assertNotNull(spec.integrityHash)
        assertTrue(spec.integrityHash.length >= 32)
    }

    @Test
    fun testRotatedOrientationPreferredWhenYieldHigher() {
        // Item: 300mm x 100mm
        // Sheet: 350mm x 650mm (with 10mm margins -> usable 330mm x 630mm)
        // Standard:
        // cols = floor((330 + 4) / (300 + 4)) = 1
        // rows = floor((630 + 4) / (100 + 4)) = 6
        // copies = 6
        // Rotated (100 x 300):
        // cols = floor((330 + 4) / (100 + 4)) = 3
        // rows = floor((630 + 4) / (300 + 4)) = 2
        // copies = 6
        // Let's test an asymmetric sheet where rotation fits MORE items:
        // Sheet usable: 220mm x 650mm
        // Standard (300x100): cols = 0 (300 > 220, impossible standard)
        // Rotated (100x300): cols = floor((220+4)/104) = 2, rows = floor((650+4)/304) = 2 -> 4 copies!
        val itemDim = PrintingDimension(BigDecimal("300.0000"), BigDecimal("100.0000"), MeasurementUnit.MILLIMETERS)
        val sheetDim = PrintingDimension(BigDecimal("240.0000"), BigDecimal("670.0000"), MeasurementUnit.MILLIMETERS)

        val spec = engine.calculateOptimalLayout(
            tenantId = "TENANT-001",
            jobId = "JOB-ROT",
            orderId = "ORD-ROT",
            orderItemId = "ITEM-ROT",
            productName = "Rotated Fit Item",
            finishedItemDimension = itemDim,
            parentSheetDimension = sheetDim,
            orientationPolicy = ImpositionOrientationPolicy.AUTO_OPTIMAL,
            requiredQuantity = 500L
        )

        assertEquals(ImpositionLayoutOrientation.ROTATED, spec.selectedOrientation)
        assertTrue(spec.copiesPerSheet >= 4)
    }

    @Test
    fun testForceStandardOrientationPolicy() {
        val itemDim = PrintingDimension(BigDecimal("200.0000"), BigDecimal("100.0000"), MeasurementUnit.MILLIMETERS)
        val sheetDim = PrintingDimension(BigDecimal("500.0000"), BigDecimal("500.0000"), MeasurementUnit.MILLIMETERS)

        val spec = engine.calculateOptimalLayout(
            tenantId = "TENANT-001",
            jobId = "JOB-FORCE",
            orderId = "ORD-FORCE",
            orderItemId = "ITEM-FORCE",
            productName = "Standard Enforced",
            finishedItemDimension = itemDim,
            parentSheetDimension = sheetDim,
            orientationPolicy = ImpositionOrientationPolicy.FORCE_STANDARD_0_DEG,
            requiredQuantity = 200L
        )

        assertEquals(ImpositionLayoutOrientation.STANDARD, spec.selectedOrientation)
    }

    @Test(expected = IllegalArgumentException::class)
    fun testItemLargerThanUsableSheet_ThrowsException() {
        val itemDim = PrintingDimension(BigDecimal("1000.0000"), BigDecimal("1000.0000"), MeasurementUnit.MILLIMETERS)
        val sheetDim = PrintingDimension(BigDecimal("500.0000"), BigDecimal("500.0000"), MeasurementUnit.MILLIMETERS)

        engine.calculateOptimalLayout(
            tenantId = "TENANT-001",
            jobId = "JOB-TOO-BIG",
            orderId = "ORD-001",
            orderItemId = "ITEM-001",
            productName = "Oversized Item",
            finishedItemDimension = itemDim,
            parentSheetDimension = sheetDim,
            requiredQuantity = 100L
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun testZeroQuantity_ThrowsException() {
        val itemDim = PrintingDimension(BigDecimal("100.0000"), BigDecimal("100.0000"), MeasurementUnit.MILLIMETERS)
        val sheetDim = PrintingDimension(BigDecimal("500.0000"), BigDecimal("500.0000"), MeasurementUnit.MILLIMETERS)

        engine.calculateOptimalLayout(
            tenantId = "TENANT-001",
            jobId = "JOB-ZERO",
            orderId = "ORD-001",
            orderItemId = "ITEM-001",
            productName = "Zero Quantity",
            finishedItemDimension = itemDim,
            parentSheetDimension = sheetDim,
            requiredQuantity = 0L
        )
    }

    @Test
    fun testDeterministicIntegrityHash_IsIdenticalForSameInputs() {
        val itemDim = PrintingDimension(BigDecimal("150.0000"), BigDecimal("100.0000"), MeasurementUnit.MILLIMETERS)
        val sheetDim = PrintingDimension(BigDecimal("700.0000"), BigDecimal("1000.0000"), MeasurementUnit.MILLIMETERS)

        val spec1 = engine.calculateOptimalLayout(
            tenantId = "TENANT-001",
            jobId = "JOB-DETERMINISM",
            orderId = "ORD-001",
            orderItemId = "ITEM-001",
            productName = "Determinism Test",
            finishedItemDimension = itemDim,
            parentSheetDimension = sheetDim,
            requiredQuantity = 1000L
        )

        val spec2 = engine.calculateOptimalLayout(
            tenantId = "TENANT-001",
            jobId = "JOB-DETERMINISM",
            orderId = "ORD-001",
            orderItemId = "ITEM-001",
            productName = "Determinism Test",
            finishedItemDimension = itemDim,
            parentSheetDimension = sheetDim,
            requiredQuantity = 1000L
        )

        assertEquals(spec1.copiesPerSheet, spec2.copiesPerSheet)
        assertEquals(spec1.requiredSheets, spec2.requiredSheets)
        assertEquals(spec1.yieldPercentage, spec2.yieldPercentage)
        assertEquals(spec1.selectedOrientation, spec2.selectedOrientation)
        assertEquals(spec1.integrityHash, spec2.integrityHash)
    }
}
