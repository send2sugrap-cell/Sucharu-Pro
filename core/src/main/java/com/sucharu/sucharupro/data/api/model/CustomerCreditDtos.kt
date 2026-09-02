package com.sucharu.sucharupro.data.api.model

import com.sucharu.sucharupro.domain.model.customercredit.CustomerAdjustment
import com.sucharu.sucharupro.domain.model.customercredit.CustomerAdvance
import com.sucharu.sucharupro.domain.model.customercredit.CustomerCreditAllocation
import com.sucharu.sucharupro.domain.model.customercredit.CustomerCreditAuditEvent
import com.sucharu.sucharupro.domain.model.customercredit.CustomerCreditSummary
import com.sucharu.sucharupro.domain.model.customercredit.CustomerRefund
import java.math.BigDecimal

/**
 * Data Transfer Objects for Customer Advance, Credit, Adjustment, and Refund REST APIs (Module 14 Step 04).
 */
data class CustomerAdvanceDto(
    val advanceId: String,
    val tenantId: String,
    val projectId: String,
    val customerId: String,
    val customerFinancialAccountId: String,
    val advanceNumber: String,
    val amount: BigDecimal,
    val allocatedAmount: BigDecimal,
    val availableAmount: BigDecimal,
    val currency: String,
    val paymentMethod: String,
    val receiptDate: Long,
    val referenceNumber: String? = null,
    val externalReference: String? = null,
    val notes: String? = null,
    val status: String,
    val idempotencyKey: String? = null,
    val cancellationReason: String? = null,
    val createdAt: Long,
    val createdBy: String,
    val updatedAt: Long,
    val updatedBy: String,
    val version: Long
)

data class CustomerCreditAllocationDto(
    val allocationId: String,
    val tenantId: String,
    val projectId: String,
    val customerId: String,
    val customerFinancialAccountId: String,
    val advanceId: String? = null,
    val invoiceId: String,
    val allocatedAmount: BigDecimal,
    val currency: String,
    val status: String,
    val reversalReason: String? = null,
    val idempotencyKey: String? = null,
    val allocatedAt: Long,
    val allocatedBy: String,
    val reversedAt: Long? = null,
    val reversedBy: String? = null,
    val version: Long
)

data class CustomerAdjustmentDto(
    val adjustmentId: String,
    val tenantId: String,
    val projectId: String,
    val customerId: String,
    val customerFinancialAccountId: String,
    val adjustmentNumber: String,
    val adjustmentType: String,
    val amount: BigDecimal,
    val currency: String,
    val reason: String,
    val referenceNumber: String? = null,
    val notes: String? = null,
    val status: String,
    val idempotencyKey: String? = null,
    val createdAt: Long,
    val createdBy: String,
    val updatedAt: Long,
    val updatedBy: String,
    val version: Long
)

data class CustomerRefundDto(
    val refundId: String,
    val tenantId: String,
    val projectId: String,
    val customerId: String,
    val customerFinancialAccountId: String,
    val paymentId: String? = null,
    val advanceId: String? = null,
    val refundNumber: String,
    val amount: BigDecimal,
    val currency: String,
    val refundMethod: String,
    val reason: String,
    val status: String,
    val rejectionReason: String? = null,
    val idempotencyKey: String? = null,
    val createdAt: Long,
    val createdBy: String,
    val approvedAt: Long? = null,
    val approvedBy: String? = null,
    val processedAt: Long? = null,
    val processedBy: String? = null,
    val completedAt: Long? = null,
    val completedBy: String? = null,
    val updatedAt: Long,
    val updatedBy: String,
    val version: Long
)

data class CustomerCreditSummaryDto(
    val customerId: String,
    val customerFinancialAccountId: String,
    val totalAdvances: BigDecimal,
    val totalAllocated: BigDecimal,
    val totalAvailableCredit: BigDecimal,
    val totalAdjustmentsCredit: BigDecimal,
    val totalAdjustmentsDebit: BigDecimal,
    val totalRefunds: BigDecimal,
    val currency: String
)

data class CustomerCreditAuditEventDto(
    val auditId: String,
    val tenantId: String,
    val projectId: String,
    val customerId: String,
    val entityType: String,
    val entityId: String,
    val actorId: String,
    val actorRole: String,
    val action: String,
    val previousStatus: String? = null,
    val newStatus: String? = null,
    val amount: BigDecimal? = null,
    val reason: String? = null,
    val occurredAt: Long,
    val metadataJson: String? = null
)

// Request DTOs
data class RecordCustomerAdvanceRequest(
    val customerId: String,
    val customerFinancialAccountId: String,
    val amount: BigDecimal,
    val currency: String = "BDT",
    val paymentMethod: String = "CASH",
    val receiptDate: Long? = null,
    val referenceNumber: String? = null,
    val externalReference: String? = null,
    val notes: String? = null,
    val idempotencyKey: String? = null
)

data class CancelCustomerAdvanceRequest(
    val reason: String,
    val expectedVersion: Long
)

data class AllocateCustomerCreditRequest(
    val customerId: String,
    val invoiceId: String,
    val advanceId: String? = null,
    val amount: BigDecimal,
    val idempotencyKey: String? = null
)

