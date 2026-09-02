package com.sucharu.sucharupro.domain.service.commercialcommitment

import com.sucharu.sucharupro.domain.model.commercialcommitment.ConvertQuotationToOrderRequest
import com.sucharu.sucharupro.domain.model.order.OrderStatusType
import com.sucharu.sucharupro.domain.model.printingquote.*
import org.junit.Assert.*
import org.junit.Test
import java.math.BigDecimal

class CommercialCommitmentConversionEngineTest {

    private fun createTestQuote() = PrintingQuote(
        quoteId = "QUO-001",
        tenantId = "tenant_test",
        projectId = "tenant_test",
        quoteNumber = "PQ-2026-001",
        jobTitle = "Art Paper Brochure",
        calculationId = "CALC-001",
        requestFingerprint = "FINGERPRINT-001",
        status = QuoteStatus.APPROVED,
        currentVersion = 1,
        currency = "BDT",
        orderedQuantity = 500L,
        customerRef = "CUST-VIP-001",
        createdBy = "user_001",
        createdAt = System.currentTimeMillis(),
        updatedAt = System.currentTimeMillis(),
        integrityHash = "HASH-001"
    )

    private fun createTestVersion() = PrintingQuoteVersion(
        versionId = "VER-001",
        quoteId = "QUO-001",
        tenantId = "tenant_test",
        projectId = "tenant_test",
        versionNumber = 1,
        status = QuoteStatus.APPROVED,
        currency = "BDT",
        calculationId = "CALC-001",
        specFingerprint = "SPEC-001",
        calcFingerprint = "CALC-001",
        quantityBreakdown = QuoteQuantityBreakdown(
            orderedQuantity = 500L,
            producedQuantity = 550L,
            sellableQuantity = 500L,
            wastageQuantity = 50L,
            wastagePercentage = BigDecimal("10.0000"),
            impositionUps = 1
        ),
        costingAssumptions = CostingAssumptions(),
        pricingAssumptions = PricingAssumptions(),
        totalCost = BigDecimal("10000.0000"),
        unitCost = BigDecimal("20.0000"),
        pricing = PrintingPricingSnapshot(
            baseSellingPrice = BigDecimal("15000.0000"),
            finalQuoteTotal = BigDecimal("15000.0000"),
            markupAmount = BigDecimal("5000.0000"),
            markupPercentage = BigDecimal("50.0000"),
            grossProfit = BigDecimal("5000.0000"),
            grossMarginPercentage = BigDecimal("33.3333"),
            contributionAmount = BigDecimal("5000.0000"),
            contributionMarginPercentage = BigDecimal("33.3333"),
            breakEvenPrice = BigDecimal("20.0000"),
            breakEvenQuantity = 333L,
            targetMarginPrice = null,
            targetMarginPercentage = null
        ),
        integrityHash = "VER-HASH-001",
        createdBy = "user_001",
        createdAt = System.currentTimeMillis(),
        isApproved = true
    )

    @Test
    fun `conversion engine accurately maps approved quotation to Order snapshot`() {
        val quote = createTestQuote()
        val version = createTestVersion()
        val request = ConvertQuotationToOrderRequest(
            quotationId = "QUO-001",
            tenantId = "tenant_test",
            projectId = "tenant_test",
            customOrderNumber = "ORD-MANUAL-99",
            requestedBy = "admin_user"
        )

        val order = CommercialCommitmentConversionEngine.createOrderFromQuotation(
            orderId = "ORD-001",
            orderNumber = "ORD-MANUAL-99",
            quote = quote,
            version = version,
            commitmentId = "COMM-001",
            request = request
        )

        assertEquals("ORD-001", order.orderId)
        assertEquals("ORD-MANUAL-99", order.orderNumber)
        assertEquals("CUST-VIP-001", order.customerId)
        assertEquals("QUO-001", order.quotationId)
        assertEquals(OrderStatusType.CONFIRMED, order.status)
        assertEquals(1, order.items.size)
        assertEquals(500, order.items[0].quantity)
        assertEquals("Art Paper Brochure", order.items[0].description)
        assertEquals(BigDecimal("15000.00"), order.totalAmount.amount)
    }
}
