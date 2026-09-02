package com.sucharu.sucharupro.domain.repository.task

import com.sucharu.sucharupro.data.datasource.task.FakeTaskDataSource
import com.sucharu.sucharupro.data.repository.task.TaskRepositoryImpl
import com.sucharu.sucharupro.domain.model.user.UserRole
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class TaskAuthorizationTest {

    private lateinit var repository: TaskRepository

    @Before
    fun setUp() {
        repository = TaskRepositoryImpl(FakeTaskDataSource())
    }

    @Test
    fun `test external roles prohibited from creating tasks`() = runBlocking {
        val custRes = repository.createTask("PRJ-01", "Task", actorUserId = "USR-CUST", callerRole = UserRole.CUSTOMER)
        assertTrue(custRes.isError)

        val vendRes = repository.createTask("PRJ-01", "Task", actorUserId = "USR-VEND", callerRole = UserRole.VENDOR)
        assertTrue(vendRes.isError)
    }
}
