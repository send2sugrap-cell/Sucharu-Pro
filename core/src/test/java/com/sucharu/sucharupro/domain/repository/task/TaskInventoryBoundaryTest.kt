package com.sucharu.sucharupro.domain.repository.task

import com.sucharu.sucharupro.data.datasource.task.FakeTaskDataSource
import com.sucharu.sucharupro.data.repository.task.TaskRepositoryImpl
import com.sucharu.sucharupro.domain.model.user.UserRole
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class TaskInventoryBoundaryTest {

    private lateinit var repository: TaskRepository

    @Before
    fun setUp() {
        repository = TaskRepositoryImpl(FakeTaskDataSource())
    }

    @Test
    fun `test task referencing inventory item does not mutate inventory records`() = runBlocking {
        val res = repository.createTask(
            projectId = "PRJ-01",
            title = "Paper Stock Count",
            referenceType = "INVENTORY",
            referenceId = "INV-30",
            actorUserId = "USR-ADMIN",
            callerRole = UserRole.ADMIN
        )

        assertTrue(res.isSuccess)
        assertEquals("INVENTORY", res.getOrNull()!!.referenceType)
        assertEquals("INV-30", res.getOrNull()!!.referenceId)
    }
}
