package com.sucharu.sucharupro.data.datasource.affiliate.wallet

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.affiliate.wallet.AffiliatePayoutRequest
import java.util.concurrent.ConcurrentHashMap

/**
 * Thread-safe in-memory Fake Data Source for Affiliate Payout Request testing.
 */
class FakeAffiliatePayoutRequestDataSource : AffiliatePayoutRequestDataSource {

    private val requests = ConcurrentHashMap<String, AffiliatePayoutRequest>()

    override suspend fun savePayoutRequest(request: AffiliatePayoutRequest): DomainResult<AffiliatePayoutRequest> {
        val key = "${request.tenantId}:${request.requestId}"
        requests[key] = request
        return DomainResult.Success(request)
    }

    override suspend fun getRequestById(tenantId: String, requestId: String): DomainResult<AffiliatePayoutRequest?> {
        val key = "$tenantId:$requestId"
        return DomainResult.Success(requests[key])
    }

    override suspend fun getRequestByIdempotencyKey(
        tenantId: String,
        idempotencyKey: String
    ): DomainResult<AffiliatePayoutRequest?> {
        val found = requests.values.find { r ->
            r.tenantId == tenantId && r.idempotencyKey == idempotencyKey
        }
        return DomainResult.Success(found)
    }

    override suspend fun listRequestsForWallet(
        tenantId: String,
        walletId: String
    ): DomainResult<List<AffiliatePayoutRequest>> {
        val filtered = requests.values.filter { r ->
            r.tenantId == tenantId && r.walletId == walletId
        }.sortedByDescending { it.requestedAt }
        return DomainResult.Success(filtered)
    }

    override suspend fun updatePayoutRequestStatus(request: AffiliatePayoutRequest): DomainResult<AffiliatePayoutRequest> {
        val key = "${request.tenantId}:${request.requestId}"
        requests[key] = request
        return DomainResult.Success(request)
    }
}
