package com.sucharu.sucharupro.domain.validation

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.delivery.pod.DeliveryProof
import com.sucharu.sucharupro.domain.model.delivery.pod.DeliveryProofStatus
import com.sucharu.sucharupro.domain.model.delivery.pod.DeliveryProofType

/**
 * Structural and semantic validation rules for DeliveryProof aggregate (Module 08 Step 08).
 */
object DeliveryProofValidator {

    fun validateProof(proof: DeliveryProof, targetProjectId: String): DomainResult<Unit> {
        if (proof.proofId.isBlank()) {
            return DomainResult.Error(message = "Proof ID cannot be blank.")
        }
        if (proof.projectId.isBlank()) {
            return DomainResult.Error(message = "Project ID cannot be blank.")
        }
        if (proof.projectId != targetProjectId) {
            return DomainResult.Error(
                message = "Project mismatch: Proof belongs to '${proof.projectId}', but target project is '$targetProjectId'."
            )
        }
        if (proof.deliveryOrderId.isBlank()) {
            return DomainResult.Error(message = "Delivery Order ID reference cannot be blank.")
        }
        if (proof.deliveryChallanId.isBlank()) {
            return DomainResult.Error(message = "Delivery Challan ID reference cannot be blank.")
        }
        if (proof.dispatchExecutionId.isBlank()) {
            return DomainResult.Error(message = "Dispatch Execution ID reference cannot be blank.")
        }
        if (proof.deliveryShipmentId.isBlank()) {
            return DomainResult.Error(message = "Delivery Shipment ID reference cannot be blank.")
        }
        if (proof.proofNo.isBlank()) {
            return DomainResult.Error(message = "Proof Number cannot be blank.")
        }
        if (proof.createdBy.isBlank()) {
            return DomainResult.Error(message = "Created By cannot be blank.")
        }
        if (proof.createdAt <= 0) {
            return DomainResult.Error(message = "Creation timestamp must be positive.")
        }
        if (proof.updatedAt < proof.createdAt) {
            return DomainResult.Error(message = "Updated timestamp cannot precede creation timestamp.")
        }

        if (proof.deliveredAt != null && proof.deliveredAt <= 0) {
            return DomainResult.Error(message = "Delivered timestamp must be positive when provided.")
        }
        if (proof.receivedAt != null && proof.receivedAt <= 0) {
            return DomainResult.Error(message = "Received timestamp must be positive when provided.")
        }
        if (proof.deliveredAt != null && proof.receivedAt != null && proof.receivedAt < proof.deliveredAt) {
            return DomainResult.Error(message = "Received timestamp cannot precede delivered timestamp.")
        }

        if (proof.proofStatus == DeliveryProofStatus.REJECTED && proof.rejectionReason.isNullOrBlank()) {
            return DomainResult.Error(message = "Rejection reason is mandatory when POD status is REJECTED.")
        }

        if ((proof.proofStatus == DeliveryProofStatus.SUBMITTED ||
                    proof.proofStatus == DeliveryProofStatus.VERIFIED ||
                    proof.proofStatus == DeliveryProofStatus.ACCEPTED) &&
            (proof.proofType == DeliveryProofType.SIGNATURE || proof.proofType == DeliveryProofType.RECIPIENT_CONFIRMATION)
        ) {
            if (proof.recipientName.isNullOrBlank()) {
                return DomainResult.Error(
                    message = "Recipient name is required for proof type '${proof.proofType.defaultLabel}'."
                )
            }
        }

        return DomainResult.Success(Unit)
    }
}
