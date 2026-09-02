package com.sucharu.sucharupro.domain.repository.task

import com.sucharu.sucharupro.data.datasource.task.FakeTaskDataSource
import com.sucharu.sucharupro.data.repository.task.TaskRepositoryImpl
import com.sucharu.sucharupro.domain.model.user.UserRole
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class TaskImmutabilityTest {

    private lateinit var repository: TaskRepository

    @Before
    fun setUp() {
        repository = TaskRepositoryImpl(FakeTaskDataSource())
    }

    @Test
    fun `test closed task cannot be re-assigned or mutated`() = runBlocking {
        val task = repository.createTask("PRJ-01", "Closed Task", assignedTo = "USR-STAFF", actorUserId = "USR-ADMIN", callerRole = UserRole.ADMIN).getOrNull()!!
        repository.startTask("PRJ-01", task.taskId, "USR-STAFF", UserRole.STAFF)
        repository.completeTask("PRJ-01", task.taskId, "Done", "USR-STAFF", UserRole.STAFF)
        repository.verifyTask("PRJ-01", task.taskId, "Verified", "USR-MGR", UserRole.MANAGER)
        val closed = repository.closeTask("PRJ-01", task.taskId, "Closed", "USR-MGR", UserRole.MANAGER).getOrNull()!!

        val updateRes = repository.updateTask(closed.copy(description = "Mutated Description"), "USR-ADMIN", UserRole.ADMIN)
        assertTrue(updateRes.isError)
    }
}
