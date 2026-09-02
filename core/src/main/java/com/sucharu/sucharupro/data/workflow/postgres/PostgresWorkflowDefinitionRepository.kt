package com.sucharu.sucharupro.data.workflow.postgres

import com.sucharu.sucharupro.data.persistence.postgres.TenantContext
import com.sucharu.sucharupro.data.persistence.postgres.TransactionManager
import com.sucharu.sucharupro.domain.workflow.model.WorkflowDefinition
import com.sucharu.sucharupro.domain.workflow.model.WorkflowStepDefinition
import com.sucharu.sucharupro.domain.workflow.model.WorkflowStepType
import com.sucharu.sucharupro.domain.workflow.model.WorkflowVersion
import java.sql.ResultSet
import java.sql.Timestamp

/**
 * Interface for workflow definition and version persistence (INFRA-04 Step 05).
 */
interface WorkflowDefinitionRepository {
    suspend fun saveDefinition(definition: WorkflowDefinition, tenantContext: TenantContext)
    suspend fun getDefinitionById(definitionId: String, tenantContext: TenantContext): WorkflowDefinition?
    suspend fun listDefinitions(tenantContext: TenantContext): List<WorkflowDefinition>
    suspend fun saveVersion(version: WorkflowVersion, tenantContext: TenantContext)
    suspend fun getVersion(definitionId: String, versionId: String, tenantContext: TenantContext): WorkflowVersion?
    suspend fun listVersions(definitionId: String, tenantContext: TenantContext): List<WorkflowVersion>
}

/**
 * PostgreSQL implementation of WorkflowDefinitionRepository with strict RLS.
 */
