package com.sucharu.sucharupro.data.persistence.postgres

import com.sucharu.sucharupro.data.datasource.vendorpayable.VendorPayableDataSource
import com.sucharu.sucharupro.domain.model.vendorpayable.*
import java.math.BigDecimal
import java.sql.ResultSet
import java.text.SimpleDateFormat
import java.util.*

/**
 * PostgreSQL JDBC implementation of VendorPayableDataSource (Module 15 Step 02).
 */
class PostgresVendorPayableDataSource(
    private val transactionManager: TransactionManager,
    private val defaultTenantId: String = "TENANT-001"
) : VendorPayableDataSource {

    private fun mapResultSetToPayable(rs: ResultSet): VendorPayable {
        return VendorPayable(
            payableId = rs.getString("payable_id"),
            tenantId = rs.getString("tenant_id"),
            projectId = rs.getString("project_id"),
            payableNumber = rs.getString("payable_number"),
            vendorId = rs.getString("vendor_id"),
            jobId = rs.getString("job_id"),
            vendorJobId = rs.getString("vendor_job_id"),
            billReference = rs.getString("bill_reference"),
            description = rs.getString("description"),
            notes = rs.getString("notes"),
            originalAmount = rs.getBigDecimal("original_amount"),
            paidAmount = rs.getBigDecimal("paid_amount"),
            currency = rs.getString("currency"),
            issueDate = rs.getLong("issue_date"),
            paymentTerms = VendorPayablePaymentTerms.valueOf(rs.getString("payment_terms")),
            customTermDays = rs.getObject("custom_term_days") as? Int,
            dueDate = rs.getLong("due_date"),
            status = VendorPayableStatus.valueOf(rs.getString("status")),
            attachmentUrl = rs.getString("attachment_url"),
            idempotencyKey = rs.getString("idempotency_key"),
            createdBy = rs.getString("created_by"),
            createdAt = rs.getLong("created_at"),
            submittedBy = rs.getString("submitted_by"),
            submittedAt = rs.getObject("submitted_at")?.let { rs.getLong("submitted_at") },
            approvedBy = rs.getString("approved_by"),
            approvedAt = rs.getObject("approved_at")?.let { rs.getLong("approved_at") },
            rejectedBy = rs.getString("rejected_by"),
            rejectedAt = rs.getObject("rejected_at")?.let { rs.getLong("rejected_at") },
            recheckRequestedBy = rs.getString("recheck_requested_by"),
            recheckRequestedAt = rs.getObject("recheck_requested_at")?.let { rs.getLong("recheck_requested_at") },
            cancelledBy = rs.getString("cancelled_by"),
            cancelledAt = rs.getObject("cancelled_at")?.let { rs.getLong("cancelled_at") },
            voidedBy = rs.getString("voided_by"),
            voidedAt = rs.getObject("voided_at")?.let { rs.getLong("voided_at") },
            rejectionReason = rs.getString("rejection_reason"),
            cancellationReason = rs.getString("cancellation_reason"),
            voidReason = rs.getString("void_reason"),
            updatedAt = rs.getLong("updated_at"),
            updatedBy = rs.getString("updated_by"),
            version = rs.getLong("version")
        )
    }

    private fun mapResultSetToAllocation(rs: ResultSet): VendorPayablePaymentAllocation {
        return VendorPayablePaymentAllocation(
            allocationId = rs.getString("allocation_id"),
            tenantId = rs.getString("tenant_id"),
            projectId = rs.getString("project_id"),
            payableId = rs.getString("payable_id"),
            vendorId = rs.getString("vendor_id"),
            amount = rs.getBigDecimal("amount"),
            currency = rs.getString("currency"),
            paymentMethod = VendorPayablePaymentMethod.valueOf(rs.getString("payment_method")),
            paymentReference = rs.getString("payment_reference"),
            paymentDate = rs.getLong("payment_date"),
            notes = rs.getString("notes"),
            allocatedBy = rs.getString("allocated_by"),
            allocatedAt = rs.getLong("allocated_at"),
            idempotencyKey = rs.getString("idempotency_key"),
            version = rs.getLong("version")
        )
    }

    private fun mapResultSetToAuditEvent(rs: ResultSet): VendorPayableAuditEvent {
        return VendorPayableAuditEvent(
            eventId = rs.getString("event_id"),
            tenantId = rs.getString("tenant_id"),
            projectId = rs.getString("project_id"),
            payableId = rs.getString("payable_id"),
            vendorId = rs.getString("vendor_id"),
            eventType = rs.getString("event_type"),
            actorId = rs.getString("actor_id"),
            actorRole = rs.getString("actor_role"),
            timestamp = rs.getLong("timestamp"),
            previousStatus = rs.getString("previous_status")?.let { VendorPayableStatus.valueOf(it) },
            newStatus = rs.getString("new_status")?.let { VendorPayableStatus.valueOf(it) },
            amount = rs.getBigDecimal("amount"),
            reason = rs.getString("reason"),
            correlationId = rs.getString("correlation_id"),
            idempotencyKey = rs.getString("idempotency_key"),
            metadataJson = rs.getString("metadata_json")
        )
    }

    override suspend fun insertPayable(payable: VendorPayable): Boolean {
        val sql = """
            INSERT INTO vendor_payables (
                payable_id, tenant_id, project_id, payable_number, vendor_id,
                job_id, vendor_job_id, bill_reference, description, notes,
                original_amount, paid_amount, currency, issue_date, payment_terms,
                custom_term_days, due_date, status, attachment_url, idempotency_key,
                created_by, created_at, submitted_by, submitted_at, approved_by,
                approved_at, rejected_by, rejected_at, recheck_requested_by, recheck_requested_at,
                cancelled_by, cancelled_at, voided_by, voided_at, rejection_reason,
                cancellation_reason, void_reason, updated_at, updated_by, version
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """.trimIndent()

        transactionManager.inTransaction(TenantContext(payable.projectId)) { tx ->
            tx.connection.prepareStatement(sql).use { stmt ->
                stmt.setString(1, payable.payableId)
                stmt.setString(2, payable.tenantId)
                stmt.setString(3, payable.projectId)
                stmt.setString(4, payable.payableNumber)
                stmt.setString(5, payable.vendorId)
                stmt.setString(6, payable.jobId)
                stmt.setString(7, payable.vendorJobId)
                stmt.setString(8, payable.billReference)
                stmt.setString(9, payable.description)
                stmt.setString(10, payable.notes)
                stmt.setBigDecimal(11, payable.originalAmount)
                stmt.setBigDecimal(12, payable.paidAmount)
                stmt.setString(13, payable.currency)
                stmt.setLong(14, payable.issueDate)
                stmt.setString(15, payable.paymentTerms.name)
                if (payable.customTermDays != null) stmt.setInt(16, payable.customTermDays) else stmt.setNull(16, java.sql.Types.INTEGER)
                stmt.setLong(17, payable.dueDate)
                stmt.setString(18, payable.status.name)
                stmt.setString(19, payable.attachmentUrl)
                stmt.setString(20, payable.idempotencyKey)
                stmt.setString(21, payable.createdBy)
                stmt.setLong(22, payable.createdAt)
                stmt.setString(23, payable.submittedBy)
                if (payable.submittedAt != null) stmt.setLong(24, payable.submittedAt) else stmt.setNull(24, java.sql.Types.BIGINT)
                stmt.setString(25, payable.approvedBy)
                if (payable.approvedAt != null) stmt.setLong(26, payable.approvedAt) else stmt.setNull(26, java.sql.Types.BIGINT)
                stmt.setString(27, payable.rejectedBy)
                if (payable.rejectedAt != null) stmt.setLong(28, payable.rejectedAt) else stmt.setNull(28, java.sql.Types.BIGINT)
                stmt.setString(29, payable.recheckRequestedBy)
                if (payable.recheckRequestedAt != null) stmt.setLong(30, payable.recheckRequestedAt) else stmt.setNull(30, java.sql.Types.BIGINT)
                stmt.setString(31, payable.cancelledBy)
                if (payable.cancelledAt != null) stmt.setLong(32, payable.cancelledAt) else stmt.setNull(32, java.sql.Types.BIGINT)
                stmt.setString(33, payable.voidedBy)
                if (payable.voidedAt != null) stmt.setLong(34, payable.voidedAt) else stmt.setNull(34, java.sql.Types.BIGINT)
                stmt.setString(35, payable.rejectionReason)
                stmt.setString(36, payable.cancellationReason)
                stmt.setString(37, payable.voidReason)
                stmt.setLong(38, payable.updatedAt)
                stmt.setString(39, payable.updatedBy)
                stmt.setLong(40, payable.version)
                stmt.executeUpdate()
            }
        }
        return true
    }

    override suspend fun updatePayable(payable: VendorPayable): Boolean {
        val sql = """
            UPDATE vendor_payables SET
                vendor_id = ?, job_id = ?, vendor_job_id = ?, bill_reference = ?,
                description = ?, notes = ?, original_amount = ?, paid_amount = ?,
                currency = ?, issue_date = ?, payment_terms = ?, custom_term_days = ?,
                due_date = ?, status = ?, attachment_url = ?, submitted_by = ?,
                submitted_at = ?, approved_by = ?, approved_at = ?, rejected_by = ?,
                rejected_at = ?, recheck_requested_by = ?, recheck_requested_at = ?,
                cancelled_by = ?, cancelled_at = ?, voided_by = ?, voided_at = ?,
                rejection_reason = ?, cancellation_reason = ?, void_reason = ?,
                updated_at = ?, updated_by = ?, version = version + 1
            WHERE payable_id = ? AND tenant_id = ? AND project_id = ?
        """.trimIndent()

        transactionManager.inTransaction(TenantContext(payable.projectId)) { tx ->
            tx.connection.prepareStatement(sql).use { stmt ->
                stmt.setString(1, payable.vendorId)
                stmt.setString(2, payable.jobId)
                stmt.setString(3, payable.vendorJobId)
                stmt.setString(4, payable.billReference)
                stmt.setString(5, payable.description)
                stmt.setString(6, payable.notes)
                stmt.setBigDecimal(7, payable.originalAmount)
                stmt.setBigDecimal(8, payable.paidAmount)
                stmt.setString(9, payable.currency)
                stmt.setLong(10, payable.issueDate)
                stmt.setString(11, payable.paymentTerms.name)
                if (payable.customTermDays != null) stmt.setInt(12, payable.customTermDays) else stmt.setNull(12, java.sql.Types.INTEGER)
                stmt.setLong(13, payable.dueDate)
                stmt.setString(14, payable.status.name)
                stmt.setString(15, payable.attachmentUrl)
                stmt.setString(16, payable.submittedBy)
                if (payable.submittedAt != null) stmt.setLong(17, payable.submittedAt) else stmt.setNull(17, java.sql.Types.BIGINT)
                stmt.setString(18, payable.approvedBy)
                if (payable.approvedAt != null) stmt.setLong(19, payable.approvedAt) else stmt.setNull(19, java.sql.Types.BIGINT)
                stmt.setString(20, payable.rejectedBy)
                if (payable.rejectedAt != null) stmt.setLong(21, payable.rejectedAt) else stmt.setNull(21, java.sql.Types.BIGINT)
                stmt.setString(22, payable.recheckRequestedBy)
                if (payable.recheckRequestedAt != null) stmt.setLong(23, payable.recheckRequestedAt) else stmt.setNull(23, java.sql.Types.BIGINT)
                stmt.setString(24, payable.cancelledBy)
                if (payable.cancelledAt != null) stmt.setLong(25, payable.cancelledAt) else stmt.setNull(25, java.sql.Types.BIGINT)
                stmt.setString(26, payable.voidedBy)
                if (payable.voidedAt != null) stmt.setLong(27, payable.voidedAt) else stmt.setNull(27, java.sql.Types.BIGINT)
                stmt.setString(28, payable.rejectionReason)
                stmt.setString(29, payable.cancellationReason)
                stmt.setString(30, payable.voidReason)
                stmt.setLong(31, payable.updatedAt)
                stmt.setString(32, payable.updatedBy)
                stmt.setString(33, payable.payableId)
                stmt.setString(34, payable.tenantId)
                stmt.setString(35, payable.projectId)
                stmt.executeUpdate()
            }
        }
        return true
    }

    override suspend fun getPayableById(tenantId: String, projectId: String, payableId: String): VendorPayable? {
        val sql = "SELECT * FROM vendor_payables WHERE payable_id = ? AND tenant_id = ? AND project_id = ?"
        return transactionManager.inReadOnly(TenantContext(projectId)) { tx ->
            tx.connection.prepareStatement(sql).use { stmt ->
                stmt.setString(1, payableId)
                stmt.setString(2, tenantId)
                stmt.setString(3, projectId)
                stmt.executeQuery().use { rs ->
                    if (rs.next()) mapResultSetToPayable(rs) else null
                }
            }
        }
    }

    override suspend fun getPayableByNumber(tenantId: String, projectId: String, payableNumber: String): VendorPayable? {
        val sql = "SELECT * FROM vendor_payables WHERE payable_number = ? AND tenant_id = ? AND project_id = ?"
        return transactionManager.inReadOnly(TenantContext(projectId)) { tx ->
            tx.connection.prepareStatement(sql).use { stmt ->
                stmt.setString(1, payableNumber)
                stmt.setString(2, tenantId)
                stmt.setString(3, projectId)
                stmt.executeQuery().use { rs ->
                    if (rs.next()) mapResultSetToPayable(rs) else null
                }
            }
        }
    }

    override suspend fun getPayableByIdempotencyKey(
        tenantId: String,
        projectId: String,
        idempotencyKey: String
    ): VendorPayable? {
        val sql = "SELECT * FROM vendor_payables WHERE idempotency_key = ? AND tenant_id = ? AND project_id = ?"
        return transactionManager.inReadOnly(TenantContext(projectId)) { tx ->
            tx.connection.prepareStatement(sql).use { stmt ->
                stmt.setString(1, idempotencyKey)
                stmt.setString(2, tenantId)
                stmt.setString(3, projectId)
                stmt.executeQuery().use { rs ->
                    if (rs.next()) mapResultSetToPayable(rs) else null
                }
            }
        }
    }

    override suspend fun listPayables(
        tenantId: String,
        projectId: String,
        vendorId: String?,
        status: VendorPayableStatus?,
        jobId: String?,
        isOverdueOnly: Boolean,
        fromDate: Long?,
        toDate: Long?,
        limit: Int,
        offset: Int
    ): List<VendorPayable> {
        val conditions = mutableListOf("tenant_id = ?", "project_id = ?")
        val params = mutableListOf<Any>(tenantId, projectId)

        if (!vendorId.isNullOrBlank()) {
            conditions.add("vendor_id = ?")
            params.add(vendorId)
        }
        if (status != null) {
            conditions.add("status = ?")
            params.add(status.name)
        }
        if (!jobId.isNullOrBlank()) {
            conditions.add("job_id = ?")
            params.add(jobId)
        }
        if (isOverdueOnly) {
            conditions.add("due_date < ? AND status IN ('APPROVED', 'PARTIALLY_PAID')")
            params.add(System.currentTimeMillis())
        }
        if (fromDate != null) {
            conditions.add("issue_date >= ?")
            params.add(fromDate)
        }
        if (toDate != null) {
            conditions.add("issue_date <= ?")
            params.add(toDate)
        }

        val sql = """
            SELECT * FROM vendor_payables
            WHERE ${conditions.joinToString(" AND ")}
            ORDER BY issue_date DESC
            LIMIT ? OFFSET ?
        """.trimIndent()

        return transactionManager.inReadOnly(TenantContext(projectId)) { tx ->
            tx.connection.prepareStatement(sql).use { stmt ->
                var idx = 1
                params.forEach { param ->
                    when (param) {
                        is String -> stmt.setString(idx++, param)
                        is Long -> stmt.setLong(idx++, param)
                        is Int -> stmt.setInt(idx++, param)
                        else -> stmt.setObject(idx++, param)
                    }
                }
                stmt.setInt(idx++, limit)
                stmt.setInt(idx, offset)

                stmt.executeQuery().use { rs ->
                    val result = mutableListOf<VendorPayable>()
                    while (rs.next()) {
                        result.add(mapResultSetToPayable(rs))
                    }
                    result
                }
            }
        }
    }

    override suspend fun countPayables(
        tenantId: String,
        projectId: String,
        vendorId: String?,
        status: VendorPayableStatus?,
        jobId: String?,
        isOverdueOnly: Boolean,
        fromDate: Long?,
        toDate: Long?
    ): Long {
        val conditions = mutableListOf("tenant_id = ?", "project_id = ?")
        val params = mutableListOf<Any>(tenantId, projectId)

        if (!vendorId.isNullOrBlank()) {
            conditions.add("vendor_id = ?")
            params.add(vendorId)
        }
        if (status != null) {
            conditions.add("status = ?")
            params.add(status.name)
        }
        if (!jobId.isNullOrBlank()) {
            conditions.add("job_id = ?")
            params.add(jobId)
        }
        if (isOverdueOnly) {
            conditions.add("due_date < ? AND status IN ('APPROVED', 'PARTIALLY_PAID')")
            params.add(System.currentTimeMillis())
        }
        if (fromDate != null) {
            conditions.add("issue_date >= ?")
            params.add(fromDate)
        }
        if (toDate != null) {
            conditions.add("issue_date <= ?")
            params.add(toDate)
        }

        val sql = "SELECT COUNT(*) FROM vendor_payables WHERE ${conditions.joinToString(" AND ")}"

        return transactionManager.inReadOnly(TenantContext(projectId)) { tx ->
            tx.connection.prepareStatement(sql).use { stmt ->
                var idx = 1
                params.forEach { param ->
                    when (param) {
                        is String -> stmt.setString(idx++, param)
                        is Long -> stmt.setLong(idx++, param)
                        is Int -> stmt.setInt(idx++, param)
                        else -> stmt.setObject(idx++, param)
                    }
                }
                stmt.executeQuery().use { rs ->
                    if (rs.next()) rs.getLong(1) else 0L
                }
            }
        }
    }

    override suspend fun generateNextPayableNumber(tenantId: String, projectId: String): String {
        val dateStr = SimpleDateFormat("yyyyMMdd", Locale.US).format(Date())
        val prefix = "PAYABLE-$dateStr-"

        val sql = """
            SELECT payable_number FROM vendor_payables
            WHERE tenant_id = ? AND project_id = ? AND payable_number LIKE ?
            ORDER BY payable_number DESC LIMIT 1
        """.trimIndent()

        return transactionManager.inReadOnly(TenantContext(projectId)) { tx ->
            tx.connection.prepareStatement(sql).use { stmt ->
                stmt.setString(1, tenantId)
                stmt.setString(2, projectId)
                stmt.setString(3, "$prefix%")
                stmt.executeQuery().use { rs ->
                    if (rs.next()) {
                        val lastNum = rs.getString("payable_number")
                        val lastSeq = lastNum.removePrefix(prefix).toLongOrNull() ?: 1000L
                        val nextSeq = lastSeq + 1
                        "$prefix${nextSeq.toString().padStart(4, '0')}"
                    } else {
                        "${prefix}1001"
                    }
                }
            }
        }
    }

    override suspend fun insertPaymentAllocation(allocation: VendorPayablePaymentAllocation): Boolean {
        val sql = """
            INSERT INTO vendor_payable_payment_allocations (
                allocation_id, tenant_id, project_id, payable_id, vendor_id,
                amount, currency, payment_method, payment_reference, payment_date,
                notes, allocated_by, allocated_at, idempotency_key, version
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """.trimIndent()

        transactionManager.inTransaction(TenantContext(allocation.projectId)) { tx ->
            tx.connection.prepareStatement(sql).use { stmt ->
                stmt.setString(1, allocation.allocationId)
                stmt.setString(2, allocation.tenantId)
                stmt.setString(3, allocation.projectId)
                stmt.setString(4, allocation.payableId)
                stmt.setString(5, allocation.vendorId)
                stmt.setBigDecimal(6, allocation.amount)
                stmt.setString(7, allocation.currency)
                stmt.setString(8, allocation.paymentMethod.name)
                stmt.setString(9, allocation.paymentReference)
                stmt.setLong(10, allocation.paymentDate)
                stmt.setString(11, allocation.notes)
                stmt.setString(12, allocation.allocatedBy)
                stmt.setLong(13, allocation.allocatedAt)
                stmt.setString(14, allocation.idempotencyKey)
                stmt.setLong(15, allocation.version)
                stmt.executeUpdate()
            }
        }
        return true
    }

    override suspend fun getAllocationsForPayable(
        tenantId: String,
        projectId: String,
        payableId: String
    ): List<VendorPayablePaymentAllocation> {
        val sql = """
            SELECT * FROM vendor_payable_payment_allocations
            WHERE tenant_id = ? AND project_id = ? AND payable_id = ?
            ORDER BY allocated_at ASC
        """.trimIndent()

        return transactionManager.inReadOnly(TenantContext(projectId)) { tx ->
            tx.connection.prepareStatement(sql).use { stmt ->
                stmt.setString(1, tenantId)
                stmt.setString(2, projectId)
                stmt.setString(3, payableId)
                stmt.executeQuery().use { rs ->
                    val list = mutableListOf<VendorPayablePaymentAllocation>()
                    while (rs.next()) {
                        list.add(mapResultSetToAllocation(rs))
                    }
                    list
                }
            }
        }
    }

    override suspend fun getAllocationByIdempotencyKey(
        tenantId: String,
        idempotencyKey: String
    ): VendorPayablePaymentAllocation? {
        val sql = """
            SELECT * FROM vendor_payable_payment_allocations
            WHERE tenant_id = ? AND idempotency_key = ?
        """.trimIndent()

        return transactionManager.inReadOnly(TenantContext(defaultTenantId)) { tx ->
            tx.connection.prepareStatement(sql).use { stmt ->
                stmt.setString(1, tenantId)
                stmt.setString(2, idempotencyKey)
                stmt.executeQuery().use { rs ->
                    if (rs.next()) mapResultSetToAllocation(rs) else null
                }
            }
        }
    }

    override suspend fun insertAuditEvent(event: VendorPayableAuditEvent): Boolean {
        val sql = """
            INSERT INTO vendor_payable_audit_events (
                event_id, tenant_id, project_id, payable_id, vendor_id,
                event_type, actor_id, actor_role, timestamp, previous_status,
                new_status, amount, reason, correlation_id, idempotency_key, metadata_json
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """.trimIndent()

        transactionManager.inTransaction(TenantContext(event.projectId)) { tx ->
            tx.connection.prepareStatement(sql).use { stmt ->
                stmt.setString(1, event.eventId)
                stmt.setString(2, event.tenantId)
                stmt.setString(3, event.projectId)
                stmt.setString(4, event.payableId)
                stmt.setString(5, event.vendorId)
                stmt.setString(6, event.eventType)
                stmt.setString(7, event.actorId)
                stmt.setString(8, event.actorRole)
                stmt.setLong(9, event.timestamp)
                stmt.setString(10, event.previousStatus?.name)
                stmt.setString(11, event.newStatus?.name)
                if (event.amount != null) stmt.setBigDecimal(12, event.amount) else stmt.setNull(12, java.sql.Types.NUMERIC)
                stmt.setString(13, event.reason)
                stmt.setString(14, event.correlationId)
                stmt.setString(15, event.idempotencyKey)
                stmt.setString(16, event.metadataJson)
                stmt.executeUpdate()
            }
        }
        return true
    }

    override suspend fun getAuditEvents(
        tenantId: String,
        projectId: String,
        payableId: String
    ): List<VendorPayableAuditEvent> {
        val sql = """
            SELECT * FROM vendor_payable_audit_events
            WHERE tenant_id = ? AND project_id = ? AND payable_id = ?
            ORDER BY timestamp ASC
        """.trimIndent()

        return transactionManager.inReadOnly(TenantContext(projectId)) { tx ->
            tx.connection.prepareStatement(sql).use { stmt ->
                stmt.setString(1, tenantId)
                stmt.setString(2, projectId)
                stmt.setString(3, payableId)
                stmt.executeQuery().use { rs ->
                    val list = mutableListOf<VendorPayableAuditEvent>()
                    while (rs.next()) {
                        list.add(mapResultSetToAuditEvent(rs))
                    }
                    list
                }
            }
        }
    }
}
