package com.sucharu.sucharupro.domain.service.printingquote

import com.sucharu.sucharupro.data.datasource.printingquote.FakePrintingQuoteDataSource
import com.sucharu.sucharupro.data.repository.printingquote.PrintingQuoteRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.printingcalculator.*
import com.sucharu.sucharupro.domain.model.printingquote.*
import com.sucharu.sucharupro.domain.model.product.ProductType
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

class PrintingQuoteServiceTest {

    private lateinit var dataSource: FakePrintingQuoteDataSource
    private lateinit var repository: PrintingQuoteRepositoryImpl
    private lateinit var service: PrintingQuoteServiceImpl

    private val tenantId = "TENANT-001"
    private val projectId = "PROJ-001"
    private val userId = "USER-ADMIN-01"

    @Before
    fun setUp() {
        dataSource = FakePrintingQuoteDataSource()
        repository = PrintingQuoteRepositoryImpl(dataSource)
        service = PrintingQuoteServiceImpl(repository)
    }

    private fun createDummyStep01(): PrintingCalculationResult {
        val now = System.currentTimeMillis()
        val dim = PrintingDimension(
            width = BigDecimal("210.0000"),
            height = BigDecimal("297.0000"),
            unit = MeasurementUnit.MILLIMETERS
        )
        val sheetDim = PrintingDimension(
            width = BigDecimal("635.0000"),
            height = BigDecimal("914.0000"),
            unit = MeasurementUnit.MILLIMETERS
        )
        val normSpec = NormalizedPrintingSpecification(
            jobTitle = "Brochure 1000 Pcs",
            productType = ProductType.PRINTING_JOB,
            finishedDimension = dim,
            normalizedDimensionMm = dim,
            quantity = QuantitySpecification(
                orderedQuantity = 1000L,
                unit = QuantityUnit.PIECES
            ),
            material = PaperMaterialSpecification(
                materialName = "Art Paper 150gsm",
                stockType = PaperStockType.ART_PAPER,
                sheetDimension = sheetDim,
                unitPricePerSheet = BigDecimal("8.5000")
            ),
            processType = PrintingProcessType.OFFSET,
            sides = PrintingSideOption.DOUBLE_SIDED_SAME,
            color = ColorSpecification(
                colorMode = ColorMode.CMYK_FOUR_COLOR,
                frontColorsCount = 4,
                backColorsCount = 4
            )
        )

        return PrintingCalculationResult(
            calculationId = "CALC-001",
            tenantId = tenantId,
            projectId = projectId,
            requestFingerprint = "FP-REQ-001",
            requestedAt = now,
            calculatedAt = now,
            status = CalculationStatus.SUCCESSFUL,
            classification = EstimateActualClassification.ESTIMATED,
            normalizedSpecification = normSpec,
            materialRequirement = MaterialRequirementResult(
                finishedItemsPerSheet = 8,
                cutDirection = "STANDARD_GRID",
                productiveSheetsRequired = 500L,
                wasteSheetsRequired = 150L,
                totalSheetsRequired = 650L,
                totalReamsRequired = BigDecimal("1.3000"),
                totalWeightKg = BigDecimal("25.0000"),
                estimatedMaterialCost = BigDecimal("5525.0000"),
                costStatus = CalculationStatus.SUCCESSFUL
            ),
            printingRequirement = PrintingRequirementResult(
                totalImpressions = 1300L,
                totalPasses = 2,
                plateCount = 4,
                estimatedPrintingCost = BigDecimal("2500.0000"),
                estimatedPlateCost = BigDecimal("1200.0000"),
                costStatus = CalculationStatus.SUCCESSFUL
            ),
            finishingRequirement = FinishingRequirementResult(
                operations = emptyList(),
                totalEstimatedFinishingCost = BigDecimal.ZERO,
                costStatus = CalculationStatus.SUCCESSFUL
            ),
            breakdownItems = emptyList(),
            totalEstimatedCost = BigDecimal("9225.0000"),
            estimatedUnitCost = BigDecimal("9.2250"),
            currency = "BDT",
            diagnostics = emptyList(),
            integrityHash = "HASH-CALC-001"
        )
    }

