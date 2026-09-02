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
 * Multi-Tenant Isolation and Security Boundary Tests for CTP Prepress Output.
 * Module 18 Step 05.
 */
class CtpOutputSecurityEdgeTest {

    private lateinit var ctpDataSource: FakeCtpOutputDataSource
    private lateinit var signatureDataSource: FakeSignatureImpositionDataSource
    private lateinit var ctpRepository: CtpOutputRepositoryImpl
    private lateinit var signatureRepository: SignatureImpositionRepositoryImpl
    private lateinit var ctpService: CtpOutputService

    private val a4Page = PrintingDimension(BigDecimal("210.0000"), BigDecimal("297.0000"), MeasurementUnit.MILLIMETERS)
    private val pressSheet = PrintingDimension(BigDecimal("635.0000"), BigDecimal("914.4000"), MeasurementUnit.MILLIMETERS)

    @Before
    fun setup() {
        ctpDataSource = FakeCtpOutputDataSource()
        signatureDataSource = FakeSignatureImpositionDataSource()
        ctpRepository = CtpOutputRepositoryImpl(ctpDataSource)
        signatureRepository = SignatureImpositionRepositoryImpl(signatureDataSource)
        ctpService = CtpOutputServiceImpl(
            ctpOutputRepository = ctpRepository,
            signatureImpositionRepository = signatureRepository
        )
    }

    private fun seedSignature(tenant: String, id: String): SignatureImpositionSpecification = runBlocking {
        val spec = SignatureImpositionEngine.optimizeSignatureImposition(
            tenantId = tenant,
            name = "Catalog Sig $id",
            jobId = "JOB-$id",
            orderId = "ORD-$id",
            orderItemId = "ITEM-$id",
            productName = "Product Catalog",
            totalPages = 16,
            signaturePageCount = 16,
            bindingMethod = BindingMethod.SADDLE_STITCH,
            sheetTurningMethod = SheetTurningMethod.SHEETWISE,
            foldingScheme = FoldingScheme.RIGHT_ANGLE_16PP,
            pageDimension = a4Page,
            parentSheetDimension = pressSheet,
            requiredQuantity = 1000L,
            paperStockType = PaperStockType.ART_PAPER,
            gsm = BigDecimal("150.0000")
        )
        val modified = spec.copy(signatureImpositionId = id)
        signatureDataSource.saveSpecification(tenant, modified)
        modified
    }

    @Test
    fun testTenantIsolation_preventsCrossTenantAccessToCtpPackages() {
        runBlocking {
            val sigAlpha = seedSignature("TENANT-ALPHA", "SIG-ALPHA-01")

            val ctpAlpha = ctpService.generateFromSignature(
                tenantId = "TENANT-ALPHA",
                signatureImpositionId = sigAlpha.signatureImpositionId,
                actor = "lead_prepress"
            )

            // Tenant Alpha can retrieve its CTP package
            val foundAlpha = ctpService.getSpecification("TENANT-ALPHA", ctpAlpha.ctpOutputId)
            assertNotNull(foundAlpha)
            assertEquals(ctpAlpha.ctpOutputId, foundAlpha?.ctpOutputId)

            // Tenant Beta attempting to access Tenant Alpha's CTP package must receive null
            val foundBeta = ctpService.getSpecification("TENANT-BETA", ctpAlpha.ctpOutputId)
            assertNull("Cross-tenant reading of CTP output package must be blocked", foundBeta)

            // Tenant Beta attempting to list specifications receives only its own
            val listBeta = ctpService.listSpecifications("TENANT-BETA")
            assertTrue("Tenant Beta list must not contain Tenant Alpha specifications", listBeta.isEmpty())

            // Tenant Beta cannot mutate status of Tenant Alpha's CTP package
            assertThrows(IllegalArgumentException::class.java) {
                runBlocking {
                    ctpService.updateStatus(
                        tenantId = "TENANT-BETA",
                        ctpOutputId = ctpAlpha.ctpOutputId,
                        newStatus = CtpOutputStatus.CANCELLED,
                        actor = "malicious_user"
                    )
                }
            }
        }
    }

    @Test(expected = IllegalArgumentException::class)
    fun testBlankTenantId_throwsIllegalArgumentException() {
        runBlocking {
            ctpService.getSpecification("", "CTP-001")
        }
    }

    @Test(expected = IllegalArgumentException::class)
    fun testBlankSignatureId_throwsIllegalArgumentException() {
        runBlocking {
            ctpService.generateFromSignature(
                tenantId = "TENANT-001",
                signatureImpositionId = "",
                actor = "operator"
            )
        }
    }

    @Test(expected = IllegalArgumentException::class)
    fun testMissingSignature_throwsIllegalArgumentException() {
        runBlocking {
            ctpService.generateFromSignature(
                tenantId = "TENANT-001",
                signatureImpositionId = "NON_EXISTENT_SIG",
                actor = "operator"
            )
        }
    }
}
