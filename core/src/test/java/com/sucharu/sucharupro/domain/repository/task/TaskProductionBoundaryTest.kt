package com.sucharu.sucharupro.domain.repository.task

import com.sucharu.sucharupro.data.datasource.task.FakeTaskDataSource
import com.sucharu.sucharupro.data.repository.task.TaskRepositoryImpl
import com.sucharu.sucharupro.domain.model.user.UserRole
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class TaskProductionBoundaryTest {

    private lateinit var repository: TaskRepository

    @Before
    fun setUp() {
        repository = TaskRepositoryImpl(FakeTaskDataSource())
    }

    @Test
    fun `test task referencing production job does not mutate production records`() = runBlocking {
        val res = repository.createTask(
            projectId = "PRJ-01",
            title = "Plate Cleaning for Job 50",
            referenceType = "PRODUCTION_JOB",
            referenceId = "JOB-50",
            actorUserId = "USR-ADMIN",
            callerRole = UserRole.ADMIN
        )

        assertTrue(res.isSuccess)
        assertEquals("PRODUCTION_JOB", res.getOrNull()!!.referenceType)
        assertEquals("JOB-50", res.getOrNull()!!.referenceId)
    }
}
