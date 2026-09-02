package com.sucharu.sucharupro.data.persistence.postgres

import com.sucharu.sucharupro.data.datasource.FinancialTransactionDataSource
import com.sucharu.sucharupro.data.persistence.postgres.RowMappers.getEnumByName
import com.sucharu.sucharupro.data.persistence.postgres.RowMappers.getMoney
import com.sucharu.sucharupro.data.persistence.postgres.RowMappers.getNullableTimestampMillis
import com.sucharu.sucharupro.data.persistence.postgres.RowMappers.getTimestampMillis
import com.sucharu.sucharupro.domain.model.finance.FinancialActivityEvent
import com.sucharu.sucharupro.domain.model.finance.FinancialActivityType
import com.sucharu.sucharupro.domain.model.finance.FinancialEntryType
import com.sucharu.sucharupro.domain.model.finance.FinancialLedgerEntry
import com.sucharu.sucharupro.domain.model.finance.FinancialReferenceType
import com.sucharu.sucharupro.domain.model.finance.FinancialTransaction
import com.sucharu.sucharupro.domain.model.finance.FinancialTransactionStatus
import com.sucharu.sucharupro.domain.model.finance.FinancialTransactionType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.sql.ResultSet
import java.sql.Timestamp

/**
 * Production-grade PostgreSQL DataSource for Financial Transactions & General Ledger (INFRA-01 Step 03).
 */
