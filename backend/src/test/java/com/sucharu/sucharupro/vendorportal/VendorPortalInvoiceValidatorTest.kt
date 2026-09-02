package com.sucharu.sucharupro.vendorportal

import com.sucharu.sucharupro.domain.model.common.Money
import com.sucharu.sucharupro.domain.model.vendorportal.*
import com.sucharu.sucharupro.domain.validation.vendorportal.VendorPortalInvoiceValidator
import org.junit.Assert.*
import org.junit.Test
import java.math.BigDecimal

class VendorPortalInvoiceValidatorTest {

    @Test
    fun testValidateInvoiceSubmissionRequiresNonBlankFieldsAndPositiveQuantities() {
        val validItem = VendorPortalInvoiceSubmissionItem(
            itemId = "ITEM-01",
            submissionId = "SUB-01",
            tenantId = "TENANT-001",
            purchaseOrderItemId = "PO-ITEM-01",
            itemName = "Item A",
            invoicedQuantity = BigDecimal("10"),
            unitPrice = Money(BigDecimal("20.00")),
            lineTotal = Money(BigDecimal("200.00"))
        )

        val validSubmission = VendorPortalInvoiceSubmission(
            submissionId = "SUB-01",
            tenantId = "TENANT-001",
            projectId = "PRJ-001",
            vendorId = "VND-001",
            purchaseOrderId = "PO-001",
            orderNumber = "PO-2026-001",
            vendorInvoiceNumber = "VINV-001",
            items = listOf(validItem),
            subtotalAmount = Money(BigDecimal("200.00")),
            totalAmount = Money(BigDecimal("200.00")),
            createdBy = "VENDOR_USER"
        )

        VendorPortalInvoiceValidator.validateInvoiceSubmission(validSubmission)

        // Blank Vendor Invoice Number
        val invalidSub = validSubmission.copy(vendorInvoiceNumber = "   ")
        try {
            VendorPortalInvoiceValidator.validateInvoiceSubmission(invalidSub)
            fail("Expected IllegalArgumentException for blank vendor invoice number")
        } catch (e: IllegalArgumentException) {
            // expected
        }

        // Empty Items
        val emptyItemsSub = validSubmission.copy(items = emptyList())
        try {
            VendorPortalInvoiceValidator.validateInvoiceSubmission(emptyItemsSub)
            fail("Expected IllegalArgumentException for empty items")
        } catch (e: IllegalArgumentException) {
            // expected
        }

        // Negative/Zero Quantity
        val zeroQtyItem = validItem.copy(invoicedQuantity = BigDecimal.ZERO)
        val zeroQtySub = validSubmission.copy(items = listOf(zeroQtyItem))
        try {
            VendorPortalInvoiceValidator.validateInvoiceSubmission(zeroQtySub)
            fail("Expected IllegalArgumentException for zero quantity")
        } catch (e: IllegalArgumentException) {
            // expected
        }
    }

    @Test
    fun testValidateSubmissionStatusTransitions() {
        VendorPortalInvoiceValidator.validateSubmissionStatusTransition(
            VendorPortalInvoiceSubmissionStatus.DRAFT,
            VendorPortalInvoiceSubmissionStatus.SUBMITTED
        )

        VendorPortalInvoiceValidator.validateSubmissionStatusTransition(
            VendorPortalInvoiceSubmissionStatus.DRAFT,
            VendorPortalInvoiceSubmissionStatus.CANCELLED
        )

        try {
            VendorPortalInvoiceValidator.validateSubmissionStatusTransition(
                VendorPortalInvoiceSubmissionStatus.CONVERTED,
                VendorPortalInvoiceSubmissionStatus.DRAFT
            )
            fail("Expected IllegalArgumentException for illegal transition")
        } catch (e: IllegalArgumentException) {
            // expected
        }
    }

    @Test
    fun testValidateInvoiceResponseRequiresCommentsAndValidDisputeDetails() {
        val validResponse = VendorPortalInvoiceResponse(
            responseId = "RESP-01",
            tenantId = "TENANT-001",
            projectId = "PRJ-001",
            vendorId = "VND-001",
            invoiceId = "INV-01",
            responseType = VendorPortalInvoiceResponseType.DISPUTE_VARIANCE,
            comment = "Detailed reason explaining discrepancy in quantity received versus invoiced.",
            respondedBy = "VENDOR_USER"
        )

        VendorPortalInvoiceValidator.validateInvoiceResponse(validResponse)

        // Too short explanation for dispute
        val shortCommentResp = validResponse.copy(comment = "No")
        try {
            VendorPortalInvoiceValidator.validateInvoiceResponse(shortCommentResp)
            fail("Expected IllegalArgumentException for short dispute comment")
        } catch (e: IllegalArgumentException) {
            // expected
        }
    }
}
