package com.sucharu.sucharupro.domain.repository.task

import com.sucharu.sucharupro.data.datasource.task.FakeTaskDataSource
import com.sucharu.sucharupro.data.model.task.TaskStatus
import com.sucharu.sucharupro.data.repository.task.TaskRepositoryImpl
import com.sucharu.sucharupro.domain.model.user.UserRole
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class TaskLifecycleTest {

    private lateinit var repository: TaskRepository

    @Before
    fun setUp() {
        repository = TaskRepositoryImpl(FakeTaskDataSource())
    }

    @Test
    fun `test complete lifecycle transition DRAFT to CLOSED`() = runBlocking {
        val createRes = repository.createTask(
            projectId = "PRJ-01",
            title = "Print Proof Check",
            assignedTo = "USR-STAFF",
            actorUserId = "USR-ADMIN",
            callerRole = UserRole.ADMIN
        )
        assertTrue(createRes.isSuccess)
        val taskId = createRes.getOrNull()!!.taskId

        val ackRes = repository.acknowledgeTask("PRJ-01", taskId, "USR-STAFF", UserRole.STAFF)
        assertTrue(ackRes.isSuccess)

        val startRes = repository.startTask("PRJ-01", taskId, "USR-STAFF", UserRole.STAFF)
        assertTrue(startRes.isSuccess)

        val compRes = repository.completeTask("PRJ-01", taskId, "Done", "USR-STAFF", UserRole.STAFF)
        assertTrue(compRes.isSuccess)

        val verRes = repository.verifyTask("PRJ-01", taskId, "Verified ok", "USR-MGR", UserRole.MANAGER)
        assertTrue(verRes.isSuccess)

        val closeRes = repository.closeTask("PRJ-01", taskId, "Closed", "USR-MGR", UserRole.MANAGER)
        assertTrue(closeRes.isSuccess)
        assertEquals(TaskStatus.CLOSED, closeRes.getOrNull()!!.status)
    }
}
