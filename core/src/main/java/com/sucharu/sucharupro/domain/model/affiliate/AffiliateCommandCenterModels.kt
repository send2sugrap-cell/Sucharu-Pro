package com.sucharu.sucharupro.domain.model.affiliate

/**
 * Priorities for Affiliate Governance Work Queue Items.
 */
enum class AffiliateGovernanceWorkItemPriority {
    URGENT,
    HIGH,
    MEDIUM,
    LOW
}

/**
 * Status states for Affiliate Governance Work Queue Items.
 */
enum class AffiliateGovernanceWorkItemStatus {
    OPEN,
    IN_PROGRESS,
    RESOLVED,
    DISMISSED,
    ESCALATED
}

/**
 * Operational Trigger Types for Governance Work Queue Items.
 */
enum class AffiliateGovernanceWorkItemType {
    PENDING_REVIEW,
    IDENTITY_VERIFICATION,
    BUSINESS_VERIFICATION,
    AGREEMENT_ACCEPTANCE,
    INCOMPLETE_PROFILE,
    ENROLLMENT_ACTION,
    SUSPENDED_REVIEW,
    GOVERNANCE_ISSUE,
    FAILED_NOTIFICATION,
    ADMIN_ACTION_CONFIRMATION
}

/**
 * Authoritative Entity representing an Operational Work Queue Item.
 */
data class AffiliateGovernanceWorkItem(
    val tenantId: String,
    val workItemId: String,
    val affiliateId: String,
    val programId: String? = null,
    val itemType: AffiliateGovernanceWorkItemType,
    val priority: AffiliateGovernanceWorkItemPriority = AffiliateGovernanceWorkItemPriority.MEDIUM,
    val status: AffiliateGovernanceWorkItemStatus = AffiliateGovernanceWorkItemStatus.OPEN,
    val title: String,
    val description: String,
    val requiredAction: String,
    val assignedRole: String? = "ADMIN",
    val assignedUserId: String? = null,
    val resolutionNotes: String? = null,
    val resolvedByUserId: String? = null,
    val resolvedAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val version: Long = 1L
) {
    val isResolved: Boolean get() = status == AffiliateGovernanceWorkItemStatus.RESOLVED || status == AffiliateGovernanceWorkItemStatus.DISMISSED
}

/**
 * Consolidated Operational Overview Metrics (State Counts Only — No Financials/Attribution).
 */
data class AffiliateCommandCenterOverview(
    val tenantId: String,
    val totalAffiliates: Long,
    val draftCount: Long,
    val pendingReviewCount: Long,
    val activeCount: Long,
    val suspendedCount: Long,
    val rejectedCount: Long,
    val terminatedCount: Long,
    val verificationPendingCount: Long,
    val profileIncompleteCount: Long,
    val agreementPendingCount: Long,
    val governanceAttentionRequiredCount: Long,
    val communicationAttentionRequiredCount: Long,
    val openWorkItemsCount: Long,
    val urgentWorkItemsCount: Long,
    val lastRefreshedAt: Long = System.currentTimeMillis()
)

/**
 * Consolidated 360-Degree Administrative Detail Inspector View.
 */
data class AffiliateAdministrativeDetailView(
    val tenantId: String,
    val affiliateId: String,
    val identityProfile: AffiliateProfile,
    val eligibility: AffiliateEligibility,
    val operationalProfile: AffiliateOperationalProfile?,
    val verifications: List<AffiliateVerificationRecord>,
    val documentReferences: List<AffiliateDocumentReference>,
    val programRelationships: List<AffiliateEnrollment>,
    val recentCommunications: List<AffiliateCommunicationRecord>,
    val openWorkItems: List<AffiliateGovernanceWorkItem>,
    val auditTrail: List<AffiliateGovernanceWorkItemAuditRecord>,
    val handoffContract: Module20Step05AffiliateCommandCenterHandoffContract
)

/**
 * SHA-256 Chained Block Audit Record for Command Center Governance Actions.
 */
data class AffiliateGovernanceWorkItemAuditRecord(
    val tenantId: String,
    val auditId: String,
    val affiliateId: String?,
    val workItemId: String?,
    val actorUserId: String,
    val actorRole: String,
    val actorType: AffiliateActorType,
    val action: String,
    val previousState: String?,
    val newState: String,
    val reason: String?,
    val correlationId: String,
    val idempotencyKey: String?,
    val recordHash: String,
    val previousAuditHash: String?,
    val chainHash: String,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * AI Governance Handoff Contract for Command Center Context (Step 05).
 */
data class Module20Step05AffiliateCommandCenterHandoffContract(
    val stepVersion: String = "20.05",
    val moduleScope: String = "AFFILIATE_ADMINISTRATIVE_COMMAND_CENTER",
    val tenantId: String,
    val userId: String,
    val totalAffiliates: Long,
    val activeAffiliates: Long,
    val openWorkItemsCount: Long,
    val urgentWorkItemsCount: Long,
    val governanceAttentionRequired: Boolean,
    val workItemTypeCounts: Map<String, Long>,
    val priorityCounts: Map<String, Long>,
    val isReadOnly: Boolean = true,
    val forbiddenActions: List<String> = listOf(
        "CALCULATE_COMMISSION",
        "ATTRIBUTE_REFERRAL",
        "ISSUE_PAYOUT",
        "MODIFY_WALLET",
        "BYPASS_RBAC",
        "BYPASS_LIFECYCLE_GOVERNANCE",
        "BYPASS_TENANT_ISOLATION"
    ),
    val integritySealHash: String,
    val generatedAt: Long = System.currentTimeMillis()
)
