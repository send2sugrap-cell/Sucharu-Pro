package com.sucharu.sucharupro.data.notification.security

import com.sucharu.sucharupro.data.persistence.postgres.TenantContext
import com.sucharu.sucharupro.domain.event.boundary.NotificationChannel
import com.sucharu.sucharupro.domain.notification.security.NotificationSuppression
import com.sucharu.sucharupro.domain.notification.security.SuppressionReason
import com.sucharu.sucharupro.domain.notification.security.SuppressionType

/**
 * Repository interface for notification suppression management (INFRA-04 Step 07).
 *
 * All operations are tenant-scoped. Suppression records are soft-deleted only.
 */
interface NotificationSuppressionRepository {
    /** Returns true if the recipient is actively suppressed for the given channel in the tenant. */
    suspend fun isSuppressed(
        projectId: String,
        recipientId: String,
        channel: NotificationChannel,
        tenantContext: TenantContext
    ): Boolean

    /** Creates a suppression record idempotently (ON CONFLICT DO NOTHING semantics). */
    suspend fun createSuppression(
        suppression: NotificationSuppression,
        tenantContext: TenantContext
    )

    /** Soft-deletes a suppression (sets is_active=false). Requires authorization at service layer. */
    suspend fun removeSuppression(
        projectId: String,
        recipientId: String,
        channel: NotificationChannel?,
        removedBy: String,
        tenantContext: TenantContext
    ): Boolean

    /** Lists active suppressions for the tenant. */
    suspend fun listSuppressions(
        projectId: String,
        tenantContext: TenantContext
    ): List<NotificationSuppression>
}

/**
 * In-memory implementation of [NotificationSuppressionRepository] for unit tests (INFRA-04 Step 07).
 */
class InMemoryNotificationSuppressionRepository : NotificationSuppressionRepository {

    private val suppressions = mutableListOf<NotificationSuppression>()

    override suspend fun isSuppressed(
        projectId: String,
        recipientId: String,
        channel: NotificationChannel,
        tenantContext: TenantContext
    ): Boolean {
        val nowMs = System.currentTimeMillis()
        return suppressions.any { s ->
            s.projectId == projectId &&
            s.recipientId == recipientId &&
            s.isActive &&
            (s.channel == null || s.channel == channel) &&
            (s.expiresAt == null || s.expiresAt > nowMs)
        }
    }

    override suspend fun createSuppression(
        suppression: NotificationSuppression,
        tenantContext: TenantContext
    ) {
        // Idempotent: don't add duplicate active suppression for same recipient+channel
        val exists = suppressions.any { s ->
            s.projectId == suppression.projectId &&
            s.recipientId == suppression.recipientId &&
            s.channel == suppression.channel &&
            s.suppressionType == suppression.suppressionType &&
            s.isActive
        }
        if (!exists) {
            suppressions.add(suppression)
        }
    }

    override suspend fun removeSuppression(
        projectId: String,
        recipientId: String,
        channel: NotificationChannel?,
        removedBy: String,
        tenantContext: TenantContext
    ): Boolean {
        val idx = suppressions.indexOfFirst { s ->
            s.projectId == projectId &&
            s.recipientId == recipientId &&
            (channel == null || s.channel == channel) &&
            s.isActive
        }
        if (idx < 0) return false
        suppressions[idx] = suppressions[idx].copy(
            isActive = false,
            removedAt = System.currentTimeMillis(),
            removedBy = removedBy
        )
        return true
    }

    override suspend fun listSuppressions(
        projectId: String,
        tenantContext: TenantContext
    ): List<NotificationSuppression> {
        return suppressions.filter { it.projectId == projectId && it.isActive }
    }

    fun clear() = suppressions.clear()
}
