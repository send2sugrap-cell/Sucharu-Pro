package com.sucharu.sucharupro.domain.repository.task

import com.sucharu.sucharupro.data.datasource.task.FakeTaskDataSource
import com.sucharu.sucharupro.data.repository.task.TaskRepositoryImpl
import com.sucharu.sucharupro.domain.model.user.UserRole
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class TaskDeadlineTest {

    private lateinit var repository: TaskRepository

    @Before
    fun setUp() {
        repository = TaskRepositoryImpl(FakeTaskDataSource())
    }

    @Test
    fun `test create task with valid due date`() = runBlocking {
        val futureDate = System.currentTimeMillis() + 86400000L
        val res = repository.createTask("PRJ-01", "Deadline Task", dueDate = futureDate, actorUserId = "USR-ADMIN", callerRole = UserRole.ADMIN)
        assertTrue(res.isSuccess)
        assertEquals(futureDate, res.getOrNull()!!.dueDate)
    }
}
