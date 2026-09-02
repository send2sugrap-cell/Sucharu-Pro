package com.sucharu.sucharupro.data.job.postgres

import com.sucharu.sucharupro.data.event.serialization.EventSerializationHelper
import com.sucharu.sucharupro.data.persistence.postgres.TenantContext
import com.sucharu.sucharupro.data.persistence.postgres.TransactionManager
import com.sucharu.sucharupro.domain.event.consumer.EventFailureClassification
import com.sucharu.sucharupro.domain.job.model.JobExecutionRecord
import com.sucharu.sucharupro.domain.job.model.JobStatus
import java.sql.ResultSet
import java.sql.Timestamp

/**
 * Interface for job execution history persistence (INFRA-04 Step 04).
 */
interface JobExecutionRepository {
    suspend fun recordExecution(execution: JobExecutionRecord, tenantContext: TenantContext)
    suspend fun getExecutionsForJob(jobId: String, tenantContext: TenantContext): List<JobExecutionRecord>
}

/**
 * PostgreSQL implementation of [JobExecutionRepository] with multi-tenant RLS.
 */
class PostgresJobExecutionRepository(
    private val transactionManager: TransactionManager
) : JobExecutionRepository {

    private fun mapRowToExecution(rs: ResultSet): JobExecutionRecord {
        val failureClassStr = rs.getString("failure_classification")
        val classification = if (!failureClassStr.isNullOrBlank()) {
            EventFailureClassification.valueOf(failureClassStr)
        } else null

        val outputJson = rs.getString("output_metadata") ?: "{}"
        val outputMetadata = EventSerializationHelper.parseJsonObject(outputJson)

        return JobExecutionRecord(
            executionId = rs.getString("execution_id"),
            projectId = rs.getString("project_id"),
            jobId = rs.getString("job_id"),
            workerId = rs.getString("worker_id"),
            attemptNumber = rs.getInt("attempt_number"),
            startedAt = rs.getTimestamp("started_at").time,
            completedAt = rs.getTimestamp("completed_at")?.time,
            durationMs = rs.getLong("duration_ms"),
            status = JobStatus.valueOf(rs.getString("status")),
            errorCode = rs.getString("error_code"),
            errorMessage = rs.getString("error_message"),
            failureClassification = classification,
            outputMetadata = outputMetadata,
            createdAt = rs.getTimestamp("created_at").time
        )
    }

    override suspend fun recordExecution(execution: JobExecutionRecord, tenantContext: TenantContext) {
        require(execution.projectId == tenantContext.projectId) {
            "Tenant isolation mismatch: execution projectId '${execution.projectId}' != tenant '${tenantContext.projectId}'"
        }

        transactionManager.inTransaction(tenantContext) { txContext ->
            val sql = """
                INSERT INTO job_executions (
                    execution_id, project_id, job_id, worker_id, attempt_number,
                    started_at, completed_at, duration_ms, status, error_code,
                    error_message, failure_classification, output_metadata, created_at
                ) VALUES (
                    ?, ?, ?, ?, ?,
                    ?, ?, ?, ?, ?,
                    ?, ?, ?::jsonb, ?
                )
            """.trimIndent()

            val startTs = Timestamp(execution.startedAt)
            val completeTs = execution.completedAt?.let { Timestamp(it) }
            val outputJson = EventSerializationHelper.serializeMap(execution.outputMetadata)
            val createdTs = Timestamp(execution.createdAt)

            txContext.sqlExecutor.executeUpdate(
                sql = sql,
                params = listOf(
                    execution.executionId,
                    tenantContext.projectId,
                    execution.jobId,
                    execution.workerId,
                    execution.attemptNumber,
                    startTs,
                    completeTs,
                    execution.durationMs,
                    execution.status.name,
                    execution.errorCode,
                    execution.errorMessage,
                    execution.failureClassification?.name,
                    outputJson,
                    createdTs
                )
            )
        }
    }

    override suspend fun getExecutionsForJob(jobId: String, tenantContext: TenantContext): List<JobExecutionRecord> {
        return transactionManager.inReadOnly(tenantContext) { txContext ->
            val sql = """
                SELECT * FROM job_executions
                WHERE project_id = ? AND job_id = ?
                ORDER BY attempt_number ASC
            """.trimIndent()

            txContext.sqlExecutor.queryList(sql, listOf(tenantContext.projectId, jobId)) { rs ->
                mapRowToExecution(rs)
            }
        }
    }
}
