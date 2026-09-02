package com.sucharu.sucharupro.domain.model.order

import com.sucharu.sucharupro.domain.model.common.Money

/**
 * Historical immutable snapshot revision of a Quotation in Sucharu Pro.
 *
 * Preserves commercial integrity across negotiation iterations (e.g. V1, V2, V3).
 */
data class QuotationRevision(
    val revisionId: String,
    val quotationId: String,
    val revisionNumber: Int,
    val items: List<QuotationItem>,
    val discount: Money = Money.ZERO,
    val deliveryRequirement: DeliveryRequirement? = null,
    val paymentTerms: PaymentTerms = PaymentTerms.DEFAULT,
    val notes: String? = null,
    val revisionReason: String? = null,
    val createdAt: String,
    val createdBy: String? = null,
    val previousRevisionId: String? = null
) {
    init {
        require(revisionId.isNotBlank()) { "Revision ID cannot be blank." }
        require(quotationId.isNotBlank()) { "Quotation ID cannot be blank." }
        require(revisionNumber >= 1) { "Revision number must be >= 1 (was $revisionNumber)." }
        require(items.isNotEmpty()) { "Quotation revision must contain at least one item." }
        require(!discount.isNegative()) { "Discount cannot be negative (was ${discount.formatted()})." }
        require(createdAt.isNotBlank()) { "Created timestamp cannot be blank." }
    }

    /** Sum of line subtotals before revision-level discount. */
    val subtotal: Money
        get() = items.fold(Money.ZERO) { acc, item -> acc + item.lineSubtotal }

    /** Final commercial total after revision-level discount. */
    val totalAmount: Money
        get() {
            val sum = subtotal
            return if (discount >= sum) Money.ZERO else sum - discount
        }

    val totalQuantity: Int get() = items.sumOf { it.quantity }
}
