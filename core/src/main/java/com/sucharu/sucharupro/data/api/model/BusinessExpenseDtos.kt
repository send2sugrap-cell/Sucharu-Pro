package com.sucharu.sucharupro.data.api.model

import com.sucharu.sucharupro.domain.model.businessexpense.BusinessExpense
import com.sucharu.sucharupro.domain.model.businessexpense.BusinessExpenseAuditEvent
import com.sucharu.sucharupro.domain.model.businessexpense.BusinessExpenseCategory
import com.sucharu.sucharupro.domain.model.businessexpense.BusinessExpensePaymentMethod
import com.sucharu.sucharupro.domain.model.businessexpense.BusinessExpenseStatus
import java.math.BigDecimal

data class CreateBusinessExpenseRequest(
    val categoryId: String,
    val amount: String,
    val currency: String = "BDT",
    val expenseDate: Long? = null,
    val paymentMethod: String = "CASH",
    val paymentReference: String? = null,
    val vendorId: String? = null,
    val jobId: String? = null,
    val branchId: String? = null,
    val locationId: String? = null,
    val description: String,
    val notes: String? = null,
    val attachmentUrl: String? = null,
    val attachmentMetadata: String? = null,
    val autoSubmit: Boolean = false
)

data class UpdateBusinessExpenseRequest(
    val categoryId: String? = null,
    val amount: String? = null,
    val currency: String? = null,
    val expenseDate: Long? = null,
    val paymentMethod: String? = null,
    val paymentReference: String? = null,
    val vendorId: String? = null,
    val jobId: String? = null,
    val branchId: String? = null,
    val locationId: String? = null,
    val description: String? = null,
    val notes: String? = null,
    val attachmentUrl: String? = null,
    val attachmentMetadata: String? = null
)

data class ApproveExpenseRequest(
    val notes: String? = null
)

data class RejectExpenseRequest(
    val reason: String
)

data class CancelExpenseRequest(
    val reason: String
)

data class CreateBusinessExpenseCategoryRequest(
    val name: String,
    val code: String,
    val description: String? = null,
    val sortOrder: Int = 0
)

data class BusinessExpenseDto(
    val expenseId: String,
    val tenantId: String,
    val projectId: String,
    val branchId: String? = null,
    val locationId: String? = null,
    val expenseNumber: String,
    val expenseCategoryId: String,
    val amount: String,
    val currency: String,
    val expenseDate: Long,
    val paymentMethod: String,
    val paymentReference: String? = null,
    val status: String,
    val vendorId: String? = null,
    val jobId: String? = null,
    val description: String,
    val notes: String? = null,
    val attachmentUrl: String? = null,
    val attachmentMetadata: String? = null,
    val createdBy: String,
    val createdAt: Long,
    val submittedBy: String? = null,
    val submittedAt: Long? = null,
    val approvedBy: String? = null,
    val approvedAt: Long? = null,
    val rejectedBy: String? = null,
    val rejectedAt: Long? = null,
    val rejectionReason: String? = null,
    val cancelledBy: String? = null,
    val cancelledAt: Long? = null,
    val cancellationReason: String? = null,
    val updatedAt: Long,
    val version: Long
)

data class BusinessExpenseCategoryDto(
    val categoryId: String,
    val tenantId: String,
    val projectId: String,
    val name: String,
    val code: String,
    val description: String? = null,
    val isActive: Boolean,
    val sortOrder: Int
)

data class BusinessExpenseAuditEventDto(
    val eventId: String,
    val tenantId: String,
    val projectId: String,
    val expenseId: String,
    val eventType: String,
    val actorId: String,
    val actorRole: String,
    val timestamp: Long,
    val previousStatus: String? = null,
    val newStatus: String? = null,
    val reason: String? = null,
    val metadataJson: String? = null
)

data class BusinessExpenseSummaryDto(
    val totalCount: Long,
    val totalAmount: String,
    val draftCount: Long,
    val pendingCount: Long,
    val approvedCount: Long,
    val rejectedCount: Long,
    val cancelledCount: Long
)

fun BusinessExpense.toDto(): BusinessExpenseDto = BusinessExpenseDto(
    expenseId = expenseId,
    tenantId = tenantId,
    projectId = projectId,
    branchId = branchId,
    locationId = locationId,
    expenseNumber = expenseNumber,
    expenseCategoryId = expenseCategoryId,
    amount = amount.toPlainString(),
    currency = currency,
    expenseDate = expenseDate,
    paymentMethod = paymentMethod.name,
    paymentReference = paymentReference,
    status = status.name,
    vendorId = vendorId,
    jobId = jobId,
    description = description,
    notes = notes,
    attachmentUrl = attachmentUrl,
    attachmentMetadata = attachmentMetadata,
    createdBy = createdBy,
    createdAt = createdAt,
    submittedBy = submittedBy,
    submittedAt = submittedAt,
    approvedBy = approvedBy,
    approvedAt = approvedAt,
    rejectedBy = rejectedBy,
    rejectedAt = rejectedAt,
    rejectionReason = rejectionReason,
    cancelledBy = cancelledBy,
    cancelledAt = cancelledAt,
    cancellationReason = cancellationReason,
    updatedAt = updatedAt,
    version = version
)

fun BusinessExpenseCategory.toDto(): BusinessExpenseCategoryDto = BusinessExpenseCategoryDto(
    categoryId = categoryId,
    tenantId = tenantId,
    projectId = projectId,
    name = name,
    code = code,
    description = description,
    isActive = isActive,
    sortOrder = sortOrder
)

fun BusinessExpenseAuditEvent.toDto(): BusinessExpenseAuditEventDto = BusinessExpenseAuditEventDto(
    eventId = eventId,
    tenantId = tenantId,
    projectId = projectId,
    expenseId = expenseId,
    eventType = eventType,
    actorId = actorId,
    actorRole = actorRole,
    timestamp = timestamp,
    previousStatus = previousStatus?.name,
    newStatus = newStatus?.name,
    reason = reason,
    metadataJson = metadataJson
)
