package com.sucharu.sucharupro.domain.model.delivery

/**
 * Represents the lifecycle stages of a Delivery Order.
 */
enum class DeliveryOrderStatus {
    DRAFT,
    PENDING,
    APPROVED,
    READY_FOR_DISPATCH,
    DISPATCHED,
    DELIVERED,
    CANCELLED
}
