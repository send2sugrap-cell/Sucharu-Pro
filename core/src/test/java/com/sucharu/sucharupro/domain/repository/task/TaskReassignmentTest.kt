package com.sucharu.sucharupro.domain.repository.task

import com.sucharu.sucharupro.data.datasource.task.FakeTaskDataSource
import com.sucharu.sucharupro.data.repository.task.TaskRepositoryImpl
import com.sucharu.sucharupro.domain.model.user.UserRole
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class TaskReassignmentTest {

    private lateinit var repository: TaskRepository

    @Before
    fun setUp() {
        repository = TaskRepositoryImpl(FakeTaskDataSource())
    }

    @Test
    fun `test reassign task preserves previous assignee`() = runBlocking {
        val task = repository.createTask("PRJ-01", "QC Inspection", assignedTo = "USR-QC1", actorUserId = "USR-ADMIN", callerRole = UserRole.ADMIN).getOrNull()!!

        val reassignRes = repository.reassignTask("PRJ-01", task.taskId, "USR-QC2", "Shift handover", "USR-ADMIN", UserRole.ADMIN)
        assertTrue(reassignRes.isSuccess)
        assertEquals("USR-QC2", reassignRes.getOrNull()!!.assignedTo)

        val history = repository.getAssignmentHistory("PRJ-01", task.taskId, "USR-ADMIN", UserRole.ADMIN).getOrNull()!!
        assertEquals(2, history.size)
        assertEquals("USR-QC1", history[1].previousAssigneeId)
        assertEquals("USR-QC2", history[1].assignedTo)
    }
}
