package com.sucharu.sucharupro.domain.model.quotationlock

import java.math.BigDecimal

/**
 * BI-03 Commercial Lock Lifecycle Status.
 */
enum class CommercialLockState {
    UNLOCKED,
    PENDING_APPROVAL,
    APPROVED,
    ACCEPTED,
    COMMERCIAL_LOCKED,
    AMENDMENT_PENDING
}

/**
 * Customer Acceptance Record for Quotation Agreement.
 */
data class CustomerAcceptanceRecord(
    val acceptedBy: String,
    val acceptedAt: String,
    val acceptanceChannel: String = "CUSTOMER_PORTAL", // CUSTOMER_PORTAL, EMAIL_APPROVAL, SIGNED_DOCUMENT
    val customerNotes: String? = null,
    val acceptedVersionNumber: Int
)

/**
 * BI-03 Immutable Commercial Lock Baseline.
 */
data class CommercialLockBaseline(
    val lockId: String,
    val quoteId: String,
    val quoteNumber: String,
    val acceptedVersionNumber: Int,
    val orderId: String,
    val orderPriceSnapshotId: String,

    val lockState: CommercialLockState = CommercialLockState.COMMERCIAL_LOCKED,
    val customerAcceptance: CustomerAcceptanceRecord,

    val agreedGrandTotal: BigDecimal,
    val agreedCurrency: String = "BDT",
    val isImmutable: Boolean = true, // Critical Invariant: Always true!

    val lockedAt: String,
    val lockedBy: String
) {
    init {
        require(lockId.isNotBlank()) { "Lock ID cannot be blank." }
        require(quoteId.isNotBlank()) { "Quote ID cannot be blank." }
        require(orderId.isNotBlank()) { "Order ID cannot be blank." }
        require(orderPriceSnapshotId.isNotBlank()) { "Order Price Snapshot ID cannot be blank." }
    }
}

/**
 * Authorized Post-Lock Commercial Amendment Request.
 */
data class CommercialAmendmentRequest(
    val amendmentId: String,
    val lockId: String,
    val orderId: String,
    val revisionNumber: Int,
    val requestedBy: String,
    val reason: String,
    val requestedChangesJson: String,
    val status: String = "PENDING", // PENDING, APPROVED, REJECTED
    val requestedAt: String
)
