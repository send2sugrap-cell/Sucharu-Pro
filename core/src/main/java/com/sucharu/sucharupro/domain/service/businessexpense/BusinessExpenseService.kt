package com.sucharu.sucharupro.domain.service.businessexpense

import com.sucharu.sucharupro.data.api.model.AuthenticatedPrincipal
import com.sucharu.sucharupro.domain.model.businessexpense.BusinessExpense
import com.sucharu.sucharupro.domain.model.businessexpense.BusinessExpenseAuditEvent
import com.sucharu.sucharupro.domain.model.businessexpense.BusinessExpenseCategory
import com.sucharu.sucharupro.domain.model.businessexpense.BusinessExpensePaymentMethod
import com.sucharu.sucharupro.domain.model.businessexpense.BusinessExpenseStatus
import com.sucharu.sucharupro.domain.model.common.DomainResult
import java.math.BigDecimal

data class CreateBusinessExpenseCommand(
    val categoryId: String,
    val amount: BigDecimal,
    val currency: String = "BDT",
    val expenseDate: Long = System.currentTimeMillis(),
    val paymentMethod: BusinessExpensePaymentMethod = BusinessExpensePaymentMethod.CASH,
    val paymentReference: String? = null,
    val vendorId: String? = null,
    val jobId: String? = null,
    val branchId: String? = null,
    val locationId: String? = null,
    val description: String,
    val notes: String? = null,
    val attachmentUrl: String? = null,
    val attachmentMetadata: String? = null,
    val idempotencyKey: String? = null,
    val autoSubmit: Boolean = false
)

data class UpdateBusinessExpenseCommand(
    val categoryId: String? = null,
    val amount: BigDecimal? = null,
    val currency: String? = null,
    val expenseDate: Long? = null,
    val paymentMethod: BusinessExpensePaymentMethod? = null,
    val paymentReference: String? = null,
    val vendorId: String? = null,
    val jobId: String? = null,
    val branchId: String? = null,
    val locationId: String? = null,
    val description: String? = null,
    val notes: String? = null,
    val attachmentUrl: String? = null,
    val attachmentMetadata: String? = null
)

data class BusinessExpenseFilter(
    val status: BusinessExpenseStatus? = null,
    val categoryId: String? = null,
    val vendorId: String? = null,
    val jobId: String? = null,
    val fromDate: Long? = null,
    val toDate: Long? = null
)

data class CreateBusinessExpenseCategoryCommand(
    val name: String,
    val code: String,
    val description: String? = null,
    val sortOrder: Int = 0
)

/**
 * Domain Service interface for Business Expense operations (Module 15 Step 01).
 */
interface BusinessExpenseService {

    suspend fun createExpense(
        principal: AuthenticatedPrincipal,
        command: CreateBusinessExpenseCommand
    ): DomainResult<BusinessExpense>

    suspend fun updateExpenseDraft(
        principal: AuthenticatedPrincipal,
        expenseId: String,
        command: UpdateBusinessExpenseCommand
    ): DomainResult<BusinessExpense>

    suspend fun submitExpense(
        principal: AuthenticatedPrincipal,
        expenseId: String
    ): DomainResult<BusinessExpense>

    suspend fun approveExpense(
        principal: AuthenticatedPrincipal,
        expenseId: String,
        notes: String? = null
    ): DomainResult<BusinessExpense>

    suspend fun rejectExpense(
        principal: AuthenticatedPrincipal,
        expenseId: String,
        reason: String
    ): DomainResult<BusinessExpense>

    suspend fun cancelExpense(
        principal: AuthenticatedPrincipal,
        expenseId: String,
        reason: String
    ): DomainResult<BusinessExpense>

    suspend fun getExpenseById(
        principal: AuthenticatedPrincipal,
        expenseId: String
    ): DomainResult<BusinessExpense>

    suspend fun getExpenseByNumber(
        principal: AuthenticatedPrincipal,
        expenseNumber: String
    ): DomainResult<BusinessExpense>

    suspend fun listExpenses(
        principal: AuthenticatedPrincipal,
        filter: BusinessExpenseFilter = BusinessExpenseFilter(),
        limit: Int = 50,
        offset: Int = 0
    ): DomainResult<List<BusinessExpense>>

    suspend fun countExpenses(
        principal: AuthenticatedPrincipal,
        filter: BusinessExpenseFilter = BusinessExpenseFilter()
    ): DomainResult<Long>

    suspend fun getExpenseAuditTrail(
        principal: AuthenticatedPrincipal,
        expenseId: String
    ): DomainResult<List<BusinessExpenseAuditEvent>>

    suspend fun listCategories(
        principal: AuthenticatedPrincipal,
        activeOnly: Boolean = true
    ): DomainResult<List<BusinessExpenseCategory>>

    suspend fun createCategory(
        principal: AuthenticatedPrincipal,
        command: CreateBusinessExpenseCategoryCommand
    ): DomainResult<BusinessExpenseCategory>
}
