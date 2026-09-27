package com.sucharu.sucharupro.data.persistence.postgres

import com.sucharu.sucharupro.domain.model.copilot.CopilotUserMemory
import java.sql.ResultSet

/**
 * Production-grade PostgreSQL DataSource for Persistent AI User Memory.
 * Enforces PostgreSQL Row-Level Security via [TenantContext].
 */
class PostgresAiUserMemoryDataSource(
    private val transactionManager: TransactionManager
) {

    private fun mapMemory(rs: ResultSet): CopilotUserMemory {
        return CopilotUserMemory(
            memoryId = rs.getString("memory_id"),
            customerId = rs.getString("customer_id"),
            tenantId = rs.getString("project_id"),
            contextCategory = rs.getString("context_category") ?: "PREFERENCE",
            preferenceKey = rs.getString("preference_key"),
            preferenceValue = rs.getString("preference_value"),
            updatedAt = rs.getString("updated_at") ?: ""
        )
    }

    suspend fun saveMemory(memory: CopilotUserMemory, projectId: String): CopilotUserMemory {
        val tenantContext = TenantContext(projectId = projectId)
        return transactionManager.inTransaction(tenantContext) { tx ->
            val sql = """
                INSERT INTO ai_user_memory (
                    memory_id, project_id, customer_id, context_category, preference_key, preference_value, is_active, updated_at
                ) VALUES (?, ?, ?, ?, ?, ?, TRUE, NOW())
                ON CONFLICT (project_id, customer_id, preference_key)
                DO UPDATE SET
                    preference_value = EXCLUDED.preference_value,
                    updated_at = NOW(),
                    version = ai_user_memory.version + 1
            """.trimIndent()

            tx.connection.prepareStatement(sql).use { stmt ->
                stmt.setString(1, memory.memoryId)
                stmt.setString(2, projectId)
                stmt.setString(3, memory.customerId)
                stmt.setString(4, memory.contextCategory)
                stmt.setString(5, memory.preferenceKey)
                stmt.setString(6, memory.preferenceValue)
                stmt.executeUpdate()
            }
            memory
        }
    }

    suspend fun getMemory(projectId: String, customerId: String, key: String): CopilotUserMemory? {
        val tenantContext = TenantContext(projectId = projectId)
        return transactionManager.inReadOnly(tenantContext) { tx ->
            val sql = """
                SELECT memory_id, project_id, customer_id, context_category, preference_key, preference_value, updated_at
                FROM ai_user_memory
                WHERE project_id = ? AND customer_id = ? AND preference_key = ? AND is_active = TRUE
                LIMIT 1
            """.trimIndent()

            tx.connection.prepareStatement(sql).use { stmt ->
                stmt.setString(1, projectId)
                stmt.setString(2, customerId)
                stmt.setString(3, key)
                val rs = stmt.executeQuery()
                if (rs.next()) mapMemory(rs) else null
            }
        }
    }

    suspend fun getMemoriesByCustomer(projectId: String, customerId: String): List<CopilotUserMemory> {
        val tenantContext = TenantContext(projectId = projectId)
        return transactionManager.inReadOnly(tenantContext) { tx ->
            val sql = """
                SELECT memory_id, project_id, customer_id, context_category, preference_key, preference_value, updated_at
                FROM ai_user_memory
                WHERE project_id = ? AND customer_id = ? AND is_active = TRUE
                ORDER BY updated_at DESC
            """.trimIndent()

            tx.connection.prepareStatement(sql).use { stmt ->
                stmt.setString(1, projectId)
                stmt.setString(2, customerId)
                val rs = stmt.executeQuery()
                val list = mutableListOf<CopilotUserMemory>()
                while (rs.next()) {
                    list.add(mapMemory(rs))
                }
                list
            }
        }
    }

    suspend fun deleteMemory(projectId: String, customerId: String, key: String): Boolean {
        val tenantContext = TenantContext(projectId = projectId)
        return transactionManager.inTransaction(tenantContext) { tx ->
            val sql = """
                UPDATE ai_user_memory
                SET is_active = FALSE, updated_at = NOW()
                WHERE project_id = ? AND customer_id = ? AND preference_key = ?
            """.trimIndent()

            tx.connection.prepareStatement(sql).use { stmt ->
                stmt.setString(1, projectId)
                stmt.setString(2, customerId)
                stmt.setString(3, key)
                val rows = stmt.executeUpdate()
                rows > 0
            }
        }
    }
}
