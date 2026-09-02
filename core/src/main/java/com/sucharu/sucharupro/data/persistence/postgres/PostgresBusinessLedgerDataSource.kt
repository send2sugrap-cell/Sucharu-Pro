package com.sucharu.sucharupro.data.persistence.postgres

import com.sucharu.sucharupro.data.datasource.businessledger.BusinessCostAllocationFilter
import com.sucharu.sucharupro.data.datasource.businessledger.BusinessLedgerDataSource
import com.sucharu.sucharupro.data.datasource.businessledger.BusinessLedgerPostingFilter
import com.sucharu.sucharupro.domain.model.businessledger.*
import java.math.BigDecimal
import java.math.RoundingMode
import java.sql.ResultSet

/**
 * PostgreSQL JDBC implementation of [BusinessLedgerDataSource] (Module 15 Step 03).
 */
class PostgresBusinessLedgerDataSource(
    private val transactionManager: TransactionManager,
    private val defaultTenantId: String = "TENANT-001"
) : BusinessLedgerDataSource {

    private fun mapResultSetToPosting(rs: ResultSet): BusinessLedgerPosting {
        return BusinessLedgerPosting(
            id = rs.getString("id"),
            tenantId = rs.getString("tenant_id"),
            projectId = rs.getString("project_id"),
            postingNumber = rs.getString("posting_number"),
            postingType = BusinessLedgerPostingType.valueOf(rs.getString("posting_type")),
            sourceType = BusinessLedgerSourceType.valueOf(rs.getString("source_type")),
            sourceId = rs.getString("source_id"),
            accountCategory = BusinessLedgerAccountCategory.valueOf(rs.getString("account_category")),
            debitAmount = rs.getBigDecimal("debit_amount"),
            creditAmount = rs.getBigDecimal("credit_amount"),
            currency = rs.getString("currency"),
            postingDate = rs.getLong("posting_date"),
            effectiveDate = rs.getLong("effective_date"),
            description = rs.getString("description"),
            reference = rs.getString("reference"),
            jobId = rs.getString("job_id"),
            vendorId = rs.getString("vendor_id"),
            expenseId = rs.getString("expense_id"),
            payableId = rs.getString("payable_id"),
            allocationId = rs.getString("allocation_id"),
            reversalOfPostingId = rs.getString("reversal_of_posting_id"),
            isReversed = rs.getBoolean("is_reversed"),
            reversalReason = rs.getString("reversal_reason"),
            reversedBy = rs.getString("reversed_by"),
            reversedAt = rs.getObject("reversed_at")?.let { rs.getLong("reversed_at") },
            correlationId = rs.getString("correlation_id"),
            idempotencyKey = rs.getString("idempotency_key"),
            checksum = rs.getString("checksum"),
            createdBy = rs.getString("created_by"),
            createdAt = rs.getLong("created_at"),
            version = rs.getLong("version")
        )
    }

    private fun mapResultSetToAllocation(rs: ResultSet): BusinessCostAllocation {
        return BusinessCostAllocation(
            id = rs.getString("id"),
            tenantId = rs.getString("tenant_id"),
            projectId = rs.getString("project_id"),
            allocationNumber = rs.getString("allocation_number"),
            sourceType = BusinessLedgerSourceType.valueOf(rs.getString("source_type")),
            sourceId = rs.getString("source_id"),
            ledgerPostingId = rs.getString("ledger_posting_id"),
            jobId = rs.getString("job_id"),
            vendorId = rs.getString("vendor_id"),
            costCategory = BusinessLedgerAccountCategory.valueOf(rs.getString("cost_category")),
            allocatedAmount = rs.getBigDecimal("allocated_amount"),
            currency = rs.getString("currency"),
            allocationDate = rs.getLong("allocation_date"),
            reason = rs.getString("reason"),
            isReversed = rs.getBoolean("is_reversed"),
            reversalReason = rs.getString("reversal_reason"),
            reversedBy = rs.getString("reversed_by"),
            reversedAt = rs.getObject("reversed_at")?.let { rs.getLong("reversed_at") },
            correlationId = rs.getString("correlation_id"),
            idempotencyKey = rs.getString("idempotency_key"),
            createdBy = rs.getString("created_by"),
            createdAt = rs.getLong("created_at"),
            version = rs.getLong("version")
        )
    }

    private fun mapResultSetToAuditEvent(rs: ResultSet): BusinessLedgerAuditEvent {
        return BusinessLedgerAuditEvent(
            eventId = rs.getString("event_id"),
            tenantId = rs.getString("tenant_id"),
            projectId = rs.getString("project_id"),
            eventType = rs.getString("event_type"),
            actorId = rs.getString("actor_id"),
            actorRole = rs.getString("actor_role"),
            timestamp = rs.getLong("timestamp"),
            sourceType = rs.getString("source_type")?.let { BusinessLedgerSourceType.valueOf(it) },
            sourceId = rs.getString("source_id"),
            postingId = rs.getString("posting_id"),
            allocationId = rs.getString("allocation_id"),
            action = rs.getString("action"),
            previousState = rs.getString("previous_state"),
            newState = rs.getString("new_state"),
            amount = rs.getBigDecimal("amount"),
            reason = rs.getString("reason"),
            correlationId = rs.getString("correlation_id"),
            idempotencyKey = rs.getString("idempotency_key"),
            checksum = rs.getString("checksum"),
            metadataJson = rs.getString("metadata_json")
        )
    }

    override suspend fun createPosting(posting: BusinessLedgerPosting): BusinessLedgerPosting {
        val sql = """
            INSERT INTO business_ledger_postings (
                id, tenant_id, project_id, posting_number, posting_type, source_type, source_id,
                account_category, debit_amount, credit_amount, currency, posting_date, effective_date,
                description, reference, job_id, vendor_id, expense_id, payable_id, allocation_id,
                reversal_of_posting_id, is_reversed, reversal_reason, reversed_by, reversed_at,
                correlation_id, idempotency_key, checksum, created_by, created_at, version
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """.trimIndent()

        transactionManager.inTransaction(TenantContext(posting.projectId)) { tx ->
            tx.connection.prepareStatement(sql).use { ps ->
                ps.setString(1, posting.id)
                ps.setString(2, posting.tenantId)
                ps.setString(3, posting.projectId)
                ps.setString(4, posting.postingNumber)
                ps.setString(5, posting.postingType.name)
                ps.setString(6, posting.sourceType.name)
                ps.setString(7, posting.sourceId)
                ps.setString(8, posting.accountCategory.name)
                ps.setBigDecimal(9, posting.debitAmount)
                ps.setBigDecimal(10, posting.creditAmount)
                ps.setString(11, posting.currency)
                ps.setLong(12, posting.postingDate)
                ps.setLong(13, posting.effectiveDate)
                ps.setString(14, posting.description)
                ps.setString(15, posting.reference)
                ps.setString(16, posting.jobId)
                ps.setString(17, posting.vendorId)
                ps.setString(18, posting.expenseId)
                ps.setString(19, posting.payableId)
                ps.setString(20, posting.allocationId)
                ps.setString(21, posting.reversalOfPostingId)
                ps.setBoolean(22, posting.isReversed)
                ps.setString(23, posting.reversalReason)
                ps.setString(24, posting.reversedBy)
                if (posting.reversedAt != null) ps.setLong(25, posting.reversedAt) else ps.setNull(25, java.sql.Types.BIGINT)
                ps.setString(26, posting.correlationId)
                ps.setString(27, posting.idempotencyKey)
                ps.setString(28, posting.checksum)
                ps.setString(29, posting.createdBy)
                ps.setLong(30, posting.createdAt)
                ps.setLong(31, posting.version)
                ps.executeUpdate()
            }
        }
        return posting
    }

    override suspend fun findPostingById(id: String, tenantId: String, projectId: String): BusinessLedgerPosting? {
        val sql = "SELECT * FROM business_ledger_postings WHERE id = ? AND tenant_id = ? AND project_id = ?"
        return transactionManager.inReadOnly(TenantContext(projectId)) { tx ->
            tx.connection.prepareStatement(sql).use { ps ->
                ps.setString(1, id)
                ps.setString(2, tenantId)
                ps.setString(3, projectId)
                ps.executeQuery().use { rs ->
                    if (rs.next()) mapResultSetToPosting(rs) else null
                }
            }
        }
    }

    override suspend fun findPostingByNumber(postingNumber: String, tenantId: String, projectId: String): BusinessLedgerPosting? {
        val sql = "SELECT * FROM business_ledger_postings WHERE posting_number = ? AND tenant_id = ? AND project_id = ?"
        return transactionManager.inReadOnly(TenantContext(projectId)) { tx ->
            tx.connection.prepareStatement(sql).use { ps ->
                ps.setString(1, postingNumber)
                ps.setString(2, tenantId)
                ps.setString(3, projectId)
                ps.executeQuery().use { rs ->
                    if (rs.next()) mapResultSetToPosting(rs) else null
                }
            }
        }
    }

    override suspend fun findPostingByIdempotencyKey(key: String, tenantId: String, projectId: String): BusinessLedgerPosting? {
        val sql = "SELECT * FROM business_ledger_postings WHERE idempotency_key = ? AND tenant_id = ? AND project_id = ?"
        return transactionManager.inReadOnly(TenantContext(projectId)) { tx ->
            tx.connection.prepareStatement(sql).use { ps ->
                ps.setString(1, key)
                ps.setString(2, tenantId)
                ps.setString(3, projectId)
                ps.executeQuery().use { rs ->
                    if (rs.next()) mapResultSetToPosting(rs) else null
                }
            }
        }
    }

    override suspend fun findPostingsBySource(
        sourceType: BusinessLedgerSourceType,
        sourceId: String,
        tenantId: String,
        projectId: String
    ): List<BusinessLedgerPosting> {
        val sql = "SELECT * FROM business_ledger_postings WHERE source_type = ? AND source_id = ? AND tenant_id = ? AND project_id = ? ORDER BY posting_date ASC"
        return transactionManager.inReadOnly(TenantContext(projectId)) { tx ->
            tx.connection.prepareStatement(sql).use { ps ->
                ps.setString(1, sourceType.name)
                ps.setString(2, sourceId)
                ps.setString(3, tenantId)
                ps.setString(4, projectId)
                ps.executeQuery().use { rs ->
                    val list = mutableListOf<BusinessLedgerPosting>()
                    while (rs.next()) {
                        list.add(mapResultSetToPosting(rs))
                    }
                    list
                }
            }
        }
    }

    override suspend fun findPostingBySourceAndType(
        sourceType: BusinessLedgerSourceType,
        sourceId: String,
        postingType: BusinessLedgerPostingType,
        tenantId: String,
        projectId: String
    ): BusinessLedgerPosting? {
        val sql = "SELECT * FROM business_ledger_postings WHERE source_type = ? AND source_id = ? AND posting_type = ? AND reversal_of_posting_id IS NULL AND tenant_id = ? AND project_id = ?"
        return transactionManager.inReadOnly(TenantContext(projectId)) { tx ->
            tx.connection.prepareStatement(sql).use { ps ->
                ps.setString(1, sourceType.name)
                ps.setString(2, sourceId)
                ps.setString(3, postingType.name)
                ps.setString(4, tenantId)
                ps.setString(5, projectId)
                ps.executeQuery().use { rs ->
                    if (rs.next()) mapResultSetToPosting(rs) else null
                }
            }
        }
    }

    override suspend fun listPostings(
        tenantId: String,
        projectId: String,
        filter: BusinessLedgerPostingFilter
    ): List<BusinessLedgerPosting> {
        val conditions = mutableListOf("tenant_id = ?", "project_id = ?")
        val params = mutableListOf<Any>(tenantId, projectId)

        filter.postingType?.let { conditions.add("posting_type = ?"); params.add(it.name) }
        filter.sourceType?.let { conditions.add("source_type = ?"); params.add(it.name) }
        filter.sourceId?.let { conditions.add("source_id = ?"); params.add(it) }
        filter.accountCategory?.let { conditions.add("account_category = ?"); params.add(it.name) }
        filter.jobId?.let { conditions.add("job_id = ?"); params.add(it) }
        filter.vendorId?.let { conditions.add("vendor_id = ?"); params.add(it) }
        filter.isReversed?.let { conditions.add("is_reversed = ?"); params.add(it) }
        filter.fromDate?.let { conditions.add("posting_date >= ?"); params.add(it) }
        filter.toDate?.let { conditions.add("posting_date <= ?"); params.add(it) }

        val sql = "SELECT * FROM business_ledger_postings WHERE ${conditions.joinToString(" AND ")} ORDER BY posting_date DESC, created_at DESC LIMIT ? OFFSET ?"
        params.add(filter.limit)
        params.add(filter.offset)

        return transactionManager.inReadOnly(TenantContext(projectId)) { tx ->
            tx.connection.prepareStatement(sql).use { ps ->
                params.forEachIndexed { i, p -> ps.setObject(i + 1, p) }
                ps.executeQuery().use { rs ->
                    val list = mutableListOf<BusinessLedgerPosting>()
                    while (rs.next()) {
                        list.add(mapResultSetToPosting(rs))
                    }
                    list
                }
            }
        }
    }

    override suspend fun countPostings(
        tenantId: String,
        projectId: String,
        filter: BusinessLedgerPostingFilter
    ): Long {
        val conditions = mutableListOf("tenant_id = ?", "project_id = ?")
        val params = mutableListOf<Any>(tenantId, projectId)

        filter.postingType?.let { conditions.add("posting_type = ?"); params.add(it.name) }
        filter.sourceType?.let { conditions.add("source_type = ?"); params.add(it.name) }
        filter.sourceId?.let { conditions.add("source_id = ?"); params.add(it) }
        filter.accountCategory?.let { conditions.add("account_category = ?"); params.add(it.name) }
        filter.jobId?.let { conditions.add("job_id = ?"); params.add(it) }
        filter.vendorId?.let { conditions.add("vendor_id = ?"); params.add(it) }
        filter.isReversed?.let { conditions.add("is_reversed = ?"); params.add(it) }
        filter.fromDate?.let { conditions.add("posting_date >= ?"); params.add(it) }
        filter.toDate?.let { conditions.add("posting_date <= ?"); params.add(it) }

        val sql = "SELECT COUNT(*) FROM business_ledger_postings WHERE ${conditions.joinToString(" AND ")}"

        return transactionManager.inReadOnly(TenantContext(projectId)) { tx ->
            tx.connection.prepareStatement(sql).use { ps ->
                params.forEachIndexed { i, p -> ps.setObject(i + 1, p) }
                ps.executeQuery().use { rs ->
                    if (rs.next()) rs.getLong(1) else 0L
                }
            }
        }
    }

    override suspend fun markPostingReversed(
        id: String,
        reversalReason: String,
        reversedBy: String,
        reversedAt: Long,
        reversalPostingId: String
    ): Boolean {
        val sql = """
            UPDATE business_ledger_postings
            SET is_reversed = TRUE, reversal_reason = ?, reversed_by = ?, reversed_at = ?, version = version + 1
            WHERE id = ? AND is_reversed = FALSE
        """.trimIndent()

        return transactionManager.inTransaction(TenantContext(defaultTenantId)) { tx ->
            tx.connection.prepareStatement(sql).use { ps ->
                ps.setString(1, reversalReason)
                ps.setString(2, reversedBy)
                ps.setLong(3, reversedAt)
                ps.setString(4, id)
                ps.executeUpdate() > 0
            }
        }
    }

    override suspend fun createCostAllocation(allocation: BusinessCostAllocation): BusinessCostAllocation {
        val sql = """
            INSERT INTO business_cost_allocations (
                id, tenant_id, project_id, allocation_number, source_type, source_id,
                ledger_posting_id, job_id, vendor_id, cost_category, allocated_amount,
                currency, allocation_date, reason, is_reversed, reversal_reason, reversed_by,
                reversed_at, correlation_id, idempotency_key, created_by, created_at, version
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """.trimIndent()

        transactionManager.inTransaction(TenantContext(allocation.projectId)) { tx ->
            tx.connection.prepareStatement(sql).use { ps ->
                ps.setString(1, allocation.id)
                ps.setString(2, allocation.tenantId)
                ps.setString(3, allocation.projectId)
                ps.setString(4, allocation.allocationNumber)
                ps.setString(5, allocation.sourceType.name)
                ps.setString(6, allocation.sourceId)
                ps.setString(7, allocation.ledgerPostingId)
                ps.setString(8, allocation.jobId)
                ps.setString(9, allocation.vendorId)
                ps.setString(10, allocation.costCategory.name)
                ps.setBigDecimal(11, allocation.allocatedAmount)
                ps.setString(12, allocation.currency)
                ps.setLong(13, allocation.allocationDate)
                ps.setString(14, allocation.reason)
                ps.setBoolean(15, allocation.isReversed)
                ps.setString(16, allocation.reversalReason)
                ps.setString(17, allocation.reversedBy)
                if (allocation.reversedAt != null) ps.setLong(18, allocation.reversedAt) else ps.setNull(18, java.sql.Types.BIGINT)
                ps.setString(19, allocation.correlationId)
                ps.setString(20, allocation.idempotencyKey)
                ps.setString(21, allocation.createdBy)
                ps.setLong(22, allocation.createdAt)
                ps.setLong(23, allocation.version)
                ps.executeUpdate()
            }
        }
        return allocation
    }

    override suspend fun findCostAllocationById(id: String, tenantId: String, projectId: String): BusinessCostAllocation? {
        val sql = "SELECT * FROM business_cost_allocations WHERE id = ? AND tenant_id = ? AND project_id = ?"
        return transactionManager.inReadOnly(TenantContext(projectId)) { tx ->
            tx.connection.prepareStatement(sql).use { ps ->
                ps.setString(1, id)
                ps.setString(2, tenantId)
                ps.setString(3, projectId)
                ps.executeQuery().use { rs ->
                    if (rs.next()) mapResultSetToAllocation(rs) else null
                }
            }
        }
    }

    override suspend fun findCostAllocationByIdempotencyKey(key: String, tenantId: String, projectId: String): BusinessCostAllocation? {
        val sql = "SELECT * FROM business_cost_allocations WHERE idempotency_key = ? AND tenant_id = ? AND project_id = ?"
        return transactionManager.inReadOnly(TenantContext(projectId)) { tx ->
            tx.connection.prepareStatement(sql).use { ps ->
                ps.setString(1, key)
                ps.setString(2, tenantId)
                ps.setString(3, projectId)
                ps.executeQuery().use { rs ->
                    if (rs.next()) mapResultSetToAllocation(rs) else null
                }
            }
        }
    }

    override suspend fun listCostAllocations(
        tenantId: String,
        projectId: String,
        filter: BusinessCostAllocationFilter
    ): List<BusinessCostAllocation> {
        val conditions = mutableListOf("tenant_id = ?", "project_id = ?")
        val params = mutableListOf<Any>(tenantId, projectId)

        filter.sourceType?.let { conditions.add("source_type = ?"); params.add(it.name) }
        filter.sourceId?.let { conditions.add("source_id = ?"); params.add(it) }
        filter.jobId?.let { conditions.add("job_id = ?"); params.add(it) }
        filter.vendorId?.let { conditions.add("vendor_id = ?"); params.add(it) }
        filter.costCategory?.let { conditions.add("cost_category = ?"); params.add(it.name) }
        filter.isReversed?.let { conditions.add("is_reversed = ?"); params.add(it) }
        filter.fromDate?.let { conditions.add("allocation_date >= ?"); params.add(it) }
        filter.toDate?.let { conditions.add("allocation_date <= ?"); params.add(it) }

        val sql = "SELECT * FROM business_cost_allocations WHERE ${conditions.joinToString(" AND ")} ORDER BY allocation_date DESC, created_at DESC LIMIT ? OFFSET ?"
        params.add(filter.limit)
        params.add(filter.offset)

        return transactionManager.inReadOnly(TenantContext(projectId)) { tx ->
            tx.connection.prepareStatement(sql).use { ps ->
                params.forEachIndexed { i, p -> ps.setObject(i + 1, p) }
                ps.executeQuery().use { rs ->
                    val list = mutableListOf<BusinessCostAllocation>()
                    while (rs.next()) {
                        list.add(mapResultSetToAllocation(rs))
                    }
                    list
                }
            }
        }
    }

    override suspend fun markCostAllocationReversed(
        id: String,
        reversalReason: String,
        reversedBy: String,
        reversedAt: Long
    ): Boolean {
        val sql = """
            UPDATE business_cost_allocations
            SET is_reversed = TRUE, reversal_reason = ?, reversed_by = ?, reversed_at = ?, version = version + 1
            WHERE id = ? AND is_reversed = FALSE
        """.trimIndent()

        return transactionManager.inTransaction(TenantContext(defaultTenantId)) { tx ->
            tx.connection.prepareStatement(sql).use { ps ->
                ps.setString(1, reversalReason)
                ps.setString(2, reversedBy)
                ps.setLong(3, reversedAt)
                ps.setString(4, id)
                ps.executeUpdate() > 0
            }
        }
    }

    override suspend fun recordAuditEvent(event: BusinessLedgerAuditEvent) {
        val sql = """
            INSERT INTO business_ledger_audit_events (
                event_id, tenant_id, project_id, event_type, actor_id, actor_role,
                timestamp, source_type, source_id, posting_id, allocation_id, action,
                previous_state, new_state, amount, reason, correlation_id, idempotency_key,
                checksum, metadata_json
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """.trimIndent()

        transactionManager.inTransaction(TenantContext(event.projectId)) { tx ->
            tx.connection.prepareStatement(sql).use { ps ->
                ps.setString(1, event.eventId)
                ps.setString(2, event.tenantId)
                ps.setString(3, event.projectId)
                ps.setString(4, event.eventType)
                ps.setString(5, event.actorId)
                ps.setString(6, event.actorRole)
                ps.setLong(7, event.timestamp)
                ps.setString(8, event.sourceType?.name)
                ps.setString(9, event.sourceId)
                ps.setString(10, event.postingId)
                ps.setString(11, event.allocationId)
                ps.setString(12, event.action)
                ps.setString(13, event.previousState)
                ps.setString(14, event.newState)
                ps.setBigDecimal(15, event.amount)
                ps.setString(16, event.reason)
                ps.setString(17, event.correlationId)
                ps.setString(18, event.idempotencyKey)
                ps.setString(19, event.checksum)
                ps.setString(20, event.metadataJson)
                ps.executeUpdate()
            }
        }
    }

    override suspend fun listAuditEvents(
        tenantId: String,
        projectId: String,
        sourceId: String?,
        postingId: String?,
        allocationId: String?
    ): List<BusinessLedgerAuditEvent> {
        val conditions = mutableListOf("tenant_id = ?", "project_id = ?")
        val params = mutableListOf<Any>(tenantId, projectId)

        sourceId?.let { conditions.add("source_id = ?"); params.add(it) }
        postingId?.let { conditions.add("posting_id = ?"); params.add(it) }
        allocationId?.let { conditions.add("allocation_id = ?"); params.add(it) }

        val sql = "SELECT * FROM business_ledger_audit_events WHERE ${conditions.joinToString(" AND ")} ORDER BY timestamp DESC"

        return transactionManager.inReadOnly(TenantContext(projectId)) { tx ->
            tx.connection.prepareStatement(sql).use { ps ->
                params.forEachIndexed { i, p -> ps.setObject(i + 1, p) }
                ps.executeQuery().use { rs ->
                    val list = mutableListOf<BusinessLedgerAuditEvent>()
                    while (rs.next()) {
                        list.add(mapResultSetToAuditEvent(rs))
                    }
                    list
                }
            }
        }
    }

    override suspend fun calculateBalanceSummary(
        tenantId: String,
        projectId: String,
        asOfTimestamp: Long
    ): BusinessLedgerBalanceSummary {
        val sql = """
            SELECT
                COALESCE(SUM(debit_amount), 0.0000) AS total_debit,
                COALESCE(SUM(credit_amount), 0.0000) AS total_credit
            FROM business_ledger_postings
            WHERE tenant_id = ? AND project_id = ? AND posting_date <= ?
        """.trimIndent()

        return transactionManager.inReadOnly(TenantContext(projectId)) { tx ->
            tx.connection.prepareStatement(sql).use { ps ->
                ps.setString(1, tenantId)
                ps.setString(2, projectId)
                ps.setLong(3, asOfTimestamp)
                ps.executeQuery().use { rs ->
                    if (rs.next()) {
                        val totalDebit = rs.getBigDecimal("total_debit").setScale(4, RoundingMode.HALF_UP)
                        val totalCredit = rs.getBigDecimal("total_credit").setScale(4, RoundingMode.HALF_UP)
                        val netMovement = totalDebit.subtract(totalCredit).setScale(4, RoundingMode.HALF_UP)
                        val openingBalance = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP)
                        val closingBalance = openingBalance.add(netMovement).setScale(4, RoundingMode.HALF_UP)

                        BusinessLedgerBalanceSummary(
                            tenantId = tenantId,
                            projectId = projectId,
                            openingBalance = openingBalance,
                            totalDebit = totalDebit,
                            totalCredit = totalCredit,
                            netMovement = netMovement,
                            closingBalance = closingBalance,
                            currency = "BDT",
                            asOfTimestamp = asOfTimestamp
                        )
                    } else {
                        BusinessLedgerBalanceSummary(tenantId = tenantId, projectId = projectId)
                    }
                }
            }
        }
    }

    override suspend fun calculatePeriodSummary(
        tenantId: String,
        projectId: String,
        fromDate: Long,
        toDate: Long
    ): BusinessLedgerPeriodSummary {
        val priorSql = """
            SELECT
                COALESCE(SUM(debit_amount), 0.0000) AS prior_debit,
                COALESCE(SUM(credit_amount), 0.0000) AS prior_credit
            FROM business_ledger_postings
            WHERE tenant_id = ? AND project_id = ? AND posting_date < ?
        """.trimIndent()

        val periodSql = """
            SELECT
                COALESCE(SUM(debit_amount), 0.0000) AS period_debit,
                COALESCE(SUM(credit_amount), 0.0000) AS period_credit,
                COUNT(*) AS posting_count
            FROM business_ledger_postings
            WHERE tenant_id = ? AND project_id = ? AND posting_date BETWEEN ? AND ?
        """.trimIndent()

        return transactionManager.inReadOnly(TenantContext(projectId)) { tx ->
            var openingBalance = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP)
            tx.connection.prepareStatement(priorSql).use { ps ->
                ps.setString(1, tenantId)
                ps.setString(2, projectId)
                ps.setLong(3, fromDate)
                ps.executeQuery().use { rs ->
                    if (rs.next()) {
                        val priorDebit = rs.getBigDecimal("prior_debit").setScale(4, RoundingMode.HALF_UP)
                        val priorCredit = rs.getBigDecimal("prior_credit").setScale(4, RoundingMode.HALF_UP)
                        openingBalance = priorDebit.subtract(priorCredit).setScale(4, RoundingMode.HALF_UP)
                    }
                }
            }

            tx.connection.prepareStatement(periodSql).use { ps ->
                ps.setString(1, tenantId)
                ps.setString(2, projectId)
                ps.setLong(3, fromDate)
                ps.setLong(4, toDate)
                ps.executeQuery().use { rs ->
                    if (rs.next()) {
                        val periodDebit = rs.getBigDecimal("period_debit").setScale(4, RoundingMode.HALF_UP)
                        val periodCredit = rs.getBigDecimal("period_credit").setScale(4, RoundingMode.HALF_UP)
                        val count = rs.getInt("posting_count")
                        val netMovement = periodDebit.subtract(periodCredit).setScale(4, RoundingMode.HALF_UP)
                        val closingBalance = openingBalance.add(netMovement).setScale(4, RoundingMode.HALF_UP)

                        BusinessLedgerPeriodSummary(
                            tenantId = tenantId,
                            projectId = projectId,
                            fromDate = fromDate,
                            toDate = toDate,
                            openingBalance = openingBalance,
                            totalDebit = periodDebit,
                            totalCredit = periodCredit,
                            netMovement = netMovement,
                            closingBalance = closingBalance,
                            postingCount = count,
                            currency = "BDT"
                        )
                    } else {
                        BusinessLedgerPeriodSummary(
                            tenantId = tenantId,
                            projectId = projectId,
                            fromDate = fromDate,
                            toDate = toDate,
                            openingBalance = openingBalance,
                            totalDebit = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP),
                            totalCredit = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP),
                            netMovement = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP),
                            closingBalance = openingBalance,
                            postingCount = 0,
                            currency = "BDT"
                        )
                    }
                }
            }
        }
    }
}
