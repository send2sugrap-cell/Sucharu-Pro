package com.sucharu.sucharupro.data.event.integration.observability

import java.util.UUID
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Immutable audit entry for integration boundaries.
 */
data class IntegrationAuditEntry(
    val auditId: String = UUID.randomUUID().toString(),
    val projectId: String,
    val eventId: String,
    val actorId: String,
    val integrationType: String,
    val action: String,
    val status: String,
    val correlationId: String,
    val details: Map<String, String>,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Tenant-isolated audit logger for integration operations (INFRA-04 Step 03).
 */
class IntegrationAuditLogger {

    private val auditLogs = CopyOnWriteArrayList<IntegrationAuditEntry>()

    /**
     * Logs an integration activity with strict tenant scoping.
     */
    fun logActivity(
        projectId: String,
        eventId: String,
        actorId: String,
        integrationType: String,
        action: String,
        status: String,
        correlationId: String,
        details: Map<String, String> = emptyMap()
    ) {
        // Sanitize details to guarantee zero secrets in audit trail
        val sanitized = details.filterKeys { k ->
            !k.contains("secret", ignoreCase = true) &&
                    !k.contains("token", ignoreCase = true) &&
                    !k.contains("password", ignoreCase = true) &&
                    !k.contains("key", ignoreCase = true)
        }

        auditLogs.add(
            IntegrationAuditEntry(
                projectId = projectId,
                eventId = eventId,
                actorId = actorId,
                integrationType = integrationType,
                action = action,
                status = status,
                correlationId = correlationId,
                details = sanitized
            )
        )
    }

    /**
     * Retrieves audit entries scoped to a specific project.
     */
    fun getLogsForProject(projectId: String): List<IntegrationAuditEntry> {
        return auditLogs.filter { it.projectId == projectId }
    }

    /**
     * Clears logs (for testing).
     */
    fun clear() {
        auditLogs.clear()
    }
}
