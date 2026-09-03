package com.sucharu.sucharupro.domain.model.affiliate

/**
 * Deterministic Affiliate Lifecycle States (Module 20 Step 01).
 */
enum class AffiliateStatus {
    PENDING,
    ACTIVE,
    SUSPENDED,
    INACTIVE,
    REJECTED,
    TERMINATED
}

/**
 * Affiliate Classification / Category Types.
 */
enum class AffiliateType {
    INDIVIDUAL,
    BUSINESS,
    PARTNER,
    CREATOR,
    REFERRAL_PARTNER
}

/**
 * Onboarding Pipeline State.
 */
enum class OnboardingState {
    DRAFT,
    SUBMITTED,
    UNDER_REVIEW,
    APPROVED,
    REJECTED
}

/**
 * Compliance / Identity Verification State.
 */
enum class VerificationState {
    UNVERIFIED,
    PENDING_DOCUMENTS,
    VERIFIED,
    REJECTED,
    EXPIRED
}

/**
 * Affiliate Append-Only Audit Event Types.
 */
enum class AffiliateAuditEventType {
    AFFILIATE_CREATED,
    AFFILIATE_UPDATED,
    AFFILIATE_ACTIVATED,
    AFFILIATE_SUSPENDED,
    AFFILIATE_REACTIVATED,
    AFFILIATE_REJECTED,
    AFFILIATE_TERMINATED,
    ELIGIBILITY_EVALUATED,
    AGREEMENT_ACCEPTED,
    VERIFICATION_STATUS_CHANGED
}

/**
 * Originating actor classification for affiliate audit records.
 */
enum class AffiliateActorType {
    HUMAN,
    SYSTEM,
    AI_AGENT
}

/**
 * Affiliate Agreement / Terms Reference.
 */
data class AffiliateAgreementReference(
    val agreementReference: String,
    val agreementVersion: String,
    val acceptedAt: Long,
    val acceptedByActorId: String,
    val isCurrent: Boolean = true
)

/**
 * Multi-Dimensional Eligibility Assessment for an Affiliate.
 */
data class AffiliateEligibility(
    val eligibilityId: String,
    val tenantId: String,
    val affiliateId: String,
    val isEligible: Boolean,
    val identityVerified: Boolean,
    val agreementAccepted: Boolean,
    val accountActive: Boolean,
    val taxCompliant: Boolean,
    val businessVerified: Boolean,
    val rejectionReasons: List<String> = emptyList(),
    val evaluatedAt: Long = System.currentTimeMillis(),
    val evaluatedBy: String
)

/**
 * Core Authoritative Affiliate Profile Entity.
 */
data class AffiliateProfile(
    val affiliateId: String,
    val tenantId: String,
    val userId: String,
    val customerId: String? = null,
    val displayName: String,
    val affiliateCode: String,
    val status: AffiliateStatus = AffiliateStatus.PENDING,
    val affiliateType: AffiliateType = AffiliateType.INDIVIDUAL,
    val contactPhone: String? = null,
    val contactEmail: String? = null,
    val taxIdOrGst: String? = null,
    val onboardingState: OnboardingState = OnboardingState.SUBMITTED,
    val verificationState: VerificationState = VerificationState.UNVERIFIED,
    val agreementReference: String? = null,
    val agreementVersion: String? = null,
    val agreementAcceptedAt: Long? = null,
    val agreementAcceptedBy: String? = null,
    val joinedAt: Long = System.currentTimeMillis(),
    val activatedAt: Long? = null,
    val suspendedAt: Long? = null,
    val terminatedAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val version: Long = 1L,
    val metadataJson: String? = null
) {
    val isActive: Boolean get() = status == AffiliateStatus.ACTIVE
    val isSuspended: Boolean get() = status == AffiliateStatus.SUSPENDED
    val isTerminated: Boolean get() = status == AffiliateStatus.TERMINATED
    val isAgreementAccepted: Boolean get() = agreementAcceptedAt != null && !agreementReference.isNullOrBlank()
}

/**
 * Cryptographically sealed, append-only affiliate audit record.
 */
data class AffiliateAuditRecord(
    val auditId: String,
    val tenantId: String,
    val affiliateId: String,
    val eventType: AffiliateAuditEventType,
    val previousStatus: AffiliateStatus? = null,
    val newStatus: AffiliateStatus,
    val actorType: AffiliateActorType,
    val actorId: String,
    val actorRole: String,
    val reason: String,
    val correlationId: String,
    val recordHash: String,
    val previousAuditHash: String? = null,
    val chainHash: String,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Outbox Domain Event Record for Transactional Dispatching.
 */
data class AffiliateOutboxEvent(
    val outboxId: String,
    val tenantId: String,
    val aggregateId: String,
    val eventType: String,
    val payloadJson: String,
    val status: String = "PENDING",
    val correlationId: String,
    val version: Long = 1L,
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * Governance Summary Metrics for Tenant Affiliate Management.
 */
data class AffiliateGovernanceSummary(
    val tenantId: String,
    val totalAffiliates: Long,
    val activeAffiliates: Long,
    val pendingAffiliates: Long,
    val suspendedAffiliates: Long,
    val terminatedAffiliates: Long,
    val verifiedCount: Long,
    val eligibleCount: Long,
    val individualCount: Long,
    val businessCount: Long,
    val partnerCount: Long,
    val creatorCount: Long,
    val referralPartnerCount: Long
)

/**
 * Module 20 Step 01 Downstream and Cross-Module AI Handoff Contract (v1.0.0).
 * Exposes authoritative read-only affiliate context for Modules 21 (Attribution),
 * 22 (Commission), 23 (Wallet & Payout), and 24 (Analytics & Reporting).
 */
data class Module20Step01AffiliateHandoffContract(
    val contractVersion: String = "v1.0.0",
    val tenantId: String,
    val affiliateId: String,
    val userId: String,
    val customerId: String?,
    val affiliateCode: String,
    val displayName: String,
    val status: AffiliateStatus,
    val affiliateType: AffiliateType,
    val isEligibleForCommission: Boolean,
    val isEligibleForAttribution: Boolean,
    val verificationState: VerificationState,
    val agreementAccepted: Boolean,
    val agreementVersion: String?,
    val joinedAt: Long,
    val activatedAt: Long?,
    val isReadOnly: Boolean = true,
    val allowedAiActions: List<String> = listOf(
        "INSPECT_AFFILIATE_PROFILE",
        "EVALUATE_AFFILIATE_ELIGIBILITY",
        "ANALYZE_AFFILIATE_STATUS",
        "RECOMMEND_ONBOARDING_COMPLETION",
        "EXPLAIN_AFFILIATE_GOVERNANCE"
    ),
    val forbiddenAiActions: List<String> = listOf(
        "ACTIVATE_AFFILIATE",
        "SUSPEND_AFFILIATE",
        "TERMINATE_AFFILIATE",
        "ALTER_AFFILIATE_ELIGIBILITY",
        "MODIFY_AGREEMENT_ACCEPTANCE",
        "MUTATE_USER_IDENTITY",
        "BYPASS_ROW_LEVEL_SECURITY",
        "REWRITE_AUDIT_HISTORY"
    ),
    val integritySealHash: String,
    val generatedAt: Long = System.currentTimeMillis()
)
