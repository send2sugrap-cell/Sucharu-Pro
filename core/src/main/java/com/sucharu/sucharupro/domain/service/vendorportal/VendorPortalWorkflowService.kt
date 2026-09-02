package com.sucharu.sucharupro.domain.service.vendorportal

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.vendorportal.*

/**
 * Service Contract for End-to-End Vendor Workflow Orchestration (Module 13 Step 11).
 */
interface VendorPortalWorkflowService {

    /**
     * Retrieves aggregated metrics, stage breakdown, and urgent actions for the Workflow Hub Command Center.
     */
    suspend fun getWorkflowHubSummary(
        tenantId: String,
        projectId: String,
        vendorId: String
    ): DomainResult<VendorWorkflowHubSummary>

    /**
     * Lists paginated workflows matching status and stage filters.
     */
    suspend fun listWorkflows(
        tenantId: String,
        projectId: String,
        vendorId: String,
        status: VendorWorkflowStatus? = null,
        stage: VendorWorkflowStage? = null,
        limit: Int = 50,
        offset: Int = 0
    ): DomainResult<List<VendorWorkflowItem>>

    /**
     * Retrieves a single workflow item by its workflow ID.
     */
    suspend fun getWorkflowDetails(
        tenantId: String,
        projectId: String,
        vendorId: String,
        workflowId: String
    ): DomainResult<VendorWorkflowItem>

    /**
     * Retrieves the chronological, append-only lifecycle timeline events for a workflow.
     */
    suspend fun getWorkflowTimeline(
        tenantId: String,
        projectId: String,
        vendorId: String,
        workflowId: String
    ): DomainResult<List<VendorWorkflowTimelineEvent>>

    /**
     * Retrieves operational exceptions and blockers for a workflow.
     */
    suspend fun getWorkflowExceptions(
        tenantId: String,
        projectId: String,
        vendorId: String,
        workflowId: String? = null,
        status: VendorWorkflowExceptionStatus? = null
    ): DomainResult<List<VendorWorkflowException>>

    /**
     * Computes role-aware next actions for the authenticated vendor user.
     */
    suspend fun getWorkflowNextActions(
        tenantId: String,
        projectId: String,
        vendorId: String,
        workflowId: String? = null,
        userRole: String? = null
    ): DomainResult<List<VendorWorkflowNextAction>>

    /**
     * Calculates SLA milestone deadlines and breach status for a workflow.
     */
    suspend fun getWorkflowSlaProjection(
        tenantId: String,
        projectId: String,
        vendorId: String,
        workflowId: String
    ): DomainResult<VendorWorkflowSlaProjection>

    /**
     * Acknowledges or completes a workflow action idempotently.
     */
    suspend fun acknowledgeWorkflowAction(
        tenantId: String,
        projectId: String,
        vendorId: String,
        workflowId: String,
        actionId: String,
        actorId: String
    ): DomainResult<VendorWorkflowNextAction>

    /**
     * Records an operational exception/blocker against a workflow.
     */
    suspend fun recordWorkflowException(
        tenantId: String,
        projectId: String,
        vendorId: String,
        workflowId: String,
        category: String,
        severity: VendorWorkflowPriority,
        title: String,
        description: String,
        actorId: String
    ): DomainResult<VendorWorkflowException>

    /**
     * Resolves an open exception with resolution notes.
     */
    suspend fun resolveWorkflowException(
        tenantId: String,
        projectId: String,
        vendorId: String,
        exceptionId: String,
        resolutionNotes: String,
        actorId: String
    ): DomainResult<VendorWorkflowException>

    /**
     * Synchronizes and synthesizes a workflow from authoritative Module 12 entities (RFQ, PO, WO, Invoices, Settlements).
     */
    suspend fun synchronizeWorkflowFromModule12(
        tenantId: String,
        projectId: String,
        vendorId: String,
        correlationId: String
    ): DomainResult<VendorWorkflowItem>
}
