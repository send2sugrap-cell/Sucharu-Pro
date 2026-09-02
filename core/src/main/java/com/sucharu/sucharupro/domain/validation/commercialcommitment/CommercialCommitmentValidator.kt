package com.sucharu.sucharupro.domain.validation.commercialcommitment

import com.sucharu.sucharupro.domain.model.commercialcommitment.CommercialCommitment
import com.sucharu.sucharupro.domain.model.commercialcommitment.CommitmentStatus
import com.sucharu.sucharupro.domain.model.commercialcommitment.ConversionEligibility
import com.sucharu.sucharupro.domain.model.printingquote.PrintingQuote
import com.sucharu.sucharupro.domain.model.printingquote.PrintingQuoteVersion
import com.sucharu.sucharupro.domain.model.printingquote.QuoteStatus
import java.math.BigDecimal

/**
 * Pure, deterministic business rule validator for Approved Quotation -> Order Conversion eligibility.
 * Module 17 Step 03.
 */
object CommercialCommitmentValidator {

    /**
     * Evaluates whether a given printing quotation and its version are eligible to be converted
     * into a commercial commitment and downstream Order.
     */
    fun evaluateEligibility(
        tenantId: String,
        quote: PrintingQuote?,
        version: PrintingQuoteVersion?,
        existingCommitment: CommercialCommitment?,
        now: Long = System.currentTimeMillis()
    ): ConversionEligibility {
        val reasons = mutableListOf<String>()

        if (quote == null) {
            return ConversionEligibility(
                isEligible = false,
                reasons = listOf("Quotation not found."),
                quotationId = "",
                quotationVersion = 0,
                quotationStatus = "NOT_FOUND",
                customerId = "",
                orderedQuantity = 0L,
                approvedGrandTotal = BigDecimal.ZERO,
                currency = "BDT",
                evaluatedAt = now
            )
        }

        // 1. Tenant Ownership
        if (quote.tenantId != tenantId) {
            reasons += "Tenant mismatch: quotation does not belong to the requesting tenant."
        }

        // 2. Quotation Status
        if (quote.status != QuoteStatus.APPROVED) {
            reasons += "Quotation status is '${quote.status}'. Only APPROVED quotations can be converted to an order."
        }

        // 3. Expiration Check
        if (quote.expiresAt != null && quote.expiresAt < now) {
            reasons += "Quotation has expired at ${quote.expiresAt}."
        }

        // 4. Version Check
        if (version == null) {
            reasons += "No calculated version snapshot found for quotation."
        } else {
            if (version.tenantId != tenantId) {
                reasons += "Version tenant mismatch."
            }
            if (version.quantityBreakdown.orderedQuantity <= 0) {
                reasons += "Ordered quantity must be greater than zero."
            }
            if (version.pricing.finalQuoteTotal < BigDecimal.ZERO) {
                reasons += "Approved grand total cannot be negative."
            }
        }

        // 5. Customer Reference
        val customerId = quote.customerRef?.takeIf { it.isNotBlank() } ?: "DEFAULT_CUSTOMER"

        // 6. Duplicate Conversion Check
        if (existingCommitment != null) {
            if (existingCommitment.status == CommitmentStatus.CONVERTED) {
                reasons += "Quotation has already been converted to Order #${existingCommitment.orderNumber ?: existingCommitment.orderId}."
            } else if (existingCommitment.status == CommitmentStatus.CANCELLED) {
                reasons += "Existing commercial commitment was cancelled."
            }
        }

        val qty = version?.quantityBreakdown?.orderedQuantity ?: quote.orderedQuantity
        val grandTotal = version?.pricing?.finalQuoteTotal ?: BigDecimal.ZERO

        return ConversionEligibility(
            isEligible = reasons.isEmpty(),
            reasons = reasons,
            quotationId = quote.quoteId,
            quotationVersion = version?.versionNumber ?: quote.currentVersion,
            quotationStatus = quote.status.name,
            customerId = customerId,
            orderedQuantity = qty,
            approvedGrandTotal = grandTotal,
            currency = quote.currency,
            existingCommitmentId = existingCommitment?.commitmentId,
            existingOrderId = existingCommitment?.orderId,
            evaluatedAt = now
        )
    }
}
