package com.sucharu.sucharupro.data.api.model.affiliate

import com.sucharu.sucharupro.domain.model.affiliate.AffiliateBusinessType

/**
 * Request DTO for creating or updating an affiliate operational profile.
 */
data class UpsertAffiliateProfileRequestDto(
    val displayName: String,
    val legalName: String? = null,
    val businessType: String = "INDIVIDUAL",
    val businessDescription: String? = null,
    val contactEmail: String? = null,
    val contactPhone: String? = null,
    val website: String? = null,
    val addressLine1: String? = null,
    val addressLine2: String? = null,
    val city: String? = null,
    val region: String? = null,
    val country: String? = null,
    val postalCode: String? = null,
    val taxIdOrGst: String? = null,
    val taxInformationReference: String? = null,
    val metadataJson: String? = null,
    val idempotencyKey: String? = null
)

/**
 * Request DTO for requesting a verification check.
 */
data class RequestVerificationRequestDto(
    val verificationType: String,
    val metadataReference: String? = null,
    val reason: String? = null,
    val previousVerificationId: String? = null,
    val idempotencyKey: String? = null
)

/**
 * Request DTO for reviewing a verification check (Approve/Reject/Request Changes).
 */
data class ReviewVerificationRequestDto(
    val reason: String,
    val changeRequestNotes: String? = null,
    val expiresAt: Long? = null,
    val idempotencyKey: String? = null
)

/**
 * Request DTO for uploading / registering a document metadata reference.
 */
data class AddDocumentReferenceRequestDto(
    val verificationId: String? = null,
    val documentType: String,
    val storageReference: String,
    val fileName: String,
    val fileSizeBytes: Long? = null,
    val mimeType: String? = null,
    val expiresAt: Long? = null,
    val idempotencyKey: String? = null
)

/**
 * Request DTO for reviewing a document (Verify/Reject).
 */
data class ReviewDocumentRequestDto(
    val rejectionReason: String? = null,
    val idempotencyKey: String? = null
)

/**
 * Request DTO for suspending or reactivating a profile.
 */
data class ProfileLifecycleActionRequestDto(
    val reason: String,
    val idempotencyKey: String? = null
)

/**
 * Response DTO representing an affiliate operational profile.
 */
data class AffiliateOperationalProfileResponseDto(
    val tenantId: String,
    val affiliateId: String,
    val displayName: String,
    val legalName: String?,
    val businessType: String,
    val businessDescription: String?,
    val contactEmail: String?,
    val contactPhone: String?,
    val website: String?,
    val addressLine1: String?,
    val addressLine2: String?,
    val city: String?,
    val region: String?,
    val country: String?,
    val postalCode: String?,
    val taxIdOrGst: String?,
    val taxInformationReference: String?,
    val profileStatus: String,
    val completenessScore: Int,
    val completenessDetailsJson: String?,
    val submittedAt: Long?,
    val verifiedAt: Long?,
    val suspendedAt: Long?,
    val createdAt: Long,
    val updatedAt: Long,
    val version: Long,
    val metadataJson: String?
)

/**
 * Response DTO for profile completeness evaluation breakdown.
 */
data class ProfileCompletenessResponseDto(
    val affiliateId: String,
    val score: Int,
    val requiredFields: List<String>,
    val completedFields: List<String>,
    val missingFields: List<String>,
    val blockingIssues: List<String>,
    val isComplete: Boolean,
    val calculatedAt: Long
)

/**
 * Response DTO for verification records.
 */
data class AffiliateVerificationResponseDto(
    val tenantId: String,
    val verificationId: String,
    val affiliateId: String,
    val verificationType: String,
    val status: String,
    val submittedAt: Long?,
    val reviewedAt: Long?,
    val reviewerUserId: String?,
    val reason: String?,
    val changeRequestNotes: String?,
    val metadataReference: String?,
    val previousVerificationId: String?,
    val expiresAt: Long?,
    val createdAt: Long,
    val updatedAt: Long,
    val version: Long
)

/**
 * Response DTO for document references.
 */
data class AffiliateDocumentResponseDto(
    val tenantId: String,
    val documentId: String,
    val affiliateId: String,
    val verificationId: String?,
    val documentType: String,
    val storageReference: String,
    val fileName: String,
    val fileSizeBytes: Long?,
    val mimeType: String?,
    val status: String,
    val rejectionReason: String?,
    val uploadedAt: Long,
    val expiresAt: Long?,
    val verifiedAt: Long?,
    val createdAt: Long,
    val updatedAt: Long,
    val version: Long
)

/**
 * Response DTO for audit records.
 */
data class AffiliateProfileAuditResponseDto(
    val tenantId: String,
    val auditId: String,
    val affiliateId: String,
    val actorUserId: String,
    val actorRole: String,
    val actorType: String,
    val action: String,
    val entityReference: String?,
    val previousState: String?,
    val newState: String,
    val reason: String?,
    val correlationId: String,
    val idempotencyKey: String?,
    val recordHash: String,
    val previousAuditHash: String?,
    val chainHash: String,
    val timestamp: Long
)

/**
 * Response DTO for profile governance summary metrics.
 */
data class AffiliateProfileGovernanceSummaryResponseDto(
    val tenantId: String,
    val totalProfiles: Long,
    val verifiedProfiles: Long,
    val pendingReviewProfiles: Long,
    val incompleteProfiles: Long,
    val changesRequiredProfiles: Long,
    val suspendedProfiles: Long,
    val totalVerifications: Long,
    val verifiedVerifications: Long,
    val pendingVerifications: Long,
    val rejectedVerifications: Long,
    val totalDocuments: Long,
    val verifiedDocuments: Long
)
