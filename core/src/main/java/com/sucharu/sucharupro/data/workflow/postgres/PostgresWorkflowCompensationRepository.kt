package com.sucharu.sucharupro.data.workflow.postgres

import com.sucharu.sucharupro.data.persistence.postgres.TenantContext
import com.sucharu.sucharupro.data.persistence.postgres.TransactionManager
import com.sucharu.sucharupro.domain.workflow.model.CompensationStatus
import com.sucharu.sucharupro.domain.workflow.model.WorkflowCompensationRecord
import java.sql.ResultSet
import java.sql.Timestamp

/**
 * Interface for saga compensation audit persistence (INFRA-04 Step 05).
 */
interface WorkflowCompensationRepository {
    suspend fun recordCompensation(compensation: WorkflowCompensationRecord, tenantContext: TenantContext)
    suspend fun getCompensationsForWorkflow(workflowId: String, tenantContext: TenantContext): List<WorkflowCompensationRecord>
}

/**
 * PostgreSQL implementation of WorkflowCompensationRepository.
 */
class PostgresWorkflowCompensationRepository(
    private val transactionManager: TransactionManager
) : WorkflowCompensationRepository {

    override suspend fun recordCompensation(
        compensation: WorkflowCompensationRecord,
        tenantContext: TenantContext
    ) {
        transactionManager.inTransaction(tenantContext) { txContext ->
            val sql = """
                INSERT INTO workflow_compensations (
                    compensation_id, project_id, workflow_id, step_id,
                    step_execution_id, status, attempt_number, payload_json,
                    result_message, error_message, started_at, completed_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (project_id, compensation_id) DO UPDATE SET
                    status = EXCLUDED.status,
                    result_message = EXCLUDED.result_message,
                    error_message = EXCLUDED.error_message,
                    completed_at = EXCLUDED.completed_at;
            """.trimIndent()

            txContext.sqlExecutor.executeUpdate(
                sql = sql,
                params = listOf(
                    compensation.compensationId,
                    tenantContext.projectId,
                    compensation.workflowId,
                    compensation.stepId,
                    compensation.stepExecutionId,
                    compensation.status.name,
                    compensation.attemptNumber,
                    compensation.payloadJson,
                    compensation.resultMessage,
                    compensation.errorMessage,
                    Timestamp(compensation.startedAt),
                    compensation.completedAt?.let { Timestamp(it) }
                )
            )
        }
    }

    override suspend fun getCompensationsForWorkflow(
        workflowId: String,
        tenantContext: TenantContext
    ): List<WorkflowCompensationRecord> {
        return transactionManager.inReadOnly(tenantContext) { txContext ->
            val sql = """
                SELECT compensation_id, project_id, workflow_id, step_id,
                       step_execution_id, status, attempt_number, payload_json,
                       result_message, error_message, started_at, completed_at
                FROM workflow_compensations
                WHERE project_id = ? AND workflow_id = ?
                ORDER BY started_at ASC
            """.trimIndent()

            txContext.sqlExecutor.queryList(
                sql = sql,
                params = listOf(tenantContext.projectId, workflowId)
            ) { rs ->
                WorkflowCompensationRecord(
                    compensationId = rs.getString("compensation_id"),
                    projectId = rs.getString("project_id"),
                    workflowId = rs.getString("workflow_id"),
                    stepId = rs.getString("step_id"),
                    stepExecutionId = rs.getString("step_execution_id"),
                    status = CompensationStatus.valueOf(rs.getString("status")),
                    attemptNumber = rs.getInt("attempt_number"),
                    payloadJson = rs.getString("payload_json"),
                    resultMessage = rs.getString("result_message"),
                    errorMessage = rs.getString("error_message"),
                    startedAt = rs.getTimestamp("started_at")?.time ?: 0L,
                    completedAt = rs.getTimestamp("completed_at")?.time
                )
            }
        }
    }
}
