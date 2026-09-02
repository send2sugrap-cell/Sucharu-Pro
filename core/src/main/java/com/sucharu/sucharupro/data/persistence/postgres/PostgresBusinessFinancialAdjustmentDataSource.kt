package com.sucharu.sucharupro.data.persistence.postgres

import com.sucharu.sucharupro.data.datasource.businessfinancialadjustment.*
import com.sucharu.sucharupro.domain.model.businessfinancialadjustment.*
import java.math.BigDecimal
import java.math.RoundingMode
import java.sql.ResultSet

/**
 * Production PostgreSQL JDBC Data Source for Business Financial Adjustments, Refunds, Write-Offs, Postings & Audits.
 */
class PostgresBusinessFinancialAdjustmentDataSource(
    private val transactionManager: TransactionManager
) : BusinessFinancialAdjustmentDataSource {

    // --- Adjustments ---

    override suspend fun insertAdjustment(adjustment: BusinessFinancialAdjustment): BusinessFinancialAdjustment {
        return transactionManager.inTransaction(TenantContext(adjustment.projectId)) { tx ->
            val sql = """
                INSERT INTO business_financial_adjustments (
                    id, tenant_id, project_id, adjustment_number, adjustment_type, source_type, source_id,
                    original_transaction_id, original_amount, adjustment_amount, effective_amount,
                    currency, reason, justification, status, period_id, cost_center_id, job_id,
                    customer_id, vendor_id, created_by, reviewed_by, approved_by, posted_by,
                    cancelled_by, rejected_by, reversal_requested_by, reversal_approved_by,
                    reviewed_at, approved_at, posted_at, reversal_requested_at, reversal_approved_at,
                    reversed_at, ledger_posting_id, reversing_posting_id, idempotency_key, created_at, updated_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """.trimIndent()
            tx.connection.prepareStatement(sql).use { ps ->
                ps.setString(1, adjustment.id)
                ps.setString(2, adjustment.tenantId)
                ps.setString(3, adjustment.projectId)
                ps.setString(4, adjustment.adjustmentNumber)
                ps.setString(5, adjustment.adjustmentType.name)
                ps.setString(6, adjustment.sourceType.name)
                ps.setString(7, adjustment.sourceId)
                ps.setString(8, adjustment.originalTransactionId)
                ps.setBigDecimal(9, adjustment.originalAmount.setScale(4, RoundingMode.HALF_UP))
                ps.setBigDecimal(10, adjustment.adjustmentAmount.setScale(4, RoundingMode.HALF_UP))
                ps.setBigDecimal(11, adjustment.effectiveAmount.setScale(4, RoundingMode.HALF_UP))
                ps.setString(12, adjustment.currency)
                ps.setString(13, adjustment.reason)
                ps.setString(14, adjustment.justification)
                ps.setString(15, adjustment.status.name)
                ps.setString(16, adjustment.periodId)
                ps.setString(17, adjustment.costCenterId)
                ps.setString(18, adjustment.jobId)
                ps.setString(19, adjustment.customerId)
                ps.setString(20, adjustment.vendorId)
                ps.setString(21, adjustment.createdBy)
                ps.setString(22, adjustment.reviewedBy)
                ps.setString(23, adjustment.approvedBy)
                ps.setString(24, adjustment.postedBy)
                ps.setString(25, adjustment.cancelledBy)
                ps.setString(26, adjustment.rejectedBy)
                ps.setString(27, adjustment.reversalRequestedBy)
                ps.setString(28, adjustment.reversalApprovedBy)
                setNullableLong(ps, 29, adjustment.reviewedAt)
                setNullableLong(ps, 30, adjustment.approvedAt)
                setNullableLong(ps, 31, adjustment.postedAt)
                setNullableLong(ps, 32, adjustment.reversalRequestedAt)
                setNullableLong(ps, 33, adjustment.reversalApprovedAt)
                setNullableLong(ps, 34, adjustment.reversedAt)
                ps.setString(35, adjustment.ledgerPostingId)
                ps.setString(36, adjustment.reversingPostingId)
                ps.setString(37, adjustment.idempotencyKey)
                ps.setLong(38, adjustment.createdAt)
                ps.setLong(39, adjustment.updatedAt)
                ps.executeUpdate()
            }
            adjustment
        }
    }

    override suspend fun updateAdjustment(adjustment: BusinessFinancialAdjustment): BusinessFinancialAdjustment {
        return transactionManager.inTransaction(TenantContext(adjustment.projectId)) { tx ->
            val sql = """
                UPDATE business_financial_adjustments SET
                    adjustment_type = ?, source_type = ?, source_id = ?, original_transaction_id = ?,
                    original_amount = ?, adjustment_amount = ?, effective_amount = ?, currency = ?,
                    reason = ?, justification = ?, status = ?, period_id = ?, cost_center_id = ?,
                    job_id = ?, customer_id = ?, vendor_id = ?, reviewed_by = ?, approved_by = ?,
                    posted_by = ?, cancelled_by = ?, rejected_by = ?, reversal_requested_by = ?,
                    reversal_approved_by = ?, reviewed_at = ?, approved_at = ?, posted_at = ?,
                    reversal_requested_at = ?, reversal_approved_at = ?, reversed_at = ?,
                    ledger_posting_id = ?, reversing_posting_id = ?, idempotency_key = ?, updated_at = ?
                WHERE id = ? AND tenant_id = ? AND project_id = ?
            """.trimIndent()
            tx.connection.prepareStatement(sql).use { ps ->
                ps.setString(1, adjustment.adjustmentType.name)
                ps.setString(2, adjustment.sourceType.name)
                ps.setString(3, adjustment.sourceId)
                ps.setString(4, adjustment.originalTransactionId)
                ps.setBigDecimal(5, adjustment.originalAmount.setScale(4, RoundingMode.HALF_UP))
                ps.setBigDecimal(6, adjustment.adjustmentAmount.setScale(4, RoundingMode.HALF_UP))
                ps.setBigDecimal(7, adjustment.effectiveAmount.setScale(4, RoundingMode.HALF_UP))
                ps.setString(8, adjustment.currency)
                ps.setString(9, adjustment.reason)
                ps.setString(10, adjustment.justification)
                ps.setString(11, adjustment.status.name)
                ps.setString(12, adjustment.periodId)
                ps.setString(13, adjustment.costCenterId)
                ps.setString(14, adjustment.jobId)
                ps.setString(15, adjustment.customerId)
                ps.setString(16, adjustment.vendorId)
                ps.setString(17, adjustment.reviewedBy)
                ps.setString(18, adjustment.approvedBy)
                ps.setString(19, adjustment.postedBy)
                ps.setString(20, adjustment.cancelledBy)
                ps.setString(21, adjustment.rejectedBy)
                ps.setString(22, adjustment.reversalRequestedBy)
                ps.setString(23, adjustment.reversalApprovedBy)
                setNullableLong(ps, 24, adjustment.reviewedAt)
                setNullableLong(ps, 25, adjustment.approvedAt)
                setNullableLong(ps, 26, adjustment.postedAt)
                setNullableLong(ps, 27, adjustment.reversalRequestedAt)
                setNullableLong(ps, 28, adjustment.reversalApprovedAt)
                setNullableLong(ps, 29, adjustment.reversedAt)
                ps.setString(30, adjustment.ledgerPostingId)
                ps.setString(31, adjustment.reversingPostingId)
                ps.setString(32, adjustment.idempotencyKey)
                ps.setLong(33, adjustment.updatedAt)
                ps.setString(34, adjustment.id)
                ps.setString(35, adjustment.tenantId)
                ps.setString(36, adjustment.projectId)
                ps.executeUpdate()
            }
            adjustment
        }
    }

    override suspend fun findAdjustmentById(id: String, tenantId: String, projectId: String): BusinessFinancialAdjustment? {
        return transactionManager.inTransaction(TenantContext(projectId)) { tx ->
            val sql = "SELECT * FROM business_financial_adjustments WHERE id = ? AND tenant_id = ? AND project_id = ?"
            tx.connection.prepareStatement(sql).use { ps ->
                ps.setString(1, id)
                ps.setString(2, tenantId)
                ps.setString(3, projectId)
                ps.executeQuery().use { rs ->
                    if (rs.next()) mapAdjustment(rs) else null
                }
            }
        }
    }

    override suspend fun findAdjustmentByNumber(number: String, tenantId: String, projectId: String): BusinessFinancialAdjustment? {
        return transactionManager.inTransaction(TenantContext(projectId)) { tx ->
            val sql = "SELECT * FROM business_financial_adjustments WHERE adjustment_number = ? AND tenant_id = ? AND project_id = ?"
            tx.connection.prepareStatement(sql).use { ps ->
                ps.setString(1, number)
                ps.setString(2, tenantId)
                ps.setString(3, projectId)
                ps.executeQuery().use { rs ->
                    if (rs.next()) mapAdjustment(rs) else null
                }
            }
        }
    }

    override suspend fun listAdjustments(tenantId: String, projectId: String, filter: AdjustmentFilter): List<BusinessFinancialAdjustment> {
        return transactionManager.inTransaction(TenantContext(projectId)) { tx ->
            val conditions = mutableListOf("tenant_id = ?", "project_id = ?")
            val params = mutableListOf<Any>(tenantId, projectId)

            if (filter.adjustmentType != null) {
                conditions.add("adjustment_type = ?")
                params.add(filter.adjustmentType.name)
            }
            if (filter.sourceType != null) {
                conditions.add("source_type = ?")
                params.add(filter.sourceType.name)
            }
            if (filter.sourceId != null) {
                conditions.add("source_id = ?")
                params.add(filter.sourceId)
            }
            if (filter.status != null) {
                conditions.add("status = ?")
                params.add(filter.status.name)
            }
            if (filter.periodId != null) {
                conditions.add("period_id = ?")
                params.add(filter.periodId)
            }
            if (filter.customerId != null) {
                conditions.add("customer_id = ?")
                params.add(filter.customerId)
            }
            if (filter.vendorId != null) {
                conditions.add("vendor_id = ?")
                params.add(filter.vendorId)
            }
            if (filter.jobId != null) {
                conditions.add("job_id = ?")
                params.add(filter.jobId)
            }
            if (filter.costCenterId != null) {
                conditions.add("cost_center_id = ?")
                params.add(filter.costCenterId)
            }

            val sql = "SELECT * FROM business_financial_adjustments WHERE ${conditions.joinToString(" AND ")} ORDER BY created_at DESC"
            tx.connection.prepareStatement(sql).use { ps ->
                params.forEachIndexed { idx, value -> ps.setObject(idx + 1, value) }
                ps.executeQuery().use { rs ->
                    val list = mutableListOf<BusinessFinancialAdjustment>()
                    while (rs.next()) {
                        list.add(mapAdjustment(rs))
                    }
                    list
                }
            }
        }
    }

    // --- Refunds ---

    override suspend fun insertRefund(refund: BusinessFinancialRefund): BusinessFinancialRefund {
        return transactionManager.inTransaction(TenantContext(refund.projectId)) { tx ->
            val sql = """
                INSERT INTO business_financial_refunds (
                    id, tenant_id, project_id, refund_number, source_type, source_id, customer_id,
                    vendor_id, original_transaction_id, eligible_balance, requested_amount,
                    approved_amount, currency, refund_reason, payment_method, status, period_id,
                    requested_by, approved_by, posted_by, approved_at, posted_at, settled_at,
                    ledger_posting_id, idempotency_key, created_at, updated_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """.trimIndent()
            tx.connection.prepareStatement(sql).use { ps ->
                ps.setString(1, refund.id)
                ps.setString(2, refund.tenantId)
                ps.setString(3, refund.projectId)
                ps.setString(4, refund.refundNumber)
                ps.setString(5, refund.sourceType.name)
                ps.setString(6, refund.sourceId)
                ps.setString(7, refund.customerId)
                ps.setString(8, refund.vendorId)
                ps.setString(9, refund.originalTransactionId)
                ps.setBigDecimal(10, refund.eligibleBalance.setScale(4, RoundingMode.HALF_UP))
                ps.setBigDecimal(11, refund.requestedAmount.setScale(4, RoundingMode.HALF_UP))
                ps.setBigDecimal(12, refund.approvedAmount.setScale(4, RoundingMode.HALF_UP))
                ps.setString(13, refund.currency)
                ps.setString(14, refund.refundReason)
                ps.setString(15, refund.paymentMethod)
                ps.setString(16, refund.status.name)
                ps.setString(17, refund.periodId)
                ps.setString(18, refund.requestedBy)
                ps.setString(19, refund.approvedBy)
                ps.setString(20, refund.postedBy)
                setNullableLong(ps, 21, refund.approvedAt)
                setNullableLong(ps, 22, refund.postedAt)
                setNullableLong(ps, 23, refund.settledAt)
                ps.setString(24, refund.ledgerPostingId)
                ps.setString(25, refund.idempotencyKey)
                ps.setLong(26, refund.createdAt)
                ps.setLong(27, refund.updatedAt)
                ps.executeUpdate()
            }
            refund
        }
    }

    override suspend fun updateRefund(refund: BusinessFinancialRefund): BusinessFinancialRefund {
        return transactionManager.inTransaction(TenantContext(refund.projectId)) { tx ->
            val sql = """
                UPDATE business_financial_refunds SET
                    source_type = ?, source_id = ?, customer_id = ?, vendor_id = ?, original_transaction_id = ?,
                    eligible_balance = ?, requested_amount = ?, approved_amount = ?, currency = ?,
                    refund_reason = ?, payment_method = ?, status = ?, period_id = ?, approved_by = ?,
                    posted_by = ?, approved_at = ?, posted_at = ?, settled_at = ?, ledger_posting_id = ?,
                    idempotency_key = ?, updated_at = ?
                WHERE id = ? AND tenant_id = ? AND project_id = ?
            """.trimIndent()
            tx.connection.prepareStatement(sql).use { ps ->
                ps.setString(1, refund.sourceType.name)
                ps.setString(2, refund.sourceId)
                ps.setString(3, refund.customerId)
                ps.setString(4, refund.vendorId)
                ps.setString(5, refund.originalTransactionId)
                ps.setBigDecimal(6, refund.eligibleBalance.setScale(4, RoundingMode.HALF_UP))
                ps.setBigDecimal(7, refund.requestedAmount.setScale(4, RoundingMode.HALF_UP))
                ps.setBigDecimal(8, refund.approvedAmount.setScale(4, RoundingMode.HALF_UP))
                ps.setString(9, refund.currency)
                ps.setString(10, refund.refundReason)
                ps.setString(11, refund.paymentMethod)
                ps.setString(12, refund.status.name)
                ps.setString(13, refund.periodId)
                ps.setString(14, refund.approvedBy)
                ps.setString(15, refund.postedBy)
                setNullableLong(ps, 16, refund.approvedAt)
                setNullableLong(ps, 17, refund.postedAt)
                setNullableLong(ps, 18, refund.settledAt)
                ps.setString(19, refund.ledgerPostingId)
                ps.setString(20, refund.idempotencyKey)
                ps.setLong(21, refund.updatedAt)
                ps.setString(22, refund.id)
                ps.setString(23, refund.tenantId)
                ps.setString(24, refund.projectId)
                ps.executeUpdate()
            }
            refund
        }
    }

    override suspend fun findRefundById(id: String, tenantId: String, projectId: String): BusinessFinancialRefund? {
        return transactionManager.inTransaction(TenantContext(projectId)) { tx ->
            val sql = "SELECT * FROM business_financial_refunds WHERE id = ? AND tenant_id = ? AND project_id = ?"
            tx.connection.prepareStatement(sql).use { ps ->
                ps.setString(1, id)
                ps.setString(2, tenantId)
                ps.setString(3, projectId)
                ps.executeQuery().use { rs ->
                    if (rs.next()) mapRefund(rs) else null
                }
            }
        }
    }

    override suspend fun findRefundByNumber(number: String, tenantId: String, projectId: String): BusinessFinancialRefund? {
        return transactionManager.inTransaction(TenantContext(projectId)) { tx ->
            val sql = "SELECT * FROM business_financial_refunds WHERE refund_number = ? AND tenant_id = ? AND project_id = ?"
            tx.connection.prepareStatement(sql).use { ps ->
                ps.setString(1, number)
                ps.setString(2, tenantId)
                ps.setString(3, projectId)
                ps.executeQuery().use { rs ->
                    if (rs.next()) mapRefund(rs) else null
                }
            }
        }
    }

    override suspend fun listRefunds(tenantId: String, projectId: String, filter: RefundFilter): List<BusinessFinancialRefund> {
        return transactionManager.inTransaction(TenantContext(projectId)) { tx ->
            val conditions = mutableListOf("tenant_id = ?", "project_id = ?")
            val params = mutableListOf<Any>(tenantId, projectId)

            if (filter.sourceType != null) {
                conditions.add("source_type = ?")
                params.add(filter.sourceType.name)
            }
            if (filter.sourceId != null) {
                conditions.add("source_id = ?")
                params.add(filter.sourceId)
            }
            if (filter.status != null) {
                conditions.add("status = ?")
                params.add(filter.status.name)
            }
            if (filter.periodId != null) {
                conditions.add("period_id = ?")
                params.add(filter.periodId)
            }
            if (filter.customerId != null) {
                conditions.add("customer_id = ?")
                params.add(filter.customerId)
            }
            if (filter.vendorId != null) {
                conditions.add("vendor_id = ?")
                params.add(filter.vendorId)
            }

            val sql = "SELECT * FROM business_financial_refunds WHERE ${conditions.joinToString(" AND ")} ORDER BY created_at DESC"
            tx.connection.prepareStatement(sql).use { ps ->
                params.forEachIndexed { idx, value -> ps.setObject(idx + 1, value) }
                ps.executeQuery().use { rs ->
                    val list = mutableListOf<BusinessFinancialRefund>()
                    while (rs.next()) {
                        list.add(mapRefund(rs))
                    }
                    list
                }
            }
        }
    }

    // --- Write-Offs ---

    override suspend fun insertWriteOff(writeOff: BusinessFinancialWriteOff): BusinessFinancialWriteOff {
        return transactionManager.inTransaction(TenantContext(writeOff.projectId)) { tx ->
            val sql = """
                INSERT INTO business_financial_write_offs (
                    id, tenant_id, project_id, write_off_number, source_type, source_id, write_off_type,
                    eligible_balance, amount, currency, reason, justification, status, period_id,
                    customer_id, vendor_id, requested_by, approved_by, posted_by, approved_at,
                    posted_at, ledger_posting_id, idempotency_key, created_at, updated_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """.trimIndent()
            tx.connection.prepareStatement(sql).use { ps ->
                ps.setString(1, writeOff.id)
                ps.setString(2, writeOff.tenantId)
                ps.setString(3, writeOff.projectId)
                ps.setString(4, writeOff.writeOffNumber)
                ps.setString(5, writeOff.sourceType.name)
                ps.setString(6, writeOff.sourceId)
                ps.setString(7, writeOff.writeOffType.name)
                ps.setBigDecimal(8, writeOff.eligibleBalance.setScale(4, RoundingMode.HALF_UP))
                ps.setBigDecimal(9, writeOff.amount.setScale(4, RoundingMode.HALF_UP))
                ps.setString(10, writeOff.currency)
                ps.setString(11, writeOff.reason)
                ps.setString(12, writeOff.justification)
                ps.setString(13, writeOff.status.name)
                ps.setString(14, writeOff.periodId)
                ps.setString(15, writeOff.customerId)
                ps.setString(16, writeOff.vendorId)
                ps.setString(17, writeOff.requestedBy)
                ps.setString(18, writeOff.approvedBy)
                ps.setString(19, writeOff.postedBy)
                setNullableLong(ps, 20, writeOff.approvedAt)
                setNullableLong(ps, 21, writeOff.postedAt)
                ps.setString(22, writeOff.ledgerPostingId)
                ps.setString(23, writeOff.idempotencyKey)
                ps.setLong(24, writeOff.createdAt)
                ps.setLong(25, writeOff.updatedAt)
                ps.executeUpdate()
            }
            writeOff
        }
    }

    override suspend fun updateWriteOff(writeOff: BusinessFinancialWriteOff): BusinessFinancialWriteOff {
        return transactionManager.inTransaction(TenantContext(writeOff.projectId)) { tx ->
            val sql = """
                UPDATE business_financial_write_offs SET
                    source_type = ?, source_id = ?, write_off_type = ?, eligible_balance = ?,
                    amount = ?, currency = ?, reason = ?, justification = ?, status = ?,
                    period_id = ?, customer_id = ?, vendor_id = ?, approved_by = ?, posted_by = ?,
                    approved_at = ?, posted_at = ?, ledger_posting_id = ?, idempotency_key = ?, updated_at = ?
                WHERE id = ? AND tenant_id = ? AND project_id = ?
            """.trimIndent()
            tx.connection.prepareStatement(sql).use { ps ->
                ps.setString(1, writeOff.sourceType.name)
                ps.setString(2, writeOff.sourceId)
                ps.setString(3, writeOff.writeOffType.name)
                ps.setBigDecimal(4, writeOff.eligibleBalance.setScale(4, RoundingMode.HALF_UP))
                ps.setBigDecimal(5, writeOff.amount.setScale(4, RoundingMode.HALF_UP))
                ps.setString(6, writeOff.currency)
                ps.setString(7, writeOff.reason)
                ps.setString(8, writeOff.justification)
                ps.setString(9, writeOff.status.name)
                ps.setString(10, writeOff.periodId)
                ps.setString(11, writeOff.customerId)
                ps.setString(12, writeOff.vendorId)
                ps.setString(13, writeOff.approvedBy)
                ps.setString(14, writeOff.postedBy)
                setNullableLong(ps, 15, writeOff.approvedAt)
                setNullableLong(ps, 16, writeOff.postedAt)
                ps.setString(17, writeOff.ledgerPostingId)
                ps.setString(18, writeOff.idempotencyKey)
                ps.setLong(19, writeOff.updatedAt)
                ps.setString(20, writeOff.id)
                ps.setString(21, writeOff.tenantId)
                ps.setString(22, writeOff.projectId)
                ps.executeUpdate()
            }
            writeOff
        }
    }

    override suspend fun findWriteOffById(id: String, tenantId: String, projectId: String): BusinessFinancialWriteOff? {
        return transactionManager.inTransaction(TenantContext(projectId)) { tx ->
            val sql = "SELECT * FROM business_financial_write_offs WHERE id = ? AND tenant_id = ? AND project_id = ?"
            tx.connection.prepareStatement(sql).use { ps ->
                ps.setString(1, id)
                ps.setString(2, tenantId)
                ps.setString(3, projectId)
                ps.executeQuery().use { rs ->
                    if (rs.next()) mapWriteOff(rs) else null
                }
            }
        }
    }

    override suspend fun findWriteOffByNumber(number: String, tenantId: String, projectId: String): BusinessFinancialWriteOff? {
        return transactionManager.inTransaction(TenantContext(projectId)) { tx ->
            val sql = "SELECT * FROM business_financial_write_offs WHERE write_off_number = ? AND tenant_id = ? AND project_id = ?"
            tx.connection.prepareStatement(sql).use { ps ->
                ps.setString(1, number)
                ps.setString(2, tenantId)
                ps.setString(3, projectId)
                ps.executeQuery().use { rs ->
                    if (rs.next()) mapWriteOff(rs) else null
                }
            }
        }
    }

    override suspend fun listWriteOffs(tenantId: String, projectId: String, filter: WriteOffFilter): List<BusinessFinancialWriteOff> {
        return transactionManager.inTransaction(TenantContext(projectId)) { tx ->
            val conditions = mutableListOf("tenant_id = ?", "project_id = ?")
            val params = mutableListOf<Any>(tenantId, projectId)

            if (filter.writeOffType != null) {
                conditions.add("write_off_type = ?")
                params.add(filter.writeOffType.name)
            }
            if (filter.sourceType != null) {
                conditions.add("source_type = ?")
                params.add(filter.sourceType.name)
            }
            if (filter.sourceId != null) {
                conditions.add("source_id = ?")
                params.add(filter.sourceId)
            }
            if (filter.status != null) {
                conditions.add("status = ?")
                params.add(filter.status.name)
            }
            if (filter.periodId != null) {
                conditions.add("period_id = ?")
                params.add(filter.periodId)
            }
            if (filter.customerId != null) {
                conditions.add("customer_id = ?")
                params.add(filter.customerId)
            }
            if (filter.vendorId != null) {
                conditions.add("vendor_id = ?")
                params.add(filter.vendorId)
            }

            val sql = "SELECT * FROM business_financial_write_offs WHERE ${conditions.joinToString(" AND ")} ORDER BY created_at DESC"
            tx.connection.prepareStatement(sql).use { ps ->
                params.forEachIndexed { idx, value -> ps.setObject(idx + 1, value) }
                ps.executeQuery().use { rs ->
                    val list = mutableListOf<BusinessFinancialWriteOff>()
                    while (rs.next()) {
                        list.add(mapWriteOff(rs))
                    }
                    list
                }
            }
        }
    }

    // --- Compensating Postings ---

    override suspend fun insertPosting(posting: BusinessFinancialAdjustmentPosting): BusinessFinancialAdjustmentPosting {
        return transactionManager.inTransaction(TenantContext(posting.projectId)) { tx ->
            val sql = """
                INSERT INTO business_financial_adjustment_postings (
                    id, tenant_id, project_id, adjustment_id, posting_number, ledger_posting_id,
                    posting_type, debit_account, credit_account, amount, currency, status,
                    posted_by, posted_at, idempotency_key, created_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """.trimIndent()
            tx.connection.prepareStatement(sql).use { ps ->
                ps.setString(1, posting.id)
                ps.setString(2, posting.tenantId)
                ps.setString(3, posting.projectId)
                ps.setString(4, posting.adjustmentId)
                ps.setString(5, posting.postingNumber)
                ps.setString(6, posting.ledgerPostingId)
                ps.setString(7, posting.postingType.name)
                ps.setString(8, posting.debitAccount)
                ps.setString(9, posting.creditAccount)
                ps.setBigDecimal(10, posting.amount.setScale(4, RoundingMode.HALF_UP))
                ps.setString(11, posting.currency)
                ps.setString(12, posting.status)
                ps.setString(13, posting.postedBy)
                ps.setLong(14, posting.postedAt)
                ps.setString(15, posting.idempotencyKey)
                ps.setLong(16, posting.createdAt)
                ps.executeUpdate()
            }
            posting
        }
    }

    override suspend fun listPostingsByAdjustmentId(adjustmentId: String, tenantId: String, projectId: String): List<BusinessFinancialAdjustmentPosting> {
        return transactionManager.inTransaction(TenantContext(projectId)) { tx ->
            val sql = "SELECT * FROM business_financial_adjustment_postings WHERE adjustment_id = ? AND tenant_id = ? AND project_id = ? ORDER BY created_at DESC"
            tx.connection.prepareStatement(sql).use { ps ->
                ps.setString(1, adjustmentId)
                ps.setString(2, tenantId)
                ps.setString(3, projectId)
                ps.executeQuery().use { rs ->
                    val list = mutableListOf<BusinessFinancialAdjustmentPosting>()
                    while (rs.next()) {
                        list.add(mapPosting(rs))
                    }
                    list
                }
            }
        }
    }

    // --- Audit Trail ---

    override suspend fun recordAuditEvent(event: BusinessFinancialAdjustmentAuditEvent): BusinessFinancialAdjustmentAuditEvent {
        return transactionManager.inTransaction(TenantContext(event.projectId)) { tx ->
            val sql = """
                INSERT INTO business_financial_adjustment_audit_events (
                    id, tenant_id, project_id, entity_type, entity_id, event_type, actor_id,
                    actor_role, timestamp, previous_status, new_status, reason, metadata_json,
                    correlation_id, idempotency_key
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """.trimIndent()
            tx.connection.prepareStatement(sql).use { ps ->
                ps.setString(1, event.id)
                ps.setString(2, event.tenantId)
                ps.setString(3, event.projectId)
                ps.setString(4, event.entityType)
                ps.setString(5, event.entityId)
                ps.setString(6, event.eventType)
                ps.setString(7, event.actorId)
                ps.setString(8, event.actorRole)
                ps.setLong(9, event.timestamp)
                ps.setString(10, event.previousStatus)
                ps.setString(11, event.newStatus)
                ps.setString(12, event.reason)
                ps.setString(13, event.metadataJson)
                ps.setString(14, event.correlationId)
                ps.setString(15, event.idempotencyKey)
                ps.executeUpdate()
            }
            event
        }
    }

    override suspend fun listAuditEvents(tenantId: String, projectId: String, entityId: String?, entityType: String?): List<BusinessFinancialAdjustmentAuditEvent> {
        return transactionManager.inTransaction(TenantContext(projectId)) { tx ->
            val conditions = mutableListOf("tenant_id = ?", "project_id = ?")
            val params = mutableListOf<Any>(tenantId, projectId)

            if (entityId != null) {
                conditions.add("entity_id = ?")
                params.add(entityId)
            }
            if (entityType != null) {
                conditions.add("entity_type = ?")
                params.add(entityType)
            }

            val sql = "SELECT * FROM business_financial_adjustment_audit_events WHERE ${conditions.joinToString(" AND ")} ORDER BY timestamp ASC"
            tx.connection.prepareStatement(sql).use { ps ->
                params.forEachIndexed { idx, value -> ps.setObject(idx + 1, value) }
                ps.executeQuery().use { rs ->
                    val list = mutableListOf<BusinessFinancialAdjustmentAuditEvent>()
                    while (rs.next()) {
                        list.add(mapAuditEvent(rs))
                    }
                    list
                }
            }
        }
    }

    // --- Row Mappers ---

    private fun mapAdjustment(rs: ResultSet): BusinessFinancialAdjustment {
        return BusinessFinancialAdjustment(
            id = rs.getString("id"),
            tenantId = rs.getString("tenant_id"),
            projectId = rs.getString("project_id"),
            adjustmentNumber = rs.getString("adjustment_number"),
            adjustmentType = BusinessFinancialAdjustmentType.valueOf(rs.getString("adjustment_type")),
            sourceType = AdjustmentSourceType.valueOf(rs.getString("source_type")),
            sourceId = rs.getString("source_id"),
            originalTransactionId = rs.getString("original_transaction_id"),
            originalAmount = rs.getBigDecimal("original_amount"),
            adjustmentAmount = rs.getBigDecimal("adjustment_amount"),
            effectiveAmount = rs.getBigDecimal("effective_amount"),
            currency = rs.getString("currency"),
            reason = rs.getString("reason"),
            justification = rs.getString("justification"),
            status = AdjustmentStatus.valueOf(rs.getString("status")),
            periodId = rs.getString("period_id"),
            costCenterId = rs.getString("cost_center_id"),
            jobId = rs.getString("job_id"),
            customerId = rs.getString("customer_id"),
            vendorId = rs.getString("vendor_id"),
            createdBy = rs.getString("created_by"),
            reviewedBy = rs.getString("reviewed_by"),
            approvedBy = rs.getString("approved_by"),
            postedBy = rs.getString("posted_by"),
            cancelledBy = rs.getString("cancelled_by"),
            rejectedBy = rs.getString("rejected_by"),
            reversalRequestedBy = rs.getString("reversal_requested_by"),
            reversalApprovedBy = rs.getString("reversal_approved_by"),
            reviewedAt = getNullableLong(rs, "reviewed_at"),
            approvedAt = getNullableLong(rs, "approved_at"),
            postedAt = getNullableLong(rs, "posted_at"),
            reversalRequestedAt = getNullableLong(rs, "reversal_requested_at"),
            reversalApprovedAt = getNullableLong(rs, "reversal_approved_at"),
            reversedAt = getNullableLong(rs, "reversed_at"),
            ledgerPostingId = rs.getString("ledger_posting_id"),
            reversingPostingId = rs.getString("reversing_posting_id"),
            idempotencyKey = rs.getString("idempotency_key"),
            createdAt = rs.getLong("created_at"),
            updatedAt = rs.getLong("updated_at")
        )
    }

    private fun mapRefund(rs: ResultSet): BusinessFinancialRefund {
        return BusinessFinancialRefund(
            id = rs.getString("id"),
            tenantId = rs.getString("tenant_id"),
            projectId = rs.getString("project_id"),
            refundNumber = rs.getString("refund_number"),
            sourceType = AdjustmentSourceType.valueOf(rs.getString("source_type")),
            sourceId = rs.getString("source_id"),
            customerId = rs.getString("customer_id"),
            vendorId = rs.getString("vendor_id"),
            originalTransactionId = rs.getString("original_transaction_id"),
            eligibleBalance = rs.getBigDecimal("eligible_balance"),
            requestedAmount = rs.getBigDecimal("requested_amount"),
            approvedAmount = rs.getBigDecimal("approved_amount"),
            currency = rs.getString("currency"),
            refundReason = rs.getString("refund_reason"),
            paymentMethod = rs.getString("payment_method"),
            status = RefundStatus.valueOf(rs.getString("status")),
            periodId = rs.getString("period_id"),
            requestedBy = rs.getString("requested_by"),
            approvedBy = rs.getString("approved_by"),
            postedBy = rs.getString("posted_by"),
            approvedAt = getNullableLong(rs, "approved_at"),
            postedAt = getNullableLong(rs, "posted_at"),
            settledAt = getNullableLong(rs, "settled_at"),
            ledgerPostingId = rs.getString("ledger_posting_id"),
            idempotencyKey = rs.getString("idempotency_key"),
            createdAt = rs.getLong("created_at"),
            updatedAt = rs.getLong("updated_at")
        )
    }

    private fun mapWriteOff(rs: ResultSet): BusinessFinancialWriteOff {
        return BusinessFinancialWriteOff(
            id = rs.getString("id"),
            tenantId = rs.getString("tenant_id"),
            projectId = rs.getString("project_id"),
            writeOffNumber = rs.getString("write_off_number"),
            sourceType = AdjustmentSourceType.valueOf(rs.getString("source_type")),
            sourceId = rs.getString("source_id"),
            writeOffType = BusinessFinancialWriteOffType.valueOf(rs.getString("write_off_type")),
            eligibleBalance = rs.getBigDecimal("eligible_balance"),
            amount = rs.getBigDecimal("amount"),
            currency = rs.getString("currency"),
            reason = rs.getString("reason"),
            justification = rs.getString("justification"),
            status = WriteOffStatus.valueOf(rs.getString("status")),
            periodId = rs.getString("period_id"),
            customerId = rs.getString("customer_id"),
            vendorId = rs.getString("vendor_id"),
            requestedBy = rs.getString("requested_by"),
            approvedBy = rs.getString("approved_by"),
            postedBy = rs.getString("posted_by"),
            approvedAt = getNullableLong(rs, "approved_at"),
            postedAt = getNullableLong(rs, "posted_at"),
            ledgerPostingId = rs.getString("ledger_posting_id"),
            idempotencyKey = rs.getString("idempotency_key"),
            createdAt = rs.getLong("created_at"),
            updatedAt = rs.getLong("updated_at")
        )
    }

    private fun mapPosting(rs: ResultSet): BusinessFinancialAdjustmentPosting {
        return BusinessFinancialAdjustmentPosting(
            id = rs.getString("id"),
            tenantId = rs.getString("tenant_id"),
            projectId = rs.getString("project_id"),
            adjustmentId = rs.getString("adjustment_id"),
            postingNumber = rs.getString("posting_number"),
            ledgerPostingId = rs.getString("ledger_posting_id"),
            postingType = AdjustmentPostingType.valueOf(rs.getString("posting_type")),
            debitAccount = rs.getString("debit_account"),
            creditAccount = rs.getString("credit_account"),
            amount = rs.getBigDecimal("amount"),
            currency = rs.getString("currency"),
            status = rs.getString("status"),
            postedBy = rs.getString("posted_by"),
            postedAt = rs.getLong("posted_at"),
            idempotencyKey = rs.getString("idempotency_key"),
            createdAt = rs.getLong("created_at")
        )
    }

    private fun mapAuditEvent(rs: ResultSet): BusinessFinancialAdjustmentAuditEvent {
        return BusinessFinancialAdjustmentAuditEvent(
            id = rs.getString("id"),
            tenantId = rs.getString("tenant_id"),
            projectId = rs.getString("project_id"),
            entityType = rs.getString("entity_type"),
            entityId = rs.getString("entity_id"),
            eventType = rs.getString("event_type"),
            actorId = rs.getString("actor_id"),
            actorRole = rs.getString("actor_role"),
            timestamp = rs.getLong("timestamp"),
            previousStatus = rs.getString("previous_status"),
            newStatus = rs.getString("new_status"),
            reason = rs.getString("reason"),
            metadataJson = rs.getString("metadata_json"),
            correlationId = rs.getString("correlation_id"),
            idempotencyKey = rs.getString("idempotency_key")
        )
    }

    private fun setNullableLong(ps: java.sql.PreparedStatement, idx: Int, value: Long?) {
        if (value != null) ps.setLong(idx, value) else ps.setNull(idx, java.sql.Types.BIGINT)
    }

    private fun getNullableLong(rs: ResultSet, col: String): Long? {
        val v = rs.getLong(col)
        return if (rs.wasNull()) null else v
    }
}
