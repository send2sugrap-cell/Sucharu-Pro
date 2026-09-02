package com.sucharu.sucharupro.domain.repository.task

import com.sucharu.sucharupro.data.datasource.task.FakeTaskDataSource
import com.sucharu.sucharupro.data.model.task.TaskStatus
import com.sucharu.sucharupro.data.repository.task.TaskRepositoryImpl
import com.sucharu.sucharupro.domain.model.user.UserRole
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class TaskAcknowledgementTest {

    private lateinit var repository: TaskRepository

    @Before
    fun setUp() {
        repository = TaskRepositoryImpl(FakeTaskDataSource())
    }

    @Test
    fun `test assignee can acknowledge task`() = runBlocking {
        val task = repository.createTask("PRJ-01", "Deliver Challan", assignedTo = "USR-DEL1", actorUserId = "USR-ADMIN", callerRole = UserRole.ADMIN).getOrNull()!!

        val ackRes = repository.acknowledgeTask("PRJ-01", task.taskId, "USR-DEL1", UserRole.STAFF)
        assertTrue(ackRes.isSuccess)
        assertEquals(TaskStatus.ACKNOWLEDGED, ackRes.getOrNull()!!.status)
        assertNotNull(ackRes.getOrNull()!!.acknowledgedAt)
    }
}
