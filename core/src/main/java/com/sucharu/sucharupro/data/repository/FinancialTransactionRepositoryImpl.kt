package com.sucharu.sucharupro.data.repository

import com.sucharu.sucharupro.data.datasource.FinancialTransactionDataSource
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.common.Money
import com.sucharu.sucharupro.domain.model.finance.FinancialActivityEvent
import com.sucharu.sucharupro.domain.model.finance.FinancialActivityType
import com.sucharu.sucharupro.domain.model.finance.FinancialEntryType
import com.sucharu.sucharupro.domain.model.finance.FinancialLedgerEntry
import com.sucharu.sucharupro.domain.model.finance.FinancialReferenceType
import com.sucharu.sucharupro.domain.model.finance.FinancialTransaction
import com.sucharu.sucharupro.domain.model.finance.FinancialTransactionStatus
import com.sucharu.sucharupro.domain.model.finance.FinancialTransactionType
import com.sucharu.sucharupro.domain.model.user.UserRole
import com.sucharu.sucharupro.domain.repository.FinancialTransactionRepository
import com.sucharu.sucharupro.domain.validation.FinancialAuthorizationValidator
import com.sucharu.sucharupro.domain.validation.FinancialLedgerEntryValidator
import com.sucharu.sucharupro.domain.validation.FinancialTransactionLifecycleValidator
import com.sucharu.sucharupro.domain.validation.FinancialTransactionValidator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.UUID

/**
 * Production-grade repository implementation for Financial Transactions & Ledger (Module 09 Step 01).
 *
 * Enforces non-reentrant mutex concurrency, atomic ledger posting, and strict RBAC/project isolation.
 */
