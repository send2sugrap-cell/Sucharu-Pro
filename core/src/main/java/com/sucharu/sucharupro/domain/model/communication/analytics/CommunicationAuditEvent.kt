package com.sucharu.sucharupro.domain.model.communication.analytics

import com.sucharu.sucharupro.domain.model.user.UserRole
import java.time.Instant

/**
 * Immutable, append-only audit event for Communication Intelligence operations.
 *
 * Audit Boundary:
 * - Records are NEVER updated or deleted after creation.
 * - Only append operations are permitted.
 * - Every governance action, snapshot verification, and export triggers an audit event.
 */
data class CommunicationAuditEvent(
    val auditEventId: String,
    val projectId: String,
    val actorUserId: String,
    val actorRole: UserRole,
    /** High-level action descriptor. e.g. "SNAPSHOT_VERIFIED", "EXPORT_REQUESTED", "RISK_ACKNOWLEDGED" */
    val action: String,
    val timestamp: Instant = Instant.now(),
    val targetType: String, // e.g. "SNAPSHOT", "EXPORT_REQUEST", "RISK_INDICATOR"
    val targetId: String,
    val previousState: String? = null,
    val newState: String? = null,
    val reason: String? = null,
    /** Correlates this audit to the originating request. */
    val correlationId: String? = null,
    /** Result of the operation. */
    val result: AuditResult,
    /** Optional failure detail — only populated on failure. */
    val failureDetail: String? = null
)

enum class AuditResult {
    SUCCESS, FAILURE, PARTIAL
}
