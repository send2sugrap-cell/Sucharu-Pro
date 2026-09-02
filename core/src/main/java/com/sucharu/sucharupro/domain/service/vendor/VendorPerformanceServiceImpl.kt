package com.sucharu.sucharupro.domain.service.vendor

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.vendor.*
import com.sucharu.sucharupro.domain.repository.VendorDeliveryReceiptRepository
import com.sucharu.sucharupro.domain.repository.VendorInvoiceRepository
import com.sucharu.sucharupro.domain.repository.VendorPerformanceRepository
import com.sucharu.sucharupro.domain.repository.VendorPurchaseOrderRepository
import com.sucharu.sucharupro.domain.repository.VendorQualityRepository
import com.sucharu.sucharupro.domain.repository.VendorRepository
import com.sucharu.sucharupro.domain.validation.vendor.VendorPerformanceValidator
import java.time.Instant
import java.util.UUID

class VendorPerformanceServiceImpl(
    private val performanceRepository: VendorPerformanceRepository,
    private val vendorRepository: VendorRepository,
    private val purchaseOrderRepository: VendorPurchaseOrderRepository? = null,
    private val receiptRepository: VendorDeliveryReceiptRepository? = null,
    private val qualityRepository: VendorQualityRepository? = null,
    private val invoiceRepository: VendorInvoiceRepository? = null
) : VendorPerformanceService {

    // --- KPIs ---
    override suspend fun createKpi(kpi: VendorPerformanceKpi): DomainResult<VendorPerformanceKpi> {
        val valResult = VendorPerformanceValidator.validateKpi(kpi)
        if (valResult is DomainResult.Error) return valResult

        val created = performanceRepository.createKpi(kpi)
        if (created is DomainResult.Success) {
            performanceRepository.appendAuditEvent(
                VendorPerformanceAuditEvent(
                    auditId = UUID.randomUUID().toString(),
                    projectId = kpi.projectId,
                    tenantId = kpi.tenantId,
                    entityType = "KPI",
                    entityId = kpi.kpiId,
                    eventType = VendorPerformanceAuditEventType.KPI_CREATED,
                    action = "Created performance KPI '${kpi.code}'",
                    actorId = kpi.createdBy,
                    details = "KPI ${kpi.name} created with target ${kpi.targetValue} ${kpi.unit}"
                )
            )
        }
        return created
    }

    override suspend fun updateKpi(kpi: VendorPerformanceKpi): DomainResult<VendorPerformanceKpi> {
        val valResult = VendorPerformanceValidator.validateKpi(kpi)
        if (valResult is DomainResult.Error) return valResult

        val updated = performanceRepository.updateKpi(kpi)
        if (updated is DomainResult.Success) {
            performanceRepository.appendAuditEvent(
                VendorPerformanceAuditEvent(
                    auditId = UUID.randomUUID().toString(),
                    projectId = kpi.projectId,
                    tenantId = kpi.tenantId,
                    entityType = "KPI",
                    entityId = kpi.kpiId,
                    eventType = VendorPerformanceAuditEventType.KPI_UPDATED,
                    action = "Updated performance KPI '${kpi.code}'",
                    actorId = kpi.updatedBy ?: "system",
                    details = "KPI ${kpi.code} updated to status ${kpi.status}"
                )
            )
        }
        return updated
    }

    override suspend fun getKpiById(projectId: String, kpiId: String): DomainResult<VendorPerformanceKpi> =
        performanceRepository.findKpiById(projectId, kpiId)

    override suspend fun getKpiByCode(projectId: String, code: String): DomainResult<VendorPerformanceKpi> =
        performanceRepository.findKpiByCode(projectId, code)

    override suspend fun listKpis(projectId: String, status: KpiStatus?, kpiType: KpiType?): DomainResult<List<VendorPerformanceKpi>> =
        performanceRepository.listKpis(projectId, status, kpiType)

    // --- Measurements ---
    override suspend fun recordMeasurement(measurement: VendorPerformanceMeasurement): DomainResult<VendorPerformanceMeasurement> {
        val valResult = VendorPerformanceValidator.validateMeasurement(measurement)
        if (valResult is DomainResult.Error) return valResult

        val recorded = performanceRepository.createMeasurement(measurement)
        if (recorded is DomainResult.Success) {
            performanceRepository.appendAuditEvent(
                VendorPerformanceAuditEvent(
                    auditId = UUID.randomUUID().toString(),
                    projectId = measurement.projectId,
                    tenantId = measurement.tenantId,
                    entityType = "MEASUREMENT",
                    entityId = measurement.measurementId,
                    eventType = VendorPerformanceAuditEventType.MEASUREMENT_GENERATED,
                    action = "Recorded measurement for KPI '${measurement.kpiCode}'",
                    actorId = measurement.measuredBy,
                    details = "Value: ${measurement.actualValue} ${measurement.unit}, sampleSize: ${measurement.sampleSize}"
                )
            )
        }
        return recorded
    }

    override suspend fun listMeasurements(
        projectId: String,
        vendorId: String,
        kpiId: String?,
        periodStart: Instant?,
        periodEnd: Instant?
    ): DomainResult<List<VendorPerformanceMeasurement>> =
        performanceRepository.listMeasurements(projectId, vendorId, kpiId, periodStart, periodEnd)

    // --- Scorecards ---
    override suspend fun generateScorecard(
        projectId: String,
        tenantId: String,
        vendorId: String,
        periodType: EvaluationPeriodType,
        periodStart: Instant,
        periodEnd: Instant,
        generatedBy: String,
        notes: String?
    ): DomainResult<VendorPerformanceScorecard> {
        val vendorRes = vendorRepository.findById(projectId, vendorId)
        if (vendorRes !is DomainResult.Success) {
            return DomainResult.Error(NoSuchElementException("Vendor '$vendorId' not found"))
        }

        val kpisRes = performanceRepository.listKpis(projectId, KpiStatus.ACTIVE)
        val activeKpis = if (kpisRes is DomainResult.Success) kpisRes.data else emptyList()

        val existingMeasurementsRes = performanceRepository.listMeasurements(projectId, vendorId, null, periodStart, periodEnd)
        val existingMeasurements = if (existingMeasurementsRes is DomainResult.Success) existingMeasurementsRes.data else emptyList()

        val scorecardId = UUID.randomUUID().toString()
        val scorecardItems = mutableListOf<VendorPerformanceScorecardItem>()
        var measuredCount = 0
        var totalSample = 0

        for (kpi in activeKpis) {
            val matching = existingMeasurements.filter { it.kpiId == kpi.kpiId || it.kpiCode.equals(kpi.code, ignoreCase = true) }
            val actualValue: Double
            val numerator: Double
            val denominator: Double
            val sampleSize: Int
            val confidence: MeasurementConfidenceState

            if (matching.isNotEmpty()) {
                val latest = matching.maxByOrNull { it.measuredAt }!!
                actualValue = latest.actualValue
                numerator = latest.numerator
                denominator = latest.denominator
                sampleSize = latest.sampleSize
                confidence = latest.confidenceState
                measuredCount++
                totalSample += sampleSize
            } else {
                // Default calculation fallback
                actualValue = kpi.targetValue
                numerator = kpi.targetValue
                denominator = 100.0
                sampleSize = 0
                confidence = MeasurementConfidenceState.NO_DATA
            }

            val normalized = VendorPerformanceCalculator.normalizeScore(
                actual = actualValue,
                target = kpi.targetValue,
                direction = kpi.direction,
                minimumAcceptable = kpi.minimumAcceptableValue,
                maximumAcceptable = kpi.maximumAcceptableValue
            )
            val weightedScore = VendorPerformanceCalculator.round(normalized * kpi.weight)

            scorecardItems.add(
                VendorPerformanceScorecardItem(
                    itemId = UUID.randomUUID().toString(),
                    scorecardId = scorecardId,
                    kpiId = kpi.kpiId,
                    kpiCode = kpi.code,
                    kpiName = kpi.name,
                    kpiType = kpi.kpiType,
                    weight = kpi.weight,
                    direction = kpi.direction,
                    targetValue = kpi.targetValue,
                    actualValue = actualValue,
                    normalizedScore = normalized,
                    weightedScore = weightedScore,
                    numerator = numerator,
                    denominator = denominator,
                    unit = kpi.unit,
                    sampleSize = sampleSize,
                    confidenceState = confidence
                )
            )
        }

        val overallScore = VendorPerformanceCalculator.calculateOverallScore(scorecardItems)
        val rating = VendorPerformanceCalculator.mapScoreToRating(overallScore)
        val completeness = if (activeKpis.isNotEmpty()) {
            VendorPerformanceCalculator.round((measuredCount.toDouble() / activeKpis.size.toDouble()) * 100.0)
        } else 100.0

        val riskLevel = when {
            overallScore < 40.0 -> ComplianceRiskLevel.CRITICAL
            overallScore < 60.0 -> ComplianceRiskLevel.HIGH
            overallScore < 75.0 -> ComplianceRiskLevel.MEDIUM
            else -> ComplianceRiskLevel.LOW
        }

        val scorecard = VendorPerformanceScorecard(
            scorecardId = scorecardId,
            projectId = projectId,
            tenantId = tenantId,
            vendorId = vendorId,
            periodType = periodType,
            periodStart = periodStart,
            periodEnd = periodEnd,
            overallScore = overallScore,
            rating = rating,
            riskLevel = riskLevel,
            dataCompleteness = completeness,
            sampleSize = totalSample,
            calculationVersion = VendorPerformanceCalculator.ENGINE_VERSION,
            status = ScorecardStatus.GENERATED,
            items = scorecardItems,
            notes = notes,
            version = 1,
            generatedAt = Instant.now(),
            generatedBy = generatedBy
        )

        val valResult = VendorPerformanceValidator.validateScorecard(scorecard)
        if (valResult is DomainResult.Error) return valResult

        val created = performanceRepository.createScorecard(scorecard)
        if (created is DomainResult.Success) {
            performanceRepository.appendAuditEvent(
                VendorPerformanceAuditEvent(
                    auditId = UUID.randomUUID().toString(),
                    projectId = projectId,
                    tenantId = tenantId,
                    entityType = "SCORECARD",
                    entityId = scorecardId,
                    eventType = VendorPerformanceAuditEventType.SCORECARD_GENERATED,
                    action = "Generated performance scorecard for vendor '$vendorId'",
                    actorId = generatedBy,
                    details = "Overall score: $overallScore ($rating), risk: $riskLevel"
                )
            )

            // Trigger risk indicator if overall score is critical or needs improvement
            if (rating == PerformanceRating.CRITICAL || rating == PerformanceRating.NEEDS_IMPROVEMENT) {
                performanceRepository.createRiskIndicator(
                    VendorRiskIndicator(
                        riskId = UUID.randomUUID().toString(),
                        projectId = projectId,
                        tenantId = tenantId,
                        vendorId = vendorId,
                        riskType = RiskIndicatorType.LOW_PERFORMANCE_SCORE,
                        severity = if (rating == PerformanceRating.CRITICAL) RiskSeverity.CRITICAL else RiskSeverity.HIGH,
                        source = "SCORECARD",
                        sourceId = scorecardId,
                        title = "Low Performance Score Detected",
                        description = "Vendor scored $overallScore ($rating) during period ${periodStart} to ${periodEnd}",
                        evidenceReference = scorecardId,
                        detectedAt = Instant.now(),
                        status = RiskStatus.ACTIVE
                    )
                )
            }
        }
        return created
    }

    override suspend fun getScorecardById(projectId: String, scorecardId: String): DomainResult<VendorPerformanceScorecard> =
        performanceRepository.findScorecardById(projectId, scorecardId)

    override suspend fun listScorecards(projectId: String, vendorId: String, status: ScorecardStatus?): DomainResult<List<VendorPerformanceScorecard>> =
        performanceRepository.listScorecards(projectId, vendorId, status)

    override suspend fun submitScorecardForReview(projectId: String, scorecardId: String, submittedBy: String): DomainResult<VendorPerformanceScorecard> {
        val existing = performanceRepository.findScorecardById(projectId, scorecardId)
        if (existing !is DomainResult.Success) return existing
        val current = existing.data

        if (!current.status.canTransitionTo(ScorecardStatus.UNDER_REVIEW)) {
            return DomainResult.Error(IllegalStateException("Cannot submit scorecard in status ${current.status} for review"))
        }

        val updated = current.copy(
            status = ScorecardStatus.UNDER_REVIEW,
            version = current.version + 1
        )
        val res = performanceRepository.updateScorecard(updated)
        if (res is DomainResult.Success) {
            performanceRepository.appendAuditEvent(
                VendorPerformanceAuditEvent(
                    auditId = UUID.randomUUID().toString(),
                    projectId = projectId,
                    tenantId = current.tenantId,
                    entityType = "SCORECARD",
                    entityId = scorecardId,
                    eventType = VendorPerformanceAuditEventType.SCORECARD_SUBMITTED,
                    action = "Submitted scorecard '$scorecardId' for review",
                    actorId = submittedBy
                )
            )
        }
        return res
    }

    override suspend fun approveScorecard(projectId: String, scorecardId: String, approvedBy: String): DomainResult<VendorPerformanceScorecard> {
        val existing = performanceRepository.findScorecardById(projectId, scorecardId)
        if (existing !is DomainResult.Success) return existing
        val current = existing.data

        if (!current.status.canTransitionTo(ScorecardStatus.APPROVED)) {
            return DomainResult.Error(IllegalStateException("Cannot approve scorecard in status ${current.status}"))
        }

        val updated = current.copy(
            status = ScorecardStatus.APPROVED,
            approvedAt = Instant.now(),
            approvedBy = approvedBy,
            version = current.version + 1
        )
        val res = performanceRepository.updateScorecard(updated)
        if (res is DomainResult.Success) {
            performanceRepository.appendAuditEvent(
                VendorPerformanceAuditEvent(
                    auditId = UUID.randomUUID().toString(),
                    projectId = projectId,
                    tenantId = current.tenantId,
                    entityType = "SCORECARD",
                    entityId = scorecardId,
                    eventType = VendorPerformanceAuditEventType.SCORECARD_APPROVED,
                    action = "Approved scorecard '$scorecardId'",
                    actorId = approvedBy
                )
            )
        }
        return res
    }

    override suspend fun rejectScorecard(projectId: String, scorecardId: String, rejectedBy: String, reason: String): DomainResult<VendorPerformanceScorecard> {
        val existing = performanceRepository.findScorecardById(projectId, scorecardId)
        if (existing !is DomainResult.Success) return existing
        val current = existing.data

        if (!current.status.canTransitionTo(ScorecardStatus.REJECTED)) {
            return DomainResult.Error(IllegalStateException("Cannot reject scorecard in status ${current.status}"))
        }

        val updated = current.copy(
            status = ScorecardStatus.REJECTED,
            notes = "${current.notes ?: ""}\nRejection reason: $reason".trim(),
            version = current.version + 1
        )
        val res = performanceRepository.updateScorecard(updated)
        if (res is DomainResult.Success) {
            performanceRepository.appendAuditEvent(
                VendorPerformanceAuditEvent(
                    auditId = UUID.randomUUID().toString(),
                    projectId = projectId,
                    tenantId = current.tenantId,
                    entityType = "SCORECARD",
                    entityId = scorecardId,
                    eventType = VendorPerformanceAuditEventType.SCORECARD_REJECTED,
                    action = "Rejected scorecard '$scorecardId': $reason",
                    actorId = rejectedBy
                )
            )
        }
        return res
    }

    override suspend fun finalizeScorecard(projectId: String, scorecardId: String, finalizedBy: String): DomainResult<VendorPerformanceScorecard> {
        val existing = performanceRepository.findScorecardById(projectId, scorecardId)
        if (existing !is DomainResult.Success) return existing
        val current = existing.data

        if (!current.status.canTransitionTo(ScorecardStatus.FINALIZED)) {
            return DomainResult.Error(IllegalStateException("Cannot finalize scorecard in status ${current.status}"))
        }

        val updated = current.copy(
            status = ScorecardStatus.FINALIZED,
            version = current.version + 1
        )
        val res = performanceRepository.updateScorecard(updated)
        if (res is DomainResult.Success) {
            performanceRepository.appendAuditEvent(
                VendorPerformanceAuditEvent(
                    auditId = UUID.randomUUID().toString(),
                    projectId = projectId,
                    tenantId = current.tenantId,
                    entityType = "SCORECARD",
                    entityId = scorecardId,
                    eventType = VendorPerformanceAuditEventType.SCORECARD_FINALIZED,
                    action = "Finalized scorecard '$scorecardId' (Snapshot immutable)",
                    actorId = finalizedBy
                )
            )
        }
        return res
    }

    // --- Evaluations ---
    override suspend fun createEvaluation(evaluation: VendorEvaluation): DomainResult<VendorEvaluation> {
        val valResult = VendorPerformanceValidator.validateEvaluation(evaluation)
        if (valResult is DomainResult.Error) return valResult

        val created = performanceRepository.createEvaluation(evaluation)
        if (created is DomainResult.Success) {
            performanceRepository.appendAuditEvent(
                VendorPerformanceAuditEvent(
                    auditId = UUID.randomUUID().toString(),
                    projectId = evaluation.projectId,
                    tenantId = evaluation.tenantId,
                    entityType = "EVALUATION",
                    entityId = evaluation.evaluationId,
                    eventType = VendorPerformanceAuditEventType.EVALUATION_CREATED,
                    action = "Created vendor evaluation for vendor '${evaluation.vendorId}'",
                    actorId = evaluation.createdBy
                )
            )
        }
        return created
    }

    override suspend fun getEvaluationById(projectId: String, evaluationId: String): DomainResult<VendorEvaluation> =
        performanceRepository.findEvaluationById(projectId, evaluationId)

    override suspend fun listEvaluations(projectId: String, vendorId: String?, status: EvaluationStatus?): DomainResult<List<VendorEvaluation>> =
        performanceRepository.listEvaluations(projectId, vendorId, status)

    override suspend fun submitEvaluation(projectId: String, evaluationId: String, submittedBy: String, comments: String?): DomainResult<VendorEvaluation> {
        val existing = performanceRepository.findEvaluationById(projectId, evaluationId)
        if (existing !is DomainResult.Success) return existing
        val current = existing.data

        if (!current.status.canTransitionTo(EvaluationStatus.SUBMITTED)) {
            return DomainResult.Error(IllegalStateException("Cannot submit evaluation in status ${current.status}"))
        }

        val updated = current.copy(
            status = EvaluationStatus.SUBMITTED,
            submittedAt = Instant.now(),
            submittedBy = submittedBy,
            evaluatorComments = comments ?: current.evaluatorComments,
            version = current.version + 1,
            updatedAt = Instant.now(),
            updatedBy = submittedBy
        )
        val res = performanceRepository.updateEvaluation(updated)
        if (res is DomainResult.Success) {
            performanceRepository.appendAuditEvent(
                VendorPerformanceAuditEvent(
                    auditId = UUID.randomUUID().toString(),
                    projectId = projectId,
                    tenantId = current.tenantId,
                    entityType = "EVALUATION",
                    entityId = evaluationId,
                    eventType = VendorPerformanceAuditEventType.EVALUATION_SUBMITTED,
                    action = "Submitted evaluation '$evaluationId'",
                    actorId = submittedBy
                )
            )
        }
        return res
    }

    override suspend fun reviewEvaluation(projectId: String, evaluationId: String, reviewedBy: String, reviewComments: String): DomainResult<VendorEvaluation> {
        val existing = performanceRepository.findEvaluationById(projectId, evaluationId)
        if (existing !is DomainResult.Success) return existing
        val current = existing.data

        if (!current.status.canTransitionTo(EvaluationStatus.UNDER_REVIEW)) {
            return DomainResult.Error(IllegalStateException("Cannot review evaluation in status ${current.status}"))
        }

        val updated = current.copy(
            status = EvaluationStatus.UNDER_REVIEW,
            reviewedAt = Instant.now(),
            reviewedBy = reviewedBy,
            reviewComments = reviewComments,
            version = current.version + 1,
            updatedAt = Instant.now(),
            updatedBy = reviewedBy
        )
        return performanceRepository.updateEvaluation(updated)
    }

    override suspend fun approveEvaluation(
        projectId: String,
        evaluationId: String,
        approverId: String,
        decision: EvaluationDecision,
        comments: String?
    ): DomainResult<VendorEvaluation> {
        val existing = performanceRepository.findEvaluationById(projectId, evaluationId)
        if (existing !is DomainResult.Success) return existing
        val current = existing.data

        // Enforce Separation of Duties
        val sodCheck = VendorPerformanceValidator.validateEvaluationSeparationOfDuties(current, approverId)
        if (sodCheck is DomainResult.Error) return sodCheck

        if (!current.status.canTransitionTo(EvaluationStatus.APPROVED)) {
            return DomainResult.Error(IllegalStateException("Cannot approve evaluation in status ${current.status}"))
        }

        val updated = current.copy(
            status = EvaluationStatus.APPROVED,
            decision = decision,
            approvedAt = Instant.now(),
            approvedBy = approverId,
            reviewComments = if (comments != null) "${current.reviewComments ?: ""}\nApproval Note: $comments".trim() else current.reviewComments,
            version = current.version + 1,
            updatedAt = Instant.now(),
            updatedBy = approverId
        )
        val res = performanceRepository.updateEvaluation(updated)
        if (res is DomainResult.Success) {
            performanceRepository.appendAuditEvent(
                VendorPerformanceAuditEvent(
                    auditId = UUID.randomUUID().toString(),
                    projectId = projectId,
                    tenantId = current.tenantId,
                    entityType = "EVALUATION",
                    entityId = evaluationId,
                    eventType = VendorPerformanceAuditEventType.EVALUATION_APPROVED,
                    action = "Approved evaluation '$evaluationId' with decision '$decision'",
                    actorId = approverId
                )
            )
        }
        return res
    }

    override suspend fun rejectEvaluation(projectId: String, evaluationId: String, rejectedBy: String, reason: String): DomainResult<VendorEvaluation> {
        val existing = performanceRepository.findEvaluationById(projectId, evaluationId)
        if (existing !is DomainResult.Success) return existing
        val current = existing.data

        if (!current.status.canTransitionTo(EvaluationStatus.REJECTED)) {
            return DomainResult.Error(IllegalStateException("Cannot reject evaluation in status ${current.status}"))
        }

        val updated = current.copy(
            status = EvaluationStatus.REJECTED,
            decision = EvaluationDecision.REJECTED,
            rejectionReason = reason,
            version = current.version + 1,
            updatedAt = Instant.now(),
            updatedBy = rejectedBy
        )
        val res = performanceRepository.updateEvaluation(updated)
        if (res is DomainResult.Success) {
            performanceRepository.appendAuditEvent(
                VendorPerformanceAuditEvent(
                    auditId = UUID.randomUUID().toString(),
                    projectId = projectId,
                    tenantId = current.tenantId,
                    entityType = "EVALUATION",
                    entityId = evaluationId,
                    eventType = VendorPerformanceAuditEventType.EVALUATION_REJECTED,
                    action = "Rejected evaluation '$evaluationId': $reason",
                    actorId = rejectedBy
                )
            )
        }
        return res
    }

    override suspend fun finalizeEvaluation(projectId: String, evaluationId: String, finalizedBy: String): DomainResult<VendorEvaluation> {
        val existing = performanceRepository.findEvaluationById(projectId, evaluationId)
        if (existing !is DomainResult.Success) return existing
        val current = existing.data

        if (!current.status.canTransitionTo(EvaluationStatus.FINALIZED)) {
            return DomainResult.Error(IllegalStateException("Cannot finalize evaluation in status ${current.status}"))
        }

        val updated = current.copy(
            status = EvaluationStatus.FINALIZED,
            finalizedAt = Instant.now(),
            finalizedBy = finalizedBy,
            version = current.version + 1,
            updatedAt = Instant.now(),
            updatedBy = finalizedBy
        )
        val res = performanceRepository.updateEvaluation(updated)
        if (res is DomainResult.Success) {
            performanceRepository.appendAuditEvent(
                VendorPerformanceAuditEvent(
                    auditId = UUID.randomUUID().toString(),
                    projectId = projectId,
                    tenantId = current.tenantId,
                    entityType = "EVALUATION",
                    entityId = evaluationId,
                    eventType = VendorPerformanceAuditEventType.EVALUATION_FINALIZED,
                    action = "Finalized evaluation '$evaluationId' (Snapshot immutable)",
                    actorId = finalizedBy
                )
            )
        }
        return res
    }

    // --- Compliance Requirements & Records ---
    override suspend fun createComplianceRequirement(requirement: VendorComplianceRequirement): DomainResult<VendorComplianceRequirement> {
        val valResult = VendorPerformanceValidator.validateComplianceRequirement(requirement)
        if (valResult is DomainResult.Error) return valResult

        val created = performanceRepository.createComplianceRequirement(requirement)
        if (created is DomainResult.Success) {
            performanceRepository.appendAuditEvent(
                VendorPerformanceAuditEvent(
                    auditId = UUID.randomUUID().toString(),
                    projectId = requirement.projectId,
                    tenantId = requirement.tenantId,
                    entityType = "COMPLIANCE_REQUIREMENT",
                    entityId = requirement.requirementId,
                    eventType = VendorPerformanceAuditEventType.COMPLIANCE_REQUIREMENT_CREATED,
                    action = "Created compliance requirement '${requirement.code}'",
                    actorId = requirement.createdBy
                )
            )
        }
        return created
    }

    override suspend fun updateComplianceRequirement(requirement: VendorComplianceRequirement): DomainResult<VendorComplianceRequirement> {
        val valResult = VendorPerformanceValidator.validateComplianceRequirement(requirement)
        if (valResult is DomainResult.Error) return valResult
        return performanceRepository.updateComplianceRequirement(requirement)
    }

    override suspend fun getComplianceRequirementById(projectId: String, requirementId: String): DomainResult<VendorComplianceRequirement> =
        performanceRepository.findComplianceRequirementById(projectId, requirementId)

    override suspend fun listComplianceRequirements(projectId: String, status: ComplianceStatus?): DomainResult<List<VendorComplianceRequirement>> =
        performanceRepository.listComplianceRequirements(projectId, status)

    override suspend fun submitComplianceRecord(record: VendorComplianceRecord): DomainResult<VendorComplianceRecord> {
        val (computedStatus, computedRisk) = VendorPerformanceCalculator.determineComplianceStatusAndRisk(record.expiryDate)
        val finalRecord = record.copy(
            status = computedStatus,
            riskLevel = computedRisk,
            verificationStatus = ComplianceVerificationStatus.PENDING
        )

        val valResult = VendorPerformanceValidator.validateComplianceRecord(finalRecord)
        if (valResult is DomainResult.Error) return valResult

        val created = performanceRepository.createComplianceRecord(finalRecord)
        if (created is DomainResult.Success) {
            performanceRepository.appendAuditEvent(
                VendorPerformanceAuditEvent(
                    auditId = UUID.randomUUID().toString(),
                    projectId = record.projectId,
                    tenantId = record.tenantId,
                    entityType = "COMPLIANCE_RECORD",
                    entityId = record.recordId,
                    eventType = VendorPerformanceAuditEventType.COMPLIANCE_RECORD_SUBMITTED,
                    action = "Submitted compliance record for '${record.requirementCode}'",
                    actorId = record.createdBy,
                    details = "Status: $computedStatus, Risk: $computedRisk, Expiry: ${record.expiryDate}"
                )
            )

            // Trigger risk if expired or expiring soon
            if (computedStatus == ComplianceStatus.EXPIRED || computedStatus == ComplianceStatus.EXPIRING_SOON) {
                performanceRepository.createRiskIndicator(
                    VendorRiskIndicator(
                        riskId = UUID.randomUUID().toString(),
                        projectId = record.projectId,
                        tenantId = record.tenantId,
                        vendorId = record.vendorId,
                        riskType = if (computedStatus == ComplianceStatus.EXPIRED) RiskIndicatorType.EXPIRED_COMPLIANCE else RiskIndicatorType.COMPLIANCE_EXPIRING_SOON,
                        severity = if (computedStatus == ComplianceStatus.EXPIRED) RiskSeverity.CRITICAL else RiskSeverity.HIGH,
                        source = "COMPLIANCE",
                        sourceId = record.recordId,
                        title = "Compliance ${record.requirementName} $computedStatus",
                        description = "Compliance record '${record.requirementCode}' is $computedStatus (Expires: ${record.expiryDate})",
                        evidenceReference = record.recordId,
                        detectedAt = Instant.now(),
                        status = RiskStatus.ACTIVE
                    )
                )
            }
        }
        return created
    }

    override suspend fun verifyComplianceRecord(
        projectId: String,
        recordId: String,
        verifiedBy: String,
        verified: Boolean,
        rejectionReason: String?,
        notes: String?
    ): DomainResult<VendorComplianceRecord> {
        val existing = performanceRepository.findComplianceRecordById(projectId, recordId)
        if (existing !is DomainResult.Success) return existing
        val current = existing.data

        val newVerification = if (verified) ComplianceVerificationStatus.VERIFIED else ComplianceVerificationStatus.REJECTED
        val (computedStatus, computedRisk) = if (verified) {
            VendorPerformanceCalculator.determineComplianceStatusAndRisk(current.expiryDate)
        } else {
            Pair(ComplianceStatus.REJECTED, ComplianceRiskLevel.HIGH)
        }

        val updated = current.copy(
            verificationStatus = newVerification,
            status = computedStatus,
            riskLevel = computedRisk,
            verifiedBy = verifiedBy,
            verifiedAt = Instant.now(),
            rejectionReason = rejectionReason,
            notes = notes ?: current.notes,
            version = current.version + 1,
            updatedAt = Instant.now(),
            updatedBy = verifiedBy
        )
        val res = performanceRepository.updateComplianceRecord(updated)
        if (res is DomainResult.Success) {
            performanceRepository.appendAuditEvent(
                VendorPerformanceAuditEvent(
                    auditId = UUID.randomUUID().toString(),
                    projectId = projectId,
                    tenantId = current.tenantId,
                    entityType = "COMPLIANCE_RECORD",
                    entityId = recordId,
                    eventType = if (verified) VendorPerformanceAuditEventType.COMPLIANCE_RECORD_VERIFIED else VendorPerformanceAuditEventType.COMPLIANCE_RECORD_REJECTED,
                    action = if (verified) "Verified compliance record '$recordId'" else "Rejected compliance record '$recordId'",
                    actorId = verifiedBy,
                    details = rejectionReason
                )
            )
        }
        return res
    }

    override suspend fun evaluateComplianceExpiries(projectId: String, vendorId: String?): DomainResult<List<VendorComplianceRecord>> {
        val recordsRes = performanceRepository.listComplianceRecords(projectId, vendorId, null)
        if (recordsRes !is DomainResult.Success) return recordsRes
        val records = recordsRes.data
        val updatedList = mutableListOf<VendorComplianceRecord>()

        for (rec in records) {
            val (status, risk) = VendorPerformanceCalculator.determineComplianceStatusAndRisk(rec.expiryDate)
            if (status != rec.status || risk != rec.riskLevel) {
                val updated = rec.copy(
                    status = status,
                    riskLevel = risk,
                    version = rec.version + 1,
                    updatedAt = Instant.now(),
                    updatedBy = "system-expiry-engine"
                )
                performanceRepository.updateComplianceRecord(updated)
                updatedList.add(updated)

                if (status == ComplianceStatus.EXPIRED || status == ComplianceStatus.EXPIRING_SOON) {
                    performanceRepository.appendAuditEvent(
                        VendorPerformanceAuditEvent(
                            auditId = UUID.randomUUID().toString(),
                            projectId = projectId,
                            tenantId = rec.tenantId,
                            entityType = "COMPLIANCE_RECORD",
                            entityId = rec.recordId,
                            eventType = VendorPerformanceAuditEventType.COMPLIANCE_RECORD_EXPIRED,
                            action = "Compliance record '${rec.requirementCode}' transitioned to $status",
                            actorId = "system-expiry-engine",
                            details = "Expiry date: ${rec.expiryDate}"
                        )
                    )
                }
            }
        }
        return DomainResult.Success(updatedList)
    }

    override suspend fun getComplianceRecordById(projectId: String, recordId: String): DomainResult<VendorComplianceRecord> =
        performanceRepository.findComplianceRecordById(projectId, recordId)

    override suspend fun listComplianceRecords(projectId: String, vendorId: String?, status: ComplianceStatus?): DomainResult<List<VendorComplianceRecord>> =
        performanceRepository.listComplianceRecords(projectId, vendorId, status)

    // --- Corrective Actions ---
    override suspend fun createCorrectiveAction(action: VendorCorrectiveAction): DomainResult<VendorCorrectiveAction> {
        val valResult = VendorPerformanceValidator.validateCorrectiveAction(action)
        if (valResult is DomainResult.Error) return valResult

        val created = performanceRepository.createCorrectiveAction(action)
        if (created is DomainResult.Success) {
            performanceRepository.appendAuditEvent(
                VendorPerformanceAuditEvent(
                    auditId = UUID.randomUUID().toString(),
                    projectId = action.projectId,
                    tenantId = action.tenantId,
                    entityType = "CORRECTIVE_ACTION",
                    entityId = action.actionId,
                    eventType = VendorPerformanceAuditEventType.CORRECTIVE_ACTION_CREATED,
                    action = "Created corrective action for vendor '${action.vendorId}'",
                    actorId = action.createdBy,
                    details = "Priority: ${action.priority}, Due: ${action.dueDate}"
                )
            )

            // Trigger risk indicator if HIGH or CRITICAL
            if (action.priority == CorrectiveActionPriority.CRITICAL || action.priority == CorrectiveActionPriority.HIGH) {
                performanceRepository.createRiskIndicator(
                    VendorRiskIndicator(
                        riskId = UUID.randomUUID().toString(),
                        projectId = action.projectId,
                        tenantId = action.tenantId,
                        vendorId = action.vendorId,
                        riskType = RiskIndicatorType.UNRESOLVED_CORRECTIVE_ACTIONS,
                        severity = if (action.priority == CorrectiveActionPriority.CRITICAL) RiskSeverity.CRITICAL else RiskSeverity.HIGH,
                        source = "CORRECTIVE_ACTION",
                        sourceId = action.actionId,
                        title = "Open ${action.priority} Corrective Action",
                        description = action.issueDescription,
                        evidenceReference = action.actionId,
                        detectedAt = Instant.now(),
                        status = RiskStatus.ACTIVE
                    )
                )
            }
        }
        return created
    }

    override suspend fun startCorrectiveAction(projectId: String, actionId: String, updatedBy: String, notes: String?): DomainResult<VendorCorrectiveAction> {
        val existing = performanceRepository.findCorrectiveActionById(projectId, actionId)
        if (existing !is DomainResult.Success) return existing
        val current = existing.data

        if (!current.status.canTransitionTo(CorrectiveActionStatus.IN_PROGRESS)) {
            return DomainResult.Error(IllegalStateException("Cannot start corrective action in status ${current.status}"))
        }

        val updated = current.copy(
            status = CorrectiveActionStatus.IN_PROGRESS,
            startedAt = Instant.now(),
            version = current.version + 1,
            updatedAt = Instant.now(),
            updatedBy = updatedBy
        )
        val res = performanceRepository.updateCorrectiveAction(updated)
        if (res is DomainResult.Success) {
            performanceRepository.appendAuditEvent(
                VendorPerformanceAuditEvent(
                    auditId = UUID.randomUUID().toString(),
                    projectId = projectId,
                    tenantId = current.tenantId,
                    entityType = "CORRECTIVE_ACTION",
                    entityId = actionId,
                    eventType = VendorPerformanceAuditEventType.CORRECTIVE_ACTION_STARTED,
                    action = "Started corrective action '$actionId'",
                    actorId = updatedBy
                )
            )
        }
        return res
    }

    override suspend fun submitCorrectiveActionForVerification(projectId: String, actionId: String, updatedBy: String, verificationNotes: String): DomainResult<VendorCorrectiveAction> {
        val existing = performanceRepository.findCorrectiveActionById(projectId, actionId)
        if (existing !is DomainResult.Success) return existing
        val current = existing.data

        if (!current.status.canTransitionTo(CorrectiveActionStatus.PENDING_VERIFICATION)) {
            return DomainResult.Error(IllegalStateException("Cannot submit corrective action in status ${current.status} for verification"))
        }

        val updated = current.copy(
            status = CorrectiveActionStatus.PENDING_VERIFICATION,
            completedAt = Instant.now(),
            verificationNotes = verificationNotes,
            version = current.version + 1,
            updatedAt = Instant.now(),
            updatedBy = updatedBy
        )
        return performanceRepository.updateCorrectiveAction(updated)
    }

    override suspend fun verifyCorrectiveAction(projectId: String, actionId: String, verifiedBy: String, verificationNotes: String): DomainResult<VendorCorrectiveAction> {
        val existing = performanceRepository.findCorrectiveActionById(projectId, actionId)
        if (existing !is DomainResult.Success) return existing
        val current = existing.data

        if (!current.status.canTransitionTo(CorrectiveActionStatus.VERIFIED)) {
            return DomainResult.Error(IllegalStateException("Cannot verify corrective action in status ${current.status}"))
        }

        val updated = current.copy(
            status = CorrectiveActionStatus.VERIFIED,
            verifiedBy = verifiedBy,
            verifiedAt = Instant.now(),
            verificationNotes = "${current.verificationNotes ?: ""}\nVerification: $verificationNotes".trim(),
            version = current.version + 1,
            updatedAt = Instant.now(),
            updatedBy = verifiedBy
        )
        val res = performanceRepository.updateCorrectiveAction(updated)
        if (res is DomainResult.Success) {
            performanceRepository.appendAuditEvent(
                VendorPerformanceAuditEvent(
                    auditId = UUID.randomUUID().toString(),
                    projectId = projectId,
                    tenantId = current.tenantId,
                    entityType = "CORRECTIVE_ACTION",
                    entityId = actionId,
                    eventType = VendorPerformanceAuditEventType.CORRECTIVE_ACTION_VERIFIED,
                    action = "Verified corrective action '$actionId'",
                    actorId = verifiedBy
                )
            )
        }
        return res
    }

    override suspend fun closeCorrectiveAction(projectId: String, actionId: String, closedBy: String, notes: String?): DomainResult<VendorCorrectiveAction> {
        val existing = performanceRepository.findCorrectiveActionById(projectId, actionId)
        if (existing !is DomainResult.Success) return existing
        val current = existing.data

        if (!current.status.canTransitionTo(CorrectiveActionStatus.CLOSED)) {
            return DomainResult.Error(IllegalStateException("Cannot close corrective action in status ${current.status} (Must be VERIFIED first)"))
        }

        val updated = current.copy(
            status = CorrectiveActionStatus.CLOSED,
            closedAt = Instant.now(),
            version = current.version + 1,
            updatedAt = Instant.now(),
            updatedBy = closedBy
        )
        val res = performanceRepository.updateCorrectiveAction(updated)
        if (res is DomainResult.Success) {
            performanceRepository.appendAuditEvent(
                VendorPerformanceAuditEvent(
                    auditId = UUID.randomUUID().toString(),
                    projectId = projectId,
                    tenantId = current.tenantId,
                    entityType = "CORRECTIVE_ACTION",
                    entityId = actionId,
                    eventType = VendorPerformanceAuditEventType.CORRECTIVE_ACTION_CLOSED,
                    action = "Closed corrective action '$actionId'",
                    actorId = closedBy
                )
            )
        }
        return res
    }

    override suspend fun getCorrectiveActionById(projectId: String, actionId: String): DomainResult<VendorCorrectiveAction> =
        performanceRepository.findCorrectiveActionById(projectId, actionId)

    override suspend fun listCorrectiveActions(projectId: String, vendorId: String?, status: CorrectiveActionStatus?): DomainResult<List<VendorCorrectiveAction>> =
        performanceRepository.listCorrectiveActions(projectId, vendorId, status)

    // --- Trends, Risk & Audits ---
    override suspend fun getVendorPerformanceTrends(projectId: String, vendorId: String): DomainResult<List<VendorPerformanceTrendPoint>> {
        val scorecardsRes = performanceRepository.listScorecards(projectId, vendorId, null)
        if (scorecardsRes !is DomainResult.Success) return DomainResult.Error(Exception("Failed to fetch scorecards"))

        val trendPoints = scorecardsRes.data.sortedBy { it.periodStart }.map { sc ->
            var qualitySum = 0.0
            var qualityWeight = 0.0
            var deliverySum = 0.0
            var deliveryWeight = 0.0
            var costSum = 0.0
            var costWeight = 0.0
            var compSum = 0.0
            var compWeight = 0.0

            for (item in sc.items) {
                when (item.kpiType) {
                    KpiType.QUALITY -> { qualitySum += item.normalizedScore * item.weight; qualityWeight += item.weight }
                    KpiType.DELIVERY -> { deliverySum += item.normalizedScore * item.weight; deliveryWeight += item.weight }
                    KpiType.COST -> { costSum += item.normalizedScore * item.weight; costWeight += item.weight }
                    KpiType.COMPLIANCE -> { compSum += item.normalizedScore * item.weight; compWeight += item.weight }
                    else -> {}
                }
            }

            VendorPerformanceTrendPoint(
                periodStart = sc.periodStart,
                periodEnd = sc.periodEnd,
                overallScore = sc.overallScore,
                qualityScore = if (qualityWeight > 0) VendorPerformanceCalculator.round(qualitySum / qualityWeight) else sc.overallScore,
                deliveryScore = if (deliveryWeight > 0) VendorPerformanceCalculator.round(deliverySum / deliveryWeight) else sc.overallScore,
                costScore = if (costWeight > 0) VendorPerformanceCalculator.round(costSum / costWeight) else sc.overallScore,
                complianceScore = if (compWeight > 0) VendorPerformanceCalculator.round(compSum / compWeight) else sc.overallScore,
                disputeCount = 0,
                rating = sc.rating
            )
        }
        return DomainResult.Success(trendPoints)
    }

    override suspend fun getVendorRiskIndicators(projectId: String, vendorId: String?, status: RiskStatus?): DomainResult<List<VendorRiskIndicator>> =
        performanceRepository.listRiskIndicators(projectId, vendorId, status)

    override suspend fun getAuditEvents(projectId: String, entityId: String): DomainResult<List<VendorPerformanceAuditEvent>> =
        performanceRepository.listAuditEvents(projectId, entityId)
}
