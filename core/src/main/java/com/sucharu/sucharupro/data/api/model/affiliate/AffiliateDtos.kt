package com.sucharu.sucharupro.data.api.model.affiliate

import com.sucharu.sucharupro.domain.model.affiliate.*

/**
 * Request DTO for creating an affiliate.
 */
data class CreateAffiliateRequestDto(
    val userId: String,
    val customerId: String? = null,
    val displayName: String,
    val affiliateCode: String? = null,
    val affiliateType: String = "INDIVIDUAL",
    val contactPhone: String? = null,
    val contactEmail: String? = null,
    val taxIdOrGst: String? = null,
    val agreementReference: String? = null,
    val agreementVersion: String? = null,
    val metadataJson: String? = null
)

/**
 * Request DTO for updating an affiliate profile.
 */
data class UpdateAffiliateProfileRequestDto(
    val displayName: String? = null,
    val contactPhone: String? = null,
    val contactEmail: String? = null,
    val taxIdOrGst: String? = null,
    val affiliateType: String? = null,
    val verificationState: String? = null,
    val metadataJson: String? = null
)

/**
 * Request DTO for lifecycle action (activate, suspend, reactivate, reject, terminate).
 */
data class AffiliateLifecycleActionRequestDto(
    val reason: String = ""
)

/**
 * Request DTO for accepting affiliate agreement.
 */
data class AcceptAffiliateAgreementRequestDto(
    val agreementReference: String,
    val agreementVersion: String = "v1.0"
)

/**
 * Response DTO for an Affiliate Profile.
 */
data class AffiliateProfileDto(
    val affiliateId: String,
    val tenantId: String,
    val userId: String,
    val customerId: String?,
    val displayName: String,
    val affiliateCode: String,
    val status: String,
    val affiliateType: String,
    val contactPhone: String?,
    val contactEmail: String?,
    val taxIdOrGst: String?,
    val onboardingState: String,
    val verificationState: String,
    val agreementReference: String?,
    val agreementVersion: String?,
    val agreementAcceptedAt: Long?,
    val agreementAcceptedBy: String?,
    val joinedAt: Long,
    val activatedAt: Long?,
    val suspendedAt: Long?,
    val terminatedAt: Long?,
    val createdAt: Long,
    val updatedAt: Long,
    val version: Long,
    val metadataJson: String?
)

/**
 * Response DTO for Eligibility.
 */
data class AffiliateEligibilityDto(
    val eligibilityId: String,
    val tenantId: String,
    val affiliateId: String,
    val isEligible: Boolean,
    val identityVerified: Boolean,
    val agreementAccepted: Boolean,
    val accountActive: Boolean,
    val taxCompliant: Boolean,
    val businessVerified: Boolean,
    val rejectionReasons: List<String>,
    val evaluatedAt: Long,
    val evaluatedBy: String
)

/**
 * Response DTO for Audit Record.
 */
data class AffiliateAuditRecordDto(
    val auditId: String,
    val tenantId: String,
    val affiliateId: String,
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
 * Response DTO for Governance Summary.
 */
data class AffiliateGovernanceSummaryDto(
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

// Extension mappers

fun AffiliateProfile.toDto() = AffiliateProfileDto(
    affiliateId = affiliateId,
    tenantId = tenantId,
    userId = userId,
    customerId = customerId,
    displayName = displayName,
    affiliateCode = affiliateCode,
    status = status.name,
    affiliateType = affiliateType.name,
    contactPhone = contactPhone,
    contactEmail = contactEmail,
    taxIdOrGst = taxIdOrGst,
    onboardingState = onboardingState.name,
    verificationState = verificationState.name,
    agreementReference = agreementReference,
    agreementVersion = agreementVersion,
    agreementAcceptedAt = agreementAcceptedAt,
    agreementAcceptedBy = agreementAcceptedBy,
    joinedAt = joinedAt,
    activatedAt = activatedAt,
    suspendedAt = suspendedAt,
    terminatedAt = terminatedAt,
    createdAt = createdAt,
    updatedAt = updatedAt,
    version = version,
    metadataJson = metadataJson
)

fun AffiliateEligibility.toDto() = AffiliateEligibilityDto(
    eligibilityId = eligibilityId,
    tenantId = tenantId,
    affiliateId = affiliateId,
    isEligible = isEligible,
    identityVerified = identityVerified,
    agreementAccepted = agreementAccepted,
    accountActive = accountActive,
    taxCompliant = taxCompliant,
    businessVerified = businessVerified,
    rejectionReasons = rejectionReasons,
    evaluatedAt = evaluatedAt,
    evaluatedBy = evaluatedBy
)

fun AffiliateAuditRecord.toDto() = AffiliateAuditRecordDto(
    auditId = auditId,
    tenantId = tenantId,
    affiliateId = affiliateId,
    eventType = eventType.name,
    previousStatus = previousStatus?.name,
    newStatus = newStatus.name,
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

fun AffiliateGovernanceSummary.toDto() = AffiliateGovernanceSummaryDto(
    tenantId = tenantId,
    totalAffiliates = totalAffiliates,
    activeAffiliates = activeAffiliates,
    pendingAffiliates = pendingAffiliates,
    suspendedAffiliates = suspendedAffiliates,
    terminatedAffiliates = terminatedAffiliates,
    verifiedCount = verifiedCount,
    eligibleCount = eligibleCount,
    individualCount = individualCount,
    businessCount = businessCount,
    partnerCount = partnerCount,
    creatorCount = creatorCount,
    referralPartnerCount = referralPartnerCount
)
