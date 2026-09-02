package com.sucharu.sucharupro.domain.model.order

import com.sucharu.sucharupro.domain.model.common.Money

/**
 * Commercial Customer Order entity in Sucharu Pro.
 *
 * Represents a customer's confirmed commitment to purchase printing/custom products.
 * Holds its own immutable snapshot of agreed items and pricing from the approved quotation revision.
 */
data class Order(
    val orderId: String,
    val orderNumber: String,
    val customerId: String,
    val quotationId: String? = null,
    val approvedQuotationRevisionId: String? = null,
    val status: OrderStatusType = OrderStatusType.CONFIRMED,
    val priority: OrderPriority = OrderPriority.NORMAL,
    val items: List<OrderItem> = emptyList(),
    val discount: Money = Money.ZERO,
    val deliveryRequirement: DeliveryRequirement? = null,
    val paymentTerms: PaymentTerms = PaymentTerms.DEFAULT,
    val jobHandoffStatus: JobHandoffStatus = JobHandoffStatus.NOT_READY,
    val notes: String? = null,
    val confirmedAt: String? = null,
    val confirmedBy: String? = null,
    val createdAt: String,
    val updatedAt: String
) {
    init {
        require(orderId.isNotBlank()) { "Order ID cannot be blank." }
        require(orderNumber.isNotBlank()) { "Order Number cannot be blank." }
        require(customerId.isNotBlank()) { "Customer ID cannot be blank." }
        require(!discount.isNegative()) { "Discount cannot be negative (was ${discount.formatted()})." }
        require(createdAt.isNotBlank()) { "Created timestamp cannot be blank." }
        require(updatedAt.isNotBlank()) { "Updated timestamp cannot be blank." }
    }

    /** Sum of order items line subtotals before order-level discount. */
    val subtotal: Money
        get() = items.fold(Money.ZERO) { acc, item -> acc + item.lineSubtotal }

    /** Net commercial total for the order. */
    val totalAmount: Money
        get() {
            val sum = subtotal
            return if (discount >= sum) Money.ZERO else sum - discount
        }

    /** Total ordered units. */
    val totalQuantity: Int get() = items.sumOf { it.quantity }

    companion object {
        /**
         * Factory function to create an [Order] directly from an approved [Quotation] and its approved [QuotationRevision].
         * Enforces the snapshot principle so subsequent quotation changes never alter the confirmed order.
         */
        fun fromApprovedQuotation(
            orderId: String,
            orderNumber: String,
            quotation: Quotation,
            revision: QuotationRevision,
            priority: OrderPriority = OrderPriority.NORMAL,
            confirmedBy: String? = null,
            timestamp: String
        ): Order {
            require(quotation.isApproved) {
                "Cannot create an Order from an unapproved quotation (status: ${quotation.status})."
            }
            require(revision.revisionId == quotation.approvedRevisionId) {
                "Provided revision '${revision.revisionId}' does not match quotation's approved revision ID '${quotation.approvedRevisionId}'."
            }

            val orderItems = revision.items.map { OrderItem.fromQuotationItem(it) }

            return Order(
                orderId = orderId,
                orderNumber = orderNumber,
                customerId = quotation.customerId,
                quotationId = quotation.quotationId,
                approvedQuotationRevisionId = revision.revisionId,
                status = OrderStatusType.CONFIRMED,
                priority = priority,
                items = orderItems,
                discount = revision.discount,
                deliveryRequirement = revision.deliveryRequirement,
                paymentTerms = revision.paymentTerms,
                jobHandoffStatus = JobHandoffStatus.READY_FOR_JOB,
                notes = revision.notes,
                confirmedAt = timestamp,
                confirmedBy = confirmedBy,
                createdAt = timestamp,
                updatedAt = timestamp
            )
        }
    }
}
