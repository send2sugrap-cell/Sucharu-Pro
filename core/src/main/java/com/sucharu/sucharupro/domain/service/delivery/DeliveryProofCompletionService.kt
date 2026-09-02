package com.sucharu.sucharupro.domain.service.delivery

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.delivery.pod.DeliveryProof
import com.sucharu.sucharupro.domain.model.delivery.pod.DeliveryProofEvidence
import com.sucharu.sucharupro.domain.model.delivery.pod.DeliveryProofEvidenceType
import com.sucharu.sucharupro.domain.model.delivery.pod.DeliveryProofRecipient
import com.sucharu.sucharupro.domain.model.delivery.pod.DeliveryProofType

/**
 * Domain service determining evidence completeness and delivery verification rules (Module 08 Step 08).
 */
object DeliveryProofCompletionService {

    fun validateEvidenceCompleteness(
        proof: DeliveryProof,
        evidenceList: List<DeliveryProofEvidence>,
        recipient: DeliveryProofRecipient?
    ): DomainResult<Unit> {
        when (proof.proofType) {
            DeliveryProofType.SIGNATURE -> {
                val hasSig = evidenceList.any {
                    it.evidenceType == DeliveryProofEvidenceType.SIGNATURE_IMAGE ||
                            it.evidenceType == DeliveryProofEvidenceType.SIGNED_DOCUMENT
                }
                if (!hasSig) {
                    return DomainResult.Error(
                        message = "Signature proof requires at least one signature image or signed document evidence."
                    )
                }
                if (proof.recipientName.isNullOrBlank() && (recipient == null || recipient.recipientName.isBlank())) {
                    return DomainResult.Error(
                        message = "Signature proof requires recipient name acknowledgment."
                    )
                }
            }

            DeliveryProofType.PHOTO -> {
                val hasPhoto = evidenceList.any { it.evidenceType == DeliveryProofEvidenceType.DELIVERY_PHOTO }
                if (!hasPhoto) {
                    return DomainResult.Error(
                        message = "Photo proof requires at least one delivery photo evidence item."
                    )
                }
            }

            DeliveryProofType.OTP -> {
                val hasOtp = evidenceList.any { it.evidenceType == DeliveryProofEvidenceType.OTP_CONFIRMATION } ||
                        (recipient != null && recipient.confirmationMethod?.equals("OTP", ignoreCase = true) == true)
                if (!hasOtp) {
                    return DomainResult.Error(
                        message = "OTP proof requires OTP confirmation record or recipient confirmation."
                    )
                }
            }

            DeliveryProofType.DIGITAL_CONFIRMATION -> {
                if (proof.recipientName.isNullOrBlank() && (recipient == null || recipient.recipientName.isBlank())) {
                    return DomainResult.Error(
                        message = "Digital confirmation proof requires recipient acknowledgment."
                    )
                }
            }

            DeliveryProofType.DOCUMENT -> {
                val hasDoc = evidenceList.any {
                    it.evidenceType == DeliveryProofEvidenceType.SIGNED_DOCUMENT ||
                            it.evidenceType == DeliveryProofEvidenceType.OTHER
                }
                if (!hasDoc) {
                    return DomainResult.Error(
                        message = "Document proof requires at least one signed document or supporting evidence file."
                    )
                }
            }

            DeliveryProofType.RECIPIENT_CONFIRMATION -> {
                if (proof.recipientName.isNullOrBlank() && (recipient == null || recipient.recipientName.isBlank())) {
                    return DomainResult.Error(
                        message = "Recipient confirmation proof requires recipient name."
                    )
                }
            }

            DeliveryProofType.COMBINED -> {
                if (evidenceList.size < 2) {
                    return DomainResult.Error(
                        message = "Combined multi-factor proof requires at least two distinct evidence items."
                    )
                }
            }

            DeliveryProofType.OTHER -> {
                if (evidenceList.isEmpty() && proof.notes.isNullOrBlank()) {
                    return DomainResult.Error(
                        message = "Other proof type requires at least one evidence item or detailed delivery notes."
                    )
                }
            }
        }

        return DomainResult.Success(Unit)
    }
}
