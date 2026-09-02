package com.sucharu.sucharupro.data.api.model.businessfinancialadjustment

import com.sucharu.sucharupro.domain.model.businessfinancialadjustment.*

data class CreateAdjustmentRequest(
    val adjustmentNumber: String? = null,
    val adjustmentType: String,
    val sourceType: String,
    val sourceId: String,
    val originalTransactionId: String? = null,
    val originalAmount: String = "0.0000",
    val adjustmentAmount: String,
    val currency: String = "BDT",
    val reason: String,
    val justification: String,
    val periodId: String,
    val costCenterId: String? = null,
    val jobId: String? = null,
    val customerId: String? = null,
    val vendorId: String? = null,
    val idempotencyKey: String? = null
)

data class SubmitAdjustmentRequest(
    val notes: String? = null,
    val correlationId: String? = null
)

data class ReviewAdjustmentRequest(
    val notes: String? = null,
    val correlationId: String? = null
)

data class ApproveAdjustmentRequest(
    val notes: String? = null,
    val correlationId: String? = null
)

data class RejectAdjustmentRequest(
    val reason: String,
    val correlationId: String? = null
)

data class CancelAdjustmentRequest(
    val reason: String,
    val correlationId: String? = null
)

data class PostAdjustmentRequest(
    val debitAccount: String? = null,
    val creditAccount: String? = null,
    val correlationId: String? = null,
    val idempotencyKey: String? = null
)

data class ReverseAdjustmentRequest(
    val reason: String,
    val correlationId: String? = null,
    val idempotencyKey: String? = null
)

data class CreateRefundRequest(
    val refundNumber: String? = null,
    val sourceType: String,
    val sourceId: String,
    val customerId: String? = null,
    val vendorId: String? = null,
    val originalTransactionId: String? = null,
    val eligibleBalance: String = "0.0000",
    val requestedAmount: String,
    val currency: String = "BDT",
    val refundReason: String,
    val paymentMethod: String = "BANK_TRANSFER",
    val periodId: String,
    val idempotencyKey: String? = null
)

data class ApproveRefundRequest(
    val approvedAmount: String? = null,
    val notes: String? = null,
    val correlationId: String? = null
)

data class PostRefundRequest(
    val correlationId: String? = null,
    val idempotencyKey: String? = null
)

data class CreateWriteOffRequest(
    val writeOffNumber: String? = null,
    val sourceType: String,
    val sourceId: String,
    val writeOffType: String,
    val eligibleBalance: String = "0.0000",
    val amount: String,
    val currency: String = "BDT",
    val reason: String,
    val justification: String,
    val periodId: String,
    val customerId: String? = null,
    val vendorId: String? = null,
    val idempotencyKey: String? = null
)

data class ApproveWriteOffRequest(
    val notes: String? = null,
    val correlationId: String? = null
)

data class PostWriteOffRequest(
    val correlationId: String? = null,
    val idempotencyKey: String? = null
)

data class FinancialAdjustmentResponse(
    val id: String,
    val tenantId: String,
    val projectId: String,
    val adjustmentNumber: String,
    val adjustmentType: String,
    val sourceType: String,
    val sourceId: String,
    val originalTransactionId: String? = null,
    val originalAmount: String,
    val adjustmentAmount: String,
    val effectiveAmount: String,
    val currency: String,
    val reason: String,
    val justification: String,
    val status: String,
    val periodId: String,
    val costCenterId: String? = null,
    val jobId: String? = null,
    val customerId: String? = null,
    val vendorId: String? = null,
    val createdBy: String,
    val reviewedBy: String? = null,
    val approvedBy: String? = null,
    val postedBy: String? = null,
    val cancelledBy: String? = null,
    val rejectedBy: String? = null,
    val reversalRequestedBy: String? = null,
    val reversalApprovedBy: String? = null,
    val reviewedAt: Long? = null,
    val approvedAt: Long? = null,
    val postedAt: Long? = null,
    val reversalRequestedAt: Long? = null,
    val reversalApprovedAt: Long? = null,
    val reversedAt: Long? = null,
    val ledgerPostingId: String? = null,
    val reversingPostingId: String? = null,
    val idempotencyKey: String? = null,
    val createdAt: Long,
    val updatedAt: Long
)

