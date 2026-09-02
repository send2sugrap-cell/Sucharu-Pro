package com.sucharu.sucharupro.data.notification.ai

import com.sucharu.sucharupro.data.persistence.postgres.TenantContext
import com.sucharu.sucharupro.domain.notification.ai.AiConfirmationStatus
import com.sucharu.sucharupro.domain.notification.ai.AiNotificationConfirmationRequest

/**
 * Tenant-isolated repository for AI notification human confirmations (INFRA-04 Step 08).
 */
interface AiNotificationConfirmationRepository {
    suspend fun saveConfirmation(
        request: AiNotificationConfirmationRequest,
        tenantContext: TenantContext
    )

    suspend fun getConfirmation(
        confirmationId: String,
        tenantContext: TenantContext
    ): AiNotificationConfirmationRequest?

    suspend fun updateConfirmationStatus(
        confirmationId: String,
        status: AiConfirmationStatus,
        approverId: String?,
        approverRole: String?,
        rejectionReason: String?,
        tenantContext: TenantContext
    ): Boolean

    suspend fun listPendingConfirmations(
        tenantContext: TenantContext
    ): List<AiNotificationConfirmationRequest>
}

/**
 * In-memory thread-safe implementation for tests.
 */
class InMemoryAiNotificationConfirmationRepository : AiNotificationConfirmationRepository {
    private val records = java.util.concurrent.ConcurrentHashMap<String, AiNotificationConfirmationRequest>()

    override suspend fun saveConfirmation(
        request: AiNotificationConfirmationRequest,
        tenantContext: TenantContext
    ) {
        require(request.projectId == tenantContext.projectId) {
            "Tenant mismatch: request project '${request.projectId}' != context '${tenantContext.projectId}'"
        }
        val key = "${request.projectId}:${request.confirmationId}"
        records[key] = request
    }

    override suspend fun getConfirmation(
        confirmationId: String,
        tenantContext: TenantContext
    ): AiNotificationConfirmationRequest? {
        val key = "${tenantContext.projectId}:$confirmationId"
        return records[key]
    }

    override suspend fun updateConfirmationStatus(
        confirmationId: String,
        status: AiConfirmationStatus,
        approverId: String?,
        approverRole: String?,
        rejectionReason: String?,
        tenantContext: TenantContext
    ): Boolean {
        val key = "${tenantContext.projectId}:$confirmationId"
        val existing = records[key] ?: return false
        if (existing.status.isTerminal) return false

        records[key] = existing.copy(
            status = status,
            approvedByHumanId = approverId,
            approverRole = approverRole,
            approvedAt = if (status == AiConfirmationStatus.APPROVED) System.currentTimeMillis() else null,
            rejectionReason = rejectionReason
        )
        return true
    }

    override suspend fun listPendingConfirmations(
        tenantContext: TenantContext
    ): List<AiNotificationConfirmationRequest> {
        val now = System.currentTimeMillis()
        return records.values
            .filter { it.projectId == tenantContext.projectId && it.status == AiConfirmationStatus.PENDING && it.expiresAt > now }
            .toList()
    }

    fun clear() = records.clear()
}
