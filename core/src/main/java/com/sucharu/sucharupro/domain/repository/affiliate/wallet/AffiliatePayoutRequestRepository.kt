package com.sucharu.sucharupro.domain.repository.affiliate.wallet

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.affiliate.wallet.AffiliatePayoutRequest

/**
 * Domain Repository interface for Affiliate Payout Request operations (Module 23 Step 05).
 */
interface AffiliatePayoutRequestRepository {
    suspend fun savePayoutRequest(request: AffiliatePayoutRequest): DomainResult<AffiliatePayoutRequest>
    suspend fun getRequestById(tenantId: String, requestId: String): DomainResult<AffiliatePayoutRequest?>
    suspend fun getRequestByIdempotencyKey(tenantId: String, idempotencyKey: String): DomainResult<AffiliatePayoutRequest?>
    suspend fun listRequestsForWallet(tenantId: String, walletId: String): DomainResult<List<AffiliatePayoutRequest>>
    suspend fun updatePayoutRequestStatus(request: AffiliatePayoutRequest): DomainResult<AffiliatePayoutRequest>
}
