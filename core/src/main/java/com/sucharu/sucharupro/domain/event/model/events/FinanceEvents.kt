package com.sucharu.sucharupro.domain.event.model.events

import com.sucharu.sucharupro.domain.event.model.DomainEvent
import com.sucharu.sucharupro.domain.event.model.DomainEventType
import java.math.BigDecimal

/**
 * Emitted when a commercial invoice is generated.
 */
data class InvoiceCreatedEvent(
    val invoiceId: String,
    val orderId: String,
    val customerId: String,
    val invoiceNumber: String,
    val totalAmount: BigDecimal,
    val currency: String = "BDT",
    val dueTimestamp: Long,
    override val aggregateVersion: Long = 1L
) : DomainEvent {
    override val eventType: DomainEventType get() = DomainEventType.INVOICE_CREATED
    override val aggregateId: String get() = invoiceId
    override val aggregateType: String get() = "FINANCE"

    init {
        require(invoiceId.isNotBlank()) { "invoiceId cannot be blank" }
        require(orderId.isNotBlank()) { "orderId cannot be blank" }
        require(customerId.isNotBlank()) { "customerId cannot be blank" }
        require(invoiceNumber.isNotBlank()) { "invoiceNumber cannot be blank" }
        require(totalAmount >= BigDecimal.ZERO) { "totalAmount cannot be negative" }
    }
}

/**
 * Emitted when a payment is received and posted to the ledger.
 *
 * Payload safety rule: Never include raw card numbers, CVVs, bank passwords, or gateway private keys.
 */
data class PaymentReceivedEvent(
    val paymentId: String,
    val invoiceId: String?,
    val orderId: String?,
    val customerId: String,
    val amount: BigDecimal,
    val currency: String = "BDT",
    val paymentMethod: String,
    val transactionRef: String,
    override val aggregateVersion: Long = 1L
) : DomainEvent {
    override val eventType: DomainEventType get() = DomainEventType.PAYMENT_RECEIVED
    override val aggregateId: String get() = paymentId
    override val aggregateType: String get() = "FINANCE"

    init {
        require(paymentId.isNotBlank()) { "paymentId cannot be blank" }
        require(customerId.isNotBlank()) { "customerId cannot be blank" }
        require(amount > BigDecimal.ZERO) { "amount must be positive" }
        require(paymentMethod.isNotBlank()) { "paymentMethod cannot be blank" }
        require(transactionRef.isNotBlank()) { "transactionRef cannot be blank" }
    }
}

/**
 * Emitted when a payment is refunded.
 */
data class PaymentRefundedEvent(
    val refundId: String,
    val originalPaymentId: String,
    val customerId: String,
    val refundedAmount: BigDecimal,
    val currency: String = "BDT",
    val reason: String,
    override val aggregateVersion: Long = 1L
) : DomainEvent {
    override val eventType: DomainEventType get() = DomainEventType.PAYMENT_REFUNDED
    override val aggregateId: String get() = refundId
    override val aggregateType: String get() = "FINANCE"

    init {
        require(refundId.isNotBlank()) { "refundId cannot be blank" }
        require(originalPaymentId.isNotBlank()) { "originalPaymentId cannot be blank" }
        require(customerId.isNotBlank()) { "customerId cannot be blank" }
        require(refundedAmount > BigDecimal.ZERO) { "refundedAmount must be positive" }
        require(reason.isNotBlank()) { "reason cannot be blank" }
    }
}
