package com.sucharu.sucharupro.domain.validation

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.delivery.pod.DeliveryProofEvidence

/**
 * Validation rules for POD evidence items (Module 08 Step 08).
 */
object DeliveryProofEvidenceValidator {

    private val ALLOWED_MIME_PREFIXES = listOf("image/", "application/pdf")

    fun validateEvidence(
        evidence: DeliveryProofEvidence,
        targetProjectId: String,
        targetProofId: String
    ): DomainResult<Unit> {
        if (evidence.evidenceId.isBlank()) {
            return DomainResult.Error(message = "Evidence ID cannot be blank.")
        }
        if (evidence.proofId.isBlank()) {
            return DomainResult.Error(message = "Proof ID cannot be blank.")
        }
        if (evidence.proofId != targetProofId) {
            return DomainResult.Error(
                message = "Proof ID mismatch: Evidence references '${evidence.proofId}', but target proof is '$targetProofId'."
            )
        }
        if (evidence.projectId.isBlank()) {
            return DomainResult.Error(message = "Project ID cannot be blank.")
        }
        if (evidence.projectId != targetProjectId) {
            return DomainResult.Error(
                message = "Project mismatch: Evidence belongs to '${evidence.projectId}', but target proof is in '$targetProjectId'."
            )
        }
        if (evidence.storageReference.isBlank()) {
            return DomainResult.Error(message = "Storage reference cannot be blank.")
        }
        if (evidence.fileName.isBlank()) {
            return DomainResult.Error(message = "File name cannot be blank.")
        }
        if (evidence.mimeType.isBlank()) {
            return DomainResult.Error(message = "MIME type cannot be blank.")
        }
        if (ALLOWED_MIME_PREFIXES.none { evidence.mimeType.startsWith(it, ignoreCase = true) }) {
            return DomainResult.Error(
                message = "Unsupported MIME type '${evidence.mimeType}'. Allowed types: images (image/*) and PDF documents (application/pdf)."
            )
        }
        if (evidence.uploadedBy.isBlank()) {
            return DomainResult.Error(message = "Uploaded By cannot be blank.")
        }
        if (evidence.uploadedAt <= 0) {
            return DomainResult.Error(message = "Upload timestamp must be positive.")
        }

        return DomainResult.Success(Unit)
    }
}
