package com.sucharu.sucharupro.domain.model.job

import com.sucharu.sucharupro.domain.model.handoff.OrderJobHandoffItem

/**
 * Production line item specification snapshot within a Production Job Card.
 *
 * Captures manufacturing-relevant attributes (description, specification, quantity, unit)
 * while intentionally leaving out commercial financial terms (costing/pricing belong to other modules).
 */
data class ProductionJobItem(
    val itemId: String,
    val description: String,
    val specification: String? = null,
    val quantity: Int,
    val unit: String = "Pcs"
) {
    companion object {
        /**
         * Creates an immutable [ProductionJobItem] from a sealed [OrderJobHandoffItem].
         */
        fun fromHandoffItem(handoffItem: OrderJobHandoffItem): ProductionJobItem {
            return ProductionJobItem(
                itemId = handoffItem.itemId,
                description = handoffItem.description,
                specification = handoffItem.specification,
                quantity = handoffItem.quantity,
                unit = handoffItem.unit
            )
        }
    }
}
