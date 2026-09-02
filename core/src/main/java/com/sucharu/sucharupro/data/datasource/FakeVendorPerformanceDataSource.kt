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
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

class FakeVendorPerformanceDataSource : VendorPerformanceDataSource {

    private val kpis = ConcurrentHashMap<String, VendorPerformanceKpi>()
    private val measurements = ConcurrentHashMap<String, VendorPerformanceMeasurement>()
    private val scorecards = ConcurrentHashMap<String, VendorPerformanceScorecard>()
    private val evaluations = ConcurrentHashMap<String, VendorEvaluation>()
    private val requirements = ConcurrentHashMap<String, VendorComplianceRequirement>()
    private val records = ConcurrentHashMap<String, VendorComplianceRecord>()
    private val evidenceList = ConcurrentHashMap<String, VendorComplianceEvidence>()
    private val correctiveActions = ConcurrentHashMap<String, VendorCorrectiveAction>()
    private val riskIndicators = ConcurrentHashMap<String, VendorRiskIndicator>()
    private val auditEvents = ConcurrentHashMap<String, VendorPerformanceAuditEvent>()

    private val scorecardsFlow = MutableStateFlow<List<VendorPerformanceScorecard>>(emptyList())
    private val evaluationsFlow = MutableStateFlow<List<VendorEvaluation>>(emptyList())
    private val correctiveActionsFlow = MutableStateFlow<List<VendorCorrectiveAction>>(emptyList())

    // --- KPIs ---
    override suspend fun createKpi(kpi: VendorPerformanceKpi): DomainResult<VendorPerformanceKpi> = synchronized(this) {
        val existingWithCode = kpis.values.find { it.projectId == kpi.projectId && it.code.equals(kpi.code, ignoreCase = true) }
        if (existingWithCode != null) {
            return@synchronized DomainResult.Error(IllegalArgumentException("KPI with code '${kpi.code}' already exists in project '${kpi.projectId}'"))
        }
        val key = "${kpi.projectId}:${kpi.kpiId}"
        if (kpis.containsKey(key)) {
            return@synchronized DomainResult.Error(IllegalArgumentException("KPI with ID '${kpi.kpiId}' already exists"))
        }
        kpis[key] = kpi
        DomainResult.Success(kpi)
    }

    override suspend fun updateKpi(kpi: VendorPerformanceKpi): DomainResult<VendorPerformanceKpi> = synchronized(this) {
        val key = "${kpi.projectId}:${kpi.kpiId}"
        val existing = kpis[key] ?: return@synchronized DomainResult.Error(NoSuchElementException("KPI not found"))
        if (existing.version != kpi.version - 1) {
            return@synchronized DomainResult.Error(IllegalStateException("Optimistic concurrency conflict on KPI update"))
        }
        kpis[key] = kpi
        DomainResult.Success(kpi)
    }

    override suspend fun findKpiById(projectId: String, kpiId: String): DomainResult<VendorPerformanceKpi> {
        val kpi = kpis["$projectId:$kpiId"]
        return if (kpi != null) DomainResult.Success(kpi) else DomainResult.Error(NoSuchElementException("KPI '$kpiId' not found"))
    }

    override suspend fun findKpiByCode(projectId: String, code: String): DomainResult<VendorPerformanceKpi> {
        val kpi = kpis.values.find { it.projectId == projectId && it.code.equals(code, ignoreCase = true) }
        return if (kpi != null) DomainResult.Success(kpi) else DomainResult.Error(NoSuchElementException("KPI with code '$code' not found"))
    }

    override suspend fun listKpis(projectId: String, status: KpiStatus?, kpiType: KpiType?): DomainResult<List<VendorPerformanceKpi>> {
        val list = kpis.values.filter {
            it.projectId == projectId &&
            (status == null || it.status == status) &&
            (kpiType == null || it.kpiType == kpiType)
        }.sortedBy { it.code }
        return DomainResult.Success(list)
    }

    // --- Measurements ---
    override suspend fun createMeasurement(measurement: VendorPerformanceMeasurement): DomainResult<VendorPerformanceMeasurement> = synchronized(this) {
        val key = "${measurement.projectId}:${measurement.measurementId}"
        measurements[key] = measurement
        DomainResult.Success(measurement)
    }

