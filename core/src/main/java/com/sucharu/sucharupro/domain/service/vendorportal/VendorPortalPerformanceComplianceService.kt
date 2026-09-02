package com.sucharu.sucharupro.domain.service.vendorportal

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.vendorportal.*

/**
 * Service interface for Vendor Portal Performance & Compliance Workspace (Module 13 Step 08).
 */
interface VendorPortalPerformanceComplianceService {

    // Performance
    suspend fun getPerformanceOverview(tenantId: String, projectId: String, vendorId: String): DomainResult<VendorPortalPerformanceOverview>
    suspend fun listPerformanceKpis(tenantId: String, projectId: String, vendorId: String): DomainResult<List<VendorPortalPerformanceKpiSummary>>
    suspend fun getPerformanceTrends(tenantId: String, projectId: String, vendorId: String): DomainResult<List<VendorPortalPerformanceTrendPoint>>
    suspend fun listScorecards(tenantId: String, projectId: String, vendorId: String): DomainResult<List<VendorPortalPerformanceScorecardSummary>>
    suspend fun getScorecardById(tenantId: String, projectId: String, vendorId: String, scorecardId: String): DomainResult<VendorPortalPerformanceScorecardSummary>

    // Evaluations
    suspend fun listEvaluations(tenantId: String, projectId: String, vendorId: String): DomainResult<List<VendorPortalEvaluationSummary>>
    suspend fun getEvaluationById(tenantId: String, projectId: String, vendorId: String, evaluationId: String): DomainResult<VendorPortalEvaluationSummary>
    suspend fun acknowledgeEvaluation(tenantId: String, projectId: String, vendorId: String, evaluationId: String, actorId: String): DomainResult<VendorPortalEvaluationSummary>
    suspend fun submitEvaluationResponse(
        tenantId: String,
        projectId: String,
        vendorId: String,
        evaluationId: String,
        subject: String,
        remarks: String,
        proposedRemediation: String? = null,
        evidenceReferences: List<String> = emptyList(),
        actorId: String
    ): DomainResult<VendorPortalEvaluationResponse>
    suspend fun listEvaluationResponses(tenantId: String, projectId: String, vendorId: String, evaluationId: String): DomainResult<List<VendorPortalEvaluationResponse>>

    // Compliance
    suspend fun getComplianceOverview(tenantId: String, projectId: String, vendorId: String): DomainResult<VendorPortalComplianceOverview>
    suspend fun listComplianceRequirements(tenantId: String, projectId: String): DomainResult<List<VendorPortalComplianceRequirementSummary>>
    suspend fun listComplianceRecords(tenantId: String, projectId: String, vendorId: String): DomainResult<List<VendorPortalComplianceRecordSummary>>
    suspend fun listCertificationExpiries(tenantId: String, projectId: String, vendorId: String): DomainResult<List<VendorPortalCertificationExpiryAlert>>
    suspend fun uploadComplianceEvidence(
        tenantId: String,
        projectId: String,
        vendorId: String,
        recordId: String? = null,
        requirementId: String? = null,
        actionId: String? = null,
        evidenceType: VendorPortalComplianceEvidenceType = VendorPortalComplianceEvidenceType.DOCUMENT,
        fileName: String,
        fileUrl: String,
        checksum: String? = null,
        fileSizeBytes: Long = 0L,
        mimeType: String? = null,
        description: String? = null,
        actorId: String
    ): DomainResult<VendorPortalComplianceEvidence>
    suspend fun listComplianceEvidence(tenantId: String, projectId: String, vendorId: String, recordId: String? = null, actionId: String? = null): DomainResult<List<VendorPortalComplianceEvidence>>

    // Corrective Actions (CAPA)
    suspend fun listCorrectiveActions(tenantId: String, projectId: String, vendorId: String): DomainResult<List<VendorPortalCorrectiveActionSummary>>
    suspend fun getCorrectiveActionById(tenantId: String, projectId: String, vendorId: String, actionId: String): DomainResult<VendorPortalCorrectiveActionSummary>
    suspend fun submitCorrectiveActionResponse(
        tenantId: String,
        projectId: String,
        vendorId: String,
        actionId: String,
        remediationNotes: String,
        rootCauseExplanation: String? = null,
        progressPercentage: Double = 0.0,
        evidenceReferences: List<String> = emptyList(),
        actorId: String
    ): DomainResult<VendorPortalCorrectiveActionResponse>
    suspend fun submitCorrectiveActionCompletionRequest(
        tenantId: String,
        projectId: String,
        vendorId: String,
        actionId: String,
        completionNotes: String,
        evidenceReferences: List<String> = emptyList(),
        actorId: String
    ): DomainResult<VendorPortalCorrectiveActionResponse>

    // Activity & Consolidated Workspace
    suspend fun listPerformanceComplianceActivity(tenantId: String, projectId: String, vendorId: String, entityType: String? = null, entityId: String? = null): DomainResult<List<VendorPortalPerformanceComplianceActivity>>
    suspend fun getWorkspace(tenantId: String, projectId: String, vendorId: String): DomainResult<VendorPortalPerformanceWorkspace>
}
