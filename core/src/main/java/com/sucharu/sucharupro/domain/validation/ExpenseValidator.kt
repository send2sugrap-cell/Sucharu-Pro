package com.sucharu.sucharupro.domain.validation

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.common.Money
import com.sucharu.sucharupro.domain.model.finance.ExpenseCategory
import com.sucharu.sucharupro.domain.model.finance.ExpensePaymentMethod
import com.sucharu.sucharupro.domain.model.finance.FinancialReferenceType

/**
 * Domain invariants and payload validation for Expenses (Module 09 Step 06).
 */
object ExpenseValidator {

    fun validateCreatePayload(
        projectId: String,
        categoryId: String,
        amount: Money,
        currency: String,
        description: String,
        paymentMethod: ExpensePaymentMethod,
        paymentReference: String?,
        referenceType: FinancialReferenceType?,
        referenceId: String?,
        expenseDate: Long,
        actorId: String
    ): DomainResult<Unit> {
        if (projectId.isBlank()) return DomainResult.Error(message = "Project ID cannot be blank.")
        if (categoryId.isBlank()) return DomainResult.Error(message = "Category ID cannot be blank.")
        if (description.isBlank()) return DomainResult.Error(message = "Expense description cannot be blank.")
        if (actorId.isBlank()) return DomainResult.Error(message = "Actor ID cannot be blank.")

        if (!amount.isPositive()) {
            return DomainResult.Error(message = "Expense amount must be strictly greater than zero.")
        }

        if (currency.length != 3 || !currency.all { it.isUpperCase() }) {
            return DomainResult.Error(message = "Currency code must be a 3-letter uppercase string (e.g. 'BDT'). Provided: '$currency'")
        }

        if (paymentMethod.requiresReference && paymentReference.isNullOrBlank()) {
            return DomainResult.Error(
                message = "Payment reference (e.g. Cheque No, EFT Trx ID, bKash Trx ID) is required for payment method '${paymentMethod.defaultLabel}'."
            )
        }

        if (referenceType != null && referenceId.isNullOrBlank()) {
            return DomainResult.Error(message = "Reference ID cannot be blank when reference type '${referenceType.name}' is specified.")
        }

        if (expenseDate <= 0) {
            return DomainResult.Error(message = "Expense date must be a valid positive timestamp.")
        }

        return DomainResult.Success(Unit)
    }

    fun validateCategoryCompatibility(
        category: ExpenseCategory,
        projectId: String
    ): DomainResult<Unit> {
        if (category.projectId != projectId) {
            return DomainResult.Error(
                message = "Expense category '${category.categoryName}' belongs to project '${category.projectId}', not '$projectId'."
            )
        }

        if (!category.isActive) {
            return DomainResult.Error(
                message = "Cannot create expense under inactive category '${category.categoryName}' (${category.categoryCode})."
            )
        }

        return DomainResult.Success(Unit)
    }
}
