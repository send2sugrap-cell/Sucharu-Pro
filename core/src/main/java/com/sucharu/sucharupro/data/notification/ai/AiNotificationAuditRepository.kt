package com.sucharu.sucharupro.data.notification.ai

import com.sucharu.sucharupro.data.persistence.postgres.TenantContext
import com.sucharu.sucharupro.domain.notification.ai.AiNotificationAuditEvent
import com.sucharu.sucharupro.domain.notification.ai.AiNotificationAuditOperation

/**
 * Append-only repository for AI notification security audit records (INFRA-04 Step 08).
 *
 * Immutability: No UPDATE or DELETE allowed.
 */
interface AiNotificationAuditRepository {
    suspend fun appendAudit(
        event: AiNotificationAuditEvent,
        tenantContext: TenantContext
    )

    suspend fun listAuditEvents(
        projectId: String,
        tenantContext: TenantContext,
        limit: Int = 100,
        operationFilter: AiNotificationAuditOperation? = null
    ): List<AiNotificationAuditEvent>
}

/**
 * In-memory append-only implementation for tests.
 */
class InMemoryAiNotificationAuditRepository : AiNotificationAuditRepository {
    private val records = java.util.concurrent.CopyOnWriteArrayList<AiNotificationAuditEvent>()

    override suspend fun appendAudit(
        event: AiNotificationAuditEvent,
        tenantContext: TenantContext
    ) {
        require(event.projectId == tenantContext.projectId) {
            "Tenant mismatch: audit event project '${event.projectId}' != context '${tenantContext.projectId}'"
        }
        records.add(event)
    }

    override suspend fun listAuditEvents(
        projectId: String,
        tenantContext: TenantContext,
        limit: Int,
        operationFilter: AiNotificationAuditOperation?
    ): List<AiNotificationAuditEvent> {
        require(projectId == tenantContext.projectId) {
            "Tenant mismatch: requested project '$projectId' != context '${tenantContext.projectId}'"
        }
        return records
            .filter { it.projectId == projectId }
            .filter { operationFilter == null || it.operation == operationFilter }
            .sortedByDescending { it.occurredAt }
            .take(limit)
    }

    fun count(): Int = records.size
    fun allRecords(): List<AiNotificationAuditEvent> = records.toList()
    fun clear() = records.clear()
}
