package com.sucharu.sucharupro.domain.model.communication.customer

/**
 * Append-only customer communication state transition and audit history (Module 10 Step 02).
 */
data class CustomerCommunicationHistory(
    val historyId: String,
    val projectId: String,
    val customerId: String,
    val communicationId: String,
    val eventType: String,
    val previousStatus: CustomerCommunicationStatus?,
    val newStatus: CustomerCommunicationStatus,
    val actorUserId: String,
    val timestamp: Long = System.currentTimeMillis(),
    val notes: String? = null,
    val metadata: Map<String, String> = emptyMap()
) {
    init {
        require(historyId.isNotBlank()) { "History ID cannot be blank." }
        require(projectId.isNotBlank()) { "Project ID cannot be blank." }
        require(customerId.isNotBlank()) { "Customer ID cannot be blank." }
        require(communicationId.isNotBlank()) { "Communication ID cannot be blank." }
        require(actorUserId.isNotBlank()) { "Actor User ID cannot be blank." }
        require(timestamp > 0) { "Timestamp must be positive." }
    }
}
