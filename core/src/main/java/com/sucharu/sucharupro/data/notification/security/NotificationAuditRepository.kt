package com.sucharu.sucharupro.data.notification.security

import com.sucharu.sucharupro.data.persistence.postgres.TenantContext
import com.sucharu.sucharupro.domain.notification.security.NotificationSecurityAuditEvent
import com.sucharu.sucharupro.domain.notification.security.NotificationSecurityOperation

/**
 * Append-only repository for notification security audit events (INFRA-04 Step 07).
 *
 * INVARIANT: No implementation may issue UPDATE or DELETE statements on audit records.
 * Corrections must be made by appending a compensating audit record.
 */
interface NotificationAuditRepository {
    /** Appends an immutable audit event. Never updates an existing record. */
    suspend fun appendAuditEvent(
        event: NotificationSecurityAuditEvent,
        tenantContext: TenantContext
    )

    /** Lists audit events for the tenant, most recent first. */
    suspend fun listAuditEvents(
        projectId: String,
        tenantContext: TenantContext,
        limit: Int = 100,
        operationFilter: NotificationSecurityOperation? = null
    ): List<NotificationSecurityAuditEvent>
}

/**
 * In-memory append-only implementation of [NotificationAuditRepository] for unit tests.
 */
class InMemoryNotificationAuditRepository : NotificationAuditRepository {

    // CopyOnWriteArrayList for thread-safe append in concurrent tests
    private val records = java.util.concurrent.CopyOnWriteArrayList<NotificationSecurityAuditEvent>()

    override suspend fun appendAuditEvent(
        event: NotificationSecurityAuditEvent,
        tenantContext: TenantContext
    ) {
        // Enforce tenant isolation: only append if projectId matches context
        require(event.projectId == tenantContext.projectId) {
            "Audit event projectId '${event.projectId}' does not match tenant context '${tenantContext.projectId}'"
        }
        records.add(event)
    }

    override suspend fun listAuditEvents(
        projectId: String,
        tenantContext: TenantContext,
        limit: Int,
        operationFilter: NotificationSecurityOperation?
    ): List<NotificationSecurityAuditEvent> {
        require(projectId == tenantContext.projectId) {
            "Cross-tenant audit list blocked: requested '$projectId', context '${tenantContext.projectId}'"
        }
        return records
            .filter { it.projectId == projectId }
            .filter { operationFilter == null || it.operation == operationFilter }
            .sortedByDescending { it.occurredAt }
            .take(limit)
    }

    fun allRecords(): List<NotificationSecurityAuditEvent> = records.toList()
    fun clear() = records.clear()
    fun count(): Int = records.size
}
