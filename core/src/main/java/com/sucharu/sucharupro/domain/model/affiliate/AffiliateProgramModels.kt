package com.sucharu.sucharupro.domain.model.affiliate

/**
 * Deterministic Lifecycle States for an Affiliate Program.
 */
enum class AffiliateProgramStatus {
    DRAFT,
    ACTIVE,
    PAUSED,
    CLOSED,
    ARCHIVED
}

/**
 * Deterministic Lifecycle States for an Affiliate Enrollment (Affiliate <-> Program Relationship).
 */
enum class AffiliateEnrollmentStatus {
    PENDING,
    APPROVED,
    ACTIVE,
    SUSPENDED,
    TERMINATED,
    EXPIRED,
    REJECTED
}

/**
 * Audit Event Classification for Program and Enrollment governance actions.
 */
enum class AffiliateProgramAuditEventType {
    PROGRAM_CREATED,
    PROGRAM_UPDATED,
    PROGRAM_ACTIVATED,
    PROGRAM_PAUSED,
    PROGRAM_CLOSED,
    PROGRAM_ARCHIVED,
    ENROLLMENT_REQUESTED,
    ENROLLMENT_APPROVED,
    ENROLLMENT_REJECTED,
    ENROLLMENT_ACTIVATED,
    ENROLLMENT_SUSPENDED,
    ENROLLMENT_RESUMED,
    ENROLLMENT_TERMINATED,
    ENROLLMENT_EXPIRED
}

/**
 * Target Entity Category for Program audit events.
 */
enum class AffiliateProgramEntityCategory {
    PROGRAM,
    ENROLLMENT
}

/**
 * Authoritative Affiliate Program Model.
 */
data class AffiliateProgram(
    val programId: String,
    val tenantId: String,
    val programCode: String,
    val programName: String,
    val description: String?,
    val status: AffiliateProgramStatus = AffiliateProgramStatus.DRAFT,
    val startDate: Long,
    val endDate: Long? = null,
    val eligibilityPolicy: String = "STANDARD",
    val termsReference: String? = null,
    val termsVersion: String? = null,
    val maxParticipants: Int? = null,
    val createdBy: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val version: Long = 1L,
    val metadataJson: String? = null
) {
    val isActive: Boolean get() = status == AffiliateProgramStatus.ACTIVE
    val isPaused: Boolean get() = status == AffiliateProgramStatus.PAUSED
    val isClosed: Boolean get() = status == AffiliateProgramStatus.CLOSED
    val isArchived: Boolean get() = status == AffiliateProgramStatus.ARCHIVED
}

/**
 * Authoritative Affiliate Enrollment Model (Relationship Binding).
 */
data class AffiliateEnrollment(
    val enrollmentId: String,
    val tenantId: String,
    val affiliateId: String,
    val programId: String,
    val enrollmentStatus: AffiliateEnrollmentStatus = AffiliateEnrollmentStatus.PENDING,
    val effectiveFrom: Long? = null,
    val effectiveTo: Long? = null,
    val enrollmentReason: String? = null,
    val requestedAt: Long = System.currentTimeMillis(),
    val approvedBy: String? = null,
    val approvedAt: Long? = null,
    val rejectedBy: String? = null,
    val rejectedAt: Long? = null,
    val rejectionReason: String? = null,
    val suspendedBy: String? = null,
    val suspendedAt: Long? = null,
    val suspensionReason: String? = null,
    val terminatedBy: String? = null,
    val terminatedAt: Long? = null,
    val terminationReason: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val version: Long = 1L,
    val metadataJson: String? = null
) {
    val isPending: Boolean get() = enrollmentStatus == AffiliateEnrollmentStatus.PENDING
    val isApproved: Boolean get() = enrollmentStatus == AffiliateEnrollmentStatus.APPROVED
    val isActive: Boolean get() = enrollmentStatus == AffiliateEnrollmentStatus.ACTIVE
    val isSuspended: Boolean get() = enrollmentStatus == AffiliateEnrollmentStatus.SUSPENDED
    val isTerminated: Boolean get() = enrollmentStatus == AffiliateEnrollmentStatus.TERMINATED
    val isRejected: Boolean get() = enrollmentStatus == AffiliateEnrollmentStatus.REJECTED
    val isExpired: Boolean get() = enrollmentStatus == AffiliateEnrollmentStatus.EXPIRED
}

