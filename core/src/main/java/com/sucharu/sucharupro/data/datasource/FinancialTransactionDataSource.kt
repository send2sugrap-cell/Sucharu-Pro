package com.sucharu.sucharupro.data.datasource

import com.sucharu.sucharupro.domain.model.finance.FinancialActivityEvent
import com.sucharu.sucharupro.domain.model.finance.FinancialLedgerEntry
import com.sucharu.sucharupro.domain.model.finance.FinancialTransaction
import com.sucharu.sucharupro.domain.model.finance.FinancialTransactionStatus
import kotlinx.coroutines.flow.Flow

/**
 * Contract for Financial Transaction & Ledger persistence (Module 09 Step 01).
 */
interface FinancialTransactionDataSource {

    suspend fun insertTransaction(transaction: FinancialTransaction)

    suspend fun updateTransaction(transaction: FinancialTransaction)

    suspend fun getTransactionById(transactionId: String): FinancialTransaction?

    suspend fun getTransactionByNumber(projectId: String, transactionNo: String): FinancialTransaction?

    suspend fun getTransactionsByReference(projectId: String, referenceId: String): List<FinancialTransaction>

    fun observeTransactions(projectId: String): Flow<List<FinancialTransaction>>

    suspend fun getTransactionsByStatus(
        projectId: String,
        status: FinancialTransactionStatus
    ): List<FinancialTransaction>

    suspend fun insertLedgerEntry(entry: FinancialLedgerEntry)

    suspend fun insertLedgerEntries(entries: List<FinancialLedgerEntry>)

    suspend fun getLedgerEntriesByTransaction(transactionId: String): List<FinancialLedgerEntry>

    fun observeLedgerEntries(projectId: String): Flow<List<FinancialLedgerEntry>>

    suspend fun insertActivityEvent(event: FinancialActivityEvent)

    suspend fun getActivityEvents(transactionId: String): List<FinancialActivityEvent>
}
