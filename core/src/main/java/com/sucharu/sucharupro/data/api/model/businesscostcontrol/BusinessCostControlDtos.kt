package com.sucharu.sucharupro.data.api.model.businesscostcontrol

import com.sucharu.sucharupro.domain.model.businesscostcontrol.*

// --- Requests ---

data class CreateFinancialPeriodRequest(
    val periodCode: String,
    val periodName: String,
    val startDate: Long,
    val endDate: Long
)

data class CloseFinancialPeriodRequest(
    val reason: String
)

data class CreateCostCommitmentRequest(
    val commitmentNumber: String? = null,
    val vendorId: String? = null,
    val jobId: String? = null,
    val costCenterId: String? = null,
    val costCategoryId: String,
    val description: String,
    val committedAmount: String,
    val currency: String? = "BDT",
    val commitmentDate: Long? = null,
    val expectedDate: Long? = null,
    val periodId: String? = null,
    val sourceType: String? = "MANUAL",
    val sourceId: String? = null,
    val idempotencyKey: String? = null,
    val correlationId: String? = null
)

data class UpdateCostCommitmentRequest(
    val vendorId: String? = null,
    val jobId: String? = null,
    val costCenterId: String? = null,
    val costCategoryId: String? = null,
    val description: String? = null,
    val committedAmount: String? = null,
    val currency: String? = null,
    val expectedDate: Long? = null,
    val periodId: String? = null
)

data class ConsumeCostCommitmentRequest(
    val amount: String,
    val sourceType: String? = "MANUAL",
    val sourceId: String,
    val currency: String? = "BDT",
    val notes: String? = null,
    val idempotencyKey: String? = null,
    val correlationId: String? = null
)

data class CancelOrCloseCommitmentRequest(
    val reason: String
)

data class CreateCostAccrualRequest(
    val accrualNumber: String? = null,
    val vendorId: String? = null,
    val jobId: String? = null,
    val costCenterId: String? = null,
    val costCategoryId: String,
    val description: String,
    val accrualAmount: String,
    val currency: String? = "BDT",
    val accountingPeriodId: String,
    val accrualDate: Long? = null,
    val sourceCommitmentId: String? = null,
    val sourceType: String? = "MANUAL",
    val sourceId: String? = null,
    val idempotencyKey: String? = null,
    val correlationId: String? = null
)

data class ReverseCostAccrualRequest(
    val reversalAmount: String,
    val reason: String,
    val accountingPeriodId: String? = null,
    val currency: String? = "BDT",
    val idempotencyKey: String? = null,
    val correlationId: String? = null
)

// --- Responses ---

data class BusinessFinancialPeriodResponse(
    val id: String,
    val tenantId: String,
    val projectId: String,
    val periodCode: String,
    val periodName: String,
    val startDate: Long,
    val endDate: Long,
    val status: String,
    val closedBy: String?,
    val closedAt: Long?,
    val closeReason: String?,
    val createdAt: Long,
    val updatedAt: Long,
    val version: Long
)

data class BusinessCostCommitmentResponse(
    val id: String,
    val tenantId: String,
    val projectId: String,
    val commitmentNumber: String,
    val vendorId: String?,
    val jobId: String?,
    val costCenterId: String?,
    val costCategoryId: String,
    val description: String,
    val committedAmount: String,
    val consumedAmount: String,
    val remainingAmount: String,
    val currency: String,
    val commitmentDate: Long,
    val expectedDate: Long?,
    val periodId: String?,
    val status: String,
    val sourceType: String,
    val sourceId: String,
    val createdBy: String,
    val approvedBy: String?,
    val createdAt: Long,
    val updatedAt: Long,
    val version: Long
)

data class BusinessCostCommitmentConsumptionResponse(
    val id: String,
    val commitmentId: String,
    val sourceType: String,
    val sourceId: String,
    val amount: String,
    val currency: String,
    val consumedAt: Long,
    val createdBy: String,
    val idempotencyKey: String?,
    val notes: String?
)

data class BusinessCostAccrualResponse(
    val id: String,
    val tenantId: String,
    val projectId: String,
    val accrualNumber: String,
    val vendorId: String?,
    val jobId: String?,
    val costCenterId: String?,
    val costCategoryId: String,
    val description: String,
    val accrualAmount: String,
    val reversedAmount: String,
    val netAccrualAmount: String,
    val currency: String,
    val accountingPeriodId: String,
    val accrualDate: Long,
    val sourceCommitmentId: String?,
    val sourceType: String,
    val sourceId: String,
    val status: String,
    val ledgerPostingId: String?,
    val reversalPostingId: String?,
    val createdBy: String,
    val reviewedBy: String?,
    val approvedBy: String?,
    val postedBy: String?,
    val reversedBy: String?,
    val createdAt: Long,
    val updatedAt: Long,
    val version: Long
)

