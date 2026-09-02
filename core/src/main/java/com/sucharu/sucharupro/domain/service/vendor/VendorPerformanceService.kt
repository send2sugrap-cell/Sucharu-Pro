package com.sucharu.sucharupro.domain.service.vendor

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.vendor.*
import java.time.Instant

/**
 * Domain service interface for Vendor Performance, Evaluation, Compliance, and Corrective Actions.
 */
interface VendorPerformanceService {

    // --- KPIs ---
    suspend fun createKpi(kpi: VendorPerformanceKpi): DomainResult<VendorPerformanceKpi>
    suspend fun updateKpi(kpi: VendorPerformanceKpi): DomainResult<VendorPerformanceKpi>
    suspend fun getKpiById(projectId: String, kpiId: String): DomainResult<VendorPerformanceKpi>
    suspend fun getKpiByCode(projectId: String, code: String): DomainResult<VendorPerformanceKpi>
    suspend fun listKpis(projectId: String, status: KpiStatus? = null, kpiType: KpiType? = null): DomainResult<List<VendorPerformanceKpi>>

    // --- Measurements ---
    suspend fun recordMeasurement(measurement: VendorPerformanceMeasurement): DomainResult<VendorPerformanceMeasurement>
    suspend fun listMeasurements(
        projectId: String,
        vendorId: String,
        kpiId: String? = null,
        periodStart: Instant? = null,
        periodEnd: Instant? = null
    ): DomainResult<List<VendorPerformanceMeasurement>>

    // --- Scorecards ---
    suspend fun generateScorecard(
        projectId: String,
        tenantId: String,
        vendorId: String,
        periodType: EvaluationPeriodType,
        periodStart: Instant,
        periodEnd: Instant,
        generatedBy: String,
        notes: String? = null
    ): DomainResult<VendorPerformanceScorecard>

    suspend fun getScorecardById(projectId: String, scorecardId: String): DomainResult<VendorPerformanceScorecard>
    suspend fun listScorecards(projectId: String, vendorId: String, status: ScorecardStatus? = null): DomainResult<List<VendorPerformanceScorecard>>
    suspend fun submitScorecardForReview(projectId: String, scorecardId: String, submittedBy: String): DomainResult<VendorPerformanceScorecard>
    suspend fun approveScorecard(projectId: String, scorecardId: String, approvedBy: String): DomainResult<VendorPerformanceScorecard>
    suspend fun rejectScorecard(projectId: String, scorecardId: String, rejectedBy: String, reason: String): DomainResult<VendorPerformanceScorecard>
    suspend fun finalizeScorecard(projectId: String, scorecardId: String, finalizedBy: String): DomainResult<VendorPerformanceScorecard>

    // --- Evaluations ---
    suspend fun createEvaluation(evaluation: VendorEvaluation): DomainResult<VendorEvaluation>
    suspend fun getEvaluationById(projectId: String, evaluationId: String): DomainResult<VendorEvaluation>
    suspend fun listEvaluations(projectId: String, vendorId: String? = null, status: EvaluationStatus? = null): DomainResult<List<VendorEvaluation>>
    suspend fun submitEvaluation(projectId: String, evaluationId: String, submittedBy: String, comments: String? = null): DomainResult<VendorEvaluation>
    suspend fun reviewEvaluation(projectId: String, evaluationId: String, reviewedBy: String, reviewComments: String): DomainResult<VendorEvaluation>
    suspend fun approveEvaluation(
        projectId: String,
        evaluationId: String,
        approverId: String,
        decision: EvaluationDecision = EvaluationDecision.APPROVED,
        comments: String? = null
    ): DomainResult<VendorEvaluation>
    suspend fun rejectEvaluation(projectId: String, evaluationId: String, rejectedBy: String, reason: String): DomainResult<VendorEvaluation>
    suspend fun finalizeEvaluation(projectId: String, evaluationId: String, finalizedBy: String): DomainResult<VendorEvaluation>

    // --- Compliance Requirements & Records ---
    suspend fun createComplianceRequirement(requirement: VendorComplianceRequirement): DomainResult<VendorComplianceRequirement>
    suspend fun updateComplianceRequirement(requirement: VendorComplianceRequirement): DomainResult<VendorComplianceRequirement>
    suspend fun getComplianceRequirementById(projectId: String, requirementId: String): DomainResult<VendorComplianceRequirement>
    suspend fun listComplianceRequirements(projectId: String, status: ComplianceStatus? = null): DomainResult<List<VendorComplianceRequirement>>

    suspend fun submitComplianceRecord(record: VendorComplianceRecord): DomainResult<VendorComplianceRecord>
    suspend fun verifyComplianceRecord(
        projectId: String,
        recordId: String,
        verifiedBy: String,
        verified: Boolean,
        rejectionReason: String? = null,
        notes: String? = null
    ): DomainResult<VendorComplianceRecord>
    suspend fun evaluateComplianceExpiries(projectId: String, vendorId: String? = null): DomainResult<List<VendorComplianceRecord>>
    suspend fun getComplianceRecordById(projectId: String, recordId: String): DomainResult<VendorComplianceRecord>
    suspend fun listComplianceRecords(projectId: String, vendorId: String? = null, status: ComplianceStatus? = null): DomainResult<List<VendorComplianceRecord>>

    // --- Corrective Actions ---
    suspend fun createCorrectiveAction(action: VendorCorrectiveAction): DomainResult<VendorCorrectiveAction>
    suspend fun startCorrectiveAction(projectId: String, actionId: String, updatedBy: String, notes: String? = null): DomainResult<VendorCorrectiveAction>
    suspend fun submitCorrectiveActionForVerification(projectId: String, actionId: String, updatedBy: String, verificationNotes: String): DomainResult<VendorCorrectiveAction>
    suspend fun verifyCorrectiveAction(projectId: String, actionId: String, verifiedBy: String, verificationNotes: String): DomainResult<VendorCorrectiveAction>
    suspend fun closeCorrectiveAction(projectId: String, actionId: String, closedBy: String, notes: String? = null): DomainResult<VendorCorrectiveAction>
    suspend fun getCorrectiveActionById(projectId: String, actionId: String): DomainResult<VendorCorrectiveAction>
    suspend fun listCorrectiveActions(projectId: String, vendorId: String? = null, status: CorrectiveActionStatus? = null): DomainResult<List<VendorCorrectiveAction>>

    // --- Trends, Risk & Audits ---
    suspend fun getVendorPerformanceTrends(projectId: String, vendorId: String): DomainResult<List<VendorPerformanceTrendPoint>>
    suspend fun getVendorRiskIndicators(projectId: String, vendorId: String? = null, status: RiskStatus? = null): DomainResult<List<VendorRiskIndicator>>
    suspend fun getAuditEvents(projectId: String, entityId: String): DomainResult<List<VendorPerformanceAuditEvent>>
}
