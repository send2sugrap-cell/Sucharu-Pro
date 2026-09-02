package com.sucharu.sucharupro.data.integration.postgres

import com.sucharu.sucharupro.data.integration.model.ExternalIntegration
import com.sucharu.sucharupro.data.integration.model.IntegrationStatus
import com.sucharu.sucharupro.data.persistence.postgres.TenantContext
import com.sucharu.sucharupro.data.persistence.postgres.TransactionManager
import com.sucharu.sucharupro.domain.event.consumer.orchestration.IntegrationType
import java.sql.ResultSet
import java.sql.Timestamp

/**
 * Interface for External Provider Integration persistence (INFRA-05 Step 05).
 */
interface IntegrationRepository {
    suspend fun saveIntegration(integration: ExternalIntegration, tenantContext: TenantContext)
    suspend fun getIntegrationById(integrationId: String, tenantContext: TenantContext): ExternalIntegration?
    suspend fun getIntegrationByProvider(provider: String, tenantContext: TenantContext): List<ExternalIntegration>
    suspend fun updateStatus(integrationId: String, status: IntegrationStatus, tenantContext: TenantContext)
    suspend fun updateActivity(integrationId: String, success: Boolean, tenantContext: TenantContext)
}

/**
 * PostgreSQL persistent implementation with Row-Level Security (RLS).
 */
class PostgresIntegrationRepository(
    private val transactionManager: TransactionManager
) : IntegrationRepository {

    private fun mapRowToIntegration(rs: ResultSet): ExternalIntegration {
        val allowedTypesStr = rs.getString("allowed_event_types") ?: ""
        val allowedTypes = if (allowedTypesStr.isNotBlank()) {
            allowedTypesStr.split(",").map { it.trim() }.filter { it.isNotEmpty() }.toSet()
        } else emptySet()

        return ExternalIntegration(
            integrationId = rs.getString("integration_id"),
            projectId = rs.getString("project_id"),
            provider = rs.getString("provider"),
            integrationType = IntegrationType.valueOf(rs.getString("integration_type")),
            status = IntegrationStatus.valueOf(rs.getString("status")),
            baseUrl = rs.getString("base_url"),
            configurationReference = rs.getString("configuration_reference"),
            allowedEventTypes = allowedTypes,
            version = rs.getString("version") ?: "v1",
            createdAt = rs.getTimestamp("created_at").time,
            updatedAt = rs.getTimestamp("updated_at").time,
            lastSuccessfulAt = rs.getTimestamp("last_successful_at")?.time,
            lastFailureAt = rs.getTimestamp("last_failure_at")?.time
        )
    }

    override suspend fun saveIntegration(integration: ExternalIntegration, tenantContext: TenantContext) {
        require(integration.projectId == tenantContext.projectId) {
            "Tenant isolation mismatch: integration projectId '${integration.projectId}' != tenant '${tenantContext.projectId}'"
        }

        transactionManager.inTransaction(tenantContext) { txContext ->
            val sql = """
                INSERT INTO external_integrations (
                    integration_id, project_id, provider, integration_type, status,
                    base_url, configuration_reference, allowed_event_types, version,
                    created_at, updated_at, last_successful_at, last_failure_at
                ) VALUES (
                    ?, ?, ?, ?, ?,
                    ?, ?, ?, ?,
                    ?, ?, ?, ?
                )
                ON CONFLICT (project_id, integration_id) DO UPDATE SET
                    provider = EXCLUDED.provider,
                    integration_type = EXCLUDED.integration_type,
                    status = EXCLUDED.status,
                    base_url = EXCLUDED.base_url,
                    configuration_reference = EXCLUDED.configuration_reference,
                    allowed_event_types = EXCLUDED.allowed_event_types,
                    version = EXCLUDED.version,
                    updated_at = EXCLUDED.updated_at
            """.trimIndent()

            val allowedStr = integration.allowedEventTypes.joinToString(",")
            val createdTs = Timestamp(integration.createdAt)
            val updatedTs = Timestamp(integration.updatedAt)
            val successTs = integration.lastSuccessfulAt?.let { Timestamp(it) }
            val failTs = integration.lastFailureAt?.let { Timestamp(it) }

            txContext.sqlExecutor.executeUpdate(
                sql = sql,
                params = listOf(
                    integration.integrationId,
                    tenantContext.projectId,
                    integration.provider,
                    integration.integrationType.name,
                    integration.status.name,
                    integration.baseUrl,
                    integration.configurationReference,
                    allowedStr,
                    integration.version,
                    createdTs,
                    updatedTs,
                    successTs,
                    failTs
                )
            )
        }
    }

    override suspend fun getIntegrationById(integrationId: String, tenantContext: TenantContext): ExternalIntegration? {
        return transactionManager.inReadOnly(tenantContext) { txContext ->
            val sql = "SELECT * FROM external_integrations WHERE project_id = ? AND integration_id = ?"
            txContext.sqlExecutor.querySingleOrNull(sql, listOf(tenantContext.projectId, integrationId)) { rs ->
                mapRowToIntegration(rs)
            }
        }
    }

    override suspend fun getIntegrationByProvider(provider: String, tenantContext: TenantContext): List<ExternalIntegration> {
        return transactionManager.inReadOnly(tenantContext) { txContext ->
            val sql = "SELECT * FROM external_integrations WHERE project_id = ? AND provider = ? ORDER BY created_at DESC"
            txContext.sqlExecutor.queryList(sql, listOf(tenantContext.projectId, provider)) { rs ->
                mapRowToIntegration(rs)
            }
        }
    }

    override suspend fun updateStatus(integrationId: String, status: IntegrationStatus, tenantContext: TenantContext) {
        transactionManager.inTransaction(tenantContext) { txContext ->
            val sql = """
                UPDATE external_integrations
                SET status = ?, updated_at = NOW()
                WHERE project_id = ? AND integration_id = ?"
            """.trimIndent()
            txContext.sqlExecutor.executeUpdate(sql, listOf(status.name, tenantContext.projectId, integrationId))
        }
    }

    override suspend fun updateActivity(integrationId: String, success: Boolean, tenantContext: TenantContext) {
        transactionManager.inTransaction(tenantContext) { txContext ->
            val sql = if (success) {
                """
                    UPDATE external_integrations
                    SET last_successful_at = NOW(), updated_at = NOW()
                    WHERE project_id = ? AND integration_id = ?
                """.trimIndent()
            } else {
                """
                    UPDATE external_integrations
                    SET last_failure_at = NOW(), updated_at = NOW()
                    WHERE project_id = ? AND integration_id = ?
                """.trimIndent()
            }
            txContext.sqlExecutor.executeUpdate(sql, listOf(tenantContext.projectId, integrationId))
        }
    }
}
