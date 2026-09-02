package com.sucharu.sucharupro.domain.model.delivery.challan

/**
 * Types of audit events for Delivery Challans (Module 08 Step 02).
 */
enum class DeliveryChallanActivityType(val defaultLabel: String) {
    CREATED("Created"),
    UPDATED("Updated"),
    SUBMITTED("Submitted"),
    APPROVED("Approved"),
    READY_FOR_DISPATCH("Ready for Dispatch"),
    CANCELLED("Cancelled"),
    LINE_ADDED("Line Added"),
    LINE_UPDATED("Line Updated"),
    LINE_REMOVED("Line Removed")
}
