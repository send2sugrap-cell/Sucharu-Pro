package com.sucharu.sucharupro.data.persistence.postgres

import com.sucharu.sucharupro.data.datasource.affiliate.wallet.AffiliatePayoutDisbursementDataSource
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.common.Money
import com.sucharu.sucharupro.domain.model.affiliate.wallet.AffiliatePayoutDisbursementRecord
import com.sucharu.sucharupro.domain.model.affiliate.wallet.DisbursementProviderStatus
import java.sql.ResultSet

/**
 * PostgreSQL JDBC Data Source for Affiliate Payout Disbursement persistence with RLS.
 */
class PostgresAffiliatePayoutDisbursementDataSource(
    private val transactionManager: TransactionManager
) : AffiliatePayoutDisbursementDataSource {

    override suspend fun saveDisbursementRecord(record: AffiliatePayoutDisbursementRecord): DomainResult<AffiliatePayoutDisbursementRecord> {
        return try {
            transactionManager.inTransaction(TenantContext(record.tenantId)) { ctx ->
                val conn = ctx.connection
                val sql = """
                    INSERT INTO affiliate_payout_disbursements (
                        disbursement_id, tenant_id, request_id, wallet_id,
                        affiliate_id, amount, currency, provider_name,
                        provider_transaction_ref, provider_status,
                        provider_response_code, failure_reason,
                        ledger_entry_id, processed_at
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    ON CONFLICT (disbursement_id) DO NOTHING
                """.trimIndent()

                conn.prepareStatement(sql).use { ps ->
                    ps.setString(1, record.disbursementId)
                    ps.setString(2, record.tenantId)
                    ps.setString(3, record.requestId)
                    ps.setString(4, record.walletId)
                    ps.setString(5, record.affiliateId)
                    ps.setBigDecimal(6, record.amount.amount)
                    ps.setString(7, record.currency)
                    ps.setString(8, record.providerName)
                    ps.setString(9, record.providerTransactionRef)
                    ps.setString(10, record.providerStatus.name)
                    ps.setString(11, record.providerResponseCode)
                    ps.setString(12, record.failureReason)
                    ps.setString(13, record.ledgerEntryId)
                    ps.setLong(14, record.processedAt)
                    ps.executeUpdate()
                }
            }
            DomainResult.Success(record)
        } catch (e: Exception) {
            PostgresErrorTranslator.translate(e, "save affiliate payout disbursement record")
        }
    }

    override suspend fun getDisbursementRecordById(
        tenantId: String,
        disbursementId: String
    ): DomainResult<AffiliatePayoutDisbursementRecord?> {
        return try {
            val result = transactionManager.inTransaction(TenantContext(tenantId)) { ctx ->
                val conn = ctx.connection
                val sql = "SELECT * FROM affiliate_payout_disbursements WHERE tenant_id = ? AND disbursement_id = ?"
                conn.prepareStatement(sql).use { ps ->
                    ps.setString(1, tenantId)
                    ps.setString(2, disbursementId)
                    ps.executeQuery().use { rs ->
                        if (rs.next()) mapRecord(rs) else null
                    }
                }
            }
            DomainResult.Success(result)
        } catch (e: Exception) {
            PostgresErrorTranslator.translate(e, "get affiliate payout disbursement record by id")
        }
    }

    override suspend fun listDisbursementsForRequest(
        tenantId: String,
        requestId: String
    ): DomainResult<List<AffiliatePayoutDisbursementRecord>> {
        return try {
            val result = transactionManager.inTransaction(TenantContext(tenantId)) { ctx ->
                val conn = ctx.connection
                val sql = "SELECT * FROM affiliate_payout_disbursements WHERE tenant_id = ? AND request_id = ? ORDER BY processed_at DESC"
                conn.prepareStatement(sql).use { ps ->
                    ps.setString(1, tenantId)
                    ps.setString(2, requestId)
                    ps.executeQuery().use { rs ->
                        val list = mutableListOf<AffiliatePayoutDisbursementRecord>()
                        while (rs.next()) list.add(mapRecord(rs))
                        list
                    }
                }
            }
            DomainResult.Success(result)
        } catch (e: Exception) {
            PostgresErrorTranslator.translate(e, "list affiliate payout disbursements")
        }
    }

    private fun mapRecord(rs: ResultSet): AffiliatePayoutDisbursementRecord {
        val amountBd = rs.getBigDecimal("amount")
        return AffiliatePayoutDisbursementRecord(
            disbursementId = rs.getString("disbursement_id"),
            tenantId = rs.getString("tenant_id"),
            requestId = rs.getString("request_id"),
            walletId = rs.getString("wallet_id"),
            affiliateId = rs.getString("affiliate_id"),
            amount = Money(amountBd),
            currency = rs.getString("currency"),
            providerName = rs.getString("provider_name"),
            providerTransactionRef = rs.getString("provider_transaction_ref"),
            providerStatus = try { DisbursementProviderStatus.valueOf(rs.getString("provider_status")) } catch (_: Exception) { DisbursementProviderStatus.UNKNOWN },
            providerResponseCode = rs.getString("provider_response_code"),
            failureReason = rs.getString("failure_reason"),
            ledgerEntryId = rs.getString("ledger_entry_id"),
            processedAt = rs.getLong("processed_at")
        )
    }
}
