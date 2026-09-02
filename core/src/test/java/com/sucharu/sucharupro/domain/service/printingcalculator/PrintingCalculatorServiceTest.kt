package com.sucharu.sucharupro.domain.service.printingcalculator

import com.sucharu.sucharupro.data.datasource.printingcalculator.FakePrintingCalculatorDataSource
import com.sucharu.sucharupro.data.repository.printingcalculator.PrintingCalculatorRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.printingcalculator.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

/**
 * Service Integration Tests for Smart Printing Calculator.
 * Module 17 Step 01.
 */
class PrintingCalculatorServiceTest {

    private lateinit var dataSource: FakePrintingCalculatorDataSource
    private lateinit var repository: PrintingCalculatorRepositoryImpl
    private lateinit var service: PrintingCalculatorServiceImpl

    @Before
    fun setUp() {
        dataSource = FakePrintingCalculatorDataSource()
        repository = PrintingCalculatorRepositoryImpl(dataSource)
        service = PrintingCalculatorServiceImpl(repository)
    }

    @Test
    fun testCalculate_orchestratesAndPersistsResult() = runBlocking {
        val req = PrintingCalculationRequest(
            tenantId = "TENANT-001",
            projectId = "PROJECT-001",
            jobTitle = "Annual Report 2026",
            quantity = 2000L,
            finishedWidth = BigDecimal("210.0000"),
            finishedHeight = BigDecimal("297.0000"),
            materialName = "Art Card 300 GSM",
            stockType = PaperStockType.ART_CARD,
            gsm = BigDecimal("300.0000"),
            sheetWidth = BigDecimal("635.0000"),
            sheetHeight = BigDecimal("914.0000"),
            materialUnitPricePerSheet = BigDecimal("15.0000"),
            processType = PrintingProcessType.OFFSET,
            sides = PrintingSideOption.DOUBLE_SIDED_SAME,
            colorMode = ColorMode.CMYK_FOUR_COLOR,
            frontColorsCount = 4,
            backColorsCount = 4
        )

        val result = service.calculate(req)
        assertTrue(result is DomainResult.Success)
        val data = (result as DomainResult.Success).data
        assertEquals("TENANT-001", data.tenantId)
        assertEquals("PROJECT-001", data.projectId)
        assertEquals(CalculationStatus.SUCCESSFUL, data.status)
        assertNotNull(data.totalEstimatedCost)
        assertNotNull(data.estimatedUnitCost)

        // Verify retrieval by ID
        val retrieved = service.getCalculationById("TENANT-001", data.calculationId)
        assertTrue(retrieved is DomainResult.Success)
        assertEquals(data.calculationId, (retrieved as DomainResult.Success).data?.calculationId)
    }

    @Test
    fun testIdempotency_returnsIdenticalCalculationForSameInputs() = runBlocking {
        val req1 = PrintingCalculationRequest(
            tenantId = "TENANT-001",
            projectId = "PROJECT-001",
            quantity = 500L,
            finishedWidth = BigDecimal("100.0000"),
            finishedHeight = BigDecimal("150.0000"),
            materialName = "Offset Paper",
            sheetWidth = BigDecimal("500.0000"),
            sheetHeight = BigDecimal("700.0000"),
            materialUnitPricePerSheet = BigDecimal("5.0000")
        )

        val res1 = service.calculate(req1)
        assertTrue(res1 is DomainResult.Success)
        val calc1 = (res1 as DomainResult.Success).data

        val req2 = req1.copy() // Identical request
        val res2 = service.calculate(req2)
        assertTrue(res2 is DomainResult.Success)
        val calc2 = (res2 as DomainResult.Success).data

        assertEquals(calc1.calculationId, calc2.calculationId)
        assertEquals(calc1.requestFingerprint, calc2.requestFingerprint)
        assertEquals(calc1.integrityHash, calc2.integrityHash)
    }

    @Test
    fun testTenantIsolation_preventsCrossTenantAccess() = runBlocking {
        val reqTenantA = PrintingCalculationRequest(
            tenantId = "TENANT-A",
            projectId = "TENANT-A",
            quantity = 1000L,
            finishedWidth = BigDecimal("210.0000"),
            finishedHeight = BigDecimal("297.0000"),
            materialName = "Art Paper",
            sheetWidth = BigDecimal("635.0000"),
            sheetHeight = BigDecimal("914.0000")
        )

        val resA = service.calculate(reqTenantA)
        val calcA = (resA as DomainResult.Success).data

        // Tenant B attempts to read Tenant A's calculation
        val crossTenantRead = service.getCalculationById("TENANT-B", calcA.calculationId)
        assertTrue(crossTenantRead is DomainResult.Success)
        assertNull((crossTenantRead as DomainResult.Success).data)
    }

    @Test
    fun testExportHandoffContract_generatesVerifiedContract() = runBlocking {
        val req = PrintingCalculationRequest(
            tenantId = "TENANT-001",
            projectId = "PROJECT-001",
            jobTitle = "Flyer 5000",
            quantity = 5000L,
            finishedWidth = BigDecimal("148.0000"),
            finishedHeight = BigDecimal("210.0000"), // A5
            materialName = "Art Paper 120 GSM",
            sheetWidth = BigDecimal("635.0000"),
            sheetHeight = BigDecimal("914.0000"),
            materialUnitPricePerSheet = BigDecimal("6.0000")
        )

        val calcRes = service.calculate(req)
        val calc = (calcRes as DomainResult.Success).data

        val handoffRes = service.exportHandoffContract("TENANT-001", calc.calculationId)
        assertTrue(handoffRes is DomainResult.Success)
        val handoff = (handoffRes as DomainResult.Success).data

        assertEquals(calc.calculationId, handoff.calculationId)
        assertEquals("TENANT-001", handoff.tenantId)
        assertEquals("1.0.0", handoff.contractVersion)
        assertTrue(handoff.isReadOnly)
        assertTrue(handoff.handoffIntegrityHash.isNotBlank())
        assertEquals(calc.requestFingerprint, handoff.requestFingerprint)
    }
}
