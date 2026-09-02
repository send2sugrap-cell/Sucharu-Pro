package com.sucharu.sucharupro.domain.model.handoff

import com.sucharu.sucharupro.domain.model.common.Money
import com.sucharu.sucharupro.domain.model.order.OrderItem

/**
 * Immutable commercial line-item snapshot captured in an [OrderJobHandoff] record.
 *
 * Guarantees snapshot isolation so future alterations to quotations or orders
 * never contaminate the handoff specification consumed by production systems.
 */
data class OrderJobHandoffItem(
    val itemId: String,
    val description: String,
    val specification: String? = null,
    val quantity: Int,
    val unit: String = "Pcs",
    val unitPrice: Money,
    val discount: Money = Money.ZERO,
    val lineSubtotal: Money
) {
    init {
        require(itemId.isNotBlank()) { "Item ID cannot be blank." }
        require(description.isNotBlank()) { "Description cannot be blank." }
        require(quantity > 0) { "Quantity must be positive (was $quantity)." }
        require(unit.isNotBlank()) { "Unit cannot be blank." }
        require(!unitPrice.isNegative()) { "Unit price cannot be negative." }
        require(!discount.isNegative()) { "Discount cannot be negative." }
        require(!lineSubtotal.isNegative()) { "Line subtotal cannot be negative." }
    }

    companion object {
        /**
         * Creates an isolated [OrderJobHandoffItem] snapshot from an [OrderItem].
         */
        fun fromOrderItem(item: OrderItem): OrderJobHandoffItem {
            return OrderJobHandoffItem(
                itemId = item.itemId,
                description = item.description,
                specification = item.specification,
                quantity = item.quantity,
                unit = item.unit,
                unitPrice = item.unitPrice,
                discount = item.discount,
                lineSubtotal = item.lineSubtotal
            )
        }
    }
}
