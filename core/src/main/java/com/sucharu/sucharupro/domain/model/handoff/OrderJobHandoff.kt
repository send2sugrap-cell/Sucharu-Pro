package com.sucharu.sucharupro.domain.model.handoff

import com.sucharu.sucharupro.domain.model.common.Money
import com.sucharu.sucharupro.domain.model.order.DeliveryRequirement
import com.sucharu.sucharupro.domain.model.order.Order
import com.sucharu.sucharupro.domain.model.order.OrderPriority

/**
 * Authoritative Commercial Order → Job Handoff record in Sucharu Pro ERP.
 *
 * Holds an immutable commercial snapshot of agreed line items, quantities, specifications,
 * priorities, and delivery requirements at the exact moment the commercial order is sealed
 * for handoff to future production management (Module 04).
 */
data class OrderJobHandoff(
    val handoffId: String,
    val orderId: String,
    val orderNumber: String,
    val customerId: String,
    val quotationId: String? = null,
    val approvedRevisionId: String? = null,
    val handoffStatus: OrderJobHandoffStatus = OrderJobHandoffStatus.READY_FOR_HANDOFF,
    val priority: OrderPriority = OrderPriority.NORMAL,
    val deliveryRequirement: DeliveryRequirement? = null,
    val items: List<OrderJobHandoffItem> = emptyList(),
    val commercialTotal: Money,
    val notes: String? = null,
    val createdAt: String,
    val createdBy: String? = null,
    val confirmedAt: String? = null,
    val confirmedBy: String? = null,
    val jobReferenceId: String? = null,
    val jobCreatedAt: String? = null
) {
    init {
        require(handoffId.isNotBlank()) { "Handoff ID cannot be blank." }
        require(orderId.isNotBlank()) { "Order ID cannot be blank." }
        require(orderNumber.isNotBlank()) { "Order Number cannot be blank." }
        require(customerId.isNotBlank()) { "Customer ID cannot be blank." }
        require(!commercialTotal.isNegative()) { "Commercial total cannot be negative." }
        require(createdAt.isNotBlank()) { "Created timestamp cannot be blank." }
    }

    /** Count of distinct line items in the handoff snapshot. */
    val itemCount: Int get() = items.size

    /** Total ordered units across all line items in the snapshot. */
    val totalQuantity: Int get() = items.sumOf { it.quantity }

    companion object {
        /**
         * Factory function to create an isolated [OrderJobHandoff] record from a validated [Order].
         */
        fun fromOrder(
            handoffId: String,
            order: Order,
            createdBy: String? = null,
            notes: String? = null,
            timestamp: String
        ): OrderJobHandoff {
            return OrderJobHandoff(
                handoffId = handoffId,
                orderId = order.orderId,
                orderNumber = order.orderNumber,
                customerId = order.customerId,
                quotationId = order.quotationId,
                approvedRevisionId = order.approvedQuotationRevisionId,
                handoffStatus = OrderJobHandoffStatus.READY_FOR_HANDOFF,
                priority = order.priority,
                deliveryRequirement = order.deliveryRequirement,
                items = order.items.map { OrderJobHandoffItem.fromOrderItem(it) },
                commercialTotal = order.totalAmount,
                notes = notes ?: order.notes,
                createdAt = timestamp,
                createdBy = createdBy
            )
        }
    }
}
