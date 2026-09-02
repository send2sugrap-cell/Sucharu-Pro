package com.sucharu.sucharupro.domain.service.commercialcommitment

import com.sucharu.sucharupro.domain.model.commercialcommitment.CommercialCommitment
import com.sucharu.sucharupro.domain.model.commercialcommitment.CommitmentStatus
import com.sucharu.sucharupro.domain.model.commercialcommitment.ConvertQuotationToOrderRequest
import com.sucharu.sucharupro.domain.model.common.Money
import com.sucharu.sucharupro.domain.model.order.*
import com.sucharu.sucharupro.domain.model.printingquote.PrintingQuote
import com.sucharu.sucharupro.domain.model.printingquote.PrintingQuoteVersion
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Instant
import java.util.UUID

/**
 * Pure, deterministic commercial conversion engine for creating Orders from Approved Quotations.
 * Module 17 Step 03.
 */
object CommercialCommitmentConversionEngine {

    /**
     * Converts an approved quotation and its calculated version snapshot into a canonical [Order] entity.
     * Enforces the snapshot principle so the resulting Order is self-contained and immutable against
     * future quotation modifications.
     */
    fun createOrderFromQuotation(
        orderId: String,
        orderNumber: String,
        quote: PrintingQuote,
        version: PrintingQuoteVersion,
        commitmentId: String,
        request: ConvertQuotationToOrderRequest,
        timestamp: Long = System.currentTimeMillis()
    ): Order {
        val isoTimestamp = Instant.ofEpochMilli(timestamp).toString()
        val customerId = quote.customerRef?.takeIf { it.isNotBlank() } ?: "DEFAULT_CUSTOMER"

        val qty = (request.requestedQuantity ?: version.quantityBreakdown.orderedQuantity).toInt()
        val unitPriceMoney = Money(
            version.pricing.baseSellingPrice.divide(
                BigDecimal(maxOf(version.quantityBreakdown.orderedQuantity, 1L)),
                2,
                RoundingMode.HALF_UP
            )
        )
        val discountMoney = Money(version.pricing.discountAmount.setScale(2, RoundingMode.HALF_UP))

        val orderItem = OrderItem(
            itemId = UUID.randomUUID().toString(),
            description = quote.jobTitle,
            specification = "Spec: ${version.specFingerprint} (Calculated v${version.versionNumber})",
            quantity = qty,
            unit = "Pcs",
            unitPrice = unitPriceMoney,
            discount = discountMoney,
            notes = quote.internalNote
        )

        val orderPriority = when (request.priority.uppercase()) {
            "URGENT" -> OrderPriority.URGENT
            "HIGH"   -> OrderPriority.HIGH
            else     -> OrderPriority.NORMAL
        }

        return Order(
            orderId = orderId,
            orderNumber = orderNumber,
            customerId = customerId,
            quotationId = quote.quoteId,
            approvedQuotationRevisionId = "v${version.versionNumber}",
            status = OrderStatusType.CONFIRMED,
            priority = orderPriority,
            items = listOf(orderItem),
            discount = discountMoney,
            deliveryRequirement = null,
            paymentTerms = PaymentTerms.DEFAULT,
            jobHandoffStatus = JobHandoffStatus.READY_FOR_JOB,
            notes = request.notes ?: quote.customerNote,
            confirmedAt = isoTimestamp,
            confirmedBy = request.requestedBy,
            createdAt = isoTimestamp,
            updatedAt = isoTimestamp
        )
    }

    /**
     * Builds or updates a [CommercialCommitment] entity from an approved quotation version.
     */
    fun buildCommitment(
        commitmentId: String,
        quote: PrintingQuote,
        version: PrintingQuoteVersion,
        request: ConvertQuotationToOrderRequest?,
        actor: String,
        idempotencyKey: String?,
        integrityHash: String,
        timestamp: Long = System.currentTimeMillis()
    ): CommercialCommitment {
        val qty = request?.requestedQuantity ?: version.quantityBreakdown.orderedQuantity
        val unitPrice = version.pricing.baseSellingPrice.divide(
            BigDecimal(maxOf(version.quantityBreakdown.orderedQuantity, 1L)),
            4,
            RoundingMode.HALF_UP
        )

        return CommercialCommitment(
            commitmentId = commitmentId,
            tenantId = quote.tenantId,
            projectId = quote.projectId,
            quotationId = quote.quoteId,
            quotationVersion = version.versionNumber,
            customerId = quote.customerRef?.takeIf { it.isNotBlank() } ?: "DEFAULT_CUSTOMER",
            orderId = null,
            orderNumber = null,
            status = CommitmentStatus.READY_FOR_CONVERSION,
            committedQuantity = qty,
            approvedUnitPrice = unitPrice,
            approvedSubtotal = version.pricing.baseSellingPrice,
            approvedDiscount = version.pricing.discountAmount,
            approvedTax = version.pricing.taxAmount,
            approvedGrandTotal = version.pricing.finalQuoteTotal,
            currency = quote.currency,
            paymentTerms = request?.paymentTerms ?: "DEFAULT",
            deliveryTerms = request?.deliveryTerms,
            conversionNotes = request?.notes ?: quote.customerNote,
            idempotencyKey = idempotencyKey,
            integrityHash = integrityHash,
            createdAt = timestamp,
            createdBy = actor,
            convertedAt = null,
            convertedBy = null
        )
    }
}
