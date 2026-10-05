package com.sucharu.sucharupro.data.repository.ai

import com.sucharu.sucharupro.data.persistence.postgres.PostgresAiUserMemoryDataSource
import com.sucharu.sucharupro.domain.model.copilot.CopilotUserMemory
import com.sucharu.sucharupro.domain.repository.ai.AiUserMemoryRepository

/**
 * Production-grade PostgreSQL Repository implementation for Persistent AI User Memory.
 */
open class PostgresAiUserMemoryRepository(
    private val dataSource: PostgresAiUserMemoryDataSource
) : AiUserMemoryRepository {

    override suspend fun saveUserMemory(memory: CopilotUserMemory, projectId: String): CopilotUserMemory {
        return dataSource.saveMemory(memory, projectId)
    }

    override suspend fun getUserMemory(projectId: String, customerId: String, key: String): CopilotUserMemory? {
        return dataSource.getMemory(projectId, customerId, key)
    }

    override suspend fun getUserMemoriesByCustomer(projectId: String, customerId: String): List<CopilotUserMemory> {
        return dataSource.getMemoriesByCustomer(projectId, customerId)
    }

    override suspend fun deleteUserMemory(projectId: String, customerId: String, key: String): Boolean {
        return dataSource.deleteMemory(projectId, customerId, key)
    }
}
