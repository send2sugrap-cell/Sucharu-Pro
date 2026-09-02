package com.sucharu.sucharupro.data.api.model.profitability

import com.sucharu.sucharupro.domain.model.profitability.*
import java.math.BigDecimal

/**
 * DTOs for Profitability Alerts, Early-Warning & Management Action Monitoring Engine.
 * Module 16 Step 09.
 */

data class ProfitabilityEvaluateAlertsRequestDto(
    val periodId: String? = null,
    val idempotencyKey: String? = null
)

data class ProfitabilityUpdateAlertStatusRequestDto(
    val newStatus: String,
    val resolutionNotes: String? = null
)

data class ProfitabilityCreateAlertRuleRequestDto(
    val ruleName: String,
    val alertType: String,
    val dimensionType: String,
    val thresholdMetric: String,
    val thresholdValue: BigDecimal,
    val comparisonOperator: String,
    val severity: String,
    val enabled: Boolean = true,
    val description: String = "",
    val effectiveFrom: Long = 0L,
    val effectiveTo: Long? = null
)

data class ProfitabilityCreateManagementActionRequestDto(
    val alertId: String,
    val actionCode: String,
    val actionTitle: String,
    val actionDescription: String,
    val priorityScore: BigDecimal? = null,
    val assignedTo: String? = null,
    val dueAt: Long? = null,
    val expectedFinancialImpact: BigDecimal? = null
)

data class ProfitabilityUpdateManagementActionStatusRequestDto(
    val newStatus: String,
    val realizedFinancialImpact: BigDecimal? = null,
    val outcomeNotes: String? = null
)

data class ProfitabilityAlertRuleDto(
    val ruleId: String,
    val tenantId: String,
    val projectId: String,
    val ruleName: String,
    val alertType: String,
    val dimensionType: String,
    val thresholdMetric: String,
    val thresholdValue: BigDecimal,
    val comparisonOperator: String,
    val severity: String,
    val enabled: Boolean,
    val description: String,
    val effectiveFrom: Long,
    val effectiveTo: Long?,
    val version: Int,
    val createdAt: Long,
    val updatedAt: Long
)

data class ProfitabilityAlertDto(
    val alertId: String,
    val tenantId: String,
    val projectId: String,
    val alertType: String,
    val severity: String,
    val status: String,
    val dimensionType: String,
    val dimensionId: String,
    val dimensionLabel: String,
    val periodId: String?,
    val sourceModule: String,
    val sourceStep: String,
    val sourceEntityType: String,
    val sourceEntityId: String,
    val triggerMetric: String,
    val observedValue: BigDecimal,
    val thresholdValue: BigDecimal,
    val direction: String,
    val financialImpact: BigDecimal,
    val detectedAt: Long,
    val firstDetectedAt: Long,
    val lastDetectedAt: Long,
    val occurrenceCount: Int,
    val fingerprint: String,
    val integrityHash: String,
    val explanation: String,
    val recommendedActionCode: String?,
    val isRecurring: Boolean,
    val ruleId: String?,
    val acknowledgedAt: Long?,
    val acknowledgedBy: String?,
    val resolvedAt: Long?,
    val resolvedBy: String?,
    val resolutionNotes: String?
)

data class ProfitabilityAlertOccurrenceDto(
    val occurrenceId: String,
    val alertId: String,
    val tenantId: String,
    val detectedAt: Long,
    val observedValue: BigDecimal,
    val financialImpact: BigDecimal,
    val previousStatus: String,
    val triggerDetails: String,
    val sourceSnapshotId: String?
)

data class ProfitabilityManagementActionDto(
    val actionId: String,
    val alertId: String,
    val tenantId: String,
    val projectId: String,
    val actionCode: String,
    val actionTitle: String,
    val actionDescription: String,
    val priorityScore: BigDecimal,
    val status: String,
    val assignedTo: String?,
    val assignedBy: String?,
    val dueAt: Long?,
    val startedAt: Long?,
    val completedAt: Long?,
    val verifiedAt: Long?,
    val verifiedBy: String?,
    val expectedFinancialImpact: BigDecimal,
    val realizedFinancialImpact: BigDecimal?,
    val outcomeNotes: String?,
    val createdAt: Long,
    val updatedAt: Long,
    val integrityHash: String
)

