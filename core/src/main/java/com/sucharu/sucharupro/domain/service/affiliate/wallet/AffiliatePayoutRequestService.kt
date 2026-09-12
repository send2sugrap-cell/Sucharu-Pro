package com.sucharu.sucharupro.domain.service.affiliate.wallet

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.common.Money
import com.sucharu.sucharupro.domain.model.affiliate.wallet.*

/**
 * Domain Service interface for Payout Request lifecycle (Module 23 Step 05).
 */
interface AffiliatePayoutRequestService {
    suspend fun submitPayoutRequest(
        tenantId: String,
        walletId: String,
        requestedAmount: Money,
        payoutMethodType: AffiliatePayoutMethodType,
        accountName: String,
        accountNumber: String,
        provider: String? = null,
        branchRouting: String? = null,
        idempotencyKey: String? = null,
        minimumThreshold: Money? = null,
        actorId: String
    ): DomainResult<AffiliatePayoutRequest>

    suspend fun getPayoutRequestDetails(tenantId: String, requestId: String): DomainResult<AffiliatePayoutRequest?>
    suspend fun listPayoutRequestsForWallet(tenantId: String, walletId: String): DomainResult<List<AffiliatePayoutRequest>>
    suspend fun updatePayoutRequestStatus(
        tenantId: String,
        requestId: String,
        newStatus: AffiliatePayoutRequestStatus,
        reason: String? = null,
        actorId: String
    ): DomainResult<AffiliatePayoutRequest>

    // Step 06 Payout Review & Approval Governance
    suspend fun reviewPayoutRequest(
        tenantId: String,
        requestId: String,
        notes: String? = null,
        reviewerId: String
    ): DomainResult<AffiliatePayoutRequest>

    suspend fun approvePayoutRequest(
        tenantId: String,
        requestId: String,
        notes: String? = null,
        approverId: String
    ): DomainResult<AffiliatePayoutRequest>

    suspend fun rejectPayoutRequest(
        tenantId: String,
        requestId: String,
        rejectionReason: String,
        reviewerId: String
    ): DomainResult<AffiliatePayoutRequest>
}