data class ReverseCreditAllocationRequest(
    val reason: String,
    val expectedVersion: Long
)

data class RecordCustomerAdjustmentRequest(
    val customerId: String,
    val customerFinancialAccountId: String,
    val adjustmentType: String, // CREDIT or DEBIT
    val amount: BigDecimal,
    val currency: String = "BDT",
    val reason: String,
    val referenceNumber: String? = null,
    val notes: String? = null,
    val idempotencyKey: String? = null
)

data class RequestCustomerRefundRequest(
    val customerId: String,
    val customerFinancialAccountId: String,
    val paymentId: String? = null,
    val advanceId: String? = null,
    val amount: BigDecimal,
    val currency: String = "BDT",
    val refundMethod: String = "CASH",
    val reason: String,
    val idempotencyKey: String? = null
)

data class ApproveCustomerRefundRequest(
    val expectedVersion: Long
)

data class ProcessCustomerRefundRequest(
    val expectedVersion: Long
)

data class CompleteCustomerRefundRequest(
    val expectedVersion: Long
)

data class CancelCustomerRefundRequest(
    val reason: String,
    val expectedVersion: Long
)

// Extension mappers
fun CustomerAdvance.toDto() = CustomerAdvanceDto(
    advanceId = advanceId,
    tenantId = tenantId,
    projectId = projectId,
    customerId = customerId,
    customerFinancialAccountId = customerFinancialAccountId,
    advanceNumber = advanceNumber,
    amount = amount,
    allocatedAmount = allocatedAmount,
    availableAmount = availableAmount,
    currency = currency,
    paymentMethod = paymentMethod.name,
    receiptDate = receiptDate,
    referenceNumber = referenceNumber,
    externalReference = externalReference,
    notes = notes,
    status = status.name,
    idempotencyKey = idempotencyKey,
    cancellationReason = cancellationReason,
    createdAt = createdAt,
    createdBy = createdBy,
    updatedAt = updatedAt,
    updatedBy = updatedBy,
    version = version
)

fun CustomerCreditAllocation.toDto() = CustomerCreditAllocationDto(
    allocationId = allocationId,
    tenantId = tenantId,
    projectId = projectId,
    customerId = customerId,
    customerFinancialAccountId = customerFinancialAccountId,
    advanceId = advanceId,
    invoiceId = invoiceId,
    allocatedAmount = allocatedAmount,
    currency = currency,
    status = status.name,
    reversalReason = reversalReason,
    idempotencyKey = idempotencyKey,
    allocatedAt = allocatedAt,
    allocatedBy = allocatedBy,
    reversedAt = reversedAt,
    reversedBy = reversedBy,
    version = version
)

fun CustomerAdjustment.toDto() = CustomerAdjustmentDto(
    adjustmentId = adjustmentId,
    tenantId = tenantId,
    projectId = projectId,
    customerId = customerId,
    customerFinancialAccountId = customerFinancialAccountId,
    adjustmentNumber = adjustmentNumber,
    adjustmentType = adjustmentType.name,
    amount = amount,
    currency = currency,
    reason = reason,
    referenceNumber = referenceNumber,
    notes = notes,
    status = status.name,
    idempotencyKey = idempotencyKey,
    createdAt = createdAt,
    createdBy = createdBy,
    updatedAt = updatedAt,
    updatedBy = updatedBy,
    version = version
)

fun CustomerRefund.toDto() = CustomerRefundDto(
    refundId = refundId,
    tenantId = tenantId,
    projectId = projectId,
    customerId = customerId,
    customerFinancialAccountId = customerFinancialAccountId,
    paymentId = paymentId,
    advanceId = advanceId,
    refundNumber = refundNumber,
    amount = amount,
    currency = currency,
    refundMethod = refundMethod.name,
    reason = reason,
    status = status.name,
    rejectionReason = rejectionReason,
    idempotencyKey = idempotencyKey,
    createdAt = createdAt,
    createdBy = createdBy,
    approvedAt = approvedAt,
    approvedBy = approvedBy,
    processedAt = processedAt,
    processedBy = processedBy,
    completedAt = completedAt,
    completedBy = completedBy,
    updatedAt = updatedAt,
    updatedBy = updatedBy,
    version = version
)

fun CustomerCreditSummary.toDto() = CustomerCreditSummaryDto(
    customerId = customerId,
    customerFinancialAccountId = customerFinancialAccountId,
    totalAdvances = totalAdvances,
    totalAllocated = totalAllocated,
    totalAvailableCredit = totalAvailableCredit,
    totalAdjustmentsCredit = totalAdjustmentsCredit,
    totalAdjustmentsDebit = totalAdjustmentsDebit,
    totalRefunds = totalRefunds,
    currency = currency
)

fun CustomerCreditAuditEvent.toDto() = CustomerCreditAuditEventDto(
    auditId = auditId,
    tenantId = tenantId,
    projectId = projectId,
    customerId = customerId,
    entityType = entityType.name,
    entityId = entityId,
    actorId = actorId,
    actorRole = actorRole,
    action = action,
    previousStatus = previousStatus,
    newStatus = newStatus,
    amount = amount,
    reason = reason,
    occurredAt = occurredAt,
    metadataJson = metadataJson
)