    override suspend fun listMeasurements(
        projectId: String,
        vendorId: String,
        kpiId: String?,
        periodStart: Instant?,
        periodEnd: Instant?
    ): DomainResult<List<VendorPerformanceMeasurement>> {
        val list = measurements.values.filter {
            it.projectId == projectId &&
            it.vendorId == vendorId &&
            (kpiId == null || it.kpiId == kpiId) &&
            (periodStart == null || !it.periodStart.isBefore(periodStart)) &&
            (periodEnd == null || !it.periodEnd.isAfter(periodEnd))
        }.sortedBy { it.measuredAt }
        return DomainResult.Success(list)
    }

    // --- Scorecards ---
    override suspend fun createScorecard(scorecard: VendorPerformanceScorecard): DomainResult<VendorPerformanceScorecard> = synchronized(this) {
        val key = "${scorecard.projectId}:${scorecard.scorecardId}"
        if (scorecards.containsKey(key)) {
            return@synchronized DomainResult.Error(IllegalArgumentException("Scorecard '${scorecard.scorecardId}' already exists"))
        }
        scorecards[key] = scorecard
        scorecardsFlow.value = scorecards.values.toList()
        DomainResult.Success(scorecard)
    }

    override suspend fun updateScorecard(scorecard: VendorPerformanceScorecard): DomainResult<VendorPerformanceScorecard> = synchronized(this) {
        val key = "${scorecard.projectId}:${scorecard.scorecardId}"
        val existing = scorecards[key] ?: return@synchronized DomainResult.Error(NoSuchElementException("Scorecard not found"))
        if (existing.status == ScorecardStatus.FINALIZED) {
            return@synchronized DomainResult.Error(IllegalStateException("Finalized scorecard cannot be modified"))
        }
        if (existing.version != scorecard.version - 1) {
            return@synchronized DomainResult.Error(IllegalStateException("Optimistic concurrency conflict on scorecard update"))
        }
        scorecards[key] = scorecard
        scorecardsFlow.value = scorecards.values.toList()
        DomainResult.Success(scorecard)
    }

    override suspend fun findScorecardById(projectId: String, scorecardId: String): DomainResult<VendorPerformanceScorecard> {
        val sc = scorecards["$projectId:$scorecardId"]
        return if (sc != null) DomainResult.Success(sc) else DomainResult.Error(NoSuchElementException("Scorecard '$scorecardId' not found"))
    }

    override suspend fun listScorecards(projectId: String, vendorId: String, status: ScorecardStatus?): DomainResult<List<VendorPerformanceScorecard>> {
        val list = scorecards.values.filter {
            it.projectId == projectId &&
            it.vendorId == vendorId &&
            (status == null || it.status == status)
        }.sortedByDescending { it.periodEnd }
        return DomainResult.Success(list)
    }

    override fun observeScorecards(projectId: String, vendorId: String?): Flow<List<VendorPerformanceScorecard>> {
        return scorecardsFlow.map { list ->
            list.filter { it.projectId == projectId && (vendorId == null || it.vendorId == vendorId) }
        }
    }

    // --- Evaluations ---
    override suspend fun createEvaluation(evaluation: VendorEvaluation): DomainResult<VendorEvaluation> = synchronized(this) {
        val key = "${evaluation.projectId}:${evaluation.evaluationId}"
        if (evaluations.containsKey(key)) {
            return@synchronized DomainResult.Error(IllegalArgumentException("Evaluation '${evaluation.evaluationId}' already exists"))
        }
        evaluations[key] = evaluation
        evaluationsFlow.value = evaluations.values.toList()
        DomainResult.Success(evaluation)
    }

    override suspend fun updateEvaluation(evaluation: VendorEvaluation): DomainResult<VendorEvaluation> = synchronized(this) {
        val key = "${evaluation.projectId}:${evaluation.evaluationId}"
        val existing = evaluations[key] ?: return@synchronized DomainResult.Error(NoSuchElementException("Evaluation not found"))
        if (existing.status == EvaluationStatus.FINALIZED) {
            return@synchronized DomainResult.Error(IllegalStateException("Finalized evaluation cannot be modified"))
        }
        if (existing.version != evaluation.version - 1) {
            return@synchronized DomainResult.Error(IllegalStateException("Optimistic concurrency conflict on evaluation update"))
        }
        evaluations[key] = evaluation
        evaluationsFlow.value = evaluations.values.toList()
        DomainResult.Success(evaluation)
    }

    override suspend fun findEvaluationById(projectId: String, evaluationId: String): DomainResult<VendorEvaluation> {
        val ev = evaluations["$projectId:$evaluationId"]
        return if (ev != null) DomainResult.Success(ev) else DomainResult.Error(NoSuchElementException("Evaluation '$evaluationId' not found"))
    }

