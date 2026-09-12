package com.sucharu.sucharupro.data.repository.affiliate.wallet

import com.sucharu.sucharupro.data.datasource.affiliate.wallet.AffiliatePayoutRequestDataSource
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.affiliate.wallet.AffiliatePayoutRequest
import com.sucharu.sucharupro.domain.repository.affiliate.wallet.AffiliatePayoutRequestRepository

/**
 * Authoritative Repository implementation for Affiliate Payout Request operations with validation.
 */
class AffiliatePayoutRequestRepositoryImpl(
    private val dataSource: AffiliatePayoutRequestDataSource
) : AffiliatePayoutRequestRepository {

    override suspend fun savePayoutRequest(request: AffiliatePayoutRequest): DomainResult<AffiliatePayoutRequest> {
        if (request.requestId.isBlank()) {
            return DomainResult.Error(message = "Payout request ID cannot be blank.")
        }
        if (request.tenantId.isBlank()) {
            return DomainResult.Error(message = "Tenant ID cannot be blank.")
        }
        if (request.walletId.isBlank()) {
            return DomainResult.Error(message = "Wallet ID cannot be blank.")
        }
        if (!request.requestedAmount.isPositive()) {
            return DomainResult.Error(message = "Requested payout amount must be positive. Provided: ${request.requestedAmount.formatted()}")
        }
        if (request.payoutMethodAccountName.isBlank() || request.payoutMethodAccountNumber.isBlank()) {
            return DomainResult.Error(message = "Payout method account name and account number cannot be blank.")
        }

        // Check idempotency if idempotencyKey is present
        if (!request.idempotencyKey.isNullOrBlank()) {
            val existingRes = dataSource.getRequestByIdempotencyKey(request.tenantId, request.idempotencyKey)
            if (existingRes is DomainResult.Success && existingRes.data != null) {
                return DomainResult.Success(existingRes.data)
            }
        }

        return dataSource.savePayoutRequest(request)
    }

    override suspend fun getRequestById(tenantId: String, requestId: String): DomainResult<AffiliatePayoutRequest?> {
        if (tenantId.isBlank() || requestId.isBlank()) {
            return DomainResult.Error(message = "Tenant ID and Payout Request ID cannot be blank.")
        }
        return dataSource.getRequestById(tenantId, requestId)
    }

    override suspend fun getRequestByIdempotencyKey(
        tenantId: String,
        idempotencyKey: String
    ): DomainResult<AffiliatePayoutRequest?> {
        if (tenantId.isBlank() || idempotencyKey.isBlank()) {
            return DomainResult.Error(message = "Tenant ID and Idempotency Key cannot be blank.")
        }
        return dataSource.getRequestByIdempotencyKey(tenantId, idempotencyKey)
    }

    override suspend fun listRequestsForWallet(
        tenantId: String,
        walletId: String
    ): DomainResult<List<AffiliatePayoutRequest>> {
        if (tenantId.isBlank() || walletId.isBlank()) {
            return DomainResult.Error(message = "Tenant ID and Wallet ID cannot be blank.")
        }
        return dataSource.listRequestsForWallet(tenantId, walletId)
    }

    override suspend fun updatePayoutRequestStatus(request: AffiliatePayoutRequest): DomainResult<AffiliatePayoutRequest> {
        if (request.tenantId.isBlank() || request.requestId.isBlank()) {
            return DomainResult.Error(message = "Tenant ID and Payout Request ID cannot be blank.")
        }
        return dataSource.updatePayoutRequestStatus(request)
    }
}
