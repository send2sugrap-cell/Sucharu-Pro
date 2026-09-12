package com.sucharu.sucharupro.data.persistence.postgres

import com.sucharu.sucharupro.data.datasource.affiliate.wallet.AffiliateWalletHoldDataSource
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.common.Money
import com.sucharu.sucharupro.domain.model.affiliate.wallet.AffiliateWalletHold
import com.sucharu.sucharupro.domain.model.affiliate.wallet.AffiliateWalletHoldStatus
import com.sucharu.sucharupro.domain.model.affiliate.wallet.AffiliateWalletHoldType
import java.sql.ResultSet

/**
 * PostgreSQL JDBC Data Source for Affiliate Wallet Hold persistence with RLS.
 */
class PostgresAffiliateWalletHoldDataSource(
    private val transactionManager: TransactionManager
) : AffiliateWalletHoldDataSource {

    override suspend fun saveHold(hold: AffiliateWalletHold): DomainResult<AffiliateWalletHold> {
        return try {
            transactionManager.inTransaction(TenantContext(hold.tenantId)) { ctx ->
                val conn = ctx.connection
                val sql = """
                    INSERT INTO affiliate_wallet_holds (
                        hold_id, tenant_id, wallet_id, affiliate_id,
                        amount, currency, hold_type, hold_reason,
                        status, reference_id, created_by, created_at,
                        released_by, released_at, release_reason
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    ON CONFLICT (hold_id) DO NOTHING
                """.trimIndent()

                conn.prepareStatement(sql).use { ps ->
                    ps.setString(1, hold.holdId)
                    ps.setString(2, hold.tenantId)
                    ps.setString(3, hold.walletId)
                    ps.setString(4, hold.affiliateId)
                    ps.setBigDecimal(5, hold.amount.amount)
                    ps.setString(6, hold.currency)
                    ps.setString(7, hold.holdType.name)
                    ps.setString(8, hold.holdReason)
                    ps.setString(9, hold.status.name)
                    ps.setString(10, hold.referenceId)
                    ps.setString(11, hold.createdBy)
                    ps.setLong(12, hold.createdAt)
                    ps.setString(13, hold.releasedBy)
                    if (hold.releasedAt != null) ps.setLong(14, hold.releasedAt) else ps.setNull(14, java.sql.Types.BIGINT)
                    ps.setString(15, hold.releaseReason)
                    ps.executeUpdate()
                }
            }
            DomainResult.Success(hold)
        } catch (e: Exception) {
            PostgresErrorTranslator.translate(e, "save affiliate wallet hold")
        }
    }

    override suspend fun getHoldById(tenantId: String, holdId: String): DomainResult<AffiliateWalletHold?> {
        return try {
            val result = transactionManager.inTransaction(TenantContext(tenantId)) { ctx ->
                val conn = ctx.connection
                val sql = "SELECT * FROM affiliate_wallet_holds WHERE tenant_id = ? AND hold_id = ?"
                conn.prepareStatement(sql).use { ps ->
                    ps.setString(1, tenantId)
                    ps.setString(2, holdId)
                    ps.executeQuery().use { rs ->
                        if (rs.next()) mapHold(rs) else null
                    }
                }
            }
            DomainResult.Success(result)
        } catch (e: Exception) {
            PostgresErrorTranslator.translate(e, "get affiliate wallet hold by id")
        }
    }

    override suspend fun listHoldsForWallet(
        tenantId: String,
        walletId: String,
        activeOnly: Boolean
    ): DomainResult<List<AffiliateWalletHold>> {
        return try {
            val result = transactionManager.inTransaction(TenantContext(tenantId)) { ctx ->
                val conn = ctx.connection
                val sql = if (activeOnly) {
                    "SELECT * FROM affiliate_wallet_holds WHERE tenant_id = ? AND wallet_id = ? AND status = 'ACTIVE' ORDER BY created_at DESC"
                } else {
                    "SELECT * FROM affiliate_wallet_holds WHERE tenant_id = ? AND wallet_id = ? ORDER BY created_at DESC"
                }
                conn.prepareStatement(sql).use { ps ->
                    ps.setString(1, tenantId)
                    ps.setString(2, walletId)
                    ps.executeQuery().use { rs ->
                        val list = mutableListOf<AffiliateWalletHold>()
                        while (rs.next()) list.add(mapHold(rs))
                        list
                    }
                }
            }
            DomainResult.Success(result)
        } catch (e: Exception) {
            PostgresErrorTranslator.translate(e, "list affiliate wallet holds")
        }
    }

    override suspend fun updateHold(hold: AffiliateWalletHold): DomainResult<AffiliateWalletHold> {
        return try {
            transactionManager.inTransaction(TenantContext(hold.tenantId)) { ctx ->
                val conn = ctx.connection
                val sql = """
                    UPDATE affiliate_wallet_holds SET
                        status = ?,
                        released_by = ?,
                        released_at = ?,
                        release_reason = ?
                    WHERE tenant_id = ? AND hold_id = ?
                """.trimIndent()

                conn.prepareStatement(sql).use { ps ->
                    ps.setString(1, hold.status.name)
                    ps.setString(2, hold.releasedBy)
                    if (hold.releasedAt != null) ps.setLong(3, hold.releasedAt) else ps.setNull(3, java.sql.Types.BIGINT)
                    ps.setString(4, hold.releaseReason)
                    ps.setString(5, hold.tenantId)
                    ps.setString(6, hold.holdId)
                    ps.executeUpdate()
                }
            }
            DomainResult.Success(hold)
        } catch (e: Exception) {
            PostgresErrorTranslator.translate(e, "update affiliate wallet hold")
        }
    }

    private fun mapHold(rs: ResultSet): AffiliateWalletHold {
        val amountBd = rs.getBigDecimal("amount")
        val relAt = rs.getLong("released_at")
        val releasedAtVal = if (rs.wasNull()) null else relAt

        return AffiliateWalletHold(
            holdId = rs.getString("hold_id"),
            tenantId = rs.getString("tenant_id"),
            walletId = rs.getString("wallet_id"),
            affiliateId = rs.getString("affiliate_id"),
            amount = Money(amountBd),
            currency = rs.getString("currency"),
            holdType = try { AffiliateWalletHoldType.valueOf(rs.getString("hold_type")) } catch (_: Exception) { AffiliateWalletHoldType.BUSINESS_POLICY_HOLD },
            holdReason = rs.getString("hold_reason"),
            status = try { AffiliateWalletHoldStatus.valueOf(rs.getString("status")) } catch (_: Exception) { AffiliateWalletHoldStatus.ACTIVE },
            referenceId = rs.getString("reference_id"),
            createdBy = rs.getString("created_by"),
            createdAt = rs.getLong("created_at"),
            releasedBy = rs.getString("released_by"),
            releasedAt = releasedAtVal,
            releaseReason = rs.getString("release_reason")
        )
    }
}
