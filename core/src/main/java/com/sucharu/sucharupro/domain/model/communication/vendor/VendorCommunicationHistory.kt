package com.sucharu.sucharupro.domain.model.communication.vendor

/**
 * Append-only state transition and audit history record for a VendorCommunication (Module 10 Step 05).
 *
 * History records are immutable after creation — no update or delete is permitted.
 */
data class VendorCommunicationHistory(
    val historyId: String,
    val projectId: String,
    val communicationId: String,
    val vendorId: String,
    val previousStatus: VendorCommunicationStatus?,
    val newStatus: VendorCommunicationStatus,
    val action: String,
    val performedBy: String,
    val performedAt: Long = System.currentTimeMillis(),
    val metadata: Map<String, String> = emptyMap()
) {
    init {
        require(historyId.isNotBlank()) { "History ID cannot be blank." }
        require(projectId.isNotBlank()) { "Project ID cannot be blank." }
        require(communicationId.isNotBlank()) { "Communication ID cannot be blank." }
        require(vendorId.isNotBlank()) { "Vendor ID cannot be blank." }
        require(action.isNotBlank()) { "Action cannot be blank." }
        require(performedBy.isNotBlank()) { "Performed By cannot be blank." }
        require(performedAt > 0) { "Performed At timestamp must be positive." }
    }
}

/**
 * Read receipt recording that a vendor actor viewed or read a communication (Module 10 Step 05).
 *
 * Vendor isolation: a read receipt is strictly scoped to (projectId, communicationId, vendorId).
 * No other vendor's read state is exposed.
 */
data class VendorCommunicationReadReceipt(
    val receiptId: String,
    val projectId: String,
    val communicationId: String,
    val vendorId: String,
    val readByActorId: String,
    val viewedAt: Long? = null,
    val readAt: Long = System.currentTimeMillis(),
    val createdAt: Long = System.currentTimeMillis()
) {
    init {
        require(receiptId.isNotBlank()) { "Receipt ID cannot be blank." }
        require(projectId.isNotBlank()) { "Project ID cannot be blank." }
        require(communicationId.isNotBlank()) { "Communication ID cannot be blank." }
        require(vendorId.isNotBlank()) { "Vendor ID cannot be blank." }
        require(readByActorId.isNotBlank()) { "Read By Actor ID cannot be blank." }
        require(readAt > 0) { "Read At timestamp must be positive." }
    }
}

/**
 * Immutable acknowledgement record for a Vendor Communication (Module 10 Step 05).
 *
 * Once created, acknowledgement records cannot be updated or deleted.
 */
data class VendorCommunicationAcknowledgement(
    val acknowledgementId: String,
    val projectId: String,
    val communicationId: String,
    val vendorId: String,
    val acknowledgedBy: String,
    val status: VendorAcknowledgementStatus,
    val message: String? = null,
    val createdAt: Long = System.currentTimeMillis()
) {
    init {
        require(acknowledgementId.isNotBlank()) { "Acknowledgement ID cannot be blank." }
        require(projectId.isNotBlank()) { "Project ID cannot be blank." }
        require(communicationId.isNotBlank()) { "Communication ID cannot be blank." }
        require(vendorId.isNotBlank()) { "Vendor ID cannot be blank." }
        require(acknowledgedBy.isNotBlank()) { "Acknowledged By cannot be blank." }
        require(createdAt > 0) { "Created At timestamp must be positive." }
    }
}

/**
 * Acknowledgement outcome status (Module 10 Step 05).
 */
enum class VendorAcknowledgementStatus(val defaultLabel: String) {
    ACKNOWLEDGED("Acknowledged"),
    DECLINED("Declined")
}
