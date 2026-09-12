package com.sucharu.sucharupro.domain.repository.affiliate.wallet

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.affiliate.wallet.AffiliatePayoutReconciliationRecord
import com.sucharu.sucharupro.domain.model.affiliate.wallet.AffiliatePayoutReversalRecord

/**
 * Domain Repository interface for Affiliate Payout Recovery, Reversal & Reconciliation (Module 23 Step 08).
 */
interface AffiliatePayoutRecoveryRepository {
    suspend fun saveReversalRecord(record: AffiliatePayoutReversalRecord): DomainResult<AffiliatePayoutReversalRecord>
    suspend fun getReversalRecordById(tenantId: String, reversalId: String): DomainResult<AffiliatePayoutReversalRecord?>
    suspend fun listReversalsForRequest(tenantId: String, requestId: String): DomainResult<List<AffiliatePayoutReversalRecord>>

    suspend fun saveReconciliationRecord(record: AffiliatePayoutReconciliationRecord): DomainResult<AffiliatePayoutReconciliationRecord>
    suspend fun listReconciliationsForRequest(tenantId: String, requestId: String): DomainResult<List<AffiliatePayoutReconciliationRecord>>
}
