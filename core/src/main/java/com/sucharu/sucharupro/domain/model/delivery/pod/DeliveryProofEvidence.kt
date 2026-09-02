package com.sucharu.sucharupro.domain.model.delivery.pod

/**
 * An individual piece of verification evidence attached to a Proof of Delivery (Module 08 Step 08).
 *
 * @param evidenceId Unique identifier for this evidence record.
 * @param proofId Reference to parent DeliveryProof.
 * @param projectId Project boundary context.
 * @param evidenceType Type of evidence (signature, photo, document, etc.).
 * @param storageReference File identifier / URI reference in persistent storage.
 * @param fileName Original or logical file name.
 * @param mimeType MIME media type (e.g., image/jpeg, application/pdf).
 * @param checksum Optional SHA-256 or MD5 integrity checksum.
 * @param description User or system description of the evidence.
 * @param isPrimary Whether this is designated as the primary proof piece.
 * @param capturedAt Timestamp when evidence was captured (epoch millis).
 * @param uploadedBy User ID who uploaded or recorded the evidence.
 * @param uploadedAt Timestamp when evidence record was created (epoch millis).
 * @param metadata Additional key-value attributes (e.g., GPS latitude/longitude, device info).
 */
data class DeliveryProofEvidence(
    val evidenceId: String,
    val proofId: String,
    val projectId: String,
    val evidenceType: DeliveryProofEvidenceType,
    val storageReference: String,
    val fileName: String,
    val mimeType: String,
    val checksum: String? = null,
    val description: String? = null,
    val isPrimary: Boolean = false,
    val capturedAt: Long? = null,
    val uploadedBy: String,
    val uploadedAt: Long = System.currentTimeMillis(),
    val metadata: Map<String, String> = emptyMap()
) {
    init {
        require(evidenceId.isNotBlank()) { "Evidence ID cannot be blank." }
        require(proofId.isNotBlank()) { "Proof ID cannot be blank." }
        require(projectId.isNotBlank()) { "Project ID cannot be blank." }
        require(storageReference.isNotBlank()) { "Storage reference cannot be blank." }
        require(fileName.isNotBlank()) { "File name cannot be blank." }
        require(mimeType.isNotBlank()) { "MIME type cannot be blank." }
        require(uploadedBy.isNotBlank()) { "Uploaded By cannot be blank." }
        require(uploadedAt > 0) { "Upload timestamp must be positive." }
    }
}