data class ProfitabilityAlertCorrelationDto(
    val correlationId: String,
    val tenantId: String,
    val projectId: String,
    val correlationTitle: String,
    val primaryDimension: String,
    val primaryEntityId: String,
    val primaryEntityLabel: String,
    val correlatedAlertIds: List<String>,
    val compositeSeverity: String,
    val totalFinancialImpact: BigDecimal,
    val correlationReason: String,
    val detectedAt: Long
)

data class ProfitabilityAlertEscalationDto(
    val escalationId: String,
    val alertId: String,
    val tenantId: String,
    val escalationLevel: String,
    val ageInHours: Long,
    val recurrenceCount: Int,
    val isActionOverdue: Boolean,
    val financialImpact: BigDecimal,
    val justification: String,
    val calculatedAt: Long
)

data class ProfitabilityMonitoringSnapshotDto(
    val snapshotId: String,
    val tenantId: String,
    val projectId: String,
    val periodId: String?,
    val totalActiveAlerts: Int,
    val criticalAlertCount: Int,
    val highAlertCount: Int,
    val mediumAlertCount: Int,
    val lowAlertCount: Int,
    val totalUnresolvedFinancialImpact: BigDecimal,
    val openActionCount: Int,
    val overdueActionCount: Int,
    val recurringIssueCount: Int,
    val escalatedAlertCount: Int,
    val severityDistribution: Map<String, Int>,
    val dimensionDistribution: Map<String, Int>,
    val generatedAt: Long,
    val integrityHash: String
)

data class ProfitabilityAlertAuditEventDto(
    val eventId: String,
    val tenantId: String,
    val projectId: String,
    val alertId: String,
    val action: String,
    val actorId: String,
    val actorRole: String,
    val previousState: String?,
    val newState: String,
    val notes: String?,
    val timestamp: Long
)

data class ProfitabilityAlertProvenanceDto(
    val provenanceId: String,
    val alertId: String,
    val tenantId: String,
    val sourceModule: String,
    val sourceStep: String,
    val sourceEntityType: String,
    val sourceEntityId: String,
    val metricKey: String,
    val metricValue: BigDecimal,
    val calculationTimestamp: Long,
    val provenanceHash: String
)

data class ProfitabilityAlertReconciliationAssertionDto(
    val tenantId: String,
    val projectId: String,
    val isBalanced: Boolean,
    val totalAlertsChecked: Int,
    val totalFinancialImpact: BigDecimal,
    val aggregatedImpactFromAlerts: BigDecimal,
    val discrepancyAmount: BigDecimal,
    val openAlertsCountMatches: Boolean,
    val actionCountsMatch: Boolean,
    val provenanceIntegrityMatches: Boolean,
    val checkedAt: Long,
    val assertions: List<String>
)

data class Module16Step09ProfitabilityAlertHandoffContractDto(
    val tenantId: String,
    val projectId: String,
    val snapshotId: String,
    val totalActiveAlerts: Int,
    val criticalAlertCount: Int,
    val highAlertCount: Int,
    val totalUnresolvedFinancialImpact: BigDecimal,
    val criticalAlerts: List<ProfitabilityAlertDto>,
    val highPriorityActions: List<ProfitabilityManagementActionDto>,
    val activeCorrelations: List<ProfitabilityAlertCorrelationDto>,
    val topEscalations: List<ProfitabilityAlertEscalationDto>,
    val overallHealthRisk: String,
    val handoffIntegrityHash: String,
    val generatedAt: Long,
    val contractVersion: String
)

// ====================================================================================
// MAPPING EXTENSIONS
// ====================================================================================

