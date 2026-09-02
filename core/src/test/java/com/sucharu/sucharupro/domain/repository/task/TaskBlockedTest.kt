package com.sucharu.sucharupro.domain.repository.task

import com.sucharu.sucharupro.data.datasource.task.FakeTaskDataSource
import com.sucharu.sucharupro.data.model.task.TaskStatus
import com.sucharu.sucharupro.data.repository.task.TaskRepositoryImpl
import com.sucharu.sucharupro.domain.model.user.UserRole
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class TaskBlockedTest {

    private lateinit var repository: TaskRepository

    @Before
    fun setUp() {
        repository = TaskRepositoryImpl(FakeTaskDataSource())
    }

    @Test
    fun `test block and unblock task flow`() = runBlocking {
        val task = repository.createTask("PRJ-01", "Paper Cutting", assignedTo = "USR-STAFF", actorUserId = "USR-ADMIN", callerRole = UserRole.ADMIN).getOrNull()!!
        repository.startTask("PRJ-01", task.taskId, "USR-STAFF", UserRole.STAFF)

        val blockRes = repository.blockTask("PRJ-01", task.taskId, "Paper out of stock", "USR-STAFF", UserRole.STAFF)
        assertTrue(blockRes.isSuccess)
        assertEquals(TaskStatus.BLOCKED, blockRes.getOrNull()!!.status)
        assertEquals("Paper out of stock", blockRes.getOrNull()!!.blockedReason)

        val unblockRes = repository.unblockTask("PRJ-01", task.taskId, "Stock replenished", "USR-MGR", UserRole.MANAGER)
        assertTrue(unblockRes.isSuccess)
        assertEquals(TaskStatus.IN_PROGRESS, unblockRes.getOrNull()!!.status)
        assertNull(unblockRes.getOrNull()!!.blockedReason)
    }
}
