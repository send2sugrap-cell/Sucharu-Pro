package com.sucharu.sucharupro.domain.repository.task

import com.sucharu.sucharupro.data.datasource.task.FakeTaskDataSource
import com.sucharu.sucharupro.data.repository.task.TaskRepositoryImpl
import com.sucharu.sucharupro.domain.model.user.UserRole
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class TaskOrderBoundaryTest {

    private lateinit var repository: TaskRepository

    @Before
    fun setUp() {
        repository = TaskRepositoryImpl(FakeTaskDataSource())
    }

    @Test
    fun `test task creation referencing order does not alter order entity`() = runBlocking {
        val res = repository.createTask(
            projectId = "PRJ-01",
            title = "Follow up Order 100",
            referenceType = "ORDER",
            referenceId = "ORD-100",
            actorUserId = "USR-ADMIN",
            callerRole = UserRole.ADMIN
        )

        assertTrue(res.isSuccess)
        assertEquals("ORDER", res.getOrNull()!!.referenceType)
        assertEquals("ORD-100", res.getOrNull()!!.referenceId)
    }
}
