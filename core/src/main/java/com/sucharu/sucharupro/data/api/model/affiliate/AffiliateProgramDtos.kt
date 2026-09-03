package com.sucharu.sucharupro.data.api.model.affiliate

import com.sucharu.sucharupro.domain.model.affiliate.AffiliateEnrollment
import com.sucharu.sucharupro.domain.model.affiliate.AffiliateEnrollmentStatus
import com.sucharu.sucharupro.domain.model.affiliate.AffiliateProgram
import com.sucharu.sucharupro.domain.model.affiliate.AffiliateProgramAuditRecord
import com.sucharu.sucharupro.domain.model.affiliate.AffiliateProgramGovernanceSummary
import com.sucharu.sucharupro.domain.model.affiliate.AffiliateProgramStatus
import com.sucharu.sucharupro.domain.model.affiliate.Module20Step02ProgramHandoffContract

/**
 * Request payload to create a new Affiliate Program.
 */
data class CreateAffiliateProgramRequestDto(
    val programCode: String,
    val programName: String,
    val description: String? = null,
    val startDate: Long,
    val endDate: Long? = null,
    val eligibilityPolicy: String = "STANDARD",
    val termsReference: String? = null,
    val termsVersion: String? = null,
    val maxParticipants: Int? = null,
    val metadataJson: String? = null
)

/**
 * Request payload to update an Affiliate Program's details.
 */
data class UpdateAffiliateProgramRequestDto(
    val programName: String? = null,
    val description: String? = null,
    val startDate: Long? = null,
    val endDate: Long? = null,
    val eligibilityPolicy: String? = null,
    val termsReference: String? = null,
    val termsVersion: String? = null,
    val maxParticipants: Int? = null,
    val metadataJson: String? = null
)

/**
 * Lifecycle action payload for Programs (activate, pause, close, archive).
 */
data class AffiliateProgramLifecycleActionRequestDto(
    val reason: String = "Operational lifecycle change"
)

/**
 * Canonical Affiliate Program DTO.
 */
data class AffiliateProgramDto(
    val programId: String,
    val tenantId: String,
    val programCode: String,
    val programName: String,
    val description: String?,
    val status: String,
    val startDate: Long,
    val endDate: Long?,
    val eligibilityPolicy: String,
    val termsReference: String?,
    val termsVersion: String?,
    val maxParticipants: Int?,
    val createdBy: String,
    val createdAt: Long,
    val updatedAt: Long,
    val version: Long,
    val metadataJson: String?
)

/**
 * Request payload to request/submit enrollment for an Affiliate into a Program.
 */
data class EnrollAffiliateRequestDto(
    val affiliateId: String,
    val programId: String = "",
    val enrollmentReason: String? = null,
    val effectiveFrom: Long? = null,
    val effectiveTo: Long? = null,
    val metadataJson: String? = null
)

/**
 * Lifecycle action payload for Enrollments (approve, reject, activate, suspend, resume, terminate).
 */
data class AffiliateEnrollmentLifecycleActionRequestDto(
    val reason: String = "Operational lifecycle transition"
)

/**
 * Canonical Affiliate Enrollment DTO.
 */
data class AffiliateEnrollmentDto(
    val enrollmentId: String,
    val tenantId: String,
    val affiliateId: String,
    val programId: String,
    val enrollmentStatus: String,
    val effectiveFrom: Long?,
    val effectiveTo: Long?,
    val enrollmentReason: String?,
    val requestedAt: Long,
    val approvedBy: String?,
    val approvedAt: Long?,
    val rejectedBy: String?,
    val rejectedAt: Long?,
    val rejectionReason: String?,
    val suspendedBy: String?,
    val suspendedAt: Long?,
    val suspensionReason: String?,
    val terminatedBy: String?,
    val terminatedAt: Long?,
    val terminationReason: String?,
    val createdAt: Long,
    val updatedAt: Long,
    val version: Long,
    val metadataJson: String?
)

/**
 * Audit record DTO for Program and Enrollment governance events.
 */
data class AffiliateProgramAuditRecordDto(
    val auditId: String,
    val tenantId: String,
    val entityType: String,
    val entityId: String,
    val eventType: String,
    val previousStatus: String?,
    val newStatus: String,
    val actorType: String,
    val actorId: String,
    val actorRole: String,
    val reason: String,
    val correlationId: String,
    val recordHash: String,
    val previousAuditHash: String?,
    val chainHash: String,
    val timestamp: Long
)

/**
 * Governance Summary DTO.
 */
