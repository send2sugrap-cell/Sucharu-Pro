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
 * Multi-Tenant Isolation & Security Boundary Edge Tests for Dynamic Nesting.
 * Module 18 Step 03.
 */
class DynamicNestingSecurityEdgeTest {

    private lateinit var service: DynamicNestingService

    @Before
    fun setUp() {
        val fakeDataSource = FakeDynamicNestingDataSource()
        val repository = DynamicNestingRepositoryImpl(fakeDataSource)
        service = DynamicNestingServiceImpl(repository)
    }

    @Test
    fun `cross tenant data leakage is prevented`() = runBlocking {
        val candidate = NestingCandidateItem(
            jobId = "JOB-TENANT-A",
            orderId = "ORD-A",
            orderItemId = "ITEM-A",
            productName = "Secret Document",
            finishedDimension = PrintingDimension(BigDecimal("210.0000"), BigDecimal("297.0000"), MeasurementUnit.MILLIMETERS),
            requiredQuantity = 1000L,
            paperStockType = PaperStockType.ART_PAPER,
            gsm = BigDecimal("150.0000")
        )
        val parentSheet = PrintingDimension(BigDecimal("635.0000"), BigDecimal("914.4000"), MeasurementUnit.MILLIMETERS)

        val specTenantA = service.optimizeAndSave(
            tenantId = "TENANT_ALPHA",
            name = "Confidential Alpha Run",
            candidateItems = listOf(candidate),
            parentSheetDimension = parentSheet,
            saveSpecification = true
        )

        // Tenant Beta attempts to read Tenant Alpha's specification
        val readByBeta = service.getNestingSpecification("TENANT_BETA", specTenantA.nestingId)
        assertNull("Tenant Beta must NOT be able to access Tenant Alpha's nesting specification", readByBeta)

        // Tenant Beta lists specifications
        val listBeta = service.listNestingSpecifications("TENANT_BETA")
        assertTrue("Tenant Beta's list must be empty", listBeta.isEmpty())
    }

    @Test
    fun `cross tenant status modification is denied`() = runBlocking {
        val candidate = NestingCandidateItem(
            jobId = "JOB-A",
            orderId = "ORD-A",
            orderItemId = "ITEM-A",
            productName = "Brochure",
            finishedDimension = PrintingDimension(BigDecimal("148.0000"), BigDecimal("210.0000"), MeasurementUnit.MILLIMETERS),
            requiredQuantity = 500L,
            paperStockType = PaperStockType.ART_CARD,
            gsm = BigDecimal("300.0000")
        )
        val parentSheet = PrintingDimension(BigDecimal("635.0000"), BigDecimal("914.4000"), MeasurementUnit.MILLIMETERS)

        val spec = service.optimizeAndSave("TENANT_ALPHA", "Alpha Run", listOf(candidate), parentSheet, saveSpecification = true)

        // Tenant Beta attempts to cancel Tenant Alpha's nesting spec
        val updateByBeta = service.updateNestingStatus(
            tenantId = "TENANT_BETA",
            nestingId = spec.nestingId,
            status = NestingStatus.CANCELLED,
            actor = "malicious_user"
        )

        assertFalse("Tenant Beta must fail to update Tenant Alpha's nesting spec", updateByBeta)

        val specAlpha = service.getNestingSpecification("TENANT_ALPHA", spec.nestingId)
        assertEquals(NestingStatus.OPTIMIZED, specAlpha?.status)
    }
}
