package com.sucharu.sucharupro.data.repository.affiliate.wallet

import com.sucharu.sucharupro.data.datasource.affiliate.wallet.AffiliatePayoutRecoveryDataSource
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.affiliate.wallet.AffiliatePayoutReconciliationRecord
import com.sucharu.sucharupro.domain.model.affiliate.wallet.AffiliatePayoutReversalRecord
import com.sucharu.sucharupro.domain.repository.affiliate.wallet.AffiliatePayoutRecoveryRepository

/**
 * Authoritative Repository implementation for Affiliate Payout Recovery, Reversal & Reconciliation operations.
 */
class AffiliatePayoutRecoveryRepositoryImpl(
    private val dataSource: AffiliatePayoutRecoveryDataSource
) : AffiliatePayoutRecoveryRepository {

    override suspend fun saveReversalRecord(record: AffiliatePayoutReversalRecord): DomainResult<AffiliatePayoutReversalRecord> {
        if (record.reversalId.isBlank()) {
            return DomainResult.Error(message = "Reversal ID cannot be blank.")
        }
        if (record.tenantId.isBlank()) {
            return DomainResult.Error(message = "Tenant ID cannot be blank.")
        }
        if (record.requestId.isBlank()) {
            return DomainResult.Error(message = "Payout Request ID cannot be blank.")
        }
        if (record.compensatingLedgerEntryId.isBlank()) {
            return DomainResult.Error(message = "Compensating ledger entry ID cannot be blank.")
        }
        if (record.reversalReason.isBlank()) {
            return DomainResult.Error(message = "Reversal reason cannot be blank.")
        }

        return dataSource.saveReversalRecord(record)
    }

    override suspend fun getReversalRecordById(
        tenantId: String,
        reversalId: String
    ): DomainResult<AffiliatePayoutReversalRecord?> {
        if (tenantId.isBlank() || reversalId.isBlank()) {
            return DomainResult.Error(message = "Tenant ID and Reversal ID cannot be blank.")
        }
        return dataSource.getReversalRecordById(tenantId, reversalId)
    }

    override suspend fun listReversalsForRequest(
        tenantId: String,
        requestId: String
    ): DomainResult<List<AffiliatePayoutReversalRecord>> {
        if (tenantId.isBlank() || requestId.isBlank()) {
            return DomainResult.Error(message = "Tenant ID and Payout Request ID cannot be blank.")
        }
        return dataSource.listReversalsForRequest(tenantId, requestId)
    }

    override suspend fun saveReconciliationRecord(record: AffiliatePayoutReconciliationRecord): DomainResult<AffiliatePayoutReconciliationRecord> {
        if (record.reconciliationId.isBlank()) {
            return DomainResult.Error(message = "Reconciliation ID cannot be blank.")
        }
        if (record.tenantId.isBlank()) {
            return DomainResult.Error(message = "Tenant ID cannot be blank.")
        }
        if (record.requestId.isBlank()) {
            return DomainResult.Error(message = "Payout Request ID cannot be blank.")
        }
        return dataSource.saveReconciliationRecord(record)
    }

    override suspend fun listReconciliationsForRequest(
        tenantId: String,
        requestId: String
    ): DomainResult<List<AffiliatePayoutReconciliationRecord>> {
        if (tenantId.isBlank() || requestId.isBlank()) {
            return DomainResult.Error(message = "Tenant ID and Payout Request ID cannot be blank.")
        }
        return dataSource.listReconciliationsForRequest(tenantId, requestId)
    }
}
