package com.sucharu.sucharupro.data.api.model.affiliate

import com.sucharu.sucharupro.domain.model.affiliate.*

/**
 * Data Transfer Objects for Step 05 Affiliate Command Center API Endpoints.
 */

data class AffiliateCommandCenterOverviewResponseDto(
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
    val lastRefreshedAt: Long
)

fun AffiliateCommandCenterOverview.toDto(): AffiliateCommandCenterOverviewResponseDto =
    AffiliateCommandCenterOverviewResponseDto(
        tenantId = tenantId,
        totalAffiliates = totalAffiliates,
        draftCount = draftCount,
        pendingReviewCount = pendingReviewCount,
        activeCount = activeCount,
        suspendedCount = suspendedCount,
        rejectedCount = rejectedCount,
        terminatedCount = terminatedCount,
        verificationPendingCount = verificationPendingCount,
        profileIncompleteCount = profileIncompleteCount,
        agreementPendingCount = agreementPendingCount,
        governanceAttentionRequiredCount = governanceAttentionRequiredCount,
        communicationAttentionRequiredCount = communicationAttentionRequiredCount,
        openWorkItemsCount = openWorkItemsCount,
        urgentWorkItemsCount = urgentWorkItemsCount,
        lastRefreshedAt = lastRefreshedAt
    )

data class AffiliateGovernanceWorkItemResponseDto(
    val tenantId: String,
    val workItemId: String,
    val affiliateId: String,
    val programId: String?,
    val itemType: String,
    val priority: String,
    val status: String,
    val title: String,
    val description: String,
    val requiredAction: String,
    val assignedRole: String?,
    val assignedUserId: String?,
    val resolutionNotes: String?,
    val resolvedByUserId: String?,
    val resolvedAt: Long?,
    val createdAt: Long,
    val updatedAt: Long,
    val version: Long
)

fun AffiliateGovernanceWorkItem.toDto(): AffiliateGovernanceWorkItemResponseDto =
    AffiliateGovernanceWorkItemResponseDto(
        tenantId = tenantId,
        workItemId = workItemId,
        affiliateId = affiliateId,
        programId = programId,
        itemType = itemType.name,
        priority = priority.name,
        status = status.name,
        title = title,
        description = description,
        requiredAction = requiredAction,
        assignedRole = assignedRole,
        assignedUserId = assignedUserId,
        resolutionNotes = resolutionNotes,
        resolvedByUserId = resolvedByUserId,
        resolvedAt = resolvedAt,
        createdAt = createdAt,
        updatedAt = updatedAt,
        version = version
    )

data class ResolveWorkItemRequestDto(
    val workItemId: String,
    val resolutionNotes: String,
    val status: String = "RESOLVED"
)

data class AdminLifecycleActionRequestDto(
    val affiliateId: String,
    val action: String, // APPROVE, REJECT, SUSPEND, REACTIVATE, TERMINATE
    val reason: String
)

data class AffiliateCommandCenterAuditResponseDto(
    val tenantId: String,
    val auditId: String,
    val affiliateId: String?,
    val workItemId: String?,
    val actorUserId: String,
    val actorRole: String,
    val actorType: String,
    val action: String,
    val previousState: String?,
    val newState: String,
    val reason: String?,
    val correlationId: String,
    val chainHash: String,
    val timestamp: Long
)

fun AffiliateGovernanceWorkItemAuditRecord.toDto(): AffiliateCommandCenterAuditResponseDto =
    AffiliateCommandCenterAuditResponseDto(
        tenantId = tenantId,
        auditId = auditId,
        affiliateId = affiliateId,
        workItemId = workItemId,
        actorUserId = actorUserId,
        actorRole = actorRole,
        actorType = actorType.name,
        action = action,
        previousState = previousState,
        newState = newState,
        reason = reason,
        correlationId = correlationId,
        chainHash = chainHash,
        timestamp = timestamp
    )
