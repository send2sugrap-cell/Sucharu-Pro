package com.sucharu.sucharupro.data.job.postgres

import com.sucharu.sucharupro.data.api.model.PrincipalType
import com.sucharu.sucharupro.data.event.serialization.EventSerializationHelper
import com.sucharu.sucharupro.data.persistence.postgres.TenantContext
import com.sucharu.sucharupro.data.persistence.postgres.TransactionManager
import com.sucharu.sucharupro.domain.event.consumer.EventFailureClassification
import com.sucharu.sucharupro.domain.job.model.JobDefinition
import com.sucharu.sucharupro.domain.job.model.JobPriority
import com.sucharu.sucharupro.domain.job.model.JobStatus
import com.sucharu.sucharupro.domain.job.model.JobTriggerType
import java.sql.ResultSet
import java.sql.Timestamp

/**
 * Interface for background job persistence (INFRA-04 Step 04).
 */
interface JobRepository {
    suspend fun enqueueJob(job: JobDefinition, tenantContext: TenantContext): Boolean
    suspend fun claimEligibleJobs(
        workerId: String,
        limit: Int = 10,
        leaseDurationMs: Long = 30000L,
        tenantContext: TenantContext
    ): List<JobDefinition>
    suspend fun markSucceeded(jobId: String, tenantContext: TenantContext)
    suspend fun markFailed(
        jobId: String,
        errorCode: String?,
        errorMessage: String?,
        classification: EventFailureClassification,
        nextAttemptAt: Long?,
        tenantContext: TenantContext
    )
    suspend fun markDeadLetter(
        jobId: String,
        errorCode: String?,
        errorMessage: String?,
        classification: EventFailureClassification,
        tenantContext: TenantContext
    )
    suspend fun markCancelled(jobId: String, reason: String, tenantContext: TenantContext)
    suspend fun getJobById(jobId: String, tenantContext: TenantContext): JobDefinition?
    suspend fun listQueuedJobs(tenantContext: TenantContext, limit: Int = 50): List<JobDefinition>
    suspend fun recoverExpiredLeases(tenantContext: TenantContext): Int
}

/**
 * Production-grade PostgreSQL Background Job repository with Row-Level Security (RLS) and SKIP LOCKED worker claiming.
 */
