package com.sucharu.sucharupro.domain.model.order

import com.sucharu.sucharupro.domain.model.common.Money

/**
 * Commercial Quotation entity representing an official commercial proposal to a Customer.
 *
 * Maintains a linked revision history ([QuotationRevision]) to track price & specification negotiations.
 */
data class Quotation(
    val quotationId: String,
    val quotationNumber: String,
    val customerId: String,
    val inquiryId: String? = null,
    val currentRevisionNumber: Int = 1,
    val revisions: List<QuotationRevision> = emptyList(),
    val status: QuotationStatusType = QuotationStatusType.DRAFT,
    val validUntil: String? = null,
    val termsAndConditions: String? = null,
    val createdAt: String,
    val updatedAt: String,
    val approvedAt: String? = null,
    val approvedBy: String? = null,
    val approvedRevisionId: String? = null
) {
    init {
        require(quotationId.isNotBlank()) { "Quotation ID cannot be blank." }
        require(quotationNumber.isNotBlank()) { "Quotation Number cannot be blank." }
        require(customerId.isNotBlank()) { "Customer ID cannot be blank." }
        require(currentRevisionNumber >= 1) { "Current revision number must be >= 1." }
        require(createdAt.isNotBlank()) { "Created timestamp cannot be blank." }
        require(updatedAt.isNotBlank()) { "Updated timestamp cannot be blank." }
    }

    /** Active current revision snapshot. */
    val currentRevision: QuotationRevision?
        get() = revisions.find { it.revisionNumber == currentRevisionNumber } ?: revisions.lastOrNull()

    /** Agreed items in the current active revision. */
    val items: List<QuotationItem> get() = currentRevision?.items ?: emptyList()

    /** Subtotal of current active revision. */
    val subtotal: Money get() = currentRevision?.subtotal ?: Money.ZERO

    /** Discount of current active revision. */
    val discount: Money get() = currentRevision?.discount ?: Money.ZERO

    /** Total commercial value of current active revision. */
    val totalAmount: Money get() = currentRevision?.totalAmount ?: Money.ZERO

    /** Current delivery requirements. */
    val deliveryRequirement: DeliveryRequirement? get() = currentRevision?.deliveryRequirement

    /** Current payment terms. */
    val paymentTerms: PaymentTerms get() = currentRevision?.paymentTerms ?: PaymentTerms.DEFAULT

    /** Total revisions count. */
    val revisionCount: Int get() = revisions.size

    /**
     * Checks if this quotation is currently approved and eligible for conversion to an Order.
     */
    val isApproved: Boolean
        get() = status == QuotationStatusType.APPROVED && !approvedRevisionId.isNullOrBlank()
}
