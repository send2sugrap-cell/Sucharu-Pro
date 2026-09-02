package com.sucharu.sucharupro.domain.repository.task

import com.sucharu.sucharupro.data.datasource.task.FakeTaskDataSource
import com.sucharu.sucharupro.data.repository.task.TaskRepositoryImpl
import com.sucharu.sucharupro.domain.model.user.UserRole
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class TaskAssigneeIsolationTest {

    private lateinit var repository: TaskRepository

    @Before
    fun setUp() {
        repository = TaskRepositoryImpl(FakeTaskDataSource())
    }

    @Test
    fun `test staff user cannot view confidential task assigned to another`() = runBlocking {
        val task = repository.createTask("PRJ-01", "Private Task", assignedTo = "USR-STAFF1", actorUserId = "USR-ADMIN", callerRole = UserRole.ADMIN).getOrNull()!!

        val viewRes = repository.getTask("PRJ-01", task.taskId, "USR-STAFF2", UserRole.STAFF)
        assertTrue(viewRes.isError)
    }
}
