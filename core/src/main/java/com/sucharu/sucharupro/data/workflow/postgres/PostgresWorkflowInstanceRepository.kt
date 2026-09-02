package com.sucharu.sucharupro.data.workflow.postgres

import com.sucharu.sucharupro.data.api.model.PrincipalType
import com.sucharu.sucharupro.data.persistence.postgres.TenantContext
import com.sucharu.sucharupro.data.persistence.postgres.TransactionManager
import com.sucharu.sucharupro.domain.workflow.model.WorkflowInstance
import com.sucharu.sucharupro.domain.workflow.model.WorkflowStatus
import com.sucharu.sucharupro.domain.workflow.model.WorkflowTransition
import java.sql.ResultSet
import java.sql.Timestamp

/**
 * Interface for workflow instance and transition lifecycle persistence (INFRA-04 Step 05).
 */
interface WorkflowInstanceRepository {
    suspend fun createInstance(instance: WorkflowInstance, tenantContext: TenantContext): Boolean
    suspend fun updateInstance(instance: WorkflowInstance, tenantContext: TenantContext)
    suspend fun getInstanceById(workflowId: String, tenantContext: TenantContext): WorkflowInstance?
    suspend fun listInstancesByStatus(status: WorkflowStatus, limit: Int, tenantContext: TenantContext): List<WorkflowInstance>
    suspend fun recordTransition(transition: WorkflowTransition, tenantContext: TenantContext)
    suspend fun getTransitionsForWorkflow(workflowId: String, tenantContext: TenantContext): List<WorkflowTransition>
}

/**
 * PostgreSQL implementation of WorkflowInstanceRepository with strict RLS.
 */