data class RefundResponse(
    val id: String,
    val tenantId: String,
    val projectId: String,
    val refundNumber: String,
    val sourceType: String,
    val sourceId: String,
    val customerId: String? = null,
    val vendorId: String? = null,
    val originalTransactionId: String? = null,
    val eligibleBalance: String,
    val requestedAmount: String,
    val approvedAmount: String,
    val currency: String,
    val refundReason: String,
    val paymentMethod: String,
    val status: String,
    val periodId: String,
    val requestedBy: String,
    val approvedBy: String? = null,
    val postedBy: String? = null,
    val approvedAt: Long? = null,
    val postedAt: Long? = null,
    val settledAt: Long? = null,
    val ledgerPostingId: String? = null,
    val idempotencyKey: String? = null,
    val createdAt: Long,
    val updatedAt: Long
)

data class WriteOffResponse(
    val id: String,
    val tenantId: String,
    val projectId: String,
    val writeOffNumber: String,
    val sourceType: String,
    val sourceId: String,
    val writeOffType: String,
    val eligibleBalance: String,
    val amount: String,
    val currency: String,
    val reason: String,
    val justification: String,
    val status: String,
    val periodId: String,
    val customerId: String? = null,
    val vendorId: String? = null,
    val requestedBy: String,
    val approvedBy: String? = null,
    val postedBy: String? = null,
    val approvedAt: Long? = null,
    val postedAt: Long? = null,
    val ledgerPostingId: String? = null,
    val idempotencyKey: String? = null,
    val createdAt: Long,
    val updatedAt: Long
)

data class FinancialAdjustmentAuditEventResponse(
    val id: String,
    val tenantId: String,
    val projectId: String,
    val entityType: String,
    val entityId: String,
    val eventType: String,
    val actorId: String,
    val actorRole: String,
    val timestamp: Long,
    val previousStatus: String? = null,
    val newStatus: String? = null,
    val reason: String? = null,
    val metadataJson: String? = null,
    val correlationId: String? = null,
    val idempotencyKey: String? = null
)

data class FinancialAdjustmentSummaryResponse(
    val tenantId: String,
    val projectId: String,
    val periodId: String? = null,
    val totalAdjustmentsCount: Int,
    val pendingAdjustmentsCount: Int,
    val approvedAdjustmentsCount: Int,
    val postedAdjustmentsCount: Int,
    val totalAdjustedAmount: String,
    val totalRefundedAmount: String,
    val totalWrittenOffAmount: String,
    val totalReversedAmount: String,
    val pendingApprovalAmount: String,
    val postedAmount: String,
    val unresolvedExceptionsCount: Int,
    val calculatedAt: Long
)

data class FinancialExceptionResponse(
    val id: String,
    val entityType: String,
    val entityId: String,
    val referenceNumber: String,
    val issueType: String,
    val severity: String,
    val description: String,
    val amount: String,
    val status: String,
    val detectedAt: Long
)

// --- Extension mappers ---

