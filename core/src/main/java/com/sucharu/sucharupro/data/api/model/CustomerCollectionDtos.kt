package com.sucharu.sucharupro.data.api.model

import com.sucharu.sucharupro.domain.model.customercollection.*
import java.math.BigDecimal

data class CreateCustomerCollectionActionRequest(
    val invoiceId: String? = null,
    val actionType: String = "REMINDER",
    val priority: String? = null,
    val scheduledAt: Long = System.currentTimeMillis(),
    val nextFollowUpAt: Long? = null,
    val assignedUserId: String? = null,
    val notes: String? = null,
    val idempotencyKey: String? = null
)

data class RescheduleCustomerCollectionActionRequest(
    val newScheduledAt: Long,
    val newNextFollowUpAt: Long? = null,
    val notes: String? = null
)

data class AssignCustomerCollectionActionRequest(
    val assignedUserId: String?,
    val notes: String? = null
)

data class CompleteCustomerCollectionActionRequest(
    val outcome: String,
    val outcomeNotes: String? = null,
    val nextFollowUpAt: Long? = null
)

data class CancelCustomerCollectionActionRequest(
    val reason: String
)

data class CreateCustomerPaymentPromiseRequest(
    val invoiceId: String? = null,
    val actionId: String? = null,
    val promisedAmount: BigDecimal,
    val promisedDate: Long,
    val notes: String? = null
)

data class CustomerCollectionActionDto(
    val actionId: String,
    val tenantId: String,
    val projectId: String,
    val customerId: String,
    val invoiceId: String?,
    val actionType: String,
    val priority: String,
    val status: String,
    val scheduledAt: Long,
    val performedAt: Long?,
    val nextFollowUpAt: Long?,
    val assignedUserId: String?,
    val outcome: String?,
    val outcomeNotes: String?,
    val cancellationReason: String?,
    val notes: String?,
    val idempotencyKey: String?,
    val createdAt: Long,
    val createdBy: String,
    val updatedAt: Long,
    val updatedBy: String,
    val version: Long
)

data class CustomerPaymentPromiseDto(
    val promiseId: String,
    val tenantId: String,
    val projectId: String,
    val customerId: String,
    val invoiceId: String?,
    val actionId: String?,
    val promisedAmount: BigDecimal,
    val promisedDate: Long,
    val status: String,
    val notes: String?,
    val fulfilledAt: Long?,
    val fulfilledPaymentId: String?,
    val createdAt: Long,
    val createdBy: String,
    val updatedAt: Long,
    val updatedBy: String,
    val version: Long
)

data class ReceivableDueScheduleItemDto(
    val invoiceId: String,
    val invoiceNumber: String,
    val customerId: String,
    val dueDate: Long,
    val dueAmount: BigDecimal,
    val totalAmount: BigDecimal,
    val daysOverdue: Int,
    val agingBucket: String,
    val priority: String
)

data class CollectionQueueItemDto(
    val customerId: String,
    val customerCode: String,
    val customerDisplayName: String,
    val invoiceId: String?,
    val invoiceNumber: String?,
    val totalOutstanding: BigDecimal,
    val overdueAmount: BigDecimal,
    val oldestDueInvoiceDate: Long?,
    val maxDaysOverdue: Int,
    val agingBucket: String,
    val creditRiskStatus: String,
    val financialHold: Boolean,
    val priority: String,
    val latestActionId: String?,
    val latestActionType: String?,
    val latestActionStatus: String?,
    val nextFollowUpAt: Long?,
    val assignedUserId: String?,
    val activePromiseCount: Int,
    val activePromisedAmount: BigDecimal
)

data class CustomerReceivableCollectionSummaryDto(
    val customerId: String,
    val totalOutstanding: BigDecimal,
    val dueTodayAmount: BigDecimal,
    val upcomingDueAmount: BigDecimal,
    val overdueAmount: BigDecimal,
    val criticalOverdueAmount: BigDecimal,
    val overdueInvoiceCount: Int,
    val pendingActionCount: Int,
    val completedActionCount: Int,
    val activePromiseCount: Int,
    val activePromisedAmount: BigDecimal,
    val creditRiskStatus: String,
    val financialHold: Boolean,
    val priority: String,
    val nextFollowUpAt: Long?,
    val latestOutcome: String?
)

