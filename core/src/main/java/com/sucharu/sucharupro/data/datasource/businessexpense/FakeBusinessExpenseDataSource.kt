package com.sucharu.sucharupro.data.datasource.businessexpense

import com.sucharu.sucharupro.domain.model.businessexpense.BusinessExpense
import com.sucharu.sucharupro.domain.model.businessexpense.BusinessExpenseAuditEvent
import com.sucharu.sucharupro.domain.model.businessexpense.BusinessExpenseCategory
import com.sucharu.sucharupro.domain.model.businessexpense.BusinessExpenseStatus
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

/**
 * Thread-safe in-memory mock implementation of BusinessExpenseDataSource (Module 15 Step 01).
 */
class FakeBusinessExpenseDataSource : BusinessExpenseDataSource {

    private val expenses = ConcurrentHashMap<String, BusinessExpense>()
    private val categories = ConcurrentHashMap<String, BusinessExpenseCategory>()
    private val auditEvents = ConcurrentHashMap<String, MutableList<BusinessExpenseAuditEvent>>()
    private val sequenceCounter = AtomicInteger(1)

    init {
        seedDefaultCategories("TENANT-001", "PRJ-001")
    }

    fun seedDefaultCategories(tenantId: String, projectId: String) {
        val defaultList = listOf(
            "Transport" to "CAT-TRN",
            "Labour" to "CAT-LBR",
            "Utilities" to "CAT-UTL",
            "Office" to "CAT-OFC",
            "Rent" to "CAT-RNT",
            "Maintenance" to "CAT-MNT",
            "Printing Operations" to "CAT-PRN",
            "Packaging" to "CAT-PKG",
            "Delivery" to "CAT-DLV",
            "Communication" to "CAT-COM",
            "Marketing" to "CAT-MKT",
            "Professional Services" to "CAT-PRF",
            "Miscellaneous" to "CAT-MSC",
            "Other" to "CAT-OTH"
        )
        defaultList.forEachIndexed { index, (name, code) ->
            val catId = "CAT-$tenantId-$projectId-$code"
            if (!categories.containsKey(catId)) {
                categories[catId] = BusinessExpenseCategory(
                    categoryId = catId,
                    tenantId = tenantId,
                    projectId = projectId,
                    name = name,
                    code = code,
                    description = "Default standard $name category",
                    isActive = true,
                    sortOrder = index + 1
                )
            }
        }
    }

    override suspend fun insertExpense(expense: BusinessExpense): Boolean {
        expenses[expense.expenseId] = expense
        return true
    }

    override suspend fun updateExpense(expense: BusinessExpense): Boolean {
        expenses[expense.expenseId] = expense
        return true
    }

    override suspend fun getExpenseById(tenantId: String, projectId: String, expenseId: String): BusinessExpense? {
        val exp = expenses[expenseId] ?: return null
        return if (exp.tenantId == tenantId && exp.projectId == projectId) exp else null
    }

    override suspend fun getExpenseByNumber(tenantId: String, projectId: String, expenseNumber: String): BusinessExpense? {
        return expenses.values.firstOrNull {
            it.tenantId == tenantId && it.projectId == projectId && it.expenseNumber == expenseNumber
        }
    }

    override suspend fun getExpenseByIdempotencyKey(
        tenantId: String,
        projectId: String,
        idempotencyKey: String
    ): BusinessExpense? {
        return expenses.values.firstOrNull {
            it.tenantId == tenantId && it.projectId == projectId && it.idempotencyKey == idempotencyKey
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
        return expenses.values
            .asSequence()
            .filter { it.tenantId == tenantId && it.projectId == projectId }
            .filter { status == null || it.status == status }
            .filter { categoryId == null || it.expenseCategoryId == categoryId }
            .filter { vendorId == null || it.vendorId == vendorId }
            .filter { jobId == null || it.jobId == jobId }
            .filter { fromDate == null || it.expenseDate >= fromDate }
            .filter { toDate == null || it.expenseDate <= toDate }
            .sortedByDescending { it.expenseDate }
            .drop(offset)
            .take(limit)
            .toList()
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
        return expenses.values
            .asSequence()
            .filter { it.tenantId == tenantId && it.projectId == projectId }
            .filter { status == null || it.status == status }
            .filter { categoryId == null || it.expenseCategoryId == categoryId }
            .filter { vendorId == null || it.vendorId == vendorId }
            .filter { jobId == null || it.jobId == jobId }
            .filter { fromDate == null || it.expenseDate >= fromDate }
            .filter { toDate == null || it.expenseDate <= toDate }
            .count()
            .toLong()
    }

    override suspend fun generateNextExpenseNumber(tenantId: String, projectId: String): String {
        val dateStr = SimpleDateFormat("yyyyMMdd", Locale.US).format(Date())
        val seq = sequenceCounter.getAndIncrement()
        return "EXP-$dateStr-${seq.toString().padStart(4, '0')}"
    }

    override suspend fun insertCategory(category: BusinessExpenseCategory): Boolean {
        categories[category.categoryId] = category
        return true
    }

    override suspend fun updateCategory(category: BusinessExpenseCategory): Boolean {
        categories[category.categoryId] = category
        return true
    }

    override suspend fun getCategoryById(tenantId: String, projectId: String, categoryId: String): BusinessExpenseCategory? {
        seedDefaultCategories(tenantId, projectId)
        val cat = categories[categoryId] ?: return null
        return if (cat.tenantId == tenantId && cat.projectId == projectId) cat else null
    }

    override suspend fun getCategoryByCode(tenantId: String, projectId: String, code: String): BusinessExpenseCategory? {
        seedDefaultCategories(tenantId, projectId)
        return categories.values.firstOrNull {
            it.tenantId == tenantId && it.projectId == projectId && it.code.equals(code, ignoreCase = true)
        }
    }

    override suspend fun listCategories(
        tenantId: String,
        projectId: String,
        activeOnly: Boolean
    ): List<BusinessExpenseCategory> {
        seedDefaultCategories(tenantId, projectId)
        return categories.values
            .asSequence()
            .filter { it.tenantId == tenantId && it.projectId == projectId }
            .filter { !activeOnly || it.isActive }
            .sortedBy { it.sortOrder }
            .toList()
    }

    override suspend fun insertAuditEvent(event: BusinessExpenseAuditEvent): Boolean {
        val list = auditEvents.computeIfAbsent(event.expenseId) { mutableListOf() }
        synchronized(list) {
            list.add(event)
        }
        return true
    }

    override suspend fun getAuditEvents(
        tenantId: String,
        projectId: String,
        expenseId: String
    ): List<BusinessExpenseAuditEvent> {
        val list = auditEvents[expenseId] ?: return emptyList()
        synchronized(list) {
            return list
                .filter { it.tenantId == tenantId && it.projectId == projectId }
                .sortedBy { it.timestamp }
                .toList()
        }
    }

    fun clear() {
        expenses.clear()
        categories.clear()
        auditEvents.clear()
        sequenceCounter.set(1)
    }
}
