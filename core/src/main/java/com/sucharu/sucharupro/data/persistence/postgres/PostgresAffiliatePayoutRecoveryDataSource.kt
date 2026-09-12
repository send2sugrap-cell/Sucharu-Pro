package com.sucharu.sucharupro.data.persistence.postgres

import com.sucharu.sucharupro.data.datasource.affiliate.wallet.AffiliatePayoutRecoveryDataSource
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.common.Money
import com.sucharu.sucharupro.domain.model.affiliate.wallet.*
import java.sql.ResultSet

/**
 * PostgreSQL JDBC Data Source for Affiliate Payout Recovery, Reversal & Reconciliation persistence with RLS.
 */
class PostgresAffiliatePayoutRecoveryDataSource(
    private val transactionManager: TransactionManager
) : AffiliatePayoutRecoveryDataSource {

    override suspend fun saveReversalRecord(record: AffiliatePayoutReversalRecord): DomainResult<AffiliatePayoutReversalRecord> {
        return try {
            transactionManager.inTransaction(TenantContext(record.tenantId)) { ctx ->
                val conn = ctx.connection
                val sql = """
                    INSERT INTO affiliate_payout_reversals (
                        reversal_id, tenant_id, request_id, wallet_id,
                        affiliate_id, reversed_amount, currency,
                        original_ledger_entry_id, compensating_ledger_entry_id,
                        reversal_reason, reversed_by, reversed_at
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    ON CONFLICT (reversal_id) DO NOTHING
                """.trimIndent()

                conn.prepareStatement(sql).use { ps ->
                    ps.setString(1, record.reversalId)
                    ps.setString(2, record.tenantId)
                    ps.setString(3, record.requestId)
                    ps.setString(4, record.walletId)
                    ps.setString(5, record.affiliateId)
                    ps.setBigDecimal(6, record.reversedAmount.amount)
                    ps.setString(7, record.currency)
                    ps.setString(8, record.originalLedgerEntryId)
                    ps.setString(9, record.compensatingLedgerEntryId)
                    ps.setString(10, record.reversalReason)
                    ps.setString(11, record.reversedBy)
                    ps.setLong(12, record.reversedAt)
                    ps.executeUpdate()
                }
            }
            DomainResult.Success(record)
        } catch (e: Exception) {
            PostgresErrorTranslator.translate(e, "save affiliate payout reversal record")
        }
    }

    override suspend fun getReversalRecordById(
        tenantId: String,
        reversalId: String
    ): DomainResult<AffiliatePayoutReversalRecord?> {
        return try {
            val result = transactionManager.inTransaction(TenantContext(tenantId)) { ctx ->
                val conn = ctx.connection
                val sql = "SELECT * FROM affiliate_payout_reversals WHERE tenant_id = ? AND reversal_id = ?"
                conn.prepareStatement(sql).use { ps ->
                    ps.setString(1, tenantId)
                    ps.setString(2, reversalId)
                    ps.executeQuery().use { rs ->
                        if (rs.next()) mapReversalRecord(rs) else null
                    }
                }
            }
            DomainResult.Success(result)
        } catch (e: Exception) {
            PostgresErrorTranslator.translate(e, "get affiliate payout reversal record by id")
        }
    }

    override suspend fun listReversalsForRequest(
        tenantId: String,
        requestId: String
    ): DomainResult<List<AffiliatePayoutReversalRecord>> {
        return try {
            val result = transactionManager.inTransaction(TenantContext(tenantId)) { ctx ->
                val conn = ctx.connection
                val sql = "SELECT * FROM affiliate_payout_reversals WHERE tenant_id = ? AND request_id = ? ORDER BY reversed_at DESC"
                conn.prepareStatement(sql).use { ps ->
                    ps.setString(1, tenantId)
                    ps.setString(2, requestId)
                    ps.executeQuery().use { rs ->
                        val list = mutableListOf<AffiliatePayoutReversalRecord>()
                        while (rs.next()) list.add(mapReversalRecord(rs))
                        list
                    }
                }
            }
            DomainResult.Success(result)
        } catch (e: Exception) {
            PostgresErrorTranslator.translate(e, "list affiliate payout reversals")
        }
    }

    override suspend fun saveReconciliationRecord(record: AffiliatePayoutReconciliationRecord): DomainResult<AffiliatePayoutReconciliationRecord> {
        return try {
            transactionManager.inTransaction(TenantContext(record.tenantId)) { ctx ->
                val conn = ctx.connection
                val sql = """
                    INSERT INTO affiliate_payout_reconciliations (
                        reconciliation_id, tenant_id, request_id, wallet_id,
                        internal_status, provider_status, provider_transaction_ref,
                        ledger_entry_id, reconciliation_status,
                        reconciliation_notes, reconciled_by, reconciled_at
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    ON CONFLICT (reconciliation_id) DO NOTHING
                """.trimIndent()

                conn.prepareStatement(sql).use { ps ->
                    ps.setString(1, record.reconciliationId)
                    ps.setString(2, record.tenantId)
                    ps.setString(3, record.requestId)
                    ps.setString(4, record.walletId)
                    ps.setString(5, record.internalStatus.name)
                    ps.setString(6, record.providerStatus?.name)
                    ps.setString(7, record.providerTransactionRef)
                    ps.setString(8, record.ledgerEntryId)
                    ps.setString(9, record.reconciliationStatus.name)
                    ps.setString(10, record.reconciliationNotes)
                    ps.setString(11, record.reconciledBy)
                    ps.setLong(12, record.reconciledAt)
                    ps.executeUpdate()
                }
            }
            DomainResult.Success(record)
        } catch (e: Exception) {
            PostgresErrorTranslator.translate(e, "save affiliate payout reconciliation record")
        }
    }

    override suspend fun listReconciliationsForRequest(
        tenantId: String,
        requestId: String
    ): DomainResult<List<AffiliatePayoutReconciliationRecord>> {
        return try {
            val result = transactionManager.inTransaction(TenantContext(tenantId)) { ctx ->
                val conn = ctx.connection
                val sql = "SELECT * FROM affiliate_payout_reconciliations WHERE tenant_id = ? AND request_id = ? ORDER BY reconciled_at DESC"
                conn.prepareStatement(sql).use { ps ->
                    ps.setString(1, tenantId)
                    ps.setString(2, requestId)
                    ps.executeQuery().use { rs ->
                        val list = mutableListOf<AffiliatePayoutReconciliationRecord>()
                        while (rs.next()) list.add(mapReconciliationRecord(rs))
                        list
                    }
                }
            }
            DomainResult.Success(result)
        } catch (e: Exception) {
            PostgresErrorTranslator.translate(e, "list affiliate payout reconciliations")
        }
    }

    private fun mapReversalRecord(rs: ResultSet): AffiliatePayoutReversalRecord {
        val amountBd = rs.getBigDecimal("reversed_amount")
        return AffiliatePayoutReversalRecord(
            reversalId = rs.getString("reversal_id"),
            tenantId = rs.getString("tenant_id"),
            requestId = rs.getString("request_id"),
            walletId = rs.getString("wallet_id"),
            affiliateId = rs.getString("affiliate_id"),
            reversedAmount = Money(amountBd),
            currency = rs.getString("currency"),
            originalLedgerEntryId = rs.getString("original_ledger_entry_id"),
            compensatingLedgerEntryId = rs.getString("compensating_ledger_entry_id"),
            reversalReason = rs.getString("reversal_reason"),
            reversedBy = rs.getString("reversed_by"),
            reversedAt = rs.getLong("reversed_at")
        )
    }

    private fun mapReconciliationRecord(rs: ResultSet): AffiliatePayoutReconciliationRecord {
        val provStatStr = rs.getString("provider_status")
        return AffiliatePayoutReconciliationRecord(
            reconciliationId = rs.getString("reconciliation_id"),
            tenantId = rs.getString("tenant_id"),
            requestId = rs.getString("request_id"),
            walletId = rs.getString("wallet_id"),
            internalStatus = try { AffiliatePayoutRequestStatus.valueOf(rs.getString("internal_status")) } catch (_: Exception) { AffiliatePayoutRequestStatus.REQUESTED },
            providerStatus = if (!provStatStr.isNullOrBlank()) try { DisbursementProviderStatus.valueOf(provStatStr) } catch (_: Exception) { null } else null,
            providerTransactionRef = rs.getString("provider_transaction_ref"),
            ledgerEntryId = rs.getString("ledger_entry_id"),
            reconciliationStatus = try { PayoutReconciliationStatus.valueOf(rs.getString("reconciliation_status")) } catch (_: Exception) { PayoutReconciliationStatus.INVESTIGATION_REQUIRED },
            reconciliationNotes = rs.getString("reconciliation_notes"),
            reconciledBy = rs.getString("reconciled_by"),
            reconciledAt = rs.getLong("reconciled_at")
        )
    }
}
