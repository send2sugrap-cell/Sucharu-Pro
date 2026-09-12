package com.sucharu.sucharupro.data.datasource.affiliate.wallet

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.affiliate.wallet.AffiliatePayoutRequest

/**
 * Data Source interface for Affiliate Payout Request persistence (Module 23 Step 05).
 */
interface AffiliatePayoutRequestDataSource {
    suspend fun savePayoutRequest(request: AffiliatePayoutRequest): DomainResult<AffiliatePayoutRequest>
    suspend fun getRequestById(tenantId: String, requestId: String): DomainResult<AffiliatePayoutRequest?>
    suspend fun getRequestByIdempotencyKey(tenantId: String, idempotencyKey: String): DomainResult<AffiliatePayoutRequest?>
    suspend fun listRequestsForWallet(tenantId: String, walletId: String): DomainResult<List<AffiliatePayoutRequest>>
    suspend fun updatePayoutRequestStatus(request: AffiliatePayoutRequest): DomainResult<AffiliatePayoutRequest>
}
