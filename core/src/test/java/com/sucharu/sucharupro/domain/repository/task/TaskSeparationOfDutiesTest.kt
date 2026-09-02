package com.sucharu.sucharupro.domain.repository.task

import com.sucharu.sucharupro.data.datasource.task.FakeTaskDataSource
import com.sucharu.sucharupro.data.repository.task.TaskRepositoryImpl
import com.sucharu.sucharupro.domain.model.user.UserRole
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class TaskSeparationOfDutiesTest {

    private lateinit var repository: TaskRepository

    @Before
    fun setUp() {
        repository = TaskRepositoryImpl(FakeTaskDataSource())
    }

    @Test
    fun `test non-admin creator cannot verify own task`() = runBlocking {
        val task = repository.createTask("PRJ-01", "QC Check", assignedTo = "USR-STAFF", actorUserId = "USR-MGR1", callerRole = UserRole.MANAGER).getOrNull()!!
        repository.startTask("PRJ-01", task.taskId, "USR-STAFF", UserRole.STAFF)
        repository.completeTask("PRJ-01", task.taskId, "Done", "USR-STAFF", UserRole.STAFF)

        // Creator USR-MGR1 attempts to verify
        val verifyRes = repository.verifyTask("PRJ-01", task.taskId, "Verified", "USR-MGR1", UserRole.MANAGER)
        assertTrue(verifyRes.isError)

        // Independent manager USR-MGR2 verifies
        val okRes = repository.verifyTask("PRJ-01", task.taskId, "Verified", "USR-MGR2", UserRole.MANAGER)
        assertTrue(okRes.isSuccess)
    }
}
