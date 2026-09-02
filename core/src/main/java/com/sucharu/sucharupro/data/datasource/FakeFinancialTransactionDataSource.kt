package com.sucharu.sucharupro.data.datasource

import com.sucharu.sucharupro.domain.model.finance.FinancialActivityEvent
import com.sucharu.sucharupro.domain.model.finance.FinancialLedgerEntry
import com.sucharu.sucharupro.domain.model.finance.FinancialTransaction
import com.sucharu.sucharupro.domain.model.finance.FinancialTransactionStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Thread-safe in-memory fake implementation of [FinancialTransactionDataSource] (Module 09 Step 01).
 */
class FakeFinancialTransactionDataSource : FinancialTransactionDataSource {

    private val mutex = Mutex()
    private val transactionsState = MutableStateFlow<Map<String, FinancialTransaction>>(emptyMap())
    private val ledgerEntriesState = MutableStateFlow<List<FinancialLedgerEntry>>(emptyList())
    private val activityEventsState = MutableStateFlow<List<FinancialActivityEvent>>(emptyList())

    override suspend fun insertTransaction(transaction: FinancialTransaction): Unit = mutex.withLock {
        val current = transactionsState.value.toMutableMap()
        current[transaction.transactionId] = transaction
        transactionsState.value = current
    }

    override suspend fun updateTransaction(transaction: FinancialTransaction): Unit = mutex.withLock {
        val current = transactionsState.value.toMutableMap()
        current[transaction.transactionId] = transaction
        transactionsState.value = current
    }

    override suspend fun getTransactionById(transactionId: String): FinancialTransaction? = mutex.withLock {
        transactionsState.value[transactionId]
    }

    override suspend fun getTransactionByNumber(projectId: String, transactionNo: String): FinancialTransaction? = mutex.withLock {
        transactionsState.value.values.firstOrNull { it.projectId == projectId && it.transactionNo == transactionNo }
    }

    override suspend fun getTransactionsByReference(projectId: String, referenceId: String): List<FinancialTransaction> = mutex.withLock {
        transactionsState.value.values.filter { it.projectId == projectId && it.referenceId == referenceId }
    }

    override fun observeTransactions(projectId: String): Flow<List<FinancialTransaction>> {
        return transactionsState.map { map ->
            map.values.filter { it.projectId == projectId }.sortedByDescending { it.createdAt }
        }
    }

    override suspend fun getTransactionsByStatus(
        projectId: String,
        status: FinancialTransactionStatus
    ): List<FinancialTransaction> = mutex.withLock {
        transactionsState.value.values
            .filter { it.projectId == projectId && it.transactionStatus == status }
            .sortedByDescending { it.createdAt }
    }

    override suspend fun insertLedgerEntry(entry: FinancialLedgerEntry): Unit = mutex.withLock {
        ledgerEntriesState.value = ledgerEntriesState.value + entry
    }

    override suspend fun insertLedgerEntries(entries: List<FinancialLedgerEntry>): Unit = mutex.withLock {
        ledgerEntriesState.value = ledgerEntriesState.value + entries
    }

    override suspend fun getLedgerEntriesByTransaction(transactionId: String): List<FinancialLedgerEntry> = mutex.withLock {
        ledgerEntriesState.value.filter { it.transactionId == transactionId }.sortedBy { it.createdAt }
    }

    override fun observeLedgerEntries(projectId: String): Flow<List<FinancialLedgerEntry>> {
        return ledgerEntriesState.map { list ->
            list.filter { it.projectId == projectId }.sortedByDescending { it.createdAt }
        }
    }

    override suspend fun insertActivityEvent(event: FinancialActivityEvent): Unit = mutex.withLock {
        activityEventsState.value = activityEventsState.value + event
    }

    override suspend fun getActivityEvents(transactionId: String): List<FinancialActivityEvent> = mutex.withLock {
        activityEventsState.value.filter { it.transactionId == transactionId }.sortedBy { it.timestamp }
    }
}
