package com.sucharu.sucharupro.data.datasource

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.vendor.ComplianceStatus
import com.sucharu.sucharupro.domain.model.vendor.CorrectiveActionStatus
import com.sucharu.sucharupro.domain.model.vendor.EvaluationStatus
import com.sucharu.sucharupro.domain.model.vendor.KpiStatus
import com.sucharu.sucharupro.domain.model.vendor.KpiType
import com.sucharu.sucharupro.domain.model.vendor.RiskStatus
import com.sucharu.sucharupro.domain.model.vendor.ScorecardStatus
import com.sucharu.sucharupro.domain.model.vendor.VendorComplianceEvidence
import com.sucharu.sucharupro.domain.model.vendor.VendorComplianceRecord
import com.sucharu.sucharupro.domain.model.vendor.VendorComplianceRequirement
import com.sucharu.sucharupro.domain.model.vendor.VendorCorrectiveAction
import com.sucharu.sucharupro.domain.model.vendor.VendorEvaluation
import com.sucharu.sucharupro.domain.model.vendor.VendorPerformanceAuditEvent
import com.sucharu.sucharupro.domain.model.vendor.VendorPerformanceKpi
import com.sucharu.sucharupro.domain.model.vendor.VendorPerformanceMeasurement
import com.sucharu.sucharupro.domain.model.vendor.VendorPerformanceScorecard
import com.sucharu.sucharupro.domain.model.vendor.VendorRiskIndicator
import java.time.Instant
import kotlinx.coroutines.flow.Flow

/**
 * Data source interface for Vendor Performance, Evaluation, Compliance, and Corrective Actions.
 */
interface VendorPerformanceDataSource {

    // KPIs
    suspend fun createKpi(kpi: VendorPerformanceKpi): DomainResult<VendorPerformanceKpi>
    suspend fun updateKpi(kpi: VendorPerformanceKpi): DomainResult<VendorPerformanceKpi>
    suspend fun findKpiById(projectId: String, kpiId: String): DomainResult<VendorPerformanceKpi>
    suspend fun findKpiByCode(projectId: String, code: String): DomainResult<VendorPerformanceKpi>
    suspend fun listKpis(projectId: String, status: KpiStatus? = null, kpiType: KpiType? = null): DomainResult<List<VendorPerformanceKpi>>

    // Measurements
    suspend fun createMeasurement(measurement: VendorPerformanceMeasurement): DomainResult<VendorPerformanceMeasurement>
    suspend fun listMeasurements(
        projectId: String,
        vendorId: String,
        kpiId: String? = null,
        periodStart: Instant? = null,
        periodEnd: Instant? = null
    ): DomainResult<List<VendorPerformanceMeasurement>>

    // Scorecards
    suspend fun createScorecard(scorecard: VendorPerformanceScorecard): DomainResult<VendorPerformanceScorecard>
    suspend fun updateScorecard(scorecard: VendorPerformanceScorecard): DomainResult<VendorPerformanceScorecard>
    suspend fun findScorecardById(projectId: String, scorecardId: String): DomainResult<VendorPerformanceScorecard>
    suspend fun listScorecards(projectId: String, vendorId: String, status: ScorecardStatus? = null): DomainResult<List<VendorPerformanceScorecard>>
    fun observeScorecards(projectId: String, vendorId: String? = null): Flow<List<VendorPerformanceScorecard>>

    // Evaluations
    suspend fun createEvaluation(evaluation: VendorEvaluation): DomainResult<VendorEvaluation>
    suspend fun updateEvaluation(evaluation: VendorEvaluation): DomainResult<VendorEvaluation>
    suspend fun findEvaluationById(projectId: String, evaluationId: String): DomainResult<VendorEvaluation>
    suspend fun listEvaluations(projectId: String, vendorId: String? = null, status: EvaluationStatus? = null): DomainResult<List<VendorEvaluation>>
    fun observeEvaluations(projectId: String, vendorId: String? = null): Flow<List<VendorEvaluation>>

    // Compliance Requirements
    suspend fun createComplianceRequirement(requirement: VendorComplianceRequirement): DomainResult<VendorComplianceRequirement>
    suspend fun updateComplianceRequirement(requirement: VendorComplianceRequirement): DomainResult<VendorComplianceRequirement>
    suspend fun findComplianceRequirementById(projectId: String, requirementId: String): DomainResult<VendorComplianceRequirement>
    suspend fun findComplianceRequirementByCode(projectId: String, code: String): DomainResult<VendorComplianceRequirement>
    suspend fun listComplianceRequirements(projectId: String, status: ComplianceStatus? = null): DomainResult<List<VendorComplianceRequirement>>

    // Compliance Records & Evidence
    suspend fun createComplianceRecord(record: VendorComplianceRecord): DomainResult<VendorComplianceRecord>
    suspend fun updateComplianceRecord(record: VendorComplianceRecord): DomainResult<VendorComplianceRecord>
    suspend fun findComplianceRecordById(projectId: String, recordId: String): DomainResult<VendorComplianceRecord>
    suspend fun listComplianceRecords(projectId: String, vendorId: String? = null, status: ComplianceStatus? = null): DomainResult<List<VendorComplianceRecord>>
    suspend fun addComplianceEvidence(evidence: VendorComplianceEvidence): DomainResult<VendorComplianceEvidence>
    suspend fun listComplianceEvidence(projectId: String, recordId: String): DomainResult<List<VendorComplianceEvidence>>

    // Corrective Actions
    suspend fun createCorrectiveAction(action: VendorCorrectiveAction): DomainResult<VendorCorrectiveAction>
    suspend fun updateCorrectiveAction(action: VendorCorrectiveAction): DomainResult<VendorCorrectiveAction>
    suspend fun findCorrectiveActionById(projectId: String, actionId: String): DomainResult<VendorCorrectiveAction>
    suspend fun listCorrectiveActions(projectId: String, vendorId: String? = null, status: CorrectiveActionStatus? = null): DomainResult<List<VendorCorrectiveAction>>
    fun observeCorrectiveActions(projectId: String, vendorId: String? = null): Flow<List<VendorCorrectiveAction>>

    // Risk Indicators & Audit Trail
    suspend fun createRiskIndicator(risk: VendorRiskIndicator): DomainResult<VendorRiskIndicator>
    suspend fun updateRiskIndicator(risk: VendorRiskIndicator): DomainResult<VendorRiskIndicator>
    suspend fun listRiskIndicators(projectId: String, vendorId: String? = null, status: RiskStatus? = null): DomainResult<List<VendorRiskIndicator>>
    suspend fun appendAuditEvent(event: VendorPerformanceAuditEvent): DomainResult<VendorPerformanceAuditEvent>
    suspend fun listAuditEvents(projectId: String, entityId: String): DomainResult<List<VendorPerformanceAuditEvent>>
}