data class CustomerCollectionAuditEventDto(
    val auditId: String,
    val tenantId: String,
    val projectId: String,
    val customerId: String,
    val actionId: String?,
    val actorId: String,
    val actorRole: String,
    val action: String,
    val previousValueJson: String?,
    val newValueJson: String?,
    val reason: String?,
    val occurredAt: Long,
    val metadataJson: String?
)

fun CustomerCollectionAction.toDto() = CustomerCollectionActionDto(
    actionId = actionId,
    tenantId = tenantId,
    projectId = projectId,
    customerId = customerId,
    invoiceId = invoiceId,
    actionType = actionType.name,
    priority = priority.name,
    status = status.name,
    scheduledAt = scheduledAt,
    performedAt = performedAt,
    nextFollowUpAt = nextFollowUpAt,
    assignedUserId = assignedUserId,
    outcome = outcome?.name,
    outcomeNotes = outcomeNotes,
    cancellationReason = cancellationReason,
    notes = notes,
    idempotencyKey = idempotencyKey,
    createdAt = createdAt,
    createdBy = createdBy,
    updatedAt = updatedAt,
    updatedBy = updatedBy,
    version = version
)

fun CustomerPaymentPromise.toDto() = CustomerPaymentPromiseDto(
    promiseId = promiseId,
    tenantId = tenantId,
    projectId = projectId,
    customerId = customerId,
    invoiceId = invoiceId,
    actionId = actionId,
    promisedAmount = promisedAmount,
    promisedDate = promisedDate,
    status = status.name,
    notes = notes,
    fulfilledAt = fulfilledAt,
    fulfilledPaymentId = fulfilledPaymentId,
    createdAt = createdAt,
    createdBy = createdBy,
    updatedAt = updatedAt,
    updatedBy = updatedBy,
    version = version
)

fun ReceivableDueScheduleItem.toDto() = ReceivableDueScheduleItemDto(
    invoiceId = invoiceId,
    invoiceNumber = invoiceNumber,
    customerId = customerId,
    dueDate = dueDate,
    dueAmount = dueAmount,
    totalAmount = totalAmount,
    daysOverdue = daysOverdue,
    agingBucket = agingBucket.name,
    priority = priority.name
)

fun CollectionQueueItem.toDto() = CollectionQueueItemDto(
    customerId = customerId,
    customerCode = customerCode,
    customerDisplayName = customerDisplayName,
    invoiceId = invoiceId,
    invoiceNumber = invoiceNumber,
    totalOutstanding = totalOutstanding,
    overdueAmount = overdueAmount,
    oldestDueInvoiceDate = oldestDueInvoiceDate,
    maxDaysOverdue = maxDaysOverdue,
    agingBucket = agingBucket.name,
    creditRiskStatus = creditRiskStatus.name,
    financialHold = financialHold,
    priority = priority.name,
    latestActionId = latestActionId,
    latestActionType = latestActionType?.name,
    latestActionStatus = latestActionStatus?.name,
    nextFollowUpAt = nextFollowUpAt,
    assignedUserId = assignedUserId,
    activePromiseCount = activePromiseCount,
    activePromisedAmount = activePromisedAmount
)

fun CustomerReceivableCollectionSummary.toDto() = CustomerReceivableCollectionSummaryDto(
    customerId = customerId,
    totalOutstanding = totalOutstanding,
    dueTodayAmount = dueTodayAmount,
    upcomingDueAmount = upcomingDueAmount,
    overdueAmount = overdueAmount,
    criticalOverdueAmount = criticalOverdueAmount,
    overdueInvoiceCount = overdueInvoiceCount,
    pendingActionCount = pendingActionCount,
    completedActionCount = completedActionCount,
    activePromiseCount = activePromiseCount,
    activePromisedAmount = activePromisedAmount,
    creditRiskStatus = creditRiskStatus.name,
    financialHold = financialHold,
    priority = priority.name,
    nextFollowUpAt = nextFollowUpAt,
    latestOutcome = latestOutcome?.name
)

fun CustomerCollectionAuditEvent.toDto() = CustomerCollectionAuditEventDto(
    auditId = auditId,
    tenantId = tenantId,
    projectId = projectId,
    customerId = customerId,
    actionId = actionId,
    actorId = actorId,
    actorRole = actorRole,
    action = action,
    previousValueJson = previousValueJson,
    newValueJson = newValueJson,
    reason = reason,
    occurredAt = occurredAt,
    metadataJson = metadataJson
)
