package com.sucharu.sucharupro.domain.repository.task

import com.sucharu.sucharupro.data.datasource.task.FakeTaskDataSource
import com.sucharu.sucharupro.data.repository.task.TaskRepositoryImpl
import com.sucharu.sucharupro.domain.model.user.UserRole
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class TaskIdempotencyTest {

    private lateinit var repository: TaskRepository

    @Before
    fun setUp() {
        repository = TaskRepositoryImpl(FakeTaskDataSource())
    }

    @Test
    fun `test duplicate creation with same idempotency key returns existing task`() = runBlocking {
        val key = "IDEM-TASK-12345"
        val t1 = repository.createTask("PRJ-01", "Task 1", idempotencyKey = key, actorUserId = "USR-ADMIN", callerRole = UserRole.ADMIN).getOrNull()!!
        val t2 = repository.createTask("PRJ-01", "Task 1 Retry", idempotencyKey = key, actorUserId = "USR-ADMIN", callerRole = UserRole.ADMIN).getOrNull()!!

        assertEquals(t1.taskId, t2.taskId)
        assertEquals(t1.taskNo, t2.taskNo)
    }
}
