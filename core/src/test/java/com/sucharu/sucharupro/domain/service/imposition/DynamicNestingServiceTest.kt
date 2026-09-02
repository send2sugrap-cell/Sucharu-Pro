package com.sucharu.sucharupro.domain.service.imposition

import com.sucharu.sucharupro.data.datasource.imposition.FakeDynamicNestingDataSource
import com.sucharu.sucharupro.data.repository.imposition.DynamicNestingRepositoryImpl
import com.sucharu.sucharupro.domain.model.imposition.*
import com.sucharu.sucharupro.domain.model.printingcalculator.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

/**
 * Service Layer & Handoff Tests for Dynamic Nesting.
 * Module 18 Step 03.
 */
class DynamicNestingServiceTest {

    private lateinit var service: DynamicNestingService
    private lateinit var fakeDataSource: FakeDynamicNestingDataSource

    @Before
    fun setUp() {
        fakeDataSource = FakeDynamicNestingDataSource()
        val repository = DynamicNestingRepositoryImpl(fakeDataSource)
        service = DynamicNestingServiceImpl(repository)
    }

    @Test
    fun `optimizeAndSave should persist dynamic nesting specification and allow retrieval`() = runBlocking {
        val candidate = NestingCandidateItem(
            jobId = "JOB-TEST",
            orderId = "ORD-TEST",
            orderItemId = "ITEM-TEST",
            productName = "Catalog Cover",
            finishedDimension = PrintingDimension(BigDecimal("210.0000"), BigDecimal("297.0000"), MeasurementUnit.MILLIMETERS),
            requiredQuantity = 1000L,
            paperStockType = PaperStockType.ART_CARD,
            gsm = BigDecimal("300.0000")
        )

        val parentSheet = PrintingDimension(BigDecimal("635.0000"), BigDecimal("914.4000"), MeasurementUnit.MILLIMETERS)

        val spec = service.optimizeAndSave(
            tenantId = "TENANT-SERVICE",
            name = "Test Nesting Run",
            candidateItems = listOf(candidate),
            parentSheetDimension = parentSheet,
            saveSpecification = true,
            actor = "operator_1"
        )

        assertNotNull(spec)
        assertTrue(spec.nestingId.startsWith("NEST-"))

        val retrieved = service.getNestingSpecification("TENANT-SERVICE", spec.nestingId)
        assertNotNull(retrieved)
        assertEquals(spec.nestingId, retrieved?.nestingId)
        assertEquals("Test Nesting Run", retrieved?.name)
    }

    @Test
    fun `updateNestingStatus should transition lifecycle state`() = runBlocking {
        val candidate = NestingCandidateItem(
            jobId = "JOB-STATUS",
            orderId = "ORD-STATUS",
            orderItemId = "ITEM-STATUS",
            productName = "Flyer",
            finishedDimension = PrintingDimension(BigDecimal("148.0000"), BigDecimal("210.0000"), MeasurementUnit.MILLIMETERS),
            requiredQuantity = 500L,
            paperStockType = PaperStockType.ART_PAPER,
            gsm = BigDecimal("150.0000")
        )
        val parentSheet = PrintingDimension(BigDecimal("635.0000"), BigDecimal("914.4000"), MeasurementUnit.MILLIMETERS)

        val spec = service.optimizeAndSave("TENANT-STAT", "Status Test", listOf(candidate), parentSheet, saveSpecification = true)

        val updateSuccess = service.updateNestingStatus(
            tenantId = "TENANT-STAT",
            nestingId = spec.nestingId,
            status = NestingStatus.APPLIED_TO_PLANNING,
            actor = "manager_1",
            notes = "Applied to production schedule"
        )

        assertTrue(updateSuccess)
        val updated = service.getNestingSpecification("TENANT-STAT", spec.nestingId)
        assertEquals(NestingStatus.APPLIED_TO_PLANNING, updated?.status)
    }

    @Test
    fun `exportHandoffContract should emit valid Module 19 substrate reservation contract`() = runBlocking {
        val candidate = NestingCandidateItem(
            jobId = "JOB-HANDOFF",
            orderId = "ORD-HANDOFF",
            orderItemId = "ITEM-HANDOFF",
            productName = "Folder",
            finishedDimension = PrintingDimension(BigDecimal("220.0000"), BigDecimal("310.0000"), MeasurementUnit.MILLIMETERS),
            requiredQuantity = 2000L,
            paperStockType = PaperStockType.ART_CARD,
            gsm = BigDecimal("350.0000")
        )
        val parentSheet = PrintingDimension(BigDecimal("635.0000"), BigDecimal("914.4000"), MeasurementUnit.MILLIMETERS)

        val spec = service.optimizeAndSave("TENANT-HANDOFF", "Handoff Test", listOf(candidate), parentSheet, saveSpecification = true)

        val contract = service.exportHandoffContract("TENANT-HANDOFF", spec.nestingId)
        assertNotNull(contract)
        assertEquals("1.0.0", contract?.contractVersion)
        assertEquals(spec.nestingId, contract?.nestingId)
        assertEquals("TENANT-HANDOFF", contract?.tenantId)
        assertEquals("ART_CARD", contract?.paperStockType)
        assertEquals(BigDecimal("350.0000"), contract?.gsm)
        assertEquals(spec.commonRequiredSheets, contract?.totalParentSheetsRequired)
        assertTrue(contract?.integrityHash?.isNotBlank() == true)
    }
}