    @Test
    fun `full quote lifecycle from creation to calculation to review and handoff export`() = runBlocking {
        // 1. Create Quote
        val createReq = CreatePrintingQuoteRequest(
            tenantId = tenantId,
            projectId = projectId,
            calculationId = "CALC-001",
            jobTitle = "Brochure 1000 Pcs",
            customerRef = "CUST-001",
            requestedBy = userId
        )
        val createResult = service.createQuote(createReq)
        assertTrue(createResult is DomainResult.Success)
        val quote = (createResult as DomainResult.Success).data
        assertEquals(QuoteStatus.DRAFT, quote.status)
        assertEquals(0, quote.currentVersion)

        // 2. Calculate Quote Version 1
        val step01 = createDummyStep01()
        val calcReq = CalculatePrintingQuoteRequest(
            quoteId = quote.quoteId,
            tenantId = tenantId,
            projectId = projectId,
            costingAssumptions = CostingAssumptions(
                overheadAllocationPct = BigDecimal("10.0000"),
                wastageCosted = true
            ),
            pricingAssumptions = PricingAssumptions(
                pricingMethod = "COST_PLUS",
                markupPercentage = BigDecimal("25.0000"),
                taxPercentage = BigDecimal("5.0000")
            ),
            quantityTierBreaks = listOf(500L, 1000L, 2000L),
            requestedBy = userId
        )
        val calcResult = service.calculateQuote(calcReq, step01)
        assertTrue(calcResult is DomainResult.Success)
        val version = (calcResult as DomainResult.Success).data
        assertEquals(1, version.versionNumber)
        assertEquals(QuoteStatus.CALCULATED, version.status)
        assertTrue(version.costComponents.isNotEmpty())
        assertTrue(version.quantityTiers.isNotEmpty())

        // 3. Submit for Review
        val submitResult = service.submitForReview(quote.quoteId, tenantId, projectId, userId)
        assertTrue(submitResult is DomainResult.Success)
        val reviewQuote = (submitResult as DomainResult.Success).data
        assertEquals(QuoteStatus.REVIEW, reviewQuote.status)

        // 4. Review & Approve Quote
        val reviewReq = QuoteReviewRequest(
            quoteId = quote.quoteId,
            tenantId = tenantId,
            projectId = projectId,
            approved = true,
            reason = "Pricing approved by management",
            requestedBy = userId
        )
        val approveResult = service.reviewQuote(reviewReq)
        assertTrue(approveResult is DomainResult.Success)
        val approvedQuote = (approveResult as DomainResult.Success).data
        assertEquals(QuoteStatus.APPROVED, approvedQuote.status)
        assertNotNull(approvedQuote.approvedAt)

        // 5. Reconcile Quote Version
        val reconcileResult = service.reconcileQuote(quote.quoteId, version.versionId, tenantId, projectId, userId)
        assertTrue(reconcileResult is DomainResult.Success)
        val reconEvent = (reconcileResult as DomainResult.Success).data
        assertTrue(reconEvent.isReconciled)
        assertTrue(reconEvent.totalCostCheck)

        // 6. Export Downstream AI Handoff Contract
        val handoffResult = service.exportHandoffContract(tenantId, quote.quoteId)
        assertTrue(handoffResult is DomainResult.Success)
        val contract = (handoffResult as DomainResult.Success).data
        assertEquals(quote.quoteNumber, contract.quoteNumber)
        assertEquals(QuoteStatus.APPROVED.name, contract.status)
        assertTrue(contract.isReadOnly)
        assertFalse(contract.isMutable)
        assertTrue(contract.integrityHash.isNotBlank())
    }
}
