package com.sucharu.sucharupro.domain.model.vendor

/**
 * State machine representing the complete operational lifecycle of a Vendor Work Order (Module 12 Step 04).
 */
enum class VendorWorkOrderStatus {
    DRAFT,
    ASSIGNED,
    READY,
    RELEASED,
    IN_PROGRESS,
    ON_HOLD,
    COMPLETED,
    CANCELLED;

    val isActive: Boolean get() = this in setOf(ASSIGNED, READY, RELEASED, IN_PROGRESS, ON_HOLD)
    val isEditable: Boolean get() = this in setOf(DRAFT, ASSIGNED, READY)
    val isTerminal: Boolean get() = this in setOf(COMPLETED, CANCELLED)

    fun canTransitionTo(target: VendorWorkOrderStatus): Boolean {
        if (this == target) return true
        return when (this) {
            DRAFT -> target in setOf(ASSIGNED, READY, CANCELLED)
            ASSIGNED -> target in setOf(READY, RELEASED, CANCELLED)
            READY -> target in setOf(RELEASED, CANCELLED)
            RELEASED -> target in setOf(IN_PROGRESS, CANCELLED)
            IN_PROGRESS -> target in setOf(ON_HOLD, COMPLETED, CANCELLED)
            ON_HOLD -> target in setOf(IN_PROGRESS, CANCELLED)
            COMPLETED -> false // Terminal
            CANCELLED -> false // Terminal
        }
    }
}