class PostgresWorkflowDefinitionRepository(
    private val transactionManager: TransactionManager
) : WorkflowDefinitionRepository {

    override suspend fun saveDefinition(definition: WorkflowDefinition, tenantContext: TenantContext) {
        transactionManager.inTransaction(tenantContext) { txContext ->
            val sql = """
                INSERT INTO workflow_definitions (
                    definition_id, project_id, workflow_name, description, is_active,
                    created_by, created_at, updated_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (project_id, definition_id) DO UPDATE SET
                    workflow_name = EXCLUDED.workflow_name,
                    description = EXCLUDED.description,
                    is_active = EXCLUDED.is_active,
                    updated_at = EXCLUDED.updated_at;
            """.trimIndent()

            txContext.sqlExecutor.executeUpdate(
                sql = sql,
                params = listOf(
                    definition.definitionId,
                    tenantContext.projectId,
                    definition.workflowName,
                    definition.description,
                    definition.isActive,
                    definition.createdBy,
                    Timestamp(definition.createdAt),
                    Timestamp(definition.updatedAt)
                )
            )
        }
    }

    override suspend fun getDefinitionById(definitionId: String, tenantContext: TenantContext): WorkflowDefinition? {
        return transactionManager.inReadOnly(tenantContext) { txContext ->
            val sql = """
                SELECT definition_id, project_id, workflow_name, description, is_active,
                       created_by, created_at, updated_at
                FROM workflow_definitions
                WHERE project_id = ? AND definition_id = ?
            """.trimIndent()

            txContext.sqlExecutor.querySingleOrNull(
                sql = sql,
                params = listOf(tenantContext.projectId, definitionId)
            ) { rs ->
                mapRowToDefinition(rs)
            }
        }
    }

    override suspend fun listDefinitions(tenantContext: TenantContext): List<WorkflowDefinition> {
        return transactionManager.inReadOnly(tenantContext) { txContext ->
            val sql = """
                SELECT definition_id, project_id, workflow_name, description, is_active,
                       created_by, created_at, updated_at
                FROM workflow_definitions
                WHERE project_id = ?
                ORDER BY created_at DESC
            """.trimIndent()

            txContext.sqlExecutor.queryList(
                sql = sql,
                params = listOf(tenantContext.projectId)
            ) { rs -> mapRowToDefinition(rs) }
        }
    }

    override suspend fun saveVersion(version: WorkflowVersion, tenantContext: TenantContext) {
        transactionManager.inTransaction(tenantContext) { txContext ->
            val sqlVersion = """
                INSERT INTO workflow_versions (
                    definition_id, project_id, version_id, definition_json,
                    is_active, published_at, published_by
                ) VALUES (?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (project_id, definition_id, version_id) DO UPDATE SET
                    definition_json = EXCLUDED.definition_json,
                    is_active = EXCLUDED.is_active,
                    published_at = EXCLUDED.published_at,
                    published_by = EXCLUDED.published_by;
            """.trimIndent()

            txContext.sqlExecutor.executeUpdate(
                sql = sqlVersion,
                params = listOf(
                    version.definitionId,
                    tenantContext.projectId,
                    version.versionId,
                    version.definitionJson,
                    version.isActive,
                    Timestamp(version.publishedAt),
                    version.publishedBy
                )
            )

            // Save steps
            for (step in version.steps) {
                val sqlStep = """
                    INSERT INTO workflow_steps (
                        step_id, project_id, definition_id, version_id, step_name,
                        step_type, sequence_order, config_json, retry_policy_json,
                        timeout_ms, compensation_step_id, required_capability
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    ON CONFLICT (project_id, definition_id, version_id, step_id) DO UPDATE SET
                        step_name = EXCLUDED.step_name,
                        step_type = EXCLUDED.step_type,
                        sequence_order = EXCLUDED.sequence_order,
                        config_json = EXCLUDED.config_json,
                        timeout_ms = EXCLUDED.timeout_ms;
                """.trimIndent()

                val configJson = mapToJson(step.config)

                txContext.sqlExecutor.executeUpdate(
                    sql = sqlStep,
                    params = listOf(
                        step.stepId,
                        tenantContext.projectId,
                        step.definitionId,
                        step.versionId,
                        step.stepName,
                        step.stepType.name,
                        step.sequenceOrder,
                        configJson,
                        null,
                        step.timeoutMs,
                        step.compensationStepId,
                        step.requiredCapability
                    )
                )
            }
        }
    }

    override suspend fun getVersion(definitionId: String, versionId: String, tenantContext: TenantContext): WorkflowVersion? {
        return transactionManager.inReadOnly(tenantContext) { txContext ->
            val sql = """
                SELECT definition_id, project_id, version_id, definition_json,
                       is_active, published_at, published_by
                FROM workflow_versions
                WHERE project_id = ? AND definition_id = ? AND version_id = ?
            """.trimIndent()

            val ver = txContext.sqlExecutor.querySingleOrNull(
                sql = sql,
                params = listOf(tenantContext.projectId, definitionId, versionId)
            ) { rs ->
                WorkflowVersion(
                    definitionId = rs.getString("definition_id"),
                    projectId = rs.getString("project_id"),
                    versionId = rs.getString("version_id"),
                    steps = emptyList(),
                    definitionJson = rs.getString("definition_json"),
                    isActive = rs.getBoolean("is_active"),
                    publishedAt = rs.getTimestamp("published_at")?.time ?: 0L,
                    publishedBy = rs.getString("published_by")
                )
            } ?: return@inReadOnly null

            val sqlSteps = """
                SELECT step_id, project_id, definition_id, version_id, step_name,
                       step_type, sequence_order, config_json, retry_policy_json,
                       timeout_ms, compensation_step_id, required_capability
                FROM workflow_steps
                WHERE project_id = ? AND definition_id = ? AND version_id = ?
                ORDER BY sequence_order ASC
            """.trimIndent()

            val steps = txContext.sqlExecutor.queryList(
                sql = sqlSteps,
                params = listOf(tenantContext.projectId, definitionId, versionId)
            ) { rs ->
                val configJsonStr = rs.getString("config_json")
                val configMap = jsonToMap(configJsonStr ?: "{}")

                WorkflowStepDefinition(
                    stepId = rs.getString("step_id"),
                    definitionId = rs.getString("definition_id"),
                    versionId = rs.getString("version_id"),
                    projectId = rs.getString("project_id"),
                    stepName = rs.getString("step_name"),
                    stepType = WorkflowStepType.valueOf(rs.getString("step_type")),
                    sequenceOrder = rs.getInt("sequence_order"),
                    config = configMap,
                    retryPolicy = null,
                    timeoutMs = rs.getLong("timeout_ms"),
                    compensationStepId = rs.getString("compensation_step_id"),
                    requiredCapability = rs.getString("required_capability")
                )
            }

            ver.copy(steps = steps)
        }
    }

    private fun mapToJson(map: Map<String, String>): String {
        if (map.isEmpty()) return "{}"
        val entries = map.entries.joinToString(",") { (k, v) ->
            "\"${escapeJson(k)}\":\"${escapeJson(v)}\""
        }
        return "{$entries}"
    }

    private fun jsonToMap(jsonStr: String): Map<String, String> {
        if (jsonStr.isBlank() || jsonStr == "{}") return emptyMap()
        val map = mutableMapOf<String, String>()
        val clean = jsonStr.trim().removeSurrounding("{", "}").trim()
        if (clean.isEmpty()) return emptyMap()
        val pairs = clean.split(",")
        for (pair in pairs) {
            val parts = pair.split(":", limit = 2)
            if (parts.size == 2) {
                val key = parts[0].trim().removeSurrounding("\"")
                val value = parts[1].trim().removeSurrounding("\"")
                map[key] = value
            }
        }
        return map
    }

    private fun escapeJson(s: String): String = s.replace("\\", "\\\\").replace("\"", "\\\"")

    override suspend fun listVersions(definitionId: String, tenantContext: TenantContext): List<WorkflowVersion> {
        return transactionManager.inReadOnly(tenantContext) { txContext ->
            val sql = """
                SELECT version_id, project_id, definition_id, definition_json,
                       is_active, published_at, published_by
                FROM workflow_versions
                WHERE project_id = ? AND definition_id = ?
                ORDER BY published_at DESC
            """.trimIndent()

            txContext.sqlExecutor.queryList(
                sql = sql,
                params = listOf(tenantContext.projectId, definitionId)
            ) { rs ->
                WorkflowVersion(
                    definitionId = rs.getString("definition_id"),
                    projectId = rs.getString("project_id"),
                    versionId = rs.getString("version_id"),
                    steps = emptyList(),
                    definitionJson = rs.getString("definition_json"),
                    isActive = rs.getBoolean("is_active"),
                    publishedAt = rs.getTimestamp("published_at")?.time ?: 0L,
                    publishedBy = rs.getString("published_by")
                )
            }
        }
    }

    private fun mapRowToDefinition(rs: ResultSet): WorkflowDefinition {
        return WorkflowDefinition(
            definitionId = rs.getString("definition_id"),
            projectId = rs.getString("project_id"),
            workflowName = rs.getString("workflow_name"),
            description = rs.getString("description"),
            isActive = rs.getBoolean("is_active"),
            createdBy = rs.getString("created_by"),
            createdAt = rs.getTimestamp("created_at")?.time ?: 0L,
            updatedAt = rs.getTimestamp("updated_at")?.time ?: 0L
        )
    }
}