class PostgresJobRepository(
    private val transactionManager: TransactionManager
) : JobRepository {

    private fun mapRowToJob(rs: ResultSet): JobDefinition {
        val failureClassStr = rs.getString("failure_classification")
        val classification = if (!failureClassStr.isNullOrBlank()) {
            EventFailureClassification.valueOf(failureClassStr)
        } else null

        val priorityInt = rs.getInt("priority")
        val priority = JobPriority.entries.firstOrNull { it.weight == priorityInt } ?: JobPriority.NORMAL

        val metadataJson = rs.getString("metadata") ?: "{}"
        val metadata = EventSerializationHelper.parseJsonObject(metadataJson)

        return JobDefinition(
            jobId = rs.getString("job_id"),
            projectId = rs.getString("project_id"),
            jobType = rs.getString("job_type"),
            jobVersion = rs.getString("job_version"),
            triggerType = JobTriggerType.valueOf(rs.getString("trigger_type")),
            priority = priority,
            status = JobStatus.valueOf(rs.getString("status")),
            attemptCount = rs.getInt("attempt_count"),
            maxAttempts = rs.getInt("max_attempts"),
            scheduledAt = rs.getTimestamp("scheduled_at").time,
            availableAt = rs.getTimestamp("available_at").time,
            startedAt = rs.getTimestamp("started_at")?.time,
            completedAt = rs.getTimestamp("completed_at")?.time,
            nextAttemptAt = rs.getTimestamp("next_attempt_at")?.time,
            claimedByWorker = rs.getString("claimed_by_worker"),
            claimedAt = rs.getTimestamp("claimed_at")?.time,
            leaseExpiresAt = rs.getTimestamp("lease_expires_at")?.time,
            payloadJson = rs.getString("payload") ?: "{}",
            metadata = metadata,
            correlationId = rs.getString("correlation_id"),
            causationId = rs.getString("causation_id"),
            requestId = rs.getString("request_id"),
            actorType = PrincipalType.valueOf(rs.getString("actor_type")),
            actorId = rs.getString("actor_id"),
            principalType = PrincipalType.valueOf(rs.getString("principal_type")),
            source = rs.getString("source"),
            lastErrorCode = rs.getString("last_error_code"),
            lastErrorMessage = rs.getString("last_error_message"),
            failureClassification = classification,
            idempotencyKey = rs.getString("idempotency_key"),
            createdAt = rs.getTimestamp("created_at").time,
            updatedAt = rs.getTimestamp("updated_at").time
        )
    }

    override suspend fun enqueueJob(job: JobDefinition, tenantContext: TenantContext): Boolean {
        require(job.projectId == tenantContext.projectId) {
            "Tenant isolation violation: Job projectId '${job.projectId}' != tenant '${tenantContext.projectId}'"
        }

        return transactionManager.inTransaction(tenantContext) { txContext ->
            val sql = """
                INSERT INTO background_jobs (
                    job_id, project_id, job_type, job_version, trigger_type, priority,
                    status, attempt_count, max_attempts, scheduled_at, available_at,
                    started_at, completed_at, next_attempt_at, claimed_by_worker,
                    claimed_at, lease_expires_at, payload, metadata, correlation_id,
                    causation_id, request_id, actor_type, actor_id, principal_type,
                    source, last_error_code, last_error_message, failure_classification,
                    idempotency_key, created_at, updated_at
                ) VALUES (
                    ?, ?, ?, ?, ?, ?,
                    ?, ?, ?, ?, ?,
                    ?, ?, ?, ?,
                    ?, ?, ?::jsonb, ?::jsonb, ?,
                    ?, ?, ?, ?, ?,
                    ?, ?, ?, ?,
                    ?, ?, ?
                )
                ON CONFLICT (project_id, idempotency_key) WHERE idempotency_key IS NOT NULL DO NOTHING
            """.trimIndent()

            val metadataJson = EventSerializationHelper.serializeMap(job.metadata)
            val schedTs = Timestamp(job.scheduledAt)
            val availTs = Timestamp(job.availableAt)
            val createdTs = Timestamp(job.createdAt)
            val updatedTs = Timestamp(job.updatedAt)

            val rows = txContext.sqlExecutor.executeUpdate(
                sql = sql,
                params = listOf(
                    job.jobId,
                    tenantContext.projectId,
                    job.jobType,
                    job.jobVersion,
                    job.triggerType.name,
                    job.priority.weight,
                    job.status.name,
                    job.attemptCount,
                    job.maxAttempts,
                    schedTs,
                    availTs,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    job.payloadJson,
                    metadataJson,
                    job.correlationId,
                    job.causationId,
                    job.requestId,
                    job.actorType.name,
                    job.actorId,
                    job.principalType.name,
                    job.source,
                    null,
                    null,
                    null,
                    job.idempotencyKey,
                    createdTs,
                    updatedTs
                )
            )
            rows > 0
        }
    }

    override suspend fun claimEligibleJobs(
        workerId: String,
        limit: Int,
        leaseDurationMs: Long,
        tenantContext: TenantContext
    ): List<JobDefinition> {
        return transactionManager.inTransaction(tenantContext) { txContext ->
            // 1. SELECT FOR UPDATE SKIP LOCKED
            val selectSql = """
                SELECT job_id FROM background_jobs
                WHERE project_id = ?
                  AND status IN ('QUEUED', 'RETRY_SCHEDULED')
                  AND available_at <= NOW()
                ORDER BY priority ASC, available_at ASC, created_at ASC
                FOR UPDATE SKIP LOCKED
                LIMIT ?
            """.trimIndent()

            val jobIds = txContext.sqlExecutor.queryList(selectSql, listOf(tenantContext.projectId, limit)) { rs ->
                rs.getString("job_id")
            }

            if (jobIds.isEmpty()) return@inTransaction emptyList()

            // 2. Claim acquired jobs
            val now = Timestamp(System.currentTimeMillis())
            val leaseExpires = Timestamp(System.currentTimeMillis() + leaseDurationMs)

            val updateSql = """
                UPDATE background_jobs
                SET status = 'CLAIMED',
                    claimed_by_worker = ?,
                    claimed_at = ?,
                    lease_expires_at = ?,
                    started_at = ?,
                    attempt_count = attempt_count + 1,
                    updated_at = NOW()
                WHERE project_id = ? AND job_id = ?
            """.trimIndent()

            for (id in jobIds) {
                txContext.sqlExecutor.executeUpdate(
                    updateSql,
                    listOf(workerId, now, leaseExpires, now, tenantContext.projectId, id)
                )
            }

            // 3. Return updated jobs
            val fetchSql = "SELECT * FROM background_jobs WHERE project_id = ? AND job_id = ?"
            jobIds.mapNotNull { id ->
                txContext.sqlExecutor.querySingleOrNull(fetchSql, listOf(tenantContext.projectId, id)) { rs ->
                    mapRowToJob(rs)
                }
            }
        }
    }

    override suspend fun markSucceeded(jobId: String, tenantContext: TenantContext) {
        transactionManager.inTransaction(tenantContext) { txContext ->
            val sql = """
                UPDATE background_jobs
                SET status = 'SUCCEEDED',
                    completed_at = NOW(),
                    claimed_by_worker = NULL,
                    lease_expires_at = NULL,
                    updated_at = NOW()
                WHERE project_id = ? AND job_id = ?
            """.trimIndent()
            txContext.sqlExecutor.executeUpdate(sql, listOf(tenantContext.projectId, jobId))
        }
    }

    override suspend fun markFailed(
        jobId: String,
        errorCode: String?,
        errorMessage: String?,
        classification: EventFailureClassification,
        nextAttemptAt: Long?,
        tenantContext: TenantContext
    ) {
        transactionManager.inTransaction(tenantContext) { txContext ->
            val status = if (classification.isRetryable && nextAttemptAt != null) {
                JobStatus.RETRY_SCHEDULED
            } else {
                JobStatus.FAILED
            }
            val nextTs = nextAttemptAt?.let { Timestamp(it) }

            val sql = """
                UPDATE background_jobs
                SET status = ?,
                    last_error_code = ?,
                    last_error_message = ?,
                    failure_classification = ?,
                    next_attempt_at = ?,
                    available_at = COALESCE(?, NOW()),
                    claimed_by_worker = NULL,
                    lease_expires_at = NULL,
                    updated_at = NOW()
                WHERE project_id = ? AND job_id = ?
            """.trimIndent()

            txContext.sqlExecutor.executeUpdate(
                sql = sql,
                params = listOf(
                    status.name,
                    errorCode,
                    errorMessage,
                    classification.name,
                    nextTs,
                    nextTs,
                    tenantContext.projectId,
                    jobId
                )
            )
        }
    }

    override suspend fun markDeadLetter(
        jobId: String,
        errorCode: String?,
        errorMessage: String?,
        classification: EventFailureClassification,
        tenantContext: TenantContext
    ) {
        transactionManager.inTransaction(tenantContext) { txContext ->
            val sql = """
                UPDATE background_jobs
                SET status = 'DEAD_LETTER',
                    last_error_code = ?,
                    last_error_message = ?,
                    failure_classification = ?,
                    claimed_by_worker = NULL,
                    lease_expires_at = NULL,
                    completed_at = NOW(),
                    updated_at = NOW()
                WHERE project_id = ? AND job_id = ?
            """.trimIndent()

            txContext.sqlExecutor.executeUpdate(
                sql,
                listOf(errorCode, errorMessage, classification.name, tenantContext.projectId, jobId)
            )
        }
    }

    override suspend fun markCancelled(jobId: String, reason: String, tenantContext: TenantContext) {
        transactionManager.inTransaction(tenantContext) { txContext ->
            val sql = """
                UPDATE background_jobs
                SET status = 'CANCELLED',
                    last_error_message = ?,
                    claimed_by_worker = NULL,
                    lease_expires_at = NULL,
                    completed_at = NOW(),
                    updated_at = NOW()
                WHERE project_id = ? AND job_id = ?
            """.trimIndent()

            txContext.sqlExecutor.executeUpdate(sql, listOf(reason, tenantContext.projectId, jobId))
        }
    }

    override suspend fun getJobById(jobId: String, tenantContext: TenantContext): JobDefinition? {
        return transactionManager.inReadOnly(tenantContext) { txContext ->
            val sql = "SELECT * FROM background_jobs WHERE project_id = ? AND job_id = ?"
            txContext.sqlExecutor.querySingleOrNull(sql, listOf(tenantContext.projectId, jobId)) { rs ->
                mapRowToJob(rs)
            }
        }
    }

    override suspend fun listQueuedJobs(tenantContext: TenantContext, limit: Int): List<JobDefinition> {
        return transactionManager.inReadOnly(tenantContext) { txContext ->
            val sql = """
                SELECT * FROM background_jobs
                WHERE project_id = ? AND status IN ('QUEUED', 'RETRY_SCHEDULED', 'WAITING')
                ORDER BY priority ASC, available_at ASC, created_at ASC
                LIMIT ?
            """.trimIndent()

            txContext.sqlExecutor.queryList(sql, listOf(tenantContext.projectId, limit)) { rs ->
                mapRowToJob(rs)
            }
        }
    }

    override suspend fun recoverExpiredLeases(tenantContext: TenantContext): Int {
        return transactionManager.inTransaction(tenantContext) { txContext ->
            val sql = """
                UPDATE background_jobs
                SET status = 'RETRY_SCHEDULED',
                    claimed_by_worker = NULL,
                    claimed_at = NULL,
                    lease_expires_at = NULL,
                    available_at = NOW(),
                    updated_at = NOW()
                WHERE project_id = ?
                  AND status IN ('CLAIMED', 'RUNNING')
                  AND lease_expires_at < NOW()
            """.trimIndent()

            txContext.sqlExecutor.executeUpdate(sql, listOf(tenantContext.projectId))
        }
    }
}
