package com.sucharu.sucharupro.domain.validation.vendorportal

import com.sucharu.sucharupro.domain.model.vendorportal.*
import java.math.BigDecimal

object VendorCollaborationValidator {

    fun validatePoAcknowledgement(ack: VendorPoAcknowledgement) {
        require(ack.acknowledgementId.isNotBlank()) { "acknowledgementId cannot be blank" }
        require(ack.purchaseOrderId.isNotBlank()) { "purchaseOrderId cannot be blank" }
        require(ack.tenantId.isNotBlank()) { "tenantId cannot be blank" }
        require(ack.projectId.isNotBlank()) { "projectId cannot be blank" }
        require(ack.vendorId.isNotBlank()) { "vendorId cannot be blank" }
        require(ack.actorId.isNotBlank()) { "actorId cannot be blank" }

        when (ack.acknowledgementType) {
            VendorPoAcknowledgementType.DECLINED -> {
                require(!ack.declineReason.isNullOrBlank()) { "Decline reason is mandatory when declining a Purchase Order." }
            }
            VendorPoAcknowledgementType.ACKNOWLEDGED_WITH_EXCEPTION -> {
                require(!ack.exceptionDetails.isNullOrBlank()) { "Exception details are mandatory when acknowledging with exception." }
            }
            VendorPoAcknowledgementType.ACKNOWLEDGED -> {
                // valid
            }
        }
    }

    fun validateWoAcknowledgement(ack: VendorWoAcknowledgement) {
        require(ack.acknowledgementId.isNotBlank()) { "acknowledgementId cannot be blank" }
        require(ack.workOrderId.isNotBlank()) { "workOrderId cannot be blank" }
        require(ack.tenantId.isNotBlank()) { "tenantId cannot be blank" }
        require(ack.projectId.isNotBlank()) { "projectId cannot be blank" }
        require(ack.vendorId.isNotBlank()) { "vendorId cannot be blank" }
        require(ack.actorId.isNotBlank()) { "actorId cannot be blank" }

        when (ack.acknowledgementType) {
            VendorWoAcknowledgementType.DECLINED -> {
                require(!ack.declineReason.isNullOrBlank()) { "Decline reason is mandatory when declining a Work Order." }
            }
            VendorWoAcknowledgementType.ACKNOWLEDGED_WITH_EXCEPTION -> {
                require(!ack.exceptionDetails.isNullOrBlank()) { "Exception details are mandatory when acknowledging with exception." }
            }
            VendorWoAcknowledgementType.ACKNOWLEDGED -> {
                // valid
            }
        }
    }

    fun validateProgressUpdate(update: VendorProgressUpdate) {
        require(update.progressUpdateId.isNotBlank()) { "progressUpdateId cannot be blank" }
        require(update.tenantId.isNotBlank()) { "tenantId cannot be blank" }
        require(update.projectId.isNotBlank()) { "projectId cannot be blank" }
        require(update.vendorId.isNotBlank()) { "vendorId cannot be blank" }
        require(update.workOrderId.isNotBlank()) { "workOrderId cannot be blank" }
        require(update.submittedBy.isNotBlank()) { "submittedBy cannot be blank" }
        require(update.statusSummary.isNotBlank()) { "statusSummary cannot be blank" }

        require(update.completedQuantity >= BigDecimal.ZERO) { "completedQuantity cannot be negative" }
        require(update.remainingQuantity >= BigDecimal.ZERO) { "remainingQuantity cannot be negative" }
        require(update.authorizedQuantity > BigDecimal.ZERO) { "authorizedQuantity must be positive" }

        require(update.completedQuantity <= update.authorizedQuantity) {
            "completedQuantity (${update.completedQuantity}) cannot exceed authorizedQuantity (${update.authorizedQuantity})"
        }

        if (update.progressPercentage != null) {
            require(update.progressPercentage in 0.0..100.0) {
                "progressPercentage must be between 0.0 and 100.0, but was ${update.progressPercentage}"
            }
        }
    }

    fun validateBlocker(blocker: VendorBlocker) {
        require(blocker.blockerId.isNotBlank()) { "blockerId cannot be blank" }
        require(blocker.tenantId.isNotBlank()) { "tenantId cannot be blank" }
        require(blocker.projectId.isNotBlank()) { "projectId cannot be blank" }
        require(blocker.vendorId.isNotBlank()) { "vendorId cannot be blank" }
        require(blocker.workOrderId.isNotBlank()) { "workOrderId cannot be blank" }
        require(blocker.title.isNotBlank()) { "blocker title cannot be blank" }
        require(blocker.description.isNotBlank()) { "blocker description cannot be blank" }
        require(blocker.reportedBy.isNotBlank()) { "reportedBy cannot be blank" }
    }

    fun validateBlockerTransition(current: VendorBlockerStatus, target: VendorBlockerStatus) {
        require(current.canTransitionTo(target)) {
            "Illegal blocker status transition from $current to $target"
        }
    }

    fun validateCompletionRequest(req: VendorCompletionRequest, authorizedQuantity: BigDecimal) {
        require(req.completionRequestId.isNotBlank()) { "completionRequestId cannot be blank" }
        require(req.tenantId.isNotBlank()) { "tenantId cannot be blank" }
        require(req.projectId.isNotBlank()) { "projectId cannot be blank" }
        require(req.vendorId.isNotBlank()) { "vendorId cannot be blank" }
        require(req.workOrderId.isNotBlank()) { "workOrderId cannot be blank" }
        require(req.completionNotes.isNotBlank()) { "completionNotes cannot be blank" }
        require(req.submittedBy.isNotBlank()) { "submittedBy cannot be blank" }

        require(req.finalCompletedQuantity >= BigDecimal.ZERO) { "finalCompletedQuantity cannot be negative" }
        require(req.finalCompletedQuantity <= authorizedQuantity) {
            "finalCompletedQuantity (${req.finalCompletedQuantity}) cannot exceed authorizedQuantity ($authorizedQuantity)"
        }
    }

    fun validateCompletionTransition(current: VendorCompletionStatus, target: VendorCompletionStatus) {
        require(current.canTransitionTo(target)) {
            "Illegal completion status transition from $current to $target"
        }
    }

    fun validateCollaborationMessage(message: VendorCollaborationMessage) {
        require(message.messageId.isNotBlank()) { "messageId cannot be blank" }
        require(message.threadId.isNotBlank()) { "threadId cannot be blank" }
        require(message.tenantId.isNotBlank()) { "tenantId cannot be blank" }
        require(message.projectId.isNotBlank()) { "projectId cannot be blank" }
        require(message.vendorId.isNotBlank()) { "vendorId cannot be blank" }
        require(message.authorId.isNotBlank()) { "authorId cannot be blank" }
        require(message.message.isNotBlank()) { "Message content cannot be blank" }
    }
}
