package com.sucharu.sucharupro.data.persistence.postgres

import java.sql.Timestamp
import java.time.Instant
import java.time.temporal.ChronoUnit

/**
 * Idempotency record stored in PostgreSQL (INFRA-01 Step 03).
 */
data class IdempotencyRecord(
    val idempotencyKey: String,
    val projectId: String,
    val endpointAction: String,
    val requestHash: String?,
    val responsePayload: String,
    val statusCode: Int,
    val createdAt: Long,
    val expiresAt: Long
)

/**
 * Shared Idempotency persistence helper preventing duplicate mutations (INFRA-01 Step 03).
 */
object IdempotencyPersistenceHelper {

    suspend fun findRecord(
        sqlExecutor: SqlExecutor,
        projectId: String,
        idempotencyKey: String
    ): IdempotencyRecord? {
        val sql = """
            SELECT idempotency_key, project_id, endpoint_action, request_hash, 
                   response_payload::text, status_code, 
                   EXTRACT(EPOCH FROM created_at) * 1000 AS created_ms, 
                   EXTRACT(EPOCH FROM expires_at) * 1000 AS expires_ms
            FROM idempotency_keys
            WHERE project_id = ? AND idempotency_key = ? AND expires_at > NOW()
        """.trimIndent()

        return sqlExecutor.querySingleOrNull(sql, listOf(projectId, idempotencyKey)) { rs ->
            IdempotencyRecord(
                idempotencyKey = rs.getString("idempotency_key"),
                projectId = rs.getString("project_id"),
                endpointAction = rs.getString("endpoint_action"),
                requestHash = rs.getString("request_hash"),
                responsePayload = rs.getString("response_payload"),
                statusCode = rs.getInt("status_code"),
                createdAt = rs.getLong("created_ms"),
                expiresAt = rs.getLong("expires_ms")
            )
        }
    }

    suspend fun saveRecord(
        sqlExecutor: SqlExecutor,
        projectId: String,
        idempotencyKey: String,
        endpointAction: String,
        requestHash: String? = null,
        responsePayload: String = "{}",
        statusCode: Int = 200,
        ttlHours: Long = 24
    ) {
        val now = Instant.now()
        val expiresAt = now.plus(ttlHours, ChronoUnit.HOURS)

        val sql = """
            INSERT INTO idempotency_keys (
                project_id, idempotency_key, endpoint_action, request_hash, 
                response_payload, status_code, created_at, expires_at
            ) VALUES (?, ?, ?, ?, ?::jsonb, ?, ?, ?)
            ON CONFLICT (project_id, idempotency_key) DO NOTHING
        """.trimIndent()

        sqlExecutor.executeUpdate(
            sql,
            listOf(
                projectId,
                idempotencyKey,
                endpointAction,
                requestHash,
                responsePayload,
                statusCode,
                Timestamp.from(now),
                Timestamp.from(expiresAt)
            )
        )
    }
}
