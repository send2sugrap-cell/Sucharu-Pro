package com.sucharu.sucharupro.data.persistence.postgres

import com.sucharu.sucharupro.data.datasource.businessexpense.BusinessExpenseDataSource
import com.sucharu.sucharupro.domain.model.businessexpense.BusinessExpense
import com.sucharu.sucharupro.domain.model.businessexpense.BusinessExpenseAuditEvent
import com.sucharu.sucharupro.domain.model.businessexpense.BusinessExpenseCategory
import com.sucharu.sucharupro.domain.model.businessexpense.BusinessExpensePaymentMethod
import com.sucharu.sucharupro.domain.model.businessexpense.BusinessExpenseStatus
import java.sql.ResultSet
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Production-grade PostgreSQL Data Source for Business Expenses, Categories, and Audit Events (Module 15 Step 01).
 */
class PostgresBusinessExpenseDataSource(
    private val transactionManager: TransactionManager,
    private val defaultTenantId: String = "TENANT-001"
) : BusinessExpenseDataSource {

    private fun mapRowToExpense(rs: ResultSet): BusinessExpense {
        return BusinessExpense(
            expenseId = rs.getString("expense_id"),
            tenantId = rs.getString("tenant_id"),
            projectId = rs.getString("project_id"),
            branchId = rs.getString("branch_id"),
            locationId = rs.getString("location_id"),
            expenseNumber = rs.getString("expense_number"),
            expenseCategoryId = rs.getString("expense_category_id"),
            amount = rs.getBigDecimal("amount"),
            currency = rs.getString("currency"),
            expenseDate = rs.getLong("expense_date"),
            paymentMethod = BusinessExpensePaymentMethod.valueOf(rs.getString("payment_method")),
            paymentReference = rs.getString("payment_reference"),
            status = BusinessExpenseStatus.valueOf(rs.getString("status")),
            vendorId = rs.getString("vendor_id"),
            jobId = rs.getString("job_id"),
            description = rs.getString("description"),
            notes = rs.getString("notes"),
            attachmentUrl = rs.getString("attachment_url"),
            attachmentMetadata = rs.getString("attachment_metadata"),
            idempotencyKey = rs.getString("idempotency_key"),
            createdBy = rs.getString("created_by"),
            createdAt = rs.getLong("created_at"),
            submittedBy = rs.getString("submitted_by"),
            submittedAt = rs.getObject("submitted_at")?.let { rs.getLong("submitted_at") },
            approvedBy = rs.getString("approved_by"),
            approvedAt = rs.getObject("approved_at")?.let { rs.getLong("approved_at") },
            rejectedBy = rs.getString("rejected_by"),
            rejectedAt = rs.getObject("rejected_at")?.let { rs.getLong("rejected_at") },
            rejectionReason = rs.getString("rejection_reason"),
            cancelledBy = rs.getString("cancelled_by"),
            cancelledAt = rs.getObject("cancelled_at")?.let { rs.getLong("cancelled_at") },
            cancellationReason = rs.getString("cancellation_reason"),
            updatedAt = rs.getLong("updated_at"),
            updatedBy = rs.getString("updated_by"),
            version = rs.getLong("version")
        )
    }

    private fun mapRowToCategory(rs: ResultSet): BusinessExpenseCategory {
        return BusinessExpenseCategory(
            categoryId = rs.getString("category_id"),
            tenantId = rs.getString("tenant_id"),
            projectId = rs.getString("project_id"),
            name = rs.getString("name"),
            code = rs.getString("code"),
            description = rs.getString("description"),
            isActive = rs.getBoolean("is_active"),
            sortOrder = rs.getInt("sort_order"),
            createdAt = rs.getLong("created_at"),
            updatedAt = rs.getLong("updated_at"),
            version = rs.getLong("version")
        )
    }

    private fun mapRowToAudit(rs: ResultSet): BusinessExpenseAuditEvent {
        return BusinessExpenseAuditEvent(
            eventId = rs.getString("event_id"),
            tenantId = rs.getString("tenant_id"),
            projectId = rs.getString("project_id"),
            expenseId = rs.getString("expense_id"),
            eventType = rs.getString("event_type"),
            actorId = rs.getString("actor_id"),
            actorRole = rs.getString("actor_role"),
            timestamp = rs.getLong("timestamp"),
            correlationId = rs.getString("correlation_id"),
            previousStatus = rs.getString("previous_status")?.let { BusinessExpenseStatus.valueOf(it) },
            newStatus = rs.getString("new_status")?.let { BusinessExpenseStatus.valueOf(it) },
            reason = rs.getString("reason"),
            metadataJson = rs.getString("metadata_json")
        )
    }

    override suspend fun insertExpense(expense: BusinessExpense): Boolean {
        val sql = """
            INSERT INTO business_expenses (
                expense_id, tenant_id, project_id, branch_id, location_id,
                expense_number, expense_category_id, amount, currency, expense_date,
                payment_method, payment_reference, status, vendor_id, job_id,
                description, notes, attachment_url, attachment_metadata, idempotency_key,
                created_by, created_at, submitted_by, submitted_at, approved_by,
                approved_at, rejected_by, rejected_at, rejection_reason, cancelled_by,
                cancelled_at, cancellation_reason, updated_at, updated_by, version
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """.trimIndent()

        transactionManager.inTransaction(TenantContext(expense.projectId)) { tx ->
            tx.connection.prepareStatement(sql).use { stmt ->
                stmt.setString(1, expense.expenseId)
                stmt.setString(2, expense.tenantId)
                stmt.setString(3, expense.projectId)
                stmt.setString(4, expense.branchId)
                stmt.setString(5, expense.locationId)
                stmt.setString(6, expense.expenseNumber)
                stmt.setString(7, expense.expenseCategoryId)
                stmt.setBigDecimal(8, expense.amount)
                stmt.setString(9, expense.currency)
                stmt.setLong(10, expense.expenseDate)
                stmt.setString(11, expense.paymentMethod.name)
                stmt.setString(12, expense.paymentReference)
                stmt.setString(13, expense.status.name)
                stmt.setString(14, expense.vendorId)
                stmt.setString(15, expense.jobId)
                stmt.setString(16, expense.description)
                stmt.setString(17, expense.notes)
                stmt.setString(18, expense.attachmentUrl)
                stmt.setString(19, expense.attachmentMetadata)
                stmt.setString(20, expense.idempotencyKey)
                stmt.setString(21, expense.createdBy)
                stmt.setLong(22, expense.createdAt)
                stmt.setString(23, expense.submittedBy)
                if (expense.submittedAt != null) stmt.setLong(24, expense.submittedAt) else stmt.setNull(24, java.sql.Types.BIGINT)
                stmt.setString(25, expense.approvedBy)
                if (expense.approvedAt != null) stmt.setLong(26, expense.approvedAt) else stmt.setNull(26, java.sql.Types.BIGINT)
                stmt.setString(27, expense.rejectedBy)
                if (expense.rejectedAt != null) stmt.setLong(28, expense.rejectedAt) else stmt.setNull(28, java.sql.Types.BIGINT)
                stmt.setString(29, expense.rejectionReason)
                stmt.setString(30, expense.cancelledBy)
                if (expense.cancelledAt != null) stmt.setLong(31, expense.cancelledAt) else stmt.setNull(31, java.sql.Types.BIGINT)
                stmt.setString(32, expense.cancellationReason)
                stmt.setLong(33, expense.updatedAt)
                stmt.setString(34, expense.updatedBy)
                stmt.setLong(35, expense.version)
                stmt.executeUpdate()
            }
        }
        return true
    }

    override suspend fun updateExpense(expense: BusinessExpense): Boolean {
        val sql = """
            UPDATE business_expenses SET
                branch_id = ?,
                location_id = ?,
                expense_category_id = ?,
                amount = ?,
                currency = ?,
                expense_date = ?,
                payment_method = ?,
                payment_reference = ?,
                status = ?,
                vendor_id = ?,
                job_id = ?,
                description = ?,
                notes = ?,
                attachment_url = ?,
                attachment_metadata = ?,
                submitted_by = ?,
                submitted_at = ?,
                approved_by = ?,
                approved_at = ?,
                rejected_by = ?,
                rejected_at = ?,
                rejection_reason = ?,
                cancelled_by = ?,
                cancelled_at = ?,
                cancellation_reason = ?,
                updated_at = ?,
                updated_by = ?,
                version = version + 1
            WHERE tenant_id = ? AND project_id = ? AND expense_id = ?
        """.trimIndent()

        val rows = transactionManager.inTransaction(TenantContext(expense.projectId)) { tx ->
            tx.connection.prepareStatement(sql).use { stmt ->
                stmt.setString(1, expense.branchId)
                stmt.setString(2, expense.locationId)
                stmt.setString(3, expense.expenseCategoryId)
                stmt.setBigDecimal(4, expense.amount)
                stmt.setString(5, expense.currency)
                stmt.setLong(6, expense.expenseDate)
                stmt.setString(7, expense.paymentMethod.name)
                stmt.setString(8, expense.paymentReference)
                stmt.setString(9, expense.status.name)
                stmt.setString(10, expense.vendorId)
                stmt.setString(11, expense.jobId)
                stmt.setString(12, expense.description)
                stmt.setString(13, expense.notes)
                stmt.setString(14, expense.attachmentUrl)
                stmt.setString(15, expense.attachmentMetadata)
                stmt.setString(16, expense.submittedBy)
                if (expense.submittedAt != null) stmt.setLong(17, expense.submittedAt) else stmt.setNull(17, java.sql.Types.BIGINT)
                stmt.setString(18, expense.approvedBy)
                if (expense.approvedAt != null) stmt.setLong(19, expense.approvedAt) else stmt.setNull(19, java.sql.Types.BIGINT)
                stmt.setString(20, expense.rejectedBy)
                if (expense.rejectedAt != null) stmt.setLong(21, expense.rejectedAt) else stmt.setNull(21, java.sql.Types.BIGINT)
                stmt.setString(22, expense.rejectionReason)
                stmt.setString(23, expense.cancelledBy)
                if (expense.cancelledAt != null) stmt.setLong(24, expense.cancelledAt) else stmt.setNull(24, java.sql.Types.BIGINT)
                stmt.setString(25, expense.cancellationReason)
                stmt.setLong(26, expense.updatedAt)
                stmt.setString(27, expense.updatedBy)
                stmt.setString(28, expense.tenantId)
                stmt.setString(29, expense.projectId)
                stmt.setString(30, expense.expenseId)
                stmt.executeUpdate()
            }
        }
        return rows > 0
    }

    override suspend fun getExpenseById(
        tenantId: String,
        projectId: String,
        expenseId: String
    ): BusinessExpense? {
        val sql = "SELECT * FROM business_expenses WHERE tenant_id = ? AND project_id = ? AND expense_id = ?"
        return transactionManager.inReadOnly(TenantContext(projectId)) { tx ->
            tx.connection.prepareStatement(sql).use { stmt ->
                stmt.setString(1, tenantId)
                stmt.setString(2, projectId)
                stmt.setString(3, expenseId)
                val rs = stmt.executeQuery()
                if (rs.next()) mapRowToExpense(rs) else null
            }
        }
    }

    override suspend fun getExpenseByNumber(
        tenantId: String,
        projectId: String,
        expenseNumber: String
    ): BusinessExpense? {
        val sql = "SELECT * FROM business_expenses WHERE tenant_id = ? AND project_id = ? AND expense_number = ?"
        return transactionManager.inReadOnly(TenantContext(projectId)) { tx ->
            tx.connection.prepareStatement(sql).use { stmt ->
                stmt.setString(1, tenantId)
                stmt.setString(2, projectId)
                stmt.setString(3, expenseNumber)
                val rs = stmt.executeQuery()
                if (rs.next()) mapRowToExpense(rs) else null
            }
        }
    }

    override suspend fun getExpenseByIdempotencyKey(
        tenantId: String,
        projectId: String,
        idempotencyKey: String
    ): BusinessExpense? {
        val sql = "SELECT * FROM business_expenses WHERE tenant_id = ? AND project_id = ? AND idempotency_key = ?"
        return transactionManager.inReadOnly(TenantContext(projectId)) { tx ->
            tx.connection.prepareStatement(sql).use { stmt ->
                stmt.setString(1, tenantId)
                stmt.setString(2, projectId)
                stmt.setString(3, idempotencyKey)
                val rs = stmt.executeQuery()
                if (rs.next()) mapRowToExpense(rs) else null
            }
        }
    }

    override suspend fun listExpenses(
        tenantId: String,
        projectId: String,
        status: BusinessExpenseStatus?,
        categoryId: String?,
        vendorId: String?,
        jobId: String?,
        fromDate: Long?,
        toDate: Long?,
        limit: Int,
        offset: Int
    ): List<BusinessExpense> {
        val conditions = mutableListOf("tenant_id = ?", "project_id = ?")
        val params = mutableListOf<Any>(tenantId, projectId)

        if (status != null) {
            conditions.add("status = ?")
            params.add(status.name)
        }
        if (categoryId != null) {
            conditions.add("expense_category_id = ?")
            params.add(categoryId)
        }
        if (vendorId != null) {
            conditions.add("vendor_id = ?")
            params.add(vendorId)
        }
        if (jobId != null) {
            conditions.add("job_id = ?")
            params.add(jobId)
        }
        if (fromDate != null) {
            conditions.add("expense_date >= ?")
            params.add(fromDate)
        }
        if (toDate != null) {
            conditions.add("expense_date <= ?")
            params.add(toDate)
        }

        val sql = """
            SELECT * FROM business_expenses
            WHERE ${conditions.joinToString(" AND ")}
            ORDER BY expense_date DESC, created_at DESC
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
                val rs = stmt.executeQuery()
                val list = mutableListOf<BusinessExpense>()
                while (rs.next()) {
                    list.add(mapRowToExpense(rs))
                }
                list
            }
        }
    }

    override suspend fun countExpenses(
        tenantId: String,
        projectId: String,
        status: BusinessExpenseStatus?,
        categoryId: String?,
        vendorId: String?,
        jobId: String?,
        fromDate: Long?,
        toDate: Long?
    ): Long {
        val conditions = mutableListOf("tenant_id = ?", "project_id = ?")
        val params = mutableListOf<Any>(tenantId, projectId)

        if (status != null) {
            conditions.add("status = ?")
            params.add(status.name)
        }
        if (categoryId != null) {
            conditions.add("expense_category_id = ?")
            params.add(categoryId)
        }
        if (vendorId != null) {
            conditions.add("vendor_id = ?")
            params.add(vendorId)
        }
        if (jobId != null) {
            conditions.add("job_id = ?")
            params.add(jobId)
        }
        if (fromDate != null) {
            conditions.add("expense_date >= ?")
            params.add(fromDate)
        }
        if (toDate != null) {
            conditions.add("expense_date <= ?")
            params.add(toDate)
        }

        val sql = "SELECT COUNT(*) FROM business_expenses WHERE ${conditions.joinToString(" AND ")}"

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
                val rs = stmt.executeQuery()
                if (rs.next()) rs.getLong(1) else 0L
            }
        }
    }

    override suspend fun generateNextExpenseNumber(tenantId: String, projectId: String): String {
        val datePrefix = SimpleDateFormat("yyyyMMdd", Locale.US).format(Date())
        val searchPattern = "EXP-$datePrefix-%"
        val sql = """
            SELECT COUNT(*) FROM business_expenses
            WHERE tenant_id = ? AND project_id = ? AND expense_number LIKE ?
        """.trimIndent()

        val count = transactionManager.inReadOnly(TenantContext(projectId)) { tx ->
            tx.connection.prepareStatement(sql).use { stmt ->
                stmt.setString(1, tenantId)
                stmt.setString(2, projectId)
                stmt.setString(3, searchPattern)
                val rs = stmt.executeQuery()
                if (rs.next()) rs.getLong(1) else 0L
            }
        }
        val seq = (count + 1).toString().padStart(4, '0')
        return "EXP-$datePrefix-$seq"
    }

    override suspend fun insertCategory(category: BusinessExpenseCategory): Boolean {
        val sql = """
            INSERT INTO business_expense_categories (
                category_id, tenant_id, project_id, name, code, description, is_active, sort_order, created_at, updated_at, version
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """.trimIndent()

        transactionManager.inTransaction(TenantContext(category.projectId)) { tx ->
            tx.connection.prepareStatement(sql).use { stmt ->
                stmt.setString(1, category.categoryId)
                stmt.setString(2, category.tenantId)
                stmt.setString(3, category.projectId)
                stmt.setString(4, category.name)
                stmt.setString(5, category.code)
                stmt.setString(6, category.description)
                stmt.setBoolean(7, category.isActive)
                stmt.setInt(8, category.sortOrder)
                stmt.setLong(9, category.createdAt)
                stmt.setLong(10, category.updatedAt)
                stmt.setLong(11, category.version)
                stmt.executeUpdate()
            }
        }
        return true
    }

    override suspend fun updateCategory(category: BusinessExpenseCategory): Boolean {
        val sql = """
            UPDATE business_expense_categories SET
                name = ?,
                description = ?,
                is_active = ?,
                sort_order = ?,
                updated_at = ?,
                version = version + 1
            WHERE tenant_id = ? AND project_id = ? AND category_id = ?
        """.trimIndent()

        val rows = transactionManager.inTransaction(TenantContext(category.projectId)) { tx ->
            tx.connection.prepareStatement(sql).use { stmt ->
                stmt.setString(1, category.name)
                stmt.setString(2, category.description)
                stmt.setBoolean(3, category.isActive)
                stmt.setInt(4, category.sortOrder)
                stmt.setLong(5, category.updatedAt)
                stmt.setString(6, category.tenantId)
                stmt.setString(7, category.projectId)
                stmt.setString(8, category.categoryId)
                stmt.executeUpdate()
            }
        }
        return rows > 0
    }

    override suspend fun getCategoryById(
        tenantId: String,
        projectId: String,
        categoryId: String
    ): BusinessExpenseCategory? {
        val sql = "SELECT * FROM business_expense_categories WHERE tenant_id = ? AND project_id = ? AND category_id = ?"
        return transactionManager.inReadOnly(TenantContext(projectId)) { tx ->
            tx.connection.prepareStatement(sql).use { stmt ->
                stmt.setString(1, tenantId)
                stmt.setString(2, projectId)
                stmt.setString(3, categoryId)
                val rs = stmt.executeQuery()
                if (rs.next()) mapRowToCategory(rs) else null
            }
        }
    }

    override suspend fun getCategoryByCode(
        tenantId: String,
        projectId: String,
        code: String
    ): BusinessExpenseCategory? {
        val sql = "SELECT * FROM business_expense_categories WHERE tenant_id = ? AND project_id = ? AND LOWER(code) = LOWER(?)"
        return transactionManager.inReadOnly(TenantContext(projectId)) { tx ->
            tx.connection.prepareStatement(sql).use { stmt ->
                stmt.setString(1, tenantId)
                stmt.setString(2, projectId)
                stmt.setString(3, code)
                val rs = stmt.executeQuery()
                if (rs.next()) mapRowToCategory(rs) else null
            }
        }
    }

    override suspend fun listCategories(
        tenantId: String,
        projectId: String,
        activeOnly: Boolean
    ): List<BusinessExpenseCategory> {
        val sql = if (activeOnly) {
            "SELECT * FROM business_expense_categories WHERE tenant_id = ? AND project_id = ? AND is_active = TRUE ORDER BY sort_order ASC, name ASC"
        } else {
            "SELECT * FROM business_expense_categories WHERE tenant_id = ? AND project_id = ? ORDER BY sort_order ASC, name ASC"
        }

        return transactionManager.inReadOnly(TenantContext(projectId)) { tx ->
            tx.connection.prepareStatement(sql).use { stmt ->
                stmt.setString(1, tenantId)
                stmt.setString(2, projectId)
                val rs = stmt.executeQuery()
                val list = mutableListOf<BusinessExpenseCategory>()
                while (rs.next()) {
                    list.add(mapRowToCategory(rs))
                }
                list
            }
        }
    }

    override suspend fun insertAuditEvent(event: BusinessExpenseAuditEvent): Boolean {
        val sql = """
            INSERT INTO business_expense_audit_events (
                event_id, tenant_id, project_id, expense_id, event_type, actor_id, actor_role, timestamp, correlation_id, previous_status, new_status, reason, metadata_json
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """.trimIndent()

        transactionManager.inTransaction(TenantContext(event.projectId)) { tx ->
            tx.connection.prepareStatement(sql).use { stmt ->
                stmt.setString(1, event.eventId)
                stmt.setString(2, event.tenantId)
                stmt.setString(3, event.projectId)
                stmt.setString(4, event.expenseId)
                stmt.setString(5, event.eventType)
                stmt.setString(6, event.actorId)
                stmt.setString(7, event.actorRole)
                stmt.setLong(8, event.timestamp)
                stmt.setString(9, event.correlationId)
                stmt.setString(10, event.previousStatus?.name)
                stmt.setString(11, event.newStatus?.name)
                stmt.setString(12, event.reason)
                stmt.setString(13, event.metadataJson)
                stmt.executeUpdate()
            }
        }
        return true
    }

    override suspend fun getAuditEvents(
        tenantId: String,
        projectId: String,
        expenseId: String
    ): List<BusinessExpenseAuditEvent> {
        val sql = """
            SELECT * FROM business_expense_audit_events
            WHERE tenant_id = ? AND project_id = ? AND expense_id = ?
            ORDER BY timestamp ASC
        """.trimIndent()

        return transactionManager.inReadOnly(TenantContext(projectId)) { tx ->
            tx.connection.prepareStatement(sql).use { stmt ->
                stmt.setString(1, tenantId)
                stmt.setString(2, projectId)
                stmt.setString(3, expenseId)
                val rs = stmt.executeQuery()
                val list = mutableListOf<BusinessExpenseAuditEvent>()
                while (rs.next()) {
                    list.add(mapRowToAudit(rs))
                }
                list
            }
        }
    }
}
