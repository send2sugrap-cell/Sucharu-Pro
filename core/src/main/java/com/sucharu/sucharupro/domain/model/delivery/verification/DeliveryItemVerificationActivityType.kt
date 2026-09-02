package com.sucharu.sucharupro.domain.model.delivery.verification

/**
 * Activity types for Delivery Item Verification audit logs (Module 08 Step 04).
 */
enum class DeliveryItemVerificationActivityType(val defaultLabel: String) {
    CREATED("Verification Created"),
    UPDATED("Verification Updated"),
    SUBMITTED("Verification Submitted"),
    STARTED("Verification In Progress"),
    LINE_VERIFIED("Line Item Verified"),
    ISSUE_REPORTED("Discrepancy Issue Reported"),
    VERIFIED("Verification Completed"),
    CLOSED("Verification Closed"),
    CANCELLED("Verification Cancelled")
}
