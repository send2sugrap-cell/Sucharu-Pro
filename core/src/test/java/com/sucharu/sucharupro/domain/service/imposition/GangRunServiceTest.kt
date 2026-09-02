package com.sucharu.sucharupro.domain.service.imposition

import com.sucharu.sucharupro.data.datasource.imposition.FakeGangRunDataSource
import com.sucharu.sucharupro.data.repository.imposition.GangRunRepositoryImpl
import com.sucharu.sucharupro.domain.model.imposition.*
import com.sucharu.sucharupro.domain.model.printingcalculator.ColorMode
import com.sucharu.sucharupro.domain.model.printingcalculator.MeasurementUnit
import com.sucharu.sucharupro.domain.model.printingcalculator.PaperStockType
import com.sucharu.sucharupro.domain.model.printingcalculator.PrintingDimension
import com.sucharu.sucharupro.domain.model.printingcalculator.PrintingSideOption
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

/**
 * Service Layer Unit Tests for GangRunService.
 * Module 18 Step 02.
 */
class GangRunServiceTest {

    private lateinit var dataSource: FakeGangRunDataSource
    private lateinit var repository: GangRunRepositoryImpl
    private lateinit var service: GangRunServiceImpl

    @Before
    fun setUp() {
        dataSource = FakeGangRunDataSource()
        repository = GangRunRepositoryImpl(dataSource)
        service = GangRunServiceImpl(repository)
    }

    @Test
    fun `clusterAndOptimize should persist gang-run specification and retrieve by id`() = runBlocking {
        val candidates = listOf(
            GangRunCandidateItem(
                jobId = "JOB-S1",
                orderId = "ORD-S1",
                orderItemId = "ITEM-S1",
                productName = "Product S1",
                finishedDimension = PrintingDimension(BigDecimal("105.0000"), BigDecimal("148.0000"), MeasurementUnit.MILLIMETERS),
                requiredQuantity = 1000L,
                paperStockType = PaperStockType.ART_CARD,
                gsm = BigDecimal("250.0000")
            ),
            GangRunCandidateItem(
                jobId = "JOB-S2",
                orderId = "ORD-S2",
                orderItemId = "ITEM-S2",
                productName = "Product S2",
                finishedDimension = PrintingDimension(BigDecimal("105.0000"), BigDecimal("148.0000"), MeasurementUnit.MILLIMETERS),
                requiredQuantity = 1000L,
                paperStockType = PaperStockType.ART_CARD,
                gsm = BigDecimal("250.0000")
            )
        )

        val parentSheet = PrintingDimension(BigDecimal("635.0000"), BigDecimal("914.4000"), MeasurementUnit.MILLIMETERS)
        val specs = service.clusterAndOptimize(
            tenantId = "TENANT-SRV",
            batchName = "Commercial Gang Form #1",
            candidates = candidates,
            parentSheetDimension = parentSheet,
            saveSpecification = true
        )

        assertEquals(1, specs.size)
        val savedSpec = specs.first()

        val retrieved = service.getGangRunSpecification("TENANT-SRV", savedSpec.gangRunId)
        assertNotNull(retrieved)
        assertEquals(savedSpec.gangRunId, retrieved!!.gangRunId)
        assertEquals(2, retrieved.allocations.size)
        assertEquals(GangRunStatus.OPTIMIZED, retrieved.status)
    }

    @Test
    fun `updateGangRunStatus should transition status correctly`() = runBlocking {
        val candidates = listOf(
            GangRunCandidateItem(
                jobId = "JOB-ST1",
                orderId = "ORD-ST1",
                orderItemId = "ITEM-ST1",
                productName = "Product ST1",
                finishedDimension = PrintingDimension(BigDecimal("105.0000"), BigDecimal("148.0000"), MeasurementUnit.MILLIMETERS),
                requiredQuantity = 500L,
                paperStockType = PaperStockType.ART_PAPER,
                gsm = BigDecimal("150.0000")
            )
        )

        val specs = service.clusterAndOptimize(
            tenantId = "TENANT-ST",
            batchName = "Status Test",
            candidates = candidates,
            parentSheetDimension = PrintingDimension(BigDecimal("635.0000"), BigDecimal("914.4000"), MeasurementUnit.MILLIMETERS)
        )
        val gangRunId = specs.first().gangRunId

        val updated = service.updateGangRunStatus("TENANT-ST", gangRunId, GangRunStatus.COMMITTED_TO_PRESS, "operator_john", "Plate generated")
        assertTrue(updated)

        val spec = service.getGangRunSpecification("TENANT-ST", gangRunId)
        assertNotNull(spec)
        assertEquals(GangRunStatus.COMMITTED_TO_PRESS, spec!!.status)
    }

    @Test
    fun `exportHandoffContract should generate valid handoff data for Module 19`() = runBlocking {
        val candidates = listOf(
            GangRunCandidateItem(
                jobId = "JOB-H1",
                orderId = "ORD-H1",
                orderItemId = "ITEM-H1",
                productName = "Product H1",
                finishedDimension = PrintingDimension(BigDecimal("105.0000"), BigDecimal("148.0000"), MeasurementUnit.MILLIMETERS),
                requiredQuantity = 800L,
                paperStockType = PaperStockType.ART_CARD,
                gsm = BigDecimal("300.0000")
            )
        )

        val specs = service.clusterAndOptimize(
            tenantId = "TENANT-HANDOFF",
            batchName = "Handoff Test",
            candidates = candidates,
            parentSheetDimension = PrintingDimension(BigDecimal("635.0000"), BigDecimal("914.4000"), MeasurementUnit.MILLIMETERS)
        )
        val gangRunId = specs.first().gangRunId

        val contract = service.exportHandoffContract("TENANT-HANDOFF", gangRunId)
        assertNotNull(contract)
        assertEquals(gangRunId, contract!!.gangRunId)
        assertEquals("TENANT-HANDOFF", contract.tenantId)
        assertEquals("ART_CARD", contract.paperStockType)
        assertEquals(BigDecimal("300.0000"), contract.gsm)
        assertTrue(contract.totalParentSheetsRequired > 0L)
        assertEquals(listOf("JOB-H1"), contract.jobIds)
        assertEquals(listOf("ITEM-H1"), contract.orderItemIds)
    }
}
