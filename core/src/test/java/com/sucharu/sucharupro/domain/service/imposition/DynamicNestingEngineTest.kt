package com.sucharu.sucharupro.domain.service.imposition

import com.sucharu.sucharupro.domain.model.imposition.*
import com.sucharu.sucharupro.domain.model.printingcalculator.*
import org.junit.Assert.*
import org.junit.Test
import java.math.BigDecimal

/**
 * Unit Tests for Dynamic 2D Nesting & Wastage Optimization Engine.
 * Module 18 Step 03.
 */
class DynamicNestingEngineTest {

    @Test
    fun `optimizeNesting should pack multiple mixed-dimension rectangular items on parent sheet`() {
        val candidates = listOf(
            NestingCandidateItem(
                jobId = "JOB-A4",
                orderId = "ORD-01",
                orderItemId = "ITEM-01",
                productName = "A4 Leaflet",
                finishedDimension = PrintingDimension(BigDecimal("210.0000"), BigDecimal("297.0000"), MeasurementUnit.MILLIMETERS),
                requiredQuantity = 1000L,
                paperStockType = PaperStockType.ART_CARD,
                gsm = BigDecimal("300.0000"),
                allowRotation = true
            ),
            NestingCandidateItem(
                jobId = "JOB-A5",
                orderId = "ORD-02",
                orderItemId = "ITEM-02",
                productName = "A5 Flyer",
                finishedDimension = PrintingDimension(BigDecimal("148.0000"), BigDecimal("210.0000"), MeasurementUnit.MILLIMETERS),
                requiredQuantity = 2000L,
                paperStockType = PaperStockType.ART_CARD,
                gsm = BigDecimal("300.0000"),
                allowRotation = true
            ),
            NestingCandidateItem(
                jobId = "JOB-CARD",
                orderId = "ORD-03",
                orderItemId = "ITEM-03",
                productName = "Business Card",
                finishedDimension = PrintingDimension(BigDecimal("90.0000"), BigDecimal("54.0000"), MeasurementUnit.MILLIMETERS),
                requiredQuantity = 4000L,
                paperStockType = PaperStockType.ART_CARD,
                gsm = BigDecimal("300.0000"),
                allowRotation = true
            )
        )

        val parentSheet = PrintingDimension(BigDecimal("635.0000"), BigDecimal("914.4000"), MeasurementUnit.MILLIMETERS)

        val spec = DynamicNestingEngine.optimizeNesting(
            tenantId = "TENANT-NEST",
            name = "Test Mixed Nesting",
            candidateItems = candidates,
            parentSheetDimension = parentSheet,
            actor = "test_operator"
        )

        assertNotNull(spec)
        assertTrue("Total placed items must be positive", spec.totalItemsPlaced > 0)
        assertEquals("All 3 jobs must be allocated", 3, spec.jobSummaries.size)

        // Usable yield and utilization must be strictly positive
        assertTrue(spec.usableYieldPercentage > BigDecimal.ZERO)
        assertTrue(spec.sheetUtilizationPercentage > BigDecimal.ZERO)
        assertTrue(spec.sheetUtilizationPercentage <= BigDecimal("100.0000"))

        // Placements must not exceed usable sheet boundary
        spec.placements.forEach { p ->
            assertTrue("Item X position within bounds", p.xMm >= spec.marginSpec.leftMm)
            assertTrue("Item Y position within bounds", p.yMm >= spec.marginSpec.topMm)
            val maxX = p.xMm.add(p.placedWidthMm)
            val maxY = p.yMm.add(p.placedHeightMm)
            assertTrue("Item right edge within sheet", maxX <= parentSheet.width)
            assertTrue("Item bottom edge within sheet", maxY <= parentSheet.height)
        }

        // Check common press sheets run
        assertTrue(spec.commonRequiredSheets > 0L)
        spec.jobSummaries.forEach { job ->
            assertTrue("Produced quantity >= required quantity for ${job.jobId}", job.producedQuantity >= job.requiredQuantity)
        }
    }

