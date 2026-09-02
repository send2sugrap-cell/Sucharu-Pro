package com.sucharu.sucharupro.domain.service.imposition

import com.sucharu.sucharupro.domain.model.imposition.*
import com.sucharu.sucharupro.domain.model.printingcalculator.ColorMode
import com.sucharu.sucharupro.domain.model.printingcalculator.MeasurementUnit
import com.sucharu.sucharupro.domain.model.printingcalculator.PaperStockType
import com.sucharu.sucharupro.domain.model.printingcalculator.PrintingDimension
import com.sucharu.sucharupro.domain.model.printingcalculator.PrintingSideOption
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

/**
 * Unit Tests for Multi-Job Compatibility Clustering and Gang-Run Batching Engine.
 * Module 18 Step 02.
 */
class GangRunClusteringEngineTest {

    private lateinit var engine: GangRunClusteringEngine

    @Before
    fun setUp() {
        engine = GangRunClusteringEngine()
    }

    @Test
    fun `formClusters should group identical substrate and process jobs into single cluster`() {
        val candidates = listOf(
            GangRunCandidateItem(
                jobId = "JOB-01",
                orderId = "ORD-01",
                orderItemId = "ITEM-01",
                productName = "Product 1",
                finishedDimension = PrintingDimension(BigDecimal("148.0000"), BigDecimal("210.0000"), MeasurementUnit.MILLIMETERS),
                requiredQuantity = 1000L,
                paperStockType = PaperStockType.ART_CARD,
                gsm = BigDecimal("300.0000"),
                colorMode = ColorMode.CMYK_FOUR_COLOR,
                printingSideOption = PrintingSideOption.SINGLE_SIDED
            ),
            GangRunCandidateItem(
                jobId = "JOB-02",
                orderId = "ORD-02",
                orderItemId = "ITEM-02",
                productName = "Product 2",
                finishedDimension = PrintingDimension(BigDecimal("148.0000"), BigDecimal("210.0000"), MeasurementUnit.MILLIMETERS),
                requiredQuantity = 2000L,
                paperStockType = PaperStockType.ART_CARD,
                gsm = BigDecimal("300.0000"),
                colorMode = ColorMode.CMYK_FOUR_COLOR,
                printingSideOption = PrintingSideOption.SINGLE_SIDED
            )
        )

        val clusters = engine.formClusters(candidates, GangRunClusteringPolicy.STRICT_IDENTICAL_SUBSTRATE)
        assertEquals(1, clusters.size)
        assertEquals(2, clusters[0].candidateItems.size)
        assertEquals(PaperStockType.ART_CARD, clusters[0].paperStockType)
        assertEquals(BigDecimal("300.0000"), clusters[0].representativeGsm)
    }

    @Test
    fun `formClusters should separate incompatible substrates into different clusters`() {
        val candidates = listOf(
            GangRunCandidateItem(
                jobId = "JOB-01",
                orderId = "ORD-01",
                orderItemId = "ITEM-01",
                productName = "Flyer",
                finishedDimension = PrintingDimension(BigDecimal("148.0000"), BigDecimal("210.0000"), MeasurementUnit.MILLIMETERS),
                requiredQuantity = 1000L,
                paperStockType = PaperStockType.ART_PAPER,
                gsm = BigDecimal("150.0000")
            ),
            GangRunCandidateItem(
                jobId = "JOB-02",
                orderId = "ORD-02",
                orderItemId = "ITEM-02",
                productName = "Card",
                finishedDimension = PrintingDimension(BigDecimal("148.0000"), BigDecimal("210.0000"), MeasurementUnit.MILLIMETERS),
                requiredQuantity = 2000L,
                paperStockType = PaperStockType.ART_CARD,
                gsm = BigDecimal("300.0000")
            )
        )

        val clusters = engine.formClusters(candidates, GangRunClusteringPolicy.STRICT_IDENTICAL_SUBSTRATE)
        assertEquals(2, clusters.size)
    }

    @Test
    fun `optimizeGangRun should allocate UP-slots proportionally and determine common press sheet run`() {
        val candidates = listOf(
            GangRunCandidateItem(
                jobId = "JOB-A",
                orderId = "ORD-01",
                orderItemId = "ITEM-A",
                productName = "Item A",
                finishedDimension = PrintingDimension(BigDecimal("148.0000"), BigDecimal("210.0000"), MeasurementUnit.MILLIMETERS),
                requiredQuantity = 2000L,
                paperStockType = PaperStockType.ART_CARD,
                gsm = BigDecimal("300.0000")
            ),
            GangRunCandidateItem(
                jobId = "JOB-B",
                orderId = "ORD-02",
                orderItemId = "ITEM-B",
                productName = "Item B",
                finishedDimension = PrintingDimension(BigDecimal("148.0000"), BigDecimal("210.0000"), MeasurementUnit.MILLIMETERS),
                requiredQuantity = 1000L,
                paperStockType = PaperStockType.ART_CARD,
                gsm = BigDecimal("300.0000")
            )
        )

        val cluster = engine.formClusters(candidates).first()
        val parentSheet = PrintingDimension(BigDecimal("635.0000"), BigDecimal("914.4000"), MeasurementUnit.MILLIMETERS)

        val spec = engine.optimizeGangRun(
            tenantId = "TENANT-TEST",
            batchName = "Test Gang Batch",
            cluster = cluster,
            parentSheetDimension = parentSheet
        )

        assertNotNull(spec)
        assertTrue(spec.allocatedSlotsCount >= 2)
        assertEquals(2, spec.allocations.size)

        val allocA = spec.allocations.first { it.jobId == "JOB-A" }
        val allocB = spec.allocations.first { it.jobId == "JOB-B" }

        // Job A requires twice as much as Job B, so assignedSlots(A) >= assignedSlots(B)
        assertTrue(allocA.assignedSlots >= allocB.assignedSlots)
        assertTrue(allocA.producedQuantity >= allocA.requiredQuantity)
        assertTrue(allocB.producedQuantity >= allocB.requiredQuantity)
        assertTrue(spec.commonRequiredSheets > 0L)
        assertTrue(spec.sheetYieldPercentage > BigDecimal.ZERO)
        assertTrue(spec.integrityHash.isNotBlank())
    }

    @Test
    fun `deterministic integrity hash should be identical for identical inputs`() {
        val candidate = GangRunCandidateItem(
            jobId = "JOB-DET",
            orderId = "ORD-DET",
            orderItemId = "ITEM-DET",
            productName = "Item Deterministic",
            finishedDimension = PrintingDimension(BigDecimal("100.0000"), BigDecimal("150.0000"), MeasurementUnit.MILLIMETERS),
            requiredQuantity = 500L,
            paperStockType = PaperStockType.ART_PAPER,
            gsm = BigDecimal("120.0000")
        )

        val cluster = GangRunCluster(
            clusterId = "C1",
            paperStockType = PaperStockType.ART_PAPER,
            representativeGsm = BigDecimal("120.0000"),
            colorMode = ColorMode.CMYK_FOUR_COLOR,
            printingSideOption = PrintingSideOption.SINGLE_SIDED,
            candidateItems = listOf(candidate)
        )
        val parentSheet = PrintingDimension(BigDecimal("635.0000"), BigDecimal("914.4000"), MeasurementUnit.MILLIMETERS)

        val spec1 = engine.optimizeGangRun("TENANT-1", "Batch-1", cluster, parentSheet)
        val spec2 = engine.optimizeGangRun("TENANT-1", "Batch-1", cluster, parentSheet)

        assertEquals(spec1.integrityHash, spec2.integrityHash)
    }
}
