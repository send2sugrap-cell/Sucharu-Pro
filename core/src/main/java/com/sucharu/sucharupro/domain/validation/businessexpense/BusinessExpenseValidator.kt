package com.sucharu.sucharupro.domain.validation.businessexpense

import com.sucharu.sucharupro.domain.model.businessexpense.BusinessExpense
import com.sucharu.sucharupro.domain.model.businessexpense.BusinessExpenseCategory
import com.sucharu.sucharupro.domain.model.businessexpense.BusinessExpensePaymentMethod
import com.sucharu.sucharupro.domain.model.businessexpense.BusinessExpenseStatus
import com.sucharu.sucharupro.domain.model.common.DomainResult
import java.math.BigDecimal

/**
 * Domain validator enforcing business rules, financial precision, lifecycle integrity,
 * and separation of duties for Business Expenses (Module 15 Step 01).
 */
object BusinessExpenseValidator {

    fun validateCreatePayload(
        tenantId: String,
        projectId: String,
        categoryId: String,
        amount: BigDecimal,
        currency: String,
        expenseDate: Long,
        paymentMethod: BusinessExpensePaymentMethod,
        paymentReference: String?,
        description: String,
        createdBy: String
    ): DomainResult<Unit> {
        if (tenantId.isBlank()) return DomainResult.Error(message = "Tenant ID cannot be blank.")
        if (projectId.isBlank()) return DomainResult.Error(message = "Project ID cannot be blank.")
        if (categoryId.isBlank()) return DomainResult.Error(message = "Category ID cannot be blank.")
        if (createdBy.isBlank()) return DomainResult.Error(message = "Created by cannot be blank.")
        if (description.isBlank()) return DomainResult.Error(message = "Expense description cannot be blank.")

        if (amount <= BigDecimal.ZERO) {
            return DomainResult.Error(message = "Expense amount must be strictly greater than zero.")
        }
        if (amount.scale() > 4) {
            return DomainResult.Error(message = "Expense amount cannot have more than 4 decimal places of precision.")
        }

        if (currency.length != 3 || !currency.all { it.isUpperCase() }) {
            return DomainResult.Error(message = "Currency code must be a 3-letter uppercase ISO currency code (e.g. 'BDT'). Provided: '$currency'")
        }

        if (expenseDate <= 0L) {
            return DomainResult.Error(message = "Expense date must be a valid positive timestamp.")
        }

        val maxFutureAllowed = System.currentTimeMillis() + (30L * 24 * 60 * 60 * 1000L) // 30 days ahead max
        if (expenseDate > maxFutureAllowed) {
            return DomainResult.Error(message = "Expense date cannot be more than 30 days in the future.")
        }

        if (paymentMethod.requiresReference && paymentReference.isNullOrBlank()) {
            return DomainResult.Error(message = "Payment reference is required for payment method '${paymentMethod.name}'.")
        }

        return DomainResult.Success(Unit)
    }

    fun validateCategoryCompatibility(
        category: BusinessExpenseCategory?,
        tenantId: String,
        projectId: String
    ): DomainResult<Unit> {
        if (category == null) {
            return DomainResult.Error(message = "Expense category not found.")
        }
        if (!category.isActive) {
            return DomainResult.Error(message = "Expense category '${category.name}' (${category.code}) is inactive.")
        }
        if (category.tenantId != tenantId || category.projectId != projectId) {
            return DomainResult.Error(message = "Expense category belongs to a different tenant/project scope.")
        }
        return DomainResult.Success(Unit)
    }

    fun validateUpdateDraft(expense: BusinessExpense): DomainResult<Unit> {
        if (expense.status != BusinessExpenseStatus.DRAFT && expense.status != BusinessExpenseStatus.REJECTED) {
            return DomainResult.Error(message = "Only DRAFT or REJECTED expenses can be edited. Current status: ${expense.status.name}")
        }
        return DomainResult.Success(Unit)
    }

    fun validateSubmit(expense: BusinessExpense): DomainResult<Unit> {
        if (expense.status != BusinessExpenseStatus.DRAFT && expense.status != BusinessExpenseStatus.REJECTED) {
            return DomainResult.Error(message = "Only DRAFT or REJECTED expenses can be submitted for approval. Current status: ${expense.status.name}")
        }
        return DomainResult.Success(Unit)
    }

    fun validateApprove(
        expense: BusinessExpense,
        actorId: String,
        isSuperAdmin: Boolean = false
    ): DomainResult<Unit> {
        if (expense.status != BusinessExpenseStatus.SUBMITTED) {
            return DomainResult.Error(message = "Only SUBMITTED expenses can be approved. Current status: ${expense.status.name}")
        }
        // Separation of Duties
        if (!isSuperAdmin && expense.createdBy == actorId) {
            return DomainResult.Error(message = "Separation of duties violation: The expense creator ($actorId) cannot approve their own expense.")
        }
        return DomainResult.Success(Unit)
    }

    fun validateReject(
        expense: BusinessExpense,
        reason: String
    ): DomainResult<Unit> {
        if (expense.status != BusinessExpenseStatus.SUBMITTED) {
            return DomainResult.Error(message = "Only SUBMITTED expenses can be rejected. Current status: ${expense.status.name}")
        }
        if (reason.isBlank()) {
            return DomainResult.Error(message = "A non-blank reason is required to reject an expense.")
        }
        return DomainResult.Success(Unit)
    }

    fun validateCancel(
        expense: BusinessExpense,
        reason: String
    ): DomainResult<Unit> {
        if (expense.status.isTerminal) {
            return DomainResult.Error(message = "Cannot cancel an expense in terminal status: ${expense.status.name}")
        }
        if (reason.isBlank()) {
            return DomainResult.Error(message = "A non-blank reason is required to cancel an expense.")
        }
        return DomainResult.Success(Unit)
    }

    fun validateStatusTransition(
        current: BusinessExpenseStatus,
        target: BusinessExpenseStatus
    ): DomainResult<Unit> {
        if (!current.canTransitionTo(target)) {
            return DomainResult.Error(message = "Invalid expense status transition from ${current.name} to ${target.name}.")
        }
        return DomainResult.Success(Unit)
    }
}
