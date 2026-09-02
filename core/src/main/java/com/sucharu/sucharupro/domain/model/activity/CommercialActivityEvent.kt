package com.sucharu.sucharupro.domain.model.activity

/**
 * Immutable audit event representing a single discrete action that occurred on a
 * commercial entity (Inquiry, Quotation, or Order) in Sucharu Pro.
 *
 * Design constraints:
 * - All instances are IMMUTABLE — once recorded, no field can be changed.
 * - Timestamps are ISO-8601 Strings (consistent with the rest of the domain).
 * - No references to mutable Inquiry, Quotation, or Order objects.
 * - [previousStatus] / [newStatus] store the String label of the status enum,
 *   NOT an enum reference, to keep the audit record stable even if enum labels evolve.
 * - [previousValue] / [newValue] are free-form String values for revision numbers,
 *   priority levels, handoff status, or order IDs.
 * - [actorId] / [actorName] are nullable; display "System" when both are null.
 *
 * @param activityId      Unique identifier for this audit event.
 * @param entityType      The type of commercial entity this event belongs to.
 * @param entityId        The primary key of the commercial entity (inquiryId, quotationId, orderId).
 * @param activityType    The kind of action that was performed.
 * @param actorId         Optional ID of the user or system actor who performed the action.
 * @param actorName       Optional display name of the actor.
 * @param timestamp       ISO-8601 timestamp when the event was recorded.
 * @param previousStatus  Optional: previous lifecycle status label before this change.
 * @param newStatus       Optional: new lifecycle status label after this change.
 * @param previousValue   Optional: previous value for numeric/label changes (e.g., revision number, priority).
 * @param newValue        Optional: new value after the change (e.g., new revision number, created order ID).
 * @param reason          Optional: stated reason for the action (required for CANCELLED events).
 * @param note            Optional: free-form operational note attached to this event.
 */
data class CommercialActivityEvent(
    val activityId: String,
    val entityType: CommercialEntityType,
    val entityId: String,
    val activityType: CommercialActivityType,
    val actorId: String? = null,
    val actorName: String? = null,
    val timestamp: String,
    val previousStatus: String? = null,
    val newStatus: String? = null,
    val previousValue: String? = null,
    val newValue: String? = null,
    val reason: String? = null,
    val note: String? = null
) {
    init {
        require(activityId.isNotBlank()) { "Activity ID cannot be blank." }
        require(entityId.isNotBlank()) { "Entity ID cannot be blank." }
        require(timestamp.isNotBlank()) { "Timestamp cannot be blank." }
    }

    /**
     * Returns the actor display name for UI rendering.
     * Falls back to "System" when neither [actorId] nor [actorName] is available.
     */
    val resolvedActorName: String
        get() = actorName?.takeIf { it.isNotBlank() } ?: "System"
}
