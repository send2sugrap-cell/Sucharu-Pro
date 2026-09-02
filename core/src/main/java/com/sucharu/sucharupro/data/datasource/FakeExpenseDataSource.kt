package com.sucharu.sucharupro.data.datasource

import com.sucharu.sucharupro.domain.model.finance.Expense
import com.sucharu.sucharupro.domain.model.finance.ExpenseActivityEvent
import com.sucharu.sucharupro.domain.model.finance.FinancialReferenceType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.atomic.AtomicInteger

/**
 * Thread-safe, reactive in-memory implementation of ExpenseDataSource (Module 09 Step 06).
 */
class FakeExpenseDataSource : ExpenseDataSource {

    private val mutex = Mutex()
    private val expenses = LinkedHashMap<String, Expense>()
    private val activityEvents = mutableListOf<ExpenseActivityEvent>()
    private val expensesFlow = MutableStateFlow<List<Expense>>(emptyList())
    private val sequenceCounter = AtomicInteger(1)

    override suspend fun insertExpense(expense: Expense): Boolean = mutex.withLock {
        if (expenses.containsKey(expense.expenseId)) return@withLock false
        expenses[expense.expenseId] = expense
        expensesFlow.value = expenses.values.toList()
        true
    }

    override suspend fun updateExpense(expense: Expense): Boolean = mutex.withLock {
        if (!expenses.containsKey(expense.expenseId)) return@withLock false
        expenses[expense.expenseId] = expense
        expensesFlow.value = expenses.values.toList()
        true
    }

    override suspend fun getExpenseById(expenseId: String): Expense? = mutex.withLock {
        expenses[expenseId]
    }

    override suspend fun getExpenseByNumber(
        projectId: String,
        expenseNo: String
    ): Expense? = mutex.withLock {
        expenses.values.firstOrNull { it.projectId == projectId && it.expenseNo.equals(expenseNo, ignoreCase = true) }
    }

    override suspend fun getExpenseByIdempotencyKey(
        projectId: String,
        idempotencyKey: String
    ): Expense? = mutex.withLock {
        expenses.values.firstOrNull {
            it.projectId == projectId &&
                    it.idempotencyKey != null &&
                    it.idempotencyKey.equals(idempotencyKey, ignoreCase = true)
        }
    }

    override suspend fun getExpensesByReference(
        projectId: String,
        referenceType: FinancialReferenceType,
        referenceId: String
    ): List<Expense> = mutex.withLock {
        expenses.values.filter {
            it.projectId == projectId &&
                    it.referenceType == referenceType &&
                    it.referenceId == referenceId
        }
    }

    override fun observeExpenses(projectId: String): Flow<List<Expense>> {
        return expensesFlow.map { list ->
            list.filter { it.projectId == projectId }
        }
    }

    override fun observeExpensesByCategory(
        projectId: String,
        categoryId: String
    ): Flow<List<Expense>> {
        return expensesFlow.map { list ->
            list.filter { it.projectId == projectId && it.categoryId == categoryId }
        }
    }

    override suspend fun insertActivityEvent(event: ExpenseActivityEvent): Boolean = mutex.withLock {
        activityEvents.add(event)
        true
    }

    override suspend fun getActivityEvents(expenseId: String): List<ExpenseActivityEvent> = mutex.withLock {
        activityEvents.filter { it.expenseId == expenseId }.toList()
    }

    override suspend fun generateNextExpenseNo(projectId: String): String = mutex.withLock {
        val seq = sequenceCounter.getAndIncrement()
        String.format("EXP-%05d", seq)
    }
}
