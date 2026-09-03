package com.sucharu.sucharupro.domain.model.affiliate

/**
 * Extensible Business / Entity Classification for Affiliates (Module 20 Step 03).
 */
enum class AffiliateBusinessType {
    INDIVIDUAL,
    BUSINESS,
    AGENCY,
    RESELLER,
    PARTNER,
    ORGANIZATION,
    OTHER
}

/**
 * Deterministic Lifecycle Status of an Affiliate's Operational Profile.
 */
enum class AffiliateProfileStatus {
    INCOMPLETE,
    SUBMITTED,
    UNDER_REVIEW,
    VERIFIED,
    CHANGES_REQUIRED,
    SUSPENDED
}

/**
 * Extensible Types of Verification Checks.
 */
enum class AffiliateVerificationType {
    IDENTITY,
    BUSINESS,
    TAX,
    CONTACT,
    ADDRESS,
    AGREEMENT,
    DOCUMENT,
    OTHER
}

/**
 * Deterministic State Machine for Individual Verification Records.
 */
enum class AffiliateVerificationStatus {
    NOT_SUBMITTED,
    SUBMITTED,
    UNDER_REVIEW,
    VERIFIED,
    REJECTED,
    EXPIRED
}

/**
 * Types of Supporting Verification Documents.
 */
enum class AffiliateDocumentType {
    IDENTITY_PROOF,
    BUSINESS_REGISTRATION,
    TAX_CERTIFICATE,
    ADDRESS_PROOF,
    BANK_STATEMENT,
    AGREEMENT_DOCUMENT,
    OTHER
}

/**
 * Lifecycle Status of a Supporting Document Reference.
 */
enum class AffiliateDocumentStatus {
    UPLOADED,
    UNDER_REVIEW,
    VERIFIED,
    REJECTED,
    EXPIRED
}

/**
 * Append-Only Audit Event Types for Profile & Verification Governance.
 */
enum class AffiliateProfileAuditEventType {
    PROFILE_CREATED,
    PROFILE_UPDATED,
    PROFILE_SUBMITTED,
    VERIFICATION_REQUESTED,
    VERIFICATION_APPROVED,
    VERIFICATION_REJECTED,
    VERIFICATION_CHANGES_REQUESTED,
    PROFILE_SUSPENDED,
    PROFILE_REACTIVATED,
    DOCUMENT_UPLOADED,
    DOCUMENT_VERIFIED,
    DOCUMENT_REJECTED
}

/**
 * Authoritative Operational & Business Profile Entity.
 */
data class AffiliateOperationalProfile(
    val tenantId: String,
    val affiliateId: String,
    val displayName: String,
    val legalName: String? = null,
    val businessType: AffiliateBusinessType = AffiliateBusinessType.INDIVIDUAL,
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
    val profileStatus: AffiliateProfileStatus = AffiliateProfileStatus.INCOMPLETE,
    val completenessScore: Int = 0,
    val completenessDetailsJson: String? = null,
    val submittedAt: Long? = null,
    val verifiedAt: Long? = null,
    val suspendedAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val version: Long = 1L,
    val metadataJson: String? = null
) {
    val isVerified: Boolean get() = profileStatus == AffiliateProfileStatus.VERIFIED
    val isSuspended: Boolean get() = profileStatus == AffiliateProfileStatus.SUSPENDED
    val isUnderReview: Boolean get() = profileStatus == AffiliateProfileStatus.UNDER_REVIEW
    val isSubmitted: Boolean get() = profileStatus == AffiliateProfileStatus.SUBMITTED
}

/**
 * Dedicated Verification Record Model.
 */
data class AffiliateVerificationRecord(
    val tenantId: String,
    val verificationId: String,
    val affiliateId: String,
    val verificationType: AffiliateVerificationType,
    val status: AffiliateVerificationStatus = AffiliateVerificationStatus.NOT_SUBMITTED,
    val submittedAt: Long? = null,
    val reviewedAt: Long? = null,
    val reviewerUserId: String? = null,
    val reason: String? = null,
    val changeRequestNotes: String? = null,
    val metadataReference: String? = null,
    val previousVerificationId: String? = null,
    val expiresAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val version: Long = 1L
) {
    val isVerified: Boolean get() = status == AffiliateVerificationStatus.VERIFIED
    val isPending: Boolean get() = status in setOf(AffiliateVerificationStatus.SUBMITTED, AffiliateVerificationStatus.UNDER_REVIEW)
}

