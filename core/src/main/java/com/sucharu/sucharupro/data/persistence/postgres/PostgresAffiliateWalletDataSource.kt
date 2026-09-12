package com.sucharu.sucharupro.data.persistence.postgres

import com.sucharu.sucharupro.data.datasource.affiliate.wallet.AffiliateWalletDataSource
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.affiliate.wallet.AffiliateWallet
import com.sucharu.sucharupro.domain.model.affiliate.wallet.AffiliateWalletStatus
import java.sql.ResultSet

/**
 * PostgreSQL JDBC Data Source for Affiliate Wallet persistence with RLS.
 */
class PostgresAffiliateWalletDataSource(
    private val transactionManager: TransactionManager
) : AffiliateWalletDataSource {

    override suspend fun saveWallet(wallet: AffiliateWallet): DomainResult<AffiliateWallet> {
        return try {
            transactionManager.inTransaction(TenantContext(wallet.tenantId)) { ctx ->
                val conn = ctx.connection
                val sql = """
                    INSERT INTO affiliate_wallets (
                        wallet_id, tenant_id, affiliate_id, currency,
                        status, created_at, updated_at, version
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                    ON CONFLICT (tenant_id, affiliate_id, currency) DO UPDATE SET
                        status = EXCLUDED.status,
                        updated_at = EXCLUDED.updated_at,
                        version = affiliate_wallets.version + 1
                """.trimIndent()

                conn.prepareStatement(sql).use { ps ->
                    ps.setString(1, wallet.walletId)
                    ps.setString(2, wallet.tenantId)
                    ps.setString(3, wallet.affiliateId)
                    ps.setString(4, wallet.currency)
                    ps.setString(5, wallet.status.name)
                    ps.setLong(6, wallet.createdAt)
                    ps.setLong(7, wallet.updatedAt)
                    ps.setLong(8, wallet.version)
                    ps.executeUpdate()
                }
            }
            DomainResult.Success(wallet)
        } catch (e: Exception) {
            PostgresErrorTranslator.translate(e, "save affiliate wallet")
        }
    }

    override suspend fun getWalletById(tenantId: String, walletId: String): DomainResult<AffiliateWallet?> {
        return try {
            val result = transactionManager.inTransaction(TenantContext(tenantId)) { ctx ->
                val conn = ctx.connection
                val sql = "SELECT * FROM affiliate_wallets WHERE tenant_id = ? AND wallet_id = ?"
                conn.prepareStatement(sql).use { ps ->
                    ps.setString(1, tenantId)
                    ps.setString(2, walletId)
                    ps.executeQuery().use { rs ->
                        if (rs.next()) mapWallet(rs) else null
                    }
                }
            }
            DomainResult.Success(result)
        } catch (e: Exception) {
            PostgresErrorTranslator.translate(e, "get affiliate wallet by id")
        }
    }

    override suspend fun getWalletByAffiliateId(
        tenantId: String,
        affiliateId: String,
        currency: String
    ): DomainResult<AffiliateWallet?> {
        return try {
            val result = transactionManager.inTransaction(TenantContext(tenantId)) { ctx ->
                val conn = ctx.connection
                val sql = "SELECT * FROM affiliate_wallets WHERE tenant_id = ? AND affiliate_id = ? AND UPPER(currency) = UPPER(?)"
                conn.prepareStatement(sql).use { ps ->
                    ps.setString(1, tenantId)
                    ps.setString(2, affiliateId)
                    ps.setString(3, currency)
                    ps.executeQuery().use { rs ->
                        if (rs.next()) mapWallet(rs) else null
                    }
                }
            }
            DomainResult.Success(result)
        } catch (e: Exception) {
            PostgresErrorTranslator.translate(e, "get affiliate wallet by affiliate id")
        }
    }

    override suspend fun updateWalletStatus(
        tenantId: String,
        walletId: String,
        status: AffiliateWalletStatus
    ): DomainResult<AffiliateWallet> {
        return try {
            val now = System.currentTimeMillis()
            val updated = transactionManager.inTransaction(TenantContext(tenantId)) { ctx ->
                val conn = ctx.connection
                val sql = """
                    UPDATE affiliate_wallets SET
                        status = ?,
                        updated_at = ?,
                        version = version + 1
                    WHERE tenant_id = ? AND wallet_id = ?
                    RETURNING *
                """.trimIndent()

                conn.prepareStatement(sql).use { ps ->
                    ps.setString(1, status.name)
                    ps.setLong(2, now)
                    ps.setString(3, tenantId)
                    ps.setString(4, walletId)
                    ps.executeQuery().use { rs ->
                        if (rs.next()) mapWallet(rs) else null
                    }
                }
            } ?: return DomainResult.Error(message = "Affiliate wallet '$walletId' not found for status update.")

            DomainResult.Success(updated)
        } catch (e: Exception) {
            PostgresErrorTranslator.translate(e, "update affiliate wallet status")
        }
    }

    private fun mapWallet(rs: ResultSet): AffiliateWallet {
        return AffiliateWallet(
            walletId = rs.getString("wallet_id"),
            tenantId = rs.getString("tenant_id"),
            affiliateId = rs.getString("affiliate_id"),
            currency = rs.getString("currency"),
            status = try { AffiliateWalletStatus.valueOf(rs.getString("status")) } catch (_: Exception) { AffiliateWalletStatus.ACTIVE },
            createdAt = rs.getLong("created_at"),
            updatedAt = rs.getLong("updated_at"),
            version = rs.getLong("version")
        )
    }
}
