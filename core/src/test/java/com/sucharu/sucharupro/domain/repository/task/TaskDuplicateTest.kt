package com.sucharu.sucharupro.domain.repository.task

import com.sucharu.sucharupro.data.datasource.task.FakeTaskDataSource
import com.sucharu.sucharupro.data.repository.task.TaskRepositoryImpl
import com.sucharu.sucharupro.domain.model.user.UserRole
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class TaskDuplicateTest {

    private lateinit var repository: TaskRepository

    @Before
    fun setUp() {
        repository = TaskRepositoryImpl(FakeTaskDataSource())
    }

    @Test
    fun `test distinct tasks receive unique sequential task numbers`() = runBlocking {
        val t1 = repository.createTask("PRJ-01", "Task 1", actorUserId = "USR-ADMIN", callerRole = UserRole.ADMIN).getOrNull()!!
        val t2 = repository.createTask("PRJ-01", "Task 2", actorUserId = "USR-ADMIN", callerRole = UserRole.ADMIN).getOrNull()!!

        assertNotEquals(t1.taskNo, t2.taskNo)
        assertNotEquals(t1.taskId, t2.taskId)
    }
}
