package com.sucharu.sucharupro.domain.repository.task

import com.sucharu.sucharupro.data.datasource.task.FakeTaskDataSource
import com.sucharu.sucharupro.data.repository.task.TaskRepositoryImpl
import com.sucharu.sucharupro.domain.model.user.UserRole
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class TaskConcurrencyTest {

    private lateinit var repository: TaskRepository

    @Before
    fun setUp() {
        repository = TaskRepositoryImpl(FakeTaskDataSource())
    }

    @Test
    fun `test concurrent task progress updates remain thread safe`() = runBlocking {
        val task = repository.createTask("PRJ-01", "Concurrent Task", assignedTo = "USR-STAFF", actorUserId = "USR-ADMIN", callerRole = UserRole.ADMIN).getOrNull()!!

        val jobs = (1..10).map { idx ->
            async {
                repository.updateProgress("PRJ-01", task.taskId, idx * 5, "Progress $idx", "USR-STAFF", UserRole.STAFF)
            }
        }
        val results = jobs.awaitAll()
        assertTrue(results.all { it.isSuccess })

        val history = repository.getProgressHistory("PRJ-01", task.taskId, "USR-STAFF", UserRole.STAFF).getOrNull()!!
        assertEquals(10, history.size)
    }
}