fun ProfitabilityAlert.toDto(): ProfitabilityAlertDto = ProfitabilityAlertDto(
    alertId = alertId,
    tenantId = tenantId,
    projectId = projectId,
    alertType = alertType.name,
    severity = severity.name,
    status = status.name,
    dimensionType = dimensionType.name,
    dimensionId = dimensionId,
    dimensionLabel = dimensionLabel,
    periodId = periodId,
    sourceModule = sourceModule,
    sourceStep = sourceStep,
    sourceEntityType = sourceEntityType,
    sourceEntityId = sourceEntityId,
    triggerMetric = triggerMetric,
    observedValue = observedValue,
    thresholdValue = thresholdValue,
    direction = direction.name,
    financialImpact = financialImpact,
    detectedAt = detectedAt,
    firstDetectedAt = firstDetectedAt,
    lastDetectedAt = lastDetectedAt,
    occurrenceCount = occurrenceCount,
    fingerprint = fingerprint,
    integrityHash = integrityHash,
    explanation = explanation,
    recommendedActionCode = recommendedActionCode?.name,
    isRecurring = isRecurring,
    ruleId = ruleId,
    acknowledgedAt = acknowledgedAt,
    acknowledgedBy = acknowledgedBy,
    resolvedAt = resolvedAt,
    resolvedBy = resolvedBy,
    resolutionNotes = resolutionNotes
)

