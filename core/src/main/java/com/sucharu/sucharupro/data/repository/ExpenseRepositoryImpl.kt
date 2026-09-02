package com.sucharu.sucharupro.data.repository

import com.sucharu.sucharupro.data.datasource.ExpenseCategoryDataSource
import com.sucharu.sucharupro.data.datasource.ExpenseDataSource
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.common.Money
import com.sucharu.sucharupro.domain.model.finance.Expense
import com.sucharu.sucharupro.domain.model.finance.ExpenseActivityEvent
import com.sucharu.sucharupro.domain.model.finance.ExpenseActivityType
import com.sucharu.sucharupro.domain.model.finance.ExpenseCategoryBreakdown
import com.sucharu.sucharupro.domain.model.finance.ExpensePaymentMethod
import com.sucharu.sucharupro.domain.model.finance.ExpenseStatus
import com.sucharu.sucharupro.domain.model.finance.ExpenseSummary
import com.sucharu.sucharupro.domain.model.finance.FinancialEntryType
import com.sucharu.sucharupro.domain.model.finance.FinancialReferenceType
import com.sucharu.sucharupro.domain.model.finance.FinancialTransactionType
import com.sucharu.sucharupro.domain.model.user.UserRole
import com.sucharu.sucharupro.domain.repository.ExpenseRepository
import com.sucharu.sucharupro.domain.repository.FinancialTransactionRepository
import com.sucharu.sucharupro.domain.validation.ExpenseAuthorizationValidator
import com.sucharu.sucharupro.domain.validation.ExpenseLifecycleValidator
import com.sucharu.sucharupro.domain.validation.ExpenseValidator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.UUID

/**
 * Thread-safe implementation of ExpenseRepository with non-reentrant mutex locking (Module 09 Step 06).
 */
