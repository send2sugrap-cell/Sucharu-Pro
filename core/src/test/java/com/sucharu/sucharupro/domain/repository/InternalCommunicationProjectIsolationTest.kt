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

class InternalCommunicationProjectIsolationTest {

    private lateinit var repository: InternalCommunicationRepositoryImpl

    @Before
    fun setUp() {
        val commDs = FakeInternalCommunicationDataSource()
        val notifDs = FakeNotificationDataSource()
        val notifRepo = NotificationRepositoryImpl(notifDs)
        repository = InternalCommunicationRepositoryImpl(commDs, notifRepo)
    }

    @Test
    fun `test access to communication from different project is rejected`() = runBlocking {
        val createRes = repository.createCommunication(
            projectId = "PROJECT-ALPHA",
            senderUserId = "USER-01",
            senderRole = UserRole.ADMIN,
            recipientType = InternalCommunicationRecipientType.USER,
            recipientUserIds = setOf("USER-02"),
            communicationType = InternalCommunicationType.DIRECT_MESSAGE,
            subject = "Alpha Confidential",
            message = "Alpha details",
            actorId = "USER-01",
            callerRole = UserRole.ADMIN
        )
        val commId = (createRes as DomainResult.Success).data.communicationId

        val accessRes = repository.getCommunication(
            projectId = "PROJECT-BETA",
            communicationId = commId,
            actorId = "USER-02",
            callerRole = UserRole.ADMIN
        )
        assertTrue(accessRes is DomainResult.Error)
        assertTrue((accessRes as DomainResult.Error).message.contains("not found") || accessRes.message.contains("Project isolation"))
    }
}
