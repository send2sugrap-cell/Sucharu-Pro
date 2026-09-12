package com.sucharu.sucharupro.domain.service.affiliate.wallet

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.affiliate.wallet.AffiliatePayoutDisbursementRecord
import com.sucharu.sucharupro.domain.model.affiliate.wallet.AffiliatePayoutReconciliationRecord
import com.sucharu.sucharupro.domain.model.affiliate.wallet.AffiliatePayoutReversalRecord

/**
 * Domain Service interface for Affiliate Payout Failure Recovery, Retry, Reversal & Reconciliation (Module 23 Step 08).
 */
interface AffiliatePayoutRecoveryService {
    suspend fun retryFailedPayoutDisbursement(
        tenantId: String,
        requestId: String,
        actorId: String
    ): DomainResult<AffiliatePayoutDisbursementRecord>

    suspend fun reverseCompletedPayout(
        tenantId: String,
        requestId: String,
        reversalReason: String,
        actorId: String
    ): DomainResult<AffiliatePayoutReversalRecord>

    suspend fun reconcilePayoutDisbursement(
        tenantId: String,
        requestId: String,
        notes: String? = null,
        actorId: String
    ): DomainResult<AffiliatePayoutReconciliationRecord>

    suspend fun listReversalsForRequest(tenantId: String, requestId: String): DomainResult<List<AffiliatePayoutReversalRecord>>
    suspend fun listReconciliationsForRequest(tenantId: String, requestId: String): DomainResult<List<AffiliatePayoutReconciliationRecord>>
}
