package com.sucharu.sucharupro.domain.repository.task

import com.sucharu.sucharupro.data.datasource.task.FakeTaskDataSource
import com.sucharu.sucharupro.data.repository.task.TaskRepositoryImpl
import com.sucharu.sucharupro.domain.model.user.UserRole
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class TaskAuditTest {

    private lateinit var repository: TaskRepository

    @Before
    fun setUp() {
        repository = TaskRepositoryImpl(FakeTaskDataSource())
    }

    @Test
    fun `test operations append immutable audit events`() = runBlocking {
        val task = repository.createTask("PRJ-01", "Audit Task", assignedTo = "USR-STAFF", actorUserId = "USR-ADMIN", callerRole = UserRole.ADMIN).getOrNull()!!
        repository.startTask("PRJ-01", task.taskId, "USR-STAFF", UserRole.STAFF)
        repository.updateProgress("PRJ-01", task.taskId, 50, "Half done", "USR-STAFF", UserRole.STAFF)

        val history = repository.getActivityHistory("PRJ-01", task.taskId, "USR-ADMIN", UserRole.ADMIN).getOrNull()!!
        assertTrue(history.size >= 4) // TASK_CREATED, TASK_ASSIGNED, TASK_STARTED, TASK_PROGRESS_UPDATED
    }
}
