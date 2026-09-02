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

class InternalCommunicationRecipientIsolationTest {

    private lateinit var repository: InternalCommunicationRepositoryImpl

    @Before
    fun setUp() {
        val commDs = FakeInternalCommunicationDataSource()
        val notifDs = FakeNotificationDataSource()
        val notifRepo = NotificationRepositoryImpl(notifDs)
        repository = InternalCommunicationRepositoryImpl(commDs, notifRepo)
    }

    @Test
    fun `test recipient isolation blocks non-recipient non-sender staff from viewing private DM`() = runBlocking {
        val createRes = repository.createCommunication(
            projectId = "PRJ-01",
            senderUserId = "SENDER-01",
            senderRole = UserRole.STAFF,
            recipientType = InternalCommunicationRecipientType.USER,
            recipientUserIds = setOf("TARGET-01"),
            communicationType = InternalCommunicationType.DIRECT_MESSAGE,
            subject = "Private DM",
            message = "Confidential chat",
            actorId = "SENDER-01",
            callerRole = UserRole.STAFF
        )
        val commId = (createRes as DomainResult.Success).data.communicationId

        val accessRes = repository.getCommunication(
            projectId = "PRJ-01",
            communicationId = commId,
            actorId = "UNAUTHORIZED-STAFF-02",
            callerRole = UserRole.STAFF
        )
        assertTrue(accessRes is DomainResult.Error)
        assertTrue((accessRes as DomainResult.Error).message.contains("not authorized") || accessRes.message.contains("Recipient isolation"))
    }
}