class PostgresFinancialTransactionDataSource(
    private val transactionManager: TransactionManager
) : FinancialTransactionDataSource {

    private fun mapFinancialTransaction(rs: ResultSet): FinancialTransaction {
        val postedAt = rs.getNullableTimestampMillis("posted_at")
        val postedBy = rs.getString("posted_by")
        val rawNotes = rs.getString("notes")
        
        val status = if (postedAt != null && !postedBy.isNullOrBlank()) {
            FinancialTransactionStatus.POSTED
        } else if (rawNotes?.startsWith("STATUS:PENDING|") == true) {
            FinancialTransactionStatus.PENDING
        } else if (rawNotes?.startsWith("STATUS:REJECTED|") == true) {
            FinancialTransactionStatus.REJECTED
        } else if (rawNotes?.startsWith("STATUS:CANCELLED|") == true) {
            FinancialTransactionStatus.CANCELLED
        } else {
            FinancialTransactionStatus.DRAFT
        }

        val cleanNotes = if (rawNotes?.startsWith("STATUS:") == true) {
            rawNotes.substringAfter("|", "").ifEmpty { null }
        } else {
            rawNotes
        }

        return FinancialTransaction(
            transactionId = rs.getString("transaction_id"),
            projectId = rs.getString("project_id"),
            transactionNo = rs.getString("transaction_number"),
            transactionType = rs.getEnumByName("transaction_type", FinancialTransactionType.SALE),
            transactionStatus = status,
            entryType = rs.getEnumByName("entry_type", FinancialEntryType.DEBIT),
            amount = rs.getMoney("total_amount"),
            currency = rs.getString("currency") ?: "BDT",
            referenceType = rs.getEnumByName("reference_type", FinancialReferenceType.MANUAL),
            referenceId = rs.getString("reference_id") ?: rs.getString("transaction_id"),
            customerId = rs.getString("customer_id"),
            vendorId = rs.getString("vendor_id"),
            transactionDate = rs.getNullableTimestampMillis("posted_at") ?: rs.getTimestampMillis("created_at"),
            description = cleanNotes ?: "",
            notes = cleanNotes,
            postedBy = rs.getString("posted_by"),
            postedAt = rs.getNullableTimestampMillis("posted_at"),
            createdBy = rs.getString("posted_by") ?: "SYSTEM",
            createdAt = rs.getTimestampMillis("created_at"),
            updatedAt = rs.getTimestampMillis("created_at")
        )
    }

    private fun mapLedgerEntry(rs: ResultSet): FinancialLedgerEntry {
        return FinancialLedgerEntry(
            entryId = rs.getString("line_id"),
            transactionId = rs.getString("transaction_id"),
            projectId = rs.getString("project_id"),
            entryNo = rs.getString("line_id"),
            entryType = rs.getEnumByName("entry_type", FinancialEntryType.DEBIT),
            amount = rs.getMoney("amount"),
            currency = "BDT",
            accountHead = rs.getString("account_name"),
            referenceType = FinancialReferenceType.MANUAL,
            referenceId = rs.getString("transaction_id"),
            entryDate = System.currentTimeMillis(),
            narration = rs.getString("account_name"),
            createdBy = "SYSTEM",
            createdAt = System.currentTimeMillis()
        )
    }

    override suspend fun insertTransaction(transaction: FinancialTransaction) {
        val tenant = TenantContext(transaction.projectId)
        transactionManager.inTransaction(tenant) { ctx ->
            val sql = """
                INSERT INTO financial_transactions (
                    project_id, transaction_id, period_id, transaction_number,
                    transaction_type, total_amount, currency, notes, posted_by, posted_at, created_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """.trimIndent()

            val periodId = "PERIOD-${transaction.projectId}-DEFAULT"
            val encodedNotes = if (transaction.transactionStatus != FinancialTransactionStatus.DRAFT && transaction.transactionStatus != FinancialTransactionStatus.POSTED) {
                "STATUS:${transaction.transactionStatus.name}|${transaction.description}"
            } else {
                transaction.description
            }
            
            ctx.sqlExecutor.executeUpdate(
                sql,
                listOf(
                    transaction.projectId,
                    transaction.transactionId,
                    periodId,
                    transaction.transactionNo,
                    transaction.transactionType.name,
                    transaction.amount.amount,
                    transaction.currency,
                    encodedNotes,
                    transaction.postedBy,
                    transaction.postedAt?.let { Timestamp(it) },
                    Timestamp(transaction.createdAt)
                )
            )
        }
    }

    override suspend fun updateTransaction(transaction: FinancialTransaction) {
        val tenant = TenantContext(transaction.projectId)
        transactionManager.inTransaction(tenant) { ctx ->
            val sql = """
                UPDATE financial_transactions
                SET transaction_type = ?, total_amount = ?, notes = ?, posted_by = ?, posted_at = ?, version = version + 1
                WHERE project_id = ? AND transaction_id = ?
            """.trimIndent()

            val encodedNotes = if (transaction.transactionStatus != FinancialTransactionStatus.DRAFT && transaction.transactionStatus != FinancialTransactionStatus.POSTED) {
                "STATUS:${transaction.transactionStatus.name}|${transaction.description}"
            } else {
                transaction.description
            }

            ctx.sqlExecutor.executeUpdate(
                sql,
                listOf(
                    transaction.transactionType.name,
                    transaction.amount.amount,
                    encodedNotes,
                    transaction.postedBy,
                    transaction.postedAt?.let { Timestamp(it) },
                    transaction.projectId,
                    transaction.transactionId
                )
            )
        }
    }

    override suspend fun getTransactionById(transactionId: String): FinancialTransaction? {
        val sql = """
            SELECT transaction_id, project_id, transaction_number, transaction_type, 
                   'POSTED' AS transaction_status, 'DEBIT' AS entry_type, total_amount, 
                   currency, 'MANUAL' AS reference_type, transaction_id AS reference_id,
                   NULL AS customer_id, NULL AS vendor_id, notes, posted_by, posted_at, created_at
            FROM financial_transactions
            WHERE transaction_id = ?
        """.trimIndent()

        val tenant = TenantContext("DEFAULT_CONTEXT")
        return transactionManager.inReadOnly(tenant) { ctx ->
            ctx.sqlExecutor.querySingleOrNull(sql, listOf(transactionId)) { rs ->
                mapFinancialTransaction(rs)
            }
        }
    }

    override suspend fun getTransactionByNumber(
        projectId: String,
        transactionNo: String
    ): FinancialTransaction? {
        val tenant = TenantContext(projectId)
        return transactionManager.inReadOnly(tenant) { ctx ->
            val sql = """
                SELECT transaction_id, project_id, transaction_number, transaction_type, 
                       'POSTED' AS transaction_status, 'DEBIT' AS entry_type, total_amount, 
                       currency, 'MANUAL' AS reference_type, transaction_id AS reference_id,
                       NULL AS customer_id, NULL AS vendor_id, notes, posted_by, posted_at, created_at
                FROM financial_transactions
                WHERE project_id = ? AND transaction_number = ?
            """.trimIndent()

            ctx.sqlExecutor.querySingleOrNull(sql, listOf(projectId, transactionNo)) { rs ->
                mapFinancialTransaction(rs)
            }
        }
    }

    override suspend fun getTransactionsByReference(
        projectId: String,
        referenceId: String
    ): List<FinancialTransaction> {
        val tenant = TenantContext(projectId)
        return transactionManager.inReadOnly(tenant) { ctx ->
            val sql = """
                SELECT transaction_id, project_id, transaction_number, transaction_type, 
                       'POSTED' AS transaction_status, 'DEBIT' AS entry_type, total_amount, 
                       currency, 'MANUAL' AS reference_type, transaction_id AS reference_id,
                       NULL AS customer_id, NULL AS vendor_id, notes, posted_by, posted_at, created_at
                FROM financial_transactions
                WHERE project_id = ? AND transaction_id = ?
            """.trimIndent()

            ctx.sqlExecutor.queryList(sql, listOf(projectId, referenceId)) { rs ->
                mapFinancialTransaction(rs)
            }
        }
    }

    override fun observeTransactions(projectId: String): Flow<List<FinancialTransaction>> = flow {
        val tenant = TenantContext(projectId)
        val list = transactionManager.inReadOnly(tenant) { ctx ->
            val sql = """
                SELECT transaction_id, project_id, transaction_number, transaction_type, 
                       'POSTED' AS transaction_status, 'DEBIT' AS entry_type, total_amount, 
                       currency, 'MANUAL' AS reference_type, transaction_id AS reference_id,
                       NULL AS customer_id, NULL AS vendor_id, notes, posted_by, posted_at, created_at
                FROM financial_transactions
                WHERE project_id = ?
                ORDER BY created_at DESC
            """.trimIndent()

            ctx.sqlExecutor.queryList(sql, listOf(projectId)) { rs ->
                mapFinancialTransaction(rs)
            }
        }
        emit(list)
    }

    override suspend fun getTransactionsByStatus(
        projectId: String,
        status: FinancialTransactionStatus
    ): List<FinancialTransaction> {
        val tenant = TenantContext(projectId)
        return transactionManager.inReadOnly(tenant) { ctx ->
            val sql = """
                SELECT transaction_id, project_id, transaction_number, transaction_type, 
                       'POSTED' AS transaction_status, 'DEBIT' AS entry_type, total_amount, 
                       currency, 'MANUAL' AS reference_type, transaction_id AS reference_id,
                       NULL AS customer_id, NULL AS vendor_id, notes, posted_by, posted_at, created_at
                FROM financial_transactions
                WHERE project_id = ?
                ORDER BY created_at DESC
            """.trimIndent()

            ctx.sqlExecutor.queryList(sql, listOf(projectId)) { rs ->
                mapFinancialTransaction(rs)
            }
        }
    }

    override suspend fun insertLedgerEntry(entry: FinancialLedgerEntry) {
        insertLedgerEntries(listOf(entry))
    }

    override suspend fun insertLedgerEntries(entries: List<FinancialLedgerEntry>) {
        if (entries.isEmpty()) return
        val tenant = TenantContext(entries.first().projectId)
        transactionManager.inTransaction(tenant) { ctx ->
            val sql = """
                INSERT INTO journal_lines (
                    line_id, project_id, transaction_id, account_code,
                    account_name, entry_type, amount
                ) VALUES (?, ?, ?, ?, ?, ?, ?)
            """.trimIndent()

            val batches = entries.map { entry ->
                listOf(
                    entry.entryId,
                    entry.projectId,
                    entry.transactionId,
                    entry.accountHead,
                    entry.accountHead,
                    entry.entryType.name,
                    entry.amount.amount
                )
            }
            ctx.sqlExecutor.executeBatch(sql, batches)
        }
    }

    override suspend fun getLedgerEntriesByTransaction(transactionId: String): List<FinancialLedgerEntry> {
        val tenant = TenantContext("DEFAULT_CONTEXT")
        return transactionManager.inReadOnly(tenant) { ctx ->
            val sql = """
                SELECT line_id, project_id, transaction_id, account_code, account_name, entry_type, amount
                FROM journal_lines
                WHERE transaction_id = ?
            """.trimIndent()

            ctx.sqlExecutor.queryList(sql, listOf(transactionId)) { rs ->
                mapLedgerEntry(rs)
            }
        }
    }

    override fun observeLedgerEntries(projectId: String): Flow<List<FinancialLedgerEntry>> = flow {
        val tenant = TenantContext(projectId)
        val list = transactionManager.inReadOnly(tenant) { ctx ->
            val sql = """
                SELECT line_id, project_id, transaction_id, account_code, account_name, entry_type, amount
                FROM journal_lines
                WHERE project_id = ?
            """.trimIndent()

            ctx.sqlExecutor.queryList(sql, listOf(projectId)) { rs ->
                mapLedgerEntry(rs)
            }
        }
        emit(list)
    }

    override suspend fun insertActivityEvent(event: FinancialActivityEvent) {
        val tenant = TenantContext(event.projectId)
        transactionManager.inTransaction(tenant) { ctx ->
            val sql = """
                INSERT INTO domain_activity_events (
                    event_id, project_id, aggregate_type, aggregate_id, event_type, actor_id, occurred_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?)
            """.trimIndent()

            ctx.sqlExecutor.executeUpdate(
                sql,
                listOf(
                    event.eventId,
                    event.projectId,
                    "FINANCIAL_TRANSACTION",
                    event.transactionId,
                    event.activityType.name,
                    event.actorId,
                    Timestamp(event.timestamp)
                )
            )
        }
    }

    override suspend fun getActivityEvents(transactionId: String): List<FinancialActivityEvent> {
        val tenant = TenantContext("DEFAULT_CONTEXT")
        return transactionManager.inReadOnly(tenant) { ctx ->
            val sql = """
                SELECT event_id, project_id, aggregate_id AS transaction_id, event_type, actor_id, 
                       EXTRACT(EPOCH FROM occurred_at) * 1000 AS occurred_ms
                FROM domain_activity_events
                WHERE aggregate_id = ?
                ORDER BY occurred_at ASC
            """.trimIndent()

            ctx.sqlExecutor.queryList(sql, listOf(transactionId)) { rs ->
                FinancialActivityEvent(
                    eventId = rs.getString("event_id"),
                    projectId = rs.getString("project_id"),
                    transactionId = rs.getString("transaction_id"),
                    activityType = rs.getEnumByName("event_type", FinancialActivityType.TRANSACTION_CREATED),
                    actorId = rs.getString("actor_id"),
                    details = rs.getString("actor_id"),
                    timestamp = rs.getLong("occurred_ms")
                )
            }
        }
    }
}
