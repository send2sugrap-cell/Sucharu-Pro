package com.sucharu.sucharupro.domain.model.finance

import com.sucharu.sucharupro.domain.model.common.Money

/**
 * Core aggregate root representing an operational business expense (Module 09 Step 06).
 */
data class Expense(
    val expenseId: String,
    val expenseNo: String,
    val projectId: String,
    val categoryId: String,
    val vendorId: String? = null,
    val referenceType: FinancialReferenceType? = null,
    val referenceId: String? = null,
    val amount: Money,
    val currency: String = "BDT",
    val expenseDate: Long = System.currentTimeMillis(),
    val description: String,
    val notes: String? = null,
    val paymentMethod: ExpensePaymentMethod,
    val paymentReference: String? = null,
    val status: ExpenseStatus = ExpenseStatus.DRAFT,
    val financialTransactionId: String? = null,
    val idempotencyKey: String? = null,
    val createdBy: String,
    val updatedBy: String? = null,
    val submittedBy: String? = null,
    val approvedBy: String? = null,
    val rejectedBy: String? = null,
    val cancelledBy: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val submittedAt: Long? = null,
    val approvedAt: Long? = null,
    val rejectedAt: Long? = null,
    val cancelledAt: Long? = null,
    val postedAt: Long? = null,
    val cancellationReason: String? = null
) {
    init {
        require(expenseId.isNotBlank()) { "Expense ID cannot be blank." }
        require(expenseNo.isNotBlank()) { "Expense Number cannot be blank." }
        require(projectId.isNotBlank()) { "Project ID cannot be blank." }
        require(categoryId.isNotBlank()) { "Category ID cannot be blank." }
        require(description.isNotBlank()) { "Expense description cannot be blank." }
        require(createdBy.isNotBlank()) { "Created By cannot be blank." }
        require(amount.isPositive()) { "Expense amount must be strictly positive (> 0)." }
        require(currency.length == 3 && currency.all { it.isUpperCase() }) {
            "Currency code must be a 3-letter uppercase string (e.g. 'BDT'). Provided: '$currency'"
        }
        if (paymentMethod.requiresReference) {
            require(!paymentReference.isNullOrBlank()) {
                "Payment reference is required for payment method '${paymentMethod.defaultLabel}'."
            }
        }
        if (referenceType != null) {
            require(!referenceId.isNullOrBlank()) {
                "Reference ID cannot be blank when reference type is specified."
            }
        }
        require(expenseDate > 0) { "Expense date must be a valid positive timestamp." }
        require(createdAt > 0) { "Creation timestamp must be positive." }
        require(updatedAt >= createdAt) { "Updated timestamp cannot precede creation timestamp." }
    }
}
