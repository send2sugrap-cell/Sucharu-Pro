package com.sucharu.sucharupro.data.api.model

import com.sucharu.sucharupro.domain.model.customerpayment.CustomerPayment
import com.sucharu.sucharupro.domain.model.customerpayment.CustomerPaymentAuditEvent
import com.sucharu.sucharupro.domain.model.customerpayment.CustomerPaymentMethod
import com.sucharu.sucharupro.domain.model.customerpayment.CustomerPaymentStatus
import java.math.BigDecimal

/**
 * Data Transfer Objects for Customer Payment REST APIs (Module 14 Step 03).
 */
data class CustomerPaymentDto(
    val paymentId: String,
    val tenantId: String,
    val projectId: String,
    val paymentNumber: String,
    val customerId: String,
    val customerFinancialAccountId: String,
    val invoiceId: String? = null,
    val amount: BigDecimal,
    val currency: String,
    val paymentMethod: String,
    val paymentDate: Long,
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

data class CustomerPaymentAuditEventDto(
    val auditId: String,
    val paymentId: String,
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

data class RecordCustomerPaymentRequest(
    val customerId: String,
    val customerFinancialAccountId: String,
    val invoiceId: String? = null,
    val amount: BigDecimal,
    val currency: String = "BDT",
    val paymentMethod: String = "CASH",
    val paymentDate: Long? = null,
    val referenceNumber: String? = null,
    val externalReference: String? = null,
    val notes: String? = null,
    val idempotencyKey: String? = null
)

data class ConfirmCustomerPaymentRequest(
    val expectedVersion: Long = 1L
)

data class CancelCustomerPaymentRequest(
    val reason: String,
    val expectedVersion: Long = 1L
)

fun CustomerPayment.toDto(): CustomerPaymentDto = CustomerPaymentDto(
    paymentId = paymentId,
    tenantId = tenantId,
    projectId = projectId,
    paymentNumber = paymentNumber,
    customerId = customerId,
    customerFinancialAccountId = customerFinancialAccountId,
    invoiceId = invoiceId,
    amount = amount,
    currency = currency,
    paymentMethod = paymentMethod.name,
    paymentDate = paymentDate,
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

fun CustomerPaymentAuditEvent.toDto(): CustomerPaymentAuditEventDto = CustomerPaymentAuditEventDto(
    auditId = auditId,
    paymentId = paymentId,
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
