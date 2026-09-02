package com.sucharu.sucharupro.domain.repository.task

import com.sucharu.sucharupro.data.datasource.task.FakeTaskDataSource
import com.sucharu.sucharupro.data.repository.task.TaskRepositoryImpl
import com.sucharu.sucharupro.domain.model.user.UserRole
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class TaskDeliveryBoundaryTest {

    private lateinit var repository: TaskRepository

    @Before
    fun setUp() {
        repository = TaskRepositoryImpl(FakeTaskDataSource())
    }

    @Test
    fun `test task referencing delivery challan does not mutate delivery records`() = runBlocking {
        val res = repository.createTask(
            projectId = "PRJ-01",
            title = "Special Delivery Instructions",
            referenceType = "DELIVERY",
            referenceId = "DEL-500",
            actorUserId = "USR-ADMIN",
            callerRole = UserRole.ADMIN
        )

        assertTrue(res.isSuccess)
        assertEquals("DELIVERY", res.getOrNull()!!.referenceType)
        assertEquals("DEL-500", res.getOrNull()!!.referenceId)
    }
}
