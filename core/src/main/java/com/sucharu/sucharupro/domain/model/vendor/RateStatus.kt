package com.sucharu.sucharupro.domain.model.vendor

/**
 * Deterministic lifecycle states for a VendorServiceRate (Module 12 Step 03).
 */
enum class RateStatus {
    DRAFT,
    ACTIVE,
    SUSPENDED,
    EXPIRED,
    ARCHIVED;

    val isActive: Boolean get() = this == ACTIVE
    val isSelectable: Boolean get() = this == ACTIVE

    /**
     * Validates whether a state transition from [this] to [target] is permitted.
     */
    fun canTransitionTo(target: RateStatus): Boolean {
        if (this == target) return true
        return when (this) {
            DRAFT -> target in setOf(ACTIVE, ARCHIVED)
            ACTIVE -> target in setOf(SUSPENDED, EXPIRED, ARCHIVED)
            SUSPENDED -> target in setOf(ACTIVE, ARCHIVED)
            EXPIRED -> target in setOf(ARCHIVED)
            ARCHIVED -> false // Terminal state
        }
    }
}
