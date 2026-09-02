package com.sucharu.sucharupro.domain.repository

import com.sucharu.sucharupro.data.datasource.FakeInternalCommunicationDataSource
import com.sucharu.sucharupro.data.datasource.FakeNotificationDataSource
import com.sucharu.sucharupro.data.repository.InternalCommunicationRepositoryImpl
import com.sucharu.sucharupro.data.repository.NotificationRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.communication.internal.InternalCommunicationRecipientType
import com.sucharu.sucharupro.domain.model.communication.internal.InternalCommunicationType
import com.sucharu.sucharupro.domain.model.user.UserRole
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class InternalCommunicationMentionTest {

    private lateinit var repository: InternalCommunicationRepositoryImpl

    @Before
    fun setUp() {
        val commDs = FakeInternalCommunicationDataSource()
        val notifDs = FakeNotificationDataSource()
        val notifRepo = NotificationRepositoryImpl(notifDs)
        repository = InternalCommunicationRepositoryImpl(commDs, notifRepo)
    }

    @Test
    fun `test createMention creates mention entity and notification`() = runBlocking {
        val commRes = repository.createCommunication(
            projectId = "PRJ-01",
            senderUserId = "USER-01",
            senderRole = UserRole.STAFF,
            recipientType = InternalCommunicationRecipientType.USER,
            recipientUserIds = setOf("USER-02"),
            communicationType = InternalCommunicationType.DIRECT_MESSAGE,
            subject = "Project Update",
            message = "Hey @USER-03 check this out",
            actorId = "USER-01",
            callerRole = UserRole.STAFF
        )
        val commId = (commRes as DomainResult.Success).data.communicationId

        val mentionRes = repository.createMention(
            projectId = "PRJ-01",
            communicationId = commId,
            mentionedUserId = "USER-03",
            actorId = "USER-01",
            callerRole = UserRole.STAFF
        )
        assertTrue(mentionRes is DomainResult.Success)

        val mentionsRes = repository.getMentions("PRJ-01", "USER-03", "USER-03", UserRole.STAFF)
        assertTrue(mentionsRes is DomainResult.Success)
        val mentions = (mentionsRes as DomainResult.Success).data
        assertEquals(1, mentions.size)
        assertEquals("USER-03", mentions[0].mentionedUserId)
    }
}