fun BusinessFinancialAdjustment.toResponse() = FinancialAdjustmentResponse(
    id = id,
    tenantId = tenantId,
    projectId = projectId,
    adjustmentNumber = adjustmentNumber,
    adjustmentType = adjustmentType.name,
    sourceType = sourceType.name,
    sourceId = sourceId,
    originalTransactionId = originalTransactionId,
    originalAmount = originalAmount.toPlainString(),
    adjustmentAmount = adjustmentAmount.toPlainString(),
    effectiveAmount = effectiveAmount.toPlainString(),
    currency = currency,
    reason = reason,
    justification = justification,
    status = status.name,
    periodId = periodId,
    costCenterId = costCenterId,
    jobId = jobId,
    customerId = customerId,
    vendorId = vendorId,
    createdBy = createdBy,
    reviewedBy = reviewedBy,
    approvedBy = approvedBy,
    postedBy = postedBy,
    cancelledBy = cancelledBy,
    rejectedBy = rejectedBy,
    reversalRequestedBy = reversalRequestedBy,
    reversalApprovedBy = reversalApprovedBy,
    reviewedAt = reviewedAt,
    approvedAt = approvedAt,
    postedAt = postedAt,
    reversalRequestedAt = reversalRequestedAt,
    reversalApprovedAt = reversalApprovedAt,
    reversedAt = reversedAt,
    ledgerPostingId = ledgerPostingId,
    reversingPostingId = reversingPostingId,
    idempotencyKey = idempotencyKey,
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun BusinessFinancialRefund.toResponse() = RefundResponse(
    id = id,
    tenantId = tenantId,
    projectId = projectId,
    refundNumber = refundNumber,
    sourceType = sourceType.name,
    sourceId = sourceId,
    customerId = customerId,
    vendorId = vendorId,
    originalTransactionId = originalTransactionId,
    eligibleBalance = eligibleBalance.toPlainString(),
    requestedAmount = requestedAmount.toPlainString(),
    approvedAmount = approvedAmount.toPlainString(),
    currency = currency,
    refundReason = refundReason,
    paymentMethod = paymentMethod,
    status = status.name,
    periodId = periodId,
    requestedBy = requestedBy,
    approvedBy = approvedBy,
    postedBy = postedBy,
    approvedAt = approvedAt,
    postedAt = postedAt,
    settledAt = settledAt,
    ledgerPostingId = ledgerPostingId,
    idempotencyKey = idempotencyKey,
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun BusinessFinancialWriteOff.toResponse() = WriteOffResponse(
    id = id,
    tenantId = tenantId,
    projectId = projectId,
    writeOffNumber = writeOffNumber,
    sourceType = sourceType.name,
    sourceId = sourceId,
    writeOffType = writeOffType.name,
    eligibleBalance = eligibleBalance.toPlainString(),
    amount = amount.toPlainString(),
    currency = currency,
    reason = reason,
    justification = justification,
    status = status.name,
    periodId = periodId,
    customerId = customerId,
    vendorId = vendorId,
    requestedBy = requestedBy,
    approvedBy = approvedBy,
    postedBy = postedBy,
    approvedAt = approvedAt,
    postedAt = postedAt,
    ledgerPostingId = ledgerPostingId,
    idempotencyKey = idempotencyKey,
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun BusinessFinancialAdjustmentAuditEvent.toResponse() = FinancialAdjustmentAuditEventResponse(
    id = id,
    tenantId = tenantId,
    projectId = projectId,
    entityType = entityType,
    entityId = entityId,
    eventType = eventType,
    actorId = actorId,
    actorRole = actorRole,
    timestamp = timestamp,
    previousStatus = previousStatus,
    newStatus = newStatus,
    reason = reason,
    metadataJson = metadataJson,
    correlationId = correlationId,
    idempotencyKey = idempotencyKey
)

fun BusinessFinancialAdjustmentSummary.toResponse() = FinancialAdjustmentSummaryResponse(
    tenantId = tenantId,
    projectId = projectId,
    periodId = periodId,
    totalAdjustmentsCount = totalAdjustmentsCount,
    pendingAdjustmentsCount = pendingAdjustmentsCount,
    approvedAdjustmentsCount = approvedAdjustmentsCount,
    postedAdjustmentsCount = postedAdjustmentsCount,
    totalAdjustedAmount = totalAdjustedAmount.toPlainString(),
    totalRefundedAmount = totalRefundedAmount.toPlainString(),
    totalWrittenOffAmount = totalWrittenOffAmount.toPlainString(),
    totalReversedAmount = totalReversedAmount.toPlainString(),
    pendingApprovalAmount = pendingApprovalAmount.toPlainString(),
    postedAmount = postedAmount.toPlainString(),
    unresolvedExceptionsCount = unresolvedExceptionsCount,
    calculatedAt = calculatedAt
)

fun BusinessFinancialException.toResponse() = FinancialExceptionResponse(
    id = id,
    entityType = entityType,
    entityId = entityId,
    referenceNumber = referenceNumber,
    issueType = issueType,
    severity = severity,
    description = description,
    amount = amount.toPlainString(),
    status = status,
    detectedAt = detectedAt
)