    override suspend fun listEvaluations(projectId: String, vendorId: String?, status: EvaluationStatus?): DomainResult<List<VendorEvaluation>> {
        val list = evaluations.values.filter {
            it.projectId == projectId &&
            (vendorId == null || it.vendorId == vendorId) &&
            (status == null || it.status == status)
        }.sortedByDescending { it.periodEnd }
        return DomainResult.Success(list)
    }

    override fun observeEvaluations(projectId: String, vendorId: String?): Flow<List<VendorEvaluation>> {
        return evaluationsFlow.map { list ->
            list.filter { it.projectId == projectId && (vendorId == null || it.vendorId == vendorId) }
        }
    }

    // --- Compliance Requirements ---
    override suspend fun createComplianceRequirement(requirement: VendorComplianceRequirement): DomainResult<VendorComplianceRequirement> = synchronized(this) {
        val existingWithCode = requirements.values.find { it.projectId == requirement.projectId && it.code.equals(requirement.code, ignoreCase = true) }
        if (existingWithCode != null) {
            return@synchronized DomainResult.Error(IllegalArgumentException("Compliance requirement with code '${requirement.code}' already exists"))
        }
        val key = "${requirement.projectId}:${requirement.requirementId}"
        requirements[key] = requirement
        DomainResult.Success(requirement)
    }

    override suspend fun updateComplianceRequirement(requirement: VendorComplianceRequirement): DomainResult<VendorComplianceRequirement> = synchronized(this) {
        val key = "${requirement.projectId}:${requirement.requirementId}"
        val existing = requirements[key] ?: return@synchronized DomainResult.Error(NoSuchElementException("Compliance requirement not found"))
        if (existing.version != requirement.version - 1) {
            return@synchronized DomainResult.Error(IllegalStateException("Optimistic concurrency conflict on compliance requirement update"))
        }
        requirements[key] = requirement
        DomainResult.Success(requirement)
    }

    override suspend fun findComplianceRequirementById(projectId: String, requirementId: String): DomainResult<VendorComplianceRequirement> {
        val req = requirements["$projectId:$requirementId"]
        return if (req != null) DomainResult.Success(req) else DomainResult.Error(NoSuchElementException("Requirement not found"))
    }

    override suspend fun findComplianceRequirementByCode(projectId: String, code: String): DomainResult<VendorComplianceRequirement> {
        val req = requirements.values.find { it.projectId == projectId && it.code.equals(code, ignoreCase = true) }
        return if (req != null) DomainResult.Success(req) else DomainResult.Error(NoSuchElementException("Requirement with code '$code' not found"))
    }

    override suspend fun listComplianceRequirements(projectId: String, status: ComplianceStatus?): DomainResult<List<VendorComplianceRequirement>> {
        val list = requirements.values.filter {
            it.projectId == projectId && (status == null || it.status == status)
        }.sortedBy { it.code }
        return DomainResult.Success(list)
    }

    // --- Compliance Records & Evidence ---
    override suspend fun createComplianceRecord(record: VendorComplianceRecord): DomainResult<VendorComplianceRecord> = synchronized(this) {
        val key = "${record.projectId}:${record.recordId}"
        records[key] = record
        DomainResult.Success(record)
    }

    override suspend fun updateComplianceRecord(record: VendorComplianceRecord): DomainResult<VendorComplianceRecord> = synchronized(this) {
        val key = "${record.projectId}:${record.recordId}"
        val existing = records[key] ?: return@synchronized DomainResult.Error(NoSuchElementException("Compliance record not found"))
        if (existing.version != record.version - 1) {
            return@synchronized DomainResult.Error(IllegalStateException("Optimistic concurrency conflict on compliance record update"))
        }
        records[key] = record
        DomainResult.Success(record)
    }

    override suspend fun findComplianceRecordById(projectId: String, recordId: String): DomainResult<VendorComplianceRecord> {
        val rec = records["$projectId:$recordId"]
        return if (rec != null) DomainResult.Success(rec) else DomainResult.Error(NoSuchElementException("Compliance record '$recordId' not found"))
    }

    override suspend fun listComplianceRecords(projectId: String, vendorId: String?, status: ComplianceStatus?): DomainResult<List<VendorComplianceRecord>> {
        val list = records.values.filter {
            it.projectId == projectId &&
            (vendorId == null || it.vendorId == vendorId) &&
            (status == null || it.status == status)
        }.sortedByDescending { it.createdAt }
        return DomainResult.Success(list)
    }

