package com.sucharu.sucharupro.domain.model.handoff

/**
 * Commercial handoff lifecycle states for an [OrderJobHandoff] record in Sucharu Pro.
 *
 * Establishes the authoritative boundary state between Commercial Order Management (Module 03)
 * and future Production Job Management (Module 04).
 */
enum class OrderJobHandoffStatus(val defaultLabel: String) {

    /** Handoff initiated and pending review or prerequisite checks. */
    PENDING("Pending"),

    /** Commercial order validation passed; ready for formal handoff confirmation. */
    READY_FOR_HANDOFF("Ready for Handoff"),

    /** Handoff formally confirmed by authorized personnel; commercial snapshot sealed. */
    HANDED_OFF("Handed Off"),

    /** Sealed handoff approved and ready for consumption by production job systems. */
    READY_FOR_PRODUCTION("Ready for Production"),

    /** Handoff cancelled before production intake. Terminal state. */
    CANCELLED("Cancelled");

    /**
     * Checks if transitioning from this handoff status to [target] is valid.
     */
    fun canTransitionTo(target: OrderJobHandoffStatus): Boolean {
        if (this == target) return false // Self-transitions rejected
        return when (this) {
            PENDING -> target in setOf(READY_FOR_HANDOFF, CANCELLED)
            READY_FOR_HANDOFF -> target in setOf(HANDED_OFF, CANCELLED)
            HANDED_OFF -> target in setOf(READY_FOR_PRODUCTION, CANCELLED)
            READY_FOR_PRODUCTION -> false // Terminal commercial handoff boundary
            CANCELLED -> false // Terminal cancelled state
        }
    }
}
