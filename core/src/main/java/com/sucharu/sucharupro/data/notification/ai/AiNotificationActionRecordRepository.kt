package com.sucharu.sucharupro.data.notification.ai

import com.sucharu.sucharupro.data.persistence.postgres.TenantContext
import com.sucharu.sucharupro.domain.notification.ai.AiNotificationActionRequest
import com.sucharu.sucharupro.domain.notification.ai.AiNotificationActionResult

/**
 * Persistent action record to guarantee deterministic idempotency for AI notification operations (INFRA-04 Step 08).
 *
 * Idempotency key dimension: (projectId, aiPrincipalId, actionType, idempotencyKey)
 */
data class AiNotificationActionRecord(
    val actionId: String,
    val projectId: String,
    val agentId: String,
    val actionType: String,
    val idempotencyKey: String,
    val status: String,
    val responseSummary: String,
    val correlationId: String,
    val executedAt: Long = System.currentTimeMillis()
)

interface AiNotificationActionRecordRepository {
    suspend fun getActionRecord(
        projectId: String,
        agentId: String,
        actionType: String,
        idempotencyKey: String,
        tenantContext: TenantContext
    ): AiNotificationActionRecord?

    suspend fun saveActionRecord(
        record: AiNotificationActionRecord,
        tenantContext: TenantContext
    )
}

/**
 * In-memory thread-safe implementation for tests.
 */
class InMemoryAiNotificationActionRecordRepository : AiNotificationActionRecordRepository {
    private val records = java.util.concurrent.ConcurrentHashMap<String, AiNotificationActionRecord>()

    private fun buildKey(projectId: String, agentId: String, actionType: String, idempotencyKey: String): String =
        "$projectId:$agentId:$actionType:$idempotencyKey"

    override suspend fun getActionRecord(
        projectId: String,
        agentId: String,
        actionType: String,
        idempotencyKey: String,
        tenantContext: TenantContext
    ): AiNotificationActionRecord? {
        val key = buildKey(projectId, agentId, actionType, idempotencyKey)
        return records[key]
    }

    override suspend fun saveActionRecord(
        record: AiNotificationActionRecord,
        tenantContext: TenantContext
    ) {
        require(record.projectId == tenantContext.projectId) {
            "Tenant mismatch: record project '${record.projectId}' != context '${tenantContext.projectId}'"
        }
        val key = buildKey(record.projectId, record.agentId, record.actionType, record.idempotencyKey)
        records[key] = record
    }

    fun clear() = records.clear()
}
