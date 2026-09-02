package com.sucharu.sucharupro.domain.repository

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.common.Money
import com.sucharu.sucharupro.domain.model.finance.FinancialActivityEvent
import com.sucharu.sucharupro.domain.model.finance.FinancialEntryType
import com.sucharu.sucharupro.domain.model.finance.FinancialLedgerEntry
import com.sucharu.sucharupro.domain.model.finance.FinancialReferenceType
import com.sucharu.sucharupro.domain.model.finance.FinancialTransaction
import com.sucharu.sucharupro.domain.model.finance.FinancialTransactionStatus
import com.sucharu.sucharupro.domain.model.finance.FinancialTransactionType
import com.sucharu.sucharupro.domain.model.user.UserRole
import kotlinx.coroutines.flow.Flow

/**
 * Domain repository contract for Financial Transactions & Ledger Foundation (Module 09 Step 01).
 */
interface FinancialTransactionRepository {

    suspend fun createTransaction(
        projectId: String,
        transactionType: FinancialTransactionType,
        entryType: FinancialEntryType,
        amount: Money,
        currency: String = "BDT",
        referenceType: FinancialReferenceType,
        referenceId: String,
        customerId: String? = null,
        vendorId: String? = null,
        transactionDate: Long = System.currentTimeMillis(),
        description: String,
        notes: String? = null,
        actorId: String,
        callerRole: UserRole
    ): DomainResult<FinancialTransaction>

    suspend fun updateDraftTransaction(
        transactionId: String,
        amount: Money? = null,
        entryType: FinancialEntryType? = null,
        description: String? = null,
        notes: String? = null,
        actorId: String,
        callerRole: UserRole
    ): DomainResult<FinancialTransaction>

    suspend fun submitTransaction(
        transactionId: String,
        actorId: String,
        callerRole: UserRole
    ): DomainResult<FinancialTransaction>

    suspend fun postTransaction(
        transactionId: String,
        accountHead: String? = null,
        actorId: String,
        callerRole: UserRole
    ): DomainResult<FinancialTransaction>

    suspend fun rejectTransaction(
        transactionId: String,
        rejectionReason: String,
        actorId: String,
        callerRole: UserRole
    ): DomainResult<FinancialTransaction>

    suspend fun cancelTransaction(
        transactionId: String,
        cancellationReason: String,
        actorId: String,
        callerRole: UserRole
    ): DomainResult<FinancialTransaction>

    suspend fun getTransactionById(
        transactionId: String,
        callerRole: UserRole
    ): DomainResult<FinancialTransaction>

    suspend fun getTransactionsByReference(
        projectId: String,
        referenceId: String,
        callerRole: UserRole
    ): DomainResult<List<FinancialTransaction>>

    fun observeTransactions(
        projectId: String,
        callerRole: UserRole
    ): Flow<List<FinancialTransaction>>

    suspend fun getLedgerEntriesByTransaction(
        transactionId: String,
        callerRole: UserRole
    ): DomainResult<List<FinancialLedgerEntry>>

    fun observeLedgerEntries(
        projectId: String,
        callerRole: UserRole
    ): Flow<List<FinancialLedgerEntry>>

    suspend fun getActivityEvents(
        transactionId: String,
        callerRole: UserRole
    ): DomainResult<List<FinancialActivityEvent>>
}
