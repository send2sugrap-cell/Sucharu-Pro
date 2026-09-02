package com.sucharu.sucharupro.data.api.model

import com.sucharu.sucharupro.domain.model.businesscost.*
import java.math.BigDecimal

// --- Request DTOs ---

data class CreateBusinessCostCenterRequest(
    val code: String = "",
    val name: String = "",
    val description: String? = null,
    val parentCostCenterId: String? = null
)

data class UpdateBusinessCostCenterRequest(
    val name: String = "",
    val description: String? = null,
    val parentCostCenterId: String? = null,
    val isActive: Boolean = true
)

data class CreateBusinessCostCategoryRequest(
    val code: String = "",
    val name: String = "",
    val description: String? = null,
    val parentCategoryId: String? = null,
    val isSystemDefined: Boolean = false
)

data class UpdateBusinessCostCategoryRequest(
    val name: String = "",
    val description: String? = null,
    val parentCategoryId: String? = null,
    val isActive: Boolean = true
)

data class TrackOperationalCostRequest(
    val sourceType: String = "BUSINESS_EXPENSE",
    val sourceId: String = "",
    val ledgerPostingId: String? = null,
    val costCenterId: String = "",
    val costCategoryId: String = "",
    val jobId: String? = null,
    val amount: String? = null,
    val currency: String = "BDT",
    val notes: String? = null,
    val idempotencyKey: String? = null,
    val correlationId: String? = null
)

data class ClassifyCostRequest(
    val costCenterId: String = "",
    val costCategoryId: String = "",
    val jobId: String? = null,
    val notes: String? = null,
    val idempotencyKey: String? = null,
    val correlationId: String? = null
)

data class ReclassifyCostRequest(
    val newCostCenterId: String = "",
    val newCostCategoryId: String = "",
    val newJobId: String? = null,
    val reason: String = "",
    val idempotencyKey: String? = null,
    val correlationId: String? = null
)

// --- Response DTOs ---

data class BusinessCostCenterResponse(
    val id: String,
    val tenantId: String,
    val projectId: String,
    val code: String,
    val name: String,
    val description: String?,
    val parentCostCenterId: String?,
    val isActive: Boolean,
    val createdAt: Long,
    val createdBy: String,
    val updatedAt: Long,
    val updatedBy: String,
    val version: Long
)

data class BusinessCostCategoryResponse(
    val id: String,
    val tenantId: String,
    val projectId: String,
    val code: String,
    val name: String,
    val description: String?,
    val parentCategoryId: String?,
    val isActive: Boolean,
    val isSystemDefined: Boolean,
    val createdAt: Long,
    val createdBy: String,
    val updatedAt: Long,
    val updatedBy: String,
    val version: Long
)

data class BusinessCostTrackingResponse(
    val id: String,
    val tenantId: String,
    val projectId: String,
    val sourceType: String,
    val sourceId: String,
    val ledgerPostingId: String?,
    val costCenterId: String,
    val costCategoryId: String,
    val jobId: String?,
    val amount: String,
    val currency: String,
    val allocationStatus: String,
    val classificationStatus: String,
    val notes: String?,
    val createdAt: Long,
    val createdBy: String,
    val updatedAt: Long,
    val updatedBy: String,
    val version: Long
)

data class BusinessCostClassificationAuditEventResponse(
    val eventId: String,
    val tenantId: String,
    val projectId: String,
    val trackingId: String,
    val action: String,
    val actorId: String,
    val actorRole: String,
    val previousStateJson: String?,
    val newStateJson: String?,
    val reason: String,
    val correlationId: String?,
    val idempotencyKey: String?,
    val timestamp: Long
)

// --- DTO Mapper Extensions ---

fun BusinessCostCenter.toResponse() = BusinessCostCenterResponse(
    id = id,
    tenantId = tenantId,
    projectId = projectId,
    code = code,
    name = name,
    description = description,
    parentCostCenterId = parentCostCenterId,
    isActive = isActive,
    createdAt = createdAt,
    createdBy = createdBy,
    updatedAt = updatedAt,
    updatedBy = updatedBy,
    version = version
)

fun BusinessCostCategory.toResponse() = BusinessCostCategoryResponse(
    id = id,
    tenantId = tenantId,
    projectId = projectId,
    code = code,
    name = name,
    description = description,
    parentCategoryId = parentCategoryId,
    isActive = isActive,
    isSystemDefined = isSystemDefined,
    createdAt = createdAt,
    createdBy = createdBy,
    updatedAt = updatedAt,
    updatedBy = updatedBy,
    version = version
)

fun BusinessCostTracking.toResponse() = BusinessCostTrackingResponse(
    id = id,
    tenantId = tenantId,
    projectId = projectId,
    sourceType = sourceType.name,
    sourceId = sourceId,
    ledgerPostingId = ledgerPostingId,
    costCenterId = costCenterId,
    costCategoryId = costCategoryId,
    jobId = jobId,
    amount = amount.toPlainString(),
    currency = currency,
    allocationStatus = allocationStatus.name,
    classificationStatus = classificationStatus.name,
    notes = notes,
    createdAt = createdAt,
    createdBy = createdBy,
    updatedAt = updatedAt,
    updatedBy = updatedBy,
    version = version
)

fun BusinessCostClassificationAuditEvent.toResponse() = BusinessCostClassificationAuditEventResponse(
    eventId = eventId,
    tenantId = tenantId,
    projectId = projectId,
    trackingId = trackingId,
    action = action,
    actorId = actorId,
    actorRole = actorRole,
    previousStateJson = previousStateJson,
    newStateJson = newStateJson,
    reason = reason,
    correlationId = correlationId,
    idempotencyKey = idempotencyKey,
    timestamp = timestamp
)
