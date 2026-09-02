package com.sucharu.sucharupro.domain.repository.task

import com.sucharu.sucharupro.data.datasource.FakeInternalCommunicationDataSource
import com.sucharu.sucharupro.data.datasource.task.FakeTaskDataSource
import com.sucharu.sucharupro.data.repository.InternalCommunicationRepositoryImpl
import com.sucharu.sucharupro.data.repository.task.TaskRepositoryImpl
import com.sucharu.sucharupro.domain.model.user.UserRole
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class TaskInternalCommunicationIntegrationTest {

    private lateinit var commRepo: InternalCommunicationRepositoryImpl
    private lateinit var repository: TaskRepository

    @Before
    fun setUp() {
        val notifRepo = com.sucharu.sucharupro.data.repository.NotificationRepositoryImpl(com.sucharu.sucharupro.data.datasource.FakeNotificationDataSource())
        commRepo = InternalCommunicationRepositoryImpl(FakeInternalCommunicationDataSource(), notifRepo)
        repository = TaskRepositoryImpl(FakeTaskDataSource(), internalCommunicationRepository = commRepo)
    }


    @Test
    fun `test mentions in task operate safely alongside internal communication`() = runBlocking {
        val task = repository.createTask("PRJ-01", "Comm Task", assignedTo = "USR-STAFF", actorUserId = "USR-ADMIN", callerRole = UserRole.ADMIN).getOrNull()!!
        val mentionRes = repository.addMention("PRJ-01", task.taskId, "USR-MGR", actorUserId = "USR-STAFF", callerRole = UserRole.STAFF)

        assertTrue(mentionRes.isSuccess)
    }
}
