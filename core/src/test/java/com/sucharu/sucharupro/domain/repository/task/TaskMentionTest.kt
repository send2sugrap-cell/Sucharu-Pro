package com.sucharu.sucharupro.domain.repository.task

import com.sucharu.sucharupro.data.datasource.task.FakeTaskDataSource
import com.sucharu.sucharupro.data.repository.task.TaskRepositoryImpl
import com.sucharu.sucharupro.domain.model.user.UserRole
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class TaskMentionTest {

    private lateinit var repository: TaskRepository

    @Before
    fun setUp() {
        repository = TaskRepositoryImpl(FakeTaskDataSource())
    }

    @Test
    fun `test add mention in task comment`() = runBlocking {
        val task = repository.createTask("PRJ-01", "Design Proofing", actorUserId = "USR-ADMIN", callerRole = UserRole.ADMIN).getOrNull()!!

        val mentionRes = repository.addMention("PRJ-01", task.taskId, "USR-MGR", commentId = "C1", actorUserId = "USR-STAFF", callerRole = UserRole.STAFF)
        assertTrue(mentionRes.isSuccess)
        assertEquals("USR-MGR", mentionRes.getOrNull()!!.mentionedUserId)
    }
}
