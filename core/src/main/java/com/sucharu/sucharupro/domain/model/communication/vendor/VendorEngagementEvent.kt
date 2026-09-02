package com.sucharu.sucharupro.domain.model.communication.vendor

/**
 * Vendor interaction and engagement telemetry events (Module 10 Step 05).
 *
 * Strictly project + vendor scoped. No unnecessary personal data is collected.
 */
data class VendorEngagementEvent(
    val eventId: String,
    val projectId: String,
    val communicationId: String,
    val vendorId: String,
    val eventType: VendorEngagementEventType,
    val actorId: String,
    val timestamp: Long = System.currentTimeMillis(),
    val metadata: Map<String, String> = emptyMap()
) {
    init {
        require(eventId.isNotBlank()) { "Event ID cannot be blank." }
        require(projectId.isNotBlank()) { "Project ID cannot be blank." }
        require(communicationId.isNotBlank()) { "Communication ID cannot be blank." }
        require(vendorId.isNotBlank()) { "Vendor ID cannot be blank." }
        require(actorId.isNotBlank()) { "Actor ID cannot be blank." }
        require(timestamp > 0) { "Timestamp must be positive." }
    }
}

/**
 * Classification of vendor engagement interactions (Module 10 Step 05).
 */
enum class VendorEngagementEventType(val defaultLabel: String) {
    VIEWED("Communication Viewed"),
    READ("Communication Read"),
    ACKNOWLEDGED("Communication Acknowledged"),
    DECLINED("Communication Declined"),
    DOCUMENT_OPENED("Document Opened"),
    NOTICE_VIEWED("Notice Viewed"),
    PAYMENT_NOTICE_VIEWED("Payment Notice Viewed"),
    DELIVERY_NOTICE_VIEWED("Delivery Notice Viewed"),
    QUALITY_NOTICE_VIEWED("Quality Notice Viewed")
}

/**
 * Aggregated vendor engagement metrics, strictly scoped to projectId + vendorId (Module 10 Step 05).
 */
data class VendorEngagementSummary(
    val projectId: String,
    val vendorId: String? = null,
    val totalCommunications: Int = 0,
    val sentCount: Int = 0,
    val deliveredCount: Int = 0,
    val readCount: Int = 0,
    val acknowledgedCount: Int = 0,
    val failedCount: Int = 0,
    val readRate: Double = 0.0,
    val acknowledgementRate: Double = 0.0,
    val recentActivityCount: Int = 0,
    val lastEngagementAt: Long? = null
) {
    init {
        require(projectId.isNotBlank()) { "Project ID cannot be blank." }
    }
}
