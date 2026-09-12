package com.sucharu.sucharupro.domain.model.affiliate.wallet

import com.sucharu.sucharupro.domain.model.common.Money

/**
 * Authoritative Approved Earning Handoff Contract (Module 23 Step 03).
 * Represents an immutable, pre-calculated, approved earning passed to Module 23 for wallet crediting.
 * Module 23 DOES NOT calculate or alter the earning amount.
 */
data class ApprovedEarningHandoff(
    val earningReferenceId: String,
    val tenantId: String,
    val affiliateId: String,
    val amount: Money,
    val currency: String = "BDT",
    val source: String = "APPROVED_COMMISSION",
    val sourceOrderId: String? = null,
    val approvedAt: Long = System.currentTimeMillis(),
    val idempotencyKey: String? = null
)
