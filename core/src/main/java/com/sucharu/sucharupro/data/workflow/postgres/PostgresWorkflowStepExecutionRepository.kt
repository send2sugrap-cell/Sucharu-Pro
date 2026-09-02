package com.sucharu.sucharupro.data.workflow.postgres

import com.sucharu.sucharupro.data.persistence.postgres.TenantContext
import com.sucharu.sucharupro.data.persistence.postgres.TransactionManager
import com.sucharu.sucharupro.domain.workflow.model.StepExecutionStatus
import com.sucharu.sucharupro.domain.workflow.model.WorkflowStepExecution
import com.sucharu.sucharupro.domain.workflow.model.WorkflowStepType
import java.sql.ResultSet
import java.sql.Timestamp

/**
 * Interface for workflow step execution history persistence (INFRA-04 Step 05).
 */
interface WorkflowStepExecutionRepository {
    suspend fun recordStepExecution(execution: WorkflowStepExecution, tenantContext: TenantContext)
    suspend fun updateStepExecution(execution: WorkflowStepExecution, tenantContext: TenantContext)
    suspend fun getExecutionsForWorkflow(workflowId: String, tenantContext: TenantContext): List<WorkflowStepExecution>
}

/**
 * PostgreSQL implementation of WorkflowStepExecutionRepository.
 */
class PostgresWorkflowStepExecutionRepository(
    private val transactionManager: TransactionManager
) : WorkflowStepExecutionRepository {

    override suspend fun recordStepExecution(execution: WorkflowStepExecution, tenantContext: TenantContext) {
        transactionManager.inTransaction(tenantContext) { txContext ->
            val sql = """
                INSERT INTO workflow_step_executions (
                    step_execution_id, project_id, workflow_id, execution_id,
                    step_id, step_name, step_type, status, attempt_number,
                    input_json, output_json, error_message, failure_classification,
                    started_at, completed_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (project_id, step_execution_id) DO UPDATE SET
                    status = EXCLUDED.status,
                    output_json = EXCLUDED.output_json,
                    error_message = EXCLUDED.error_message,
                    completed_at = EXCLUDED.completed_at;
            """.trimIndent()

            txContext.sqlExecutor.executeUpdate(
                sql = sql,
                params = listOf(
                    execution.stepExecutionId,
                    tenantContext.projectId,
                    execution.workflowId,
                    execution.executionId,
                    execution.stepId,
                    execution.stepName,
                    execution.stepType.name,
                    execution.status.name,
                    execution.attemptNumber,
                    execution.inputJson,
                    execution.outputJson,
                    execution.errorMessage,
                    execution.failureClassification,
                    Timestamp(execution.startedAt),
                    execution.completedAt?.let { Timestamp(it) }
                )
            )
        }
    }

    override suspend fun updateStepExecution(execution: WorkflowStepExecution, tenantContext: TenantContext) {
        recordStepExecution(execution, tenantContext)
    }

    override suspend fun getExecutionsForWorkflow(
        workflowId: String,
        tenantContext: TenantContext
    ): List<WorkflowStepExecution> {
        return transactionManager.inReadOnly(tenantContext) { txContext ->
            val sql = """
                SELECT step_execution_id, project_id, workflow_id, execution_id,
                       step_id, step_name, step_type, status, attempt_number,
                       input_json, output_json, error_message, failure_classification,
                       started_at, completed_at
                FROM workflow_step_executions
                WHERE project_id = ? AND workflow_id = ?
                ORDER BY started_at ASC
            """.trimIndent()

            txContext.sqlExecutor.queryList(
                sql = sql,
                params = listOf(tenantContext.projectId, workflowId)
            ) { rs ->
                WorkflowStepExecution(
                    stepExecutionId = rs.getString("step_execution_id"),
                    projectId = rs.getString("project_id"),
                    workflowId = rs.getString("workflow_id"),
                    executionId = rs.getString("execution_id"),
                    stepId = rs.getString("step_id"),
                    stepName = rs.getString("step_name"),
                    stepType = WorkflowStepType.valueOf(rs.getString("step_type")),
                    status = StepExecutionStatus.valueOf(rs.getString("status")),
                    attemptNumber = rs.getInt("attempt_number"),
                    inputJson = rs.getString("input_json"),
                    outputJson = rs.getString("output_json"),
                    errorMessage = rs.getString("error_message"),
                    failureClassification = rs.getString("failure_classification"),
                    startedAt = rs.getTimestamp("started_at")?.time ?: 0L,
                    completedAt = rs.getTimestamp("completed_at")?.time
                )
            }
        }
    }
}
