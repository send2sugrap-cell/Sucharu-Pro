package com.sucharu.sucharupro.data.api.model

import com.sucharu.sucharupro.domain.model.customerfinancial.CustomerFinancialAccount
import com.sucharu.sucharupro.domain.model.customerfinancial.CustomerFinancialAccountAuditEvent
import com.sucharu.sucharupro.domain.model.customerfinancial.CustomerFinancialAccountStatus

/**
 * DTO representing a Customer Financial Account (Module 14 Step 01).
 */
data class CustomerFinancialAccountDto(
    val financialAccountId: String,
    val tenantId: String,
    val projectId: String,
    val customerId: String,
    val accountNumber: String,
    val currency: String,
    val status: String,
    val suspensionReason: String? = null,
    val closedReason: String? = null,
    val notes: String? = null,
    val createdAt: Long,
    val createdBy: String,
    val updatedAt: Long,
    val updatedBy: String,
    val version: Long
)

data class CustomerFinancialAccountAuditEventDto(
    val auditId: String,
    val financialAccountId: String,
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

data class CreateCustomerFinancialAccountRequest(
    val customerId: String,
    val currency: String = "BDT",
    val notes: String? = null
)

data class UpdateCustomerFinancialAccountStatusRequest(
    val status: String,
    val reason: String? = null,
    val expectedVersion: Long = 1L
)

data class UpdateCustomerFinancialAccountNotesRequest(
    val notes: String?,
    val expectedVersion: Long = 1L
)

fun CustomerFinancialAccount.toDto(): CustomerFinancialAccountDto = CustomerFinancialAccountDto(
    financialAccountId = financialAccountId,
    tenantId = tenantId,
    projectId = projectId,
    customerId = customerId,
    accountNumber = accountNumber,
    currency = currency,
    status = status.name,
    suspensionReason = suspensionReason,
    closedReason = closedReason,
    notes = notes,
    createdAt = createdAt,
    createdBy = createdBy,
    updatedAt = updatedAt,
    updatedBy = updatedBy,
    version = version
)

fun CustomerFinancialAccountAuditEvent.toDto(): CustomerFinancialAccountAuditEventDto = CustomerFinancialAccountAuditEventDto(
    auditId = auditId,
    financialAccountId = financialAccountId,
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