    override suspend fun addComplianceEvidence(evidence: VendorComplianceEvidence): DomainResult<VendorComplianceEvidence> = synchronized(this) {
        val key = "${evidence.projectId}:${evidence.evidenceId}"
        evidenceList[key] = evidence
        DomainResult.Success(evidence)
    }

    override suspend fun listComplianceEvidence(projectId: String, recordId: String): DomainResult<List<VendorComplianceEvidence>> {
        val list = evidenceList.values.filter {
            it.projectId == projectId && it.recordId == recordId
        }.sortedBy { it.uploadedAt }
        return DomainResult.Success(list)
    }

    // --- Corrective Actions ---
    override suspend fun createCorrectiveAction(action: VendorCorrectiveAction): DomainResult<VendorCorrectiveAction> = synchronized(this) {
        val key = "${action.projectId}:${action.actionId}"
        if (correctiveActions.containsKey(key)) {
            return@synchronized DomainResult.Error(IllegalArgumentException("Corrective action '${action.actionId}' already exists"))
        }
        correctiveActions[key] = action
        correctiveActionsFlow.value = correctiveActions.values.toList()
        DomainResult.Success(action)
    }

    override suspend fun updateCorrectiveAction(action: VendorCorrectiveAction): DomainResult<VendorCorrectiveAction> = synchronized(this) {
        val key = "${action.projectId}:${action.actionId}"
        val existing = correctiveActions[key] ?: return@synchronized DomainResult.Error(NoSuchElementException("Corrective action not found"))
        if (existing.version != action.version - 1) {
            return@synchronized DomainResult.Error(IllegalStateException("Optimistic concurrency conflict on corrective action update"))
        }
        correctiveActions[key] = action
        correctiveActionsFlow.value = correctiveActions.values.toList()
        DomainResult.Success(action)
    }

    override suspend fun findCorrectiveActionById(projectId: String, actionId: String): DomainResult<VendorCorrectiveAction> {
        val act = correctiveActions["$projectId:$actionId"]
        return if (act != null) DomainResult.Success(act) else DomainResult.Error(NoSuchElementException("Corrective action '$actionId' not found"))
    }

    override suspend fun listCorrectiveActions(projectId: String, vendorId: String?, status: CorrectiveActionStatus?): DomainResult<List<VendorCorrectiveAction>> {
        val list = correctiveActions.values.filter {
            it.projectId == projectId &&
            (vendorId == null || it.vendorId == vendorId) &&
            (status == null || it.status == status)
        }.sortedBy { it.dueDate }
        return DomainResult.Success(list)
    }

    override fun observeCorrectiveActions(projectId: String, vendorId: String?): Flow<List<VendorCorrectiveAction>> {
        return correctiveActionsFlow.map { list ->
            list.filter { it.projectId == projectId && (vendorId == null || it.vendorId == vendorId) }
        }
    }

    // --- Risk Indicators & Audit Trail ---
    override suspend fun createRiskIndicator(risk: VendorRiskIndicator): DomainResult<VendorRiskIndicator> = synchronized(this) {
        val key = "${risk.projectId}:${risk.riskId}"
        riskIndicators[key] = risk
        DomainResult.Success(risk)
    }

    override suspend fun updateRiskIndicator(risk: VendorRiskIndicator): DomainResult<VendorRiskIndicator> = synchronized(this) {
        val key = "${risk.projectId}:${risk.riskId}"
        riskIndicators[key] = risk
        DomainResult.Success(risk)
    }

    override suspend fun listRiskIndicators(projectId: String, vendorId: String?, status: RiskStatus?): DomainResult<List<VendorRiskIndicator>> {
        val list = riskIndicators.values.filter {
            it.projectId == projectId &&
            (vendorId == null || it.vendorId == vendorId) &&
            (status == null || it.status == status)
        }.sortedByDescending { it.detectedAt }
        return DomainResult.Success(list)
    }

    override suspend fun appendAuditEvent(event: VendorPerformanceAuditEvent): DomainResult<VendorPerformanceAuditEvent> = synchronized(this) {
        val key = "${event.projectId}:${event.auditId}"
        auditEvents[key] = event
        DomainResult.Success(event)
    }

    override suspend fun listAuditEvents(projectId: String, entityId: String): DomainResult<List<VendorPerformanceAuditEvent>> {
        val list = auditEvents.values.filter {
            it.projectId == projectId && it.entityId == entityId
        }.sortedBy { it.occurredAt }
        return DomainResult.Success(list)
    }
}
