package com.sucharu.sucharupro.domain.repository.task

import com.sucharu.sucharupro.data.datasource.task.FakeTaskDataSource
import com.sucharu.sucharupro.data.repository.task.TaskRepositoryImpl
import com.sucharu.sucharupro.domain.model.user.UserRole
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class TaskAssignmentTest {

    private lateinit var repository: TaskRepository

    @Before
    fun setUp() {
        repository = TaskRepositoryImpl(FakeTaskDataSource())
    }

    @Test
    fun `test assign task records history`() = runBlocking {
        val task = repository.createTask("PRJ-01", "Design Banner", actorUserId = "USR-ADMIN", callerRole = UserRole.ADMIN).getOrNull()!!

        val assignRes = repository.assignTask("PRJ-01", task.taskId, "USR-DES1", "High priority assignment", "USR-ADMIN", UserRole.ADMIN)
        assertTrue(assignRes.isSuccess)
        assertEquals("USR-DES1", assignRes.getOrNull()!!.assignedTo)

        val history = repository.getAssignmentHistory("PRJ-01", task.taskId, "USR-ADMIN", UserRole.ADMIN).getOrNull()!!
        assertEquals(1, history.size)
        assertEquals("USR-DES1", history[0].assignedTo)
    }
}