data class BusinessCostAccrualReversalResponse(
    val id: String,
    val accrualId: String,
    val reversalAmount: String,
    val currency: String,
    val reversalDate: Long,
    val accountingPeriodId: String,
    val reason: String,
    val ledgerPostingId: String,
    val createdBy: String,
    val createdAt: Long,
    val idempotencyKey: String?
)

data class BusinessCostControlAuditEventResponse(
    val id: String,
    val entityType: String,
    val entityId: String,
    val eventType: String,
    val actorUserId: String,
    val actorRole: String,
    val timestamp: Long,
    val correlationId: String?,
    val idempotencyKey: String?,
    val previousState: String?,
    val newState: String?,
    val amount: String?,
    val currency: String?,
    val reason: String?,
    val metadata: String?
)

data class BusinessCostControlExceptionResponse(
    val exceptionType: String,
    val description: String,
    val severity: String,
    val sourceEntityId: String,
    val amount: String?,
    val currency: String
)

data class BusinessCostControlDashboardResponse(
    val totalCommitments: String,
    val activeCommitments: String,
    val consumedCommitments: String,
    val remainingCommitments: String,
    val accruedCosts: String,
    val unbilledLiabilities: String,
    val vendorPayables: String,
    val unreconciledAmount: String,
    val totalCommitmentCount: Int,
    val activeCommitmentCount: Int,
    val pendingAccrualCount: Int,
    val exceptionCount: Int,
    val currency: String
)

data class BusinessCostReconciliationSummaryResponse(
    val commitmentAmount: String,
    val consumedAmount: String,
    val accruedAmount: String,
    val payableAmount: String,
    val paidAmount: String,
    val remainingCommitment: String,
    val unbilledAmount: String,
    val unreconciledAmount: String,
    val exceptions: List<BusinessCostControlExceptionResponse>,
    val currency: String
)

data class BusinessCostPeriodEndReportResponse(
    val period: BusinessFinancialPeriodResponse,
    val openCommitmentsAmount: String,
    val pendingAccrualsCount: Int,
    val pendingAccrualsAmount: String,
    val postedAccrualsAmount: String,
    val unbilledLiabilitiesAmount: String,
    val unreconciledPayablesAmount: String,
    val exceptionsCount: Int,
    val isReadyForClosure: Boolean,
    val warnings: List<String>
)

// --- Domain Mappers ---

fun BusinessFinancialPeriod.toResponse() = BusinessFinancialPeriodResponse(
    id = id,
    tenantId = tenantId,
    projectId = projectId,
    periodCode = periodCode,
    periodName = periodName,
    startDate = startDate,
    endDate = endDate,
    status = status.name,
    closedBy = closedBy,
    closedAt = closedAt,
    closeReason = closeReason,
    createdAt = createdAt,
    updatedAt = updatedAt,
    version = version
)

fun BusinessCostCommitment.toResponse() = BusinessCostCommitmentResponse(
    id = id,
    tenantId = tenantId,
    projectId = projectId,
    commitmentNumber = commitmentNumber,
    vendorId = vendorId,
    jobId = jobId,
    costCenterId = costCenterId,
    costCategoryId = costCategoryId,
    description = description,
    committedAmount = committedAmount.toPlainString(),
    consumedAmount = consumedAmount.toPlainString(),
    remainingAmount = remainingAmount.toPlainString(),
    currency = currency,
    commitmentDate = commitmentDate,
    expectedDate = expectedDate,
    periodId = periodId,
    status = status.name,
    sourceType = sourceType.name,
    sourceId = sourceId,
    createdBy = createdBy,
    approvedBy = approvedBy,
    createdAt = createdAt,
    updatedAt = updatedAt,
    version = version
)

fun BusinessCostCommitmentConsumption.toResponse() = BusinessCostCommitmentConsumptionResponse(
    id = id,
    commitmentId = commitmentId,
    sourceType = sourceType.name,
    sourceId = sourceId,
    amount = amount.toPlainString(),
    currency = currency,
    consumedAt = consumedAt,
    createdBy = createdBy,
    idempotencyKey = idempotencyKey,
    notes = notes
)

