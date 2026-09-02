package com.sucharu.sucharupro.domain.repository.task

import com.sucharu.sucharupro.data.datasource.task.FakeTaskDataSource
import com.sucharu.sucharupro.data.repository.task.TaskRepositoryImpl
import com.sucharu.sucharupro.domain.model.user.UserRole
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class TaskValidationTest {

    private lateinit var repository: TaskRepository

    @Before
    fun setUp() {
        repository = TaskRepositoryImpl(FakeTaskDataSource())
    }

    @Test
    fun `test create task fails on blank title`() = runBlocking {
        val result = repository.createTask(
            projectId = "PRJ-01",
            title = "",
            actorUserId = "USR-ADMIN",
            callerRole = UserRole.ADMIN
        )
        assertTrue(result.isError)
    }

    @Test
    fun `test create task fails on credential leakage`() = runBlocking {
        val result = repository.createTask(
            projectId = "PRJ-01",
            title = "Task with password=12345",
            actorUserId = "USR-ADMIN",
            callerRole = UserRole.ADMIN
        )
        assertTrue(result.isError)
        assertTrue(result.getOrNull() == null)
    }
}
