package com.sucharu.sucharupro.businessexpense

import com.sucharu.sucharupro.domain.model.businessexpense.BusinessExpense
import com.sucharu.sucharupro.domain.model.businessexpense.BusinessExpenseCategory
import com.sucharu.sucharupro.domain.model.businessexpense.BusinessExpensePaymentMethod
import com.sucharu.sucharupro.domain.model.businessexpense.BusinessExpenseStatus
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.validation.businessexpense.BusinessExpenseValidator
import org.junit.Assert.*
import org.junit.Test
import java.math.BigDecimal

class BusinessExpenseDomainTest {

    @Test
    fun testExpenseStatusTransitions() {
        // Valid transitions
        assertTrue(BusinessExpenseStatus.DRAFT.canTransitionTo(BusinessExpenseStatus.SUBMITTED))
        assertTrue(BusinessExpenseStatus.DRAFT.canTransitionTo(BusinessExpenseStatus.CANCELLED))
        assertTrue(BusinessExpenseStatus.SUBMITTED.canTransitionTo(BusinessExpenseStatus.APPROVED))
        assertTrue(BusinessExpenseStatus.SUBMITTED.canTransitionTo(BusinessExpenseStatus.REJECTED))
        assertTrue(BusinessExpenseStatus.SUBMITTED.canTransitionTo(BusinessExpenseStatus.CANCELLED))
        assertTrue(BusinessExpenseStatus.APPROVED.canTransitionTo(BusinessExpenseStatus.POSTABLE))
        assertTrue(BusinessExpenseStatus.APPROVED.canTransitionTo(BusinessExpenseStatus.CANCELLED))
        assertTrue(BusinessExpenseStatus.REJECTED.canTransitionTo(BusinessExpenseStatus.DRAFT))
        assertTrue(BusinessExpenseStatus.REJECTED.canTransitionTo(BusinessExpenseStatus.SUBMITTED))
        assertTrue(BusinessExpenseStatus.REJECTED.canTransitionTo(BusinessExpenseStatus.CANCELLED))

        // Invalid transitions
        assertFalse(BusinessExpenseStatus.CANCELLED.canTransitionTo(BusinessExpenseStatus.DRAFT))
        assertFalse(BusinessExpenseStatus.CANCELLED.canTransitionTo(BusinessExpenseStatus.SUBMITTED))
        assertFalse(BusinessExpenseStatus.CANCELLED.canTransitionTo(BusinessExpenseStatus.APPROVED))
        assertFalse(BusinessExpenseStatus.DRAFT.canTransitionTo(BusinessExpenseStatus.APPROVED))
        assertFalse(BusinessExpenseStatus.APPROVED.canTransitionTo(BusinessExpenseStatus.DRAFT))
    }

    @Test
    fun testExpenseValidation_InvalidAmounts() {
        val zeroResult = BusinessExpenseValidator.validateCreatePayload(
            tenantId = "TENANT-1",
            projectId = "PRJ-1",
            categoryId = "CAT-1",
            amount = BigDecimal.ZERO,
            currency = "BDT",
            expenseDate = System.currentTimeMillis(),
            paymentMethod = BusinessExpensePaymentMethod.CASH,
            paymentReference = null,
            description = "Office Supplies",
            createdBy = "USER-1"
        )
        assertTrue(zeroResult is DomainResult.Error)

        val negativeResult = BusinessExpenseValidator.validateCreatePayload(
            tenantId = "TENANT-1",
            projectId = "PRJ-1",
            categoryId = "CAT-1",
            amount = BigDecimal("-50.00"),
            currency = "BDT",
            expenseDate = System.currentTimeMillis(),
            paymentMethod = BusinessExpensePaymentMethod.CASH,
            paymentReference = null,
            description = "Office Supplies",
            createdBy = "USER-1"
        )
        assertTrue(negativeResult is DomainResult.Error)

        val excessivePrecision = BusinessExpenseValidator.validateCreatePayload(
            tenantId = "TENANT-1",
            projectId = "PRJ-1",
            categoryId = "CAT-1",
            amount = BigDecimal("100.12345"), // 5 decimals
            currency = "BDT",
            expenseDate = System.currentTimeMillis(),
            paymentMethod = BusinessExpensePaymentMethod.CASH,
            paymentReference = null,
            description = "Office Supplies",
            createdBy = "USER-1"
        )
        assertTrue(excessivePrecision is DomainResult.Error)
    }

