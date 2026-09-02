package com.sucharu.sucharupro.domain.model.returns

import com.sucharu.sucharupro.domain.model.user.UserRole

/**
 * Event types for Return Request audit logging (Module 11 Step 02 & Step 03).
 */
enum class ReturnActivityType(val defaultLabel: String) {
    RETURN_REQUEST_CREATED("Return Request Created"),
    RETURN_REQUEST_UPDATED("Return Request Updated"),
    RETURN_REQUEST_CANCELLED("Return Request Cancelled"),
    RETURN_REQUEST_SUBMITTED_FOR_INSPECTION("Return Submitted for Inspection"),
    RETURN_INSPECTION_RECORDED("Return Inspection Recorded"),
    RETURN_INSPECTION_COMPLETED("Return Inspection Completed"),
    RETURN_REQUEST_APPROVED("Return Request Approved"),
    RETURN_REQUEST_REJECTED("Return Request Rejected"),
    RETURN_RECEIVED("Return Received"),
    RETURN_PROCESSED("Return Processed"),
    RETURN_SETTLED("Return Settled"),
    RETURN_EXCEPTION_DETECTED("Return Exception Detected"),
    RETURN_EXCEPTION_ACKNOWLEDGED("Return Exception Acknowledged"),
    RETURN_EXCEPTION_RESOLVED("Return Exception Resolved"),
    RETURN_EXCEPTION_DISMISSED("Return Exception Dismissed"),
    NOTE_ADDED("Note Added");

    val displayName: String
        get() = defaultLabel
}

/**
 * Append-only immutable audit record for return request lifecycle and actions (Module 11 Step 02 & Step 03).
 */
data class ReturnActivityEvent(
    val eventId: String,
    val projectId: String,
    val returnId: String,
    val activityType: ReturnActivityType,
    val actorId: String,
    val actorRole: UserRole? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val previousStatus: ReturnStatus? = null,
    val newStatus: ReturnStatus? = null,
    val metadata: Map<String, String> = emptyMap(),
    val notes: String? = null
) {
    init {
        require(eventId.isNotBlank()) { "Event ID cannot be blank." }
        require(projectId.isNotBlank()) { "Project ID cannot be blank." }
        require(returnId.isNotBlank()) { "Return ID cannot be blank." }
        require(actorId.isNotBlank()) { "Actor ID cannot be blank." }
        require(timestamp > 0) { "Timestamp must be positive." }
    }
}
