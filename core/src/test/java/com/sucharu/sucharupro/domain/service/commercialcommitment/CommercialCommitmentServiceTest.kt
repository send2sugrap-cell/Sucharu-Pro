package com.sucharu.sucharupro.domain.service.commercialcommitment

import com.sucharu.sucharupro.data.datasource.FakeOrderDataSource
import com.sucharu.sucharupro.data.datasource.commercialcommitment.FakeCommercialCommitmentDataSource
import com.sucharu.sucharupro.data.datasource.printingquote.FakePrintingQuoteDataSource
import com.sucharu.sucharupro.data.repository.OrderRepositoryImpl
import com.sucharu.sucharupro.data.repository.commercialcommitment.CommercialCommitmentRepositoryImpl
import com.sucharu.sucharupro.data.repository.printingquote.PrintingQuoteRepositoryImpl
import com.sucharu.sucharupro.domain.model.commercialcommitment.CommitmentStatus
import com.sucharu.sucharupro.domain.model.commercialcommitment.ConvertQuotationToOrderRequest
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.printingquote.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

class CommercialCommitmentServiceTest {

    private lateinit var commitmentRepository: CommercialCommitmentRepositoryImpl
    private lateinit var quoteRepository: PrintingQuoteRepositoryImpl
    private lateinit var orderRepository: OrderRepositoryImpl
    private lateinit var service: CommercialCommitmentServiceImpl

    private val tenantId = "tenant_test_001"

    @Before
    fun setUp() {
        val commDs = FakeCommercialCommitmentDataSource()
        val quoteDs = FakePrintingQuoteDataSource()
        val orderDs = FakeOrderDataSource()

        commitmentRepository = CommercialCommitmentRepositoryImpl(commDs)
        quoteRepository = PrintingQuoteRepositoryImpl(quoteDs)
        orderRepository = OrderRepositoryImpl(orderDs)

        service = CommercialCommitmentServiceImpl(
            commitmentRepository = commitmentRepository,
            quoteRepository = quoteRepository,
            orderRepository = orderRepository
        )
    }

    private fun setupApprovedQuote(quoteId: String = "QUO-TEST-001"): PrintingQuote {
        val now = System.currentTimeMillis()
        val quote = PrintingQuote(
            quoteId = quoteId,
            tenantId = tenantId,
            projectId = tenantId,
            quoteNumber = "PQ-2026-901",
            jobTitle = "Annual Magazine",
            calculationId = "CALC-001",
            requestFingerprint = "FP-001",
            status = QuoteStatus.APPROVED,
            currentVersion = 1,
            currency = "BDT",
            orderedQuantity = 1000L,
            customerRef = "CUST-001",
            createdBy = "tester",
            createdAt = now,
            updatedAt = now,
            approvedAt = now,
            approvedBy = "manager_001",
            integrityHash = "HASH"
        )
        val version = PrintingQuoteVersion(
            versionId = "VER-$quoteId-1",
            quoteId = quoteId,
            tenantId = tenantId,
            projectId = tenantId,
            versionNumber = 1,
            status = QuoteStatus.APPROVED,
            currency = "BDT",
            calculationId = "CALC-001",
            specFingerprint = "SPEC-001",
            calcFingerprint = "CALC-001",
            quantityBreakdown = QuoteQuantityBreakdown(
                orderedQuantity = 1000L,
                producedQuantity = 1100L,
                sellableQuantity = 1000L,
                wastageQuantity = 100L,
                wastagePercentage = BigDecimal("10.0000"),
                impositionUps = 1
            ),
            costingAssumptions = CostingAssumptions(),
            pricingAssumptions = PricingAssumptions(),
            totalCost = BigDecimal("50000.0000"),
            unitCost = BigDecimal("50.0000"),
            pricing = PrintingPricingSnapshot(
                baseSellingPrice = BigDecimal("75000.0000"),
                finalQuoteTotal = BigDecimal("75000.0000"),
                markupAmount = BigDecimal("25000.0000"),
                markupPercentage = BigDecimal("50.0000"),
                grossProfit = BigDecimal("25000.0000"),
                grossMarginPercentage = BigDecimal("33.3333"),
                contributionAmount = BigDecimal("25000.0000"),
                contributionMarginPercentage = BigDecimal("33.3333"),
                breakEvenPrice = BigDecimal("50.0000"),
                breakEvenQuantity = 667L,
                targetMarginPrice = null,
                targetMarginPercentage = null
            ),
            integrityHash = "VHASH",
            createdBy = "tester",
            createdAt = now,
            isApproved = true
        )
        runBlocking {
            quoteRepository.saveQuote(quote)
            quoteRepository.saveQuoteVersion(version)
        }
        return quote
    }

    @Test
    fun `full commercial conversion lifecycle from eligibility to order conversion and reconciliation`() = runBlocking {
        val quoteId = "QUO-LIFECYCLE-001"
        setupApprovedQuote(quoteId)

        // 1. Evaluate Eligibility
        val elRes = service.evaluateEligibility(tenantId, quoteId)
        assertTrue(elRes is DomainResult.Success)
        val eligibility = (elRes as DomainResult.Success).data
        assertTrue(eligibility.isEligible)
        assertEquals("QUO-LIFECYCLE-001", eligibility.quotationId)

        // 2. Prepare Commitment
        val prepRes = service.prepareCommitment(tenantId, quoteId, 1, null, "admin_user")
        assertTrue(prepRes is DomainResult.Success)
        val commitment = (prepRes as DomainResult.Success).data
        assertEquals(CommitmentStatus.READY_FOR_CONVERSION, commitment.status)
        assertEquals(BigDecimal("75000.0000"), commitment.approvedGrandTotal)

        // 3. Convert to Order
        val req = ConvertQuotationToOrderRequest(
            quotationId = quoteId,
            tenantId = tenantId,
            projectId = tenantId,
            customOrderNumber = "ORD-TEST-001",
            idempotencyKey = "IDEM-001",
            requestedBy = "admin_user"
        )
        val convRes = service.convertQuotationToOrder(tenantId, req)
        assertTrue(convRes is DomainResult.Success)
        val conversion = (convRes as DomainResult.Success).data
        assertTrue(conversion.isSuccess)
        assertEquals("ORD-TEST-001", conversion.orderNumber)
        assertEquals(CommitmentStatus.CONVERTED, conversion.commitment.status)

        // 4. Verify Idempotency on second conversion call
        val convRes2 = service.convertQuotationToOrder(tenantId, req)
        assertTrue(convRes2 is DomainResult.Success)
        val conversion2 = (convRes2 as DomainResult.Success).data
        assertEquals(conversion.orderId, conversion2.orderId)
        assertTrue(conversion2.message.contains("Idempotent duplicate"))

        // 5. Reconcile Commitment with Order and Quote
        val reconRes = service.reconcileCommitment(tenantId, conversion.commitment.commitmentId)
        assertTrue(reconRes is DomainResult.Success)
        val recon = (reconRes as DomainResult.Success).data
        assertTrue(recon.isFullyReconciled)
        assertTrue(recon.customerMatch)
        assertTrue(recon.currencyMatch)
        assertTrue(recon.amountMatch)

        // 6. Export AI Handoff Contract
        val handoffRes = service.exportHandoffContract(tenantId, conversion.commitment.commitmentId)
        assertTrue(handoffRes is DomainResult.Success)
        val handoff = (handoffRes as DomainResult.Success).data
        assertEquals("RECONCILED", handoff.reconciliationStatus)
        assertEquals("CONVERTED", handoff.commitmentStatus)
        assertTrue(handoff.integrityHash.isNotBlank())
    }
}
