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
 * Multi-Tenant Isolation and Security Boundary Tests for Signature Imposition.
 * Module 18 Step 04.
 */
class SignatureImpositionSecurityEdgeTest {

    private lateinit var dataSource: FakeSignatureImpositionDataSource
    private lateinit var repository: SignatureImpositionRepositoryImpl
    private lateinit var service: SignatureImpositionService

    private val a4Page = PrintingDimension(BigDecimal("210.0000"), BigDecimal("297.0000"), MeasurementUnit.MILLIMETERS)
    private val pressSheet = PrintingDimension(BigDecimal("635.0000"), BigDecimal("914.4000"), MeasurementUnit.MILLIMETERS)

    @Before
    fun setup() {
        dataSource = FakeSignatureImpositionDataSource()
        repository = SignatureImpositionRepositoryImpl(dataSource)
        service = SignatureImpositionServiceImpl(repository)
    }

    @Test
    fun `tenant isolation prevents cross-tenant access to signature imposition specifications`() = runBlocking {
        val specTenantA = service.optimizeAndSave(
            tenantId = "TENANT-ALPHA",
            name = "Confidential Financial Report 2026",
            jobId = "JOB-SEC-01",
            orderId = "ORD-SEC-01",
            orderItemId = "ITEM-01",
            productName = "Financial Report",
            totalPages = 32,
            pageDimension = a4Page,
            parentSheetDimension = pressSheet,
            requiredQuantity = 1000L,
            paperStockType = PaperStockType.ART_PAPER,
            gsm = BigDecimal("150.0000")
        )

        // Tenant Alpha can retrieve its specification
        val foundAlpha = service.getSpecification("TENANT-ALPHA", specTenantA.signatureImpositionId)
        assertNotNull(foundAlpha)

        // Tenant Beta attempting to access Tenant Alpha's specification must receive null
        val foundBeta = service.getSpecification("TENANT-BETA", specTenantA.signatureImpositionId)
        assertNull("Cross-tenant reading must be blocked by tenant isolation", foundBeta)

        // Tenant Beta cannot update status of Tenant Alpha's specification
        val mutatedBeta = service.updateStatus(
            tenantId = "TENANT-BETA",
            signatureImpositionId = specTenantA.signatureImpositionId,
            status = SignatureStatus.CANCELLED,
            actor = "malicious_user"
        )
        assertFalse("Cross-tenant status mutation must return false", mutatedBeta)

        // Tenant Alpha's status remains untouched
        val reloadedAlpha = service.getSpecification("TENANT-ALPHA", specTenantA.signatureImpositionId)
        assertEquals(SignatureStatus.OPTIMIZED, reloadedAlpha?.status)
    }

    @Test
    fun `empty tenant ID must throw IllegalArgumentException`() {
        assertThrows(IllegalArgumentException::class.java) {
            runBlocking {
                service.optimizeAndSave(
                    tenantId = "",
                    name = "Blank Tenant Spec",
                    jobId = "JOB-01",
                    orderId = "ORD-01",
                    orderItemId = "ITEM-01",
                    productName = "Catalog",
                    totalPages = 16,
                    pageDimension = a4Page,
                    parentSheetDimension = pressSheet,
                    requiredQuantity = 100L,
                    paperStockType = PaperStockType.ART_PAPER,
                    gsm = BigDecimal("120.0000")
                )
            }
        }
    }

    @Test
    fun `invalid zero or negative page count must throw IllegalArgumentException`() {
        assertThrows(IllegalArgumentException::class.java) {
            SignatureImpositionEngine.optimizeSignatureImposition(
                tenantId = "TENANT-001",
                name = "Invalid Pages Spec",
                jobId = "JOB-01",
                orderId = "ORD-01",
                orderItemId = "ITEM-01",
                productName = "Catalog",
                totalPages = 0, // Invalid: total pages must be > 0
                pageDimension = a4Page,
                parentSheetDimension = pressSheet,
                requiredQuantity = 100L,
                paperStockType = PaperStockType.ART_PAPER,
                gsm = BigDecimal("120.0000")
            )
        }
    }
}
