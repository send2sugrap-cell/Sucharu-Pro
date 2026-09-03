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

// ============================================================
// MODULE 20 STEP 06: FINAL GOVERNANCE, INTEGRITY & CROSS-MODULE READINESS
// ============================================================

/**
 * Per-dimension readiness flags that downstream modules (21–24) may inspect
 * to determine whether an affiliate has satisfied all Module 20 invariants.
 *
 * This contract is READ-ONLY. Modules 21–24 MUST NOT mutate any field here.
 */
data class AffiliateIntegrationReadinessState(
    val affiliateId: String,
    val tenantId: String,

    // Step 01 gates
    val profileExists: Boolean,
    val isIdentityVerified: Boolean,
    val isAgreementAccepted: Boolean,
    val isAccountActive: Boolean,
    val isTaxCompliant: Boolean,
    val isBusinessVerified: Boolean,
    val isFullyEligible: Boolean,

    // Step 02 gates
    val hasActiveEnrollment: Boolean,
    val hasAtLeastOneProgramEnrollment: Boolean,

    // Step 03 gates
    val hasOperationalProfile: Boolean,
    val profileCompletenessScore: Int,            // 0–100
    val hasVerifiedDocuments: Boolean,

    // Step 04 gates
    val hasAcceptedNotificationPreferences: Boolean,

    // Step 05 gates
    val hasNoOpenUrgentWorkItems: Boolean,
    val hasClearGovernanceQueue: Boolean,

    // Derived readiness flags for downstream modules
    val isReadyForAttribution: Boolean,           // Module 21
    val isReadyForCommission: Boolean,            // Module 22
    val isReadyForPayout: Boolean,                // Module 23
    val isReadyForAnalytics: Boolean,             // Module 24

    // Metadata
    val readinessScore: Int,                      // 0–100 composite score
    val assessedAt: Long = System.currentTimeMillis(),
    val assessedBy: String,
    val integrityHash: String
)

/**
 * Severity level of a lifecycle integrity violation.
 */
enum class AffiliateIntegrityViolationSeverity {
    CRITICAL,
    HIGH,
    MEDIUM,
    LOW,
    INFO
}

/**
 * A single cross-step lifecycle integrity violation detected by the engine.
 */
data class AffiliateIntegrityViolation(
    val code: String,
    val description: String,
    val severity: AffiliateIntegrityViolationSeverity,
    val step: String,
    val recommendation: String
)

/**
 * Result of a full cross-step lifecycle integrity assessment for one affiliate.
 */
data class AffiliateLifecycleIntegrityResult(
    val checkId: String,
    val tenantId: String,
    val affiliateId: String,
    val isIntegrityValid: Boolean,
    val violations: List<AffiliateIntegrityViolation> = emptyList(),
    val criticalCount: Int,
    val highCount: Int,
    val mediumCount: Int,
    val lowCount: Int,
    val summary: String,
    val checkedAt: Long = System.currentTimeMillis(),
    val checkedBy: String,
    val resultHash: String
)

/**
 * Result of a SHA-256 audit chain tamper-detection scan across a set of audit records.
 */
data class AuditChainVerificationResult(
    val verificationId: String,
    val tenantId: String,
    val affiliateId: String,
    val totalRecordsChecked: Int,
    val isChainIntact: Boolean,
    val firstTamperedAuditId: String? = null,
    val firstTamperedIndex: Int? = null,
    val summary: String,
    val verifiedAt: Long = System.currentTimeMillis()
)

/**
 * Master Unified Module 20 Final Governance Handoff Contract (v20.06).
 *
 * This is the SOLE authoritative entry point contract for Modules 21 (Attribution),
 * 22 (Commission), 23 (Wallet & Payout), and 24 (Analytics & Reporting).
 *
 * Modules 21–24 MUST:
 *   1. Verify `isReadyForAttribution` / `isReadyForCommission` / `isReadyForPayout` / `isReadyForAnalytics` before acting.
 *   2. Treat all data herein as READ-ONLY (isReadOnly = true).
 *   3. NEVER mutate the affiliate domain; route mutations via Module 20 service layer.
 *   4. Check `forbiddenAiActions` before executing any AI-Agent workflow.
 *
 * This contract is cryptographically sealed with a SHA-256 `integritySealHash`.
 */
data class Module20Step06FinalGovernanceHandoffContract(
    val contractVersion: String = "v20.06",
    val tenantId: String,
    val affiliateId: String,
    val userId: String,
    val customerId: String?,
    val affiliateCode: String,
    val displayName: String,

    // Live lifecycle state
    val currentStatus: AffiliateStatus,
    val affiliateType: AffiliateType,
    val verificationState: VerificationState,
    val onboardingState: OnboardingState,

    // Eligibility summary
    val isFullyEligible: Boolean,
    val eligibilityRejectionReasons: List<String>,

    // Step-wise readiness
    val integrationReadiness: AffiliateIntegrationReadinessState,

    // Latest lifecycle integrity result
    val integrityResult: AffiliateLifecycleIntegrityResult,

    // Authoritative downstream routing flags
    val isReadyForAttribution: Boolean,
    val isReadyForCommission: Boolean,
    val isReadyForPayout: Boolean,
    val isReadyForAnalytics: Boolean,

    // Security governance
    val isReadOnly: Boolean = true,
    val allowedAiActions: List<String> = listOf(
        "INSPECT_AFFILIATE_GOVERNANCE_STATUS",
        "READ_INTEGRATION_READINESS_STATE",
        "ANALYZE_LIFECYCLE_INTEGRITY",
        "VERIFY_AUDIT_CHAIN_INTEGRITY",
        "GENERATE_ANALYTICS_SIGNALS",
        "ROUTE_ATTRIBUTION_SIGNALS",
        "EVALUATE_COMMISSION_ELIGIBILITY",
        "EVALUATE_PAYOUT_ELIGIBILITY"
    ),
    val forbiddenAiActions: List<String> = listOf(
        "ACTIVATE_AFFILIATE",
        "SUSPEND_AFFILIATE",
        "TERMINATE_AFFILIATE",
        "MODIFY_ELIGIBILITY",
        "ALTER_AUDIT_CHAIN",
        "BYPASS_RLS",
        "REWRITE_ONBOARDING_STATE",
        "GRANT_COMMISSION_WITHOUT_MODULE22",
        "EXECUTE_PAYOUT_WITHOUT_MODULE23",
        "ATTRIBUTE_WITHOUT_MODULE21"
    ),

    // Timestamps & seal
    val joinedAt: Long,
    val activatedAt: Long?,
    val integritySealHash: String,
    val generatedAt: Long = System.currentTimeMillis()
)