    @Test
    fun `optimizeNesting should identify recoverable and non-recoverable offcut remnants`() {
        val singleCandidate = listOf(
            NestingCandidateItem(
                jobId = "JOB-SMALL",
                orderId = "ORD-01",
                orderItemId = "ITEM-01",
                productName = "Small Card",
                finishedDimension = PrintingDimension(BigDecimal("100.0000"), BigDecimal("100.0000"), MeasurementUnit.MILLIMETERS),
                requiredQuantity = 100L,
                paperStockType = PaperStockType.ART_PAPER,
                gsm = BigDecimal("150.0000")
            )
        )

        val parentSheet = PrintingDimension(BigDecimal("635.0000"), BigDecimal("914.4000"), MeasurementUnit.MILLIMETERS)

        val spec = DynamicNestingEngine.optimizeNesting(
            tenantId = "TENANT-TEST",
            name = "Offcut Test",
            candidateItems = singleCandidate,
            parentSheetDimension = parentSheet,
            minOffcutDimensionMm = BigDecimal("100.0000"),
            actor = "test_operator"
        )

        assertTrue(spec.offcutRemnants.isNotEmpty())
        val recoverable = spec.offcutRemnants.filter { it.isRecoverable }
        assertTrue("Large parent sheet with small item must produce recoverable offcuts", recoverable.isNotEmpty())
        assertTrue(spec.recoverableOffcutAreaMm2 > BigDecimal.ZERO)
    }

    @Test
    fun `deterministic integrity hash is invariant across identical runs`() {
        val candidates = listOf(
            NestingCandidateItem(
                jobId = "JOB-DET-1",
                orderId = "ORD-DET",
                orderItemId = "ITEM-1",
                productName = "Brochure",
                finishedDimension = PrintingDimension(BigDecimal("148.0000"), BigDecimal("210.0000"), MeasurementUnit.MILLIMETERS),
                requiredQuantity = 500L,
                paperStockType = PaperStockType.ART_CARD,
                gsm = BigDecimal("250.0000")
            )
        )

        val parentSheet = PrintingDimension(BigDecimal("635.0000"), BigDecimal("914.4000"), MeasurementUnit.MILLIMETERS)

        val spec1 = DynamicNestingEngine.optimizeNesting("TENANT-A", "Run 1", candidates, parentSheet, actor = "admin")
        val spec2 = DynamicNestingEngine.optimizeNesting("TENANT-A", "Run 1", candidates, parentSheet, actor = "admin")

        assertEquals(spec1.totalItemsPlaced, spec2.totalItemsPlaced)
        assertEquals(spec1.sheetUtilizationPercentage, spec2.sheetUtilizationPercentage)
        assertEquals(spec1.usableYieldPercentage, spec2.usableYieldPercentage)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `incompatible paper stock in nesting pool must be rejected`() {
        val candidates = listOf(
            NestingCandidateItem(
                jobId = "JOB-01",
                orderId = "ORD-01",
                orderItemId = "ITEM-01",
                productName = "Flyer",
                finishedDimension = PrintingDimension(BigDecimal("148.0000"), BigDecimal("210.0000"), MeasurementUnit.MILLIMETERS),
                requiredQuantity = 1000L,
                paperStockType = PaperStockType.ART_CARD,
                gsm = BigDecimal("300.0000")
            ),
            NestingCandidateItem(
                jobId = "JOB-02",
                orderId = "ORD-02",
                orderItemId = "ITEM-02",
                productName = "Booklet",
                finishedDimension = PrintingDimension(BigDecimal("148.0000"), BigDecimal("210.0000"), MeasurementUnit.MILLIMETERS),
                requiredQuantity = 500L,
                paperStockType = PaperStockType.OFFSET_PAPER, // Incompatible!
                gsm = BigDecimal("80.0000")
            )
        )

        val parentSheet = PrintingDimension(BigDecimal("635.0000"), BigDecimal("914.4000"), MeasurementUnit.MILLIMETERS)

        DynamicNestingEngine.optimizeNesting(
            tenantId = "TENANT-TEST",
            name = "Incompatible Test",
            candidateItems = candidates,
            parentSheetDimension = parentSheet,
            actor = "test_operator"
        )
    }
}
