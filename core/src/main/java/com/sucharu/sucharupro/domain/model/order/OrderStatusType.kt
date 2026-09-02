package com.sucharu.sucharupro.domain.model.order

/**
 * Commercial lifecycle states for a customer order in Sucharu Pro.
 *
 * This enum tracks the ORDER's commercial/business state, NOT the internal
 * production workflow. For production stage tracking, use [ProductionStageType].
 *
 * Order lifecycle:
 *   PENDING → CONFIRMED → IN_PRODUCTION → READY → DELIVERED
 *                                        ↓
 *                                   CANCELLED / ON_HOLD (can happen at most stages)
 *
 * An order in IN_PRODUCTION state will have a linked production job tracked
 * separately via ProductionStageType (13-stage canonical workflow).
 */
enum class OrderStatusType(val defaultLabel: String) {
    /** Order received but not yet confirmed (pending quotation approval or deposit). */
    PENDING("Pending"),

    /** Order confirmed by customer (deposit received or credit approved). */
    CONFIRMED("Confirmed"),

    /** Order is actively being processed in production. */
    IN_PRODUCTION("In Production"),

    /** Production complete. Job is ready for pickup or delivery. */
    READY("Ready"),

    /** Delivered to customer or dispatched via challan. Terminal state. */
    DELIVERED("Delivered"),

    /** Order placed on hold (awaiting customer input, material, or payment). */
    ON_HOLD("On Hold"),

    /** Order cancelled before or during production. Terminal state. */
    CANCELLED("Cancelled");

    /**
     * Checks if transitioning from this commercial order status to [target] is valid.
     */
    fun canTransitionTo(target: OrderStatusType): Boolean {
        if (this == target) return false // Self-transitions are rejected
        return when (this) {
            PENDING -> target in setOf(CONFIRMED, ON_HOLD, CANCELLED)
            CONFIRMED -> target in setOf(IN_PRODUCTION, ON_HOLD, CANCELLED)
            IN_PRODUCTION -> target in setOf(READY, ON_HOLD, CANCELLED)
            READY -> target in setOf(DELIVERED, ON_HOLD)
            DELIVERED -> false // Terminal delivery state: no further transitions allowed
            ON_HOLD -> target in setOf(CONFIRMED, CANCELLED)
            CANCELLED -> false // Terminal cancelled state: no further transitions allowed
        }
    }
}
