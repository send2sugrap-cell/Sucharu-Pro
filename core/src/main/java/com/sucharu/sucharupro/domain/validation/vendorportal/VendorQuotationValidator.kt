package com.sucharu.sucharupro.domain.validation.vendorportal

import com.sucharu.sucharupro.domain.model.vendorportal.*
import java.math.BigDecimal

/**
 * Domain invariants and state validation for Vendor Quotations, Revisions, Evaluations and Awards (Module 13 Step 03).
 */
object VendorQuotationValidator {

    fun validateQuotation(quotation: VendorQuotation) {
        require(quotation.quotationId.isNotBlank()) { "Quotation ID cannot be blank." }
        require(quotation.rfqId.isNotBlank()) { "RFQ ID cannot be blank." }
        require(quotation.invitationId.isNotBlank()) { "Invitation ID cannot be blank." }
        require(quotation.vendorId.isNotBlank()) { "Vendor ID cannot be blank." }
        require(quotation.tenantId.isNotBlank()) { "Tenant ID cannot be blank." }
        require(quotation.projectId.isNotBlank()) { "Project ID cannot be blank." }
        require(quotation.quotationNumber.isNotBlank()) { "Quotation Number cannot be blank." }
        require(quotation.revisionNumber >= 1) { "Revision Number must be at least 1." }
        require(quotation.validityPeriodDays > 0) { "Validity Period must be strictly positive." }
        require(quotation.grandTotal.amount >= BigDecimal.ZERO) { "Quotation Grand Total cannot be negative." }
    }

    fun validateQuotationItem(item: VendorQuotationItem) {
        require(item.quotationItemId.isNotBlank()) { "Quotation Item ID cannot be blank." }
        require(item.quotationId.isNotBlank()) { "Quotation ID cannot be blank." }
        require(item.rfqItemId.isNotBlank()) { "RFQ Item ID cannot be blank." }
        require(item.quantity > BigDecimal.ZERO) { "Item quantity must be strictly positive." }
        require(item.unitPrice.amount >= BigDecimal.ZERO) { "Unit price cannot be negative." }
        require(item.discountAmount.amount >= BigDecimal.ZERO) { "Discount amount cannot be negative." }
        require(item.taxAmount.amount >= BigDecimal.ZERO) { "Tax amount cannot be negative." }
        require(item.lineTotal.amount >= BigDecimal.ZERO) { "Line total cannot be negative." }
    }

    fun validateQuotationTransition(current: VendorQuotationStatus, target: VendorQuotationStatus) {
        require(current.canTransitionTo(target)) {
            "Illegal quotation state transition from $current to $target."
        }
    }

    fun validateQuotationSubmission(quotation: VendorQuotation, rfqItems: List<VendorRfqItem>) {
        validateQuotation(quotation)
        require(quotation.items.isNotEmpty()) { "Quotation must contain at least one item response." }

        val rfqItemIds = rfqItems.map { it.rfqItemId }.toSet()
        val quotationRfqItemIds = quotation.items.map { it.rfqItemId }

        require(quotationRfqItemIds.size == quotationRfqItemIds.distinct().size) {
            "Duplicate RFQ item responses detected in quotation."
        }

        val missingMandatory = rfqItemIds - quotationRfqItemIds.toSet()
        require(missingMandatory.isEmpty()) {
            "All mandatory RFQ items must be quoted. Missing items: $missingMandatory"
        }

        for (item in quotation.items) {
            validateQuotationItem(item)
            val expectedLineTotal = VendorQuotationCalculator.calculateLineTotal(
                quantity = item.quantity,
                unitPrice = item.unitPrice,
                discountAmount = item.discountAmount,
                taxAmount = item.taxAmount
            )
            require(item.lineTotal == expectedLineTotal) {
                "Line total mismatch for item '${item.rfqItemId}'. Expected: ${expectedLineTotal.amount}, actual: ${item.lineTotal.amount}"
            }
        }

        val expectedTotals = VendorQuotationCalculator.calculateQuotationTotals(quotation.items)
        require(quotation.grandTotal == expectedTotals.grandTotal) {
            "Grand total mismatch. Expected: ${expectedTotals.grandTotal.amount}, actual: ${quotation.grandTotal.amount}"
        }
    }

    fun validateRevisionRequest(quotation: VendorQuotation, reason: String) {
        require(quotation.status in setOf(VendorQuotationStatus.SUBMITTED, VendorQuotationStatus.UNDER_REVIEW)) {
            "Only submitted or under-review quotations can be marked for revision. Current: ${quotation.status}"
        }
        require(reason.isNotBlank()) { "Reason for revision cannot be blank." }
    }

    fun validateEvaluation(evaluation: VendorRfqEvaluation) {
        require(evaluation.evaluationId.isNotBlank()) { "Evaluation ID cannot be blank." }
        require(evaluation.rfqId.isNotBlank()) { "RFQ ID cannot be blank." }
        require(evaluation.quotationId.isNotBlank()) { "Quotation ID cannot be blank." }
        require(evaluation.vendorId.isNotBlank()) { "Vendor ID cannot be blank." }
        require(evaluation.evaluatorUserId.isNotBlank()) { "Evaluator user cannot be blank." }
        require(evaluation.totalScore in 0.0..100.0) { "Total score must be between 0.0 and 100.0." }
    }

    fun validateAwardDecision(
        rfq: VendorRfq,
        quotation: VendorQuotation,
        awardActorId: String,
        awardReason: String
    ) {
        require(rfq.status in setOf(VendorRfqStatus.CLOSED, VendorRfqStatus.EVALUATION)) {
            "RFQ must be CLOSED or in EVALUATION before awarding. Current: ${rfq.status}"
        }
        require(quotation.status in setOf(VendorQuotationStatus.SUBMITTED, VendorQuotationStatus.UNDER_REVIEW, VendorQuotationStatus.REVISED)) {
            "Only active submitted quotations can be awarded. Current: ${quotation.status}"
        }
        require(quotation.rfqId == rfq.rfqId) { "Quotation does not belong to the target RFQ." }
        require(awardReason.isNotBlank()) { "Award reason cannot be blank." }
        require(awardActorId.isNotBlank()) { "Awarding actor cannot be blank." }

        // Separation of Duties: Vendor users or quotation submitters cannot award
        require(awardActorId != quotation.submittedBy) {
            "Separation of Duties violation: Quotation submitter '$awardActorId' cannot award the RFQ."
        }
        require(awardActorId != quotation.vendorId) {
            "Separation of Duties violation: Vendor '$awardActorId' cannot award the RFQ."
        }
    }
}
