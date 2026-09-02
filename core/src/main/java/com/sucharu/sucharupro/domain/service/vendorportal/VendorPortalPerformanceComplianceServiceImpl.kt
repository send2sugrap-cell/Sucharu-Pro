package com.sucharu.sucharupro.domain.service.vendorportal

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.vendor.*
import com.sucharu.sucharupro.domain.model.vendorportal.*
import com.sucharu.sucharupro.domain.repository.VendorPortalPerformanceComplianceRepository
import com.sucharu.sucharupro.domain.repository.VendorRepository
import com.sucharu.sucharupro.domain.service.vendor.VendorPerformanceService
import com.sucharu.sucharupro.domain.validation.vendorportal.VendorPortalPerformanceComplianceValidator
import java.util.UUID

/**
 * Production-grade implementation of VendorPortalPerformanceComplianceService.
 * Seamlessly projects Module 12 canonical performance and compliance state into vendor-safe views.
 */
class VendorPortalPerformanceComplianceServiceImpl(
    private val portalRepository: VendorPortalPerformanceComplianceRepository,
    private val canonicalPerformanceService: VendorPerformanceService,
    private val vendorRepository: VendorRepository
) : VendorPortalPerformanceComplianceService {

    private suspend fun validateVendorAccess(projectId: String, vendorId: String): DomainResult<Unit> {
        val vendorRes = vendorRepository.findById(projectId, vendorId)
        if (vendorRes is DomainResult.Error || (vendorRes is DomainResult.Success && vendorRes.data == null)) {
            return DomainResult.Error(message = "Vendor '$vendorId' not found or access denied in project '$projectId'.")
        }
        val vendor = (vendorRes as DomainResult.Success).data!!
        if (vendor.status == VendorStatus.SUSPENDED || vendor.status == VendorStatus.INACTIVE) {
            return DomainResult.Error(message = "Vendor account is currently ${vendor.status}. Access is restricted.")
        }
        return DomainResult.Success(Unit)
    }

    override suspend fun getPerformanceOverview(
        tenantId: String,
        projectId: String,
        vendorId: String
    ): DomainResult<VendorPortalPerformanceOverview> {
        val accessCheck = validateVendorAccess(projectId, vendorId)
        if (accessCheck is DomainResult.Error) return accessCheck

        val scorecardsRes = canonicalPerformanceService.listScorecards(projectId, vendorId)
        val scorecards = if (scorecardsRes is DomainResult.Success) scorecardsRes.data else emptyList()

        val evaluationsRes = canonicalPerformanceService.listEvaluations(projectId, vendorId)
        val evaluations = if (evaluationsRes is DomainResult.Success) evaluationsRes.data else emptyList()

        val correctiveActionsRes = canonicalPerformanceService.listCorrectiveActions(projectId, vendorId)
        val correctiveActions = if (correctiveActionsRes is DomainResult.Success) correctiveActionsRes.data else emptyList()

        val latestApprovedScorecard = scorecards.filter { it.status == ScorecardStatus.APPROVED || it.status == ScorecardStatus.FINALIZED }
            .maxByOrNull { it.periodEnd } ?: scorecards.maxByOrNull { it.periodEnd }

        val overallScore = latestApprovedScorecard?.overallScore ?: 0.0
        val rating = latestApprovedScorecard?.rating ?: PerformanceRating.ACCEPTABLE
        val riskLevel = latestApprovedScorecard?.riskLevel ?: ComplianceRiskLevel.LOW

        val otdItem = latestApprovedScorecard?.items?.find { it.kpiCode.contains("OTD", ignoreCase = true) || it.kpiCode.contains("DELIVERY", ignoreCase = true) }
        val otdRate = otdItem?.actualValue ?: 0.0

        val fulfillmentItem = latestApprovedScorecard?.items?.find { it.kpiCode.contains("FULFILL", ignoreCase = true) || it.kpiCode.contains("PO", ignoreCase = true) }
        val fulfillmentRate = fulfillmentItem?.actualValue ?: 0.0

        val defectItem = latestApprovedScorecard?.items?.find { it.kpiCode.contains("DEFECT", ignoreCase = true) || it.kpiCode.contains("REJECT", ignoreCase = true) }
        val defectRate = defectItem?.actualValue ?: 0.0

        val qualityRating = when {
            defectRate <= 1.0 -> "EXCELLENT"
            defectRate <= 3.0 -> "GOOD"
            defectRate <= 6.0 -> "FAIR"
            else -> "NEEDS_IMPROVEMENT"
        }

        val strengths = mutableListOf<String>()
        val improvements = mutableListOf<String>()

        latestApprovedScorecard?.items?.forEach { item ->
            if (item.normalizedScore >= 85.0) {
                strengths.add("Strong performance in ${item.kpiName} (${item.actualValue} ${item.unit})")
            } else if (item.normalizedScore < 70.0) {
                improvements.add("Improvement target for ${item.kpiName} (${item.actualValue} ${item.unit})")
            }
        }

        val activeEvaluationsCount = evaluations.count { it.status == EvaluationStatus.SUBMITTED || it.status == EvaluationStatus.UNDER_REVIEW }
        val openActionsCount = correctiveActions.count { it.status == CorrectiveActionStatus.OPEN || it.status == CorrectiveActionStatus.IN_PROGRESS }

        val overview = VendorPortalPerformanceOverview(
            vendorId = vendorId,
            overallScore = overallScore,
            rating = rating,
            riskLevel = riskLevel,
            onTimeDeliveryRate = otdRate,
            poFulfillmentRate = fulfillmentRate,
            defectRate = defectRate,
            qualityRating = qualityRating,
            totalScorecards = scorecards.size,
            activeEvaluations = activeEvaluationsCount,
            openCorrectiveActions = openActionsCount,
            latestPeriodStart = latestApprovedScorecard?.periodStart?.toEpochMilli(),
            latestPeriodEnd = latestApprovedScorecard?.periodEnd?.toEpochMilli(),
            topStrengths = strengths.take(4),
            improvementAreas = improvements.take(4)
        )

        return DomainResult.Success(overview)
    }

    override suspend fun listPerformanceKpis(
        tenantId: String,
        projectId: String,
        vendorId: String
    ): DomainResult<List<VendorPortalPerformanceKpiSummary>> {
        val accessCheck = validateVendorAccess(projectId, vendorId)
        if (accessCheck is DomainResult.Error) return accessCheck

        val scorecardsRes = canonicalPerformanceService.listScorecards(projectId, vendorId)
        val scorecards = if (scorecardsRes is DomainResult.Success) scorecardsRes.data else emptyList()
        val latest = scorecards.maxByOrNull { it.periodEnd }

        val items = latest?.items?.map { item ->
            VendorPortalPerformanceKpiSummary(
                kpiId = item.kpiId,
                code = item.kpiCode,
                name = item.kpiName,
                description = "Measurement for ${item.kpiName}",
                kpiType = item.kpiType,
                targetValue = item.targetValue,
                actualValue = item.actualValue,
                normalizedScore = item.normalizedScore,
                weightedScore = item.weightedScore,
                weight = item.weight,
                unit = item.unit,
                direction = item.direction,
                sampleSize = item.sampleSize,
                confidenceState = item.confidenceState
            )
        } ?: emptyList()

        return DomainResult.Success(items)
    }

    override suspend fun getPerformanceTrends(
        tenantId: String,
        projectId: String,
        vendorId: String
    ): DomainResult<List<VendorPortalPerformanceTrendPoint>> {
        val accessCheck = validateVendorAccess(projectId, vendorId)
        if (accessCheck is DomainResult.Error) return accessCheck

        val trendsRes = canonicalPerformanceService.getVendorPerformanceTrends(projectId, vendorId)
        if (trendsRes is DomainResult.Error) return trendsRes

        val points = (trendsRes as DomainResult.Success).data.map { pt ->
            VendorPortalPerformanceTrendPoint(
                periodStart = pt.periodStart.toEpochMilli(),
                periodEnd = pt.periodEnd.toEpochMilli(),
                overallScore = pt.overallScore,
                qualityScore = pt.qualityScore,
                deliveryScore = pt.deliveryScore,
                costScore = pt.costScore,
                complianceScore = pt.complianceScore,
                disputeCount = pt.disputeCount,
                rating = pt.rating
            )
        }

        return DomainResult.Success(points)
    }

    override suspend fun listScorecards(
        tenantId: String,
        projectId: String,
        vendorId: String
    ): DomainResult<List<VendorPortalPerformanceScorecardSummary>> {
        val accessCheck = validateVendorAccess(projectId, vendorId)
        if (accessCheck is DomainResult.Error) return accessCheck

        val scorecardsRes = canonicalPerformanceService.listScorecards(projectId, vendorId)
        if (scorecardsRes is DomainResult.Error) return scorecardsRes

        val summaries = (scorecardsRes as DomainResult.Success).data.map { mapScorecardToSummary(it) }
        return DomainResult.Success(summaries)
    }

    override suspend fun getScorecardById(
        tenantId: String,
        projectId: String,
        vendorId: String,
        scorecardId: String
    ): DomainResult<VendorPortalPerformanceScorecardSummary> {
        val accessCheck = validateVendorAccess(projectId, vendorId)
        if (accessCheck is DomainResult.Error) return accessCheck

        val scRes = canonicalPerformanceService.getScorecardById(projectId, scorecardId)
        if (scRes is DomainResult.Error) return scRes
        val scorecard = (scRes as DomainResult.Success).data
        if (scorecard.vendorId != vendorId) {
            return DomainResult.Error(message = "Access Denied: Scorecard '$scorecardId' belongs to another vendor.")
        }

        return DomainResult.Success(mapScorecardToSummary(scorecard))
    }

    override suspend fun listEvaluations(
        tenantId: String,
        projectId: String,
        vendorId: String
    ): DomainResult<List<VendorPortalEvaluationSummary>> {
        val accessCheck = validateVendorAccess(projectId, vendorId)
        if (accessCheck is DomainResult.Error) return accessCheck

        val evalsRes = canonicalPerformanceService.listEvaluations(projectId, vendorId)
        if (evalsRes is DomainResult.Error) return evalsRes

        val summaries = (evalsRes as DomainResult.Success).data.map { mapEvaluationToSummary(it) }
        return DomainResult.Success(summaries)
    }

    override suspend fun getEvaluationById(
        tenantId: String,
        projectId: String,
        vendorId: String,
        evaluationId: String
    ): DomainResult<VendorPortalEvaluationSummary> {
        val accessCheck = validateVendorAccess(projectId, vendorId)
        if (accessCheck is DomainResult.Error) return accessCheck

        val evalRes = canonicalPerformanceService.getEvaluationById(projectId, evaluationId)
        if (evalRes is DomainResult.Error) return evalRes
        val evaluation = (evalRes as DomainResult.Success).data
        if (evaluation.vendorId != vendorId) {
            return DomainResult.Error(message = "Access Denied: Evaluation '$evaluationId' belongs to another vendor.")
        }

        return DomainResult.Success(mapEvaluationToSummary(evaluation))
    }

    override suspend fun acknowledgeEvaluation(
        tenantId: String,
        projectId: String,
        vendorId: String,
        evaluationId: String,
        actorId: String
    ): DomainResult<VendorPortalEvaluationSummary> {
        val accessCheck = validateVendorAccess(projectId, vendorId)
        if (accessCheck is DomainResult.Error) return accessCheck

        val evalRes = canonicalPerformanceService.getEvaluationById(projectId, evaluationId)
        if (evalRes is DomainResult.Error) return evalRes
        val evaluation = (evalRes as DomainResult.Success).data
        if (evaluation.vendorId != vendorId) {
            return DomainResult.Error(message = "Access Denied: Evaluation '$evaluationId' belongs to another vendor.")
        }

        val ackResponse = VendorPortalEvaluationResponse(
            responseId = "VPER-ACK-" + UUID.randomUUID().toString().take(8),
            evaluationId = evaluationId,
            tenantId = tenantId,
            projectId = projectId,
            vendorId = vendorId,
            responseType = VendorPortalEvaluationResponseType.ACKNOWLEDGEMENT,
            subject = "Evaluation Acknowledged",
            remarks = "Acknowledged by vendor portal user $actorId",
            status = VendorPortalEvaluationResponseStatus.ACKNOWLEDGED,
            submittedBy = actorId,
            submittedAt = System.currentTimeMillis()
        )
        portalRepository.saveEvaluationResponse(ackResponse)

        portalRepository.recordAudit(
            VendorPortalPerformanceComplianceActivity(
                activityId = "ACT-" + UUID.randomUUID().toString().take(8),
                tenantId = tenantId,
                projectId = projectId,
                vendorId = vendorId,
                eventType = VendorPortalPerformanceComplianceAuditEventType.EVALUATION_ACKNOWLEDGED,
                entityType = "EVALUATION",
                entityId = evaluationId,
                actorId = actorId,
                description = "Vendor acknowledged evaluation score (${evaluation.evaluationScore}) for period ${evaluation.periodType}."
            )
        )

        val summary = mapEvaluationToSummary(evaluation).copy(
            acknowledgedAt = System.currentTimeMillis(),
            acknowledgedBy = actorId
        )
        return DomainResult.Success(summary)
    }

    override suspend fun submitEvaluationResponse(
        tenantId: String,
        projectId: String,
        vendorId: String,
        evaluationId: String,
        subject: String,
        remarks: String,
        proposedRemediation: String?,
        evidenceReferences: List<String>,
        actorId: String
    ): DomainResult<VendorPortalEvaluationResponse> {
        val accessCheck = validateVendorAccess(projectId, vendorId)
        if (accessCheck is DomainResult.Error) return accessCheck

        val valRes = VendorPortalPerformanceComplianceValidator.validateEvaluationResponse(
            tenantId, projectId, vendorId, evaluationId, subject, remarks, actorId
        )
        if (valRes is DomainResult.Error) return valRes

        val evalRes = canonicalPerformanceService.getEvaluationById(projectId, evaluationId)
        if (evalRes is DomainResult.Error) return evalRes
        val evaluation = (evalRes as DomainResult.Success).data
        if (evaluation.vendorId != vendorId) {
            return DomainResult.Error(message = "Access Denied: Evaluation '$evaluationId' belongs to another vendor.")
        }

        val response = VendorPortalEvaluationResponse(
            responseId = "VPER-" + UUID.randomUUID().toString().take(8),
            evaluationId = evaluationId,
            tenantId = tenantId,
            projectId = projectId,
            vendorId = vendorId,
            responseType = VendorPortalEvaluationResponseType.FORMAL_RESPONSE,
            subject = subject,
            remarks = remarks,
            proposedRemediation = proposedRemediation,
            evidenceReferences = evidenceReferences,
            status = VendorPortalEvaluationResponseStatus.SUBMITTED,
            submittedBy = actorId,
            submittedAt = System.currentTimeMillis()
        )

        val saveRes = portalRepository.saveEvaluationResponse(response)
        if (saveRes is DomainResult.Error) return saveRes

        portalRepository.recordAudit(
            VendorPortalPerformanceComplianceActivity(
                activityId = "ACT-" + UUID.randomUUID().toString().take(8),
                tenantId = tenantId,
                projectId = projectId,
                vendorId = vendorId,
                eventType = VendorPortalPerformanceComplianceAuditEventType.EVALUATION_RESPONSE_SUBMITTED,
                entityType = "EVALUATION",
                entityId = evaluationId,
                actorId = actorId,
                description = "Vendor submitted response '$subject' on evaluation $evaluationId."
            )
        )

        return saveRes
    }

    override suspend fun listEvaluationResponses(
        tenantId: String,
        projectId: String,
        vendorId: String,
        evaluationId: String
    ): DomainResult<List<VendorPortalEvaluationResponse>> {
        val accessCheck = validateVendorAccess(projectId, vendorId)
        if (accessCheck is DomainResult.Error) return accessCheck
        return portalRepository.listEvaluationResponses(tenantId, projectId, vendorId, evaluationId)
    }

    override suspend fun getComplianceOverview(
        tenantId: String,
        projectId: String,
        vendorId: String
    ): DomainResult<VendorPortalComplianceOverview> {
        val accessCheck = validateVendorAccess(projectId, vendorId)
        if (accessCheck is DomainResult.Error) return accessCheck

        val reqsRes = canonicalPerformanceService.listComplianceRequirements(projectId)
        val requirements = if (reqsRes is DomainResult.Success) reqsRes.data else emptyList()

        val recordsRes = canonicalPerformanceService.listComplianceRecords(projectId, vendorId)
        val records = if (recordsRes is DomainResult.Success) recordsRes.data else emptyList()

        val expiriesRes = canonicalPerformanceService.evaluateComplianceExpiries(projectId, vendorId)
        val expiringRecords = if (expiriesRes is DomainResult.Success) expiriesRes.data else emptyList()

        val correctiveActionsRes = canonicalPerformanceService.listCorrectiveActions(projectId, vendorId)
        val correctiveActions = if (correctiveActionsRes is DomainResult.Success) correctiveActionsRes.data else emptyList()

        val compliantCount = records.count { it.status == ComplianceStatus.VERIFIED }
        val pendingCount = records.count { it.status == ComplianceStatus.PENDING || it.status == ComplianceStatus.UNDER_REVIEW || it.status == ComplianceStatus.SUBMITTED }
        val nonCompliantCount = records.count { it.status == ComplianceStatus.REJECTED }
        val expiredCount = expiringRecords.count { it.status == ComplianceStatus.EXPIRED || (it.expiryDate != null && it.expiryDate.isBefore(java.time.Instant.now())) }

        val upcoming30Days = expiringRecords.count {
            it.expiryDate != null && it.expiryDate.isAfter(java.time.Instant.now()) &&
                    it.expiryDate.isBefore(java.time.Instant.now().plus(java.time.Duration.ofDays(30)))
        }

        val openActionsCount = correctiveActions.count { it.status == CorrectiveActionStatus.OPEN || it.status == CorrectiveActionStatus.IN_PROGRESS }

        val complianceRate = if (requirements.isNotEmpty()) {
            VendorPortalPerformanceComplianceValidator.calculatePercentage(compliantCount, requirements.size)
        } else 100.0

        val overallRisk = when {
            nonCompliantCount > 0 || expiredCount > 0 -> ComplianceRiskLevel.HIGH
            pendingCount > 0 || upcoming30Days > 0 -> ComplianceRiskLevel.MEDIUM
            else -> ComplianceRiskLevel.LOW
        }

        val overallStatus = when {
            nonCompliantCount > 0 -> ComplianceStatus.REJECTED
            pendingCount > 0 -> ComplianceStatus.UNDER_REVIEW
            else -> ComplianceStatus.VERIFIED
        }

        val overview = VendorPortalComplianceOverview(
            vendorId = vendorId,
            overallRiskLevel = overallRisk,
            overallComplianceStatus = overallStatus,
            totalRequirements = requirements.size,
            compliantCount = compliantCount,
            pendingCount = pendingCount,
            nonCompliantCount = nonCompliantCount,
            expiredCertificationsCount = expiredCount,
            upcomingExpiringCertificationsCount = upcoming30Days,
            openCorrectiveActionsCount = openActionsCount,
            complianceRate = complianceRate
        )

        return DomainResult.Success(overview)
    }

    override suspend fun listComplianceRequirements(
        tenantId: String,
        projectId: String
    ): DomainResult<List<VendorPortalComplianceRequirementSummary>> {
        val reqsRes = canonicalPerformanceService.listComplianceRequirements(projectId)
        if (reqsRes is DomainResult.Error) return reqsRes

        val summaries = (reqsRes as DomainResult.Success).data.map {
            VendorPortalComplianceRequirementSummary(
                requirementId = it.requirementId,
                requirementType = it.requirementType,
                code = it.code,
                name = it.name,
                description = it.description,
                mandatory = it.mandatory,
                riskLevel = it.riskLevel,
                validityDays = it.validityDays
            )
        }
        return DomainResult.Success(summaries)
    }

    override suspend fun listComplianceRecords(
        tenantId: String,
        projectId: String,
        vendorId: String
    ): DomainResult<List<VendorPortalComplianceRecordSummary>> {
        val accessCheck = validateVendorAccess(projectId, vendorId)
        if (accessCheck is DomainResult.Error) return accessCheck

        val recordsRes = canonicalPerformanceService.listComplianceRecords(projectId, vendorId)
        if (recordsRes is DomainResult.Error) return recordsRes

        val evidenceListRes = portalRepository.listComplianceEvidence(tenantId, projectId, vendorId)
        val evidenceList = if (evidenceListRes is DomainResult.Success) evidenceListRes.data else emptyList()

        val summaries = (recordsRes as DomainResult.Success).data.map { record ->
            val attachedEvidence = evidenceList.filter { it.recordId == record.recordId }
            val expiryMillis = record.expiryDate?.toEpochMilli()
            val daysRemaining = VendorPortalPerformanceComplianceValidator.calculateDaysRemaining(expiryMillis)
            val alertLevel = VendorPortalPerformanceComplianceValidator.resolveExpiryAlertLevel(expiryMillis)

            VendorPortalComplianceRecordSummary(
                recordId = record.recordId,
                tenantId = record.tenantId,
                projectId = record.projectId,
                vendorId = record.vendorId,
                requirementId = record.requirementId,
                requirementCode = record.requirementCode,
                requirementName = record.requirementName,
                requirementType = record.requirementType,
                mandatory = record.mandatory,
                effectiveDate = record.effectiveDate.toEpochMilli(),
                expiryDate = expiryMillis,
                status = record.status,
                riskLevel = record.riskLevel,
                verificationStatus = record.verificationStatus,
                rejectionReason = record.rejectionReason,
                notes = record.notes,
                daysUntilExpiry = daysRemaining,
                expiryAlertLevel = alertLevel,
                evidenceCount = attachedEvidence.size,
                evidenceList = attachedEvidence
            )
        }

        return DomainResult.Success(summaries)
    }

    override suspend fun listCertificationExpiries(
        tenantId: String,
        projectId: String,
        vendorId: String
    ): DomainResult<List<VendorPortalCertificationExpiryAlert>> {
        val accessCheck = validateVendorAccess(projectId, vendorId)
        if (accessCheck is DomainResult.Error) return accessCheck

        val recordsRes = canonicalPerformanceService.evaluateComplianceExpiries(projectId, vendorId)
        if (recordsRes is DomainResult.Error) return recordsRes

        val alerts = (recordsRes as DomainResult.Success).data.mapNotNull { record ->
            val expiryMillis = record.expiryDate?.toEpochMilli() ?: return@mapNotNull null
            val daysRemaining = VendorPortalPerformanceComplianceValidator.calculateDaysRemaining(expiryMillis) ?: 0L
            val alertLevel = VendorPortalPerformanceComplianceValidator.resolveExpiryAlertLevel(expiryMillis)

            VendorPortalCertificationExpiryAlert(
                recordId = record.recordId,
                certificationName = record.requirementName,
                requirementCode = record.requirementCode,
                expiryDate = expiryMillis,
                daysRemaining = daysRemaining,
                alertLevel = alertLevel,
                mandatory = record.mandatory,
                status = record.status
            )
        }.sortedBy { it.daysRemaining }

        return DomainResult.Success(alerts)
    }

    override suspend fun uploadComplianceEvidence(
        tenantId: String,
        projectId: String,
        vendorId: String,
        recordId: String?,
        requirementId: String?,
        actionId: String?,
        evidenceType: VendorPortalComplianceEvidenceType,
        fileName: String,
        fileUrl: String,
        checksum: String?,
        fileSizeBytes: Long,
        mimeType: String?,
        description: String?,
        actorId: String
    ): DomainResult<VendorPortalComplianceEvidence> {
        val accessCheck = validateVendorAccess(projectId, vendorId)
        if (accessCheck is DomainResult.Error) return accessCheck

        val valRes = VendorPortalPerformanceComplianceValidator.validateComplianceEvidence(
            tenantId, projectId, vendorId, fileName, fileUrl, actorId
        )
        if (valRes is DomainResult.Error) return valRes

        val evidence = VendorPortalComplianceEvidence(
            evidenceId = "VPCE-" + UUID.randomUUID().toString().take(8),
            recordId = recordId,
            requirementId = requirementId,
            actionId = actionId,
            tenantId = tenantId,
            projectId = projectId,
            vendorId = vendorId,
            evidenceType = evidenceType,
            fileName = fileName,
            fileUrl = fileUrl,
            checksum = checksum,
            fileSizeBytes = fileSizeBytes,
            mimeType = mimeType,
            description = description,
            uploadedBy = actorId,
            uploadedAt = System.currentTimeMillis()
        )

        val saveRes = portalRepository.saveComplianceEvidence(evidence)
        if (saveRes is DomainResult.Error) return saveRes

        portalRepository.recordAudit(
            VendorPortalPerformanceComplianceActivity(
                activityId = "ACT-" + UUID.randomUUID().toString().take(8),
                tenantId = tenantId,
                projectId = projectId,
                vendorId = vendorId,
                eventType = VendorPortalPerformanceComplianceAuditEventType.COMPLIANCE_EVIDENCE_UPLOADED,
                entityType = "COMPLIANCE_EVIDENCE",
                entityId = evidence.evidenceId,
                actorId = actorId,
                description = "Vendor uploaded compliance evidence '$fileName' (${evidence.evidenceType})."
            )
        )

        return saveRes
    }

    override suspend fun listComplianceEvidence(
        tenantId: String,
        projectId: String,
        vendorId: String,
        recordId: String?,
        actionId: String?
    ): DomainResult<List<VendorPortalComplianceEvidence>> {
        val accessCheck = validateVendorAccess(projectId, vendorId)
        if (accessCheck is DomainResult.Error) return accessCheck
        return portalRepository.listComplianceEvidence(tenantId, projectId, vendorId, recordId, actionId)
    }

    override suspend fun listCorrectiveActions(
        tenantId: String,
        projectId: String,
        vendorId: String
    ): DomainResult<List<VendorPortalCorrectiveActionSummary>> {
        val accessCheck = validateVendorAccess(projectId, vendorId)
        if (accessCheck is DomainResult.Error) return accessCheck

        val actionsRes = canonicalPerformanceService.listCorrectiveActions(projectId, vendorId)
        if (actionsRes is DomainResult.Error) return actionsRes

        val summaries = (actionsRes as DomainResult.Success).data.map { action ->
            val responsesRes = portalRepository.listCorrectiveActionResponses(tenantId, projectId, vendorId, action.actionId)
            val responses = if (responsesRes is DomainResult.Success) responsesRes.data else emptyList()
            val latest = responses.firstOrNull()?.remediationNotes

            val isOverdue = action.status != CorrectiveActionStatus.CLOSED &&
                    action.status != CorrectiveActionStatus.VERIFIED &&
                    action.dueDate.isBefore(java.time.Instant.now())

            VendorPortalCorrectiveActionSummary(
                actionId = action.actionId,
                tenantId = action.tenantId,
                projectId = action.projectId,
                vendorId = action.vendorId,
                sourceType = action.sourceType,
                sourceId = action.sourceId,
                issueDescription = action.issueDescription,
                rootCause = action.rootCause,
                actionPlan = action.actionPlan,
                priority = action.priority,
                dueDate = action.dueDate.toEpochMilli(),
                status = action.status,
                startedAt = action.startedAt?.toEpochMilli(),
                completedAt = action.completedAt?.toEpochMilli(),
                closedAt = action.closedAt?.toEpochMilli(),
                isOverdue = isOverdue,
                latestVendorResponse = latest,
                responsesCount = responses.size
            )
        }

        return DomainResult.Success(summaries)
    }

    override suspend fun getCorrectiveActionById(
        tenantId: String,
        projectId: String,
        vendorId: String,
        actionId: String
    ): DomainResult<VendorPortalCorrectiveActionSummary> {
        val accessCheck = validateVendorAccess(projectId, vendorId)
        if (accessCheck is DomainResult.Error) return accessCheck

        val actionRes = canonicalPerformanceService.getCorrectiveActionById(projectId, actionId)
        if (actionRes is DomainResult.Error) return actionRes
        val action = (actionRes as DomainResult.Success).data
        if (action.vendorId != vendorId) {
            return DomainResult.Error(message = "Access Denied: Corrective action '$actionId' belongs to another vendor.")
        }

        val responsesRes = portalRepository.listCorrectiveActionResponses(tenantId, projectId, vendorId, action.actionId)
        val responses = if (responsesRes is DomainResult.Success) responsesRes.data else emptyList()
        val latest = responses.firstOrNull()?.remediationNotes

        val isOverdue = action.status != CorrectiveActionStatus.CLOSED &&
                action.status != CorrectiveActionStatus.VERIFIED &&
                action.dueDate.isBefore(java.time.Instant.now())

        val summary = VendorPortalCorrectiveActionSummary(
            actionId = action.actionId,
            tenantId = action.tenantId,
            projectId = action.projectId,
            vendorId = action.vendorId,
            sourceType = action.sourceType,
            sourceId = action.sourceId,
            issueDescription = action.issueDescription,
            rootCause = action.rootCause,
            actionPlan = action.actionPlan,
            priority = action.priority,
            dueDate = action.dueDate.toEpochMilli(),
            status = action.status,
            startedAt = action.startedAt?.toEpochMilli(),
            completedAt = action.completedAt?.toEpochMilli(),
            closedAt = action.closedAt?.toEpochMilli(),
            isOverdue = isOverdue,
            latestVendorResponse = latest,
            responsesCount = responses.size
        )

        return DomainResult.Success(summary)
    }

    override suspend fun submitCorrectiveActionResponse(
        tenantId: String,
        projectId: String,
        vendorId: String,
        actionId: String,
        remediationNotes: String,
        rootCauseExplanation: String?,
        progressPercentage: Double,
        evidenceReferences: List<String>,
        actorId: String
    ): DomainResult<VendorPortalCorrectiveActionResponse> {
        val accessCheck = validateVendorAccess(projectId, vendorId)
        if (accessCheck is DomainResult.Error) return accessCheck

        val valRes = VendorPortalPerformanceComplianceValidator.validateCorrectiveActionResponse(
            tenantId, projectId, vendorId, actionId, remediationNotes, progressPercentage, actorId
        )
        if (valRes is DomainResult.Error) return valRes

        val actionRes = canonicalPerformanceService.getCorrectiveActionById(projectId, actionId)
        if (actionRes is DomainResult.Error) return actionRes
        val action = (actionRes as DomainResult.Success).data
        if (action.vendorId != vendorId) {
            return DomainResult.Error(message = "Access Denied: Corrective action '$actionId' belongs to another vendor.")
        }

        val response = VendorPortalCorrectiveActionResponse(
            responseId = "VPCAR-" + UUID.randomUUID().toString().take(8),
            actionId = actionId,
            tenantId = tenantId,
            projectId = projectId,
            vendorId = vendorId,
            remediationNotes = remediationNotes,
            rootCauseExplanation = rootCauseExplanation,
            progressPercentage = progressPercentage,
            isCompletionRequest = false,
            evidenceReferences = evidenceReferences,
            status = VendorPortalRemediationStatus.IN_PROGRESS,
            submittedBy = actorId,
            submittedAt = System.currentTimeMillis()
        )

        val saveRes = portalRepository.saveCorrectiveActionResponse(response)
        if (saveRes is DomainResult.Error) return saveRes

        portalRepository.recordAudit(
            VendorPortalPerformanceComplianceActivity(
                activityId = "ACT-" + UUID.randomUUID().toString().take(8),
                tenantId = tenantId,
                projectId = projectId,
                vendorId = vendorId,
                eventType = VendorPortalPerformanceComplianceAuditEventType.CORRECTIVE_ACTION_RESPONSE_SUBMITTED,
                entityType = "CORRECTIVE_ACTION",
                entityId = actionId,
                actorId = actorId,
                description = "Vendor submitted progress update ($progressPercentage%) on corrective action $actionId."
            )
        )

        return saveRes
    }

    override suspend fun submitCorrectiveActionCompletionRequest(
        tenantId: String,
        projectId: String,
        vendorId: String,
        actionId: String,
        completionNotes: String,
        evidenceReferences: List<String>,
        actorId: String
    ): DomainResult<VendorPortalCorrectiveActionResponse> {
        val accessCheck = validateVendorAccess(projectId, vendorId)
        if (accessCheck is DomainResult.Error) return accessCheck

        val valRes = VendorPortalPerformanceComplianceValidator.validateCorrectiveActionResponse(
            tenantId, projectId, vendorId, actionId, completionNotes, 100.0, actorId
        )
        if (valRes is DomainResult.Error) return valRes

        val actionRes = canonicalPerformanceService.getCorrectiveActionById(projectId, actionId)
        if (actionRes is DomainResult.Error) return actionRes
        val action = (actionRes as DomainResult.Success).data
        if (action.vendorId != vendorId) {
            return DomainResult.Error(message = "Access Denied: Corrective action '$actionId' belongs to another vendor.")
        }

        val response = VendorPortalCorrectiveActionResponse(
            responseId = "VPCAR-COMP-" + UUID.randomUUID().toString().take(8),
            actionId = actionId,
            tenantId = tenantId,
            projectId = projectId,
            vendorId = vendorId,
            remediationNotes = completionNotes,
            progressPercentage = 100.0,
            isCompletionRequest = true,
            evidenceReferences = evidenceReferences,
            status = VendorPortalRemediationStatus.COMPLETED_PENDING_VERIFICATION,
            submittedBy = actorId,
            submittedAt = System.currentTimeMillis()
        )

        val saveRes = portalRepository.saveCorrectiveActionResponse(response)
        if (saveRes is DomainResult.Error) return saveRes

        canonicalPerformanceService.submitCorrectiveActionForVerification(projectId, actionId, actorId, completionNotes)

        portalRepository.recordAudit(
            VendorPortalPerformanceComplianceActivity(
                activityId = "ACT-" + UUID.randomUUID().toString().take(8),
                tenantId = tenantId,
                projectId = projectId,
                vendorId = vendorId,
                eventType = VendorPortalPerformanceComplianceAuditEventType.CORRECTIVE_ACTION_COMPLETION_REQUESTED,
                entityType = "CORRECTIVE_ACTION",
                entityId = actionId,
                actorId = actorId,
                description = "Vendor submitted completion request for verification on corrective action $actionId."
            )
        )

        return saveRes
    }

    override suspend fun listPerformanceComplianceActivity(
        tenantId: String,
        projectId: String,
        vendorId: String,
        entityType: String?,
        entityId: String?
    ): DomainResult<List<VendorPortalPerformanceComplianceActivity>> {
        val accessCheck = validateVendorAccess(projectId, vendorId)
        if (accessCheck is DomainResult.Error) return accessCheck
        return portalRepository.listAuditEvents(tenantId, projectId, vendorId, entityType, entityId)
    }

    override suspend fun getWorkspace(
        tenantId: String,
        projectId: String,
        vendorId: String
    ): DomainResult<VendorPortalPerformanceWorkspace> {
        val accessCheck = validateVendorAccess(projectId, vendorId)
        if (accessCheck is DomainResult.Error) return accessCheck

        val overviewRes = getPerformanceOverview(tenantId, projectId, vendorId)
        if (overviewRes is DomainResult.Error) return overviewRes

        val compOverviewRes = getComplianceOverview(tenantId, projectId, vendorId)
        if (compOverviewRes is DomainResult.Error) return compOverviewRes

        val scorecardsRes = listScorecards(tenantId, projectId, vendorId)
        val scorecards = if (scorecardsRes is DomainResult.Success) scorecardsRes.data else emptyList()

        val evalsRes = listEvaluations(tenantId, projectId, vendorId)
        val evaluations = if (evalsRes is DomainResult.Success) evalsRes.data else emptyList()

        val expiriesRes = listCertificationExpiries(tenantId, projectId, vendorId)
        val expiries = if (expiriesRes is DomainResult.Success) expiriesRes.data else emptyList()

        val actionsRes = listCorrectiveActions(tenantId, projectId, vendorId)
        val actions = if (actionsRes is DomainResult.Success) actionsRes.data else emptyList()

        val workspace = VendorPortalPerformanceWorkspace(
            overview = (overviewRes as DomainResult.Success).data,
            complianceOverview = (compOverviewRes as DomainResult.Success).data,
            recentScorecards = scorecards.take(5),
            pendingEvaluations = evaluations.filter { it.status == EvaluationStatus.SUBMITTED || it.status == EvaluationStatus.UNDER_REVIEW },
            urgentExpiries = expiries.filter { it.alertLevel != VendorPortalExpiryAlertLevel.NORMAL }.take(5),
            openCorrectiveActions = actions.filter { it.status == CorrectiveActionStatus.OPEN || it.status == CorrectiveActionStatus.IN_PROGRESS }
        )

        return DomainResult.Success(workspace)
    }

    private fun mapScorecardToSummary(sc: VendorPerformanceScorecard): VendorPortalPerformanceScorecardSummary =
        VendorPortalPerformanceScorecardSummary(
            scorecardId = sc.scorecardId,
            tenantId = sc.tenantId,
            projectId = sc.projectId,
            vendorId = sc.vendorId,
            periodType = sc.periodType,
            periodStart = sc.periodStart.toEpochMilli(),
            periodEnd = sc.periodEnd.toEpochMilli(),
            overallScore = sc.overallScore,
            rating = sc.rating,
            riskLevel = sc.riskLevel,
            dataCompleteness = sc.dataCompleteness,
            sampleSize = sc.sampleSize,
            status = sc.status,
            notes = sc.notes,
            items = sc.items.map { item ->
                VendorPortalPerformanceKpiSummary(
                    kpiId = item.kpiId,
                    code = item.kpiCode,
                    name = item.kpiName,
                    description = "Measurement for ${item.kpiName}",
                    kpiType = item.kpiType,
                    targetValue = item.targetValue,
                    actualValue = item.actualValue,
                    normalizedScore = item.normalizedScore,
                    weightedScore = item.weightedScore,
                    weight = item.weight,
                    unit = item.unit,
                    direction = item.direction,
                    sampleSize = item.sampleSize,
                    confidenceState = item.confidenceState
                )
            },
            generatedAt = sc.generatedAt.toEpochMilli(),
            approvedAt = sc.approvedAt?.toEpochMilli()
        )

    private fun mapEvaluationToSummary(ev: VendorEvaluation): VendorPortalEvaluationSummary =
        VendorPortalEvaluationSummary(
            evaluationId = ev.evaluationId,
            tenantId = ev.tenantId,
            projectId = ev.projectId,
            vendorId = ev.vendorId,
            scorecardId = ev.scorecardId,
            periodType = ev.periodType,
            periodStart = ev.periodStart.toEpochMilli(),
            periodEnd = ev.periodEnd.toEpochMilli(),
            status = ev.status,
            decision = ev.decision,
            evaluationScore = ev.evaluationScore,
            rating = ev.rating,
            evaluatorComments = ev.evaluatorComments,
            reviewComments = ev.reviewComments,
            criteria = ev.criteria.map {
                VendorPortalEvaluationCriterionSummary(
                    criterionId = it.criterionId,
                    name = it.name,
                    category = it.category,
                    weight = it.weight,
                    score = it.score,
                    comments = it.comments
                )
            },
            acknowledgedAt = null,
            acknowledgedBy = null,
            finalizedAt = ev.finalizedAt?.toEpochMilli(),
            createdAt = ev.createdAt.toEpochMilli()
        )
}