/**
 * Supporting Document Metadata Reference Model.
 */
data class AffiliateDocumentReference(
    val tenantId: String,
    val documentId: String,
    val affiliateId: String,
    val verificationId: String? = null,
    val documentType: AffiliateDocumentType,
    val storageReference: String,
    val fileName: String,
    val fileSizeBytes: Long? = null,
    val mimeType: String? = null,
    val status: AffiliateDocumentStatus = AffiliateDocumentStatus.UPLOADED,
    val rejectionReason: String? = null,
    val uploadedAt: Long = System.currentTimeMillis(),
    val expiresAt: Long? = null,
    val verifiedAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val version: Long = 1L
)

/**
 * Deterministic Profile Completeness Result.
 */
data class ProfileCompletenessResult(
    val affiliateId: String,
    val score: Int,
    val requiredFields: List<String>,
    val completedFields: List<String>,
    val missingFields: List<String>,
    val blockingIssues: List<String>,
    val isComplete: Boolean,
    val calculatedAt: Long = System.currentTimeMillis()
)

/**
 * Cryptographic Append-Only Audit Record for Profile & Verification Governance.
 */
data class AffiliateProfileAuditRecord(
    val tenantId: String,
    val auditId: String,
    val affiliateId: String,
    val actorUserId: String,
    val actorRole: String,
    val actorType: AffiliateActorType,
    val action: String,
    val entityReference: String? = null,
    val previousState: String? = null,
    val newState: String,
    val reason: String? = null,
    val correlationId: String,
    val idempotencyKey: String? = null,
    val recordHash: String,
    val previousAuditHash: String? = null,
    val chainHash: String,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Outbox Domain Event Record for Transactional Dispatching.
 */
data class AffiliateProfileOutboxEvent(
    val tenantId: String,
    val outboxId: String,
    val aggregateId: String,
    val eventType: String,
    val payloadJson: String,
    val status: String = "PENDING",
    val correlationId: String,
    val version: Long = 1L,
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * Governance Summary Metrics for Profile & Verification.
 */
data class AffiliateProfileGovernanceSummary(
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

/**
 * Module 20 Step 03 Downstream AI Governance Handoff Contract (v1.0.0).
 * Sealed immutable contract for advisory context across Modules 21, 22, 23, 24.
 */
data class Module20Step03AffiliateProfileHandoffContract(
    val contractVersion: String = "v1.0.0",
    val tenantId: String,
    val affiliateId: String,
    val displayName: String,
    val legalName: String?,
    val businessType: AffiliateBusinessType,
    val profileStatus: AffiliateProfileStatus,
    val completenessScore: Int,
    val isProfileComplete: Boolean,
    val isVerified: Boolean,
    val missingFields: List<String>,
    val blockingIssues: List<String>,
    val verificationSummary: Map<String, String>,
    val documentCount: Int,
    val isReadOnly: Boolean = true,
    val allowedAiActions: List<String> = listOf(
        "EXPLAIN_PROFILE_COMPLETENESS",
        "EXPLAIN_VERIFICATION_STATUS",
        "IDENTIFY_MISSING_REQUIREMENTS",
        "GUIDE_VERIFICATION_WORKFLOW",
        "ASSIST_GOVERNANCE_REVIEW",
        "INSPECT_PROFILE_CONTEXT"
    ),
    val forbiddenAiActions: List<String> = listOf(
        "APPROVE_VERIFICATION",
        "REJECT_VERIFICATION",
        "CHANGE_PROFILE_STATUS",
        "MUTATE_PROFILE_DATA",
        "UPLOAD_DOCUMENT",
        "DELETE_DOCUMENT",
        "BYPASS_RBAC",
        "BYPASS_ROW_LEVEL_SECURITY",
        "REWRITE_AUDIT_HISTORY"
    ),
    val integritySealHash: String,
    val generatedAt: Long = System.currentTimeMillis()
)
