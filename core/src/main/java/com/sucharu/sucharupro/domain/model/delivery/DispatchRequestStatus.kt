package com.sucharu.sucharupro.domain.model.delivery

/**
 * Represents the status of a specific dispatch request associated with a Delivery Order.
 */
enum class DispatchRequestStatus {
    REQUESTED,
    APPROVED,
    READY,
    DISPATCHED,
    CANCELLED
}
