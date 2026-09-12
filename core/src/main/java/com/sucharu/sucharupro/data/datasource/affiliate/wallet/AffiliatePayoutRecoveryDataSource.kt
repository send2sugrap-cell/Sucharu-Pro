package com.sucharu.sucharupro.data.datasource.affiliate.wallet

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.affiliate.wallet.AffiliatePayoutReconciliationRecord
import com.sucharu.sucharupro.domain.model.affiliate.wallet.AffiliatePayoutReversalRecord

/**
 * Data Source interface for Affiliate Payout Recovery, Reversal & Reconciliation persistence (Module 23 Step 08).
 */
interface AffiliatePayoutRecoveryDataSource {
    suspend fun saveReversalRecord(record: AffiliatePayoutReversalRecord): DomainResult<AffiliatePayoutReversalRecord>
    suspend fun getReversalRecordById(tenantId: String, reversalId: String): DomainResult<AffiliatePayoutReversalRecord?>
    suspend fun listReversalsForRequest(tenantId: String, requestId: String): DomainResult<List<AffiliatePayoutReversalRecord>>

    suspend fun saveReconciliationRecord(record: AffiliatePayoutReconciliationRecord): DomainResult<AffiliatePayoutReconciliationRecord>
    suspend fun listReconciliationsForRequest(tenantId: String, requestId: String): DomainResult<List<AffiliatePayoutReconciliationRecord>>
}
