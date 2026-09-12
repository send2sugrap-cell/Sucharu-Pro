package com.sucharu.sucharupro.data.datasource.affiliate.wallet

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.affiliate.wallet.AffiliatePayoutReconciliationRecord
import com.sucharu.sucharupro.domain.model.affiliate.wallet.AffiliatePayoutReversalRecord
import java.util.concurrent.ConcurrentHashMap

/**
 * Thread-safe in-memory Fake Data Source for Affiliate Payout Recovery, Reversal & Reconciliation testing.
 */
class FakeAffiliatePayoutRecoveryDataSource : AffiliatePayoutRecoveryDataSource {

    private val reversals = ConcurrentHashMap<String, AffiliatePayoutReversalRecord>()
    private val reconciliations = ConcurrentHashMap<String, AffiliatePayoutReconciliationRecord>()

    override suspend fun saveReversalRecord(record: AffiliatePayoutReversalRecord): DomainResult<AffiliatePayoutReversalRecord> {
        val key = "${record.tenantId}:${record.reversalId}"
        reversals[key] = record
        return DomainResult.Success(record)
    }

    override suspend fun getReversalRecordById(
        tenantId: String,
        reversalId: String
    ): DomainResult<AffiliatePayoutReversalRecord?> {
        val key = "$tenantId:$reversalId"
        return DomainResult.Success(reversals[key])
    }

    override suspend fun listReversalsForRequest(
        tenantId: String,
        requestId: String
    ): DomainResult<List<AffiliatePayoutReversalRecord>> {
        val filtered = reversals.values.filter { r ->
            r.tenantId == tenantId && r.requestId == requestId
        }.sortedByDescending { it.reversedAt }
        return DomainResult.Success(filtered)
    }

    override suspend fun saveReconciliationRecord(record: AffiliatePayoutReconciliationRecord): DomainResult<AffiliatePayoutReconciliationRecord> {
        val key = "${record.tenantId}:${record.reconciliationId}"
        reconciliations[key] = record
        return DomainResult.Success(record)
    }

    override suspend fun listReconciliationsForRequest(
        tenantId: String,
        requestId: String
    ): DomainResult<List<AffiliatePayoutReconciliationRecord>> {
        val filtered = reconciliations.values.filter { r ->
            r.tenantId == tenantId && r.requestId == requestId
        }.sortedByDescending { it.reconciledAt }
        return DomainResult.Success(filtered)
    }
}
