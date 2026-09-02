package com.sucharu.sucharupro.domain.model.delivery.pod

import com.sucharu.sucharupro.domain.model.user.UserRole

/**
 * Event types for Proof of Delivery audit logging (Module 08 Step 08).
 */
enum class DeliveryProofActivityType(val defaultLabel: String) {
    CREATED("POD Record Created"),
    UPDATED("POD Record Updated"),
    EVIDENCE_ADDED("Evidence Attached"),
    EVIDENCE_REMOVED("Evidence Detached"),
    RECIPIENT_CONFIRMED("Recipient Acknowledgment Recorded"),
    SIGNATURE_CAPTURED("Recipient Signature Captured"),
    OTP_CONFIRMED("OTP Verification Confirmed"),
    PHOTO_ATTACHED("Delivery Photo Attached"),
    DOCUMENT_ATTACHED("Signed Document Attached"),
    SUBMITTED("POD Submitted for Review"),
    REVIEW_STARTED("POD Review Started"),
    VERIFIED("POD Verified"),
    ACCEPTED("POD Accepted"),
    REJECTED("POD Rejected"),
    CANCELLED("POD Cancelled"),
    NOTE_ADDED("Note Added")
}

/**
 * Append-only immutable audit record for POD lifecycle operations (Module 08 Step 08).
 */
data class DeliveryProofActivityEvent(
    val eventId: String,
    val projectId: String,
    val proofId: String,
    val activityType: DeliveryProofActivityType,
    val actorId: String,
    val actorRole: UserRole? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val previousStatus: DeliveryProofStatus? = null,
    val newStatus: DeliveryProofStatus? = null,
    val metadata: Map<String, String> = emptyMap(),
    val notes: String? = null
) {
    init {
        require(eventId.isNotBlank()) { "Event ID cannot be blank." }
        require(projectId.isNotBlank()) { "Project ID cannot be blank." }
        require(proofId.isNotBlank()) { "Proof ID cannot be blank." }
        require(actorId.isNotBlank()) { "Actor ID cannot be blank." }
        require(timestamp > 0) { "Timestamp must be positive." }
    }
}