data class AffiliateProgramGovernanceSummaryDto(
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
    val generatedAt: Long
)

/**
 * AI Handoff Contract DTO.
 */
data class Module20Step02ProgramHandoffContractDto(
    val contractVersion: String,
    val tenantId: String,
    val enrollmentId: String,
    val affiliateId: String,
    val affiliateCode: String,
    val programId: String,
    val programCode: String,
    val programName: String,
    val programStatus: String,
    val enrollmentStatus: String,
    val effectiveFrom: Long?,
    val effectiveTo: Long?,
    val isEligibleForCommission: Boolean,
    val isEligibleForAttribution: Boolean,
    val isReadOnly: Boolean,
    val allowedAiActions: List<String>,
    val forbiddenAiActions: List<String>,
    val integritySealHash: String,
    val generatedAt: Long
)

// Mapping extensions
fun AffiliateProgram.toDto() = AffiliateProgramDto(
    programId = programId,
    tenantId = tenantId,
    programCode = programCode,
    programName = programName,
    description = description,
    status = status.name,
    startDate = startDate,
    endDate = endDate,
    eligibilityPolicy = eligibilityPolicy,
    termsReference = termsReference,
    termsVersion = termsVersion,
    maxParticipants = maxParticipants,
    createdBy = createdBy,
    createdAt = createdAt,
    updatedAt = updatedAt,
    version = version,
    metadataJson = metadataJson
)

fun AffiliateEnrollment.toDto() = AffiliateEnrollmentDto(
    enrollmentId = enrollmentId,
    tenantId = tenantId,
    affiliateId = affiliateId,
    programId = programId,
    enrollmentStatus = enrollmentStatus.name,
    effectiveFrom = effectiveFrom,
    effectiveTo = effectiveTo,
    enrollmentReason = enrollmentReason,
    requestedAt = requestedAt,
    approvedBy = approvedBy,
    approvedAt = approvedAt,
    rejectedBy = rejectedBy,
    rejectedAt = rejectedAt,
    rejectionReason = rejectionReason,
    suspendedBy = suspendedBy,
    suspendedAt = suspendedAt,
    suspensionReason = suspensionReason,
    terminatedBy = terminatedBy,
    terminatedAt = terminatedAt,
    terminationReason = terminationReason,
    createdAt = createdAt,
    updatedAt = updatedAt,
    version = version,
    metadataJson = metadataJson
)

fun AffiliateProgramAuditRecord.toDto() = AffiliateProgramAuditRecordDto(
    auditId = auditId,
    tenantId = tenantId,
    entityType = entityType.name,
    entityId = entityId,
    eventType = eventType.name,
    previousStatus = previousStatus,
    newStatus = newStatus,
    actorType = actorType.name,
    actorId = actorId,
    actorRole = actorRole,
    reason = reason,
    correlationId = correlationId,
    recordHash = recordHash,
    previousAuditHash = previousAuditHash,
    chainHash = chainHash,
    timestamp = timestamp
)

fun AffiliateProgramGovernanceSummary.toDto() = AffiliateProgramGovernanceSummaryDto(
    tenantId = tenantId,
    totalPrograms = totalPrograms,
    activePrograms = activePrograms,
    pausedPrograms = pausedPrograms,
    closedPrograms = closedPrograms,
    archivedPrograms = archivedPrograms,
    totalEnrollments = totalEnrollments,
    activeEnrollments = activeEnrollments,
    pendingEnrollments = pendingEnrollments,
    suspendedEnrollments = suspendedEnrollments,
    terminatedEnrollments = terminatedEnrollments,
    rejectedEnrollments = rejectedEnrollments,
    generatedAt = generatedAt
)

fun Module20Step02ProgramHandoffContract.toDto() = Module20Step02ProgramHandoffContractDto(
    contractVersion = contractVersion,
    tenantId = tenantId,
    enrollmentId = enrollmentId,
    affiliateId = affiliateId,
    affiliateCode = affiliateCode,
    programId = programId,
    programCode = programCode,
    programName = programName,
    programStatus = programStatus.name,
    enrollmentStatus = enrollmentStatus.name,
    effectiveFrom = effectiveFrom,
    effectiveTo = effectiveTo,
    isEligibleForCommission = isEligibleForCommission,
    isEligibleForAttribution = isEligibleForAttribution,
    isReadOnly = isReadOnly,
    allowedAiActions = allowedAiActions,
    forbiddenAiActions = forbiddenAiActions,
    integritySealHash = integritySealHash,
    generatedAt = generatedAt
)
