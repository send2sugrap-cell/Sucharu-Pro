package com.sucharu.sucharupro.domain.repository.task

import com.sucharu.sucharupro.data.datasource.task.FakeTaskDataSource
import com.sucharu.sucharupro.data.repository.task.TaskRepositoryImpl
import com.sucharu.sucharupro.domain.model.user.UserRole
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class TaskCommentTest {

    private lateinit var repository: TaskRepository

    @Before
    fun setUp() {
        repository = TaskRepositoryImpl(FakeTaskDataSource())
    }

    @Test
    fun `test add discussion comment`() = runBlocking {
        val task = repository.createTask("PRJ-01", "Design Proofing", actorUserId = "USR-ADMIN", callerRole = UserRole.ADMIN).getOrNull()!!

        val commRes = repository.addComment("PRJ-01", task.taskId, "Please verify customer logo vector file.", "USR-DES1", UserRole.DESIGNER)
        assertTrue(commRes.isSuccess)
        assertEquals("Please verify customer logo vector file.", commRes.getOrNull()!!.content)
    }
}