    @Test
    fun testExpenseValidation_PaymentMethodReferences() {
        val bkashNoRef = BusinessExpenseValidator.validateCreatePayload(
            tenantId = "TENANT-1",
            projectId = "PRJ-1",
            categoryId = "CAT-1",
            amount = BigDecimal("500.00"),
            currency = "BDT",
            expenseDate = System.currentTimeMillis(),
            paymentMethod = BusinessExpensePaymentMethod.BKASH,
            paymentReference = "",
            description = "Emergency Transport",
            createdBy = "USER-1"
        )
        assertTrue(bkashNoRef is DomainResult.Error)

        val bkashWithRef = BusinessExpenseValidator.validateCreatePayload(
            tenantId = "TENANT-1",
            projectId = "PRJ-1",
            categoryId = "CAT-1",
            amount = BigDecimal("500.00"),
            currency = "BDT",
            expenseDate = System.currentTimeMillis(),
            paymentMethod = BusinessExpensePaymentMethod.BKASH,
            paymentReference = "TRX-BKASH-9988",
            description = "Emergency Transport",
            createdBy = "USER-1"
        )
        assertTrue(bkashWithRef is DomainResult.Success)
    }

    @Test
    fun testExpenseValidation_CategoryCompatibility() {
        val activeCategory = BusinessExpenseCategory(
            categoryId = "CAT-01",
            tenantId = "TENANT-1",
            projectId = "PRJ-1",
            name = "Office",
            code = "CAT-OFC",
            isActive = true
        )
        val activeRes = BusinessExpenseValidator.validateCategoryCompatibility(activeCategory, "TENANT-1", "PRJ-1")
        assertTrue(activeRes is DomainResult.Success)

        val inactiveCategory = activeCategory.copy(isActive = false)
        val inactiveRes = BusinessExpenseValidator.validateCategoryCompatibility(inactiveCategory, "TENANT-1", "PRJ-1")
        assertTrue(inactiveRes is DomainResult.Error)

        val foreignTenantCategory = activeCategory.copy(tenantId = "TENANT-2")
        val foreignRes = BusinessExpenseValidator.validateCategoryCompatibility(foreignTenantCategory, "TENANT-1", "PRJ-1")
        assertTrue(foreignRes is DomainResult.Error)
    }

    @Test
    fun testExpenseValidation_SeparationOfDuties() {
        val expense = BusinessExpense(
            expenseId = "EXP-01",
            tenantId = "TENANT-1",
            projectId = "PRJ-1",
            expenseNumber = "EXP-20261015-0001",
            expenseCategoryId = "CAT-01",
            amount = BigDecimal("1500.00"),
            currency = "BDT",
            expenseDate = System.currentTimeMillis(),
            paymentMethod = BusinessExpensePaymentMethod.CASH,
            status = BusinessExpenseStatus.SUBMITTED,
            description = "Lunch Catering",
            createdBy = "USER-STAFF-1"
        )

        // Creator cannot approve their own expense
        val selfApprovalRes = BusinessExpenseValidator.validateApprove(expense, actorId = "USER-STAFF-1", isSuperAdmin = false)
        assertTrue(selfApprovalRes is DomainResult.Error)

        // Another manager/approver can approve
        val managerApprovalRes = BusinessExpenseValidator.validateApprove(expense, actorId = "USER-MGR-1", isSuperAdmin = false)
        assertTrue(managerApprovalRes is DomainResult.Success)

        // Superadmin exception
        val adminApprovalRes = BusinessExpenseValidator.validateApprove(expense, actorId = "USER-STAFF-1", isSuperAdmin = true)
        assertTrue(adminApprovalRes is DomainResult.Success)
    }

    @Test
    fun testExpenseValidation_RejectionAndCancellationReasons() {
        val submittedExpense = BusinessExpense(
            expenseId = "EXP-01",
            tenantId = "TENANT-1",
            projectId = "PRJ-1",
            expenseNumber = "EXP-20261015-0001",
            expenseCategoryId = "CAT-01",
            amount = BigDecimal("1500.00"),
            currency = "BDT",
            expenseDate = System.currentTimeMillis(),
            paymentMethod = BusinessExpensePaymentMethod.CASH,
            status = BusinessExpenseStatus.SUBMITTED,
            description = "Lunch Catering",
            createdBy = "USER-STAFF-1"
        )

        // Blank rejection reason rejected
        val blankRejection = BusinessExpenseValidator.validateReject(submittedExpense, "   ")
        assertTrue(blankRejection is DomainResult.Error)

        val validRejection = BusinessExpenseValidator.validateReject(submittedExpense, "Missing vendor receipt attachment")
        assertTrue(validRejection is DomainResult.Success)

        // Blank cancellation reason rejected
        val blankCancellation = BusinessExpenseValidator.validateCancel(submittedExpense, "")
        assertTrue(blankCancellation is DomainResult.Error)

        val validCancellation = BusinessExpenseValidator.validateCancel(submittedExpense, "Duplicate entry created by mistake")
        assertTrue(validCancellation is DomainResult.Success)
    }
}
