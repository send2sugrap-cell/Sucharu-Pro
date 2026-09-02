package com.sucharu.sucharupro.domain.repository.task

import com.sucharu.sucharupro.data.datasource.task.FakeTaskDataSource
import com.sucharu.sucharupro.data.repository.task.TaskRepositoryImpl
import com.sucharu.sucharupro.domain.model.user.UserRole
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class TaskProgressTest {

    private lateinit var repository: TaskRepository

    @Before
    fun setUp() {
        repository = TaskRepositoryImpl(FakeTaskDataSource())
    }

    @Test
    fun `test update progress between 0 and 100`() = runBlocking {
        val task = repository.createTask("PRJ-01", "Printing Job 100", assignedTo = "USR-STAFF", actorUserId = "USR-ADMIN", callerRole = UserRole.ADMIN).getOrNull()!!

        val progRes = repository.updateProgress("PRJ-01", task.taskId, 45, "Halfway printed", "USR-STAFF", UserRole.STAFF)
        assertTrue(progRes.isSuccess)
        assertEquals(45, progRes.getOrNull()!!.progressPercentage)

        val history = repository.getProgressHistory("PRJ-01", task.taskId, "USR-STAFF", UserRole.STAFF).getOrNull()!!
        assertEquals(1, history.size)
        assertEquals(45, history[0].newProgress)
    }

    @Test
    fun `test progress out of bounds fails`() = runBlocking {
        val task = repository.createTask("PRJ-01", "Printing Job 101", assignedTo = "USR-STAFF", actorUserId = "USR-ADMIN", callerRole = UserRole.ADMIN).getOrNull()!!

        val failRes = repository.updateProgress("PRJ-01", task.taskId, 150, "Invalid", "USR-STAFF", UserRole.STAFF)
        assertTrue(failRes.isError)
    }
}