fun BusinessCostAccrual.toResponse() = BusinessCostAccrualResponse(
    id = id,
    tenantId = tenantId,
    projectId = projectId,
    accrualNumber = accrualNumber,
    vendorId = vendorId,
    jobId = jobId,
    costCenterId = costCenterId,
    costCategoryId = costCategoryId,
    description = description,
    accrualAmount = accrualAmount.toPlainString(),
    reversedAmount = reversedAmount.toPlainString(),
    netAccrualAmount = netAccrualAmount.toPlainString(),
    currency = currency,
    accountingPeriodId = accountingPeriodId,
    accrualDate = accrualDate,
    sourceCommitmentId = sourceCommitmentId,
    sourceType = sourceType.name,
    sourceId = sourceId,
    status = status.name,
    ledgerPostingId = ledgerPostingId,
    reversalPostingId = reversalPostingId,
    createdBy = createdBy,
    reviewedBy = reviewedBy,
    approvedBy = approvedBy,
    postedBy = postedBy,
    reversedBy = reversedBy,
    createdAt = createdAt,
    updatedAt = updatedAt,
    version = version
)

fun BusinessCostAccrualReversal.toResponse() = BusinessCostAccrualReversalResponse(
    id = id,
    accrualId = accrualId,
    reversalAmount = reversalAmount.toPlainString(),
    currency = currency,
    reversalDate = reversalDate,
    accountingPeriodId = accountingPeriodId,
    reason = reason,
    ledgerPostingId = ledgerPostingId,
    createdBy = createdBy,
    createdAt = createdAt,
    idempotencyKey = idempotencyKey
)

fun BusinessCostControlAuditEvent.toResponse() = BusinessCostControlAuditEventResponse(
    id = id,
    entityType = entityType,
    entityId = entityId,
    eventType = eventType,
    actorUserId = actorUserId,
    actorRole = actorRole,
    timestamp = timestamp,
    correlationId = correlationId,
    idempotencyKey = idempotencyKey,
    previousState = previousState,
    newState = newState,
    amount = amount?.toPlainString(),
    currency = currency,
    reason = reason,
    metadata = metadata
)

fun BusinessCostControlException.toResponse() = BusinessCostControlExceptionResponse(
    exceptionType = exceptionType.name,
    description = description,
    severity = severity.name,
    sourceEntityId = sourceEntityId,
    amount = amount?.toPlainString(),
    currency = currency
)

fun BusinessCostControlDashboard.toResponse() = BusinessCostControlDashboardResponse(
    totalCommitments = totalCommitments.toPlainString(),
    activeCommitments = activeCommitments.toPlainString(),
    consumedCommitments = consumedCommitments.toPlainString(),
    remainingCommitments = remainingCommitments.toPlainString(),
    accruedCosts = accruedCosts.toPlainString(),
    unbilledLiabilities = unbilledLiabilities.toPlainString(),
    vendorPayables = vendorPayables.toPlainString(),
    unreconciledAmount = unreconciledAmount.toPlainString(),
    totalCommitmentCount = totalCommitmentCount,
    activeCommitmentCount = activeCommitmentCount,
    pendingAccrualCount = pendingAccrualCount,
    exceptionCount = exceptionCount,
    currency = currency
)

fun BusinessCostReconciliationSummary.toResponse() = BusinessCostReconciliationSummaryResponse(
    commitmentAmount = commitmentAmount.toPlainString(),
    consumedAmount = consumedAmount.toPlainString(),
    accruedAmount = accruedAmount.toPlainString(),
    payableAmount = payableAmount.toPlainString(),
    paidAmount = paidAmount.toPlainString(),
    remainingCommitment = remainingCommitment.toPlainString(),
    unbilledAmount = unbilledAmount.toPlainString(),
    unreconciledAmount = unreconciledAmount.toPlainString(),
    exceptions = exceptions.map { it.toResponse() },
    currency = currency
)

fun BusinessCostPeriodEndReport.toResponse() = BusinessCostPeriodEndReportResponse(
    period = period.toResponse(),
    openCommitmentsAmount = openCommitmentsAmount.toPlainString(),
    pendingAccrualsCount = pendingAccrualsCount,
    pendingAccrualsAmount = pendingAccrualsAmount.toPlainString(),
    postedAccrualsAmount = postedAccrualsAmount.toPlainString(),
    unbilledLiabilitiesAmount = unbilledLiabilitiesAmount.toPlainString(),
    unreconciledPayablesAmount = unreconciledPayablesAmount.toPlainString(),
    exceptionsCount = exceptionsCount,
    isReadyForClosure = isReadyForClosure,
    warnings = warnings
)
