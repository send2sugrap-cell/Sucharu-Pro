package com.sucharu.sucharupro.domain.model.delivery

/**
 * Discrete types of audit events for Delivery Orders.
 */
enum class DeliveryActivityType(val defaultLabel: String) {
    CREATED("Created"),
    STATUS_CHANGED("Status Changed"),
    PRIORITY_CHANGED("Priority Changed"),
    DISPATCH_REQUESTED("Dispatch Requested"),
    DISPATCHED("Dispatched"),
    DELIVERED("Delivered"),
    CANCELLED("Cancelled"),
    NOTES_UPDATED("Notes Updated")
}
