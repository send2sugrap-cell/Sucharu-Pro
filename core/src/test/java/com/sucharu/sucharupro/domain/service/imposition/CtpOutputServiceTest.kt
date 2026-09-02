package com.sucharu.sucharupro.domain.service.imposition

import com.sucharu.sucharupro.data.datasource.imposition.FakeCtpOutputDataSource
import com.sucharu.sucharupro.data.datasource.imposition.FakeSignatureImpositionDataSource
import com.sucharu.sucharupro.data.repository.imposition.CtpOutputRepositoryImpl
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
 * Integration and Service Tests for [CtpOutputService].
 * Module 18 Step 05.
 */
class CtpOutputServiceTest {

    private val tenantId = "tenant_ctp_service_test"
    private lateinit var ctpDataSource: FakeCtpOutputDataSource
    private lateinit var signatureDataSource: FakeSignatureImpositionDataSource
    private lateinit var ctpService: CtpOutputService

    @Before
    fun setUp() {
        ctpDataSource = FakeCtpOutputDataSource()
        signatureDataSource = FakeSignatureImpositionDataSource()

        val ctpRepo = CtpOutputRepositoryImpl(ctpDataSource)
        val sigRepo = SignatureImpositionRepositoryImpl(signatureDataSource)

        ctpService = CtpOutputServiceImpl(
            ctpOutputRepository = ctpRepo,
            signatureImpositionRepository = sigRepo
        )
    }

    private suspend fun seedSignatureSpecification(): SignatureImpositionSpecification {
        val pageDim = PrintingDimension(BigDecimal("210.0000"), BigDecimal("297.0000"), MeasurementUnit.MILLIMETERS)
        val sheetDim = PrintingDimension(BigDecimal("635.0000"), BigDecimal("914.4000"), MeasurementUnit.MILLIMETERS)

        val sigSpec = SignatureImpositionEngine.optimizeSignatureImposition(
            tenantId = tenantId,
            name = "Test Signature Imposition",
            jobId = "JOB-SIG-99",
            orderId = "ORD-88",
            orderItemId = "ITEM-77",
            productName = "Product Catalog",
            totalPages = 16,
            signaturePageCount = 16,
            bindingMethod = BindingMethod.SADDLE_STITCH,
            sheetTurningMethod = SheetTurningMethod.SHEETWISE,
            foldingScheme = FoldingScheme.RIGHT_ANGLE_16PP,
            pageDimension = pageDim,
            parentSheetDimension = sheetDim,
            requiredQuantity = 500L,
            paperStockType = PaperStockType.ART_PAPER,
            gsm = BigDecimal("150.0000")
        )
        signatureDataSource.saveSpecification(tenantId, sigSpec)
        return sigSpec
    }

    @Test
    fun testGenerateFromSignature_savesAndRetrievesSuccessfully() = runBlocking {
        val sigSpec = seedSignatureSpecification()

        val ctpSpec = ctpService.generateFromSignature(
            tenantId = tenantId,
            signatureImpositionId = sigSpec.signatureImpositionId,
            actor = "ctp_lead"
        )

        assertNotNull(ctpSpec)
        assertEquals(CtpOutputStatus.GENERATED, ctpSpec.status)
        assertEquals(8, ctpSpec.outputPackage.totalPlatesCount)

        val retrieved = ctpService.getSpecification(tenantId, ctpSpec.ctpOutputId)
        assertNotNull(retrieved)
        assertEquals(ctpSpec.ctpOutputId, retrieved?.ctpOutputId)
        assertEquals(ctpSpec.integrityHash, retrieved?.integrityHash)
    }

    @Test
    fun testStatusTransitions_approveAndExportLifecycle() = runBlocking {
        val sigSpec = seedSignatureSpecification()
        val ctpSpec = ctpService.generateFromSignature(
            tenantId = tenantId,
            signatureImpositionId = sigSpec.signatureImpositionId,
            actor = "ctp_lead"
        )

        // GENERATED -> APPROVED
        val approved = ctpService.updateStatus(
            tenantId = tenantId,
            ctpOutputId = ctpSpec.ctpOutputId,
            newStatus = CtpOutputStatus.APPROVED,
            actor = "prepress_manager",
            reason = "Prepress QC passed"
        )
        assertEquals(CtpOutputStatus.APPROVED, approved.status)

        // APPROVED -> EXPORTED_TO_RIP
        val exported = ctpService.updateStatus(
            tenantId = tenantId,
            ctpOutputId = ctpSpec.ctpOutputId,
            newStatus = CtpOutputStatus.EXPORTED_TO_RIP,
            actor = "rip_operator",
            reason = "Sent to Screen PlateRite 8600 CTP"
        )
        assertEquals(CtpOutputStatus.EXPORTED_TO_RIP, exported.status)
    }

    @Test
    fun testGetHandoffContract_emitsValidContract() = runBlocking {
        val sigSpec = seedSignatureSpecification()
        val ctpSpec = ctpService.generateFromSignature(
            tenantId = tenantId,
            signatureImpositionId = sigSpec.signatureImpositionId,
            actor = "ctp_lead"
        )

        val contract = ctpService.getHandoffContract(tenantId, ctpSpec.ctpOutputId)
        assertNotNull(contract)
        assertEquals("1.0.0", contract.contractVersion)
        assertEquals(tenantId, contract.tenantId)
        assertEquals(ctpSpec.ctpOutputId, contract.ctpOutputId)
        assertEquals(ctpSpec.integrityHash, contract.ctpOutputIntegrityHash)
        assertEquals(8, contract.totalPlatesCount)
    }
}
