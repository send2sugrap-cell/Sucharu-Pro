package com.sucharu.sucharupro.data.api.model

import com.sucharu.sucharupro.domain.model.customerinvoice.CustomerInvoice
import com.sucharu.sucharupro.domain.model.customerinvoice.CustomerInvoiceAuditEvent
import com.sucharu.sucharupro.domain.model.customerinvoice.CustomerInvoiceLine
import com.sucharu.sucharupro.domain.model.customerinvoice.CustomerInvoiceStatus
import java.math.BigDecimal

/**
 * Data Transfer Objects for Customer Invoice & Receivable REST APIs (Module 14 Step 02).
 */
data class CustomerInvoiceLineDto(
    val lineId: String,
    val invoiceId: String,
    val description: String,
    val productId: String? = null,
    val jobId: String? = null,
    val quantity: BigDecimal,
    val unit: String,
    val unitPrice: BigDecimal,
    val discount: BigDecimal,
    val tax: BigDecimal,
    val lineTotal: BigDecimal,
    val notes: String? = null,
    val lineOrder: Int
)

data class CustomerInvoiceDto(
    val invoiceId: String,
    val tenantId: String,
    val projectId: String,
    val customerId: String,
    val customerFinancialAccountId: String,
    val invoiceNumber: String,
    val sourceOrderId: String? = null,
    val sourceJobId: String? = null,
    val issueDate: Long? = null,
    val dueDate: Long? = null,
    val currency: String,
    val subtotal: BigDecimal,
    val discount: BigDecimal,
    val tax: BigDecimal,
    val adjustment: BigDecimal,
    val grandTotal: BigDecimal,
    val paidAmount: BigDecimal,
    val dueAmount: BigDecimal,
    val status: String,
    val lines: List<CustomerInvoiceLineDto> = emptyList(),
    val notes: String? = null,
    val cancellationReason: String? = null,
    val createdAt: Long,
    val createdBy: String,
    val updatedAt: Long,
    val updatedBy: String,
    val version: Long
)

data class CustomerInvoiceAuditEventDto(
    val auditId: String,
    val invoiceId: String,
    val customerId: String,
    val tenantId: String,
    val projectId: String,
    val actorId: String,
    val actorRole: String,
    val action: String,
    val previousStatus: String? = null,
    val newStatus: String? = null,
    val reason: String? = null,
    val occurredAt: Long,
    val metadataJson: String? = null
)

data class CustomerInvoiceLineRequest(
    val description: String,
    val productId: String? = null,
    val jobId: String? = null,
    val quantity: BigDecimal = BigDecimal.ONE,
    val unit: String = "PCS",
    val unitPrice: BigDecimal = BigDecimal.ZERO,
    val discount: BigDecimal = BigDecimal.ZERO,
    val tax: BigDecimal = BigDecimal.ZERO,
    val notes: String? = null
)

data class CreateCustomerInvoiceRequest(
    val customerId: String,
    val customerFinancialAccountId: String,
    val sourceOrderId: String? = null,
    val sourceJobId: String? = null,
    val dueDate: Long? = null,
    val currency: String = "BDT",
    val lines: List<CustomerInvoiceLineRequest> = emptyList(),
    val discount: BigDecimal = BigDecimal.ZERO,
    val tax: BigDecimal = BigDecimal.ZERO,
    val adjustment: BigDecimal = BigDecimal.ZERO,
    val notes: String? = null
)

data class UpdateCustomerInvoiceDraftRequest(
    val lines: List<CustomerInvoiceLineRequest> = emptyList(),
    val discount: BigDecimal = BigDecimal.ZERO,
    val tax: BigDecimal = BigDecimal.ZERO,
    val adjustment: BigDecimal = BigDecimal.ZERO,
    val notes: String? = null,
    val expectedVersion: Long = 1L
)

data class IssueCustomerInvoiceRequest(
    val expectedVersion: Long = 1L
)

data class CancelCustomerInvoiceRequest(
    val reason: String,
    val expectedVersion: Long = 1L
)

fun CustomerInvoiceLine.toDto(): CustomerInvoiceLineDto = CustomerInvoiceLineDto(
    lineId = lineId,
    invoiceId = invoiceId,
    description = description,
    productId = productId,
    jobId = jobId,
    quantity = quantity,
    unit = unit,
    unitPrice = unitPrice,
    discount = discount,
    tax = tax,
    lineTotal = lineTotal,
    notes = notes,
    lineOrder = lineOrder
)

fun CustomerInvoice.toDto(): CustomerInvoiceDto = CustomerInvoiceDto(
    invoiceId = invoiceId,
    tenantId = tenantId,
    projectId = projectId,
    customerId = customerId,
    customerFinancialAccountId = customerFinancialAccountId,
    invoiceNumber = invoiceNumber,
    sourceOrderId = sourceOrderId,
    sourceJobId = sourceJobId,
    issueDate = issueDate,
    dueDate = dueDate,
    currency = currency,
    subtotal = subtotal,
    discount = discount,
    tax = tax,
    adjustment = adjustment,
    grandTotal = grandTotal,
    paidAmount = paidAmount,
    dueAmount = dueAmount,
    status = status.name,
    lines = lines.map { it.toDto() },
    notes = notes,
    cancellationReason = cancellationReason,
    createdAt = createdAt,
    createdBy = createdBy,
    updatedAt = updatedAt,
    updatedBy = updatedBy,
    version = version
)

fun CustomerInvoiceAuditEvent.toDto(): CustomerInvoiceAuditEventDto = CustomerInvoiceAuditEventDto(
    auditId = auditId,
    invoiceId = invoiceId,
    customerId = customerId,
    tenantId = tenantId,
    projectId = projectId,
    actorId = actorId,
    actorRole = actorRole,
    action = action,
    previousStatus = previousStatus?.name,
    newStatus = newStatus?.name,
    reason = reason,
    occurredAt = occurredAt,
    metadataJson = metadataJson
)
