package com.sucharu.sucharupro.domain.model.delivery.pod

/**
 * High-level summary metrics for Proof of Delivery management (Module 08 Step 08).
 */
data class DeliveryProofSummary(
    val totalProofs: Int = 0,
    val draftCount: Int = 0,
    val pendingReviewCount: Int = 0,
    val submittedCount: Int = 0,
    val verifiedCount: Int = 0,
    val acceptedCount: Int = 0,
    val rejectedCount: Int = 0,
    val cancelledCount: Int = 0,
    val totalEvidenceCount: Int = 0
)
