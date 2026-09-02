package com.sucharu.sucharupro.domain.repository.task

import com.sucharu.sucharupro.data.datasource.task.FakeTaskDataSource
import com.sucharu.sucharupro.data.repository.task.TaskRepositoryImpl
import com.sucharu.sucharupro.domain.model.user.UserRole
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class TaskDesignBoundaryTest {

    private lateinit var repository: TaskRepository

    @Before
    fun setUp() {
        repository = TaskRepositoryImpl(FakeTaskDataSource())
    }

    @Test
    fun `test task referencing design artwork does not alter design records`() = runBlocking {
        val res = repository.createTask(
            projectId = "PRJ-01",
            title = "Customer Proof Revision",
            referenceType = "DESIGN",
            referenceId = "DSG-10",
            actorUserId = "USR-ADMIN",
            callerRole = UserRole.ADMIN
        )

        assertTrue(res.isSuccess)
        assertEquals("DESIGN", res.getOrNull()!!.referenceType)
        assertEquals("DSG-10", res.getOrNull()!!.referenceId)
    }
}
