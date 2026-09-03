package com.sucharu.sucharupro.domain.service.affiliate

import com.sucharu.sucharupro.domain.model.affiliate.*

/**
 * Domain Service Interface for Affiliate Administrative Command Center & Governance Operations.
 * Module 20 Step 05.
 */
interface AffiliateCommandCenterService {

    /**
     * Computes consolidated operational state counts for the command center overview dashboard.
     */
    suspend fun getCommandCenterOverview(tenantId: String): AffiliateCommandCenterOverview

    /**
     * Lists prioritized governance work items in the administrative work queue.
     */
    suspend fun listWorkItems(
        tenantId: String,
        priority: AffiliateGovernanceWorkItemPriority? = null,
        status: AffiliateGovernanceWorkItemStatus? = null,
        itemType: AffiliateGovernanceWorkItemType? = null,
        affiliateId: String? = null
    ): List<AffiliateGovernanceWorkItem>

    /**
     * Retrieves a work queue item by ID.
     */
    suspend fun findWorkItemById(tenantId: String, workItemId: String): AffiliateGovernanceWorkItem?

    /**
     * Resolves or dismisses an administrative work queue item with resolution notes.
     */
    suspend fun resolveWorkItem(
        tenantId: String,
        workItemId: String,
        resolutionNotes: String,
        status: AffiliateGovernanceWorkItemStatus = AffiliateGovernanceWorkItemStatus.RESOLVED,
        actorUserId: String,
        actorRole: String,
        correlationId: String
    ): AffiliateGovernanceWorkItem

    /**
     * Generates a 360-degree consolidated administrative detail view for an affiliate.
     */
    suspend fun getAdministrativeDetailView(
        tenantId: String,
        affiliateId: String,
        userId: String
    ): AffiliateAdministrativeDetailView

    /**
     * Executes controlled administrative lifecycle mutations with governance validation,
     * SHA-256 audit block logging, and operational notification dispatch.
     */
    suspend fun executeAdminAction(
        tenantId: String,
        affiliateId: String,
        action: String,
        reason: String,
        actorUserId: String,
        actorRole: String,
        actorType: AffiliateActorType = AffiliateActorType.HUMAN,
        correlationId: String
    ): AffiliateProfile

    /**
     * Returns SHA-256 audit trail for command center administrative actions.
     */
    suspend fun listAuditRecords(
        tenantId: String,
        affiliateId: String? = null
    ): List<AffiliateGovernanceWorkItemAuditRecord>

    /**
     * Synthesizes immutable AI Governance Handoff Contract for Step 05 context.
     */
    suspend fun getHandoffContract(
        tenantId: String,
        userId: String
    ): Module20Step05AffiliateCommandCenterHandoffContract
}
