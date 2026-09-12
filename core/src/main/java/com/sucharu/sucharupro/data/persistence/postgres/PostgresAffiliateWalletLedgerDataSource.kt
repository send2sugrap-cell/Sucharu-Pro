package com.sucharu.sucharupro.data.persistence.postgres

import com.sucharu.sucharupro.data.datasource.affiliate.wallet.AffiliateWalletLedgerDataSource
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.common.Money
import com.sucharu.sucharupro.domain.model.affiliate.wallet.AffiliateWalletLedgerDirection
import com.sucharu.sucharupro.domain.model.affiliate.wallet.AffiliateWalletLedgerEntry
import com.sucharu.sucharupro.domain.model.affiliate.wallet.AffiliateWalletLedgerEntryType
import java.sql.ResultSet

/**
 * PostgreSQL JDBC Data Source for Affiliate Wallet Ledger persistence with RLS and Money precision.
 */
class PostgresAffiliateWalletLedgerDataSource(
    private val transactionManager: TransactionManager
) : AffiliateWalletLedgerDataSource {

    override suspend fun postLedgerEntry(entry: AffiliateWalletLedgerEntry): DomainResult<AffiliateWalletLedgerEntry> {
        return try {
            transactionManager.inTransaction(TenantContext(entry.tenantId)) { ctx ->
                val conn = ctx.connection
                val sql = """
                    INSERT INTO affiliate_wallet_ledger_entries (
                        entry_id, tenant_id, wallet_id, affiliate_id,
                        currency, entry_type, direction, amount,
                        reference_id, idempotency_key, reversal_of_entry_id,
                        reason, actor_id, created_at
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    ON CONFLICT (tenant_id, idempotency_key) DO NOTHING
                """.trimIndent()

                conn.prepareStatement(sql).use { ps ->
                    ps.setString(1, entry.entryId)
                    ps.setString(2, entry.tenantId)
                    ps.setString(3, entry.walletId)
                    ps.setString(4, entry.affiliateId)
                    ps.setString(5, entry.currency)
                    ps.setString(6, entry.entryType.name)
                    ps.setString(7, entry.direction.name)
                    ps.setBigDecimal(8, entry.amount.amount)
                    ps.setString(9, entry.referenceId)
                    ps.setString(10, entry.idempotencyKey)
                    ps.setString(11, entry.reversalOfEntryId)
                    ps.setString(12, entry.reason)
                    ps.setString(13, entry.actorId)
                    ps.setLong(14, entry.createdAt)
                    ps.executeUpdate()
                }
            }
            DomainResult.Success(entry)
        } catch (e: Exception) {
            PostgresErrorTranslator.translate(e, "post affiliate wallet ledger entry")
        }
    }

    override suspend fun getLedgerEntryById(
        tenantId: String,
        entryId: String
    ): DomainResult<AffiliateWalletLedgerEntry?> {
        return try {
            val result = transactionManager.inTransaction(TenantContext(tenantId)) { ctx ->
                val conn = ctx.connection
                val sql = "SELECT * FROM affiliate_wallet_ledger_entries WHERE tenant_id = ? AND entry_id = ?"
                conn.prepareStatement(sql).use { ps ->
                    ps.setString(1, tenantId)
                    ps.setString(2, entryId)
                    ps.executeQuery().use { rs ->
                        if (rs.next()) mapEntry(rs) else null
                    }
                }
            }
            DomainResult.Success(result)
        } catch (e: Exception) {
            PostgresErrorTranslator.translate(e, "get affiliate wallet ledger entry by id")
        }
    }

    override suspend fun getLedgerEntryByIdempotencyKey(
        tenantId: String,
        idempotencyKey: String
    ): DomainResult<AffiliateWalletLedgerEntry?> {
        return try {
            val result = transactionManager.inTransaction(TenantContext(tenantId)) { ctx ->
                val conn = ctx.connection
                val sql = "SELECT * FROM affiliate_wallet_ledger_entries WHERE tenant_id = ? AND idempotency_key = ?"
                conn.prepareStatement(sql).use { ps ->
                    ps.setString(1, tenantId)
                    ps.setString(2, idempotencyKey)
                    ps.executeQuery().use { rs ->
                        if (rs.next()) mapEntry(rs) else null
                    }
                }
            }
            DomainResult.Success(result)
        } catch (e: Exception) {
            PostgresErrorTranslator.translate(e, "get affiliate wallet ledger entry by idempotency key")
        }
    }

    override suspend fun listLedgerEntriesForWallet(
        tenantId: String,
        walletId: String,
        limit: Int
    ): DomainResult<List<AffiliateWalletLedgerEntry>> {
        return try {
            val result = transactionManager.inTransaction(TenantContext(tenantId)) { ctx ->
                val conn = ctx.connection
                val sql = "SELECT * FROM affiliate_wallet_ledger_entries WHERE tenant_id = ? AND wallet_id = ? ORDER BY created_at ASC LIMIT ?"
                conn.prepareStatement(sql).use { ps ->
                    ps.setString(1, tenantId)
                    ps.setString(2, walletId)
                    ps.setInt(3, limit)
                    ps.executeQuery().use { rs ->
                        val list = mutableListOf<AffiliateWalletLedgerEntry>()
                        while (rs.next()) list.add(mapEntry(rs))
                        list
                    }
                }
            }
            DomainResult.Success(result)
        } catch (e: Exception) {
            PostgresErrorTranslator.translate(e, "list affiliate wallet ledger entries")
        }
    }

    override suspend fun getReversalForEntry(
        tenantId: String,
        originalEntryId: String
    ): DomainResult<AffiliateWalletLedgerEntry?> {
        return try {
            val result = transactionManager.inTransaction(TenantContext(tenantId)) { ctx ->
                val conn = ctx.connection
                val sql = "SELECT * FROM affiliate_wallet_ledger_entries WHERE tenant_id = ? AND reversal_of_entry_id = ?"
                conn.prepareStatement(sql).use { ps ->
                    ps.setString(1, tenantId)
                    ps.setString(2, originalEntryId)
                    ps.executeQuery().use { rs ->
                        if (rs.next()) mapEntry(rs) else null
                    }
                }
            }
            DomainResult.Success(result)
        } catch (e: Exception) {
            PostgresErrorTranslator.translate(e, "get reversal for affiliate wallet ledger entry")
        }
    }

    private fun mapEntry(rs: ResultSet): AffiliateWalletLedgerEntry {
        val amountBd = rs.getBigDecimal("amount")
        return AffiliateWalletLedgerEntry(
            entryId = rs.getString("entry_id"),
            tenantId = rs.getString("tenant_id"),
            walletId = rs.getString("wallet_id"),
            affiliateId = rs.getString("affiliate_id"),
            currency = rs.getString("currency"),
            entryType = try { AffiliateWalletLedgerEntryType.valueOf(rs.getString("entry_type")) } catch (_: Exception) { AffiliateWalletLedgerEntryType.CREDIT },
            direction = try { AffiliateWalletLedgerDirection.valueOf(rs.getString("direction")) } catch (_: Exception) { AffiliateWalletLedgerDirection.CREDIT },
            amount = Money(amountBd),
            referenceId = rs.getString("reference_id"),
            idempotencyKey = rs.getString("idempotency_key"),
            reversalOfEntryId = rs.getString("reversal_of_entry_id"),
            reason = rs.getString("reason"),
            actorId = rs.getString("actor_id"),
            createdAt = rs.getLong("created_at")
        )
    }
}
