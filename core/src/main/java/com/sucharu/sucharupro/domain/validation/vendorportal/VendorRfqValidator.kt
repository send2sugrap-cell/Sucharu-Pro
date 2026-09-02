package com.sucharu.sucharupro.domain.validation.vendorportal

import com.sucharu.sucharupro.domain.model.vendorportal.*
import java.math.BigDecimal

/**
 * Domain invariants and state validation for RFQs, Items, Invitations and Clarifications (Module 13 Step 03).
 */
object VendorRfqValidator {

    fun validateRfq(rfq: VendorRfq) {
        require(rfq.rfqId.isNotBlank()) { "RFQ ID cannot be blank." }
        require(rfq.rfqNumber.isNotBlank()) { "RFQ Number cannot be blank." }
        require(rfq.title.isNotBlank()) { "RFQ Title cannot be blank." }
        require(rfq.tenantId.isNotBlank()) { "Tenant ID cannot be blank." }
        require(rfq.projectId.isNotBlank()) { "Project ID cannot be blank." }
        require(rfq.requestedBy.isNotBlank()) { "Requesting user cannot be blank." }
        require(rfq.responseDeadline > rfq.issueDate) { "Response deadline must be after the issue date." }
    }

    fun validateRfqItem(item: VendorRfqItem) {
        require(item.rfqItemId.isNotBlank()) { "RFQ Item ID cannot be blank." }
        require(item.rfqId.isNotBlank()) { "RFQ ID cannot be blank." }
        require(item.description.isNotBlank()) { "Item description cannot be blank." }
        require(item.quantity > BigDecimal.ZERO) { "Item quantity must be strictly positive." }
        if (item.targetUnitPrice != null) {
            require(item.targetUnitPrice.amount >= BigDecimal.ZERO) { "Target unit price cannot be negative." }
        }
    }

    fun validateRfqTransition(current: VendorRfqStatus, target: VendorRfqStatus) {
        require(current.canTransitionTo(target)) {
            "Illegal RFQ state transition from $current to $target."
        }
    }

    fun validateInvitation(invitation: VendorRfqInvitation) {
        require(invitation.invitationId.isNotBlank()) { "Invitation ID cannot be blank." }
        require(invitation.rfqId.isNotBlank()) { "RFQ ID cannot be blank." }
        require(invitation.vendorId.isNotBlank()) { "Vendor ID cannot be blank." }
        require(invitation.tenantId.isNotBlank()) { "Tenant ID cannot be blank." }
        require(invitation.projectId.isNotBlank()) { "Project ID cannot be blank." }
    }

    fun validateInvitationTransition(current: VendorRfqInvitationStatus, target: VendorRfqInvitationStatus) {
        require(current.canTransitionTo(target)) {
            "Illegal RFQ invitation state transition from $current to $target."
        }
    }

    fun validateClarification(clarification: VendorRfqClarification) {
        require(clarification.clarificationId.isNotBlank()) { "Clarification ID cannot be blank." }
        require(clarification.rfqId.isNotBlank()) { "RFQ ID cannot be blank." }
        require(clarification.vendorId.isNotBlank()) { "Vendor ID cannot be blank." }
        require(clarification.question.isNotBlank()) { "Question cannot be blank." }
        require(clarification.askedBy.isNotBlank()) { "Asking user cannot be blank." }
    }

    fun validateClarificationAnswer(answer: String, answeredBy: String) {
        require(answer.isNotBlank()) { "Answer cannot be blank." }
        require(answeredBy.isNotBlank()) { "AnsweredBy cannot be blank." }
    }

    fun validateDeadline(responseDeadline: Long, currentTimeMillis: Long = System.currentTimeMillis()) {
        require(currentTimeMillis <= responseDeadline) {
            "The RFQ response deadline has passed ($responseDeadline). Submissions or modifications are not permitted."
        }
    }
}
