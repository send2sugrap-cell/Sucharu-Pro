package com.sucharu.sucharupro.domain.model.communication.vendor

/**
 * Lifecycle states of a Vendor Communication (Module 10 Step 05).
 *
 * Terminal states: [ACKNOWLEDGED], [DECLINED], [CANCELLED].
 * [FAILED] is NOT terminal — retry is permitted.
 */
enum class VendorCommunicationStatus(
    val defaultLabel: String,
    val isTerminal: Boolean = false
) {
    DRAFT("Draft", false),
    SCHEDULED("Scheduled", false),
    QUEUED("Queued", false),
    SENT("Sent", false),
    DELIVERED("Delivered", false),
    READ("Read", false),
    ACKNOWLEDGED("Acknowledged", true),
    DECLINED("Declined", true),
    FAILED("Delivery Failed", false),
    CANCELLED("Cancelled", true)
}