/**
 * Append-Only Cryptographically Sealed Audit Record for Program & Enrollment actions.
 */
data class AffiliateProgramAuditRecord(
    val auditId: String,
    val tenantId: String,
    val entityType: AffiliateProgramEntityCategory,
    val entityId: String,
    val eventType: AffiliateProgramAuditEventType,
    val previousStatus: String? = null,
    val newStatus: String,
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
 * Outbox Domain Event Record for Program & Enrollment Events.
 */
data class AffiliateProgramOutboxEvent(
    val outboxId: String,
    val tenantId: String,
    val aggregateType: String, // 'PROGRAM' or 'ENROLLMENT'
    val aggregateId: String,
    val eventType: String,
    val payloadJson: String,
    val status: String = "PENDING",
    val correlationId: String,
    val version: Long = 1L,
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * Program & Relationship Governance Summary Metrics.
 */
data class AffiliateProgramGovernanceSummary(
    val tenantId: String,
    val totalPrograms: Long,
    val activePrograms: Long,
    val pausedPrograms: Long,
    val closedPrograms: Long,
    val archivedPrograms: Long,
    val totalEnrollments: Long,
    val activeEnrollments: Long,
    val pendingEnrollments: Long,
    val suspendedEnrollments: Long,
    val terminatedEnrollments: Long,
    val rejectedEnrollments: Long,
    val generatedAt: Long = System.currentTimeMillis()
)

/**
 * Module 20 Step 02 Downstream AI Handoff Contract (v1.0.0).
 * Exposes authoritative read-only program participation and enrollment state for
 * Module 21 (Attribution), Module 22 (Commission), Module 23 (Wallet & Payout), and Module 24 (Analytics).
 */
data class Module20Step02ProgramHandoffContract(
    val contractVersion: String = "v1.0.0",
    val tenantId: String,
    val enrollmentId: String,
    val affiliateId: String,
    val affiliateCode: String,
    val programId: String,
    val programCode: String,
    val programName: String,
    val programStatus: AffiliateProgramStatus,
    val enrollmentStatus: AffiliateEnrollmentStatus,
    val effectiveFrom: Long?,
    val effectiveTo: Long?,
    val isEligibleForCommission: Boolean,
    val isEligibleForAttribution: Boolean,
    val isReadOnly: Boolean = true,
    val allowedAiActions: List<String> = listOf(
        "READ_AFFILIATE_PROGRAM_DETAILS",
        "INSPECT_ENROLLMENT_STATUS",
        "EXPLAIN_PROGRAM_ELIGIBILITY",
        "IDENTIFY_GOVERNANCE_CONFLICTS",
        "SUMMARIZE_PROGRAM_PARTICIPATION",
        "RECOMMEND_ADMINISTRATIVE_NEXT_STEPS"
    ),
    val forbiddenAiActions: List<String> = listOf(
        "APPROVE_ENROLLMENT",
        "ACTIVATE_ENROLLMENT",
        "SUSPEND_ENROLLMENT",
        "TERMINATE_ENROLLMENT",
        "MODIFY_PROGRAM_CONFIGURATION",
        "BYPASS_ROW_LEVEL_SECURITY",
        "MUTATE_POSTGRESQL_DIRECTLY",
        "ALTER_AUDIT_HISTORY",
        "MODIFY_COMMISSION",
        "TRIGGER_PAYOUT"
    ),
    val integritySealHash: String,
    val generatedAt: Long = System.currentTimeMillis()
)
