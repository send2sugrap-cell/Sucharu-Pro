package com.sucharu.sucharupro.domain.model.notification

/**
 * Lifecycle states of a notification (Module 10 Step 01).
 */
enum class NotificationStatus(val defaultLabel: String, val isTerminal: Boolean = false) {
    DRAFT("Draft", false),
    QUEUED("Queued for Delivery", false),
    PROCESSING("Processing Delivery", false),
    SENT("Sent to Provider", false),
    DELIVERED("Delivered", false),
    READ("Read & Acknowledged", true),
    FAILED("Delivery Failed", false),
    CANCELLED("Cancelled", true)
}