class PostgresWorkflowInstanceRepository(
    private val transactionManager: TransactionManager
) : WorkflowInstanceRepository {

    override suspend fun createInstance(instance: WorkflowInstance, tenantContext: TenantContext): Boolean {
        return transactionManager.inTransaction(tenantContext) { txContext ->
            val sql = """
                INSERT INTO workflow_instances (
                    workflow_id, project_id, definition_id, version_id, execution_id,
                    status, current_step_id, context_json, correlation_id, causation_id,
                    request_id, actor_type, actor_id, principal_type, idempotency_key,
                    created_at, updated_at, completed_at, failed_at, error_message
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (project_id, workflow_id) DO NOTHING;
            """.trimIndent()

            val rows = txContext.sqlExecutor.executeUpdate(
                sql = sql,
                params = listOf(
                    instance.workflowId,
                    tenantContext.projectId,
                    instance.definitionId,
                    instance.versionId,
                    instance.executionId,
                    instance.status.name,
                    instance.currentStepId,
                    "{}",
                    instance.correlationId,
                    instance.causationId,
                    instance.requestId,
                    instance.actorType.name,
                    instance.actorId,
                    instance.principalType.name,
                    instance.idempotencyKey,
                    Timestamp(instance.createdAt),
                    Timestamp(instance.updatedAt),
                    instance.completedAt?.let { Timestamp(it) },
                    instance.failedAt?.let { Timestamp(it) },
                    instance.errorMessage
                )
            )
            rows > 0
        }
    }

    override suspend fun updateInstance(instance: WorkflowInstance, tenantContext: TenantContext) {
        transactionManager.inTransaction(tenantContext) { txContext ->
            val sql = """
                UPDATE workflow_instances SET
                    status = ?,
                    current_step_id = ?,
                    context_json = ?,
                    updated_at = ?,
                    completed_at = ?,
                    failed_at = ?,
                    error_message = ?
                WHERE project_id = ? AND workflow_id = ?
            """.trimIndent()

            txContext.sqlExecutor.executeUpdate(
                sql = sql,
                params = listOf(
                    instance.status.name,
                    instance.currentStepId,
                    "{}",
                    Timestamp(instance.updatedAt),
                    instance.completedAt?.let { Timestamp(it) },
                    instance.failedAt?.let { Timestamp(it) },
                    instance.errorMessage,
                    tenantContext.projectId,
                    instance.workflowId
                )
            )
        }
    }

    override suspend fun getInstanceById(workflowId: String, tenantContext: TenantContext): WorkflowInstance? {
        return transactionManager.inReadOnly(tenantContext) { txContext ->
            val sql = """
                SELECT workflow_id, project_id, definition_id, version_id, execution_id,
                       status, current_step_id, context_json, correlation_id, causation_id,
                       request_id, actor_type, actor_id, principal_type, idempotency_key,
                       created_at, updated_at, completed_at, failed_at, error_message
                FROM workflow_instances
                WHERE project_id = ? AND workflow_id = ?
            """.trimIndent()

            txContext.sqlExecutor.querySingleOrNull(
                sql = sql,
                params = listOf(tenantContext.projectId, workflowId)
            ) { rs ->
                mapRowToInstance(rs)
            }
        }
    }

    override suspend fun listInstancesByStatus(
        status: WorkflowStatus,
        limit: Int,
        tenantContext: TenantContext
    ): List<WorkflowInstance> {
        return transactionManager.inReadOnly(tenantContext) { txContext ->
            val sql = """
                SELECT workflow_id, project_id, definition_id, version_id, execution_id,
                       status, current_step_id, context_json, correlation_id, causation_id,
                       request_id, actor_type, actor_id, principal_type, idempotency_key,
                       created_at, updated_at, completed_at, failed_at, error_message
                FROM workflow_instances
                WHERE project_id = ? AND status = ?
                ORDER BY created_at DESC
                LIMIT ?
            """.trimIndent()

            txContext.sqlExecutor.queryList(
                sql = sql,
                params = listOf(tenantContext.projectId, status.name, limit)
            ) { rs ->
                mapRowToInstance(rs)
            }
        }
    }

    override suspend fun recordTransition(transition: WorkflowTransition, tenantContext: TenantContext) {
        transactionManager.inTransaction(tenantContext) { txContext ->
            val sql = """
                INSERT INTO workflow_transitions (
                    transition_id, project_id, workflow_id, execution_id,
                    from_status, to_status, trigger_type, actor_type, actor_id,
                    principal_type, metadata_json, transitioned_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """.trimIndent()

            txContext.sqlExecutor.executeUpdate(
                sql = sql,
                params = listOf(
                    transition.transitionId,
                    tenantContext.projectId,
                    transition.workflowId,
                    transition.executionId,
                    transition.fromStatus.name,
                    transition.toStatus.name,
                    transition.triggerType,
                    transition.actorType.name,
                    transition.actorId,
                    transition.principalType.name,
                    "{}",
                    Timestamp(transition.transitionedAt)
                )
            )
        }
    }

    override suspend fun getTransitionsForWorkflow(
        workflowId: String,
        tenantContext: TenantContext
    ): List<WorkflowTransition> {
        return transactionManager.inReadOnly(tenantContext) { txContext ->
            val sql = """
                SELECT transition_id, project_id, workflow_id, execution_id,
                       from_status, to_status, trigger_type, actor_type, actor_id,
                       principal_type, metadata_json, transitioned_at
                FROM workflow_transitions
                WHERE project_id = ? AND workflow_id = ?
                ORDER BY transitioned_at ASC
            """.trimIndent()

            txContext.sqlExecutor.queryList(
                sql = sql,
                params = listOf(tenantContext.projectId, workflowId)
            ) { rs ->
                WorkflowTransition(
                    transitionId = rs.getString("transition_id"),
                    projectId = rs.getString("project_id"),
                    workflowId = rs.getString("workflow_id"),
                    executionId = rs.getString("execution_id"),
                    fromStatus = WorkflowStatus.valueOf(rs.getString("from_status")),
                    toStatus = WorkflowStatus.valueOf(rs.getString("to_status")),
                    triggerType = rs.getString("trigger_type"),
                    actorType = PrincipalType.valueOf(rs.getString("actor_type")),
                    actorId = rs.getString("actor_id"),
                    principalType = PrincipalType.valueOf(rs.getString("principal_type")),
                    metadata = emptyMap(),
                    transitionedAt = rs.getTimestamp("transitioned_at")?.time ?: 0L
                )
            }
        }
    }

    private fun mapRowToInstance(rs: ResultSet): WorkflowInstance {
        return WorkflowInstance(
            workflowId = rs.getString("workflow_id"),
            projectId = rs.getString("project_id"),
            definitionId = rs.getString("definition_id"),
            versionId = rs.getString("version_id"),
            executionId = rs.getString("execution_id"),
            status = WorkflowStatus.valueOf(rs.getString("status")),
            currentStepId = rs.getString("current_step_id"),
            context = emptyMap(),
            correlationId = rs.getString("correlation_id"),
            causationId = rs.getString("causation_id"),
            requestId = rs.getString("request_id"),
            actorType = PrincipalType.valueOf(rs.getString("actor_type")),
            actorId = rs.getString("actor_id"),
            principalType = PrincipalType.valueOf(rs.getString("principal_type")),
            idempotencyKey = rs.getString("idempotency_key"),
            createdAt = rs.getTimestamp("created_at")?.time ?: 0L,
            updatedAt = rs.getTimestamp("updated_at")?.time ?: 0L,
            completedAt = rs.getTimestamp("completed_at")?.time,
            failedAt = rs.getTimestamp("failed_at")?.time,
            errorMessage = rs.getString("error_message")
        )
    }
}
