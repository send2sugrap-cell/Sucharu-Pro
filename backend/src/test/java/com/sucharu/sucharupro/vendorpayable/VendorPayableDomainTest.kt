package com.sucharu.sucharupro.vendorpayable

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.vendorpayable.*
import com.sucharu.sucharupro.domain.validation.vendorpayable.VendorPayableValidator
import org.junit.Assert.*
import org.junit.Test
import java.math.BigDecimal
import java.math.RoundingMode

class VendorPayableDomainTest {

    private val tenantId = "TENANT-001"
    private val projectId = "PRJ-001"
    private val vendorId = "VEND-1001"

    @Test
    fun testValidPayableValidationSucceeds() {
        val res = VendorPayableValidator.validateCreatePayload(
            tenantId = tenantId,
            projectId = projectId,
            vendorId = vendorId,
            originalAmount = BigDecimal("5500.00"),
            currency = "BDT",
            issueDate = System.currentTimeMillis(),
            paymentTerms = VendorPayablePaymentTerms.NET_30,
            customTermDays = null,
            description = "Offset Printing Plates",
            createdBy = "USER-01"
        )
        assertTrue(res is DomainResult.Success)
    }

    @Test
    fun testInvalidAmountsRejected() {
        // Zero amount
        val zeroRes = VendorPayableValidator.validateCreatePayload(
            tenantId = tenantId,
            projectId = projectId,
            vendorId = vendorId,
            originalAmount = BigDecimal("0.00"),
            currency = "BDT",
            issueDate = System.currentTimeMillis(),
            paymentTerms = VendorPayablePaymentTerms.NET_30,
            customTermDays = null,
            description = "Zero bill",
            createdBy = "USER-01"
        )
        assertTrue(zeroRes is DomainResult.Error)

        // Negative amount
        val negRes = VendorPayableValidator.validateCreatePayload(
            tenantId = tenantId,
            projectId = projectId,
            vendorId = vendorId,
            originalAmount = BigDecimal("-500.00"),
            currency = "BDT",
            issueDate = System.currentTimeMillis(),
            paymentTerms = VendorPayablePaymentTerms.NET_30,
            customTermDays = null,
            description = "Negative bill",
            createdBy = "USER-01"
        )
        assertTrue(negRes is DomainResult.Error)

        // > 4 decimal places
        val precisionRes = VendorPayableValidator.validateCreatePayload(
            tenantId = tenantId,
            projectId = projectId,
            vendorId = vendorId,
            originalAmount = BigDecimal("100.12345"),
            currency = "BDT",
            issueDate = System.currentTimeMillis(),
            paymentTerms = VendorPayablePaymentTerms.NET_30,
            customTermDays = null,
            description = "High precision bill",
            createdBy = "USER-01"
        )
        assertTrue(precisionRes is DomainResult.Error)
    }

    @Test
    fun testPaymentTermsDueDateCalculation() {
        val issueDate = 1761868800000L // 2025-11-01 00:00:00 UTC
        val oneDayMillis = 24L * 60L * 60L * 1000L

        val immediateDue = VendorPayablePaymentTerms.IMMEDIATE.calculateDueDate(issueDate)
        assertEquals(issueDate, immediateDue)

        val net30Due = VendorPayablePaymentTerms.NET_30.calculateDueDate(issueDate)
        assertEquals(issueDate + (30L * oneDayMillis), net30Due)

        val customDue = VendorPayablePaymentTerms.CUSTOM.calculateDueDate(issueDate, 42)
        assertEquals(issueDate + (42L * oneDayMillis), customDue)
    }

    @Test
    fun testSeparationOfDutiesValidation() {
        val payable = VendorPayable(
            payableId = "PAY-001",
            tenantId = tenantId,
            projectId = projectId,
            payableNumber = "PAYABLE-20261016-0001",
            vendorId = vendorId,
            description = "Paper Stock",
            originalAmount = BigDecimal("10000.00"),
            issueDate = System.currentTimeMillis(),
            dueDate = System.currentTimeMillis() + 86400000L,
            status = VendorPayableStatus.SUBMITTED,
            createdBy = "USER-CREATOR"
        )

        // Creator attempting to approve -> Denied
        val selfApprove = VendorPayableValidator.validateApprove(payable, actorId = "USER-CREATOR")
        assertTrue(selfApprove is DomainResult.Error)
        assertTrue((selfApprove as DomainResult.Error).message.contains("Separation of duties"))

        // Different actor -> Approved
        val validApprove = VendorPayableValidator.validateApprove(payable, actorId = "USER-MANAGER")
        assertTrue(validApprove is DomainResult.Success)

        // Super Admin override -> Allowed
        val superAdminApprove = VendorPayableValidator.validateApprove(payable, actorId = "USER-CREATOR", isSuperAdmin = true)
        assertTrue(superAdminApprove is DomainResult.Success)
    }

    @Test
    fun testMandatoryReasonsForRejectionCancellationAndVoid() {
        val submittedPayable = VendorPayable(
            payableId = "PAY-001",
            tenantId = tenantId,
            projectId = projectId,
            payableNumber = "PAYABLE-20261016-0001",
            vendorId = vendorId,
            description = "UV Varnish Coating",
            originalAmount = BigDecimal("3000.00"),
            issueDate = System.currentTimeMillis(),
            dueDate = System.currentTimeMillis() + 86400000L,
            status = VendorPayableStatus.SUBMITTED,
            createdBy = "USER-01"
        )

        // Blank rejection reason fails
        val blankReject = VendorPayableValidator.validateReject(submittedPayable, "")
        assertTrue(blankReject is DomainResult.Error)

        // Valid rejection reason succeeds
        val validReject = VendorPayableValidator.validateReject(submittedPayable, "Coating quality failed QC inspection")
        assertTrue(validReject is DomainResult.Success)

        // Blank cancellation reason fails
        val blankCancel = VendorPayableValidator.validateCancel(submittedPayable, "   ")
        assertTrue(blankCancel is DomainResult.Error)

        // Blank void reason fails
        val approvedPayable = submittedPayable.copy(status = VendorPayableStatus.APPROVED)
        val blankVoid = VendorPayableValidator.validateVoid(approvedPayable, "")
        assertTrue(blankVoid is DomainResult.Error)
    }

    @Test
    fun testOutstandingAmountMathematicalInvariant() {
        val payable = VendorPayable(
            payableId = "PAY-001",
            tenantId = tenantId,
            projectId = projectId,
            payableNumber = "PAYABLE-20261016-0001",
            vendorId = vendorId,
            description = "Glue and Binding Tape",
            originalAmount = BigDecimal("5000.0000"),
            paidAmount = BigDecimal("1500.0000"),
            issueDate = System.currentTimeMillis(),
            dueDate = System.currentTimeMillis() + 86400000L,
            status = VendorPayableStatus.PARTIALLY_PAID,
            createdBy = "USER-01"
        )

        assertEquals(BigDecimal("3500.0000"), payable.outstandingAmount)
        assertEquals(payable.originalAmount, payable.paidAmount.add(payable.outstandingAmount))
    }
}
