package com.sucharu.sucharupro.domain.repository.task

import com.sucharu.sucharupro.data.datasource.task.FakeTaskDataSource
import com.sucharu.sucharupro.data.repository.task.TaskRepositoryImpl
import com.sucharu.sucharupro.domain.model.user.UserRole
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class TaskOverdueTest {

    private lateinit var repository: TaskRepository

    @Before
    fun setUp() {
        repository = TaskRepositoryImpl(FakeTaskDataSource())
    }

    @Test
    fun `test overdue task detection`() = runBlocking {
        val pastDate = System.currentTimeMillis() - 86400000L
        repository.createTask("PRJ-01", "Past Task", dueDate = pastDate, assignedTo = "USR-STAFF", actorUserId = "USR-ADMIN", callerRole = UserRole.ADMIN)

        val overdueRes = repository.getOverdueTasks("PRJ-01", "USR-ADMIN", UserRole.ADMIN)
        assertTrue(overdueRes.isSuccess)
        assertEquals(1, overdueRes.getOrNull()!!.size)
        assertTrue(overdueRes.getOrNull()!![0].isOverdue)
    }
}
