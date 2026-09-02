package com.sucharu.sucharupro.data.api.model

import com.sucharu.sucharupro.domain.model.customerfinancialreporting.*

data class CustomerFinancialAlertDto(
    val alertId: String,
    val tenantId: String,
    val projectId: String,
    val customerId: String,
    val alertType: String,
    val severity: String,
    val status: String,
    val title: String,
    val safeMessage: String,
    val sourceType: String,
    val sourceId: String,
    val detectedAt: Long,
    val dueAt: Long?,
    val resolvedAt: Long?,
    val acknowledgedAt: Long?,
    val acknowledgedBy: String?,
    val dismissedAt: Long?,
    val dismissedBy: String?,
    val dismissalReason: String?,
    val expiresAt: Long?,
    val correlationId: String?,
    val deduplicationKey: String,
    val metadata: Map<String, String>,
    val version: Long
)

fun CustomerFinancialAlert.toDto(): CustomerFinancialAlertDto = CustomerFinancialAlertDto(
    alertId = alertId,
    tenantId = tenantId,
    projectId = projectId,
    customerId = customerId,
    alertType = alertType.name,
    severity = severity.name,
    status = status.name,
    title = title,
    safeMessage = safeMessage,
    sourceType = sourceType,
    sourceId = sourceId,
    detectedAt = detectedAt,
    dueAt = dueAt,
    resolvedAt = resolvedAt,
    acknowledgedAt = acknowledgedAt,
    acknowledgedBy = acknowledgedBy,
    dismissedAt = dismissedAt,
    dismissedBy = dismissedBy,
    dismissalReason = dismissalReason,
    expiresAt = expiresAt,
    correlationId = correlationId,
    deduplicationKey = deduplicationKey,
    metadata = metadata,
    version = version
)

data class CustomerFinancialAlertSummaryDto(
    val totalOpen: Int,
    val criticalCount: Int,
    val highCount: Int,
    val mediumCount: Int,
    val lowCount: Int,
    val infoCount: Int,
    val acknowledgedCount: Int,
    val resolvedCount: Int,
    val dismissedCount: Int
)

fun CustomerFinancialAlertSummary.toDto(): CustomerFinancialAlertSummaryDto = CustomerFinancialAlertSummaryDto(
    totalOpen = totalOpen,
    criticalCount = criticalCount,
    highCount = highCount,
    mediumCount = mediumCount,
    lowCount = lowCount,
    infoCount = infoCount,
    acknowledgedCount = acknowledgedCount,
    resolvedCount = resolvedCount,
    dismissedCount = dismissedCount
)

data class CustomerFinancialAlertAuditEventDto(
    val eventId: String,
    val alertId: String,
    val eventType: String,
    val actorId: String,
    val actorRole: String,
    val timestamp: Long,
    val detailsJson: String
)

fun CustomerFinancialAlertAuditEvent.toDto(): CustomerFinancialAlertAuditEventDto = CustomerFinancialAlertAuditEventDto(
    eventId = eventId,
    alertId = alertId,
    eventType = eventType.name,
    actorId = actorId,
    actorRole = actorRole,
    timestamp = timestamp,
    detailsJson = detailsJson
)

data class ResolveCustomerFinancialAlertRequest(
    val reason: String = "Resolved"
)

data class DismissCustomerFinancialAlertRequest(
    val reason: String
)

data class CustomerFinancialReportScheduleDto(
    val scheduleId: String,
    val tenantId: String,
    val projectId: String,
    val customerId: String,
    val reportType: String,
    val format: String,
    val frequency: String,
    val timezone: String,
    val status: String,
    val nextRunAt: Long,
    val lastRunAt: Long?,
    val lastRunStatus: String?,
    val consecutiveFailures: Int,
    val createdBy: String,
    val createdAt: Long,
    val updatedAt: Long,
    val version: Long
)

fun CustomerFinancialReportSchedule.toDto(): CustomerFinancialReportScheduleDto = CustomerFinancialReportScheduleDto(
    scheduleId = scheduleId,
    tenantId = tenantId,
    projectId = projectId,
    customerId = customerId,
    reportType = reportType.name,
    format = format.name,
    frequency = frequency.name,
    timezone = timezone,
    status = status.name,
    nextRunAt = nextRunAt,
    lastRunAt = lastRunAt,
    lastRunStatus = lastRunStatus,
    consecutiveFailures = consecutiveFailures,
    createdBy = createdBy,
    createdAt = createdAt,
    updatedAt = updatedAt,
    version = version
)

data class CreateCustomerFinancialReportScheduleRequest(
    val reportType: String,
    val format: String = "PDF",
    val frequency: String,
    val timezone: String = "Asia/Dhaka",
    val firstRunAt: Long? = null
)

data class UpdateCustomerFinancialReportScheduleRequest(
    val format: String? = null,
    val frequency: String? = null,
    val timezone: String? = null,
    val nextRunAt: Long? = null
)

data class CustomerFinancialScheduleExecutionDto(
    val executionId: String,
    val scheduleId: String,
    val customerId: String,
    val reportType: String,
    val format: String,
    val executedAt: Long,
    val status: String,
    val documentDeliveryId: String?,
    val errorMessage: String?,
    val correlationId: String?
)

fun CustomerFinancialScheduleExecution.toDto(): CustomerFinancialScheduleExecutionDto = CustomerFinancialScheduleExecutionDto(
    executionId = executionId,
    scheduleId = scheduleId,
    customerId = customerId,
    reportType = reportType.name,
    format = format.name,
    executedAt = executedAt,
    status = status.name,
    documentDeliveryId = documentDeliveryId,
    errorMessage = errorMessage,
    correlationId = correlationId
)
