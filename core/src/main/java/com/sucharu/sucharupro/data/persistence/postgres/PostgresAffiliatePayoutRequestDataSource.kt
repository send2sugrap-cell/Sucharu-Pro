package com.sucharu.sucharupro.data.persistence.postgres

import com.sucharu.sucharupro.data.datasource.affiliate.wallet.AffiliatePayoutRequestDataSource
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.common.Money
import com.sucharu.sucharupro.domain.model.affiliate.wallet.AffiliatePayoutMethodType
import com.sucharu.sucharupro.domain.model.affiliate.wallet.AffiliatePayoutRequest
import com.sucharu.sucharupro.domain.model.affiliate.wallet.AffiliatePayoutRequestStatus
import java.sql.ResultSet

/**
 * PostgreSQL JDBC Data Source for Affiliate Payout Request persistence with RLS.
 */
class PostgresAffiliatePayoutRequestDataSource(
    private val transactionManager: TransactionManager
) : AffiliatePayoutRequestDataSource {

    override suspend fun savePayoutRequest(request: AffiliatePayoutRequest): DomainResult<AffiliatePayoutRequest> {
        return try {
            transactionManager.inTransaction(TenantContext(request.tenantId)) { ctx ->
                val conn = ctx.connection
                val sql = """
                    INSERT INTO affiliate_payout_requests (
                        request_id, tenant_id, wallet_id, affiliate_id,
                        requested_amount, currency, payout_method_type,
                        payout_method_account_name, payout_method_account_number,
                        payout_method_provider, payout_method_branch_routing,
                        status, reservation_hold_id, payout_reference,
                        idempotency_key, rejection_reason, review_notes,
                        requested_by, requested_at, updated_at, version
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    ON CONFLICT (tenant_id, idempotency_key) DO NOTHING
                """.trimIndent()

                conn.prepareStatement(sql).use { ps ->
                    ps.setString(1, request.requestId)
                    ps.setString(2, request.tenantId)
                    ps.setString(3, request.walletId)
                    ps.setString(4, request.affiliateId)
                    ps.setBigDecimal(5, request.requestedAmount.amount)
                    ps.setString(6, request.currency)
                    ps.setString(7, request.payoutMethodType.name)
                    ps.setString(8, request.payoutMethodAccountName)
                    ps.setString(9, request.payoutMethodAccountNumber)
                    ps.setString(10, request.payoutMethodProvider)
                    ps.setString(11, request.payoutMethodBranchRouting)
                    ps.setString(12, request.status.name)
                    ps.setString(13, request.reservationHoldId)
                    ps.setString(14, request.payoutReference)
                    ps.setString(15, request.idempotencyKey)
                    ps.setString(16, request.rejectionReason)
                    ps.setString(17, request.reviewNotes)
                    ps.setString(18, request.requestedBy)
                    ps.setLong(19, request.requestedAt)
                    ps.setLong(20, request.updatedAt)
                    ps.setLong(21, request.version)
                    ps.executeUpdate()
                }
            }
            DomainResult.Success(request)
        } catch (e: Exception) {
            PostgresErrorTranslator.translate(e, "save affiliate payout request")
        }
    }

    override suspend fun getRequestById(tenantId: String, requestId: String): DomainResult<AffiliatePayoutRequest?> {
        return try {
            val result = transactionManager.inTransaction(TenantContext(tenantId)) { ctx ->
                val conn = ctx.connection
                val sql = "SELECT * FROM affiliate_payout_requests WHERE tenant_id = ? AND request_id = ?"
                conn.prepareStatement(sql).use { ps ->
                    ps.setString(1, tenantId)
                    ps.setString(2, requestId)
                    ps.executeQuery().use { rs ->
                        if (rs.next()) mapRequest(rs) else null
                    }
                }
            }
            DomainResult.Success(result)
        } catch (e: Exception) {
            PostgresErrorTranslator.translate(e, "get affiliate payout request by id")
        }
    }

    override suspend fun getRequestByIdempotencyKey(
        tenantId: String,
        idempotencyKey: String
    ): DomainResult<AffiliatePayoutRequest?> {
        return try {
            val result = transactionManager.inTransaction(TenantContext(tenantId)) { ctx ->
                val conn = ctx.connection
                val sql = "SELECT * FROM affiliate_payout_requests WHERE tenant_id = ? AND idempotency_key = ?"
                conn.prepareStatement(sql).use { ps ->
                    ps.setString(1, tenantId)
                    ps.setString(2, idempotencyKey)
                    ps.executeQuery().use { rs ->
                        if (rs.next()) mapRequest(rs) else null
                    }
                }
            }
            DomainResult.Success(result)
        } catch (e: Exception) {
            PostgresErrorTranslator.translate(e, "get affiliate payout request by idempotency key")
        }
    }

    override suspend fun listRequestsForWallet(
        tenantId: String,
        walletId: String
    ): DomainResult<List<AffiliatePayoutRequest>> {
        return try {
            val result = transactionManager.inTransaction(TenantContext(tenantId)) { ctx ->
                val conn = ctx.connection
                val sql = "SELECT * FROM affiliate_payout_requests WHERE tenant_id = ? AND wallet_id = ? ORDER BY requested_at DESC"
                conn.prepareStatement(sql).use { ps ->
                    ps.setString(1, tenantId)
                    ps.setString(2, walletId)
                    ps.executeQuery().use { rs ->
                        val list = mutableListOf<AffiliatePayoutRequest>()
                        while (rs.next()) list.add(mapRequest(rs))
                        list
                    }
                }
            }
            DomainResult.Success(result)
        } catch (e: Exception) {
            PostgresErrorTranslator.translate(e, "list affiliate payout requests")
        }
    }

    override suspend fun updatePayoutRequestStatus(request: AffiliatePayoutRequest): DomainResult<AffiliatePayoutRequest> {
        return try {
            transactionManager.inTransaction(TenantContext(request.tenantId)) { ctx ->
                val conn = ctx.connection
                val sql = """
                    UPDATE affiliate_payout_requests SET
                        status = ?,
                        rejection_reason = ?,
                        review_notes = ?,
                        updated_at = ?,
                        version = version + 1
                    WHERE tenant_id = ? AND request_id = ?
                """.trimIndent()

                conn.prepareStatement(sql).use { ps ->
                    ps.setString(1, request.status.name)
                    ps.setString(2, request.rejectionReason)
                    ps.setString(3, request.reviewNotes)
                    ps.setLong(4, request.updatedAt)
                    ps.setString(5, request.tenantId)
                    ps.setString(6, request.requestId)
                    ps.executeUpdate()
                }
            }
            DomainResult.Success(request)
        } catch (e: Exception) {
            PostgresErrorTranslator.translate(e, "update affiliate payout request status")
        }
    }

    private fun mapRequest(rs: ResultSet): AffiliatePayoutRequest {
        val reqAmountBd = rs.getBigDecimal("requested_amount")
        return AffiliatePayoutRequest(
            requestId = rs.getString("request_id"),
            tenantId = rs.getString("tenant_id"),
            walletId = rs.getString("wallet_id"),
            affiliateId = rs.getString("affiliate_id"),
            requestedAmount = Money(reqAmountBd),
            currency = rs.getString("currency"),
            payoutMethodType = try { AffiliatePayoutMethodType.valueOf(rs.getString("payout_method_type")) } catch (_: Exception) { AffiliatePayoutMethodType.OTHER },
            payoutMethodAccountName = rs.getString("payout_method_account_name"),
            payoutMethodAccountNumber = rs.getString("payout_method_account_number"),
            payoutMethodProvider = rs.getString("payout_method_provider"),
            payoutMethodBranchRouting = rs.getString("payout_method_branch_routing"),
            status = try { AffiliatePayoutRequestStatus.valueOf(rs.getString("status")) } catch (_: Exception) { AffiliatePayoutRequestStatus.REQUESTED },
            reservationHoldId = rs.getString("reservation_hold_id"),
            payoutReference = rs.getString("payout_reference"),
            idempotencyKey = rs.getString("idempotency_key"),
            rejectionReason = rs.getString("rejection_reason"),
            reviewNotes = rs.getString("review_notes"),
            requestedBy = rs.getString("requested_by"),
            requestedAt = rs.getLong("requested_at"),
            updatedAt = rs.getLong("updated_at"),
            version = rs.getLong("version")
        )
    }
}
