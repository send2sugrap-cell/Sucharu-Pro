package com.sucharu.sucharupro.domain.model.communication.customer

/**
 * Lifecycle states of customer communication (Module 10 Step 02).
 */
enum class CustomerCommunicationStatus(val defaultLabel: String, val isTerminal: Boolean = false) {
    DRAFT("Draft", false),
    SCHEDULED("Scheduled", false),
    QUEUED("Queued", false),
    SENT("Sent", false),
    DELIVERED("Delivered", false),
    READ("Read", false),
    ACKNOWLEDGED("Acknowledged", true),
    FAILED("Delivery Failed", false),
    CANCELLED("Cancelled", true)
}
