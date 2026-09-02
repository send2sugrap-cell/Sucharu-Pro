package com.sucharu.sucharupro.domain.service.imposition

import com.sucharu.sucharupro.data.datasource.imposition.FakeSignatureImpositionDataSource
import com.sucharu.sucharupro.data.repository.imposition.SignatureImpositionRepositoryImpl
import com.sucharu.sucharupro.domain.model.imposition.*
import com.sucharu.sucharupro.domain.model.printingcalculator.MeasurementUnit
import com.sucharu.sucharupro.domain.model.printingcalculator.PaperStockType
import com.sucharu.sucharupro.domain.model.printingcalculator.PrintingDimension
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

/**
 * Service Layer and Repository Integration Tests for Signature Imposition.
 * Module 18 Step 04.
 */
class SignatureImpositionServiceTest {

    private lateinit var dataSource: FakeSignatureImpositionDataSource
    private lateinit var repository: SignatureImpositionRepositoryImpl
    private lateinit var service: SignatureImpositionService

    private val a4Page = PrintingDimension(BigDecimal("210.0000"), BigDecimal("297.0000"), MeasurementUnit.MILLIMETERS)
    private val pressSheet25x36 = PrintingDimension(BigDecimal("635.0000"), BigDecimal("914.4000"), MeasurementUnit.MILLIMETERS)

    @Before
    fun setup() {
        dataSource = FakeSignatureImpositionDataSource()
        repository = SignatureImpositionRepositoryImpl(dataSource)
        service = SignatureImpositionServiceImpl(repository)
    }

    @Test
    fun `optimizeAndSave should persist and retrieve signature imposition specification`() = runBlocking {
        val spec = service.optimizeAndSave(
            tenantId = "TENANT-SRV",
            name = "Service Test 32pp",
            jobId = "JOB-SRV-01",
            orderId = "ORD-SRV-01",
            orderItemId = "ITEM-01",
            productName = "Service Booklet",
            totalPages = 32,
            signaturePageCount = 16,
            bindingMethod = BindingMethod.SADDLE_STITCH,
            sheetTurningMethod = SheetTurningMethod.SHEETWISE,
            foldingScheme = FoldingScheme.RIGHT_ANGLE_16PP,
            pageDimension = a4Page,
            parentSheetDimension = pressSheet25x36,
            requiredQuantity = 1500L,
            paperStockType = PaperStockType.ART_PAPER,
            gsm = BigDecimal("130.0000")
        )

        assertNotNull(spec)
        val retrieved = service.getSpecification("TENANT-SRV", spec.signatureImpositionId)
        assertNotNull(retrieved)
        assertEquals(spec.signatureImpositionId, retrieved?.signatureImpositionId)
        assertEquals(spec.integrityHash, retrieved?.integrityHash)
        assertEquals(SignatureStatus.OPTIMIZED, retrieved?.status)
    }

    @Test
    fun `updateStatus should mutate status and record audit in repository`() = runBlocking {
        val spec = service.optimizeAndSave(
            tenantId = "TENANT-SRV",
            name = "Status Mutation Test",
            jobId = "JOB-SRV-02",
            orderId = "ORD-SRV-02",
            orderItemId = "ITEM-02",
            productName = "Status Booklet",
            totalPages = 16,
            pageDimension = a4Page,
            parentSheetDimension = pressSheet25x36,
            requiredQuantity = 500L,
            paperStockType = PaperStockType.ART_PAPER,
            gsm = BigDecimal("150.0000")
        )

        val updated = service.updateStatus(
            tenantId = "TENANT-SRV",
            signatureImpositionId = spec.signatureImpositionId,
            status = SignatureStatus.APPROVED,
            actor = "lead_prepress_operator",
            notes = "Approved for plate output"
        )

        assertTrue(updated)
        val reloaded = service.getSpecification("TENANT-SRV", spec.signatureImpositionId)
        assertEquals(SignatureStatus.APPROVED, reloaded?.status)
    }

    @Test
    fun `exportHandoffContract should emit cryptographic contract for Module 19`() = runBlocking {
        val spec = service.optimizeAndSave(
            tenantId = "TENANT-SRV",
            name = "Handoff Contract Test",
            jobId = "JOB-SRV-03",
            orderId = "ORD-SRV-03",
            orderItemId = "ITEM-03",
            productName = "Annual Report",
            totalPages = 48,
            signaturePageCount = 16,
            bindingMethod = BindingMethod.PERFECT_BOUND,
            sheetTurningMethod = SheetTurningMethod.SHEETWISE,
            foldingScheme = FoldingScheme.RIGHT_ANGLE_16PP,
            pageDimension = a4Page,
            parentSheetDimension = pressSheet25x36,
            requiredQuantity = 2000L,
            paperStockType = PaperStockType.ART_PAPER,
            gsm = BigDecimal("150.0000")
        )

        val contract = service.exportHandoffContract("TENANT-SRV", spec.signatureImpositionId)
        assertNotNull(contract)
        assertEquals("1.0.0", contract?.contractVersion)
        assertEquals(spec.signatureImpositionId, contract?.signatureImpositionId)
        assertEquals(3, contract?.totalSignatures) // 48 / 16 = 3 signatures
        assertEquals(2000L, contract?.sheetsPerSignature)
        assertEquals(6000L, contract?.totalParentSheetsRequired) // 3 * 2000 = 6000 sheets
        assertEquals(spec.integrityHash, contract?.integrityHash)
    }
}