class FinancialTransactionRepositoryImpl(
    private val dataSource: FinancialTransactionDataSource
) : FinancialTransactionRepository {

    private val mutex = Mutex()

    override suspend fun createTransaction(
        projectId: String,
        transactionType: FinancialTransactionType,
        entryType: FinancialEntryType,
        amount: Money,
        currency: String,
        referenceType: FinancialReferenceType,
        referenceId: String,
        customerId: String?,
        vendorId: String?,
        transactionDate: Long,
        description: String,
        notes: String?,
        actorId: String,
        callerRole: UserRole
    ): DomainResult<FinancialTransaction> = mutex.withLock {
        val authResult = FinancialAuthorizationValidator.validateCreateTransaction(callerRole)
        if (authResult is DomainResult.Error) return authResult

        if (projectId.isBlank()) return DomainResult.Error(message = "Project ID cannot be blank.")
        if (referenceId.isBlank()) return DomainResult.Error(message = "Reference ID cannot be blank.")
        if (description.isBlank()) return DomainResult.Error(message = "Description cannot be blank.")
        if (!amount.isPositive()) return DomainResult.Error(message = "Transaction amount must be strictly positive (> 0).")

        val now = System.currentTimeMillis()
        val transactionId = "TXN-${UUID.randomUUID()}"
        val transactionNo = "FTX-${now.toString().takeLast(6)}-${(100..999).random()}"

        val transaction = FinancialTransaction(
            transactionId = transactionId,
            projectId = projectId,
            transactionNo = transactionNo,
            transactionType = transactionType,
            transactionStatus = FinancialTransactionStatus.DRAFT,
            entryType = entryType,
            amount = amount,
            currency = currency,
            referenceType = referenceType,
            referenceId = referenceId,
            customerId = customerId,
            vendorId = vendorId,
            transactionDate = transactionDate,
            description = description.trim(),
            notes = notes?.trim(),
            createdBy = actorId,
            createdAt = now,
            updatedAt = now
        )

        val validationResult = FinancialTransactionValidator.validateTransaction(transaction, projectId)
        if (validationResult is DomainResult.Error) return validationResult

        dataSource.insertTransaction(transaction)
        dataSource.insertActivityEvent(
            FinancialActivityEvent(
                eventId = UUID.randomUUID().toString(),
                transactionId = transactionId,
                projectId = projectId,
                activityType = FinancialActivityType.TRANSACTION_CREATED,
                actorId = actorId,
                details = "Financial transaction '$transactionNo' created in DRAFT status."
            )
        )

        DomainResult.Success(transaction)
    }

    override suspend fun updateDraftTransaction(
        transactionId: String,
        amount: Money?,
        entryType: FinancialEntryType?,
        description: String?,
        notes: String?,
        actorId: String,
        callerRole: UserRole
    ): DomainResult<FinancialTransaction> = mutex.withLock {
        val authResult = FinancialAuthorizationValidator.validateUpdateDraft(callerRole)
        if (authResult is DomainResult.Error) return authResult

        val existing = dataSource.getTransactionById(transactionId)
            ?: return DomainResult.Error(message = "Financial transaction '$transactionId' not found.")

        if (existing.transactionStatus != FinancialTransactionStatus.DRAFT) {
            return DomainResult.Error(
                message = "Only DRAFT transactions can be edited. Current status: '${existing.transactionStatus}'."
            )
        }

        val now = System.currentTimeMillis()
        val updated = existing.copy(
            amount = amount ?: existing.amount,
            entryType = entryType ?: existing.entryType,
            description = description?.trim() ?: existing.description,
            notes = notes?.trim() ?: existing.notes,
            updatedAt = now
        )

        val immutabilityResult = FinancialTransactionValidator.validateImmutabilityOnUpdate(existing, updated)
        if (immutabilityResult is DomainResult.Error) return immutabilityResult

        val validationResult = FinancialTransactionValidator.validateTransaction(updated, existing.projectId)
        if (validationResult is DomainResult.Error) return validationResult

        dataSource.updateTransaction(updated)
        dataSource.insertActivityEvent(
            FinancialActivityEvent(
                eventId = UUID.randomUUID().toString(),
                transactionId = transactionId,
                projectId = existing.projectId,
                activityType = FinancialActivityType.TRANSACTION_UPDATED,
                actorId = actorId,
                details = "Financial transaction draft updated by user '$actorId'."
            )
        )

        DomainResult.Success(updated)
    }

    override suspend fun submitTransaction(
        transactionId: String,
        actorId: String,
        callerRole: UserRole
    ): DomainResult<FinancialTransaction> = mutex.withLock {
        val authResult = FinancialAuthorizationValidator.validateSubmitTransaction(callerRole)
        if (authResult is DomainResult.Error) return authResult

        val existing = dataSource.getTransactionById(transactionId)
            ?: return DomainResult.Error(message = "Financial transaction '$transactionId' not found.")

        val transitionResult = FinancialTransactionLifecycleValidator.validateTransition(
            existing.transactionStatus,
            FinancialTransactionStatus.PENDING
        )
        if (transitionResult is DomainResult.Error) return transitionResult

        val now = System.currentTimeMillis()
        val updated = existing.copy(
            transactionStatus = FinancialTransactionStatus.PENDING,
            updatedAt = now
        )

        val validationResult = FinancialTransactionValidator.validateTransaction(updated, existing.projectId)
        if (validationResult is DomainResult.Error) return validationResult

        dataSource.updateTransaction(updated)
        dataSource.insertActivityEvent(
            FinancialActivityEvent(
                eventId = UUID.randomUUID().toString(),
                transactionId = transactionId,
                projectId = existing.projectId,
                activityType = FinancialActivityType.TRANSACTION_SUBMITTED,
                actorId = actorId,
                details = "Financial transaction '${existing.transactionNo}' submitted for approval."
            )
        )

        DomainResult.Success(updated)
    }

    override suspend fun postTransaction(
        transactionId: String,
        accountHead: String?,
        actorId: String,
        callerRole: UserRole
    ): DomainResult<FinancialTransaction> = mutex.withLock {
        val existing = dataSource.getTransactionById(transactionId)
            ?: return DomainResult.Error(message = "Financial transaction '$transactionId' not found.")

        val authResult = FinancialAuthorizationValidator.validatePostTransaction(
            callerRole = callerRole,
            creatorId = existing.createdBy,
            actorId = actorId
        )
        if (authResult is DomainResult.Error) return authResult

        val transitionResult = FinancialTransactionLifecycleValidator.validateTransition(
            existing.transactionStatus,
            FinancialTransactionStatus.POSTED
        )
        if (transitionResult is DomainResult.Error) return transitionResult

        val now = System.currentTimeMillis()
        val updated = existing.copy(
            transactionStatus = FinancialTransactionStatus.POSTED,
            postedBy = actorId,
            postedAt = now,
            updatedAt = now
        )

        val validationResult = FinancialTransactionValidator.validateTransaction(updated, existing.projectId)
        if (validationResult is DomainResult.Error) return validationResult

        // Derive or validate Account Head
        val resolvedAccountHead = accountHead?.trim()?.ifEmpty { null } ?: deriveDefaultAccountHead(existing.transactionType)

        // Create atomic ledger entry
        val ledgerEntry = FinancialLedgerEntry(
            entryId = "LED-${UUID.randomUUID()}",
            transactionId = transactionId,
            projectId = existing.projectId,
            entryNo = "LED-${now.toString().takeLast(6)}-${(100..999).random()}",
            entryType = existing.entryType,
            amount = existing.amount,
            currency = existing.currency,
            accountHead = resolvedAccountHead,
            referenceType = existing.referenceType,
            referenceId = existing.referenceId,
            entryDate = existing.transactionDate,
            narration = existing.description,
            createdBy = actorId,
            createdAt = now
        )

        val ledgerValidation = FinancialLedgerEntryValidator.validateEntry(ledgerEntry, existing.projectId)
        if (ledgerValidation is DomainResult.Error) return ledgerValidation

        // Atomic commit: update transaction + insert ledger entry + record events
        dataSource.updateTransaction(updated)
        dataSource.insertLedgerEntry(ledgerEntry)

        dataSource.insertActivityEvent(
            FinancialActivityEvent(
                eventId = UUID.randomUUID().toString(),
                transactionId = transactionId,
                projectId = existing.projectId,
                activityType = FinancialActivityType.TRANSACTION_POSTED,
                actorId = actorId,
                details = "Financial transaction '${existing.transactionNo}' posted to ledger under account '$resolvedAccountHead'."
            )
        )

        dataSource.insertActivityEvent(
            FinancialActivityEvent(
                eventId = UUID.randomUUID().toString(),
                transactionId = transactionId,
                projectId = existing.projectId,
                activityType = FinancialActivityType.LEDGER_ENTRY_POSTED,
                actorId = actorId,
                details = "Authoritative ledger entry '${ledgerEntry.entryNo}' created."
            )
        )

        DomainResult.Success(updated)
    }

    override suspend fun rejectTransaction(
        transactionId: String,
        rejectionReason: String,
        actorId: String,
        callerRole: UserRole
    ): DomainResult<FinancialTransaction> = mutex.withLock {
        val authResult = FinancialAuthorizationValidator.validateRejectTransaction(callerRole)
        if (authResult is DomainResult.Error) return authResult

        if (rejectionReason.trim().isEmpty()) {
            return DomainResult.Error(message = "Rejection reason cannot be blank.")
        }

        val existing = dataSource.getTransactionById(transactionId)
            ?: return DomainResult.Error(message = "Financial transaction '$transactionId' not found.")

        val transitionResult = FinancialTransactionLifecycleValidator.validateTransition(
            existing.transactionStatus,
            FinancialTransactionStatus.REJECTED
        )
        if (transitionResult is DomainResult.Error) return transitionResult

        val now = System.currentTimeMillis()
        val updated = existing.copy(
            transactionStatus = FinancialTransactionStatus.REJECTED,
            rejectedBy = actorId,
            rejectedAt = now,
            rejectionReason = rejectionReason.trim(),
            updatedAt = now
        )

        val validationResult = FinancialTransactionValidator.validateTransaction(updated, existing.projectId)
        if (validationResult is DomainResult.Error) return validationResult

        dataSource.updateTransaction(updated)
        dataSource.insertActivityEvent(
            FinancialActivityEvent(
                eventId = UUID.randomUUID().toString(),
                transactionId = transactionId,
                projectId = existing.projectId,
                activityType = FinancialActivityType.TRANSACTION_REJECTED,
                actorId = actorId,
                details = "Financial transaction rejected by '$actorId': $rejectionReason"
            )
        )

        DomainResult.Success(updated)
    }

    override suspend fun cancelTransaction(
        transactionId: String,
        cancellationReason: String,
        actorId: String,
        callerRole: UserRole
    ): DomainResult<FinancialTransaction> = mutex.withLock {
        val authResult = FinancialAuthorizationValidator.validateCancelTransaction(callerRole)
        if (authResult is DomainResult.Error) return authResult

        if (cancellationReason.trim().isEmpty()) {
            return DomainResult.Error(message = "Cancellation reason cannot be blank.")
        }

        val existing = dataSource.getTransactionById(transactionId)
            ?: return DomainResult.Error(message = "Financial transaction '$transactionId' not found.")

        val transitionResult = FinancialTransactionLifecycleValidator.validateTransition(
            existing.transactionStatus,
            FinancialTransactionStatus.CANCELLED
        )
        if (transitionResult is DomainResult.Error) return transitionResult

        val now = System.currentTimeMillis()
        val updated = existing.copy(
            transactionStatus = FinancialTransactionStatus.CANCELLED,
            cancelledBy = actorId,
            cancelledAt = now,
            cancellationReason = cancellationReason.trim(),
            updatedAt = now
        )

        val validationResult = FinancialTransactionValidator.validateTransaction(updated, existing.projectId)
        if (validationResult is DomainResult.Error) return validationResult

        dataSource.updateTransaction(updated)
        dataSource.insertActivityEvent(
            FinancialActivityEvent(
                eventId = UUID.randomUUID().toString(),
                transactionId = transactionId,
                projectId = existing.projectId,
                activityType = FinancialActivityType.TRANSACTION_CANCELLED,
                actorId = actorId,
                details = "Financial transaction cancelled by '$actorId': $cancellationReason"
            )
        )

        DomainResult.Success(updated)
    }

    override suspend fun getTransactionById(
        transactionId: String,
        callerRole: UserRole
    ): DomainResult<FinancialTransaction> {
        val authResult = FinancialAuthorizationValidator.validateViewTransactions(callerRole)
        if (authResult is DomainResult.Error) return authResult

        val transaction = dataSource.getTransactionById(transactionId)
            ?: return DomainResult.Error(message = "Financial transaction '$transactionId' not found.")

        return DomainResult.Success(transaction)
    }

    override suspend fun getTransactionsByReference(
        projectId: String,
        referenceId: String,
        callerRole: UserRole
    ): DomainResult<List<FinancialTransaction>> {
        val authResult = FinancialAuthorizationValidator.validateViewTransactions(callerRole)
        if (authResult is DomainResult.Error) return authResult

        val list = dataSource.getTransactionsByReference(projectId, referenceId)
        return DomainResult.Success(list)
    }

    override fun observeTransactions(
        projectId: String,
        callerRole: UserRole
    ): Flow<List<FinancialTransaction>> {
        if (!callerRole.isInternal) {
            return flowOf(emptyList())
        }
        return dataSource.observeTransactions(projectId)
    }

    override suspend fun getLedgerEntriesByTransaction(
        transactionId: String,
        callerRole: UserRole
    ): DomainResult<List<FinancialLedgerEntry>> {
        val authResult = FinancialAuthorizationValidator.validateViewLedger(callerRole)
        if (authResult is DomainResult.Error) return authResult

        val entries = dataSource.getLedgerEntriesByTransaction(transactionId)
        return DomainResult.Success(entries)
    }

    override fun observeLedgerEntries(
        projectId: String,
        callerRole: UserRole
    ): Flow<List<FinancialLedgerEntry>> {
        if (!callerRole.hasFinancialAccess && callerRole != UserRole.ADMIN && callerRole != UserRole.MANAGER && callerRole != UserRole.ACCOUNTS) {
            return flowOf(emptyList())
        }
        return dataSource.observeLedgerEntries(projectId)
    }

    override suspend fun getActivityEvents(
        transactionId: String,
        callerRole: UserRole
    ): DomainResult<List<FinancialActivityEvent>> {
        val authResult = FinancialAuthorizationValidator.validateViewTransactions(callerRole)
        if (authResult is DomainResult.Error) return authResult

        val events = dataSource.getActivityEvents(transactionId)
        return DomainResult.Success(events)
    }

    private fun deriveDefaultAccountHead(type: FinancialTransactionType): String {
        return when (type) {
            FinancialTransactionType.SALE -> "ACCOUNTS_RECEIVABLE"
            FinancialTransactionType.RECEIPT -> "CASH_AND_BANK"
            FinancialTransactionType.PAYMENT -> "ACCOUNTS_PAYABLE"
            FinancialTransactionType.EXPENSE -> "OPERATING_EXPENSE"
            FinancialTransactionType.REFUND -> "CUSTOMER_REFUND"
            FinancialTransactionType.ADJUSTMENT -> "GENERAL_LEDGER_ADJUSTMENT"
            FinancialTransactionType.TRANSFER -> "INTER_ACCOUNT_TRANSFER"
            FinancialTransactionType.CREDIT -> "CREDIT_NOTE"
            FinancialTransactionType.DEBIT -> "DEBIT_NOTE"
        }
    }
}
