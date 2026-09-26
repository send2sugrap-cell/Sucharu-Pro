package com.sucharu.sucharupro.data.api.model.quotationlock

import kotlinx.serialization.Serializable

@Serializable
data class AcceptAndLockQuotationRequestDto(
    val quoteId: String,
    val quoteNumber: String = "QUOTE-2026-001",
    val versionNumber: Int = 1,
    val customerId: String = "CUST-1001",
    val orderId: String = "ORD-1001",
    val orderPriceSnapshotId: String = "SNAP-1001",
    val agreedTotal: String = "410.00",
    val acceptanceChannel: String = "CUSTOMER_PORTAL"
)

@Serializable
data class CommercialLockBaselineDto(
    val lockId: String,
    val quoteId: String,
    val quoteNumber: String,
    val acceptedVersionNumber: Int,
    val orderId: String,
    val orderPriceSnapshotId: String,
    val lockState: String = "COMMERCIAL_LOCKED",
    val acceptedBy: String,
    val acceptedAt: String,
    val agreedGrandTotal: String,
    val agreedCurrency: String = "BDT",
    val isImmutable: Boolean = true,
    val lockedAt: String
)

@Serializable
data class CommercialAmendmentRequestDto(
    val amendmentId: String,
    val lockId: String,
    val orderId: String,
    val revisionNumber: Int,
    val requestedBy: String,
    val reason: String,
    val status: String = "PENDING",
    val requestedAt: String
)
