package com.sucharu.sucharupro.domain.repository.task

import com.sucharu.sucharupro.data.datasource.task.FakeTaskDataSource
import com.sucharu.sucharupro.data.repository.task.TaskRepositoryImpl
import com.sucharu.sucharupro.domain.model.user.UserRole
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class TaskProjectIsolationTest {

    private lateinit var repository: TaskRepository

    @Before
    fun setUp() {
        repository = TaskRepositoryImpl(FakeTaskDataSource())
    }

    @Test
    fun `test cross project task access blocked`() = runBlocking {
        val task = repository.createTask("PRJ-01", "Project 1 Task", actorUserId = "USR-ADMIN", callerRole = UserRole.ADMIN).getOrNull()!!

        val accessRes = repository.getTask("PRJ-02", task.taskId, "USR-ADMIN", UserRole.ADMIN)
        assertTrue(accessRes.isError)
    }
}
