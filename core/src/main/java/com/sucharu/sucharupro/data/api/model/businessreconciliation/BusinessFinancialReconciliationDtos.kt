package com.sucharu.sucharupro.data.api.model.businessreconciliation

import com.sucharu.sucharupro.domain.model.businessreconciliation.*
import java.math.BigDecimal
import java.math.RoundingMode

// --- Request DTOs ---

data class CreateReconciliationRunRequest(
    val periodId: String,
    val runNumber: String? = null,
    val runType: String = "FULL_PERIOD",
    val notes: String? = null,
    val idempotencyKey: String? = null
)

data class ExecuteReconciliationRunRequest(
    val correlationId: String? = null
)

data class AssignDiscrepancyRequest(
    val assignedTo: String
)

data class ResolveDiscrepancyRequest(
    val resolutionNote: String,
    val correlationId: String? = null
)

data class WaiveDiscrepancyRequest(
    val waiverReason: String,
    val correlationId: String? = null
)

data class RejectDiscrepancyRequest(
    val rejectionReason: String,
    val correlationId: String? = null
)

data class ApproveReconciliationRequest(
    val notes: String? = null,
    val correlationId: String? = null
)

data class LinkCorrectionRequest(
    val correctionType: String,
    val correctionId: String,
    val note: String? = null,
    val correlationId: String? = null
)

// --- Response DTOs ---

data class BusinessFinancialReconciliationRunResponse(
    val id: String,
    val tenantId: String,
    val projectId: String,
    val periodId: String,
    val runNumber: String,
    val runType: String,
    val status: String,
    val startedAt: Long,
    val completedAt: Long?,
    val createdBy: String,
    val reviewedBy: String?,
    val approvedBy: String?,
    val totalRecordsChecked: Int,
    val matchedRecords: Int,
    val discrepancyCount: Int,
    val criticalDiscrepancyCount: Int,
    val warningCount: Int,
    val checksum: String,
    val notes: String?,
    val createdAt: Long,
    val updatedAt: Long
)

data class BusinessFinancialReconciliationDiscrepancyResponse(
    val id: String,
    val tenantId: String,
    val projectId: String,
    val reconciliationRunId: String,
    val periodId: String,
    val discrepancyType: String,
    val severity: String,
    val sourceType: String,
    val sourceId: String,
    val expectedAmount: String,
    val actualAmount: String,
    val differenceAmount: String,
    val currency: String,
    val description: String,
    val status: String,
    val detectedAt: Long,
    val assignedTo: String?,
    val resolutionNote: String?,
    val resolvedBy: String?,
    val resolvedAt: Long?,
    val approvedBy: String?,
    val approvedAt: Long?,
    val linkedCorrectionType: String?,
    val linkedCorrectionId: String?,
    val createdAt: Long,
    val updatedAt: Long
)

data class PeriodCloseReadinessResponse(
    val periodId: String,
    val isReady: Boolean,
    val blockingIssues: List<String>,
    val warnings: List<String>,
    val reconciliationRunIds: List<String>,
    val unresolvedCriticalCount: Int,
    val unresolvedWarningCount: Int,
    val allRequiredRunsApproved: Boolean,
    val calculatedAt: Long
)

data class ReconciliationDashboardSummaryResponse(
    val totalRuns: Int,
    val approvedRuns: Int,
    val openDiscrepancies: Int,
    val criticalDiscrepancies: Int,
    val resolvedDiscrepancies: Int,
    val totalRecordsChecked: Int,
    val totalMatchedRecords: Int,
    val readyToClosePeriods: Int
)

data class BusinessFinancialReconciliationAuditEventResponse(
    val id: String,
    val tenantId: String,
    val projectId: String,
    val reconciliationRunId: String?,
    val discrepancyId: String?,
    val eventType: String,
    val actorId: String,
    val actorRole: String,
    val correlationId: String?,
    val reason: String?,
    val beforeState: String?,
    val afterState: String?,
    val timestamp: Long
)

