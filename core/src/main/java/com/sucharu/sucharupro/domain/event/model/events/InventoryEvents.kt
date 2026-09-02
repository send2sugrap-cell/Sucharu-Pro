package com.sucharu.sucharupro.domain.event.model.events

import com.sucharu.sucharupro.domain.event.model.DomainEvent
import com.sucharu.sucharupro.domain.event.model.DomainEventType
import java.math.BigDecimal

/**
 * Emitted when stock is received into warehouse.
 */
data class StockReceivedEvent(
    val movementId: String,
    val productId: String,
    val sku: String,
    val quantity: BigDecimal,
    val warehouseId: String,
    val purchaseOrderId: String? = null,
    override val aggregateVersion: Long = 1L
) : DomainEvent {
    override val eventType: DomainEventType get() = DomainEventType.STOCK_RECEIVED
    override val aggregateId: String get() = movementId
    override val aggregateType: String get() = "INVENTORY"

    init {
        require(movementId.isNotBlank()) { "movementId cannot be blank" }
        require(productId.isNotBlank()) { "productId cannot be blank" }
        require(sku.isNotBlank()) { "sku cannot be blank" }
        require(warehouseId.isNotBlank()) { "warehouseId cannot be blank" }
        require(quantity > BigDecimal.ZERO) { "quantity must be positive" }
    }
}

/**
 * Emitted when stock is issued for production or delivery.
 */
data class StockIssuedEvent(
    val movementId: String,
    val productId: String,
    val sku: String,
    val quantity: BigDecimal,
    val warehouseId: String,
    val destinationType: String,
    val destinationRefId: String,
    override val aggregateVersion: Long = 1L
) : DomainEvent {
    override val eventType: DomainEventType get() = DomainEventType.STOCK_ISSUED
    override val aggregateId: String get() = movementId
    override val aggregateType: String get() = "INVENTORY"

    init {
        require(movementId.isNotBlank()) { "movementId cannot be blank" }
        require(productId.isNotBlank()) { "productId cannot be blank" }
        require(warehouseId.isNotBlank()) { "warehouseId cannot be blank" }
        require(quantity > BigDecimal.ZERO) { "quantity must be positive" }
    }
}

/**
 * Emitted when inventory stock is reconciled or adjusted.
 */
data class StockAdjustedEvent(
    val adjustmentId: String,
    val productId: String,
    val warehouseId: String,
    val varianceQuantity: BigDecimal,
    val reason: String,
    val authorizedBy: String,
    override val aggregateVersion: Long = 1L
) : DomainEvent {
    override val eventType: DomainEventType get() = DomainEventType.STOCK_ADJUSTED
    override val aggregateId: String get() = adjustmentId
    override val aggregateType: String get() = "INVENTORY"

    init {
        require(adjustmentId.isNotBlank()) { "adjustmentId cannot be blank" }
        require(productId.isNotBlank()) { "productId cannot be blank" }
        require(reason.isNotBlank()) { "reason cannot be blank" }
        require(authorizedBy.isNotBlank()) { "authorizedBy cannot be blank" }
    }
}
