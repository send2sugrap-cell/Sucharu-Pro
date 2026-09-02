package com.sucharu.sucharupro.data.repository

import com.sucharu.sucharupro.data.datasource.VendorPerformanceDataSource
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.vendor.*
import com.sucharu.sucharupro.domain.repository.VendorPerformanceRepository
import kotlinx.coroutines.flow.Flow
import java.time.Instant

/**
 * Repository implementation for Vendor Performance, Evaluation, Compliance, and Corrective Actions.
 */
class VendorPerformanceRepositoryImpl(
    private val dataSource: VendorPerformanceDataSource
) : VendorPerformanceRepository {

    override suspend fun createKpi(kpi: VendorPerformanceKpi): DomainResult<VendorPerformanceKpi> =
        dataSource.createKpi(kpi)

    override suspend fun updateKpi(kpi: VendorPerformanceKpi): DomainResult<VendorPerformanceKpi> =
        dataSource.updateKpi(kpi)

    override suspend fun findKpiById(projectId: String, kpiId: String): DomainResult<VendorPerformanceKpi> =
        dataSource.findKpiById(projectId, kpiId)

    override suspend fun findKpiByCode(projectId: String, code: String): DomainResult<VendorPerformanceKpi> =
        dataSource.findKpiByCode(projectId, code)

    override suspend fun listKpis(projectId: String, status: KpiStatus?, kpiType: KpiType?): DomainResult<List<VendorPerformanceKpi>> =
        dataSource.listKpis(projectId, status, kpiType)

    override suspend fun createMeasurement(measurement: VendorPerformanceMeasurement): DomainResult<VendorPerformanceMeasurement> =
        dataSource.createMeasurement(measurement)

    override suspend fun listMeasurements(
        projectId: String,
        vendorId: String,
        kpiId: String?,
        periodStart: Instant?,
        periodEnd: Instant?
    ): DomainResult<List<VendorPerformanceMeasurement>> =
        dataSource.listMeasurements(projectId, vendorId, kpiId, periodStart, periodEnd)

    override suspend fun createScorecard(scorecard: VendorPerformanceScorecard): DomainResult<VendorPerformanceScorecard> =
        dataSource.createScorecard(scorecard)

    override suspend fun updateScorecard(scorecard: VendorPerformanceScorecard): DomainResult<VendorPerformanceScorecard> =
        dataSource.updateScorecard(scorecard)

    override suspend fun findScorecardById(projectId: String, scorecardId: String): DomainResult<VendorPerformanceScorecard> =
        dataSource.findScorecardById(projectId, scorecardId)

    override suspend fun listScorecards(projectId: String, vendorId: String, status: ScorecardStatus?): DomainResult<List<VendorPerformanceScorecard>> =
        dataSource.listScorecards(projectId, vendorId, status)

    override fun observeScorecards(projectId: String, vendorId: String?): Flow<List<VendorPerformanceScorecard>> =
        dataSource.observeScorecards(projectId, vendorId)

    override suspend fun createEvaluation(evaluation: VendorEvaluation): DomainResult<VendorEvaluation> =
        dataSource.createEvaluation(evaluation)

    override suspend fun updateEvaluation(evaluation: VendorEvaluation): DomainResult<VendorEvaluation> =
        dataSource.updateEvaluation(evaluation)

    override suspend fun findEvaluationById(projectId: String, evaluationId: String): DomainResult<VendorEvaluation> =
        dataSource.findEvaluationById(projectId, evaluationId)

    override suspend fun listEvaluations(projectId: String, vendorId: String?, status: EvaluationStatus?): DomainResult<List<VendorEvaluation>> =
        dataSource.listEvaluations(projectId, vendorId, status)

    override fun observeEvaluations(projectId: String, vendorId: String?): Flow<List<VendorEvaluation>> =
        dataSource.observeEvaluations(projectId, vendorId)

    override suspend fun createComplianceRequirement(requirement: VendorComplianceRequirement): DomainResult<VendorComplianceRequirement> =
        dataSource.createComplianceRequirement(requirement)

    override suspend fun updateComplianceRequirement(requirement: VendorComplianceRequirement): DomainResult<VendorComplianceRequirement> =
        dataSource.updateComplianceRequirement(requirement)

    override suspend fun findComplianceRequirementById(projectId: String, requirementId: String): DomainResult<VendorComplianceRequirement> =
        dataSource.findComplianceRequirementById(projectId, requirementId)

    override suspend fun findComplianceRequirementByCode(projectId: String, code: String): DomainResult<VendorComplianceRequirement> =
        dataSource.findComplianceRequirementByCode(projectId, code)

    override suspend fun listComplianceRequirements(projectId: String, status: ComplianceStatus?): DomainResult<List<VendorComplianceRequirement>> =
        dataSource.listComplianceRequirements(projectId, status)

    override suspend fun createComplianceRecord(record: VendorComplianceRecord): DomainResult<VendorComplianceRecord> =
        dataSource.createComplianceRecord(record)

    override suspend fun updateComplianceRecord(record: VendorComplianceRecord): DomainResult<VendorComplianceRecord> =
        dataSource.updateComplianceRecord(record)

    override suspend fun findComplianceRecordById(projectId: String, recordId: String): DomainResult<VendorComplianceRecord> =
        dataSource.findComplianceRecordById(projectId, recordId)

    override suspend fun listComplianceRecords(projectId: String, vendorId: String?, status: ComplianceStatus?): DomainResult<List<VendorComplianceRecord>> =
        dataSource.listComplianceRecords(projectId, vendorId, status)

    override suspend fun addComplianceEvidence(evidence: VendorComplianceEvidence): DomainResult<VendorComplianceEvidence> =
        dataSource.addComplianceEvidence(evidence)

    override suspend fun listComplianceEvidence(projectId: String, recordId: String): DomainResult<List<VendorComplianceEvidence>> =
        dataSource.listComplianceEvidence(projectId, recordId)

    override suspend fun createCorrectiveAction(action: VendorCorrectiveAction): DomainResult<VendorCorrectiveAction> =
        dataSource.createCorrectiveAction(action)

    override suspend fun updateCorrectiveAction(action: VendorCorrectiveAction): DomainResult<VendorCorrectiveAction> =
        dataSource.updateCorrectiveAction(action)

    override suspend fun findCorrectiveActionById(projectId: String, actionId: String): DomainResult<VendorCorrectiveAction> =
        dataSource.findCorrectiveActionById(projectId, actionId)

    override suspend fun listCorrectiveActions(projectId: String, vendorId: String?, status: CorrectiveActionStatus?): DomainResult<List<VendorCorrectiveAction>> =
        dataSource.listCorrectiveActions(projectId, vendorId, status)

    override fun observeCorrectiveActions(projectId: String, vendorId: String?): Flow<List<VendorCorrectiveAction>> =
        dataSource.observeCorrectiveActions(projectId, vendorId)

    override suspend fun createRiskIndicator(risk: VendorRiskIndicator): DomainResult<VendorRiskIndicator> =
        dataSource.createRiskIndicator(risk)

    override suspend fun updateRiskIndicator(risk: VendorRiskIndicator): DomainResult<VendorRiskIndicator> =
        dataSource.updateRiskIndicator(risk)

    override suspend fun listRiskIndicators(projectId: String, vendorId: String?, status: RiskStatus?): DomainResult<List<VendorRiskIndicator>> =
        dataSource.listRiskIndicators(projectId, vendorId, status)

    override suspend fun appendAuditEvent(event: VendorPerformanceAuditEvent): DomainResult<VendorPerformanceAuditEvent> =
        dataSource.appendAuditEvent(event)

    override suspend fun listAuditEvents(projectId: String, entityId: String): DomainResult<List<VendorPerformanceAuditEvent>> =
        dataSource.listAuditEvents(projectId, entityId)
}
