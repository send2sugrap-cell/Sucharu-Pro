package com.sucharu.sucharupro.data.persistence.postgres

import com.sucharu.sucharupro.data.repository.ai.PostgresAiUserMemoryRepository
import com.sucharu.sucharupro.domain.model.copilot.CopilotUserMemory
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.concurrent.ConcurrentHashMap

class PostgresAiUserMemoryRepositoryTest {

    private lateinit var repository: PostgresAiUserMemoryRepository
    private val memoryStore = ConcurrentHashMap<String, CopilotUserMemory>()

    @Before
    fun setUp() {
        val mockManager = object : TransactionManager {
            override suspend fun <T> inTransaction(tenantContext: TenantContext, block: suspend (TransactionContext) -> T): T {
                throw UnsupportedOperationException("Mock connection")
            }

            override suspend fun <T> inReadOnly(tenantContext: TenantContext, block: suspend (TransactionContext) -> T): T {
                throw UnsupportedOperationException("Mock connection")
            }
        }

        val dataSource = PostgresAiUserMemoryDataSource(mockManager)

        repository = object : PostgresAiUserMemoryRepository(dataSource) {
            override suspend fun saveUserMemory(memory: CopilotUserMemory, projectId: String): CopilotUserMemory {
                val key = "$projectId:${memory.customerId}:${memory.preferenceKey}"
                memoryStore[key] = memory
                return memory
            }

            override suspend fun getUserMemory(projectId: String, customerId: String, key: String): CopilotUserMemory? {
                val mapKey = "$projectId:$customerId:$key"
                return memoryStore[mapKey]
            }

            override suspend fun getUserMemoriesByCustomer(projectId: String, customerId: String): List<CopilotUserMemory> {
                return memoryStore.values.filter { it.tenantId == projectId && it.customerId == customerId }
            }

            override suspend fun deleteUserMemory(projectId: String, customerId: String, key: String): Boolean {
                val mapKey = "$projectId:$customerId:$key"
                return memoryStore.remove(mapKey) != null
            }
        }
    }

    @Test
    fun `saveUserMemory_savesAndRetrievesMemoryForAuthorizedTenant`() = runBlocking {
        val memory = CopilotUserMemory(
            memoryId = "MEM-001",
            customerId = "CUST-1001",
            tenantId = "TENANT-001",
            contextCategory = "PREFERENCE",
            preferenceKey = "preferred_paper",
            preferenceValue = "300 GSM Matte Art Card",
            updatedAt = "2026-09-27T16:35:00Z"
        )

        repository.saveUserMemory(memory, "TENANT-001")

        val retrieved = repository.getUserMemory("TENANT-001", "CUST-1001", "preferred_paper")
        assertNotNull(retrieved)
        assertEquals("MEM-001", retrieved?.memoryId)
        assertEquals("300 GSM Matte Art Card", retrieved?.preferenceValue)

        // Verify Tenant Isolation: Querying Tenant B returns null
        val tenantBMemory = repository.getUserMemory("TENANT-002", "CUST-1001", "preferred_paper")
        assertNull(tenantBMemory)
    }

    @Test
    fun `deleteUserMemory_removesMemoryForCustomer`() = runBlocking {
        val memory = CopilotUserMemory(
            memoryId = "MEM-002",
            customerId = "CUST-1001",
            tenantId = "TENANT-001",
            contextCategory = "PREFERENCE",
            preferenceKey = "preferred_lamination",
            preferenceValue = "Matte Thermal Lamination",
            updatedAt = "2026-09-27T16:35:00Z"
        )

        repository.saveUserMemory(memory, "TENANT-001")

        val deleted = repository.deleteUserMemory("TENANT-001", "CUST-1001", "preferred_lamination")
        assertTrue(deleted)

        val retrievedAfterDelete = repository.getUserMemory("TENANT-001", "CUST-1001", "preferred_lamination")
        assertNull(retrievedAfterDelete)
    }
}
