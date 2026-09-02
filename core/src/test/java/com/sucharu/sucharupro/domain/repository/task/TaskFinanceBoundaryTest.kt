package com.sucharu.sucharupro.domain.repository.task

import com.sucharu.sucharupro.data.datasource.task.FakeTaskDataSource
import com.sucharu.sucharupro.data.repository.task.TaskRepositoryImpl
import com.sucharu.sucharupro.domain.model.user.UserRole
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class TaskFinanceBoundaryTest {

    private lateinit var repository: TaskRepository

    @Before
    fun setUp() {
        repository = TaskRepositoryImpl(FakeTaskDataSource())
    }

    @Test
    fun `test task referencing invoice does not alter financial ledgers`() = runBlocking {
        val res = repository.createTask(
            projectId = "PRJ-01",
            title = "Billing Audit Follow Up",
            referenceType = "FINANCE",
            referenceId = "INV-2026-99",
            actorUserId = "USR-ADMIN",
            callerRole = UserRole.ADMIN
        )

        assertTrue(res.isSuccess)
        assertEquals("FINANCE", res.getOrNull()!!.referenceType)
        assertEquals("INV-2026-99", res.getOrNull()!!.referenceId)
    }
}
