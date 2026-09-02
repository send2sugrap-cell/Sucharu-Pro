package com.sucharu.sucharupro.domain.validation.commercialcommitment

import com.sucharu.sucharupro.domain.model.commercialcommitment.CommercialCommitment
import com.sucharu.sucharupro.domain.model.commercialcommitment.CommitmentStatus
import com.sucharu.sucharupro.domain.model.printingquote.*
import org.junit.Assert.*
import org.junit.Test
import java.math.BigDecimal

class CommercialCommitmentValidatorTest {

    private fun createTestQuote(
        status: QuoteStatus = QuoteStatus.APPROVED,
        tenantId: String = "tenant_test",
        expiresAt: Long? = null
    ) = PrintingQuote(
        quoteId = "QUO-001",
        tenantId = tenantId,
        projectId = tenantId,
        quoteNumber = "PQ-2026-001",
        jobTitle = "Flyers",
        calculationId = "CALC-001",
        requestFingerprint = "FINGERPRINT-001",
        status = status,
        currentVersion = 1,
        currency = "BDT",
        orderedQuantity = 1000L,
        customerRef = "CUST-001",
        createdBy = "user_001",
        createdAt = System.currentTimeMillis(),
        updatedAt = System.currentTimeMillis(),
        expiresAt = expiresAt,
        integrityHash = "HASH-001"
    )

    private fun createTestVersion(
        tenantId: String = "tenant_test",
        quantity: Long = 1000L,
        total: BigDecimal = BigDecimal("5000.0000")
    ) = PrintingQuoteVersion(
        versionId = "VER-001",
        quoteId = "QUO-001",
        tenantId = tenantId,
        projectId = tenantId,
        versionNumber = 1,
        status = QuoteStatus.APPROVED,
        currency = "BDT",
        calculationId = "CALC-001",
        specFingerprint = "SPEC-001",
        calcFingerprint = "CALC-001",
        quantityBreakdown = QuoteQuantityBreakdown(
            orderedQuantity = quantity,
            producedQuantity = quantity + 50L,
            sellableQuantity = quantity,
            wastageQuantity = 50L,
            wastagePercentage = BigDecimal("5.0000"),
            impositionUps = 1
        ),
        costingAssumptions = CostingAssumptions(),
        pricingAssumptions = PricingAssumptions(),
        totalCost = BigDecimal("4000.0000"),
        unitCost = BigDecimal("4.0000"),
        pricing = PrintingPricingSnapshot(
            baseSellingPrice = total,
            finalQuoteTotal = total,
            markupAmount = BigDecimal("1000.0000"),
            markupPercentage = BigDecimal("25.0000"),
            grossProfit = BigDecimal("1000.0000"),
            grossMarginPercentage = BigDecimal("20.0000"),
            contributionAmount = BigDecimal("1000.0000"),
            contributionMarginPercentage = BigDecimal("20.0000"),
            breakEvenPrice = BigDecimal("4.0000"),
            breakEvenQuantity = 800L,
            targetMarginPrice = null,
            targetMarginPercentage = null
        ),
        integrityHash = "VER-HASH-001",
        createdBy = "user_001",
        createdAt = System.currentTimeMillis(),
        isApproved = true
    )

    @Test
    fun `approved quote with valid version is eligible for conversion`() {
        val quote = createTestQuote()
        val version = createTestVersion()
        val result = CommercialCommitmentValidator.evaluateEligibility("tenant_test", quote, version, null)
        assertTrue(result.isEligible)
        assertTrue(result.reasons.isEmpty())
        assertEquals("QUO-001", result.quotationId)
    }

    @Test
    fun `unapproved quote is not eligible for conversion`() {
        val quote = createTestQuote(status = QuoteStatus.CALCULATED)
        val version = createTestVersion()
        val result = CommercialCommitmentValidator.evaluateEligibility("tenant_test", quote, version, null)
        assertFalse(result.isEligible)
        assertTrue(result.reasons.any { it.contains("Only APPROVED quotations") })
    }

    @Test
    fun `expired quote is blocked from conversion`() {
        val quote = createTestQuote(expiresAt = System.currentTimeMillis() - 100000)
        val version = createTestVersion()
        val result = CommercialCommitmentValidator.evaluateEligibility("tenant_test", quote, version, null)
        assertFalse(result.isEligible)
        assertTrue(result.reasons.any { it.contains("expired") })
    }

    @Test
    fun `already converted quote is blocked from duplicate conversion`() {
        val quote = createTestQuote()
        val version = createTestVersion()
        val existingCommitment = CommercialCommitment(
            commitmentId = "COMM-001",
            tenantId = "tenant_test",
            projectId = "tenant_test",
            quotationId = "QUO-001",
            quotationVersion = 1,
            customerId = "CUST-001",
            orderId = "ORD-001",
            orderNumber = "ORD-12345",
            status = CommitmentStatus.CONVERTED,
            committedQuantity = 1000L,
            approvedUnitPrice = BigDecimal("5.0000"),
            approvedSubtotal = BigDecimal("5000.0000"),
            approvedGrandTotal = BigDecimal("5000.0000"),
            integrityHash = "HASH",
            createdAt = System.currentTimeMillis(),
            createdBy = "user_001"
        )
        val result = CommercialCommitmentValidator.evaluateEligibility("tenant_test", quote, version, existingCommitment)
        assertFalse(result.isEligible)
        assertTrue(result.reasons.any { it.contains("already been converted") })
    }
}
