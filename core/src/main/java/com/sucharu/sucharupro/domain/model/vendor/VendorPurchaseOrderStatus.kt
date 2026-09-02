package com.sucharu.sucharupro.domain.model.vendor

/**
 * State machine representing the commercial lifecycle of a Vendor Purchase Order (Module 12 Step 05).
 */
enum class VendorPurchaseOrderStatus {
    DRAFT,
    PENDING_APPROVAL,
    APPROVED,
    ISSUED,
    ACKNOWLEDGED,
    PARTIALLY_FULFILLED,
    FULFILLED,
    CLOSED,
    CANCELLED;

    val isEditable: Boolean get() = this == DRAFT
    val isPendingApproval: Boolean get() = this == PENDING_APPROVAL
    val isApprovedOrIssued: Boolean get() = this in setOf(APPROVED, ISSUED, ACKNOWLEDGED, PARTIALLY_FULFILLED, FULFILLED)
    val isTerminal: Boolean get() = this in setOf(CLOSED, CANCELLED)
    val isActive: Boolean get() = this in setOf(PENDING_APPROVAL, APPROVED, ISSUED, ACKNOWLEDGED, PARTIALLY_FULFILLED)

    fun canTransitionTo(target: VendorPurchaseOrderStatus): Boolean {
        if (this == target) return true
        return when (this) {
            DRAFT -> target in setOf(PENDING_APPROVAL, CANCELLED)
            PENDING_APPROVAL -> target in setOf(APPROVED, DRAFT, CANCELLED)
            APPROVED -> target in setOf(ISSUED, CANCELLED)
            ISSUED -> target in setOf(ACKNOWLEDGED, PARTIALLY_FULFILLED, CANCELLED)
            ACKNOWLEDGED -> target in setOf(PARTIALLY_FULFILLED, FULFILLED, CANCELLED)
            PARTIALLY_FULFILLED -> target in setOf(FULFILLED, CANCELLED)
            FULFILLED -> target == CLOSED
            CLOSED -> false // Terminal
            CANCELLED -> false // Terminal
        }
    }
}
