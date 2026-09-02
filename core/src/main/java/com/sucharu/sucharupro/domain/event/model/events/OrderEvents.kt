package com.sucharu.sucharupro.domain.event.model.events

import com.sucharu.sucharupro.domain.event.model.DomainEvent
import com.sucharu.sucharupro.domain.event.model.DomainEventType
import java.math.BigDecimal

/**
 * Emitted when a new commercial order is created.
 */
data class OrderCreatedEvent(
    val orderId: String,
    val customerId: String,
    val totalAmount: BigDecimal,
    val itemCount: Int,
    val currency: String = "BDT",
    override val aggregateVersion: Long = 1L
) : DomainEvent {
    override val eventType: DomainEventType get() = DomainEventType.ORDER_CREATED
    override val aggregateId: String get() = orderId
    override val aggregateType: String get() = "ORDER"

    init {
        require(orderId.isNotBlank()) { "orderId cannot be blank" }
        require(customerId.isNotBlank()) { "customerId cannot be blank" }
        require(totalAmount >= BigDecimal.ZERO) { "totalAmount cannot be negative" }
        require(itemCount > 0) { "itemCount must be positive" }
    }
}

/**
 * Emitted when an existing commercial order is updated.
 */
data class OrderUpdatedEvent(
    val orderId: String,
    val customerId: String,
    val updatedTotalAmount: BigDecimal,
    val updateReason: String,
    override val aggregateVersion: Long = 1L,
    val currency: String = "BDT"
) : DomainEvent {
    override val eventType: DomainEventType get() = DomainEventType.ORDER_UPDATED
    override val aggregateId: String get() = orderId
    override val aggregateType: String get() = "ORDER"

    init {
        require(orderId.isNotBlank()) { "orderId cannot be blank" }
        require(customerId.isNotBlank()) { "customerId cannot be blank" }
        require(updateReason.isNotBlank()) { "updateReason cannot be blank" }
    }
}

/**
 * Emitted when an order is cancelled.
 */
data class OrderCancelledEvent(
    val orderId: String,
    val customerId: String,
    val cancellationReason: String,
    override val aggregateVersion: Long
) : DomainEvent {
    override val eventType: DomainEventType get() = DomainEventType.ORDER_CANCELLED
    override val aggregateId: String get() = orderId
    override val aggregateType: String get() = "ORDER"

    init {
        require(orderId.isNotBlank()) { "orderId cannot be blank" }
        require(customerId.isNotBlank()) { "customerId cannot be blank" }
        require(cancellationReason.isNotBlank()) { "cancellationReason cannot be blank" }
    }
}