class ExpenseRepositoryImpl(
    private val expenseDataSource: ExpenseDataSource,
    private val categoryDataSource: ExpenseCategoryDataSource,
    private val financialTransactionRepository: FinancialTransactionRepository
) : ExpenseRepository {

    private val mutex = Mutex()

    override suspend fun createExpense(
        projectId: String,
        categoryId: String,
        amount: Money,
        currency: String,
        description: String,
        paymentMethod: ExpensePaymentMethod,
        paymentReference: String?,
        vendorId: String?,
        referenceType: FinancialReferenceType?,
        referenceId: String?,
        expenseDate: Long,
        notes: String?,
        idempotencyKey: String?,
        actorId: String,
        callerRole: UserRole
    ): DomainResult<Expense> = mutex.withLock {
        val authResult = ExpenseAuthorizationValidator.validateCreateDraftExpense(callerRole)
        if (authResult is DomainResult.Error) return@withLock authResult

        // Idempotency check
        if (!idempotencyKey.isNullOrBlank()) {
            val existingIdempotent = expenseDataSource.getExpenseByIdempotencyKey(projectId, idempotencyKey.trim())
            if (existingIdempotent != null) {
                return@withLock DomainResult.Success(existingIdempotent)
            }
        }

        val valResult = ExpenseValidator.validateCreatePayload(
            projectId = projectId,
            categoryId = categoryId,
            amount = amount,
            currency = currency,
            description = description,
            paymentMethod = paymentMethod,
            paymentReference = paymentReference,
            referenceType = referenceType,
            referenceId = referenceId,
            expenseDate = expenseDate,
            actorId = actorId
        )
        if (valResult is DomainResult.Error) return@withLock valResult

        val category = categoryDataSource.getCategoryById(categoryId)
            ?: return@withLock DomainResult.Error(message = "Expense category '$categoryId' not found.")

        val categoryCheck = ExpenseValidator.validateCategoryCompatibility(category, projectId)
        if (categoryCheck is DomainResult.Error) return@withLock categoryCheck

        val expenseId = UUID.randomUUID().toString()
        val expenseNo = expenseDataSource.generateNextExpenseNo(projectId)
        val now = System.currentTimeMillis()
        val initialStatus = if (callerRole == UserRole.STAFF) ExpenseStatus.DRAFT else ExpenseStatus.PENDING

        val expense = Expense(
            expenseId = expenseId,
            expenseNo = expenseNo,
            projectId = projectId,
            categoryId = categoryId,
            vendorId = vendorId?.trim(),
            referenceType = referenceType,
            referenceId = referenceId?.trim(),
            amount = amount,
            currency = currency,
            expenseDate = expenseDate,
            description = description.trim(),
            notes = notes?.trim(),
            paymentMethod = paymentMethod,
            paymentReference = paymentReference?.trim(),
            status = initialStatus,
            financialTransactionId = null,
            idempotencyKey = idempotencyKey?.trim(),
            createdBy = actorId,
            createdAt = now,
            updatedAt = now
        )

        val inserted = expenseDataSource.insertExpense(expense)
        if (!inserted) {
            return@withLock DomainResult.Error(message = "Failed to insert expense record.")
        }

        expenseDataSource.insertActivityEvent(
            ExpenseActivityEvent(
                eventId = UUID.randomUUID().toString(),
                expenseId = expenseId,
                projectId = projectId,
                activityType = ExpenseActivityType.EXPENSE_CREATED,
                actorId = actorId,
                details = "Expense #$expenseNo created under category '${category.categoryName}' for ${amount.formatted()} $currency."
            )
        )

        DomainResult.Success(expense)
    }

    override suspend fun updateDraftExpense(
        expenseId: String,
        categoryId: String?,
        amount: Money?,
        description: String?,
        paymentMethod: ExpensePaymentMethod?,
        paymentReference: String?,
        vendorId: String?,
        referenceType: FinancialReferenceType?,
        referenceId: String?,
        expenseDate: Long?,
        notes: String?,
        actorId: String,
        callerRole: UserRole
    ): DomainResult<Expense> = mutex.withLock {
        val authResult = ExpenseAuthorizationValidator.validateUpdateDraft(callerRole)
        if (authResult is DomainResult.Error) return@withLock authResult

        val existing = expenseDataSource.getExpenseById(expenseId)
            ?: return@withLock DomainResult.Error(message = "Expense '$expenseId' not found.")

        if (existing.status != ExpenseStatus.DRAFT) {
            return@withLock DomainResult.Error(
                message = "Only DRAFT expenses can be edited. Current status: ${existing.status.name}."
            )
        }

        val updatedCategoryId = categoryId ?: existing.categoryId
        if (categoryId != null) {
            val cat = categoryDataSource.getCategoryById(categoryId)
                ?: return@withLock DomainResult.Error(message = "Target category '$categoryId' not found.")
            val catCheck = ExpenseValidator.validateCategoryCompatibility(cat, existing.projectId)
            if (catCheck is DomainResult.Error) return@withLock catCheck
        }

        val updatedMethod = paymentMethod ?: existing.paymentMethod
        val updatedRef = paymentReference ?: existing.paymentReference
        if (updatedMethod.requiresReference && updatedRef.isNullOrBlank()) {
            return@withLock DomainResult.Error(
                message = "Payment reference is required for payment method '${updatedMethod.defaultLabel}'."
            )
        }

        val updated = existing.copy(
            categoryId = updatedCategoryId,
            amount = amount ?: existing.amount,
            description = description?.trim() ?: existing.description,
            paymentMethod = updatedMethod,
            paymentReference = updatedRef?.trim(),
            vendorId = vendorId?.trim() ?: existing.vendorId,
            referenceType = referenceType ?: existing.referenceType,
            referenceId = referenceId?.trim() ?: existing.referenceId,
            expenseDate = expenseDate ?: existing.expenseDate,
            notes = notes?.trim() ?: existing.notes,
            updatedBy = actorId,
            updatedAt = System.currentTimeMillis()
        )

        expenseDataSource.updateExpense(updated)

        expenseDataSource.insertActivityEvent(
            ExpenseActivityEvent(
                eventId = UUID.randomUUID().toString(),
                expenseId = expenseId,
                projectId = existing.projectId,
                activityType = ExpenseActivityType.EXPENSE_UPDATED,
                actorId = actorId,
                details = "Draft expense #${existing.expenseNo} updated."
            )
        )

        DomainResult.Success(updated)
    }

    override suspend fun submitExpense(
        expenseId: String,
        actorId: String,
        callerRole: UserRole
    ): DomainResult<Expense> = mutex.withLock {
        val authResult = ExpenseAuthorizationValidator.validateSubmitExpense(callerRole)
        if (authResult is DomainResult.Error) return@withLock authResult

        val existing = expenseDataSource.getExpenseById(expenseId)
            ?: return@withLock DomainResult.Error(message = "Expense '$expenseId' not found.")

        val transitionResult = ExpenseLifecycleValidator.validateTransition(
            existing.status,
            ExpenseStatus.PENDING
        )
        if (transitionResult is DomainResult.Error) return@withLock transitionResult

        val now = System.currentTimeMillis()
        val updated = existing.copy(
            status = ExpenseStatus.PENDING,
            submittedBy = actorId,
            submittedAt = now,
            updatedBy = actorId,
            updatedAt = now
        )

        expenseDataSource.updateExpense(updated)

        expenseDataSource.insertActivityEvent(
            ExpenseActivityEvent(
                eventId = UUID.randomUUID().toString(),
                expenseId = expenseId,
                projectId = existing.projectId,
                activityType = ExpenseActivityType.EXPENSE_SUBMITTED,
                actorId = actorId,
                details = "Expense #${existing.expenseNo} submitted for approval."
            )
        )

        DomainResult.Success(updated)
    }

    override suspend fun approveExpense(
        expenseId: String,
        actorId: String,
        callerRole: UserRole
    ): DomainResult<Expense> = mutex.withLock {
        val existing = expenseDataSource.getExpenseById(expenseId)
            ?: return@withLock DomainResult.Error(message = "Expense '$expenseId' not found.")

        val authResult = ExpenseAuthorizationValidator.validateApproveExpense(
            callerRole = callerRole,
            creatorId = existing.createdBy,
            approverId = actorId
        )
        if (authResult is DomainResult.Error) return@withLock authResult

        val transitionResult = ExpenseLifecycleValidator.validateTransition(
            existing.status,
            ExpenseStatus.APPROVED
        )
        if (transitionResult is DomainResult.Error) return@withLock transitionResult

        val now = System.currentTimeMillis()
        val updated = existing.copy(
            status = ExpenseStatus.APPROVED,
            approvedBy = actorId,
            approvedAt = now,
            updatedBy = actorId,
            updatedAt = now
        )

        expenseDataSource.updateExpense(updated)

        expenseDataSource.insertActivityEvent(
            ExpenseActivityEvent(
                eventId = UUID.randomUUID().toString(),
                expenseId = expenseId,
                projectId = existing.projectId,
                activityType = ExpenseActivityType.EXPENSE_APPROVED,
                actorId = actorId,
                details = "Expense #${existing.expenseNo} approved."
            )
        )

        DomainResult.Success(updated)
    }

    override suspend fun rejectExpense(
        expenseId: String,
        rejectionReason: String,
        actorId: String,
        callerRole: UserRole
    ): DomainResult<Expense> = mutex.withLock {
        val authResult = ExpenseAuthorizationValidator.validateRejectExpense(callerRole)
        if (authResult is DomainResult.Error) return@withLock authResult

        if (rejectionReason.isBlank()) {
            return@withLock DomainResult.Error(message = "Rejection reason cannot be blank.")
        }

        val existing = expenseDataSource.getExpenseById(expenseId)
            ?: return@withLock DomainResult.Error(message = "Expense '$expenseId' not found.")

        val transitionResult = ExpenseLifecycleValidator.validateTransition(
            existing.status,
            ExpenseStatus.REJECTED
        )
        if (transitionResult is DomainResult.Error) return@withLock transitionResult

        val now = System.currentTimeMillis()
        val updated = existing.copy(
            status = ExpenseStatus.REJECTED,
            rejectedBy = actorId,
            rejectedAt = now,
            cancellationReason = rejectionReason.trim(),
            updatedBy = actorId,
            updatedAt = now
        )

        expenseDataSource.updateExpense(updated)

        expenseDataSource.insertActivityEvent(
            ExpenseActivityEvent(
                eventId = UUID.randomUUID().toString(),
                expenseId = expenseId,
                projectId = existing.projectId,
                activityType = ExpenseActivityType.EXPENSE_REJECTED,
                actorId = actorId,
                details = "Expense #${existing.expenseNo} rejected. Reason: ${rejectionReason.trim()}"
            )
        )

        DomainResult.Success(updated)
    }

    override suspend fun cancelExpense(
        expenseId: String,
        cancellationReason: String,
        actorId: String,
        callerRole: UserRole
    ): DomainResult<Expense> = mutex.withLock {
        val authResult = ExpenseAuthorizationValidator.validateCancelExpense(callerRole)
        if (authResult is DomainResult.Error) return@withLock authResult

        if (cancellationReason.isBlank()) {
            return@withLock DomainResult.Error(message = "Cancellation reason cannot be blank.")
        }

        val existing = expenseDataSource.getExpenseById(expenseId)
            ?: return@withLock DomainResult.Error(message = "Expense '$expenseId' not found.")

        val transitionResult = ExpenseLifecycleValidator.validateTransition(
            existing.status,
            ExpenseStatus.CANCELLED
        )
        if (transitionResult is DomainResult.Error) return@withLock transitionResult

        val now = System.currentTimeMillis()
        val updated = existing.copy(
            status = ExpenseStatus.CANCELLED,
            cancelledBy = actorId,
            cancelledAt = now,
            cancellationReason = cancellationReason.trim(),
            updatedBy = actorId,
            updatedAt = now
        )

        expenseDataSource.updateExpense(updated)

        expenseDataSource.insertActivityEvent(
            ExpenseActivityEvent(
                eventId = UUID.randomUUID().toString(),
                expenseId = expenseId,
                projectId = existing.projectId,
                activityType = ExpenseActivityType.EXPENSE_CANCELLED,
                actorId = actorId,
                details = "Expense #${existing.expenseNo} cancelled. Reason: ${cancellationReason.trim()}"
            )
        )

        DomainResult.Success(updated)
    }

    override suspend fun postExpense(
        expenseId: String,
        overrideAccountHead: String?,
        actorId: String,
        callerRole: UserRole
    ): DomainResult<Expense> = mutex.withLock {
        val existing = expenseDataSource.getExpenseById(expenseId)
            ?: return@withLock DomainResult.Error(message = "Expense '$expenseId' not found.")

        if (existing.status.isTerminal) {
            return@withLock DomainResult.Error(
                message = "Terminal expense '${existing.expenseNo}' (${existing.status.name}) cannot be posted to financial ledger."
            )
        }

        // Separation of duties check
        val authResult = ExpenseAuthorizationValidator.validatePostExpense(
            callerRole = callerRole,
            creatorId = existing.createdBy,
            posterId = actorId
        )
        if (authResult is DomainResult.Error) return@withLock authResult

        val transitionResult = ExpenseLifecycleValidator.validateTransition(
            existing.status,
            ExpenseStatus.POSTED
        )
        if (transitionResult is DomainResult.Error) return@withLock transitionResult

        val category = categoryDataSource.getCategoryById(existing.categoryId)
        val resolvedAccountHead = overrideAccountHead?.trim()
            ?: category?.accountHead
            ?: "OFFICE_EXPENSE"

        // Create & Post Canonical Step 01 Financial Transaction
        val txnCreateRes = financialTransactionRepository.createTransaction(
            projectId = existing.projectId,
            transactionType = FinancialTransactionType.EXPENSE,
            entryType = FinancialEntryType.DEBIT,
            amount = existing.amount,
            currency = existing.currency,
            referenceType = FinancialReferenceType.EXPENSE,
            referenceId = existing.expenseId,
            vendorId = existing.vendorId,
            description = "Operational expense disbursement: ${existing.description} (${existing.paymentMethod.defaultLabel})",
            notes = existing.notes,
            actorId = existing.createdBy,
            callerRole = callerRole
        )
        if (txnCreateRes is DomainResult.Error) return@withLock txnCreateRes
        val financialTxn = (txnCreateRes as DomainResult.Success).data

        // Submit & Post Financial Transaction
        financialTransactionRepository.submitTransaction(financialTxn.transactionId, existing.createdBy, callerRole)
        val txnPostRes = financialTransactionRepository.postTransaction(
            transactionId = financialTxn.transactionId,
            accountHead = resolvedAccountHead,
            actorId = actorId,
            callerRole = callerRole
        )
        if (txnPostRes is DomainResult.Error) return@withLock txnPostRes

        val now = System.currentTimeMillis()
        val postedExpense = existing.copy(
            status = ExpenseStatus.POSTED,
            financialTransactionId = financialTxn.transactionId,
            approvedBy = existing.approvedBy ?: actorId,
            approvedAt = existing.approvedAt ?: now,
            postedAt = now,
            updatedBy = actorId,
            updatedAt = now
        )

        expenseDataSource.updateExpense(postedExpense)

        expenseDataSource.insertActivityEvent(
            ExpenseActivityEvent(
                eventId = UUID.randomUUID().toString(),
                expenseId = existing.expenseId,
                projectId = existing.projectId,
                activityType = ExpenseActivityType.EXPENSE_POSTED,
                actorId = actorId,
                details = "Expense #${existing.expenseNo} posted to financial ledger (#${financialTxn.transactionNo}) under account head '$resolvedAccountHead'."
            )
        )

        DomainResult.Success(postedExpense)
    }

    override suspend fun getExpenseById(
        expenseId: String,
        callerRole: UserRole
    ): DomainResult<Expense> = mutex.withLock {
        val authResult = ExpenseAuthorizationValidator.validateViewExpenses(callerRole)
        if (authResult is DomainResult.Error) return@withLock authResult

        val expense = expenseDataSource.getExpenseById(expenseId)
            ?: return@withLock DomainResult.Error(message = "Expense '$expenseId' not found.")

        DomainResult.Success(expense)
    }

    override suspend fun getExpenseByNumber(
        projectId: String,
        expenseNo: String,
        callerRole: UserRole
    ): DomainResult<Expense> = mutex.withLock {
        val authResult = ExpenseAuthorizationValidator.validateViewExpenses(callerRole)
        if (authResult is DomainResult.Error) return@withLock authResult

        val expense = expenseDataSource.getExpenseByNumber(projectId, expenseNo)
            ?: return@withLock DomainResult.Error(message = "Expense '#$expenseNo' not found in project '$projectId'.")

        DomainResult.Success(expense)
    }

    override suspend fun getExpenseByIdempotencyKey(
        projectId: String,
        idempotencyKey: String,
        callerRole: UserRole
    ): DomainResult<Expense?> = mutex.withLock {
        val authResult = ExpenseAuthorizationValidator.validateViewExpenses(callerRole)
        if (authResult is DomainResult.Error) return@withLock authResult

        val expense = expenseDataSource.getExpenseByIdempotencyKey(projectId, idempotencyKey.trim())
        DomainResult.Success(expense)
    }

    override fun observeExpenses(
        projectId: String,
        callerRole: UserRole
    ): Flow<List<Expense>> {
        val authResult = ExpenseAuthorizationValidator.validateViewExpenses(callerRole)
        if (authResult is DomainResult.Error) return emptyFlow()
        return expenseDataSource.observeExpenses(projectId)
    }

    override fun observeExpensesByCategory(
        projectId: String,
        categoryId: String,
        callerRole: UserRole
    ): Flow<List<Expense>> {
        val authResult = ExpenseAuthorizationValidator.validateViewExpenses(callerRole)
        if (authResult is DomainResult.Error) return emptyFlow()
        return expenseDataSource.observeExpensesByCategory(projectId, categoryId)
    }

    override suspend fun getExpenseSummary(
        projectId: String,
        startDate: Long?,
        endDate: Long?,
        callerRole: UserRole
    ): DomainResult<ExpenseSummary> = mutex.withLock {
        val authResult = ExpenseAuthorizationValidator.validateViewExpenses(callerRole)
        if (authResult is DomainResult.Error) return@withLock authResult

        val expenses = expenseDataSource.observeExpenses(projectId).first().filter { expense ->
            (startDate == null || expense.expenseDate >= startDate) &&
                    (endDate == null || expense.expenseDate <= endDate)
        }

        var total = Money.ZERO
        var posted = Money.ZERO
        var pending = Money.ZERO
        var approved = Money.ZERO
        var draft = Money.ZERO
        var cancelled = Money.ZERO

        var postedCount = 0
        var pendingCount = 0

        val categoryMap = mutableMapOf<String, Pair<Money, Int>>()
        val categories = categoryDataSource.getCategoriesByProject(projectId)

        expenses.forEach { exp ->
            total += exp.amount
            when (exp.status) {
                ExpenseStatus.POSTED -> {
                    posted += exp.amount
                    postedCount++
                }
                ExpenseStatus.PENDING -> {
                    pending += exp.amount
                    pendingCount++
                }
                ExpenseStatus.APPROVED -> approved += exp.amount
                ExpenseStatus.DRAFT -> draft += exp.amount
                ExpenseStatus.CANCELLED,
                ExpenseStatus.REJECTED -> cancelled += exp.amount
            }

            val current = categoryMap[exp.categoryId] ?: (Money.ZERO to 0)
            categoryMap[exp.categoryId] = (current.first + exp.amount) to (current.second + 1)
        }

        val breakdowns = categoryMap.map { (catId, pair) ->
            val catName = categories.firstOrNull { it.categoryId == catId }?.categoryName ?: "Uncategorized"
            ExpenseCategoryBreakdown(
                categoryId = catId,
                categoryName = catName,
                totalAmount = pair.first,
                expenseCount = pair.second
            )
        }

        val summary = ExpenseSummary(
            projectId = projectId,
            totalExpenses = total,
            postedExpenses = posted,
            pendingExpenses = pending,
            approvedExpenses = approved,
            draftExpenses = draft,
            cancelledExpenses = cancelled,
            totalCount = expenses.size,
            postedCount = postedCount,
            pendingCount = pendingCount,
            categoryBreakdowns = breakdowns
        )

        DomainResult.Success(summary)
    }

    override suspend fun getActivityEvents(
        expenseId: String,
        callerRole: UserRole
    ): DomainResult<List<ExpenseActivityEvent>> = mutex.withLock {
        val authResult = ExpenseAuthorizationValidator.validateViewExpenses(callerRole)
        if (authResult is DomainResult.Error) return@withLock authResult

        val events = expenseDataSource.getActivityEvents(expenseId)
        DomainResult.Success(events)
    }
}