// --- Mapper Extensions ---

fun BusinessFinancialReconciliationRun.toResponse(): BusinessFinancialReconciliationRunResponse =
    BusinessFinancialReconciliationRunResponse(
        id = id,
        tenantId = tenantId,
        projectId = projectId,
        periodId = periodId,
        runNumber = runNumber,
        runType = runType.name,
        status = status.name,
        startedAt = startedAt,
        completedAt = completedAt,
        createdBy = createdBy,
        reviewedBy = reviewedBy,
        approvedBy = approvedBy,
        totalRecordsChecked = totalRecordsChecked,
        matchedRecords = matchedRecords,
        discrepancyCount = discrepancyCount,
        criticalDiscrepancyCount = criticalDiscrepancyCount,
        warningCount = warningCount,
        checksum = checksum,
        notes = notes,
        createdAt = createdAt,
        updatedAt = updatedAt
    )

fun BusinessFinancialReconciliationDiscrepancy.toResponse(): BusinessFinancialReconciliationDiscrepancyResponse =
    BusinessFinancialReconciliationDiscrepancyResponse(
        id = id,
        tenantId = tenantId,
        projectId = projectId,
        reconciliationRunId = reconciliationRunId,
        periodId = periodId,
        discrepancyType = discrepancyType.name,
        severity = severity.name,
        sourceType = sourceType,
        sourceId = sourceId,
        expectedAmount = expectedAmount.setScale(4, RoundingMode.HALF_UP).toPlainString(),
        actualAmount = actualAmount.setScale(4, RoundingMode.HALF_UP).toPlainString(),
        differenceAmount = differenceAmount.setScale(4, RoundingMode.HALF_UP).toPlainString(),
        currency = currency,
        description = description,
        status = status.name,
        detectedAt = detectedAt,
        assignedTo = assignedTo,
        resolutionNote = resolutionNote,
        resolvedBy = resolvedBy,
        resolvedAt = resolvedAt,
        approvedBy = approvedBy,
        approvedAt = approvedAt,
        linkedCorrectionType = linkedCorrectionType,
        linkedCorrectionId = linkedCorrectionId,
        createdAt = createdAt,
        updatedAt = updatedAt
    )

fun PeriodCloseReadiness.toResponse(): PeriodCloseReadinessResponse =
    PeriodCloseReadinessResponse(
        periodId = periodId,
        isReady = isReady,
        blockingIssues = blockingIssues,
        warnings = warnings,
        reconciliationRunIds = reconciliationRunIds,
        unresolvedCriticalCount = unresolvedCriticalCount,
        unresolvedWarningCount = unresolvedWarningCount,
        allRequiredRunsApproved = allRequiredRunsApproved,
        calculatedAt = calculatedAt
    )

fun ReconciliationDashboardSummary.toResponse(): ReconciliationDashboardSummaryResponse =
    ReconciliationDashboardSummaryResponse(
        totalRuns = totalRuns,
        approvedRuns = approvedRuns,
        openDiscrepancies = openDiscrepancies,
        criticalDiscrepancies = criticalDiscrepancies,
        resolvedDiscrepancies = resolvedDiscrepancies,
        totalRecordsChecked = totalRecordsChecked,
        totalMatchedRecords = totalMatchedRecords,
        readyToClosePeriods = readyToClosePeriods
    )

fun BusinessFinancialReconciliationAuditEvent.toResponse(): BusinessFinancialReconciliationAuditEventResponse =
    BusinessFinancialReconciliationAuditEventResponse(
        id = id,
        tenantId = tenantId,
        projectId = projectId,
        reconciliationRunId = reconciliationRunId,
        discrepancyId = discrepancyId,
        eventType = eventType,
        actorId = actorId,
        actorRole = actorRole,
        correlationId = correlationId,
        reason = reason,
        beforeState = beforeState,
        afterState = afterState,
        timestamp = timestamp
    )
