package com.sucharu.sucharupro.domain.repository

import com.sucharu.sucharupro.data.datasource.FakeInternalCommunicationDataSource
import com.sucharu.sucharupro.data.datasource.FakeNotificationDataSource
import com.sucharu.sucharupro.data.repository.InternalCommunicationRepositoryImpl
import com.sucharu.sucharupro.data.repository.NotificationRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.communication.internal.InternalCommunicationRecipientType
import com.sucharu.sucharupro.domain.model.user.UserRole
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class InternalCommunicationThreadTest {

    private lateinit var repository: InternalCommunicationRepositoryImpl

    @Before
    fun setUp() {
        val commDs = FakeInternalCommunicationDataSource()
        val notifDs = FakeNotificationDataSource()
        val notifRepo = NotificationRepositoryImpl(notifDs)
        repository = InternalCommunicationRepositoryImpl(commDs, notifRepo)
    }

    @Test
    fun `test createThread creates thread entity and root message`() = runBlocking {
        val res = repository.createThread(
            projectId = "PRJ-01",
            subject = "Design Review Thread",
            initialMessage = "Let us discuss design proof v2",
            senderUserId = "DESIGNER-01",
            senderRole = UserRole.DESIGNER,
            recipientType = InternalCommunicationRecipientType.USER,
            recipientUserIds = setOf("QC-01", "MANAGER-01"),
            actorId = "DESIGNER-01",
            callerRole = UserRole.DESIGNER
        )
        assertTrue(res is DomainResult.Success)
        val thread = (res as DomainResult.Success).data

        assertEquals("PRJ-01", thread.projectId)
        assertEquals("Design Review Thread", thread.subject)
        assertTrue(thread.participantUserIds.contains("DESIGNER-01"))
        assertTrue(thread.participantUserIds.contains("QC-01"))
    }
}
