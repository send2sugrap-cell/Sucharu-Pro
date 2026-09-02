package com.sucharu.sucharupro.domain.repository.task

import com.sucharu.sucharupro.data.datasource.FakeNotificationDataSource
import com.sucharu.sucharupro.data.datasource.task.FakeTaskDataSource
import com.sucharu.sucharupro.data.repository.NotificationRepositoryImpl
import com.sucharu.sucharupro.data.repository.task.TaskRepositoryImpl
import com.sucharu.sucharupro.domain.model.user.UserRole
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class TaskNotificationIntegrationTest {

    private lateinit var notifRepo: NotificationRepositoryImpl
    private lateinit var repository: TaskRepository

    @Before
    fun setUp() {
        notifRepo = NotificationRepositoryImpl(FakeNotificationDataSource())
        repository = TaskRepositoryImpl(FakeTaskDataSource(), notificationRepository = notifRepo)
    }

    @Test
    fun `test assigning task dispatches notification to assignee`() = runBlocking {
        val task = repository.createTask("PRJ-01", "Notified Task", assignedTo = "USR-STAFF", actorUserId = "USR-ADMIN", callerRole = UserRole.ADMIN).getOrNull()!!

        val notifs = notifRepo.getUserNotifications("PRJ-01", "USR-STAFF", actorId = "USR-ADMIN", callerRole = UserRole.ADMIN).getOrNull()!!
        assertTrue(notifs.isNotEmpty())
        assertEquals("USR-STAFF", notifs[0].recipientUserId)
        assertEquals("TASK", notifs[0].referenceType)
        assertEquals(task.taskId, notifs[0].referenceId)
    }
}
