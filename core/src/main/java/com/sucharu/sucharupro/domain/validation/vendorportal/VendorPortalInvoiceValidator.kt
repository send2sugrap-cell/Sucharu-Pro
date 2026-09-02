package com.sucharu.sucharupro.domain.validation.vendorportal

import com.sucharu.sucharupro.domain.model.common.Money
import com.sucharu.sucharupro.domain.model.vendorportal.*
import java.math.BigDecimal

/**
 * Invariant validation for vendor invoice submissions, responses, evidence, and state machines (Module 13 Step 06).
 */
object VendorPortalInvoiceValidator {

    fun validateInvoiceSubmission(submission: VendorPortalInvoiceSubmission) {
        require(submission.submissionId.isNotBlank()) { "Submission ID cannot be blank." }
        require(submission.tenantId.isNotBlank()) { "Tenant ID cannot be blank." }
        require(submission.projectId.isNotBlank()) { "Project ID cannot be blank." }
        require(submission.vendorId.isNotBlank()) { "Vendor ID cannot be blank." }
        require(submission.purchaseOrderId.isNotBlank()) { "Purchase order ID cannot be blank." }
        require(submission.orderNumber.isNotBlank()) { "Order number cannot be blank." }
        require(submission.vendorInvoiceNumber.isNotBlank()) { "Vendor invoice number cannot be blank." }
        require(submission.invoiceDate > 0) { "Invoice date must be a valid positive timestamp." }
        require(submission.currency.isNotBlank()) { "Currency cannot be blank." }
        require(submission.createdBy.isNotBlank()) { "CreatedBy cannot be blank." }
        require(submission.items.isNotEmpty()) { "Invoice submission must contain at least one line item." }

        require(submission.subtotalAmount.amount >= BigDecimal.ZERO) { "Subtotal amount cannot be negative." }
        require(submission.taxAmount.amount >= BigDecimal.ZERO) { "Tax amount cannot be negative." }
        require(submission.discountAmount.amount >= BigDecimal.ZERO) { "Discount amount cannot be negative." }
        require(submission.shippingAmount.amount >= BigDecimal.ZERO) { "Shipping amount cannot be negative." }
        require(submission.otherCharges.amount >= BigDecimal.ZERO) { "Other charges cannot be negative." }
        require(submission.totalAmount.amount >= BigDecimal.ZERO) { "Total amount cannot be negative." }

        for (item in submission.items) {
            validateSubmissionItem(item)
        }
    }

    fun validateSubmissionItem(item: VendorPortalInvoiceSubmissionItem) {
        require(item.itemId.isNotBlank()) { "Item ID cannot be blank." }
        require(item.submissionId.isNotBlank()) { "Submission ID cannot be blank." }
        require(item.purchaseOrderItemId.isNotBlank()) { "Purchase order item ID cannot be blank." }
        require(item.itemName.isNotBlank()) { "Item name cannot be blank." }
        require(item.invoicedQuantity > BigDecimal.ZERO) { "Invoiced quantity must be strictly positive." }
        require(item.unitPrice.amount >= BigDecimal.ZERO) { "Unit price cannot be negative." }
        require(item.taxAmount.amount >= BigDecimal.ZERO) { "Tax amount cannot be negative." }
        require(item.lineTotal.amount >= BigDecimal.ZERO) { "Line total cannot be negative." }
    }

    fun validateSubmissionStatusTransition(
        currentStatus: VendorPortalInvoiceSubmissionStatus,
        targetStatus: VendorPortalInvoiceSubmissionStatus
    ) {
        val validTransitions = mapOf(
            VendorPortalInvoiceSubmissionStatus.DRAFT to setOf(
                VendorPortalInvoiceSubmissionStatus.SUBMITTED,
                VendorPortalInvoiceSubmissionStatus.CANCELLED
            ),
            VendorPortalInvoiceSubmissionStatus.SUBMITTED to setOf(
                VendorPortalInvoiceSubmissionStatus.CONVERTED,
                VendorPortalInvoiceSubmissionStatus.REJECTED,
                VendorPortalInvoiceSubmissionStatus.CANCELLED
            ),
            VendorPortalInvoiceSubmissionStatus.CONVERTED to emptySet(),
            VendorPortalInvoiceSubmissionStatus.REJECTED to emptySet(),
            VendorPortalInvoiceSubmissionStatus.CANCELLED to emptySet()
        )

        val allowed = validTransitions[currentStatus] ?: emptySet()
        require(allowed.contains(targetStatus)) {
            "Illegal invoice submission status transition from $currentStatus to $targetStatus."
        }
    }

    fun validateInvoiceResponse(response: VendorPortalInvoiceResponse) {
        require(response.responseId.isNotBlank()) { "Response ID cannot be blank." }
        require(response.tenantId.isNotBlank()) { "Tenant ID cannot be blank." }
        require(response.projectId.isNotBlank()) { "Project ID cannot be blank." }
        require(response.vendorId.isNotBlank()) { "Vendor ID cannot be blank." }
        require(response.invoiceId.isNotBlank()) { "Invoice ID cannot be blank." }
        require(response.comment.isNotBlank()) { "Comment / explanation cannot be blank." }
        require(response.respondedBy.isNotBlank()) { "RespondedBy cannot be blank." }

        when (response.responseType) {
            VendorPortalInvoiceResponseType.DISPUTE_VARIANCE -> {
                require(response.comment.trim().length >= 10) {
                    "Dispute explanation must contain at least 10 characters detailing the discrepancy."
                }
            }
            VendorPortalInvoiceResponseType.PROPOSE_CORRECTION -> {
                require(!response.proposedCorrection.isNullOrBlank()) {
                    "Proposed correction description is required."
                }
            }
            VendorPortalInvoiceResponseType.CLARIFY_EXCEPTION,
            VendorPortalInvoiceResponseType.ACCEPT_VARIANCE,
            VendorPortalInvoiceResponseType.SUBMIT_ADDITIONAL_DOCS -> {
                // Comment is verified above
            }
        }
    }

    fun validateFinancialEvidence(evidence: VendorPortalFinancialEvidence) {
        require(evidence.evidenceId.isNotBlank()) { "Evidence ID cannot be blank." }
        require(evidence.tenantId.isNotBlank()) { "Tenant ID cannot be blank." }
        require(evidence.projectId.isNotBlank()) { "Project ID cannot be blank." }
        require(evidence.vendorId.isNotBlank()) { "Vendor ID cannot be blank." }
        require(evidence.entityType.isNotBlank()) { "Entity type cannot be blank." }
        require(evidence.entityId.isNotBlank()) { "Entity ID cannot be blank." }
        require(evidence.filename.isNotBlank()) { "Filename cannot be blank." }
        require(evidence.fileReference.isNotBlank()) { "File reference URI cannot be blank." }
        require(evidence.sizeBytes > 0) { "File size in bytes must be positive." }
        require(evidence.uploadedBy.isNotBlank()) { "UploadedBy cannot be blank." }
    }
}
