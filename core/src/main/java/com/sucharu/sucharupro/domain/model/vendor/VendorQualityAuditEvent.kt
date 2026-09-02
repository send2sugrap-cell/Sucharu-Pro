package com.sucharu.sucharupro.domain.model.vendor

/**
 * Immutable audit event for quality inspections and rejections.
 */
data class VendorQualityAuditEvent(
    val auditId: String,
    val projectId: String,
    val tenantId: String = "TENANT-001",
    val entityType: String, // INSPECTION, REJECTION
    val entityId: String,
    val eventType: String,
    val actorId: String,
    val correlationId: String? = null,
    val occurredAt: Long = System.currentTimeMillis(),
    val details: String? = null
)