fun ProfitabilityAlertRule.toDto(): ProfitabilityAlertRuleDto = ProfitabilityAlertRuleDto(
    ruleId = ruleId,
    tenantId = tenantId,
    projectId = projectId,
    ruleName = ruleName,
    alertType = alertType.name,
    dimensionType = dimensionType.name,
    thresholdMetric = thresholdMetric,
    thresholdValue = thresholdValue,
    comparisonOperator = comparisonOperator.name,
    severity = severity.name,
    enabled = enabled,
    description = description,
    effectiveFrom = effectiveFrom,
    effectiveTo = effectiveTo,
    version = version,
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun ProfitabilityManagementAction.toDto(): ProfitabilityManagementActionDto = ProfitabilityManagementActionDto(
    actionId = actionId,
    alertId = alertId,
    tenantId = tenantId,
    projectId = projectId,
    actionCode = actionCode.name,
    actionTitle = actionTitle,
    actionDescription = actionDescription,
    priorityScore = priorityScore,
    status = status.name,
    assignedTo = assignedTo,
    assignedBy = assignedBy,
    dueAt = dueAt,
    startedAt = startedAt,
    completedAt = completedAt,
    verifiedAt = verifiedAt,
    verifiedBy = verifiedBy,
    expectedFinancialImpact = expectedFinancialImpact,
    realizedFinancialImpact = realizedFinancialImpact,
    outcomeNotes = outcomeNotes,
    createdAt = createdAt,
    updatedAt = updatedAt,
    integrityHash = integrityHash
)

fun ProfitabilityAlertCorrelation.toDto(): ProfitabilityAlertCorrelationDto = ProfitabilityAlertCorrelationDto(
    correlationId = correlationId,
    tenantId = tenantId,
    projectId = projectId,
    correlationTitle = correlationTitle,
    primaryDimension = primaryDimension.name,
    primaryEntityId = primaryEntityId,
    primaryEntityLabel = primaryEntityLabel,
    correlatedAlertIds = correlatedAlertIds,
    compositeSeverity = compositeSeverity.name,
    totalFinancialImpact = totalFinancialImpact,
    correlationReason = correlationReason,
    detectedAt = detectedAt
)

fun ProfitabilityAlertEscalation.toDto(): ProfitabilityAlertEscalationDto = ProfitabilityAlertEscalationDto(
    escalationId = escalationId,
    alertId = alertId,
    tenantId = tenantId,
    escalationLevel = escalationLevel.name,
    ageInHours = ageInHours,
    recurrenceCount = recurrenceCount,
    isActionOverdue = isActionOverdue,
    financialImpact = financialImpact,
    justification = justification,
    calculatedAt = calculatedAt
)

fun ProfitabilityMonitoringSnapshot.toDto(): ProfitabilityMonitoringSnapshotDto = ProfitabilityMonitoringSnapshotDto(
    snapshotId = snapshotId,
    tenantId = tenantId,
    projectId = projectId,
    periodId = periodId,
    totalActiveAlerts = totalActiveAlerts,
    criticalAlertCount = criticalAlertCount,
    highAlertCount = highAlertCount,
    mediumAlertCount = mediumAlertCount,
    lowAlertCount = lowAlertCount,
    totalUnresolvedFinancialImpact = totalUnresolvedFinancialImpact,
    openActionCount = openActionCount,
    overdueActionCount = overdueActionCount,
    recurringIssueCount = recurringIssueCount,
    escalatedAlertCount = escalatedAlertCount,
    severityDistribution = severityDistribution.mapKeys { it.key.name },
    dimensionDistribution = dimensionDistribution.mapKeys { it.key.name },
    generatedAt = generatedAt,
    integrityHash = integrityHash
)

fun ProfitabilityAlertAuditEvent.toDto(): ProfitabilityAlertAuditEventDto = ProfitabilityAlertAuditEventDto(
    eventId = eventId,
    tenantId = tenantId,
    projectId = projectId,
    alertId = alertId,
    action = action,
    actorId = actorId,
    actorRole = actorRole,
    previousState = previousState,
    newState = newState,
    notes = notes,
    timestamp = timestamp
)

fun ProfitabilityAlertProvenance.toDto(): ProfitabilityAlertProvenanceDto = ProfitabilityAlertProvenanceDto(
    provenanceId = provenanceId,
    alertId = alertId,
    tenantId = tenantId,
    sourceModule = sourceModule,
    sourceStep = sourceStep,
    sourceEntityType = sourceEntityType,
    sourceEntityId = sourceEntityId,
    metricKey = metricKey,
    metricValue = metricValue,
    calculationTimestamp = calculationTimestamp,
    provenanceHash = provenanceHash
)

fun ProfitabilityAlertReconciliationAssertion.toDto(): ProfitabilityAlertReconciliationAssertionDto = ProfitabilityAlertReconciliationAssertionDto(
    tenantId = tenantId,
    projectId = projectId,
    isBalanced = isBalanced,
    totalAlertsChecked = totalAlertsChecked,
    totalFinancialImpact = totalFinancialImpact,
    aggregatedImpactFromAlerts = aggregatedImpactFromAlerts,
    discrepancyAmount = discrepancyAmount,
    openAlertsCountMatches = openAlertsCountMatches,
    actionCountsMatch = actionCountsMatch,
    provenanceIntegrityMatches = provenanceIntegrityMatches,
    checkedAt = checkedAt,
    assertions = assertions
)

fun Module16Step09ProfitabilityAlertHandoffContract.toDto(): Module16Step09ProfitabilityAlertHandoffContractDto = Module16Step09ProfitabilityAlertHandoffContractDto(
    tenantId = tenantId,
    projectId = projectId,
    snapshotId = snapshotId,
    totalActiveAlerts = totalActiveAlerts,
    criticalAlertCount = criticalAlertCount,
    highAlertCount = highAlertCount,
    totalUnresolvedFinancialImpact = totalUnresolvedFinancialImpact,
    criticalAlerts = criticalAlerts.map { it.toDto() },
    highPriorityActions = highPriorityActions.map { it.toDto() },
    activeCorrelations = activeCorrelations.map { it.toDto() },
    topEscalations = topEscalations.map { it.toDto() },
    overallHealthRisk = overallHealthRisk.name,
    handoffIntegrityHash = handoffIntegrityHash,
    generatedAt = generatedAt,
    contractVersion = contractVersion
)
