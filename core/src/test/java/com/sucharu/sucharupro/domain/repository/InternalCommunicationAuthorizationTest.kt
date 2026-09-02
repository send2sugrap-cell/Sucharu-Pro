package com.sucharu.sucharupro.domain.repository

import com.sucharu.sucharupro.data.datasource.FakeInternalCommunicationDataSource
import com.sucharu.sucharupro.data.datasource.FakeNotificationDataSource
import com.sucharu.sucharupro.data.repository.InternalCommunicationRepositoryImpl
import com.sucharu.sucharupro.data.repository.NotificationRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.communication.internal.InternalCommunicationPriority
import com.sucharu.sucharupro.domain.model.communication.internal.InternalCommunicationRecipientType
import com.sucharu.sucharupro.domain.model.communication.internal.InternalCommunicationType
import com.sucharu.sucharupro.domain.model.user.UserRole
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class InternalCommunicationAuthorizationTest {

    private lateinit var repository: InternalCommunicationRepositoryImpl

    @Before
    fun setUp() {
        val commDs = FakeInternalCommunicationDataSource()
        val notifDs = FakeNotificationDataSource()
        val notifRepo = NotificationRepositoryImpl(notifDs)
        repository = InternalCommunicationRepositoryImpl(commDs, notifRepo)
    }

    @Test
    fun `test customer role is strictly blocked from creating communication`() = runBlocking {
        val res = repository.createCommunication(
            projectId = "PRJ-01",
            senderUserId = "CUST-01",
            senderRole = UserRole.CUSTOMER,
            recipientType = InternalCommunicationRecipientType.USER,
            recipientUserIds = setOf("USER-01"),
            communicationType = InternalCommunicationType.DIRECT_MESSAGE,
            subject = "Hello",
            message = "External message",
            actorId = "CUST-01",
            callerRole = UserRole.CUSTOMER
        )
        assertTrue(res is DomainResult.Error)
        assertTrue((res as DomainResult.Error).message.contains("prohibited from accessing internal communications"))
    }

    @Test
    fun `test vendor role is strictly blocked from broadcast`() = runBlocking {
        val res = repository.broadcastCommunication(
            projectId = "PRJ-01",
            recipientType = InternalCommunicationRecipientType.PROJECT,
            subject = "Vendor Notice",
            message = "Unsafe broadcast",
            actorId = "VENDOR-01",
            callerRole = UserRole.VENDOR
        )
        assertTrue(res is DomainResult.Error)
        assertTrue((res as DomainResult.Error).message.contains("prohibited") || (res as DomainResult.Error).message.contains("not authorized"))
    }

    @Test
    fun `test staff role is rejected from unrestricted project broadcast`() = runBlocking {
        val res = repository.broadcastCommunication(
            projectId = "PRJ-01",
            recipientType = InternalCommunicationRecipientType.PROJECT,
            subject = "Company Wide Notice",
            message = "Staff attempt",
            actorId = "STAFF-01",
            callerRole = UserRole.STAFF
        )
        assertTrue(res is DomainResult.Error)
        assertTrue((res as DomainResult.Error).message.contains("not authorized to initiate organizational broadcasts"))
    }
}
