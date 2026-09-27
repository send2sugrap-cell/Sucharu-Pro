package com.sucharu.sucharupro.domain.repository.ai

import com.sucharu.sucharupro.domain.model.copilot.CopilotUserMemory

/**
 * Repository interface for Persistent AI User Memory & Conversation Context.
 */
interface AiUserMemoryRepository {

    suspend fun saveUserMemory(memory: CopilotUserMemory, projectId: String): CopilotUserMemory

    suspend fun getUserMemory(projectId: String, customerId: String, key: String): CopilotUserMemory?

    suspend fun getUserMemoriesByCustomer(projectId: String, customerId: String): List<CopilotUserMemory>

    suspend fun deleteUserMemory(projectId: String, customerId: String, key: String): Boolean
}
